package it.lagioiaproductions.nutrislot.data.repository.model

import it.lagioiaproductions.nutrislot.domain.model.ImportedMealNutrition
import it.lagioiaproductions.nutrislot.domain.model.MealOptionSourceType
import it.lagioiaproductions.nutrislot.domain.model.MealSlotType
import it.lagioiaproductions.nutrislot.domain.model.WeekDay

data class ReviewedImportedMealCell(
    val dayOfWeek: WeekDay,
    val mealSlotType: MealSlotType,
    val mealText: String,
    val nutrition: ImportedMealNutrition? = null
)

data class ReviewedImportedMealOption(
    val mealSlotType: MealSlotType,
    val title: String?,
    val mealText: String,
    val sourceType: MealOptionSourceType,
    val tags: List<String>,
    val pageNumber: Int?,
    val nutrition: ImportedMealNutrition? = null
)

data class ReviewedImportedMealRule(
    val mealSlotType: MealSlotType,
    val label: String,
    val requiredComponents: List<String>,
    val pageNumber: Int?
)

data class ReviewedImportedWeeklyFrequencyTarget(
    val title: String,
    val canonicalKey: String,
    val portionText: String?,
    val minimumTimesPerWeek: Int?,
    val maximumTimesPerWeek: Int?,
    val matchTerms: List<String>,
    val pageNumber: Int?,
    val sourceText: String?
)
