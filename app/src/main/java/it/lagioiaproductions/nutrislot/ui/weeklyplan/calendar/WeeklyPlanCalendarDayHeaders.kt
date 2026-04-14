package it.lagioiaproductions.nutrislot.ui.weeklyplan

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import it.lagioiaproductions.nutrislot.domain.model.WeekDay

@Composable
internal fun CalendarHeaderRow(
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
            .height(weeklyCalendarDayHeaderHeight)
    ) {
        WeekShoppingHeaderCell(onAddWeekToShopping = onAddWeekToShopping)

        orderedDays.forEach { day ->
            DayHeaderCell(
                day = day,
                isSelected = day == selectedDay,
                isToday = day == currentDay,
                visibleEventsCount = allSlotsByDay[day].orEmpty().size,
                onClick = { onSelectCalendarDay(day) },
                onAddDayToShopping = {
                    onAddDayToShopping(extractShoppingItemsFromSlots(allSlotsByDay[day].orEmpty()))
                }
            )
        }
    }
}

@Composable
private fun WeekShoppingHeaderCell(
    onAddWeekToShopping: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(weeklyCalendarTimeRailWidth)
            .fillMaxHeight()
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
    ) {
        IconButton(onClick = onAddWeekToShopping) {
            Icon(
                imageVector = Icons.Default.ShoppingCart,
                contentDescription = "Aggiungi settimana alla spesa",
                tint = MaterialTheme.colorScheme.primary
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
    onClick: () -> Unit,
    onAddDayToShopping: () -> Unit
) {
    val todayBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.88f)
    val todayGlowColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.20f)
    val backgroundColor = when {
        isToday && isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.82f)
        isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.60f)
        isToday -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.34f)
        else -> MaterialTheme.colorScheme.surface
    }

    Box(
        modifier = Modifier
            .width(weeklyCalendarDayColumnWidth)
            .fillMaxHeight()
            .then(
                if (isToday) {
                    Modifier.todayColumnOutline(
                        drawTop = true,
                        drawBottom = false,
                        drawLeft = true,
                        drawRight = true,
                        color = todayBorderColor,
                        glowColor = todayGlowColor
                    )
                } else {
                    Modifier.border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
                }
            )
            .padding(4.dp)
    ) {
        ElevatedCard(
            onClick = onClick,
            modifier = Modifier.fillMaxSize(),
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
                        if (isToday) append("Oggi - ")
                        append("$visibleEventsCount eventi")
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
