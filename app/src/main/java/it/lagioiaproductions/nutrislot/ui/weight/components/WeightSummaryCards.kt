package it.lagioiaproductions.nutrislot.ui.weight.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import it.lagioiaproductions.nutrislot.ui.shared.WeightEntryUi
import it.lagioiaproductions.nutrislot.ui.shared.WeightSummaryUi
import it.lagioiaproductions.nutrislot.ui.weight.support.formatDelta
import it.lagioiaproductions.nutrislot.ui.weight.support.formatFullDate
import it.lagioiaproductions.nutrislot.ui.weight.support.formatWeight
import it.lagioiaproductions.nutrislot.ui.weight.support.recentAverageWeight
import kotlin.math.abs

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

@Composable
internal fun WeightTrendCard(
    entries: List<WeightEntryUi>
) {
    val chartEntries = entries
        .sortedBy { it.createdAtEpochMillis }
        .takeLast(12)
    val recentAverage = recentAverageWeight(entries)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Trend",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Vista rapida dell'andamento delle ultime misurazioni.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.75f)
                ) {
                    Text(
                        text = "${chartEntries.size.coerceAtLeast(1)} punti",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    WeightTrendLegend()
                    WeightTrendChart(entries = entries)
                }
            }

            if (entries.isNotEmpty()) {
                val min = entries.minOf { it.weightKg }
                val max = entries.maxOf { it.weightKg }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    WeightMetricBadge(
                        title = "Min",
                        value = "${formatWeight(min)} kg",
                        supporting = "Storico",
                        accent = Color(0xFF2563EB),
                        modifier = Modifier.weight(1f)
                    )
                    WeightMetricBadge(
                        title = "Max",
                        value = "${formatWeight(max)} kg",
                        supporting = "Storico",
                        accent = Color(0xFF7C3AED),
                        modifier = Modifier.weight(1f)
                    )
                    WeightMetricBadge(
                        title = "Media",
                        value = recentAverage?.let { "${formatWeight(it)} kg" } ?: "--",
                        supporting = "Recente",
                        accent = Color(0xFF0F766E),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun WeightMetricBadge(
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

@Composable
private fun WeightTrendLegend() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        WeightLegendDot(
            label = "Andamento",
            color = MaterialTheme.colorScheme.primary
        )
        WeightLegendDot(
            label = "Punti",
            color = MaterialTheme.colorScheme.tertiary
        )
    }
}

@Composable
private fun WeightLegendDot(
    label: String,
    color: Color
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(10.dp)
                .height(10.dp)
                .background(color = color, shape = CircleShape)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun WeightTrendChart(
    entries: List<WeightEntryUi>
) {
    val chartEntries = entries
        .sortedBy { it.createdAtEpochMillis }
        .takeLast(12)

    if (chartEntries.size < 2) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Aggiungi almeno 2 misurazioni per vedere il trend.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val lineColor = MaterialTheme.colorScheme.primary
    val pointColor = MaterialTheme.colorScheme.tertiary
    val gridColor = MaterialTheme.colorScheme.outlineVariant

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
    ) {
        val minWeight = chartEntries.minOf { it.weightKg }
        val maxWeight = chartEntries.maxOf { it.weightKg }
        val range = (maxWeight - minWeight).takeIf { abs(it) > 0.001f } ?: 1f

        val leftPadding = 16.dp.toPx()
        val rightPadding = 16.dp.toPx()
        val topPadding = 20.dp.toPx()
        val bottomPadding = 20.dp.toPx()

        val chartWidth = size.width - leftPadding - rightPadding
        val chartHeight = size.height - topPadding - bottomPadding

        drawLine(
            color = gridColor,
            start = Offset(leftPadding, size.height - bottomPadding),
            end = Offset(size.width - rightPadding, size.height - bottomPadding),
            strokeWidth = 2f
        )

        drawLine(
            color = gridColor.copy(alpha = 0.5f),
            start = Offset(leftPadding, topPadding + chartHeight / 2f),
            end = Offset(size.width - rightPadding, topPadding + chartHeight / 2f),
            strokeWidth = 1f
        )

        val points = chartEntries.mapIndexed { index, entry ->
            val x = leftPadding + (chartWidth * index / chartEntries.lastIndex.toFloat())
            val normalized = (entry.weightKg - minWeight) / range
            val y = topPadding + chartHeight - (normalized * chartHeight)
            Offset(x, y)
        }

        val linePath = Path().apply {
            moveTo(points.first().x, points.first().y)
            points.drop(1).forEach { point ->
                lineTo(point.x, point.y)
            }
        }

        val fillPath = Path().apply {
            moveTo(points.first().x, size.height - bottomPadding)
            lineTo(points.first().x, points.first().y)
            points.drop(1).forEach { point ->
                lineTo(point.x, point.y)
            }
            lineTo(points.last().x, size.height - bottomPadding)
            close()
        }

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    lineColor.copy(alpha = 0.22f),
                    lineColor.copy(alpha = 0.02f)
                ),
                startY = topPadding,
                endY = size.height - bottomPadding
            )
        )

        drawPath(
            path = linePath,
            color = lineColor,
            style = Stroke(width = 4.dp.toPx())
        )

        points.forEach { point ->
            drawCircle(
                color = pointColor.copy(alpha = 0.24f),
                radius = 10.dp.toPx(),
                center = point
            )
            drawCircle(
                color = pointColor,
                radius = 5.dp.toPx(),
                center = point
            )
        }
    }
}
