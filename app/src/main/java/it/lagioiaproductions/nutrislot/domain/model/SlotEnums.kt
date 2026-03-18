package it.lagioiaproductions.nutrislot.domain.model

enum class MealSlotType(
    val sortOrder: Int,
    val displayName: String
) {
    BREAKFAST(
        sortOrder = 0,
        displayName = "Colazione"
    ),
    MORNING_SNACK(
        sortOrder = 1,
        displayName = "Spuntino mattina"
    ),
    LUNCH(
        sortOrder = 2,
        displayName = "Pranzo"
    ),
    AFTERNOON_SNACK(
        sortOrder = 3,
        displayName = "Spuntino pomeridiano"
    ),
    DINNER(
        sortOrder = 4,
        displayName = "Cena"
    );
    companion object {
        fun orderedValues(): List<MealSlotType> = entries.sortedBy { it.sortOrder }
    }
}

enum class WeekDay(
    val sortOrder: Int,
    val displayName: String
) {
    MONDAY(
        sortOrder = 0,
        displayName = "Lunedì"
    ),
    TUESDAY(
        sortOrder = 1,
        displayName = "Martedì"
    ),
    WEDNESDAY(
        sortOrder = 2,
        displayName = "Mercoledì"
    ),
    THURSDAY(
        sortOrder = 3,
        displayName = "Giovedì"
    ),
    FRIDAY(
        sortOrder = 4,
        displayName = "Venerdì"
    ),
    SATURDAY(
        sortOrder = 5,
        displayName = "Sabato"
    ),
    SUNDAY(
        sortOrder = 6,
        displayName = "Domenica"
    );
    companion object {
        fun orderedValues(): List<WeekDay> = entries.sortedBy { it.sortOrder }
    }
}
enum class CellRecognitionState {
    RECOGNIZED,
    SUSPECTED,
    MISSING_DAY,
    MISSING_MEAL_SLOT,
    EMPTY
}