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
                plannedMealText = normalizedCellMap[day to mealSlotType].orEmpty()
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

    val targetEntities = weeklyTargets.mapIndexed { index, target ->
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
