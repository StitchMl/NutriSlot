package it.lagioiaproductions.nutrislot.domain.model

data class WeeklyPlan(
    val id: String,
    val title: String?,
    val sourceFileName: String?,
    val createdAtEpochMillis: Long
)

data class MealSlot(
    val id: String,
    val planId: String,
    val dayOfWeek: WeekDay,
    val mealSlotType: MealSlotType,
    val plannedMealText: String
)

data class MealConsumption(
    val id: String,
    val planId: String,
    val targetSlotId: String,
    val sourceSlotId: String,
    val consumedAtEpochMillis: Long
)

data class MealAssignment(
    val id: String,
    val planId: String,
    val targetSlotId: String,
    val sourceSlotId: String,
    val assignedAtEpochMillis: Long
)

data class ImportedPlanDraft(
    val sourceFileName: String,
    val rawExtractedText: String?,
    val cells: List<ImportedMealCell>,
    val warnings: List<ImportWarning>,
    val status: ImportStatus
)

data class ImportedMealCell(
    val id: String,
    val dayOfWeek: WeekDay?,
    val mealSlotType: MealSlotType?,
    val rawText: String,
    val normalizedText: String,
    val recognitionState: CellRecognitionState
)

data class ImportWarning(
    val message: String
)

enum class ImportStatus {
    SUCCESS,
    PARTIAL,
    UNSUPPORTED,
    FAILED
}

enum class CellRecognitionState {
    RECOGNIZED,
    SUSPECTED,
    MISSING_DAY,
    MISSING_MEAL_SLOT,
    EMPTY
}

sealed interface SlotDisplayState {

    data object Empty : SlotDisplayState

    data object PlannedAvailable : SlotDisplayState

    data object ConsumedAsPlanned : SlotDisplayState

    data class ConsumedWithReplacement(
        val sourceSlotId: String
    ) : SlotDisplayState

    data object OriginalMealAlreadyUsedElsewhere : SlotDisplayState
}

data class WeeklyPlanSnapshot(
    val plan: WeeklyPlan,
    val slots: List<MealSlot>,
    val consumptions: List<MealConsumption>,
    val assignments: List<MealAssignment>
)

@Suppress("unused")
data class SlotPresentation(
    val slot: MealSlot,
    val displayState: SlotDisplayState,
    val coveringConsumption: MealConsumption?
)

fun List<MealSlot>.sortedForWeeklyDisplay(): List<MealSlot> {
    return sortedWith(
        compareBy(
            { it.dayOfWeek.sortOrder },
            { it.mealSlotType.sortOrder }
        )
    )
}