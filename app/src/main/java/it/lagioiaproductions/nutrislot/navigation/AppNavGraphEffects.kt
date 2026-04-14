package it.lagioiaproductions.nutrislot.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import it.lagioiaproductions.nutrislot.ui.shared.AppBridgeViewModel
import it.lagioiaproductions.nutrislot.ui.weeklyplan.state.WeeklyPlanUiState
import it.lagioiaproductions.nutrislot.ui.weeklyplan.viewmodel.WeeklyPlanViewModel

@Composable
internal fun WeeklyPlanNavigationEffects(
    isPlannerVisible: Boolean,
    weeklyPlanUiState: WeeklyPlanUiState,
    weeklyPlanViewModel: WeeklyPlanViewModel,
    bridgeViewModel: AppBridgeViewModel
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(Unit) {
        weeklyPlanViewModel.loadLatestPlan()
    }

    LaunchedEffect(weeklyPlanUiState.pendingCalorieSyncEvent?.id) {
        weeklyPlanUiState.pendingCalorieSyncEvent?.let { event ->
            bridgeViewModel.addWeeklyPlanConsumptionToCalories(
                dayKey = event.targetDayKey,
                consumptionId = event.consumptionId,
                mealText = event.mealText,
                mealSlotLabel = event.mealSlotLabel
            )
            weeklyPlanViewModel.consumePendingCalorieSyncEvent()
        }
    }

    LaunchedEffect(weeklyPlanUiState.pendingCalorieUndoEvent?.id) {
        weeklyPlanUiState.pendingCalorieUndoEvent?.let { event ->
            bridgeViewModel.removeWeeklyPlanConsumptionFromCalories(
                consumptionId = event.consumptionId
            )
            weeklyPlanViewModel.consumePendingCalorieUndoEvent()
        }
    }

    DisposableEffect(lifecycleOwner, isPlannerVisible) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && isPlannerVisible) {
                weeklyPlanViewModel.loadLatestPlan()
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}
