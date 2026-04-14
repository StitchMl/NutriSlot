package it.lagioiaproductions.nutrislot.ui.water.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import it.lagioiaproductions.nutrislot.ui.water.dialogs.ContainerPresetsDialog
import it.lagioiaproductions.nutrislot.ui.water.dialogs.ReminderDialog
import it.lagioiaproductions.nutrislot.ui.water.theme.WaterTrackerColors
import it.lagioiaproductions.nutrislot.ui.water.state.WaterTrackerUiState
import it.lagioiaproductions.nutrislot.ui.water.background.AnimatedWaterBackground
import it.lagioiaproductions.nutrislot.ui.water.components.WaterActionsPanel
import it.lagioiaproductions.nutrislot.ui.water.components.WaterContainerPresetsSection
import it.lagioiaproductions.nutrislot.ui.water.components.WaterGoalMeter
import it.lagioiaproductions.nutrislot.ui.water.components.WaterHeaderTipCard
import it.lagioiaproductions.nutrislot.ui.water.components.WaterRecordsCard
import it.lagioiaproductions.nutrislot.ui.water.dialogs.AmountDialog
import it.lagioiaproductions.nutrislot.ui.water.dialogs.ResetWaterDialog

@Suppress("AssignedValueIsNeverRead")
@Composable
fun WaterTrackerScreen(
    uiState: WaterTrackerUiState,
    onAddWater: (Int, Boolean) -> Unit,
    onResetWater: () -> Unit,
    onUpdateGoal: (Int) -> Unit,
    onUpdateReminder: (Boolean, Int) -> Unit,
    onAddContainerPreset: (Int) -> Unit,
    onRemoveContainerPreset: (Int) -> Unit
) {
    var showAddDialog by rememberSaveable { mutableStateOf(false) }
    var showGoalDialog by rememberSaveable { mutableStateOf(false) }
    var showReminderDialog by rememberSaveable { mutableStateOf(false) }
    var showResetDialog by rememberSaveable { mutableStateOf(false) }
    var showPresetsDialog by rememberSaveable { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (uiState.isGoalReached) WaterTrackerColors.GoalReachedBottom else WaterTrackerColors.Background)
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            WaterHeaderTipCard(
                message = "Do not drink cold water or water with ice"
            )

            WaterGoalMeter(
                uiState = uiState,
                onPrimaryPresetClick = { amount ->
                    onAddWater(amount, false)
                }
            )

            WaterContainerPresetsSection(
                presets = uiState.containerPresets,
                onPresetClick = { amount -> onAddWater(amount, false) },
                onCustomAddClick = { showAddDialog = true },
                onManagePresetsClick = { showPresetsDialog = true }
            )

            WaterActionsPanel(
                onGoalClick = { showGoalDialog = true },
                onResetDayClick = { showResetDialog = true },
                onReminderClick = { showReminderDialog = true }
            )

            WaterRecordsCard(uiState = uiState)

            Spacer(modifier = Modifier.height(12.dp))
        }
    }

    if (showAddDialog) {
        AmountDialog(
            title = "Add water",
            currentValueMl = uiState.consumedMl,
            confirmLabel = "Add",
            presets = uiState.containerPresets.ifEmpty { listOf(250, 500, 600) },
            initialInput = "",
            helperText = "If the amount is new, it will also be saved as a quick container.",
            onDismiss = { showAddDialog = false },
            onConfirm = { ml ->
                onAddWater(ml, true)
                showAddDialog = false
            }
        )
    }

    if (showGoalDialog) {
        AmountDialog(
            title = "Change goal",
            currentValueMl = uiState.targetMl,
            confirmLabel = "Save",
            presets = listOf(1500, 2000, 2500, 3000, 3500, 4000),
            initialInput = if (uiState.targetMl > 0) uiState.targetMl.toString() else "",
            helperText = "Set the daily hydration target in ml.",
            onDismiss = { showGoalDialog = false },
            onConfirm = { ml ->
                onUpdateGoal(ml)
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
                onUpdateReminder(enabled, interval)
                showReminderDialog = false
            }
        )
    }

    if (showResetDialog) {
        ResetWaterDialog(
            title = "Reset day",
            description = "This resets only the water consumed today.",
            confirmLabel = "Reset day",
            onDismiss = { showResetDialog = false },
            onConfirm = {
                onResetWater()
                showResetDialog = false
            }
        )
    }

    if (showPresetsDialog) {
        ContainerPresetsDialog(
            presets = uiState.containerPresets,
            onDismiss = { showPresetsDialog = false },
            onAddPreset = onAddContainerPreset,
            onRemovePreset = onRemoveContainerPreset
        )
    }
}