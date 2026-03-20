package it.lagioiaproductions.nutrislot.domain.model

data class ImportedPlanDraft(
    val sourceFileName: String,
    val rawExtractedText: String?,
    val cells: List<ImportedMealCell>,
    val additionalOptions: List<ImportedMealOption> = emptyList(),
    val mealRules: List<ImportedMealRule> = emptyList(),
    val warnings: List<ImportWarning>,
    val status: ImportStatus
)

data class ImportedMealNutrition(
    val calories: Int? = null,
    val proteinGrams: Int? = null,
    val carbsGrams: Int? = null,
    val fibreGrams: Int? = null
) {
    val hasAnyValue: Boolean
        get() = calories != null ||
                proteinGrams != null ||
                carbsGrams != null ||
                fibreGrams != null
}

data class ImportedMealCell(
    val id: String,
    val dayOfWeek: WeekDay?,
    val mealSlotType: MealSlotType?,
    val rawText: String,
    val normalizedText: String,
    val recognitionState: CellRecognitionState,
    val nutrition: ImportedMealNutrition? = null
)

data class ImportedMealOption(
    val id: String,
    val mealSlotType: MealSlotType,
    val title: String?,
    val rawText: String,
    val normalizedText: String,
    val sourceType: MealOptionSourceType,
    val tags: List<String> = emptyList(),
    val pageNumber: Int? = null,
    val nutrition: ImportedMealNutrition? = null
)

data class ImportedMealRule(
    val id: String,
    val mealSlotType: MealSlotType,
    val label: String,
    val requiredComponents: List<String>,
    val pageNumber: Int? = null
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