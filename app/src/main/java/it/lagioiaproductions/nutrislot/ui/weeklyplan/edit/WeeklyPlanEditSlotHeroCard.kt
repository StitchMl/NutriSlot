package it.lagioiaproductions.nutrislot.ui.weeklyplan.edit

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import it.lagioiaproductions.nutrislot.domain.model.MealConsumptionTargetSource
import it.lagioiaproductions.nutrislot.ui.weeklyplan.slot.EditSlotDialogUi

/**
 * Summarizes the current slot, source of classification and customization density in the dialog header.
 */
@Composable
internal fun EditSlotHeroCard(
    dialogUi: EditSlotDialogUi,
    selectedTargetCount: Int,
    didUserEditConsumptionTargets: Boolean
) {
    val gradientColors = when {
        didUserEditConsumptionTargets || dialogUi.consumptionTargetSource == MealConsumptionTargetSource.MANUAL -> {
            listOf(Color(0xFFF7C58B), Color(0xFFF3A18F), MaterialTheme.colorScheme.primaryContainer)
        }
        dialogUi.consumptionTargetSource == MealConsumptionTargetSource.GEMINI -> {
            listOf(Color(0xFFA9E2D0), Color(0xFFB7D7FF), MaterialTheme.colorScheme.secondaryContainer)
        }
        else -> {
            listOf(
                MaterialTheme.colorScheme.primaryContainer,
                MaterialTheme.colorScheme.secondaryContainer,
                Color(0xFFFFE6B4)
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.linearGradient(gradientColors),
                shape = MaterialTheme.shapes.large
            )
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Modifica rapida",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color(0xFF4A382C)
                    )
                    Text(
                        text = "${dialogUi.dayLabel} | ${dialogUi.mealSlotLabel}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2C211B)
                    )
                    Text(
                        text = slotTimeLabelFromLabel(dialogUi.mealSlotLabel),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF5E4A3E)
                    )
                }

                EditDialogPill(
                    text = headerSourceLabel(
                        source = dialogUi.consumptionTargetSource,
                        didUserEditConsumptionTargets = didUserEditConsumptionTargets
                    ),
                    containerColor = Color.White.copy(alpha = 0.78f),
                    contentColor = Color(0xFF2C211B)
                )
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                EditDialogPill(
                    text = if (selectedTargetCount == 0) "Nessun target" else "$selectedTargetCount target",
                    containerColor = Color.White.copy(alpha = 0.78f),
                    contentColor = Color(0xFF2C211B)
                )
                EditDialogPill(
                    text = if (dialogUi.nutritionText.isBlank()) "Nutrienti opzionali" else "Nutrienti presenti",
                    containerColor = Color.White.copy(alpha = 0.66f),
                    contentColor = Color(0xFF5C473A)
                )
            }
        }
    }
}

/**
 * Small reusable token used by the edit dialog to highlight metadata and counters.
 */
@Composable
internal fun EditDialogPill(
    text: String,
    containerColor: Color,
    contentColor: Color
) {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = containerColor
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            style = MaterialTheme.typography.labelMedium,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
