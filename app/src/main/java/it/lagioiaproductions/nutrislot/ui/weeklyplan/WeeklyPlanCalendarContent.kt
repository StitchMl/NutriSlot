package it.lagioiaproductions.nutrislot.ui.weeklyplan

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Update
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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

private val TimeRailWidth = 78.dp
private val DayColumnWidth = 220.dp
private val DayHeaderHeight = 88.dp
private val MinTimeBandHeight = 188.dp
private val CalendarBottomScrollPadding = 104.dp

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
    onSelectCalendarDay: (WeekDay) -> Unit,
    onToggleConsumedSlotsVisibility: () -> Unit,
    onAddMealToShopping: (List<String>) -> Unit,
    onAddDayToShopping: (List<String>) -> Unit,
    onAddWeekToShopping: (List<String>) -> Unit,
    onOpenWeeklyQuantityChecklist: () -> Unit,
    plannerFeedbackMessage: String?
) {
    val visibleSlots = remember(uiState.slots, uiState.showConsumedSlotsInCalendar) {
        if (uiState.showConsumedSlotsInCalendar) uiState.slots
        else uiState.slots.filterNot { it.isActuallyCompletedThisWeek }
    }
    val slotsByDayAndType = remember(visibleSlots, uiState.orderedCalendarDays) {
        uiState.orderedCalendarDays.associateWith { day ->
            CalendarSlotOrder.associateWith { slotType ->
                visibleSlots.firstOrNull { it.dayOfWeek == day && it.mealSlotType == slotType }
            }
        }
    }
    val allSlotsByDay = remember(uiState.slots, uiState.orderedCalendarDays) {
        uiState.orderedCalendarDays.associateWith { day ->
            uiState.slots.filter { it.dayOfWeek == day }.sortedBy { it.mealSlotType.sortOrder }
        }
    }
    val weekShoppingItems = remember(allSlotsByDay, uiState.orderedCalendarDays) {
        uiState.orderedCalendarDays
            .flatMap { day -> extractShoppingItemsFromSlots(allSlotsByDay[day].orEmpty()) }
            .distinct()
    }

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
                Column(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
                    CalendarHeaderRow(
                        orderedDays = uiState.orderedCalendarDays,
                        selectedDay = uiState.selectedCalendarDay,
                        currentDay = uiState.currentWeekReferenceDay,
                        allSlotsByDay = allSlotsByDay,
                        onSelectCalendarDay = onSelectCalendarDay,
                        onAddDayToShopping = onAddDayToShopping,
                        onAddWeekToShopping = { onAddWeekToShopping(weekShoppingItems) },
                    )

                    CalendarSlotOrder.forEach { slotType ->
                        CalendarSlotRow(
                            slotType = slotType,
                            orderedDays = uiState.orderedCalendarDays,
                            slotsByDayAndType = slotsByDayAndType,
                            onOpenSlotAction = onOpenSlotAction,
                            onOpenSlotEdit = onOpenSlotEdit,
                            onAddMealToShopping = onAddMealToShopping
                        )
                    }

                    Spacer(modifier = Modifier.height(CalendarBottomScrollPadding))
                }
            }
        }
    }
}

