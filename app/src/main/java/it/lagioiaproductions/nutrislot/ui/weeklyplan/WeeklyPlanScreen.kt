package it.lagioiaproductions.nutrislot.ui.weeklyplan

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
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
import it.lagioiaproductions.nutrislot.ui.shared.ShoppingFeedbackUi
import kotlinx.coroutines.delay

private val CalendarSlotOrder = listOf(
    MealSlotType.BREAKFAST,
    MealSlotType.MORNING_SNACK,
    MealSlotType.LUNCH,
    MealSlotType.AFTERNOON_SNACK,
    MealSlotType.DINNER
)

private data class PlannerFeedbackTokenUi(
    val id: Long,
    val message: String
)

private val TimeRailWidth = 72.dp
private val DayColumnWidth = 170.dp
private val DayHeaderHeight = 78.dp
private val TimeBandHeight = 106.dp
private val CalendarBottomScrollPadding = 81.dp

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
    onToggleConsumedSlotsVisibility: () -> Unit,
    onAddMealToShopping: (List<String>) -> Unit,
    onAddDayToShopping: (List<String>) -> Unit,
    onAddWeekToShopping: (List<String>) -> Unit,
    shoppingFeedback: ShoppingFeedbackUi?,
    onConsumeShoppingFeedback: () -> Unit
) {
    val hasLoadedPlan = uiState.planId != null || uiState.slots.isNotEmpty()

    var plannerFeedback by remember { mutableStateOf<PlannerFeedbackTokenUi?>(null) }
    var nextPlannerFeedbackId by remember { mutableLongStateOf(1L) }

    LaunchedEffect(plannerFeedback?.id) {
        val activeId = plannerFeedback?.id ?: return@LaunchedEffect
        delay(2200)
        if (plannerFeedback?.id == activeId) {
            plannerFeedback = null
        }
    }

    LaunchedEffect(shoppingFeedback?.id) {
        shoppingFeedback?.let { feedback ->
            plannerFeedback = PlannerFeedbackTokenUi(
                id = nextPlannerFeedbackId++,
                message = feedback.message
            )
            onConsumeShoppingFeedback()
        }
    }

    fun dispatchPlannerShoppingFeedback(
        rawItems: List<String>,
        submit: (List<String>) -> Unit,
        singleLabel: String,
        pluralLabel: String
    ) {
        val cleanedItems = rawItems
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase() }

        if (cleanedItems.isEmpty()) {
            plannerFeedback = PlannerFeedbackTokenUi(
                id = nextPlannerFeedbackId++,
                message = "Nessun elemento valido da aggiungere alla lista della spesa."
            )
            return
        }

        submit(cleanedItems)

        plannerFeedback = PlannerFeedbackTokenUi(
            id = nextPlannerFeedbackId++,
            message = when (cleanedItems.size) {
                1 -> "$singleLabel aggiunto alla lista della spesa."
                else -> "${cleanedItems.size} $pluralLabel aggiunti alla lista della spesa."
            }
        )
    }

    val addMealWithFeedback: (List<String>) -> Unit = { items ->
        dispatchPlannerShoppingFeedback(
            rawItems = items,
            submit = onAddMealToShopping,
            singleLabel = "Pasto",
            pluralLabel = "articoli del pasto"
        )
    }

    val addDayWithFeedback: (List<String>) -> Unit = { items ->
        dispatchPlannerShoppingFeedback(
            rawItems = items,
            submit = onAddDayToShopping,
            singleLabel = "Giorno",
            pluralLabel = "articoli del giorno"
        )
    }

    val addWeekWithFeedback: (List<String>) -> Unit = { items ->
        dispatchPlannerShoppingFeedback(
            rawItems = items,
            submit = onAddWeekToShopping,
            singleLabel = "Settimana",
            pluralLabel = "articoli della settimana"
        )
    }

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

            !hasLoadedPlan -> {
                ImportOnlyContent(
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
                    onToggleConsumedSlotsVisibility = onToggleConsumedSlotsVisibility,
                    onAddMealToShopping = addMealWithFeedback,
                    onAddDayToShopping = addDayWithFeedback,
                    onAddWeekToShopping = addWeekWithFeedback,
                    plannerFeedbackMessage = plannerFeedback?.message
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
private fun ImportOnlyContent(
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
private fun WeeklyCalendarGridContent(
    innerPadding: PaddingValues,
    uiState: WeeklyPlanUiState,
    onImportClick: () -> Unit,
    onRefreshClick: () -> Unit,
    onOpenSlotAction: (slotId: String) -> Unit,
    onSelectCalendarDay: (WeekDay) -> Unit,
    onToggleConsumedSlotsVisibility: () -> Unit,
    onAddMealToShopping: (List<String>) -> Unit,
    onAddDayToShopping: (List<String>) -> Unit,
    onAddWeekToShopping: (List<String>) -> Unit,
    plannerFeedbackMessage: String?
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

    val allSlotsByDay = remember(uiState.slots, uiState.orderedCalendarDays) {
        uiState.orderedCalendarDays.associateWith { day ->
            uiState.slots
                .filter { it.dayOfWeek == day }
                .sortedBy { it.mealSlotType.sortOrder }
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
            onAddWeekToShopping = {
                onAddWeekToShopping(weekShoppingItems)
            }
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
                    Row {
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
                                allSlotsForDay = allSlotsByDay[day].orEmpty(),
                                onSelectDay = { onSelectCalendarDay(day) },
                                onOpenSlotAction = onOpenSlotAction,
                                onAddMealToShopping = onAddMealToShopping,
                                onAddDayToShopping = onAddDayToShopping
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(CalendarBottomScrollPadding))
                }
            }
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
    onAddWeekToShopping: () -> Unit
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
            IconButton(onClick = { menuExpanded = true }) {
                Text(
                    text = "⚙",
                    style = MaterialTheme.typography.titleLarge
                )
            }

            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Aggiungi settimana alla spesa") },
                    onClick = {
                        menuExpanded = false
                        onAddWeekToShopping()
                    }
                )

                DropdownMenuItem(
                    text = { Text("Importa nuovo piano") },
                    onClick = {
                        menuExpanded = false
                        onImportClick()
                    }
                )

                DropdownMenuItem(
                    text = { Text("Aggiorna") },
                    onClick = {
                        menuExpanded = false
                        onRefreshClick()
                    }
                )

                DropdownMenuItem(
                    text = {
                        Text(
                            if (showConsumedSlots) {
                                "Nascondi completati"
                            } else {
                                "Mostra completati"
                            }
                        )
                    },
                    onClick = {
                        menuExpanded = false
                        onToggleConsumedSlotsVisibility()
                    }
                )
            }
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
    allSlotsForDay: List<WeeklySlotUi>,
    onSelectDay: () -> Unit,
    onOpenSlotAction: (slotId: String) -> Unit,
    onAddMealToShopping: (List<String>) -> Unit,
    onAddDayToShopping: (List<String>) -> Unit
) {
    val isSelected = day == selectedDay
    val isToday = day == currentDay
    val columnBorderColor = when {
        isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.65f)
        isToday -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.55f)
        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
    }

    val dayShoppingItems = remember(allSlotsForDay) {
        extractShoppingItemsFromSlots(allSlotsForDay)
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
            onClick = onSelectDay,
            onAddDayToShopping = {
                onAddDayToShopping(dayShoppingItems)
            }
        )

        CalendarSlotOrder.forEach { slotType ->
            CalendarGridCell(
                slotUi = slotsForDay[slotType],
                bandHeight = bandHeight,
                onOpenSlotAction = onOpenSlotAction,
                onAddMealToShopping = onAddMealToShopping
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
    onClick: () -> Unit,
    onAddDayToShopping: () -> Unit
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

                Surface(
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    modifier = Modifier.clickable(onClick = onAddDayToShopping)
                ) {
                    Text(
                        text = "🛒",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium
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

@Composable
private fun CalendarGridCell(
    slotUi: WeeklySlotUi?,
    bandHeight: Dp,
    onOpenSlotAction: (slotId: String) -> Unit,
    onAddMealToShopping: (List<String>) -> Unit
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
                onManageClick = { onOpenSlotAction(slotUi.slotId) },
                onAddToShoppingClick = {
                    onAddMealToShopping(
                        extractShoppingItemsFromMealText(slotUi.displayedMealText)
                    )
                }
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

private fun extractShoppingItemsFromSlots(slots: List<WeeklySlotUi>): List<String> {
    return slots
        .flatMap { extractShoppingItemsFromMealText(it.displayedMealText) }
        .distinct()
}

private fun extractShoppingItemsFromMealText(mealText: String): List<String> {
    return parseMealSections(mealText)
        .flatten()
        .map { line ->
            line
                .replace(Regex("^[-•\\s]+"), "")
                .replace(Regex("\\s+"), " ")
                .trim()
                .removeSuffix(".")
        }
        .filter { it.isNotBlank() }
        .distinct()
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