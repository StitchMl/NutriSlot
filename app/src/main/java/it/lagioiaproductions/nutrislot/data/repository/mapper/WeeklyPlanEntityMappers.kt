package it.lagioiaproductions.nutrislot.data.repository.mapper

import it.lagioiaproductions.nutrislot.data.local.room.MealAssignmentEntity
import it.lagioiaproductions.nutrislot.data.local.room.MealConsumptionEntity
import it.lagioiaproductions.nutrislot.data.local.room.MealOptionEntity
import it.lagioiaproductions.nutrislot.data.local.room.MealRuleEntity
import it.lagioiaproductions.nutrislot.data.local.room.MealSlotEntity
import it.lagioiaproductions.nutrislot.data.local.room.WeeklyPlanEntity
import it.lagioiaproductions.nutrislot.domain.model.MealAssignment
import it.lagioiaproductions.nutrislot.domain.model.MealConsumption
import it.lagioiaproductions.nutrislot.domain.model.MealOption
import it.lagioiaproductions.nutrislot.domain.model.MealOptionSourceType
import it.lagioiaproductions.nutrislot.domain.model.MealRule
import it.lagioiaproductions.nutrislot.domain.model.MealSlot
import it.lagioiaproductions.nutrislot.domain.model.MealSlotType
import it.lagioiaproductions.nutrislot.domain.model.WeekDay
import it.lagioiaproductions.nutrislot.domain.model.WeeklyPlan

internal fun WeeklyPlanEntity.toDomain(): WeeklyPlan {
    return WeeklyPlan(
        id = id,
        title = title,
        sourceFileName = sourceFileName,
        createdAtEpochMillis = createdAtEpochMillis
    )
}

internal fun MealSlotEntity.toDomain(): MealSlot {
    return MealSlot(
        id = id,
        planId = planId,
        dayOfWeek = WeekDay.valueOf(dayOfWeek),
        mealSlotType = MealSlotType.valueOf(mealSlotType),
        plannedMealText = plannedMealText
    )
}

internal fun MealConsumptionEntity.toDomain(): MealConsumption {
    return MealConsumption(
        id = id,
        planId = planId,
        targetSlotId = targetSlotId,
        sourceSlotId = sourceSlotId,
        consumedAtEpochMillis = consumedAtEpochMillis
    )
}

internal fun MealAssignmentEntity.toDomain(): MealAssignment {
    return MealAssignment(
        id = id,
        planId = planId,
        targetSlotId = targetSlotId,
        sourceSlotId = sourceSlotId,
        assignedAtEpochMillis = assignedAtEpochMillis
    )
}

internal fun MealOptionEntity.toDomain(): MealOption {
    return MealOption(
        id = id,
        planId = planId,
        mealSlotType = MealSlotType.valueOf(mealSlotType),
        title = title,
        mealText = mealText,
        sourceType = MealOptionSourceType.valueOf(sourceType),
        tags = deserializeStringList(tagsSerialized),
        pageNumber = pageNumber
    )
}

internal fun MealRuleEntity.toDomain(): MealRule {
    return MealRule(
        id = id,
        planId = planId,
        mealSlotType = MealSlotType.valueOf(mealSlotType),
        label = label,
        requiredComponents = deserializeStringList(requiredComponentsSerialized),
        pageNumber = pageNumber
    )
}

internal fun normalizeMealText(text: String): String {
    return text
        .replace("\r\n", "\n")
        .replace("\r", "\n")
        .lines()
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .joinToString(separator = "\n")
}

internal fun serializeStringList(values: List<String>): String {
    return values
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .joinToString(separator = "||")
}

internal fun deserializeStringList(serialized: String): List<String> {
    return serialized
        .split("||")
        .map { it.trim() }
        .filter { it.isNotBlank() }
}

internal fun areMealSlotTypesCompatible(
    targetType: MealSlotType,
    sourceType: MealSlotType
): Boolean {
    if (targetType == sourceType) {
        return true
    }

    return when (targetType) {
        MealSlotType.LUNCH -> sourceType == MealSlotType.DINNER
        MealSlotType.DINNER -> sourceType == MealSlotType.LUNCH
        MealSlotType.MORNING_SNACK -> sourceType == MealSlotType.AFTERNOON_SNACK
        MealSlotType.AFTERNOON_SNACK -> sourceType == MealSlotType.MORNING_SNACK
        MealSlotType.BREAKFAST -> false
    }
}