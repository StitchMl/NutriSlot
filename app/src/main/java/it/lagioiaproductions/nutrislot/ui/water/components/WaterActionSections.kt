package it.lagioiaproductions.nutrislot.ui.water.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.TrackChanges
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import it.lagioiaproductions.nutrislot.ui.water.state.formatWaterAmount
import it.lagioiaproductions.nutrislot.ui.water.theme.WaterTrackerColors

/** Shows the quick container presets used to log water consumption rapidly. */
@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun WaterContainerPresetsSection(
    presets: List<Int>,
    onPresetClick: (Int) -> Unit,
    onCustomAddClick: () -> Unit,
    onManagePresetsClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = WaterTrackerColors.Panel
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Quick containers",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = WaterTrackerColors.TextPrimary
                )

                Text(
                    text = "${presets.size} saved",
                    style = MaterialTheme.typography.labelLarge,
                    color = WaterTrackerColors.TextSecondary
                )
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                presets.forEach { amount ->
                    Surface(
                        onClick = { onPresetClick(amount) },
                        shape = RoundedCornerShape(18.dp),
                        color = WaterTrackerColors.PanelSecondary
                    ) {
                        Text(
                            text = formatWaterAmount(amount),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = WaterTrackerColors.TextPrimary
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = onCustomAddClick) {
                    Text(
                        text = "Custom amount",
                        color = WaterTrackerColors.AccentBlue,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                TextButton(onClick = onManagePresetsClick) {
                    Text(
                        text = "Manage bottles",
                        color = WaterTrackerColors.TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

/** Groups secondary water tracker actions such as goal, reset and reminders. */
@Composable
fun WaterActionsPanel(
    onGoalClick: () -> Unit,
    onResetDayClick: () -> Unit,
    onReminderClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = WaterTrackerColors.Panel
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            WaterIconActionButton(
                imageVector = Icons.Outlined.TrackChanges,
                contentDescription = "Imposta o modifica goal",
                onClick = onGoalClick
            )
            WaterIconActionButton(
                imageVector = Icons.Outlined.Refresh,
                contentDescription = "Reset del giorno",
                onClick = onResetDayClick
            )
            WaterIconActionButton(
                imageVector = Icons.Outlined.NotificationsNone,
                contentDescription = "Reminder acqua",
                onClick = onReminderClick
            )
        }
    }
}

/** Small circular icon button used by the tracker action panel. */
@Composable
private fun WaterIconActionButton(
    imageVector: ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.size(56.dp),
        shape = CircleShape,
        color = WaterTrackerColors.PanelSecondary
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = imageVector,
                contentDescription = contentDescription,
                tint = WaterTrackerColors.TextPrimary
            )
        }
    }
}
