package it.lagioiaproductions.nutrislot.data.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import it.lagioiaproductions.nutrislot.BuildConfig
import it.lagioiaproductions.nutrislot.data.ai.GeminiNutritionEstimator
import it.lagioiaproductions.nutrislot.data.local.room.NutriSlotDatabase
import it.lagioiaproductions.nutrislot.domain.model.ImportedMealNutrition

class NutritionEnrichmentWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    private val estimator = GeminiNutritionEstimator(
        apiKey = BuildConfig.GEMINI_API_KEY
    )

    override suspend fun doWork(): Result {
        val planId = inputData.getString(KEY_PLAN_ID)?.trim().orEmpty()
        if (planId.isBlank()) return Result.failure()

        val dao = NutriSlotDatabase
            .getInstance(applicationContext)
            .weeklyPlanDao()

        val plan = dao.getPlanById(planId) ?: return Result.failure()

        return runCatching {
            val slots = dao.getSlotsForPlan(plan.id)
            val options = dao.getMealOptionsForPlan(plan.id)

            val nutritionCache = linkedMapOf<String, ImportedMealNutrition?>()

            @Suppress("RedundantSuspendModifier")
            suspend fun estimateCached(text: String): ImportedMealNutrition? {
                val baseText = stripAiNutritionFooter(text)
                if (baseText.isBlank()) return null

                if (nutritionCache.containsKey(baseText)) {
                    return nutritionCache[baseText]
                }

                val estimated = estimator.estimateNutritionForMeal(baseText)
                nutritionCache[baseText] = estimated
                return estimated
            }

            var updatedSlotsCount = 0
            var updatedOptionsCount = 0

            val updatedSlots = slots.map { slot ->
                val enrichedText = appendAiNutritionFooter(
                    originalMealText = stripAiNutritionFooter(slot.plannedMealText),
                    nutrition = estimateCached(slot.plannedMealText)
                )

                if (enrichedText != slot.plannedMealText) {
                    updatedSlotsCount++
                    slot.copy(plannedMealText = enrichedText)
                } else {
                    slot
                }
            }

            val updatedOptions = options.map { option ->
                val enrichedText = appendAiNutritionFooter(
                    originalMealText = stripAiNutritionFooter(option.mealText),
                    nutrition = estimateCached(option.mealText)
                )

                if (enrichedText != option.mealText) {
                    updatedOptionsCount++
                    option.copy(mealText = enrichedText)
                } else {
                    option
                }
            }

            if (updatedSlots.isNotEmpty()) {
                dao.insertMealSlots(updatedSlots)
            }
            if (updatedOptions.isNotEmpty()) {
                dao.insertMealOptions(updatedOptions)
            }

            Result.success(
                workDataOf(
                    KEY_UPDATED_SLOTS to updatedSlotsCount,
                    KEY_UPDATED_OPTIONS to updatedOptionsCount
                )
            )
        }.getOrElse {
            Result.retry()
        }
    }

    companion object {
        private const val KEY_PLAN_ID = "plan_id"
        private const val KEY_UPDATED_SLOTS = "updated_slots"
        private const val KEY_UPDATED_OPTIONS = "updated_options"

        private fun uniqueWorkName(planId: String): String =
            "nutrition_enrichment_$planId"

        fun enqueue(
            context: Context,
            planId: String
        ) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = OneTimeWorkRequestBuilder<NutritionEnrichmentWorker>()
                .setInputData(
                    Data.Builder()
                        .putString(KEY_PLAN_ID, planId)
                        .build()
                )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                uniqueWorkName(planId),
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }
}

private fun stripAiNutritionFooter(text: String): String {
    val markerIndex = text.indexOf("Nutrienti: ")
    return if (markerIndex >= 0) {
        text.substring(0, markerIndex).trim()
    } else {
        text.trim()
    }
}

private fun appendAiNutritionFooter(
    originalMealText: String,
    nutrition: ImportedMealNutrition?
): String {
    val cleaned = originalMealText
        .replace("\r\n", "\n")
        .replace("\r", "\n")
        .trim()

    if (cleaned.isBlank() || nutrition == null || !nutrition.hasAnyValue) {
        return cleaned
    }

    val lines = buildList {
        add("Nutrienti: ")
        nutrition.calories?.let { add("$it kcal") }
        nutrition.proteinGrams?.let { add("$it g proteine") }
        nutrition.carbsGrams?.let { add("$it g carboidrati") }
        nutrition.fibreGrams?.let { add("$it g fibre") }
    }

    return buildString {
        append(cleaned)
        append("\n\n")
        append(lines.joinToString("\n"))
    }
}