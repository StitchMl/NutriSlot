package it.lagioiaproductions.nutrislot.ui.weeklyplan.edit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Groups dialog actions by intent: save, propagate, enrich with Gemini and local dialog controls.
 */
@Composable
internal fun EditSlotActionBar(
    isGeminiRecalculating: Boolean,
    isGeminiCatalogingTargets: Boolean,
    canActOnMeal: Boolean,
    onRecalculateNutritionWithGemini: () -> Unit,
    onSave: () -> Unit,
    onSaveForNextWeeks: () -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit,
    canReset: Boolean
) {
    val isGeminiBusy = isGeminiRecalculating || isGeminiCatalogingTargets

    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.46f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FilledTonalButton(
                    onClick = onSave,
                    enabled = canActOnMeal && !isGeminiBusy,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Save,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = " Salva qui",
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                FilledTonalButton(
                    onClick = onSaveForNextWeeks,
                    enabled = canActOnMeal && !isGeminiBusy,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = " Salva come base",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FilledTonalButton(
                    onClick = onRecalculateNutritionWithGemini,
                    enabled = !isGeminiBusy && canActOnMeal,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    if (isGeminiBusy) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Calcola nutrienti con Gemini"
                        )
                    }
                    Text(
                        text = " Nutrienti AI",
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                OutlinedIconButton(
                    onClick = onReset,
                    enabled = canReset && !isGeminiBusy
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Undo,
                        contentDescription = "Ripristina"
                    )
                }

                OutlinedIconButton(
                    onClick = onDismiss,
                    enabled = !isGeminiBusy
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Chiudi"
                    )
                }
            }
        }
    }
}
