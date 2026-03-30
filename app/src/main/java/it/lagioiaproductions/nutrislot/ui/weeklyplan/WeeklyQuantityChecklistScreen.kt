package it.lagioiaproductions.nutrislot.ui.weeklyplan

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun WeeklyQuantityChecklistScreen(
    items: List<WeeklyQuantityChecklistItemUi>,
    onBackClick: () -> Unit,
    onOpenWaterTracker: () -> Unit = {}
) {
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
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    WeeklyQuantityChecklistOverviewCard(
                        items = items,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }

                items(items, key = { it.id }) { item ->
                    WeeklyQuantityChecklistCard(
                        item = item,
                        onOpenWaterTracker = onOpenWaterTracker,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp)
                    )
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
                text = "Target di consumo",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Conteggio automatico dei target giornalieri e settimanali, con l'acqua collegata alla scheda dedicata.",
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
                text = "Nessun target disponibile",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Qui compariranno i target giornalieri e settimanali rilevati dal PDF o dalle note del piano.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun WeeklyQuantityChecklistOverviewCard(
    items: List<WeeklyQuantityChecklistItemUi>,
    modifier: Modifier = Modifier
) {
    val satisfiedCount = items.count { it.isSatisfied }
    val alertCount = items.count { item ->
        item.status == WeeklyQuantityChecklistStatusUi.UNDER_TARGET ||
                item.status == WeeklyQuantityChecklistStatusUi.OVER_LIMIT
    }
    val focusedCount = items.count { item ->
        item.status == WeeklyQuantityChecklistStatusUi.UNDER_TARGET
    }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.94f),
                            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.78f),
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.98f)
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Controllo dei target",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "$satisfiedCount/${items.size}",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "I target si aggiornano in base ai box del piano completati e, per l'acqua, usando la scheda acqua.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    WeeklyChecklistPill(
                        text = "$focusedCount da completare",
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.78f),
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                    WeeklyChecklistPill(
                        text = "$alertCount da controllare",
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.78f),
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                    WeeklyChecklistPill(
                        text = "${items.size - alertCount} in ordine",
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.8f),
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun WeeklyQuantityChecklistCard(
    item: WeeklyQuantityChecklistItemUi,
    onOpenWaterTracker: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = checklistContainerColor(item.status)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = item.targetDescription,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                WeeklyChecklistStatusBadge(item = item)
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                WeeklyChecklistPill(
                    text = item.sourceLabel,
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f),
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )

                WeeklyChecklistPill(
                    text = item.periodDescription.replaceFirstChar { char ->
                        if (char.isLowerCase()) char.titlecase() else char.toString()
                    },
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.68f),
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )

                item.portionText
                    ?.takeIf { it.isNotBlank() }
                    ?.let { portionText ->
                        WeeklyChecklistPill(
                            text = portionText,
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.statusLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = checklistContentColor(item.status)
                    )
                    Text(
                        text = item.progressLabel,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                LinearProgressIndicator(
                    progress = { item.progressRatio.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth(),
                    color = checklistProgressColor(item.status),
                    trackColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.68f)
                )

                Text(
                    text = item.progressTrackingLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = checklistSupportingText(item),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (item.hasLinkedWaterTracking) {
                TextButton(
                    onClick = onOpenWaterTracker
                ) {
                    Text(
                        text = "Apri scheda acqua",
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun WeeklyChecklistStatusBadge(
    item: WeeklyQuantityChecklistItemUi
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = checklistBadgeContainerColor(item.status)
    ) {
        Text(
            text = item.statusLabel,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            color = checklistBadgeContentColor(item.status)
        )
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
            color = contentColor
        )
    }
}

@Composable
private fun checklistContainerColor(
    status: WeeklyQuantityChecklistStatusUi
): Color {
    return when (status) {
        WeeklyQuantityChecklistStatusUi.UNDER_TARGET -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.62f)
        WeeklyQuantityChecklistStatusUi.ON_TRACK -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.58f)
        WeeklyQuantityChecklistStatusUi.COMPLETED -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.58f)
        WeeklyQuantityChecklistStatusUi.LIMIT_REACHED -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.72f)
        WeeklyQuantityChecklistStatusUi.OVER_LIMIT -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.72f)
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

@Composable
private fun checklistBadgeContainerColor(
    status: WeeklyQuantityChecklistStatusUi
): Color {
    return when (status) {
        WeeklyQuantityChecklistStatusUi.UNDER_TARGET -> MaterialTheme.colorScheme.surface
        WeeklyQuantityChecklistStatusUi.ON_TRACK -> MaterialTheme.colorScheme.surface
        WeeklyQuantityChecklistStatusUi.COMPLETED -> MaterialTheme.colorScheme.surface
        WeeklyQuantityChecklistStatusUi.LIMIT_REACHED -> MaterialTheme.colorScheme.surface
        WeeklyQuantityChecklistStatusUi.OVER_LIMIT -> MaterialTheme.colorScheme.error
    }
}

@Composable
private fun checklistBadgeContentColor(
    status: WeeklyQuantityChecklistStatusUi
): Color {
    return when (status) {
        WeeklyQuantityChecklistStatusUi.UNDER_TARGET -> MaterialTheme.colorScheme.primary
        WeeklyQuantityChecklistStatusUi.ON_TRACK -> MaterialTheme.colorScheme.secondary
        WeeklyQuantityChecklistStatusUi.COMPLETED -> MaterialTheme.colorScheme.tertiary
        WeeklyQuantityChecklistStatusUi.LIMIT_REACHED -> MaterialTheme.colorScheme.tertiary
        WeeklyQuantityChecklistStatusUi.OVER_LIMIT -> MaterialTheme.colorScheme.onError
    }
}

private fun checklistSupportingText(
    item: WeeklyQuantityChecklistItemUi
): String {
    return when (item.status) {
        WeeklyQuantityChecklistStatusUi.UNDER_TARGET -> {
            if (item.metric == WeeklyQuantityChecklistMetricUi.MILLILITERS) {
                "Serve ancora acqua registrata nella scheda acqua per chiudere il target di ${item.periodDescription}."
            } else if (item.remainingMinimumValue == 1) {
                "Serve ancora 1 pasto compatibile per chiudere il target minimo di ${item.periodDescription}."
            } else {
                "Servono ancora ${item.remainingMinimumValue} elementi compatibili per chiudere il target minimo di ${item.periodDescription}."
            }
        }

        WeeklyQuantityChecklistStatusUi.ON_TRACK -> {
            if (item.minimumTargetValue != null) {
                "Hai raggiunto il minimo e sei ancora dentro il range consigliato per ${item.periodDescription}."
            } else {
                "Sei ancora dentro il limite consigliato per ${item.periodDescription}."
            }
        }

        WeeklyQuantityChecklistStatusUi.COMPLETED -> {
            if (item.hasLinkedWaterTracking) {
                "Obiettivo centrato usando i dati registrati nella scheda acqua."
            } else {
                "Obiettivo minimo centrato per ${item.periodDescription}."
            }
        }

        WeeklyQuantityChecklistStatusUi.LIMIT_REACHED -> {
            if (item.minimumTargetValue != null && item.maximumTargetValue != null && !item.isExactTarget) {
                "Range completato senza superare il limite previsto."
            } else {
                "Hai raggiunto il limite previsto senza superarlo."
            }
        }

        WeeklyQuantityChecklistStatusUi.OVER_LIMIT -> {
            if (item.hasLinkedWaterTracking) {
                "Hai superato il limite configurato per l'acqua in ${item.periodDescription}."
            } else {
                "Hai superato il limite consigliato per ${item.periodDescription}."
            }
        }
    }
}
