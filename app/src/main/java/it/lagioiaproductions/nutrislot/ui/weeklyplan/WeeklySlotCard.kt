package it.lagioiaproductions.nutrislot.ui.weeklyplan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
internal fun WeeklySlotCard(
    slotUi: WeeklySlotUi,
    onManageClick: () -> Unit
) {
    val mealSections = parseMealSections(slotUi.displayedMealText)

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MealTypeBadge(slotUi.mealSlotType)

                WeeklyStatusBadge(
                    text = slotStatusLabel(slotUi.displayState),
                    containerColor = slotStatusContainerColor(slotUi.displayState),
                    contentColor = slotStatusContentColor(slotUi.displayState)
                )
            }

            if (
                slotUi.reassignedFromDayLabel != null &&
                slotUi.reassignedFromMealSlotLabel != null &&
                !slotUi.isActuallyCompletedThisWeek
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        text = "Questo slot ora contiene il pasto originario di ${slotUi.reassignedFromDayLabel} • ${slotUi.reassignedFromMealSlotLabel}",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            if (mealSections.isEmpty()) {
                Text(
                    text = "Nessun pasto pianificato per questo slot.",
                    style = MaterialTheme.typography.bodyLarge
                )
            } else {
                MealTextBlock(sections = mealSections)
            }

            FilledTonalButton(
                onClick = onManageClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Gestisci slot")
            }
        }
    }
}

@Composable
internal fun MealTextBlock(
    sections: List<List<String>>
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            sections.forEachIndexed { index, section ->
                if (index > 0) {
                    SectionSeparatorBadge()
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    section.forEach { line ->
                        Text(
                            text = "• $line",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun SectionSeparatorBadge() {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Text(
            text = "In aggiunta",
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}