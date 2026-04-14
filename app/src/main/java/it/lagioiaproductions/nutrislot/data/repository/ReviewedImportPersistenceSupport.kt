package it.lagioiaproductions.nutrislot.data.repository

import it.lagioiaproductions.nutrislot.data.local.room.MealOptionEntity
import it.lagioiaproductions.nutrislot.data.local.room.MealRuleEntity
import it.lagioiaproductions.nutrislot.data.local.room.MealSlotEntity
import it.lagioiaproductions.nutrislot.data.local.room.WeeklyFrequencyTargetEntity
import it.lagioiaproductions.nutrislot.data.local.room.WeeklyPlanEntity
import it.lagioiaproductions.nutrislot.data.repository.mapper.normalizeMealText
import it.lagioiaproductions.nutrislot.data.repository.mapper.serializeStringList
import it.lagioiaproductions.nutrislot.data.repository.model.ReviewedImportedMealCell
import it.lagioiaproductions.nutrislot.data.repository.model.ReviewedImportedMealOption
import it.lagioiaproductions.nutrislot.data.repository.model.ReviewedImportedMealRule
import it.lagioiaproductions.nutrislot.data.repository.model.ReviewedImportedWeeklyFrequencyTarget
import it.lagioiaproductions.nutrislot.domain.model.MealSlotType
import it.lagioiaproductions.nutrislot.domain.model.WeekDay
import it.lagioiaproductions.nutrislot.domain.model.WeeklyFrequencyTargetSupport
import java.util.UUID

internal data class ImportedPlanPersistencePayload(
    val plan: WeeklyPlanEntity,
    val slots: List<MealSlotEntity>,
    val options: List<MealOptionEntity>,
    val rules: List<MealRuleEntity>,
    val weeklyTargets: List<WeeklyFrequencyTargetEntity>,
    val reusedExistingPlanId: Boolean
)

internal fun buildImportedPlanPersistencePayload(
    existingPlanId: String?,
    sourceFileName: String?,
    cells: List<ReviewedImportedMealCell>,
    extraOptions: List<ReviewedImportedMealOption> = emptyList(),
    mealRules: List<ReviewedImportedMealRule> = emptyList(),
    weeklyTargets: List<ReviewedImportedWeeklyFrequencyTarget> = emptyList(),
    createdAtEpochMillis: Long = System.currentTimeMillis()
): ImportedPlanPersistencePayload {
    val reusedExistingPlanId = !existingPlanId.isNullOrBlank()
    val planId = existingPlanId?.takeIf(String::isNotBlank) ?: UUID.randomUUID().toString()
    val resolvedWeeklyTargets = weeklyTargets.ifEmpty {
        if (sourceFileName == null) {
            manualBaselineWeeklyFrequencyTargets()
        } else {
            emptyList()
        }
    }

    val normalizedCellMap = cells.associateBy(
        keySelector = { it.dayOfWeek to it.mealSlotType },
        valueTransform = { normalizeMealText(it.mealText) }
    )

    val plan = WeeklyPlanEntity(
        id = planId,
        title = sourceFileName
            ?.substringBeforeLast(".", missingDelimiterValue = sourceFileName)
            ?.takeIf { it.isNotBlank() },
        sourceFileName = sourceFileName,
        createdAtEpochMillis = createdAtEpochMillis
    )

    val slots = WeekDay.orderedValues().flatMap { day ->
        MealSlotType.orderedValues().map { mealSlotType ->
            MealSlotEntity(
                id = "${planId}_${day.name}_${mealSlotType.name}",
                planId = planId,
                dayOfWeek = day.name,
                mealSlotType = mealSlotType.name,
                plannedMealText = normalizedCellMap[day to mealSlotType].orEmpty(),
                consumptionTargetKeysSerialized = "",
                consumptionTargetSource = null
            )
        }
    }

    val options = extraOptions.mapIndexed { index, option ->
        MealOptionEntity(
            id = "${planId}_OPTION_$index",
            planId = planId,
            mealSlotType = option.mealSlotType.name,
            title = option.title,
            mealText = normalizeMealText(option.mealText),
            sourceType = option.sourceType.name,
            tagsSerialized = serializeStringList(option.tags),
            pageNumber = option.pageNumber
        )
    }

    val rules = mealRules.mapIndexed { index, rule ->
        MealRuleEntity(
            id = "${planId}_RULE_$index",
            planId = planId,
            mealSlotType = rule.mealSlotType.name,
            label = rule.label,
            requiredComponentsSerialized = serializeStringList(rule.requiredComponents),
            pageNumber = rule.pageNumber
        )
    }

    val targetEntities = resolvedWeeklyTargets.mapIndexed { index, target ->
        WeeklyFrequencyTargetEntity(
            id = "${planId}_TARGET_$index",
            planId = planId,
            title = target.title,
            canonicalKey = target.canonicalKey,
            portionText = target.portionText,
            minimumTimesPerWeek = target.minimumTimesPerWeek,
            maximumTimesPerWeek = target.maximumTimesPerWeek,
            matchTermsSerialized = serializeStringList(target.matchTerms),
            pageNumber = target.pageNumber,
            sourceText = target.sourceText
        )
    }

    return ImportedPlanPersistencePayload(
        plan = plan,
        slots = slots,
        options = options,
        rules = rules,
        weeklyTargets = targetEntities,
        reusedExistingPlanId = reusedExistingPlanId
    )
}

