package it.lagioiaproductions.nutrislot.data.repository

import it.lagioiaproductions.nutrislot.data.local.room.MealAssignmentEntity
import it.lagioiaproductions.nutrislot.data.local.room.MealConsumptionEntity
import it.lagioiaproductions.nutrislot.data.local.room.MealSlotEntity
import it.lagioiaproductions.nutrislot.data.local.room.WeeklyPlanDao
import it.lagioiaproductions.nutrislot.data.local.room.WeeklyPlanEntity
import it.lagioiaproductions.nutrislot.domain.model.MealAssignment
import it.lagioiaproductions.nutrislot.domain.model.MealConsumption
import it.lagioiaproductions.nutrislot.domain.model.MealSlot
import it.lagioiaproductions.nutrislot.domain.model.MealSlotType
import it.lagioiaproductions.nutrislot.domain.model.WeekDay
import it.lagioiaproductions.nutrislot.domain.model.WeeklyPlan
import it.lagioiaproductions.nutrislot.domain.model.WeeklyPlanSnapshot
import it.lagioiaproductions.nutrislot.domain.model.sortedForWeeklyDisplay
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import java.util.UUID

data class ReviewedImportedMealCell(
    val dayOfWeek: WeekDay,
    val mealSlotType: MealSlotType,
    val mealText: String
)

