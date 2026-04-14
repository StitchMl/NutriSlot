@file:Suppress("SameParameterValue", "SameParameterValue", "SameParameterValue",
    "SameParameterValue", "SameParameterValue"
)

package it.lagioiaproductions.nutrislot.ui.weeklyplan.calendar

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import it.lagioiaproductions.nutrislot.domain.model.MealSlotType
import it.lagioiaproductions.nutrislot.domain.model.WeekDay
import it.lagioiaproductions.nutrislot.ui.weeklyplan.state.WeeklyPlanUiState
import it.lagioiaproductions.nutrislot.ui.weeklyplan.slot.WeeklySlotUi
import it.lagioiaproductions.nutrislot.ui.weeklyplan.shopping.extractShoppingItemsFromSlots
import it.lagioiaproductions.nutrislot.ui.weeklyplan.state.orderedCalendarDays

internal val weeklyCalendarSlotOrder = listOf(
    MealSlotType.BREAKFAST,
    MealSlotType.MORNING_SNACK,
    MealSlotType.LUNCH,
    MealSlotType.AFTERNOON_SNACK,
    MealSlotType.DINNER
)

internal val weeklyCalendarTimeRailWidth = 78.dp
internal val weeklyCalendarDayColumnWidth = 220.dp
internal val weeklyCalendarDayHeaderHeight = 88.dp
internal val weeklyCalendarMinTimeBandHeight = 188.dp
internal val weeklyCalendarBottomScrollPadding = 104.dp

internal data class WeeklyCalendarGridData(
    val slotsByDayAndType: Map<WeekDay, Map<MealSlotType, WeeklySlotUi?>>,
    val allSlotsByDay: Map<WeekDay, List<WeeklySlotUi>>,
    val weekShoppingItems: List<String>
)

@Composable
internal fun rememberWeeklyCalendarGridData(
    uiState: WeeklyPlanUiState
): WeeklyCalendarGridData {
    val visibleSlots = remember(uiState.slots, uiState.showConsumedSlotsInCalendar) {
        if (uiState.showConsumedSlotsInCalendar) {
            uiState.slots
        } else {
            uiState.slots.filterNot { it.isActuallyCompletedThisWeek }
        }
    }

    val slotsByDayAndType = remember(visibleSlots, orderedCalendarDays) {
        orderedCalendarDays.associateWith { day ->
            weeklyCalendarSlotOrder.associateWith { slotType ->
                visibleSlots.firstOrNull { it.dayOfWeek == day && it.mealSlotType == slotType }
            }
        }
    }

    val allSlotsByDay = remember(uiState.slots, orderedCalendarDays) {
        orderedCalendarDays.associateWith { day ->
            uiState.slots
                .filter { it.dayOfWeek == day }
                .sortedBy { it.mealSlotType.sortOrder }
        }
    }

    val weekShoppingItems = remember(allSlotsByDay, orderedCalendarDays) {
        orderedCalendarDays
            .flatMap { day -> extractShoppingItemsFromSlots(allSlotsByDay[day].orEmpty()) }
            .distinct()
    }

    return WeeklyCalendarGridData(
        slotsByDayAndType = slotsByDayAndType,
        allSlotsByDay = allSlotsByDay,
        weekShoppingItems = weekShoppingItems
    )
}

internal fun slotTimeLabel(slotType: MealSlotType): String {
    return when (slotType) {
        MealSlotType.BREAKFAST -> "07:30"
        MealSlotType.MORNING_SNACK -> "10:30"
        MealSlotType.LUNCH -> "13:00"
        MealSlotType.AFTERNOON_SNACK -> "16:30"
        MealSlotType.DINNER -> "20:00"
    }
}

internal fun Modifier.todayColumnOutline(
    drawTop: Boolean,
    drawBottom: Boolean,
    drawLeft: Boolean,
    drawRight: Boolean,
    color: Color,
    glowColor: Color
): Modifier = this.drawBehind {
    val glowStroke = 7.dp.toPx()
    val borderStroke = 3.dp.toPx()
    val glowHalf = glowStroke / 2f
    val borderHalf = borderStroke / 2f

    fun drawEdges(strokeWidth: Float, halfStroke: Float, edgeColor: Color) {
        if (drawLeft) {
            drawLine(
                color = edgeColor,
                start = Offset(halfStroke, 0f),
                end = Offset(halfStroke, size.height),
                strokeWidth = strokeWidth
            )
        }
        if (drawRight) {
            drawLine(
                color = edgeColor,
                start = Offset(size.width - halfStroke, 0f),
                end = Offset(size.width - halfStroke, size.height),
                strokeWidth = strokeWidth
            )
        }
        if (drawTop) {
            drawLine(
                color = edgeColor,
                start = Offset(0f, halfStroke),
                end = Offset(size.width, halfStroke),
                strokeWidth = strokeWidth
            )
        }
        if (drawBottom) {
            drawLine(
                color = edgeColor,
                start = Offset(0f, size.height - halfStroke),
                end = Offset(size.width, size.height - halfStroke),
                strokeWidth = strokeWidth
            )
        }
    }

    drawEdges(glowStroke, glowHalf, glowColor)
    drawEdges(borderStroke, borderHalf, color)
}
