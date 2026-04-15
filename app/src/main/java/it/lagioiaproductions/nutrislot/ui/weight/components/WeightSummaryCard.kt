package it.lagioiaproductions.nutrislot.ui.weight.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import it.lagioiaproductions.nutrislot.ui.shared.WeightEntryUi
import it.lagioiaproductions.nutrislot.ui.shared.WeightSummaryUi
import it.lagioiaproductions.nutrislot.ui.weight.support.formatDelta
import it.lagioiaproductions.nutrislot.ui.weight.support.formatFullDate
import it.lagioiaproductions.nutrislot.ui.weight.support.formatWeight
import it.lagioiaproductions.nutrislot.ui.weight.support.recentAverageWeight

/** Hero summary for the weight feature with latest value, delta and compact KPIs. */
@Composable
internal fun WeightSummaryCard(
    summary: WeightSummaryUi,
    entries: List<WeightEntryUi>,
    totalEntries: Int
) {
    val latestEntry = entries.maxByOrNull { it.createdAtEpochMillis }
    val latestText = summary.latestWeightKg?.let { "${formatWeight(it)} kg" } ?: "--"
    val delta = summary.deltaFromPreviousKg
    val deltaText = delta?.let { formatDelta(it) } ?: "--"
    val averageText = recentAverageWeight(entries)?.let { "${formatWeight(it)} kg" } ?: "--"
    val deltaAccent = when {
        delta == null -> MaterialTheme.colorScheme.primary
        delta < 0f -> Color(0xFF0F766E)
        delta > 0f -> Color(0xFFB45309)
        else -> MaterialTheme.colorScheme.primary
    }
    val deltaSupport = when {
        delta == null -> "Prima base"
        delta < 0f -> "In discesa"
        delta > 0f -> "In salita"
        else -> "Stabile"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.95f),
                            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.72f),
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Memoria peso",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = latestText,
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = latestEntry?.let { "Ultima registrazione ${formatFullDate(it.dateKey)}" }
                        ?: "Le misurazioni salvate restano disponibili qui.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                latestEntry?.note?.takeIf { it.isNotBlank() }?.let { note ->
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f)
                    ) {
                        Text(
                            text = note,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    WeightMetricBadge(
                        title = "Delta",
                        value = deltaText,
                        supporting = deltaSupport,
                        accent = deltaAccent,
                        modifier = Modifier.weight(1f)
                    )
                    WeightMetricBadge(
                        title = "Media 7",
                        value = averageText,
                        supporting = "Ultime misure",
                        accent = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    WeightMetricBadge(
                        title = "Totale",
                        value = totalEntries.toString(),
                        supporting = "Salvate",
                        accent = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

/** Reusable KPI badge used by the weight summary and trend cards. */
@Composable
internal fun WeightMetricBadge(
    title: String,
    value: String,
    supporting: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = accent
            )
            Text(
                text = supporting,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