private fun manualBaselineWeeklyFrequencyTargets(): List<ReviewedImportedWeeklyFrequencyTarget> {
    return listOf(
        manualBaselineWeeklyFrequencyTarget(
            canonicalKey = "frutta e verdura",
            sourceText = "Consumare almeno 5 porzioni di frutta e verdura al giorno per assicurarsi un quantitativo sufficiente di antiossidanti",
            minimumTimesPerWeek = 5
        ),
        manualBaselineWeeklyFrequencyTarget(
            canonicalKey = "acqua",
            portionText = "2 l",
            sourceText = "Bere almeno 2L di acqua/die per favorire l'escrezione renale",
            minimumTimesPerWeek = 2000
        ),
        manualBaselineWeeklyFrequencyTarget(
            canonicalKey = "caffe e the",
            sourceText = "Consumare massimo N.3 caffe/the al giorno",
            maximumTimesPerWeek = 3
        ),
        manualBaselineWeeklyFrequencyTarget(
            canonicalKey = "carne bianca",
            sourceText = "Carne bianca 2-3 volte a settimana",
            minimumTimesPerWeek = 2,
            maximumTimesPerWeek = 3
        ),
        manualBaselineWeeklyFrequencyTarget(
            canonicalKey = "carne rossa",
            sourceText = "Carne rossa 1 volta a settimana",
            minimumTimesPerWeek = 1,
            maximumTimesPerWeek = 1
        ),
        manualBaselineWeeklyFrequencyTarget(
            canonicalKey = "affettati",
            sourceText = "Affettati 1 volta a settimana",
            minimumTimesPerWeek = 1,
            maximumTimesPerWeek = 1
        ),
        manualBaselineWeeklyFrequencyTarget(
            canonicalKey = "uova",
            portionText = "N. 2 uova",
            sourceText = "Uova 1 porzione a settimana (N. 2 uova)",
            minimumTimesPerWeek = 1,
            maximumTimesPerWeek = 1
        ),
        manualBaselineWeeklyFrequencyTarget(
            canonicalKey = "formaggi",
            sourceText = "Formaggi 2 volte a settimana",
            minimumTimesPerWeek = 2,
            maximumTimesPerWeek = 2
        ),
        manualBaselineWeeklyFrequencyTarget(
            canonicalKey = "patate",
            sourceText = "Patate 1 volta a settimana",
            minimumTimesPerWeek = 1,
            maximumTimesPerWeek = 1
        ),
        manualBaselineWeeklyFrequencyTarget(
            canonicalKey = "piatto unico",
            portionText = "legumi + cereal",
            sourceText = "Piatto unico 2-3 volte a settimana (legumi + cereal)",
            minimumTimesPerWeek = 2,
            maximumTimesPerWeek = 3
        ),
        manualBaselineWeeklyFrequencyTarget(
            canonicalKey = "pesce",
            sourceText = "Pesce 3-4 volte a settimana",
            minimumTimesPerWeek = 3,
            maximumTimesPerWeek = 4
        )
    ).sortedBy { it.title }
}

private fun manualBaselineWeeklyFrequencyTarget(
    canonicalKey: String,
    sourceText: String,
    portionText: String? = null,
    minimumTimesPerWeek: Int? = null,
    maximumTimesPerWeek: Int? = null
): ReviewedImportedWeeklyFrequencyTarget {
    val title = WeeklyFrequencyTargetSupport.formatTitle(canonicalKey)
    return ReviewedImportedWeeklyFrequencyTarget(
        title = title,
        canonicalKey = canonicalKey,
        portionText = portionText,
        minimumTimesPerWeek = minimumTimesPerWeek,
        maximumTimesPerWeek = maximumTimesPerWeek,
        matchTerms = WeeklyFrequencyTargetSupport.resolveMatchTerms(
            title = title,
            sourceText = sourceText
        ),
        pageNumber = null,
        sourceText = sourceText
    )
}
