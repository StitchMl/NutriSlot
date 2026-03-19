@file:Suppress("AssignedValueIsNeverRead")

package it.lagioiaproductions.nutrislot.ui.water

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun WaterTrackerScreen() {
    var targetMl by rememberSaveable { mutableIntStateOf(2000) }
    var consumedMl by rememberSaveable { mutableIntStateOf(1200) }

    var remindersEnabled by rememberSaveable { mutableStateOf(false) }
    var reminderIntervalMinutes by rememberSaveable { mutableIntStateOf(90) }

    var showAddDialog by rememberSaveable { mutableStateOf(false) }
    var showGoalDialog by rememberSaveable { mutableStateOf(false) }
    var showReminderDialog by rememberSaveable { mutableStateOf(false) }
    var showResetDialog by rememberSaveable { mutableStateOf(false) }

    val uiState = WaterTrackerUiState(
        targetMl = targetMl,
        consumedMl = consumedMl,
        remindersEnabled = remindersEnabled,
        reminderIntervalMinutes = reminderIntervalMinutes
    )

    val topTextColor = if (uiState.isGoalReached) {
        WaterTrackerColors.TextOnBlue
    } else {
        WaterTrackerColors.HeaderText
    }

    val topValueColor = if (uiState.isGoalReached) {
        WaterTrackerColors.TextOnBlue
    } else {
        WaterTrackerColors.HeaderValue
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        AnimatedWaterBackground(
            progress = uiState.progressVisual,
            isGoalReached = uiState.isGoalReached
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 22.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Water Reminder",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = topTextColor
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 26.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    WaterTopMetric(
                        title = "Daily Goal",
                        value = "${uiState.targetMl} ml",
                        titleColor = topTextColor,
                        valueColor = topValueColor
                    )

                    WaterTopMetric(
                        title = "Complete",
                        value = "${uiState.consumedMl} ml",
                        alignEnd = true,
                        titleColor = topTextColor,
                        valueColor = topValueColor
                    )
                }

                Text(
                    text = uiState.hydrationStatus.label,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp),
                    textAlign = TextAlign.End,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = topTextColor
                )
            }

            WaterCenterBubble(uiState = uiState)

            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                WaterGlassCard(
                    uiState = uiState,
                    onUndoLast = {
                        consumedMl = (consumedMl - 250).coerceAtLeast(0)
                    },
                    onResetAll = {
                        showResetDialog = true
                    }
                )

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Bottom
                ) {
                    WaterActionWithLabel(
                        symbol = "⏰",
                        label = "Reminder",
                        onClick = { showReminderDialog = true },
                        isPrimary = false
                    )

                    WaterActionWithLabel(
                        symbol = "+",
                        label = "Add",
                        onClick = { showAddDialog = true },
                        isPrimary = true
                    )

                    WaterActionWithLabel(
                        symbol = "ml",
                        label = "Goal",
                        onClick = { showGoalDialog = true },
                        isPrimary = false
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AmountDialog(
            title = "Add water",
            currentValueMl = uiState.consumedMl,
            confirmLabel = "Add",
            presets = listOf(150, 250, 500, 750, 1000),
            initialInput = "",
            helperText = "Insert ml to add to the current amount.",
            onDismiss = { showAddDialog = false },
            onConfirm = { ml ->
                consumedMl += ml
                showAddDialog = false
            }
        )
    }

    if (showGoalDialog) {
        AmountDialog(
            title = "Change daily goal",
            currentValueMl = uiState.targetMl,
            confirmLabel = "Save",
            presets = listOf(2000, 2500, 3000, 3500, 4000),
            initialInput = uiState.targetMl.toString(),
            helperText = "Set the daily hydration target in ml.",
            onDismiss = { showGoalDialog = false },
            onConfirm = { ml ->
                targetMl = ml.coerceAtLeast(250)
                showGoalDialog = false
            }
        )
    }

    if (showReminderDialog) {
        ReminderDialog(
            enabled = uiState.remindersEnabled,
            intervalMinutes = uiState.reminderIntervalMinutes,
            onDismiss = { showReminderDialog = false },
            onConfirm = { enabled, interval ->
                remindersEnabled = enabled
                reminderIntervalMinutes = interval
                showReminderDialog = false
            }
        )
    }

    if (showResetDialog) {
        ResetWaterDialog(
            onDismiss = { showResetDialog = false },
            onConfirm = {
                consumedMl = 0
                showResetDialog = false
            }
        )
    }
}