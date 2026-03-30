package it.lagioiaproductions.nutrislot.ui.weeklyplan

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
                                    hiddenCount = (dailyItems.size - visibleDailyItems.size).coerceAtLeast(0),
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
                                    hiddenCount = (weeklyItems.size - visibleWeeklyItems.size).coerceAtLeast(0),
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

@Composable
private fun WeeklyQuantityChecklistTopBar(
    onBackClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBackClick) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Indietro"
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = "🎯 Target di consumo",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Meno testo, più colpo d'occhio.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun WeeklyQuantityChecklistEmptyState(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "🧾 Nessun target disponibile",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Qui compariranno i target del tuo piano.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun WeeklyQuantityChecklistOverviewCard(
    items: List<WeeklyQuantityChecklistItemUi>,
    onOpenWaterTracker: () -> Unit
) {
    val satisfiedCount = items.count { it.isSatisfied }
    val attentionItems = items.filterNot { it.isSatisfied }
    val dailyItems = items.filter { it.period == WeeklyQuantityChecklistPeriodUi.DAILY }
    val weeklyItems = items.filter { it.period == WeeklyQuantityChecklistPeriodUi.WEEKLY }
    val focusItem = attentionItems.firstOrNull()
    val waterItem = items.firstOrNull { it.hasLinkedWaterTracking }

    Card(
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.96f),
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.78f),
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.98f)
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "✨ Panoramica veloce",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "$satisfiedCount/${items.size}",
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (attentionItems.isEmpty()) {
                                "✅ Tutto in ordine"
                            } else {
                                "⚠️ ${attentionItems.size} da seguire"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (waterItem != null) {
                        FilledTonalButton(
                            onClick = onOpenWaterTracker
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Opacity,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("💧 Acqua")
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    WeeklyChecklistStatCard(
                        modifier = Modifier.weight(1f),
                        value = "$satisfiedCount/${items.size}",
                        label = "✅ Ok",
                        supporting = "${items.count { it.status == WeeklyQuantityChecklistStatusUi.COMPLETED || it.status == WeeklyQuantityChecklistStatusUi.LIMIT_REACHED }} centrati"
                    )
                    WeeklyChecklistStatCard(
                        modifier = Modifier.weight(1f),
                        value = attentionItems.size.toString(),
                        label = "⚠️ Focus",
                        supporting = if (attentionItems.isEmpty()) "Zero urgenze" else "Guarda qui"
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    WeeklyChecklistStatCard(
                        modifier = Modifier.weight(1f),
                        value = dailyItems.size.toString(),
                        label = "🌞 Oggi",
                        supporting = "${dailyItems.count { !it.isSatisfied }} focus"
                    )
                    WeeklyChecklistStatCard(
                        modifier = Modifier.weight(1f),
                        value = weeklyItems.size.toString(),
                        label = "📅 Week",
                        supporting = "${weeklyItems.count { !it.isSatisfied }} focus"
                    )
                }

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.24f)
                )

                if (focusItem != null) {
                    WeeklyChecklistFocusCard(item = focusItem)
                } else {
                    Text(
                        text = "🔄 Giornalieri e settimanali si aggiornano da soli.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun WeeklyChecklistFilterRow(
    items: List<WeeklyQuantityChecklistItemUi>,
    selectedFilter: ChecklistVisualFilter,
    onFilterSelected: (ChecklistVisualFilter) -> Unit
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ChecklistVisualFilter.entries.forEach { filter ->
            val count = filter.countOf(items)
            FilterChip(
                selected = filter == selectedFilter,
                onClick = { onFilterSelected(filter) },
                label = {
                    Text(
                        text = if (count > 0) {
                            "${filter.emoji} ${filter.label} $count"
                        } else {
                            "${filter.emoji} ${filter.label}"
                        }
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    }
}

@Composable
private fun WeeklyChecklistFilteredEmptyState() {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Text(
            text = "🫥 Nessun target in questo filtro",
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun WeeklyChecklistStatCard(
    modifier: Modifier = Modifier,
    value: String,
    label: String,
    supporting: String
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.76f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = supporting,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun WeeklyChecklistFocusCard(
    item: WeeklyQuantityChecklistItemUi
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = checklistContainerColor(item.status).copy(alpha = 0.92f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Surface(
                shape = CircleShape,
                color = checklistContentColor(item.status).copy(alpha = 0.14f)
            ) {
                Icon(
                    imageVector = checklistStatusIcon(item),
                    contentDescription = null,
                    modifier = Modifier.padding(10.dp),
                    tint = checklistContentColor(item.status)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "⚡ Da guardare adesso",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${targetEmoji(item)} ${item.title}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = checklistCompactHint(item),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun WeeklyChecklistSectionToggle(
    hiddenCount: Int,
    expanded: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        TextButton(onClick = onToggle) {
            Text(
                text = if (expanded) {
                    "Riduci"
                } else {
                    "Mostra altri $hiddenCount"
                },
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun WeeklyChecklistSectionHeader(
    title: String,
    subtitle: String,
    items: List<WeeklyQuantityChecklistItemUi>,
    icon: ImageVector
) {
    val attentionCount = items.count { !it.isSatisfied }

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.padding(10.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            WeeklyChecklistPill(
                text = if (attentionCount == 0) {
                    "OK ${items.size}"
                } else {
                    "Focus $attentionCount"
                },
                containerColor = if (attentionCount == 0) {
                    MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.88f)
                } else {
                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.82f)
                },
                contentColor = if (attentionCount == 0) {
                    MaterialTheme.colorScheme.onTertiaryContainer
                } else {
                    MaterialTheme.colorScheme.onErrorContainer
                }
            )
        }
    }
}

@Composable
private fun WeeklyQuantityChecklistCard(
    item: WeeklyQuantityChecklistItemUi,
    onOpenWaterTracker: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = checklistContainerColor(item.status)
        ),
        border = BorderStroke(
            width = 1.dp,
            color = checklistContentColor(item.status).copy(alpha = 0.14f)
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Surface(
                    shape = CircleShape,
                    color = checklistContentColor(item.status).copy(alpha = 0.14f)
                ) {
                    Icon(
                        imageVector = checklistStatusIcon(item),
                        contentDescription = null,
                        modifier = Modifier.padding(10.dp),
                        tint = checklistContentColor(item.status)
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "${targetEmoji(item)} ${item.title}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = item.targetDescription,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                WeeklyChecklistPill(
                    text = "${periodEmoji(item)} " + item.periodDescription.replaceFirstChar { char ->
                        if (char.isLowerCase()) char.titlecase() else char.toString()
                    },
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "${statusEmoji(item.status)} ${item.statusLabel}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = checklistContentColor(item.status)
                    )
                    Text(
                        text = checklistCompactHint(item),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "Fatto",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = item.progressLabel,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LinearProgressIndicator(
                    progress = { item.progressRatio.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp),
                    color = checklistProgressColor(item.status),
                    trackColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.66f)
                )

                Text(
                    text = item.progressTrackingLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                WeeklyChecklistPill(
                    text = "📄 ${item.sourceLabel}",
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )

                item.portionText
                    ?.takeIf { it.isNotBlank() }
                    ?.let { portionText ->
                        WeeklyChecklistPill(
                            text = "📏 $portionText",
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.74f),
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
            }

            if (item.hasLinkedWaterTracking) {
                FilledTonalButton(
                    onClick = onOpenWaterTracker
                ) {
                    Icon(
                        imageVector = Icons.Filled.Opacity,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "💧 Apri scheda acqua",
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun WeeklyChecklistPill(
    text: String,
    containerColor: Color,
    contentColor: Color
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = containerColor
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelMedium,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun checklistStatusIcon(
    item: WeeklyQuantityChecklistItemUi
): ImageVector {
    return when {
        item.hasLinkedWaterTracking -> Icons.Filled.Opacity
        item.status == WeeklyQuantityChecklistStatusUi.UNDER_TARGET ||
            item.status == WeeklyQuantityChecklistStatusUi.OVER_LIMIT -> Icons.Filled.ErrorOutline
        else -> Icons.Filled.CheckCircle
    }
}

@Composable
private fun checklistContainerColor(
    status: WeeklyQuantityChecklistStatusUi
): Color {
    return when (status) {
        WeeklyQuantityChecklistStatusUi.UNDER_TARGET -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.56f)
        WeeklyQuantityChecklistStatusUi.ON_TRACK -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
        WeeklyQuantityChecklistStatusUi.COMPLETED -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.48f)
        WeeklyQuantityChecklistStatusUi.LIMIT_REACHED -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.62f)
        WeeklyQuantityChecklistStatusUi.OVER_LIMIT -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.64f)
    }
}

@Composable
private fun checklistContentColor(
    status: WeeklyQuantityChecklistStatusUi
): Color {
    return when (status) {
        WeeklyQuantityChecklistStatusUi.UNDER_TARGET -> MaterialTheme.colorScheme.primary
        WeeklyQuantityChecklistStatusUi.ON_TRACK -> MaterialTheme.colorScheme.secondary
        WeeklyQuantityChecklistStatusUi.COMPLETED -> MaterialTheme.colorScheme.tertiary
        WeeklyQuantityChecklistStatusUi.LIMIT_REACHED -> MaterialTheme.colorScheme.tertiary
        WeeklyQuantityChecklistStatusUi.OVER_LIMIT -> MaterialTheme.colorScheme.error
    }
}

@Composable
private fun checklistProgressColor(
    status: WeeklyQuantityChecklistStatusUi
): Color {
    return when (status) {
        WeeklyQuantityChecklistStatusUi.UNDER_TARGET -> MaterialTheme.colorScheme.primary
        WeeklyQuantityChecklistStatusUi.ON_TRACK -> MaterialTheme.colorScheme.secondary
        WeeklyQuantityChecklistStatusUi.COMPLETED -> MaterialTheme.colorScheme.tertiary
        WeeklyQuantityChecklistStatusUi.LIMIT_REACHED -> MaterialTheme.colorScheme.tertiary
        WeeklyQuantityChecklistStatusUi.OVER_LIMIT -> MaterialTheme.colorScheme.error
    }
}

private enum class ChecklistVisualFilter(
    val emoji: String,
    val label: String
) {
    ATTENTION("⚠️", "Focus"),
    DAILY("🌞", "Oggi"),
    WEEKLY("📅", "Week"),
    WATER("💧", "Acqua"),
    ALL("🎯", "Tutti");

    fun matches(item: WeeklyQuantityChecklistItemUi): Boolean {
        return when (this) {
            ATTENTION -> !item.isSatisfied
            DAILY -> item.period == WeeklyQuantityChecklistPeriodUi.DAILY
            WEEKLY -> item.period == WeeklyQuantityChecklistPeriodUi.WEEKLY
            WATER -> item.hasLinkedWaterTracking
            ALL -> true
        }
    }

    fun countOf(items: List<WeeklyQuantityChecklistItemUi>): Int {
        return items.count(::matches)
    }
}

private fun checklistCompactHint(
    item: WeeklyQuantityChecklistItemUi
): String {
    return when (item.status) {
        WeeklyQuantityChecklistStatusUi.UNDER_TARGET -> {
            when (item.metric) {
                WeeklyQuantityChecklistMetricUi.MILLILITERS -> "Ancora ${shortMlLabel(item.remainingMinimumValue)}"
                WeeklyQuantityChecklistMetricUi.PORTIONS -> "Ancora ${item.remainingMinimumValue} porz."
                WeeklyQuantityChecklistMetricUi.OCCURRENCES -> "Ancora ${item.remainingMinimumValue} volte"
            }
        }

        WeeklyQuantityChecklistStatusUi.ON_TRACK -> "Sei nel range"
        WeeklyQuantityChecklistStatusUi.COMPLETED -> "Fatto"
        WeeklyQuantityChecklistStatusUi.LIMIT_REACHED -> "Perfetto"
        WeeklyQuantityChecklistStatusUi.OVER_LIMIT -> "Oltre il limite"
    }
}

private fun shortMlLabel(
    value: Int
): String {
    return if (value >= 1000 && value % 1000 == 0) {
        "${value / 1000}L"
    } else {
        "$value ml"
    }
}

private fun targetEmoji(
    item: WeeklyQuantityChecklistItemUi
): String {
    val normalizedKey = "${item.id} ${item.title}".lowercase()
    return when {
        normalizedKey.contains("acqua") -> "💧"
        normalizedKey.contains("frutta") || normalizedKey.contains("verdura") -> "🥦"
        normalizedKey.contains("caffe") || normalizedKey.contains("th") -> "☕"
        normalizedKey.contains("carne bianca") -> "🍗"
        normalizedKey.contains("carne rossa") -> "🥩"
        normalizedKey.contains("affettati") -> "🥓"
        normalizedKey.contains("uova") -> "🥚"
        normalizedKey.contains("formaggi") -> "🧀"
        normalizedKey.contains("patate") -> "🥔"
        normalizedKey.contains("piatto unico") || normalizedKey.contains("legumi") -> "🍲"
        normalizedKey.contains("pesce") -> "🐟"
        item.period == WeeklyQuantityChecklistPeriodUi.DAILY -> "🌞"
        else -> "📌"
    }
}

private fun periodEmoji(
    item: WeeklyQuantityChecklistItemUi
): String {
    return if (item.period == WeeklyQuantityChecklistPeriodUi.DAILY) "🌞" else "📅"
}

private fun statusEmoji(
    status: WeeklyQuantityChecklistStatusUi
): String {
    return when (status) {
        WeeklyQuantityChecklistStatusUi.UNDER_TARGET -> "⚠️"
        WeeklyQuantityChecklistStatusUi.ON_TRACK -> "🟢"
        WeeklyQuantityChecklistStatusUi.COMPLETED -> "✅"
        WeeklyQuantityChecklistStatusUi.LIMIT_REACHED -> "🎯"
        WeeklyQuantityChecklistStatusUi.OVER_LIMIT -> "🚫"
    }
}

