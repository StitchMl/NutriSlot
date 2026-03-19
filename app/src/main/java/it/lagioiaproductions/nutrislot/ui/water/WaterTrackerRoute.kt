package it.lagioiaproductions.nutrislot.ui.water

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import it.lagioiaproductions.nutrislot.data.water.WaterPreferencesRepository

@Composable
fun WaterTrackerRoute() {
    val appContext = LocalContext.current.applicationContext
    val repository = remember(appContext) {
        WaterPreferencesRepository(appContext)
    }

    val viewModel: WaterTrackerViewModel = viewModel(
        factory = WaterTrackerViewModelFactory(repository)
    )

    val uiState by viewModel.uiState.collectAsState()

    WaterTrackerScreen(
        uiState = uiState,
        onAddWater = viewModel::addWater,
        onResetWater = viewModel::resetWater,
        onUpdateGoal = viewModel::updateGoal,
        onClearGoal = viewModel::clearGoal,
        onUpdateReminder = viewModel::updateReminder,
        onAddContainerPreset = viewModel::addContainerPreset,
        onRemoveContainerPreset = viewModel::removeContainerPreset
    )
}