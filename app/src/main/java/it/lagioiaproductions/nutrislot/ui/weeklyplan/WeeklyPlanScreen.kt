package it.lagioiaproductions.nutrislot.ui.weeklyplan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import it.lagioiaproductions.nutrislot.domain.model.WeekDay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeeklyPlanScreen(
    uiState: WeeklyPlanUiState,
    onImportClick: () -> Unit,
    onRefreshClick: () -> Unit,
    onOpenSlotAction: (slotId: String) -> Unit,
    onDismissSlotAction: () -> Unit,
    onConsumeAsPlanned: () -> Unit,
    onConsumeReplacement: (sourceSlotId: String) -> Unit,
    onSelectCalendarDay: (WeekDay) -> Unit,
    onToggleConsumedSlotsVisibility: () -> Unit
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text("La tua dieta")
                }
            )
        }
    ) { innerPadding ->
        when {
            uiState.isLoading -> {
                LoadingContent(innerPadding = innerPadding)
            }
            uiState.errorMessage != null -> {
                ErrorContent(
                    innerPadding = innerPadding,
                    message = uiState.errorMessage,
                    onImportClick = onImportClick,
                    onRefreshClick = onRefreshClick
                )
            }
            uiState.isEmpty -> {
                EmptyContent(
                    innerPadding = innerPadding,
                    onImportClick = onImportClick
                )
            }
            else -> {
                LoadedWeeklyPlanContent(
                    innerPadding = innerPadding,
                    uiState = uiState,
                    onImportClick = onImportClick,
                    onRefreshClick = onRefreshClick,
                    onOpenSlotAction = onOpenSlotAction,
                    onSelectCalendarDay = onSelectCalendarDay,
                    onToggleConsumedSlotsVisibility = onToggleConsumedSlotsVisibility
                )
            }
        }

        uiState.slotActionDialog?.let { dialogUi ->
            SlotActionDialog(
                dialogUi = dialogUi,
                isApplying = uiState.isApplyingSlotAction,
                onDismiss = onDismissSlotAction,
                onConsumeAsPlanned = onConsumeAsPlanned,
                onConsumeReplacement = onConsumeReplacement
            )
        }
    }
}

@Composable
private fun LoadedWeeklyPlanContent(
    innerPadding: PaddingValues,
    uiState: WeeklyPlanUiState,
    onImportClick: () -> Unit,
    onRefreshClick: () -> Unit,
    onOpenSlotAction: (slotId: String) -> Unit,
    onSelectCalendarDay: (WeekDay) -> Unit,
    onToggleConsumedSlotsVisibility: () -> Unit
) {
    val selectedDaySlots = uiState.slots
        .filter { it.dayOfWeek == uiState.selectedCalendarDay }

    val visibleSelectedDaySlots = if (uiState.showConsumedSlotsInCalendar) {
        selectedDaySlots
    } else {
        selectedDaySlots.filterNot { slotUi ->
            slotUi.isActuallyCompletedThisWeek
        }
    }

    val hiddenSelectedDaySlotsCount = selectedDaySlots.size - visibleSelectedDaySlots.size

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .safeDrawingPadding(),
        contentPadding = PaddingValues(
            start = 16.dp,
            top = 12.dp,
            end = 16.dp,
            bottom = 28.dp
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            DietHeroCard(uiState = uiState)
        }

        uiState.actionMessage?.let { message ->
            item {
                FeedbackCard(
                    title = "Aggiornamento completato",
                    message = message,
                    isError = false
                )
            }
        }

        uiState.actionErrorMessage?.let { message ->
            item {
                FeedbackCard(
                    title = "Operazione non riuscita",
                    message = message,
                    isError = true
                )
            }
        }

        item {
            DayStripCard(
                uiState = uiState,
                onSelectCalendarDay = onSelectCalendarDay
            )
        }

        item {
            CalendarControlsCard(
                uiState = uiState,
                hiddenSelectedDaySlotsCount = hiddenSelectedDaySlotsCount,
                onToggleConsumedSlotsVisibility = onToggleConsumedSlotsVisibility,
                onImportClick = onImportClick,
                onRefreshClick = onRefreshClick
            )
        }

        item {
            SelectedDayHeroCard(
                selectedDay = uiState.selectedCalendarDay,
                visibleSlotsCount = visibleSelectedDaySlots.size,
                totalSlotsCount = selectedDaySlots.size
            )
        }

        if (visibleSelectedDaySlots.isEmpty()) {
            item {
                EmptySelectedDayStateCard(
                    selectedDay = uiState.selectedCalendarDay,
                    hiddenSelectedDaySlotsCount = hiddenSelectedDaySlotsCount,
                    isShowingConsumed = uiState.showConsumedSlotsInCalendar
                )
            }
        } else {
            items(
                items = visibleSelectedDaySlots,
                key = { it.slotId }
            ) { slotUi ->
                WeeklySlotCard(
                    slotUi = slotUi,
                    onManageClick = { onOpenSlotAction(slotUi.slotId) }
                )
            }
        }
    }
}