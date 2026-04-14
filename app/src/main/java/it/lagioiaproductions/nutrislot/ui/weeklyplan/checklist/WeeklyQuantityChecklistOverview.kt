package it.lagioiaproductions.nutrislot.ui.weeklyplan.checklist

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
internal fun WeeklyQuantityChecklistOverviewCard(
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
