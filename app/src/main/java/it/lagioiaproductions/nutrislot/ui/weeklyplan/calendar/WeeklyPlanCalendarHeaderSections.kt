package it.lagioiaproductions.nutrislot.ui.weeklyplan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Update
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
internal fun LoadedPlanTopBar(
    title: String,
    onImportClick: () -> Unit,
    onRefreshClick: () -> Unit,
    onToggleConsumedSlotsVisibility: () -> Unit,
    showConsumedSlots: Boolean,
    checklistItems: List<WeeklyQuantityChecklistItemUi>,
    onOpenWeeklyQuantityChecklist: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val checklistSummary = remember(checklistItems) {
        buildChecklistMenuSummary(checklistItems)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        IconButton(onClick = onRefreshClick) {
            Icon(
                imageVector = Icons.Default.Update,
                contentDescription = "Aggiorna",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        IconButton(onClick = onToggleConsumedSlotsVisibility) {
            Icon(
                imageVector = if (showConsumedSlots) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                contentDescription = if (showConsumedSlots) "Nascondi completati" else "Mostra completati",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Box {
            IconButton(onClick = { menuExpanded = true }) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Impostazioni piano",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Apri nuovo piano") },
                    onClick = {
                        menuExpanded = false
                        onImportClick()
                    }
                )
                DropdownMenuItem(
                    text = {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text("Target di consumo")
                            Text(
                                text = checklistSummary,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    onClick = {
                        menuExpanded = false
                        onOpenWeeklyQuantityChecklist()
                    }
                )
            }
        }
    }
}

@Composable
internal fun PlannerFeedbackToken(
    message: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        tonalElevation = 2.dp
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

private fun buildChecklistMenuSummary(
    items: List<WeeklyQuantityChecklistItemUi>
): String {
    if (items.isEmpty()) {
        return "Target giornalieri e settimanali"
    }

    val dailyCount = items.count { it.period == WeeklyQuantityChecklistPeriodUi.DAILY }
    val weeklyCount = items.count { it.period == WeeklyQuantityChecklistPeriodUi.WEEKLY }
    val alertCount = items.count { item ->
        item.status == WeeklyQuantityChecklistStatusUi.UNDER_TARGET ||
                item.status == WeeklyQuantityChecklistStatusUi.OVER_LIMIT
    }

    return buildString {
        append("${items.size} target")
        if (dailyCount > 0 || weeklyCount > 0) {
            append(" • ")
            when {
                dailyCount > 0 && weeklyCount > 0 -> append("$dailyCount oggi, $weeklyCount settimana")
                dailyCount > 0 -> append("$dailyCount giornalieri")
                else -> append("$weeklyCount settimanali")
            }
        }
        if (alertCount > 0) {
            append(" • $alertCount da controllare")
        }
    }
}
