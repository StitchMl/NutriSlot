package it.lagioiaproductions.nutrislot.data.repository

import it.lagioiaproductions.nutrislot.data.local.room.MealAssignmentEntity
import it.lagioiaproductions.nutrislot.data.local.room.MealConsumptionEntity
import it.lagioiaproductions.nutrislot.data.local.room.MealOptionEntity
import it.lagioiaproductions.nutrislot.data.local.room.MealRuleEntity
import it.lagioiaproductions.nutrislot.data.local.room.MealSlotEntity
import it.lagioiaproductions.nutrislot.data.local.room.WeeklyPlanDao
import it.lagioiaproductions.nutrislot.data.local.room.WeeklyPlanEntity
import it.lagioiaproductions.nutrislot.data.repository.mapper.areMealSlotTypesCompatible
import it.lagioiaproductions.nutrislot.data.repository.mapper.normalizeMealText
import it.lagioiaproductions.nutrislot.data.repository.mapper.serializeStringList
import it.lagioiaproductions.nutrislot.data.repository.mapper.toDomain
import it.lagioiaproductions.nutrislot.data.repository.model.ReviewedImportedMealCell
import it.lagioiaproductions.nutrislot.data.repository.model.ReviewedImportedMealOption
import it.lagioiaproductions.nutrislot.data.repository.model.ReviewedImportedMealRule
import it.lagioiaproductions.nutrislot.data.repository.planning.WeeklyPlanningCalculator
import it.lagioiaproductions.nutrislot.domain.model.MealSlotType
import it.lagioiaproductions.nutrislot.domain.model.WeekDay
import it.lagioiaproductions.nutrislot.domain.model.WeeklyPlanSnapshot
import it.lagioiaproductions.nutrislot.domain.model.sortedForWeeklyDisplay
import java.util.UUID

