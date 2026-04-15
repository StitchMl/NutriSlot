package it.lagioiaproductions.nutrislot.ui.importpreview.layout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import it.lagioiaproductions.nutrislot.domain.model.MealSlotType
import it.lagioiaproductions.nutrislot.domain.model.WeekDay
import it.lagioiaproductions.nutrislot.ui.importfile.state.EditableImportedMealCellUi
import it.lagioiaproductions.nutrislot.ui.importpreview.components.EditableMealCellCard
import it.lagioiaproductions.nutrislot.ui.importpreview.state.PreviewSlotOrder
import it.lagioiaproductions.nutrislot.ui.weeklyplan.calendar.slotTimeLabel

/** Calendar header that renders visible days and their filled slot counters. */
@Composable
internal fun PreviewCalendarHeaderRow(
    visibleDays: List<WeekDay>,
    filledCountByDay: Map<WeekDay, Int>
) {
    Row(
        modifier = Modifier.height(DayHeaderHeight)
    ) {
        Spacer(
            modifier = Modifier
                .width(TimeRailWidth)
                .height(DayHeaderHeight)
        )

        visibleDays.forEach { day ->
            val filledCount = filledCountByDay[day] ?: 0

            Surface(
                modifier = Modifier
                    .width(DayColumnWidth)
                    .height(DayHeaderHeight)
                    .padding(start = 8.dp),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainer
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
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
                    Text(
                        text = "$filledCount/${PreviewSlotOrder.size}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/** Calendar body row for a single meal slot across all visible days. */
@Composable
internal fun PreviewCalendarBodyRow(
    slotType: MealSlotType,
    visibleDays: List<WeekDay>,
    cellsByDayAndSlot: Map<WeekDay, Map<MealSlotType, EditableImportedMealCellUi?>>,
    onCellClick: (EditableImportedMealCellUi) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = PreviewRowMinHeight)
            .padding(top = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            modifier = Modifier
                .width(TimeRailWidth)
                .heightIn(min = PreviewRowMinHeight),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = slotTimeLabel(slotType),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = slotType.displayName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        visibleDays.forEach { day ->
            val cell = cellsByDayAndSlot[day]?.get(slotType)

            EditableMealCellCard(
                modifier = Modifier
                    .width(DayColumnWidth)
                    .padding(start = 8.dp),
                cell = cell,
                slotType = slotType,
                onClick = {
                    if (cell != null) onCellClick(cell)
                }
            )
        }
    }
}
