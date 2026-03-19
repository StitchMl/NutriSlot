package it.lagioiaproductions.nutrislot.ui.water

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun WaterTopMetric(
    title: String,
    value: String,
    titleColor: Color,
    valueColor: Color,
    alignEnd: Boolean = false
) {
    Column(
        horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            color = titleColor
        )
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            color = valueColor
        )
    }
}

@Composable
fun WaterCenterBubble(
    uiState: WaterTrackerUiState
) {
    Box(
        modifier = Modifier
            .size(212.dp)
            .offset(y = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.size(212.dp),
            shape = CircleShape,
            color = WaterTrackerColors.SurfaceGlass
        ) {}

        Surface(
            modifier = Modifier.size(170.dp),
            shape = CircleShape,
            color = WaterTrackerColors.SurfaceGlassStrong
        ) {}

        Surface(
            modifier = Modifier.size(136.dp),
            shape = CircleShape,
            color = WaterTrackerColors.BubbleInner
        ) {}

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "${uiState.progressPercent}%",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.SemiBold,
                color = WaterTrackerColors.TextOnBlue
            )
            Text(
                text = "${uiState.consumedMl} / ${uiState.targetMl} ml",
                style = MaterialTheme.typography.bodyMedium,
                color = WaterTrackerColors.TextOnBlue.copy(alpha = 0.96f)
            )
            Text(
                text = if (uiState.remainingMl > 0) {
                    "Missing ${uiState.remainingMl} ml"
                } else {
                    "Goal reached"
                },
                modifier = Modifier.padding(top = 4.dp),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium,
                color = WaterTrackerColors.TextOnBlue.copy(alpha = 0.92f)
            )
        }
    }
}

@Composable
fun WaterGlassCard(
    uiState: WaterTrackerUiState,
    onUndoLast: () -> Unit,
    onResetAll: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        color = WaterTrackerColors.SurfaceGlass
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                WaterMiniInfo(
                    title = "Remaining",
                    value = "${uiState.remainingMl} ml"
                )

                WaterMiniInfo(
                    title = "Reminder",
                    value = if (uiState.remindersEnabled) {
                        "Every ${uiState.reminderIntervalMinutes} min"
                    } else {
                        "Off"
                    },
                    alignEnd = true
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = onUndoLast) {
                    Text(
                        text = "Remove 250 ml",
                        color = WaterTrackerColors.DangerSoft,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                TextButton(onClick = onResetAll) {
                    Text(
                        text = "Reset all",
                        color = WaterTrackerColors.TextOnBlue,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun WaterMiniInfo(
    title: String,
    value: String,
    alignEnd: Boolean = false
) {
    Column(
        horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = WaterTrackerColors.TextOnBlue.copy(alpha = 0.82f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = WaterTrackerColors.TextOnBlue
        )
    }
}

@Composable
fun WaterActionWithLabel(
    symbol: String,
    label: String,
    onClick: () -> Unit,
    isPrimary: Boolean
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            onClick = onClick,
            modifier = Modifier.size(if (isPrimary) 62.dp else 52.dp),
            shape = CircleShape,
            color = if (isPrimary) Color(0xFF138FE1) else Color(0x290A7BCF),
            contentColor = WaterTrackerColors.TextOnBlue,
            shadowElevation = if (isPrimary) 10.dp else 2.dp,
            tonalElevation = 0.dp
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = symbol,
                    style = if (isPrimary) {
                        MaterialTheme.typography.headlineMedium
                    } else {
                        MaterialTheme.typography.titleLarge
                    },
                    fontWeight = FontWeight.SemiBold,
                    color = WaterTrackerColors.TextOnBlue
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = WaterTrackerColors.TextOnBlue.copy(alpha = 0.92f),
            fontWeight = FontWeight.Medium
        )
    }
}