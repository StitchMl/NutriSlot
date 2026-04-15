package it.lagioiaproductions.nutrislot.ui.water.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import it.lagioiaproductions.nutrislot.ui.water.state.WaterTrackerUiState
import it.lagioiaproductions.nutrislot.ui.water.state.formatWaterAmount
import it.lagioiaproductions.nutrislot.ui.water.theme.WaterTrackerColors

/** Summarizes reminder state and remaining intake for the current day. */
@Composable
fun WaterRecordsCard(
    uiState: WaterTrackerUiState
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = WaterTrackerColors.Panel
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Today's records",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = WaterTrackerColors.TextPrimary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = if (uiState.remindersEnabled) {
                            "Reminder ogni ${uiState.reminderIntervalMinutes} min"
                        } else {
                            "Reminder off"
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = WaterTrackerColors.TextPrimary
                    )

                    Text(
                        text = if (uiState.isGoalConfigured) {
                            "Restano ${uiState.remainingMl} ml"
                        } else {
                            "Imposta un goal giornaliero"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = WaterTrackerColors.TextSecondary
                    )
                }

                Text(
                    text = formatWaterAmount(uiState.mainPresetMl),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = WaterTrackerColors.AccentBlue
                )
            }
        }
    }
}
