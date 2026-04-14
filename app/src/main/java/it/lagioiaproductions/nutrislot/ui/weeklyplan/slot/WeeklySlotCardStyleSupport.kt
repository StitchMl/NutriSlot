package it.lagioiaproductions.nutrislot.ui.weeklyplan.slot

import androidx.compose.ui.graphics.Color
import it.lagioiaproductions.nutrislot.domain.model.MealSlotType
import it.lagioiaproductions.nutrislot.domain.model.SlotDisplayState
import it.lagioiaproductions.nutrislot.ui.weeklyplan.parser.MealVisualInfo

internal data class FoodVisualStyle(
    val container: Color,
    val border: Color,
    val accent: Color,
    val title: Color,
    val body: Color,
    val meta: Color
)

internal fun foodVisualStyleForMeal(
    visualInfo: MealVisualInfo?,
    slotType: MealSlotType,
    displayState: SlotDisplayState,
    isCompleted: Boolean
): FoodVisualStyle {
    val base = when (visualInfo?.semanticKey) {
        "panino" -> baseFoodStyle(Color(0xFFF3E0C5), Color(0xFFC78A48), Color(0xFFC78A48))
        "piadina" -> baseFoodStyle(Color(0xFFFFDFC9), Color(0xFFE58D5E), Color(0xFFE58D5E))
        "frisella" -> baseFoodStyle(Color(0xFFF0E3CE), Color(0xFFB98A52), Color(0xFFB98A52))
        "insalata", "verdura", "avocado" -> baseFoodStyle(Color(0xFFD5F5D9), Color(0xFF54B868), Color(0xFF54B868))
        "cereale_primo" -> baseFoodStyle(Color(0xFFFFD8B0), Color(0xFFF08A24), Color(0xFFF08A24))
        "carne" -> baseFoodStyle(Color(0xFFFFCDC7), Color(0xFFE96A5F), Color(0xFFE96A5F))
        "pesce" -> baseFoodStyle(Color(0xFFCDEBFF), Color(0xFF4DA3FF), Color(0xFF4DA3FF))
        "uova" -> baseFoodStyle(Color(0xFFFFF2BA), Color(0xFFE0B400), Color(0xFFE0B400))
        "latticino", "formaggio" -> baseFoodStyle(Color(0xFFE1F0FF), Color(0xFF6BA4FF), Color(0xFF6BA4FF))
        "frutta", "banana", "mela", "pera" -> baseFoodStyle(Color(0xFFFFD4E1), Color(0xFFFF5C8A), Color(0xFFFF5C8A))
        "pane" -> baseFoodStyle(Color(0xFFF2DFC7), Color(0xFFC78A48), Color(0xFFC78A48))
        "colazione_secca", "pancake", "dolce_spalmabile", "caffe" -> baseFoodStyle(Color(0xFFFFE6D5), Color(0xFFFFA36C), Color(0xFFFFA36C))
        "olio" -> baseFoodStyle(Color(0xFFE6F3C8), Color(0xFF97B63E), Color(0xFF97B63E))
        else -> fallbackStyleForSlot(slotType)
    }

    if (displayState == SlotDisplayState.OriginalMealAlreadyUsedElsewhere) {
        return FoodVisualStyle(
            container = Color(0xFFE9E6EC),
            border = Color(0xFFAAA2B1),
            accent = Color(0xFFAAA2B1),
            title = Color(0xFF3A3440),
            body = Color(0xFF4A4351),
            meta = Color(0xFF6A6271)
        )
    }

    if (isCompleted) {
        return base.copy(
            container = base.container.copy(alpha = 0.78f),
            border = base.border.copy(alpha = 0.75f),
            accent = base.accent.copy(alpha = 0.78f),
            meta = base.meta.copy(alpha = 0.85f)
        )
    }

    return base
}

private fun baseFoodStyle(
    container: Color,
    border: Color,
    accent: Color
): FoodVisualStyle {
    return FoodVisualStyle(
        container = container,
        border = border,
        accent = accent,
        title = Color(0xFF1F1A1A),
        body = Color(0xFF2F2727),
        meta = Color(0xFF5A4C4C)
    )
}

private fun fallbackStyleForSlot(
    slotType: MealSlotType
): FoodVisualStyle {
    return when (slotType) {
        MealSlotType.BREAKFAST -> baseFoodStyle(
            Color(0xFFFFE6D5),
            Color(0xFFFFA36C),
            Color(0xFFFFA36C)
        )

        MealSlotType.MORNING_SNACK -> baseFoodStyle(
            Color(0xFFE4F7E7),
            Color(0xFF6BCB77),
            Color(0xFF6BCB77)
        )

        MealSlotType.LUNCH -> baseFoodStyle(
            Color(0xFFDDF0FF),
            Color(0xFF5AA9FF),
            Color(0xFF5AA9FF)
        )

        MealSlotType.AFTERNOON_SNACK -> baseFoodStyle(
            Color(0xFFFFE8C7),
            Color(0xFFFFB84D),
            Color(0xFFFFB84D)
        )

        MealSlotType.DINNER -> baseFoodStyle(
            Color(0xFFE7E0FF),
            Color(0xFF8B7CFF),
            Color(0xFF8B7CFF)
        )
    }
}
