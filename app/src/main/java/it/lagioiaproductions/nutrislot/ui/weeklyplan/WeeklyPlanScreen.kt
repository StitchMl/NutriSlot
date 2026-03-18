package it.lagioiaproductions.nutrislot.ui.weeklyplan

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import it.lagioiaproductions.nutrislot.domain.model.MealSlotType
import it.lagioiaproductions.nutrislot.domain.model.WeekDay

private val CalendarSlotOrder = listOf(
    MealSlotType.BREAKFAST,
    MealSlotType.MORNING_SNACK,
    MealSlotType.LUNCH,
    MealSlotType.AFTERNOON_SNACK,
    MealSlotType.DINNER
)

private val TimeRailWidth = 72.dp
private val DayColumnWidth = 170.dp
private val DayHeaderHeight = 78.dp
private val TimeBandHeight = 106.dp

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
    Scaffold { innerPadding ->
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
                WeeklyCalendarGridContent(
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
private fun WeeklyCalendarGridContent(
    innerPadding: PaddingValues,
    uiState: WeeklyPlanUiState,
    onImportClick: () -> Unit,
    onRefreshClick: () -> Unit,
    onOpenSlotAction: (slotId: String) -> Unit,
    onSelectCalendarDay: (WeekDay) -> Unit,
    onToggleConsumedSlotsVisibility: () -> Unit
) {
    val visibleSlots = remember(
        uiState.slots,
        uiState.showConsumedSlotsInCalendar
    ) {
        if (uiState.showConsumedSlotsInCalendar) {
            uiState.slots
        } else {
            uiState.slots.filterNot { it.isActuallyCompletedThisWeek }
        }
    }

    val slotsByDayAndType = remember(visibleSlots, uiState.orderedCalendarDays) {
        uiState.orderedCalendarDays.associateWith { day ->
            CalendarSlotOrder.associateWith { slotType ->
                visibleSlots.firstOrNull {
                    it.dayOfWeek == day && it.mealSlotType == slotType
                }
            }
        }
    }

    val horizontalScroll = rememberScrollState()
    val verticalScroll = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        CompactCalendarTopBar(
            uiState = uiState,
            onImportClick = onImportClick,
            onRefreshClick = onRefreshClick,
            onToggleConsumedSlotsVisibility = onToggleConsumedSlotsVisibility
        )

        uiState.actionMessage?.let { message ->
            FeedbackCard(
                title = "Aggiornamento completato",
                message = message,
                isError = false
            )
        }

        uiState.actionErrorMessage?.let { message ->
            FeedbackCard(
                title = "Operazione non riuscita",
                message = message,
                isError = true
            )
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .horizontalScroll(horizontalScroll)
                    .verticalScroll(verticalScroll)
            ) {
                Row(
                    modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                ) {
                    TimeRailColumn(
                        slotOrder = CalendarSlotOrder,
                        headerHeight = DayHeaderHeight,
                        bandHeight = TimeBandHeight
                    )

                    uiState.orderedCalendarDays.forEach { day ->
                        DayTimelineColumn(
                            day = day,
                            selectedDay = uiState.selectedCalendarDay,
                            currentDay = uiState.currentWeekReferenceDay,
                            dayWidth = DayColumnWidth,
                            headerHeight = DayHeaderHeight,
                            bandHeight = TimeBandHeight,
                            slotsForDay = slotsByDayAndType[day].orEmpty(),
                            onSelectDay = { onSelectCalendarDay(day) },
                            onOpenSlotAction = onOpenSlotAction
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactCalendarTopBar(
    uiState: WeeklyPlanUiState,
    onImportClick: () -> Unit,
    onRefreshClick: () -> Unit,
    onToggleConsumedSlotsVisibility: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Weekly plan",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = uiState.planTitle?.takeIf { it.isNotBlank() }
                        ?: "Calendario pasti settimanale",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onImportClick,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Importa")
                }

                OutlinedButton(
                    onClick = onRefreshClick,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Aggiorna")
                }
            }

            FilterChip(
                selected = uiState.showConsumedSlotsInCalendar,
                onClick = onToggleConsumedSlotsVisibility,
                label = {
                    Text(
                        if (uiState.showConsumedSlotsInCalendar) {
                            "Mostra completati"
                        } else {
                            "Nascondi completati"
                        }
                    )
                }
            )
        }
    }
}

@Composable
private fun TimeRailColumn(
    slotOrder: List<MealSlotType>,
    headerHeight: Dp,
    bandHeight: Dp
) {
    Column(
        modifier = Modifier
            .width(TimeRailWidth)
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(headerHeight)
        )

        slotOrder.forEach { slotType ->
            TimeRailCell(
                timeLabel = slotTimeLabel(slotType),
                slotLabel = slotType.displayName,
                bandHeight = bandHeight
            )
        }
    }
}

@Composable
private fun TimeRailCell(
    timeLabel: String,
    slotLabel: String,
    bandHeight: Dp
) {
    val borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(bandHeight)
            .border(1.dp, borderColor)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = timeLabel,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = slotLabel,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun DayTimelineColumn(
    day: WeekDay,
    selectedDay: WeekDay,
    currentDay: WeekDay,
    dayWidth: Dp,
    headerHeight: Dp,
    bandHeight: Dp,
    slotsForDay: Map<MealSlotType, WeeklySlotUi?>,
    onSelectDay: () -> Unit,
    onOpenSlotAction: (slotId: String) -> Unit
) {
    val isSelected = day == selectedDay
    val isToday = day == currentDay
    val columnBorderColor = when {
        isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.65f)
        isToday -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.55f)
        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
    }

    Column(
        modifier = Modifier
            .width(dayWidth)
            .border(1.dp, columnBorderColor)
    ) {
        DayHeaderCell(
            day = day,
            isSelected = isSelected,
            isToday = isToday,
            visibleEventsCount = slotsForDay.values.count { it != null },
            headerHeight = headerHeight,
            onClick = onSelectDay
        )

        CalendarSlotOrder.forEach { slotType ->
            CalendarGridCell(
                slotUi = slotsForDay[slotType],
                bandHeight = bandHeight,
                onOpenSlotAction = onOpenSlotAction
            )
        }
    }
}

@Composable
private fun DayHeaderCell(
    day: WeekDay,
    isSelected: Boolean,
    isToday: Boolean,
    visibleEventsCount: Int,
    headerHeight: Dp,
    onClick: () -> Unit
) {
    val backgroundColor = when {
        isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
        isToday -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
        else -> MaterialTheme.colorScheme.surface
    }

    ElevatedCard(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(headerHeight)
            .padding(4.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = day.displayName.take(3).uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = day.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Text(
                text = buildString {
                    if (isToday) append("Oggi • ")
                    append("$visibleEventsCount eventi")
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CalendarGridCell(
    slotUi: WeeklySlotUi?,
    bandHeight: Dp,
    onOpenSlotAction: (slotId: String) -> Unit
) {
    val borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(bandHeight)
            .border(1.dp, borderColor)
            .padding(4.dp)
    ) {
        if (slotUi != null) {
            WeeklySlotCard(
                modifier = Modifier.fillMaxSize(),
                slotUi = slotUi,
                onManageClick = { onOpenSlotAction(slotUi.slotId) }
            )
        }
    }
}

fun slotTimeLabel(slotType: MealSlotType): String {
    return when (slotType) {
        MealSlotType.BREAKFAST -> "07:30"
        MealSlotType.MORNING_SNACK -> "10:30"
        MealSlotType.LUNCH -> "13:00"
        MealSlotType.AFTERNOON_SNACK -> "16:30"
        MealSlotType.DINNER -> "20:00"
    }
}