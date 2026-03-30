package it.lagioiaproductions.nutrislot.ui.weeklyplan

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import it.lagioiaproductions.nutrislot.domain.model.WeekDay

@Composable
fun ImportOnlyContent(
    innerPadding: PaddingValues,
    onImportClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
        contentAlignment = Alignment.Center
    ) {
        Button(onClick = onImportClick) {
            Text("Importa piano")
        }
    }
}

@Composable
fun WeeklyCalendarGridContent(
    innerPadding: PaddingValues,
    uiState: WeeklyPlanUiState,
    onImportClick: () -> Unit,
    onRefreshClick: () -> Unit,
    onOpenSlotAction: (slotId: String) -> Unit,
    onOpenSlotEdit: (slotId: String) -> Unit,
    onToggleSlotCompleted: (slotId: String) -> Unit,
    onSelectCalendarDay: (WeekDay) -> Unit,
    onToggleConsumedSlotsVisibility: () -> Unit,
    onAddMealToShopping: (List<String>) -> Unit,
    onAddDayToShopping: (List<String>) -> Unit,
    onAddWeekToShopping: (List<String>) -> Unit,
    onOpenWeeklyQuantityChecklist: () -> Unit,
    plannerFeedbackMessage: String?
) {
    val gridData = rememberWeeklyCalendarGridData(uiState)
    val horizontalScroll = rememberScrollState()
    val verticalScroll = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
    ) {
        LoadedPlanTopBar(
            title = uiState.planTitle?.takeIf { it.isNotBlank() } ?: "Weekly plan",
            onImportClick = onImportClick,
            onRefreshClick = onRefreshClick,
            onToggleConsumedSlotsVisibility = onToggleConsumedSlotsVisibility,
            showConsumedSlots = uiState.showConsumedSlotsInCalendar,
            checklistItems = uiState.weeklyQuantityChecklist,
            onOpenWeeklyQuantityChecklist = onOpenWeeklyQuantityChecklist
        )

        AnimatedVisibility(
            visible = !plannerFeedbackMessage.isNullOrBlank(),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            PlannerFeedbackToken(
                message = plannerFeedbackMessage.orEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .padding(bottom = 8.dp)
            )
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RectangleShape,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .horizontalScroll(horizontalScroll)
                    .verticalScroll(verticalScroll)
            ) {
                Column(
                    modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                ) {
                    CalendarHeaderRow(
                        orderedDays = orderedCalendarDays,
                        selectedDay = uiState.selectedCalendarDay,
                        currentDay = uiState.currentWeekReferenceDay,
                        allSlotsByDay = gridData.allSlotsByDay,
                        onSelectCalendarDay = onSelectCalendarDay,
                        onAddDayToShopping = onAddDayToShopping,
                        onAddWeekToShopping = { onAddWeekToShopping(gridData.weekShoppingItems) }
                    )

                    weeklyCalendarSlotOrder.forEachIndexed { index, slotType ->
                        CalendarSlotRow(
                            slotType = slotType,
                            orderedDays = orderedCalendarDays,
                            selectedDay = uiState.selectedCalendarDay,
                            currentDay = uiState.currentWeekReferenceDay,
                            isLastRow = index == weeklyCalendarSlotOrder.lastIndex,
                            slotsByDayAndType = gridData.slotsByDayAndType,
                            onOpenSlotAction = onOpenSlotAction,
                            onOpenSlotEdit = onOpenSlotEdit,
                            onToggleSlotCompleted = onToggleSlotCompleted,
                            onAddMealToShopping = onAddMealToShopping
                        )
                    }

                    Spacer(modifier = Modifier.height(weeklyCalendarBottomScrollPadding))
                }
            }
        }
    }
}
