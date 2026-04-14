package it.lagioiaproductions.nutrislot.ui.weeklyplan.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import it.lagioiaproductions.nutrislot.domain.model.MealSlotType
import it.lagioiaproductions.nutrislot.domain.model.WeekDay
import it.lagioiaproductions.nutrislot.ui.weeklyplan.slot.WeeklySlotCard
import it.lagioiaproductions.nutrislot.ui.weeklyplan.slot.WeeklySlotUi
import it.lagioiaproductions.nutrislot.ui.weeklyplan.shopping.extractShoppingItemsFromMealText

@Composable
internal fun CalendarSlotRow(
    slotType: MealSlotType,
    orderedDays: List<WeekDay>,
    selectedDay: WeekDay,
    currentDay: WeekDay,
    isLastRow: Boolean,
    slotsByDayAndType: Map<WeekDay, Map<MealSlotType, WeeklySlotUi?>>,
    onOpenSlotAction: (slotId: String) -> Unit,
    onOpenSlotEdit: (slotId: String) -> Unit,
    onToggleSlotCompleted: (slotId: String) -> Unit,
    onAddMealToShopping: (List<String>) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .heightIn(min = weeklyCalendarMinTimeBandHeight)
    ) {
        TimeRailCell(
            timeLabel = slotTimeLabel(slotType),
            slotLabel = slotType.displayName
        )

        orderedDays.forEach { day ->
            CalendarGridCell(
                slotUi = slotsByDayAndType[day]?.get(slotType),
                isToday = day == currentDay,
                isSelected = day == selectedDay,
                drawTodayBottomBorder = day == currentDay && isLastRow,
                onOpenSlotAction = onOpenSlotAction,
                onOpenSlotEdit = onOpenSlotEdit,
                onToggleSlotCompleted = onToggleSlotCompleted,
                onAddMealToShopping = onAddMealToShopping
            )
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
            .width(weeklyCalendarTimeRailWidth)
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
private fun CalendarGridCell(
    slotUi: WeeklySlotUi?,
    isToday: Boolean,
    isSelected: Boolean,
    drawTodayBottomBorder: Boolean,
    onOpenSlotAction: (slotId: String) -> Unit,
    onOpenSlotEdit: (slotId: String) -> Unit,
    onToggleSlotCompleted: (slotId: String) -> Unit,
    onAddMealToShopping: (List<String>) -> Unit
) {
    val todayBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.88f)
    val todayGlowColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.20f)
    val regularBorderColor = when {
        isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.26f)
        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
    }
    val cellBackgroundColor = when {
        isToday && isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.14f)
        isToday -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.12f)
        isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.10f)
        else -> MaterialTheme.colorScheme.surface
    }

    Box(
        modifier = Modifier
            .width(weeklyCalendarDayColumnWidth)
            .heightIn(min = weeklyCalendarMinTimeBandHeight)
            .background(cellBackgroundColor)
            .then(
                if (isToday) {
                    Modifier.todayColumnOutline(
                        drawTop = false,
                        drawBottom = drawTodayBottomBorder,
                        drawLeft = true,
                        drawRight = true,
                        color = todayBorderColor,
                        glowColor = todayGlowColor
                    )
                } else {
                    Modifier.border(1.dp, regularBorderColor)
                }
            )
            .padding(4.dp)
    ) {
        slotUi?.let { weeklySlot ->
            WeeklySlotCard(
                modifier = Modifier.fillMaxWidth(),
                slotUi = weeklySlot,
                onManageClick = { onOpenSlotAction(weeklySlot.slotId) },
                onEditClick = { onOpenSlotEdit(weeklySlot.slotId) },
                onToggleCompletedClick = { onToggleSlotCompleted(weeklySlot.slotId) },
                onAddToShoppingClick = {
                    onAddMealToShopping(
                        extractShoppingItemsFromMealText(weeklySlot.displayedMealText)
                    )
                }
            )
        }
    }
}
