package it.lagioiaproductions.nutrislot.ui.weeklyplan.edit

import androidx.compose.ui.graphics.Color
import it.lagioiaproductions.nutrislot.domain.model.MealConsumptionTargetSource
import it.lagioiaproductions.nutrislot.ui.weeklyplan.slot.EditSlotDialogUi
import it.lagioiaproductions.nutrislot.ui.weeklyplan.slot.EditSlotSaveRequest

/**
 * Builds the persistence payload expected by the edit flow from the transient dialog state.
 */
internal fun buildEditSlotSaveRequest(
    mealText: String,
    nutritionText: String,
    selectedTargetKeys: List<String>,
    didUserEditConsumptionTargets: Boolean
): EditSlotSaveRequest {
    return EditSlotSaveRequest(
        mealText = mealText,
        nutritionText = nutritionText,
        selectedConsumptionTargetCanonicalKeys = selectedTargetKeys.distinct(),
        didUserEditConsumptionTargets = didUserEditConsumptionTargets
    )
}

/**
 * Toggles a canonical key while preserving insertion order and uniqueness.
 */
internal fun toggleTargetSelection(
    currentSelection: List<String>,
    canonicalKey: String
): List<String> {
    return if (canonicalKey in currentSelection) {
        currentSelection - canonicalKey
    } else {
        currentSelection + canonicalKey
    }
}

/**
 * Explains to the user how the current target selection will be treated on save.
 */
internal fun targetSelectionHint(
    dialogUi: EditSlotDialogUi,
    didUserEditConsumptionTargets: Boolean
): String {
    return when {
        didUserEditConsumptionTargets -> "Hai scelto tu cosa tracciare."
        dialogUi.consumptionTargetSource == MealConsumptionTargetSource.MANUAL -> {
            "Questa selezione resta fissa finche non la cambi."
        }
        dialogUi.consumptionTargetSource == MealConsumptionTargetSource.GEMINI -> {
            "Puoi confermare o correggere i target riconosciuti."
        }
        else -> {
            "Se cambi il pasto senza toccare i chip, i target si aggiornano da soli."
        }
    }
}

/**
 * Labels the source badge shown in the dialog hero.
 */
internal fun headerSourceLabel(
    source: MealConsumptionTargetSource?,
    didUserEditConsumptionTargets: Boolean
): String {
    return when {
        didUserEditConsumptionTargets -> "Manuale"
        source == MealConsumptionTargetSource.MANUAL -> "Manuale"
        source == MealConsumptionTargetSource.GEMINI -> "Gemini"
        else -> "Auto"
    }
}

/**
 * Maps each canonical target to an emoji shortcut for the chip list.
 */
internal fun targetEmojiForCanonicalKey(canonicalKey: String): String {
    return when (canonicalKey) {
        "acqua" -> "\uD83D\uDCA7"
        "frutta e verdura" -> "\uD83E\uDD66"
        "caffe e the" -> "\u2615"
        "carne bianca" -> "\uD83C\uDF57"
        "carne rossa" -> "\uD83E\uDD69"
        "affettati" -> "\uD83E\uDD53"
        "uova" -> "\uD83E\uDD5A"
        "formaggi" -> "\uD83E\uDDC0"
        "patate" -> "\uD83E\uDD54"
        "piatto unico" -> "\uD83C\uDF72"
        "pesce" -> "\uD83D\uDC1F"
        "legumi" -> "\uD83E\uDED8"
        else -> "\u2728"
    }
}

/**
 * Provides a stable accent color per target chip so the selection remains visually scannable.
 */
internal fun targetChipAccent(canonicalKey: String): Color {
    return when (canonicalKey) {
        "acqua" -> Color(0xFFBEE7FF)
        "frutta e verdura" -> Color(0xFFD0EAA9)
        "caffe e the" -> Color(0xFFF6D4A7)
        "carne bianca" -> Color(0xFFFFD8AE)
        "carne rossa" -> Color(0xFFF4B4AD)
        "affettati" -> Color(0xFFFFC9BF)
        "uova" -> Color(0xFFFFE7A3)
        "formaggi" -> Color(0xFFFFE3A9)
        "patate" -> Color(0xFFF2D3A7)
        "piatto unico" -> Color(0xFFDCCAFF)
        "pesce" -> Color(0xFFBDD9FF)
        "legumi" -> Color(0xFFD9C7FF)
        else -> Color(0xFFF0D9CC)
    }
}

/**
 * Converts the localized meal label into the default time shown in the dialog hero.
 */
internal fun slotTimeLabelFromLabel(mealSlotLabel: String): String {
    return when (mealSlotLabel.trim().lowercase()) {
        "colazione" -> "07:30"
        "spuntino mattina" -> "10:30"
        "pranzo" -> "13:00"
        "spuntino pomeridiano" -> "16:30"
        "cena" -> "20:00"
        else -> ""
    }
}
