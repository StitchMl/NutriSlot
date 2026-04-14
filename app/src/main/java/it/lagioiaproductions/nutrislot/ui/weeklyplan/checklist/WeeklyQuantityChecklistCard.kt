package it.lagioiaproductions.nutrislot.ui.weeklyplan.checklist

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
internal fun WeeklyQuantityChecklistCard(
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
internal fun WeeklyChecklistPill(
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

internal fun checklistStatusIcon(
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
internal fun checklistContainerColor(
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
internal fun checklistContentColor(
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
internal fun checklistProgressColor(
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

internal fun checklistCompactHint(
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

internal fun targetEmoji(
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

internal fun periodEmoji(
    item: WeeklyQuantityChecklistItemUi
): String {
    return if (item.period == WeeklyQuantityChecklistPeriodUi.DAILY) "🌞" else "📅"
}

internal fun statusEmoji(
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
