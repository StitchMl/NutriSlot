package it.lagioiaproductions.nutrislot.ui.water

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun WaterHeaderTipCard(
    message: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            modifier = Modifier.size(66.dp),
            shape = CircleShape,
            color = WaterTrackerColors.AccentPurple.copy(alpha = 0.14f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "💧",
                    style = MaterialTheme.typography.headlineMedium
                )
            }
        }

        Surface(
            modifier = Modifier
                .weight(1f)
                .widthIn(min = 0.dp),
            shape = RoundedCornerShape(22.dp),
            color = WaterTrackerColors.Panel
        ) {
            Text(
                text = message,
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
                style = MaterialTheme.typography.bodyLarge,
                color = WaterTrackerColors.TextPrimary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun WaterGoalMeter(
    uiState: WaterTrackerUiState,
    onPrimaryPresetClick: (Int) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(306.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(286.dp)
                    .border(
                        width = 5.dp,
                        color = WaterTrackerColors.WhiteRing,
                        shape = CircleShape
                    )
            )

            Box(
                modifier = Modifier
                    .size(238.dp)
                    .clip(CircleShape)
                    .background(WaterTrackerColors.WhiteCard)
            ) {
                WaterLiquidFill(progress = uiState.progressVisual)

                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (uiState.isGoalConfigured) {
                            "${uiState.consumedMl}/${uiState.targetMl}ml"
                        } else {
                            "${uiState.consumedMl}ml"
                        },
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = WaterTrackerColors.TextDark
                    )

                    Text(
                        text = if (uiState.isGoalConfigured) {
                            "Daily Drink Target"
                        } else {
                            "Goal non impostato"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = WaterTrackerColors.TextDark,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = uiState.hydrationStatusLabel,
                        modifier = Modifier.padding(top = 8.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = WaterTrackerColors.AccentPurple
                    )
                }

                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .offset(y = (-18).dp),
                    shape = RoundedCornerShape(18.dp),
                    color = WaterTrackerColors.WhiteCard.copy(alpha = 0.95f)
                ) {
                    Text(
                        text = formatWaterAmount(uiState.mainPresetMl),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = WaterTrackerColors.TextDark
                    )
                }
            }
        }

        Surface(
            onClick = { onPrimaryPresetClick(uiState.mainPresetMl) },
            shape = RoundedCornerShape(22.dp),
            color = WaterTrackerColors.AccentPurple.copy(alpha = 0.18f)
        ) {
            Text(
                text = "Aggiungi ${formatWaterAmount(uiState.mainPresetMl)}",
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = WaterTrackerColors.TextPrimary
            )
        }

        Text(
            text = "Conferma rapidamente quando hai bevuto",
            modifier = Modifier.padding(top = 10.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = WaterTrackerColors.TextSecondary
        )
    }
}

@Composable
private fun WaterLiquidFill(
    progress: Float
) {
    Canvas(
        modifier = Modifier.fillMaxSize()
    ) {
        val width = size.width
        val height = size.height
        val normalizedProgress = progress.coerceIn(0f, 1f)
        val liquidBaseY = height * (0.84f - normalizedProgress * 0.42f)

        val path = Path().apply {
            moveTo(0f, height)
            lineTo(0f, liquidBaseY)

            var x = 0f
            val wavelength = width / 1.2f
            val amplitude = 14.dp.toPx()

            while (x <= width) {
                val y = liquidBaseY + sin((x / wavelength) * (2f * PI).toFloat()) * amplitude
                lineTo(x, y)
                x += 8f
            }

            lineTo(width, height)
            close()
        }

        drawPath(path = path, color = WaterTrackerColors.Liquid)
        drawPath(
            path = path,
            color = WaterTrackerColors.LiquidDeep.copy(alpha = 0.55f)
        )

        drawRect(
            color = Color.White.copy(alpha = 0.25f),
            topLeft = Offset(0f, liquidBaseY - 10.dp.toPx()),
            size = Size(width, 8.dp.toPx())
        )
    }
}

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

@Composable
private fun WaterIconActionButton(
    imageVector: androidx.compose.ui.graphics.vector.ImageVector,
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