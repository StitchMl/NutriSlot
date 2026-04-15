package it.lagioiaproductions.nutrislot.data.repository

import androidx.room.withTransaction
import it.lagioiaproductions.nutrislot.data.local.room.MealAssignmentEntity
import it.lagioiaproductions.nutrislot.data.local.room.MealConsumptionEntity
import it.lagioiaproductions.nutrislot.data.local.room.NutriSlotDatabase
import it.lagioiaproductions.nutrislot.data.local.room.WeeklyPlanDao
import it.lagioiaproductions.nutrislot.data.repository.mapper.normalizeMealText
import it.lagioiaproductions.nutrislot.data.repository.mapper.serializeStringList
import it.lagioiaproductions.nutrislot.data.repository.mapper.toDomain
import it.lagioiaproductions.nutrislot.data.repository.model.ReviewedImportedMealCell
import it.lagioiaproductions.nutrislot.data.repository.model.ReviewedImportedMealOption
import it.lagioiaproductions.nutrislot.data.repository.model.ReviewedImportedMealRule
import it.lagioiaproductions.nutrislot.data.repository.model.ReviewedImportedWeeklyFrequencyTarget
import it.lagioiaproductions.nutrislot.data.repository.planning.WeeklyPlanningCalculator
import it.lagioiaproductions.nutrislot.domain.model.MealConsumptionTargetSource
import it.lagioiaproductions.nutrislot.domain.model.WeeklyPlanSnapshot
import it.lagioiaproductions.nutrislot.domain.model.sortedForWeeklyDisplay
import java.util.UUID

/**
 * Main data gateway for weekly plan mutations and snapshot reads.
 *
 * The repository now focuses on transaction orchestration while validation and
 * assignment-specific helpers live in dedicated collaborators.
 */