class WeeklyPlanRepository(
    private val weeklyPlanDao: WeeklyPlanDao
) {

    @Suppress("unused")
    suspend fun saveReviewedImport(
        sourceFileName: String?,
        cells: List<ReviewedImportedMealCell>
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

        weeklyPlanDao.insertImportedPlan(
            plan = weeklyPlanEntity,
            slots = mealSlotEntities
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

        val activeWeekConsumptions = consumptionEntities.filter { consumption ->
            isInCurrentWeek(consumption.consumedAtEpochMillis)
        }
        val activeWeekAssignments = assignmentEntities.filter { assignment ->
            isInCurrentWeek(assignment.assignedAtEpochMillis)
        }

        val planning = buildActiveWeekPlanning(
            slotEntities = slotEntities,
            actualConsumptions = activeWeekConsumptions,
            pendingAssignments = activeWeekAssignments
        )

        val targetSlot = slotEntities.firstOrNull { it.id == targetSlotId }
            ?: throw IllegalStateException("Slot target non trovato.")

        val sourceSlot = slotEntities.firstOrNull { it.id == sourceSlotId }
            ?: throw IllegalStateException("Slot sorgente non trovato.")

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
            usage.sourceSlotId == sourceSlotId &&
                    usage.targetSlotId != targetSlotId
        }

        if (sourceUsedOrReservedByAnotherTarget) {
            throw IllegalStateException(
                "Il pasto selezionato è già stato usato o assegnato in un altro slot della settimana."
            )
        }

        val currentTargetAssignmentIds = activeWeekAssignments
            .filter { it.targetSlotId == targetSlotId }
            .map { it.id }

        if (currentTargetAssignmentIds.isNotEmpty()) {
            weeklyPlanDao.deleteMealAssignmentsByIds(currentTargetAssignmentIds)
        }

        val newAssignment = MealAssignmentEntity(
            id = UUID.randomUUID().toString(),
            planId = plan.id,
            targetSlotId = targetSlot.id,
            sourceSlotId = sourceSlot.id,
            assignedAtEpochMillis = System.currentTimeMillis()
        )

        weeklyPlanDao.insertMealAssignments(
            assignments = listOf(newAssignment)
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

        val activeWeekConsumptions = consumptionEntities.filter { consumption ->
            isInCurrentWeek(consumption.consumedAtEpochMillis)
        }
        val activeWeekAssignments = assignmentEntities.filter { assignment ->
            isInCurrentWeek(assignment.assignedAtEpochMillis)
        }

        val planning = buildActiveWeekPlanning(
            slotEntities = slotEntities,
            actualConsumptions = activeWeekConsumptions,
            pendingAssignments = activeWeekAssignments
        )

        val targetSlot = slotEntities.firstOrNull { it.id == targetSlotId }
            ?: throw IllegalStateException("Slot target non trovato.")

        val sourceSlot = slotEntities.firstOrNull { it.id == sourceSlotId }
            ?: throw IllegalStateException("Slot sorgente non trovato.")

        if (planning.actualSourceByTarget.containsKey(targetSlotId)) {
            throw IllegalStateException("Questo slot risulta già completato nella settimana corrente.")
        }

        val pendingAssignmentForTarget = activeWeekAssignments
            .sortedBy { it.assignedAtEpochMillis }
            .lastOrNull { it.targetSlotId == targetSlotId }

        val expectedSourceSlotId = pendingAssignmentForTarget?.sourceSlotId ?: targetSlotId
        if (expectedSourceSlotId != sourceSlotId) {
            throw IllegalStateException(
                "Lo slot ha un pasto assegnato diverso. Aggiorna prima la selezione del pasto."
            )
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
            usage.sourceSlotId == sourceSlotId &&
                    usage.targetSlotId != targetSlotId
        }

        if (sourceUsedOrReservedByAnotherTarget) {
            throw IllegalStateException(
                "Il pasto selezionato è già stato usato o assegnato in un altro slot della settimana."
            )
        }

        val currentTargetAssignmentIds = activeWeekAssignments
            .filter { it.targetSlotId == targetSlotId }
            .map { it.id }

        if (currentTargetAssignmentIds.isNotEmpty()) {
            weeklyPlanDao.deleteMealAssignmentsByIds(currentTargetAssignmentIds)
        }

        val newConsumption = MealConsumptionEntity(
            id = UUID.randomUUID().toString(),
            planId = plan.id,
            targetSlotId = targetSlot.id,
            sourceSlotId = sourceSlot.id,
            consumedAtEpochMillis = System.currentTimeMillis()
        )

        weeklyPlanDao.insertMealConsumptions(
            consumptions = listOf(newConsumption)
        )
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

        return WeeklyPlanSnapshot(
            plan = planEntity.toDomain(),
            slots = slotEntities.map { it.toDomain() }.sortedForWeeklyDisplay(),
            consumptions = consumptionEntities.map { it.toDomain() },
            assignments = assignmentEntities.map { it.toDomain() }
        )
    }

    private fun WeeklyPlanEntity.toDomain(): WeeklyPlan {
        return WeeklyPlan(
            id = id,
            title = title,
            sourceFileName = sourceFileName,
            createdAtEpochMillis = createdAtEpochMillis
        )
    }

    private fun MealSlotEntity.toDomain(): MealSlot {
        return MealSlot(
            id = id,
            planId = planId,
            dayOfWeek = WeekDay.valueOf(dayOfWeek),
            mealSlotType = MealSlotType.valueOf(mealSlotType),
            plannedMealText = plannedMealText
        )
    }

    private fun MealConsumptionEntity.toDomain(): MealConsumption {
        return MealConsumption(
            id = id,
            planId = planId,
            targetSlotId = targetSlotId,
            sourceSlotId = sourceSlotId,
            consumedAtEpochMillis = consumedAtEpochMillis
        )
    }

    private fun MealAssignmentEntity.toDomain(): MealAssignment {
        return MealAssignment(
            id = id,
            planId = planId,
            targetSlotId = targetSlotId,
            sourceSlotId = sourceSlotId,
            assignedAtEpochMillis = assignedAtEpochMillis
        )
    }

    private fun normalizeMealText(text: String): String {
        return text
            .replace("\r\n", "\n")
            .replace("\r", "\n")
            .lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .joinToString(separator = "\n")
    }

    private fun areMealSlotTypesCompatible(
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

    private fun isInCurrentWeek(epochMillis: Long): Boolean {
        val zoneId = ZoneId.systemDefault()
        val date = Instant.ofEpochMilli(epochMillis).atZone(zoneId).toLocalDate()
        val today = LocalDate.now(zoneId)
        val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val nextWeekStart = weekStart.plusWeeks(1)

        return !date.isBefore(weekStart) && date.isBefore(nextWeekStart)
    }

    private fun buildActiveWeekPlanning(
        slotEntities: List<MealSlotEntity>,
        actualConsumptions: List<MealConsumptionEntity>,
        pendingAssignments: List<MealAssignmentEntity>
    ): ActiveWeekPlanning {
        val actualSourceByTarget = linkedMapOf<String, String>()
        actualConsumptions
            .sortedBy { it.consumedAtEpochMillis }
            .forEach { consumption ->
                actualSourceByTarget[consumption.targetSlotId] = consumption.sourceSlotId
            }

        val pendingSourceByTarget = linkedMapOf<String, String>()
        pendingAssignments
            .sortedBy { it.assignedAtEpochMillis }
            .forEach { assignment ->
                if (actualSourceByTarget.containsKey(assignment.targetSlotId)) {
                    return@forEach
                }

                val sourceSlot = slotEntities.firstOrNull { it.id == assignment.sourceSlotId }
                    ?: return@forEach

                if (sourceSlot.plannedMealText.isBlank()) {
                    return@forEach
                }

                pendingSourceByTarget[assignment.targetSlotId] = assignment.sourceSlotId
            }

        val usedSourceByTarget = buildList {
            actualSourceByTarget.forEach { (targetSlotId, sourceSlotId) ->
                add(
                    SourceUsage(
                        targetSlotId = targetSlotId,
                        sourceSlotId = sourceSlotId
                    )
                )
            }

            pendingSourceByTarget.forEach { (targetSlotId, sourceSlotId) ->
                add(
                    SourceUsage(
                        targetSlotId = targetSlotId,
                        sourceSlotId = sourceSlotId
                    )
                )
            }
        }

        return ActiveWeekPlanning(
            actualSourceByTarget = actualSourceByTarget,
            pendingSourceByTarget = pendingSourceByTarget,
            usedSourceByTarget = usedSourceByTarget
        )
    }
}

private data class SourceUsage(
    val targetSlotId: String,
    val sourceSlotId: String
)

private data class ActiveWeekPlanning(
    val actualSourceByTarget: Map<String, String>,
    val pendingSourceByTarget: Map<String, String>,
    val usedSourceByTarget: List<SourceUsage>
)