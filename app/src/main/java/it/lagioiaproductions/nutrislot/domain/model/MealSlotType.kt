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