package it.lagioiaproductions.nutrislot.ui.shared

import org.junit.Assert.assertEquals
import org.junit.Test

class AppBridgeSupportTest {

    @Test
    fun splitMealText_ignoresNutritionFooterInSubtitle() {
        val mealText = """
            Pasta e ceci
            Olio evo

            Nutrienti: 520 kcal • 24 g proteine • 63 g carboidrati
        """.trimIndent()

        val result = AppBridgeSupport.splitMealText(mealText)

        assertEquals("Pasta e ceci", result.first)
        assertEquals("Olio evo", result.second)
    }

    @Test
    fun parseNutritionFromMealText_readsMergedNutritionFooter() {
        val parsed = AppBridgeSupport.parseNutritionFromMealText(
            "Pasta e ceci\n\nNutrienti: 520 kcal • 24 g proteine • 63 g carboidrati • 9 g fibre"
        )

        assertEquals(520, parsed.calories)
        assertEquals(24, parsed.protein)
        assertEquals(63, parsed.carbs)
        assertEquals(9, parsed.fibre)
    }
}
