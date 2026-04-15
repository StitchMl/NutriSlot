package it.lagioiaproductions.nutrislot.ui.weeklyplan.edit

import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import it.lagioiaproductions.nutrislot.ui.weeklyplan.parser.parseMealSectionVisuals
import it.lagioiaproductions.nutrislot.ui.weeklyplan.slot.EditSlotDialogUi
import it.lagioiaproductions.nutrislot.ui.weeklyplan.slot.EditSlotSaveRequest

/**
 * Entry point for the slot editing flow.
 *
 * The dialog owns only the transient form state and delegates visual sections to
 * smaller composables so that each file has a single UI responsibility.
 */
@Composable
fun EditSlotDialog(
    dialogUi: EditSlotDialogUi,
    onDismiss: () -> Unit,
    onSave: (EditSlotSaveRequest) -> Unit,
    onSaveForNextWeeks: (EditSlotSaveRequest) -> Unit,
    onReset: () -> Unit,
    onRecalculateNutritionWithGemini: (mealText: String) -> Unit
) {
    var mealText by remember(dialogUi.slotId, dialogUi.mealText) {
        mutableStateOf(dialogUi.mealText)
    }
    var nutritionText by remember(dialogUi.slotId, dialogUi.nutritionText) {
        mutableStateOf(dialogUi.nutritionText)
    }
    var selectedTargetKeys by remember(
        dialogUi.slotId,
        dialogUi.selectedConsumptionTargetCanonicalKeys
    ) {
        mutableStateOf(dialogUi.selectedConsumptionTargetCanonicalKeys.distinct())
    }

    val didUserEditConsumptionTargets = selectedTargetKeys.toSet() !=
        dialogUi.selectedConsumptionTargetCanonicalKeys.distinct().toSet()
    val isGeminiBusy = dialogUi.isGeminiRecalculating || dialogUi.isGeminiCatalogingTargets
    val parsedSections = remember(mealText) { parseMealSectionVisuals(mealText) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            EditSlotHeroCard(
                dialogUi = dialogUi,
                selectedTargetCount = selectedTargetKeys.size,
                didUserEditConsumptionTargets = didUserEditConsumptionTargets
            )
        },
        text = {
            EditSlotDialogContent(
                dialogUi = dialogUi,
                mealText = mealText,
                onMealTextChange = { mealText = it },
                nutritionText = nutritionText,
                onNutritionTextChange = { nutritionText = it },
                selectedTargetKeys = selectedTargetKeys,
                onToggleTargetKey = { canonicalKey ->
                    selectedTargetKeys = toggleTargetSelection(
                        currentSelection = selectedTargetKeys,
                        canonicalKey = canonicalKey
                    )
                },
                didUserEditConsumptionTargets = didUserEditConsumptionTargets,
                isGeminiBusy = isGeminiBusy,
                parsedSections = parsedSections,
                onRecalculateNutritionWithGemini = {
                    onRecalculateNutritionWithGemini(mealText)
                },
                onSave = {
                    onSave(
                        buildEditSlotSaveRequest(
                            mealText = mealText,
                            nutritionText = nutritionText,
                            selectedTargetKeys = selectedTargetKeys,
                            didUserEditConsumptionTargets = didUserEditConsumptionTargets
                        )
                    )
                },
                onSaveForNextWeeks = {
                    onSaveForNextWeeks(
                        buildEditSlotSaveRequest(
                            mealText = mealText,
                            nutritionText = nutritionText,
                            selectedTargetKeys = selectedTargetKeys,
                            didUserEditConsumptionTargets = didUserEditConsumptionTargets
                        )
                    )
                },
                onReset = onReset,
                onDismiss = onDismiss
            )
        },
        confirmButton = {},
        dismissButton = {}
    )
}
