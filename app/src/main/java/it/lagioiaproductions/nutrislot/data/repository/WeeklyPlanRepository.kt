package it.lagioiaproductions.nutrislot.data.repository

import it.lagioiaproductions.nutrislot.data.local.room.MealAssignmentEntity
import it.lagioiaproductions.nutrislot.data.local.room.MealConsumptionEntity
import it.lagioiaproductions.nutrislot.data.local.room.MealOptionEntity
import it.lagioiaproductions.nutrislot.data.local.room.MealRuleEntity
import it.lagioiaproductions.nutrislot.data.local.room.MealSlotEntity
import it.lagioiaproductions.nutrislot.data.local.room.WeeklyFrequencyTargetEntity
import it.lagioiaproductions.nutrislot.data.local.room.WeeklyPlanDao
import it.lagioiaproductions.nutrislot.data.local.room.WeeklyPlanEntity
import it.lagioiaproductions.nutrislot.data.repository.mapper.areMealSlotTypesCompatible
import it.lagioiaproductions.nutrislot.data.repository.mapper.normalizeMealText
import it.lagioiaproductions.nutrislot.data.repository.mapper.serializeStringList
import it.lagioiaproductions.nutrislot.data.repository.mapper.toDomain
import it.lagioiaproductions.nutrislot.data.repository.model.ReviewedImportedMealCell
import it.lagioiaproductions.nutrislot.data.repository.model.ReviewedImportedMealOption
import it.lagioiaproductions.nutrislot.data.repository.model.ReviewedImportedMealRule
import it.lagioiaproductions.nutrislot.data.repository.model.ReviewedImportedWeeklyFrequencyTarget
import it.lagioiaproductions.nutrislot.data.repository.planning.ActiveWeekPlanning
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
        mealRules: List<ReviewedImportedMealRule> = emptyList(),
        weeklyTargets: List<ReviewedImportedWeeklyFrequencyTarget> = emptyList()
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

        val weeklyTargetEntities = weeklyTargets.mapIndexed { index, target ->
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

        weeklyPlanDao.insertImportedPlan(
            plan = weeklyPlanEntity,
            slots = mealSlotEntities,
            options = optionEntities,
            rules = ruleEntities,
            weeklyTargets = weeklyTargetEntities
        )

        return planId
    }

    suspend fun undoMealConsumption(
        planId: String,
        targetSlotId: String
    ): String {
        val plan = weeklyPlanDao.getPlanById(planId)
            ?: throw IllegalStateException("Piano non trovato.")

        val consumptionEntities = weeklyPlanDao.getConsumptionsForPlan(plan.id)
        val assignmentEntities = weeklyPlanDao.getAssignmentsForPlan(plan.id)

        val activeConsumptions = consumptionEntities.filter { consumption ->
            WeeklyPlanningCalculator.isInCurrentWeek(consumption.consumedAtEpochMillis)
        }

        val latestConsumptionForTarget = activeConsumptions
            .filter { it.targetSlotId == targetSlotId }
            .maxByOrNull { it.consumedAtEpochMillis }
            ?: throw IllegalStateException("Nessun consumo da annullare per questo slot.")

        deleteCurrentAssignmentsForTargets(
            targetSlotIds = setOf(targetSlotId),
            assignments = assignmentEntities
        )

        weeklyPlanDao.deleteMealConsumptionsByIds(
            consumptionIds = listOf(latestConsumptionForTarget.id)
        )

        if (latestConsumptionForTarget.sourceSlotId != targetSlotId) {
            weeklyPlanDao.insertMealAssignments(
                assignments = listOf(
                    MealAssignmentEntity(
                        id = UUID.randomUUID().toString(),
                        planId = plan.id,
                        targetSlotId = latestConsumptionForTarget.targetSlotId,
                        sourceSlotId = latestConsumptionForTarget.sourceSlotId,
                        assignedAtEpochMillis = System.currentTimeMillis()
                    )
                )
            )
        }

        return latestConsumptionForTarget.id
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

        val activeAssignments = assignmentEntities.filter { assignment ->
            WeeklyPlanningCalculator.isInCurrentWeek(assignment.assignedAtEpochMillis)
        }
        val activeConsumptions = consumptionEntities.filter { consumption ->
            WeeklyPlanningCalculator.isInCurrentWeek(consumption.consumedAtEpochMillis)
        }

        val planning = WeeklyPlanningCalculator.buildActiveWeekPlanning(
            slotEntities = slotEntities,
            actualConsumptions = activeConsumptions,
            pendingAssignments = activeAssignments
        )

        val slotById = slotEntities.associateBy { it.id }

        val targetSlot = slotById[targetSlotId]
            ?: throw IllegalStateException("Slot target non trovato.")

        val sourceSlot = slotById[sourceSlotId]
            ?: throw IllegalStateException("Slot sorgente non trovato.")

        validateReplacement(
            planning = planning,
            targetSlot = targetSlot,
            sourceSlot = sourceSlot,
            targetSlotId = targetSlotId,
            sourceSlotId = sourceSlotId
        )

        val targetCurrentSourceSlotId = currentEffectiveSourceSlotId(
            planning = planning,
            targetSlotId = targetSlotId
        )
        val sourceCurrentSourceSlotId = currentEffectiveSourceSlotId(
            planning = planning,
            targetSlotId = sourceSlotId
        )

        val targetCurrentSourceSlot = slotById[targetCurrentSourceSlotId]
            ?: throw IllegalStateException("Pasto corrente del target non trovato.")
        val sourceCurrentSourceSlot = slotById[sourceCurrentSourceSlotId]
            ?: throw IllegalStateException("Pasto corrente della sorgente non trovato.")

        validateSwitchCompatibility(
            targetSlot = targetSlot,
            sourceSlot = sourceSlot,
            targetCurrentSourceSlot = targetCurrentSourceSlot,
            sourceCurrentSourceSlot = sourceCurrentSourceSlot
        )

        deleteCurrentAssignmentsForTargets(
            targetSlotIds = setOf(targetSlotId, sourceSlotId),
            assignments = assignmentEntities
        )

        val newAssignments = buildList {
            if (
                sourceCurrentSourceSlotId != targetSlotId &&
                sourceCurrentSourceSlot.plannedMealText.isNotBlank()
            ) {
                add(
                    MealAssignmentEntity(
                        id = UUID.randomUUID().toString(),
                        planId = plan.id,
                        targetSlotId = targetSlotId,
                        sourceSlotId = sourceCurrentSourceSlotId,
                        assignedAtEpochMillis = System.currentTimeMillis()
                    )
                )
            }

            if (
                targetCurrentSourceSlotId != sourceSlotId &&
                targetCurrentSourceSlot.plannedMealText.isNotBlank()
            ) {
                add(
                    MealAssignmentEntity(
                        id = UUID.randomUUID().toString(),
                        planId = plan.id,
                        targetSlotId = sourceSlotId,
                        sourceSlotId = targetCurrentSourceSlotId,
                        assignedAtEpochMillis = System.currentTimeMillis()
                    )
                )
            }
        }

        if (newAssignments.isNotEmpty()) {
            weeklyPlanDao.insertMealAssignments(assignments = newAssignments)
        }
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

        deleteCurrentAssignmentsForTargets(
            targetSlotIds = setOf(targetSlotId),
            assignments = assignmentEntities
        )

        val updatedTargetSlot = targetSlot.copy(
            plannedMealText = normalizeMealText(selectedOption.mealText)
        )

        weeklyPlanDao.insertMealSlots(
            slots = listOf(updatedTargetSlot)
        )
    }

    suspend fun updateSlotPlannedMealText(
        planId: String,
        slotId: String,
        mealText: String
    ) {
        val plan = weeklyPlanDao.getPlanById(planId)
            ?: throw IllegalStateException("Piano non trovato.")

        val targetSlot = weeklyPlanDao.getSlotsForPlan(plan.id)
            .firstOrNull { it.id == slotId }
            ?: throw IllegalStateException("Slot non trovato.")

        weeklyPlanDao.insertMealSlots(
            slots = listOf(
                targetSlot.copy(
                    plannedMealText = normalizeMealText(mealText)
                )
            )
        )
    }

    suspend fun recordMealConsumption(
        planId: String,
        targetSlotId: String,
        sourceSlotId: String
    ): MealConsumptionEntity {
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

        deleteCurrentAssignmentsForTargets(
            targetSlotIds = setOf(targetSlotId),
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
        return newConsumption
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
        val weeklyTargetEntities = weeklyPlanDao.getWeeklyFrequencyTargetsForPlan(planId)

        return WeeklyPlanSnapshot(
            plan = planEntity.toDomain(),
            slots = slotEntities.map { it.toDomain() }.sortedForWeeklyDisplay(),
            consumptions = consumptionEntities.map { it.toDomain() },
            assignments = assignmentEntities.map { it.toDomain() },
            mealOptions = optionEntities.map { it.toDomain() },
            mealRules = ruleEntities.map { it.toDomain() },
            weeklyTargets = weeklyTargetEntities.map { it.toDomain() }
        )
    }

    private fun currentEffectiveSourceSlotId(
        planning: ActiveWeekPlanning,
        targetSlotId: String
    ): String {
        return planning.pendingSourceByTarget[targetSlotId] ?: targetSlotId
    }

    private suspend fun deleteCurrentAssignmentsForTargets(
        targetSlotIds: Set<String>,
        assignments: List<MealAssignmentEntity>
    ) {
        val currentTargetAssignmentIds = assignments
            .filter { it.targetSlotId in targetSlotIds }
            .map { it.id }

        if (currentTargetAssignmentIds.isNotEmpty()) {
            weeklyPlanDao.deleteMealAssignmentsByIds(currentTargetAssignmentIds)
        }
    }

    private fun validateCatalogOptionAssignment(
        planning: ActiveWeekPlanning,
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

    private fun validateSwitchCompatibility(
        targetSlot: MealSlotEntity,
        sourceSlot: MealSlotEntity,
        targetCurrentSourceSlot: MealSlotEntity,
        sourceCurrentSourceSlot: MealSlotEntity
    ) {
        val targetType = MealSlotType.valueOf(targetSlot.mealSlotType)
        val sourceType = MealSlotType.valueOf(sourceSlot.mealSlotType)
        val targetCurrentSourceType = MealSlotType.valueOf(targetCurrentSourceSlot.mealSlotType)
        val sourceCurrentSourceType = MealSlotType.valueOf(sourceCurrentSourceSlot.mealSlotType)

        if (!areMealSlotTypesCompatible(targetType = targetType, sourceType = sourceCurrentSourceType)) {
            throw IllegalStateException("Il pasto selezionato non è compatibile con lo slot target.")
        }

        if (
            targetCurrentSourceSlot.plannedMealText.isNotBlank() &&
            !areMealSlotTypesCompatible(targetType = sourceType, sourceType = targetCurrentSourceType)
        ) {
            throw IllegalStateException("Lo switch non è compatibile con il tipo dello slot sorgente.")
        }
    }

    private fun validateReplacement(
        planning: ActiveWeekPlanning,
        targetSlot: MealSlotEntity,
        sourceSlot: MealSlotEntity,
        targetSlotId: String,
        sourceSlotId: String
    ) {
        if (planning.actualSourceByTarget.containsKey(targetSlotId)) {
            throw IllegalStateException("Questo slot risulta già completato nella settimana corrente.")
        }

        if (planning.actualSourceByTarget.containsKey(sourceSlotId)) {
            throw IllegalStateException("Il pasto selezionato è già stato consumato nella settimana corrente.")
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
    }
}
