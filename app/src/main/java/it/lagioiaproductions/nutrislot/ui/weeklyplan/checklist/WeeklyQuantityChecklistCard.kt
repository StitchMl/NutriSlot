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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/** Main checklist card used to summarize one weekly target and its progress. */
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
                    text = "\uD83D\uDCC4 ${item.sourceLabel}",
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )

                item.portionText
                    ?.takeIf { it.isNotBlank() }
                    ?.let { portionText ->
                        WeeklyChecklistPill(
                            text = "\uD83D\uDCCF $portionText",
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
                        text = "\uD83D\uDCA7 Apri scheda acqua",
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

/** Small pill token reused across checklist cards for metadata labels. */
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
