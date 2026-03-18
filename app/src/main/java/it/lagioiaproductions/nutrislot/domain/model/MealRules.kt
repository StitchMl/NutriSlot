package it.lagioiaproductions.nutrislot.domain.model

enum class MealOptionSourceType {
    WEEKLY_APPENDIX,
    BREAKFAST_ALTERNATIVE,
    SNACK_ALTERNATIVE,
    PRE_WORKOUT,
    LUNCH_DINNER_ALTERNATIVE,
    FUORI_CASA,
    OTHER
}

data class MealOption(
    val id: String,
    val planId: String,
    val mealSlotType: MealSlotType,
    val title: String?,
    val mealText: String,
    val sourceType: MealOptionSourceType,
    val tags: List<String>,
    val pageNumber: Int?
)

data class MealRule(
    val id: String,
    val planId: String,
    val mealSlotType: MealSlotType,
    val label: String,
    val requiredComponents: List<String>,
    val pageNumber: Int?
)