class WeeklyPlanRepository(
    private val database: NutriSlotDatabase,
    private val weeklyPlanDao: WeeklyPlanDao = database.weeklyPlanDao()
) {
    private val assignmentSupport = WeeklyPlanAssignmentSupport(weeklyPlanDao)
    private val mutationValidator = WeeklyPlanMutationValidator()

    /**
     * Persists a reviewed import and reuses the latest plan record when possible.
     */
    suspend fun saveReviewedImport(
        sourceFileName: String?,
        cells: List<ReviewedImportedMealCell>,
        extraOptions: List<ReviewedImportedMealOption> = emptyList(),
        mealRules: List<ReviewedImportedMealRule> = emptyList(),
        weeklyTargets: List<ReviewedImportedWeeklyFrequencyTarget> = emptyList()
    ): String = database.withTransaction {
        val latestPlanId = weeklyPlanDao.getLatestPlan()?.id
        val payload = buildImportedPlanPersistencePayload(
            existingPlanId = latestPlanId,
            sourceFileName = sourceFileName,
            cells = cells,
            extraOptions = extraOptions,
            mealRules = mealRules,
            weeklyTargets = weeklyTargets
        )

        if (payload.reusedExistingPlanId) {
            weeklyPlanDao.deleteMealOptionsForPlan(payload.plan.id)
            weeklyPlanDao.deleteMealRulesForPlan(payload.plan.id)
            weeklyPlanDao.deleteWeeklyFrequencyTargetsForPlan(payload.plan.id)
        }

        weeklyPlanDao.insertImportedPlan(
            plan = payload.plan,
            slots = payload.slots,
            options = payload.options,
            rules = payload.rules,
            weeklyTargets = payload.weeklyTargets
        )

        payload.plan.id
    }

    /**
     * Reverts the latest consumption registered for the given target slot in the active week.
     */
    suspend fun undoMealConsumption(
        planId: String,
        targetSlotId: String
    ): String = database.withTransaction {
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

        val activeAssignments = assignmentEntities.filter { assignment ->
            WeeklyPlanningCalculator.isInCurrentWeek(assignment.assignedAtEpochMillis)
        }

        assignmentSupport.deleteCurrentAssignmentsForTargets(
            targetSlotIds = setOf(targetSlotId),
            assignments = activeAssignments
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

        latestConsumptionForTarget.id
    }

    /**
     * Swaps or reassigns meals between two slots inside the active week.
     */
    suspend fun assignMealToSlot(
        planId: String,
        targetSlotId: String,
        sourceSlotId: String
    ) = database.withTransaction {
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

        mutationValidator.validateReplacement(
            planning = planning,
            targetSlot = targetSlot,
            sourceSlot = sourceSlot,
            targetSlotId = targetSlotId,
            sourceSlotId = sourceSlotId
        )

        val targetCurrentSourceSlotId = assignmentSupport.currentEffectiveSourceSlotId(
            planning = planning,
            targetSlotId = targetSlotId
        )
        val sourceCurrentSourceSlotId = assignmentSupport.currentEffectiveSourceSlotId(
            planning = planning,
            targetSlotId = sourceSlotId
        )

        val targetCurrentSourceSlot = slotById[targetCurrentSourceSlotId]
            ?: throw IllegalStateException("Pasto corrente del target non trovato.")
        val sourceCurrentSourceSlot = slotById[sourceCurrentSourceSlotId]
            ?: throw IllegalStateException("Pasto corrente della sorgente non trovato.")

        mutationValidator.validateSwitchCompatibility(
            targetSlot = targetSlot,
            sourceSlot = sourceSlot,
            targetCurrentSourceSlot = targetCurrentSourceSlot,
            sourceCurrentSourceSlot = sourceCurrentSourceSlot
        )

        assignmentSupport.deleteCurrentAssignmentsForTargets(
            targetSlotIds = setOf(targetSlotId, sourceSlotId),
            assignments = activeAssignments
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

    /**
     * Applies a catalog option directly to a slot when the choice is compatible with its type.
     */
    suspend fun assignCatalogOptionToSlot(
        planId: String,
        targetSlotId: String,
        optionId: String
    ) = database.withTransaction {
        val plan = weeklyPlanDao.getPlanById(planId)
            ?: throw IllegalStateException("Piano non trovato.")

        val slotEntities = weeklyPlanDao.getSlotsForPlan(plan.id)
        val consumptionEntities = weeklyPlanDao.getConsumptionsForPlan(plan.id)
        val assignmentEntities = weeklyPlanDao.getAssignmentsForPlan(plan.id)
        val optionEntities = weeklyPlanDao.getMealOptionsForPlan(plan.id)
        val activeAssignments = assignmentEntities.filter { assignment ->
            WeeklyPlanningCalculator.isInCurrentWeek(assignment.assignedAtEpochMillis)
        }

        val planning = WeeklyPlanningCalculator.buildActiveWeekPlanning(
            slotEntities = slotEntities,
            actualConsumptions = consumptionEntities.filter { consumption ->
                WeeklyPlanningCalculator.isInCurrentWeek(consumption.consumedAtEpochMillis)
            },
            pendingAssignments = activeAssignments
        )

        val targetSlot = slotEntities.firstOrNull { it.id == targetSlotId }
            ?: throw IllegalStateException("Slot target non trovato.")

        val selectedOption = optionEntities.firstOrNull { it.id == optionId }
            ?: throw IllegalStateException("Opzione extra non trovata.")

        mutationValidator.validateCatalogOptionAssignment(
            planning = planning,
            targetSlot = targetSlot,
            selectedOption = selectedOption,
            targetSlotId = targetSlotId
        )

        assignmentSupport.deleteCurrentAssignmentsForTargets(
            targetSlotIds = setOf(targetSlotId),
            assignments = activeAssignments
        )

        val updatedTargetSlot = targetSlot.copy(
            plannedMealText = normalizeMealText(selectedOption.mealText),
            consumptionTargetKeysSerialized = "",
            consumptionTargetSource = null
        )

        weeklyPlanDao.insertMealSlots(slots = listOf(updatedTargetSlot))
    }

    /**
     * Updates the base planned meal text and stored target metadata for a slot.
     */
    suspend fun updateSlotPlannedMealText(
        planId: String,
        slotId: String,
        mealText: String,
        consumptionTargetCanonicalKeys: List<String>,
        consumptionTargetSource: MealConsumptionTargetSource?
    ) = database.withTransaction {
        val plan = weeklyPlanDao.getPlanById(planId)
            ?: throw IllegalStateException("Piano non trovato.")

        val targetSlot = weeklyPlanDao.getSlotsForPlan(plan.id)
            .firstOrNull { it.id == slotId }
            ?: throw IllegalStateException("Slot non trovato.")

        weeklyPlanDao.insertMealSlots(
            slots = listOf(
                targetSlot.copy(
                    plannedMealText = normalizeMealText(mealText),
                    consumptionTargetKeysSerialized = serializeStringList(consumptionTargetCanonicalKeys),
                    consumptionTargetSource = consumptionTargetSource?.name
                )
            )
        )
    }

    /**
     * Records a consumption event for the active week after validating compatibility and availability.
     */
    suspend fun recordMealConsumption(
        planId: String,
        targetSlotId: String,
        sourceSlotId: String,
        usesCustomizedTargetMeal: Boolean = false
    ): MealConsumptionEntity = database.withTransaction {
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
            throw IllegalStateException("Questo slot risulta gia completato nella settimana corrente.")
        }

        val consumesCustomizedTargetMeal = usesCustomizedTargetMeal && sourceSlotId == targetSlotId

        val pendingAssignmentForTarget = activeAssignments
            .sortedBy { it.assignedAtEpochMillis }
            .lastOrNull { it.targetSlotId == targetSlotId }

        val expectedSourceSlotId = pendingAssignmentForTarget?.sourceSlotId ?: targetSlotId
        if (!consumesCustomizedTargetMeal && expectedSourceSlotId != sourceSlotId) {
            throw IllegalStateException(
                "Lo slot ha un pasto assegnato diverso. Aggiorna prima la selezione del pasto."
            )
        }

        mutationValidator.validateReplacement(
            planning = planning,
            targetSlot = targetSlot,
            sourceSlot = sourceSlot,
            targetSlotId = targetSlotId,
            sourceSlotId = sourceSlotId,
            allowsCustomizedTargetMeal = consumesCustomizedTargetMeal
        )

        assignmentSupport.deleteCurrentAssignmentsForTargets(
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
        newConsumption
    }

    /**
     * Reads the latest plan snapshot if one exists.
     */
    suspend fun getLatestWeeklyPlanSnapshot(): WeeklyPlanSnapshot? {
        val latestPlan = weeklyPlanDao.getLatestPlan() ?: return null
        return getWeeklyPlanSnapshot(latestPlan.id)
    }

    /**
     * Rehydrates a complete domain snapshot for the requested plan id.
     */
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
}
