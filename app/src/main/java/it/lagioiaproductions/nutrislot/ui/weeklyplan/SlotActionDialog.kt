@file:Suppress("unused", "unused", "unused", "unused", "unused", "unused")

package it.lagioiaproductions.nutrislot.ui.weeklyplan

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun SlotActionDialog(
    dialogUi: SlotActionDialogUi,
    isApplying: Boolean,
    onDismiss: () -> Unit,
    onConsumeAsPlanned: () -> Unit,
    onConsumeReplacement: (sourceSlotId: String) -> Unit,
    onSelectExtraCatalogOption: (optionId: String) -> Unit,
    onUndoCompletedMeal: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {
            if (!isApplying) {
                onDismiss()
            }
        },
        title = {
            Text(slotActionDialogTitle(dialogUi))
        },
        text = {
            SlotActionDialogBody(
                dialogUi = dialogUi,
                isApplying = isApplying,
                onConsumeReplacement = onConsumeReplacement,
                onSelectExtraCatalogOption = onSelectExtraCatalogOption
            )
        },
        confirmButton = {
            SlotActionDialogCloseButton(
                enabled = !isApplying,
                onDismiss = onDismiss
            )
        },
        dismissButton = null
    )
}

private fun slotActionDialogTitle(dialogUi: SlotActionDialogUi): String {
    return "${dialogUi.targetDayLabel} - ${dialogUi.targetMealSlotLabel}"
}

@Composable
private fun SlotActionDialogCloseButton(
    enabled: Boolean,
    onDismiss: () -> Unit
) {
    TextButton(
        onClick = onDismiss,
        enabled = enabled
    ) {
        Icon(
            imageVector = Icons.Default.Close,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(4.dp))
        Text("Chiudi")
    }
}