@Composable
private fun CalendarHeaderRow(
    orderedDays: List<WeekDay>,
    selectedDay: WeekDay,
    currentDay: WeekDay,
    allSlotsByDay: Map<WeekDay, List<WeeklySlotUi>>,
    onSelectCalendarDay: (WeekDay) -> Unit,
    onAddWeekToShopping: () -> Unit,
    onAddDayToShopping: (List<String>) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(DayHeaderHeight)
    ) {
        Box (
            modifier = Modifier
                .width(TimeRailWidth)
                .fillMaxHeight()
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
        ) {
            IconButton(onClick = { onAddWeekToShopping() }) {
                Icon(
                    imageVector = Icons.Default.ShoppingCart,
                    contentDescription = "Aggiungi settimana alla spesa",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        orderedDays.forEach { day ->
            DayHeaderCell(
                day = day,
                isSelected = day == selectedDay,
                isToday = day == currentDay,
                visibleEventsCount = allSlotsByDay[day].orEmpty().size,
                headerHeight = DayHeaderHeight,
                dayWidth = DayColumnWidth,
                onClick = { onSelectCalendarDay(day) },
                onAddDayToShopping = {
                    onAddDayToShopping(extractShoppingItemsFromSlots(allSlotsByDay[day].orEmpty()))
                }
            )
        }
    }
}

@Composable
private fun CalendarSlotRow(
    slotType: MealSlotType,
    orderedDays: List<WeekDay>,
    slotsByDayAndType: Map<WeekDay, Map<MealSlotType, WeeklySlotUi?>>,
    onOpenSlotAction: (slotId: String) -> Unit,
    onOpenSlotEdit: (slotId: String) -> Unit,
    onAddMealToShopping: (List<String>) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .heightIn(min = MinTimeBandHeight)
    ) {
        TimeRailCell(timeLabel = slotTimeLabel(slotType), slotLabel = slotType.displayName)

        orderedDays.forEach { day ->
            CalendarGridCell(
                slotUi = slotsByDayAndType[day]?.get(slotType),
                dayWidth = DayColumnWidth,
                onOpenSlotAction = onOpenSlotAction,
                onOpenSlotEdit = onOpenSlotEdit,
                onAddMealToShopping = onAddMealToShopping
            )
        }
    }
}

@Composable
private fun LoadedPlanTopBar(
    title: String,
    onImportClick: () -> Unit,
    onRefreshClick: () -> Unit,
    onToggleConsumedSlotsVisibility: () -> Unit,
    showConsumedSlots: Boolean,
    onOpenWeeklyQuantityChecklist: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Box {
            IconButton(onClick = { onRefreshClick() }) {
                Icon(
                    imageVector = Icons.Default.Update,
                    contentDescription = "Aggiorna",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Box {
            IconButton(onClick = { onToggleConsumedSlotsVisibility() }) {
                Icon(
                    imageVector = (if (showConsumedSlots) Icons.Default.Visibility else Icons.Default.VisibilityOff),
                    contentDescription = (if (showConsumedSlots) "Nascondi completati" else "Mostra completati"),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Box {
            IconButton(onClick = { menuExpanded = true }) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Impostazioni piano",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                DropdownMenuItem(
                    text = { Text("Importa nuovo piano") },
                    onClick = {
                        menuExpanded = false
                        onImportClick()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Controllo quantità settimanali") },
                    onClick = {
                        menuExpanded = false
                        onOpenWeeklyQuantityChecklist()
                    }
                )
            }
        }
    }
}

@Composable
private fun TimeRailCell(
    timeLabel: String,
    slotLabel: String
) {
    val borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)

    Column(
        modifier = Modifier
            .width(TimeRailWidth)
            .fillMaxHeight()
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
private fun DayHeaderCell(
    day: WeekDay,
    isSelected: Boolean,
    isToday: Boolean,
    visibleEventsCount: Int,
    headerHeight: Dp,
    dayWidth: Dp,
    onClick: () -> Unit,
    onAddDayToShopping: () -> Unit
) {
    val backgroundColor = when {
        isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
        isToday -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
        else -> MaterialTheme.colorScheme.surface
    }

    Box(
        modifier = Modifier
            .width(dayWidth)
            .fillMaxHeight()
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
            .padding(4.dp)
    ) {
        ElevatedCard(
            onClick = onClick,
            modifier = Modifier
                .fillMaxSize()
                .height(headerHeight),
            shape = MaterialTheme.shapes.medium
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(backgroundColor)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.SpaceBetween
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

                    IconButton(
                        onClick = onAddDayToShopping,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShoppingCart,
                            contentDescription = "Aggiungi giorno alla spesa",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
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
}

@Composable
private fun CalendarGridCell(
    slotUi: WeeklySlotUi?,
    dayWidth: Dp,
    onOpenSlotAction: (slotId: String) -> Unit,
    onOpenSlotEdit: (slotId: String) -> Unit,
    onAddMealToShopping: (List<String>) -> Unit
) {
    val borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)

    Box(
        modifier = Modifier
            .width(dayWidth)
            .heightIn(min = MinTimeBandHeight)
            .border(1.dp, borderColor)
            .padding(4.dp)
    ) {
        slotUi?.let {
            WeeklySlotCard(
                modifier = Modifier.fillMaxWidth(),
                slotUi = it,
                onManageClick = { onOpenSlotAction(it.slotId) },
                onEditClick = { onOpenSlotEdit(it.slotId) },
                onAddToShoppingClick = {
                    onAddMealToShopping(extractShoppingItemsFromMealText(it.displayedMealText))
                }
            )
        }
    }
}

@Composable
private fun PlannerFeedbackToken(
    message: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        tonalElevation = 2.dp
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
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