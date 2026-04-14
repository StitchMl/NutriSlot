package it.lagioiaproductions.nutrislot.ui.weeklyplan

import it.lagioiaproductions.nutrislot.domain.model.ImportedMealNutrition
import it.lagioiaproductions.nutrislot.ui.weeklyplan.edit.extractStoredNutritionSummary
import it.lagioiaproductions.nutrislot.ui.weeklyplan.edit.mergeMealTextWithNutritionSummary
import it.lagioiaproductions.nutrislot.ui.weeklyplan.edit.stripStoredMealNutrition
import it.lagioiaproductions.nutrislot.ui.weeklyplan.edit.toNutritionSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WeeklyPlanNutritionTextSupportTest {

    @Test
    fun mergeMealTextWithNutritionSummary_appendsReadableFooter() {
        val storedText = mergeMealTextWithNutritionSummary(
            mealText = "Pasta e ceci",
            nutritionSummary = "420 kcal\n25 g proteine"
        )

        assertEquals(
            "Pasta e ceci\n\nNutrienti: 420 kcal • 25 g proteine",
            storedText
        )
        assertEquals("420 kcal • 25 g proteine", extractStoredNutritionSummary(storedText))
    }

    @Test
    fun stripStoredMealNutrition_removesExistingFooter() {
        val baseText = stripStoredMealNutrition(
            "Riso basmati\nPollo alla piastra\n\nNutrienti: 560 kcal • 38 g proteine"
        )

        assertEquals("Riso basmati\nPollo alla piastra", baseText)
    }

    @Test
    fun toNutritionSummary_formatsOnlyAvailableValues() {
        val summary = ImportedMealNutrition(
            calories = 510,
            proteinGrams = 34,
            carbsGrams = null,
            fibreGrams = 7
        ).toNutritionSummary()

        assertEquals("510 kcal • 34 g proteine • 7 g fibre", summary)
        assertNull(ImportedMealNutrition().toNutritionSummary())
    }
}