class WeeklyPlanRepository(
    private val weeklyPlanDao: WeeklyPlanDao
) {

    suspend fun saveReviewedImport(
        sourceFileName: String?,
        cells: List<ReviewedImportedMealCell>,
        extraOptions: List<ReviewedImportedMealOption> = emptyList(),
        mealRules: List<ReviewedImportedMealRule> = emptyList()
    ): String {
        val planId = UUID.randomUUID().toString()
        val createdAt = System.currentTimeMillis()

        val normalizedCellMap = cells.associateBy(
            keySelector = { it.dayOfWeek to it.mealSlotType },
            valueTransform = { normalizeMealText(it.mealText) }
        )

        val weeklyPlanEntity = WeeklyPlanEntity(
            id = planId,
            title = sourceFileName
                ?.substringBeforeLast(".", missingDelimiterValue = sourceFileName)
                ?.takeIf { it.isNotBlank() },
            sourceFileName = sourceFileName,
            createdAtEpochMillis = createdAt
        )

        val mealSlotEntities = WeekDay.orderedValues().flatMap { day ->
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

        val optionEntities = extraOptions.mapIndexed { index, option ->
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

        val ruleEntities = mealRules.mapIndexed { index, rule ->
            MealRuleEntity(
                id = "${planId}_RULE_$index",
                planId = planId,
                mealSlotType = rule.mealSlotType.name,
                label = rule.label,
                requiredComponentsSerialized = serializeStringList(rule.requiredComponents),
                pageNumber = rule.pageNumber
            )
        }

        weeklyPlanDao.insertImportedPlan(
            plan = weeklyPlanEntity,
            slots = mealSlotEntities,
            options = optionEntities,
            rules = ruleEntities
        )

        return planId
    }

    suspend fun assignMealToSlot(
        planId: String,
        targetSlotId: String,
        sourceSlotId: String
    ) {
        val plan = weeklyPlanDao.getPlanById(planId)
            ?: throw IllegalStateException("Piano non trovato.")

        val slotEntities = weeklyPlanDao.getSlotsForPlan(plan.id)
        val consumptionEntities = weeklyPlanDao.getConsumptionsForPlan(plan.id)
        val assignmentEntities = weeklyPlanDao.getAssignmentsForPlan(plan.id)

        val planning = WeeklyPlanningCalculator.buildActiveWeekPlanning(
            slotEntities = slotEntities,
            actualConsumptions = consumptionEntities.filter { consumption ->
                WeeklyPlanningCalculator.isInCurrentWeek(consumption.consumedAtEpochMillis)
            },
            pendingAssignments = assignmentEntities.filter { assignment ->
                WeeklyPlanningCalculator.isInCurrentWeek(assignment.assignedAtEpochMillis)
            }
        )

        val targetSlot = slotEntities.firstOrNull { it.id == targetSlotId }
            ?: throw IllegalStateException("Slot target non trovato.")

        val sourceSlot = slotEntities.firstOrNull { it.id == sourceSlotId }
            ?: throw IllegalStateException("Slot sorgente non trovato.")

        validateReplacement(
            planning = planning,
            targetSlot = targetSlot,
            sourceSlot = sourceSlot,
            targetSlotId = targetSlotId,
            sourceSlotId = sourceSlotId
        )

        deleteCurrentAssignmentsForTarget(
            targetSlotId = targetSlotId,
            assignments = assignmentEntities
        )

        val newAssignment = MealAssignmentEntity(
            id = UUID.randomUUID().toString(),
            planId = plan.id,
            targetSlotId = targetSlot.id,
            sourceSlotId = sourceSlot.id,
            assignedAtEpochMillis = System.currentTimeMillis()
        )

        weeklyPlanDao.insertMealAssignments(assignments = listOf(newAssignment))
    }

    suspend fun assignCatalogOptionToSlot(
        planId: String,
        targetSlotId: String,
        optionId: String
    ) {
        val plan = weeklyPlanDao.getPlanById(planId)
            ?: throw IllegalStateException("Piano non trovato.")

        val slotEntities = weeklyPlanDao.getSlotsForPlan(plan.id)
        val consumptionEntities = weeklyPlanDao.getConsumptionsForPlan(plan.id)
        val assignmentEntities = weeklyPlanDao.getAssignmentsForPlan(plan.id)
        val optionEntities = weeklyPlanDao.getMealOptionsForPlan(plan.id)

        val planning = WeeklyPlanningCalculator.buildActiveWeekPlanning(
            slotEntities = slotEntities,
            actualConsumptions = consumptionEntities.filter { consumption ->
                WeeklyPlanningCalculator.isInCurrentWeek(consumption.consumedAtEpochMillis)
            },
            pendingAssignments = assignmentEntities.filter { assignment ->
                WeeklyPlanningCalculator.isInCurrentWeek(assignment.assignedAtEpochMillis)
            }
        )

        val targetSlot = slotEntities.firstOrNull { it.id == targetSlotId }
            ?: throw IllegalStateException("Slot target non trovato.")

        val selectedOption = optionEntities.firstOrNull { it.id == optionId }
            ?: throw IllegalStateException("Opzione extra non trovata.")

        validateCatalogOptionAssignment(
            planning = planning,
            targetSlot = targetSlot,
            selectedOption = selectedOption,
            targetSlotId = targetSlotId
        )

        deleteCurrentAssignmentsForTarget(
            targetSlotId = targetSlotId,
            assignments = assignmentEntities
        )

        val updatedTargetSlot = targetSlot.copy(
            plannedMealText = normalizeMealText(selectedOption.mealText)
        )

        weeklyPlanDao.insertMealSlots(
            slots = listOf(updatedTargetSlot)
        )
    }

    suspend fun recordMealConsumption(
        planId: String,
        targetSlotId: String,
        sourceSlotId: String
    ) {
        val plan = weeklyPlanDao.getPlanById(planId)
            ?: throw IllegalStateException("Piano non trovato.")

        val slotEntities = weeklyPlanDao.getSlotsForPlan(plan.id)
        val consumptionEntities = weeklyPlanDao.getConsumptionsForPlan(plan.id)
        val assignmentEntities = weeklyPlanDao.getAssignmentsForPlan(plan.id)

        val activeConsumptions = consumptionEntities.filter { consumption ->
            WeeklyPlanningCalculator.isInCurrentWeek(consumption.consumedAtEpochMillis)
        }
        val activeAssignments = assignmentEntities.filter { assignment ->
            WeeklyPlanningCalculator.isInCurrentWeek(assignment.assignedAtEpochMillis)
        }

        val planning = WeeklyPlanningCalculator.buildActiveWeekPlanning(
            slotEntities = slotEntities,
            actualConsumptions = activeConsumptions,
            pendingAssignments = activeAssignments
        )

        val targetSlot = slotEntities.firstOrNull { it.id == targetSlotId }
            ?: throw IllegalStateException("Slot target non trovato.")

        val sourceSlot = slotEntities.firstOrNull { it.id == sourceSlotId }
            ?: throw IllegalStateException("Slot sorgente non trovato.")

        if (planning.actualSourceByTarget.containsKey(targetSlotId)) {
            throw IllegalStateException("Questo slot risulta già completato nella settimana corrente.")
        }

        val pendingAssignmentForTarget = activeAssignments
            .sortedBy { it.assignedAtEpochMillis }
            .lastOrNull { it.targetSlotId == targetSlotId }

        val expectedSourceSlotId = pendingAssignmentForTarget?.sourceSlotId ?: targetSlotId
        if (expectedSourceSlotId != sourceSlotId) {
            throw IllegalStateException(
                "Lo slot ha un pasto assegnato diverso. Aggiorna prima la selezione del pasto."
            )
        }

        validateReplacement(
            planning = planning,
            targetSlot = targetSlot,
            sourceSlot = sourceSlot,
            targetSlotId = targetSlotId,
            sourceSlotId = sourceSlotId
        )

        deleteCurrentAssignmentsForTarget(
            targetSlotId = targetSlotId,
            assignments = activeAssignments
        )

        val newConsumption = MealConsumptionEntity(
            id = UUID.randomUUID().toString(),
            planId = plan.id,
            targetSlotId = targetSlot.id,
            sourceSlotId = sourceSlot.id,
            consumedAtEpochMillis = System.currentTimeMillis()
        )

        weeklyPlanDao.insertMealConsumptions(consumptions = listOf(newConsumption))
    }

    suspend fun getLatestWeeklyPlanSnapshot(): WeeklyPlanSnapshot? {
        val latestPlan = weeklyPlanDao.getLatestPlan() ?: return null
        return getWeeklyPlanSnapshot(latestPlan.id)
    }

    suspend fun getWeeklyPlanSnapshot(planId: String): WeeklyPlanSnapshot? {
        val planEntity = weeklyPlanDao.getPlanById(planId) ?: return null
        val slotEntities = weeklyPlanDao.getSlotsForPlan(planId)
        val consumptionEntities = weeklyPlanDao.getConsumptionsForPlan(planId)
        val assignmentEntities = weeklyPlanDao.getAssignmentsForPlan(planId)
        val optionEntities = weeklyPlanDao.getMealOptionsForPlan(planId)
        val ruleEntities = weeklyPlanDao.getMealRulesForPlan(planId)

        return WeeklyPlanSnapshot(
            plan = planEntity.toDomain(),
            slots = slotEntities.map { it.toDomain() }.sortedForWeeklyDisplay(),
            consumptions = consumptionEntities.map { it.toDomain() },
            assignments = assignmentEntities.map { it.toDomain() },
            mealOptions = optionEntities.map { it.toDomain() },
            mealRules = ruleEntities.map { it.toDomain() }
        )
    }

    private suspend fun deleteCurrentAssignmentsForTarget(
        targetSlotId: String,
        assignments: List<MealAssignmentEntity>
    ) {
        val currentTargetAssignmentIds = assignments
            .filter { it.targetSlotId == targetSlotId }
            .map { it.id }

        if (currentTargetAssignmentIds.isNotEmpty()) {
            weeklyPlanDao.deleteMealAssignmentsByIds(currentTargetAssignmentIds)
        }
    }

    private fun validateCatalogOptionAssignment(
        planning: it.lagioiaproductions.nutrislot.data.repository.planning.ActiveWeekPlanning,
        targetSlot: MealSlotEntity,
        selectedOption: MealOptionEntity,
        targetSlotId: String
    ) {
        if (planning.actualSourceByTarget.containsKey(targetSlotId)) {
            throw IllegalStateException("Questo slot risulta già completato nella settimana corrente.")
        }

        val targetType = MealSlotType.valueOf(targetSlot.mealSlotType)
        val optionType = MealSlotType.valueOf(selectedOption.mealSlotType)

        if (!areMealSlotTypesCompatible(targetType = targetType, sourceType = optionType)) {
            throw IllegalStateException(
                "Questa opzione extra non è compatibile con lo slot ${targetSlot.mealSlotType.lowercase()}."
            )
        }

        if (selectedOption.mealText.isBlank()) {
            throw IllegalStateException("L'opzione extra selezionata non contiene un pasto valido.")
        }
    }

    private fun validateReplacement(
        planning: it.lagioiaproductions.nutrislot.data.repository.planning.ActiveWeekPlanning,
        targetSlot: MealSlotEntity,
        sourceSlot: MealSlotEntity,
        targetSlotId: String,
        sourceSlotId: String
    ) {
        if (planning.actualSourceByTarget.containsKey(targetSlotId)) {
            throw IllegalStateException("Questo slot risulta già completato nella settimana corrente.")
        }

        if (sourceSlot.plannedMealText.isBlank()) {
            throw IllegalStateException("Lo slot sorgente non contiene un pasto disponibile.")
        }

        if (
            !areMealSlotTypesCompatible(
                targetType = MealSlotType.valueOf(targetSlot.mealSlotType),
                sourceType = MealSlotType.valueOf(sourceSlot.mealSlotType)
            )
        ) {
            throw IllegalStateException(
                "Sostituzione non consentita tra ${targetSlot.mealSlotType} e ${sourceSlot.mealSlotType}."
            )
        }

        val sourceUsedOrReservedByAnotherTarget = planning.usedSourceByTarget.any { usage ->
            usage.sourceSlotId == sourceSlotId && usage.targetSlotId != targetSlotId
        }

        if (sourceUsedOrReservedByAnotherTarget) {
            throw IllegalStateException(
                "Il pasto selezionato è già stato usato o assegnato in un altro slot della settimana."
            )
        }
    }
}