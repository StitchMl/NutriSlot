@file:Suppress("SameParameterValue", "SameParameterValue", "SameParameterValue")

package it.lagioiaproductions.nutrislot.ui.weeklyplan.slot

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import it.lagioiaproductions.nutrislot.ui.weeklyplan.calendar.WeeklyStatusBadge
import it.lagioiaproductions.nutrislot.ui.weeklyplan.parser.ParsedMealSectionUi
import it.lagioiaproductions.nutrislot.ui.weeklyplan.calendar.slotStatusContainerColor
import it.lagioiaproductions.nutrislot.ui.weeklyplan.calendar.slotStatusContentColor
import it.lagioiaproductions.nutrislot.ui.weeklyplan.calendar.slotStatusLabel

@Composable
internal fun SlotActionDialogBody(
    dialogUi: SlotActionDialogUi,
    isApplying: Boolean,
    onConsumeReplacement: (sourceSlotId: String) -> Unit,
    onSelectExtraCatalogOption: (optionId: String) -> Unit
) {
    val targetSections = rememberDialogMealSections(dialogUi.currentDisplayedMealText)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 500.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        WeeklyStatusBadge(
            text = "Stato: ${slotStatusLabel(dialogUi.targetDisplayState)}",
            containerColor = slotStatusContainerColor(dialogUi.targetDisplayState),
            contentColor = slotStatusContentColor(dialogUi.targetDisplayState)
        )

        dialogUi.mealRuleSummary?.let { ruleSummary ->
            DialogInfoBlock(
                title = "Composizione attesa",
                body = ruleSummary
            )
        }

        CurrentAssignedMealSection(sections = targetSections)
        ReassignedMealInfoCard(dialogUi = dialogUi)

        ReplacementOptionsSection(
            options = dialogUi.replacementOptions,
            enabled = !isApplying,
            onConsumeReplacement = onConsumeReplacement
        )

        ExtraCatalogOptionsSection(
            options = dialogUi.extraCatalogOptions,
            enabled = !isApplying,
            onSelectExtraCatalogOption = onSelectExtraCatalogOption
        )
    }
}

@Composable
private fun CurrentAssignedMealSection(
    sections: List<ParsedMealSectionUi>
) {
    if (sections.isEmpty()) {
        Text(
            text = "Questo slot non ha un pasto disponibile in questo momento.",
            style = MaterialTheme.typography.bodyMedium
        )
        return
    }

    DialogSectionTitle("Pasto attualmente assegnato")
    MealSectionsBlock(sections = sections)
}

@Composable
private fun ReassignedMealInfoCard(
    dialogUi: SlotActionDialogUi
) {
    val sourceDayLabel = dialogUi.reassignedFromDayLabel ?: return
    val sourceMealSlotLabel = dialogUi.reassignedFromMealSlotLabel ?: return
    if (dialogUi.isTargetActuallyCompletedThisWeek) return

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Text(
            text = "Questo slot contiene il pasto originario di $sourceDayLabel - $sourceMealSlotLabel.",
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

@Composable
private fun ReplacementOptionsSection(
    options: List<ReplacementMealOptionUi>,
    enabled: Boolean,
    onConsumeReplacement: (sourceSlotId: String) -> Unit
) {
    if (options.isEmpty()) return

    DialogSectionTitle("Sostituisci con un pasto gia pianificato")
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        options.forEach { replacement ->
            ReplacementOptionCard(
                option = replacement,
                enabled = enabled,
                onClick = { onConsumeReplacement(replacement.sourceSlotId) }
            )
        }
    }
}

@Composable
private fun ExtraCatalogOptionsSection(
    options: List<ExtraCatalogMealOptionUi>,
    enabled: Boolean,
    onSelectExtraCatalogOption: (optionId: String) -> Unit
) {
    if (options.isEmpty()) return

    DialogSectionTitle("Opzioni extra dal PDF")
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        options.forEach { option ->
            ExtraCatalogOptionCard(
                option = option,
                enabled = enabled,
                onClick = { onSelectExtraCatalogOption(option.optionId) }
            )
        }
    }
}

@Composable
private fun DialogSectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
private fun DialogInfoBlock(
    title: String,
    body: String
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}
