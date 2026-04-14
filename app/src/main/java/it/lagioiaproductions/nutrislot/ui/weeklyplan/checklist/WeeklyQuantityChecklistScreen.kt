@file:Suppress("AssignedValueIsNeverRead")

package it.lagioiaproductions.nutrislot.ui.weeklyplan.checklist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun WeeklyQuantityChecklistScreen(
    items: List<WeeklyQuantityChecklistItemUi>,
    onBackClick: () -> Unit,
    onOpenWaterTracker: () -> Unit = {}
) {
    val hasAttentionItems = items.any { !it.isSatisfied }
    var selectedFilterName by rememberSaveable { mutableStateOf<String?>(null) }
    val selectedFilter = selectedFilterName
        ?.let { name -> ChecklistVisualFilter.entries.firstOrNull { it.name == name } }
        ?: if (hasAttentionItems) ChecklistVisualFilter.ATTENTION else ChecklistVisualFilter.ALL

    val filteredItems = items.filter(selectedFilter::matches)
    val dailyItems = filteredItems.filter { it.period == WeeklyQuantityChecklistPeriodUi.DAILY }
    val weeklyItems = filteredItems.filter { it.period == WeeklyQuantityChecklistPeriodUi.WEEKLY }
    var showAllDaily by rememberSaveable(selectedFilter.name) { mutableStateOf(false) }
    var showAllWeekly by rememberSaveable(selectedFilter.name) { mutableStateOf(false) }
    val maxVisibleItemsPerSection = 4
    val visibleDailyItems = if (showAllDaily || dailyItems.size <= maxVisibleItemsPerSection) {
        dailyItems
    } else {
        dailyItems.take(maxVisibleItemsPerSection)
    }
    val visibleWeeklyItems = if (showAllWeekly || weeklyItems.size <= maxVisibleItemsPerSection) {
        weeklyItems
    } else {
        weeklyItems.take(maxVisibleItemsPerSection)
    }

    Scaffold(
        topBar = {
            WeeklyQuantityChecklistTopBar(onBackClick = onBackClick)
        }
    ) { innerPadding ->
        if (items.isEmpty()) {
            WeeklyQuantityChecklistEmptyState(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    WeeklyQuantityChecklistOverviewCard(
                        items = items,
                        onOpenWaterTracker = onOpenWaterTracker
                    )
                }

                item {
                    WeeklyChecklistFilterRow(
                        items = items,
                        selectedFilter = selectedFilter,
                        onFilterSelected = { filter ->
                            selectedFilterName = filter.name
                        }
                    )
                }

                if (filteredItems.isEmpty()) {
                    item {
                        WeeklyChecklistFilteredEmptyState()
                    }
                } else {
                    if (dailyItems.isNotEmpty()) {
                        item {
                            WeeklyChecklistSectionHeader(
                                title = "🌞 Oggi",
                                subtitle = "Reset giornaliero",
                                items = dailyItems,
                                icon = Icons.Filled.Schedule
                            )
                        }

                        items(visibleDailyItems, key = { it.id }) { item ->
                            WeeklyQuantityChecklistCard(
                                item = item,
                                onOpenWaterTracker = onOpenWaterTracker
                            )
                        }

                        if (dailyItems.size > maxVisibleItemsPerSection) {
                            item {
                                WeeklyChecklistSectionToggle(
                                    hiddenCount = (dailyItems.size - visibleDailyItems.size).coerceAtLeast(
                                        0
                                    ),
                                    expanded = showAllDaily,
                                    onToggle = { showAllDaily = !showAllDaily }
                                )
                            }
                        }
                    }

                    if (weeklyItems.isNotEmpty()) {
                        item {
                            WeeklyChecklistSectionHeader(
                                title = "📅 Settimana",
                                subtitle = "Reset a fine settimana",
                                items = weeklyItems,
                                icon = Icons.Filled.DateRange
                            )
                        }

                        items(visibleWeeklyItems, key = { it.id }) { item ->
                            WeeklyQuantityChecklistCard(
                                item = item,
                                onOpenWaterTracker = onOpenWaterTracker
                            )
                        }

                        if (weeklyItems.size > maxVisibleItemsPerSection) {
                            item {
                                WeeklyChecklistSectionToggle(
                                    hiddenCount = (weeklyItems.size - visibleWeeklyItems.size).coerceAtLeast(
                                        0
                                    ),
                                    expanded = showAllWeekly,
                                    onToggle = { showAllWeekly = !showAllWeekly }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
