package it.lagioiaproductions.nutrislot.ui.weeklyplan

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import it.lagioiaproductions.nutrislot.domain.model.MealSlotType
import it.lagioiaproductions.nutrislot.domain.model.SlotDisplayState

@Composable
internal fun WeeklySlotCard(
    modifier: Modifier = Modifier,
    slotUi: WeeklySlotUi,
    onManageClick: () -> Unit
) {
    val palette = calendarEventPalette(
        slotType = slotUi.mealSlotType,
        displayState = slotUi.displayState,
        isCompleted = slotUi.isActuallyCompletedThisWeek
    )

    val flattenedLines = remember(slotUi.displayedMealText) {
        parseMealSections(slotUi.displayedMealText)
            .flatten()
            .map { it.trim() }
            .filter { it.isNotBlank() }
    }

    val title = remember(flattenedLines, slotUi.mealSlotType) {
        flattenedLines.firstOrNull()
            ?: slotUi.mealSlotType.displayName
    }

    val description = remember(flattenedLines) {
        flattenedLines.drop(1).joinToString(" • ")
    }

    val emoji = remember(slotUi.displayedMealText, slotUi.mealSlotType) {
        mealEmojiForSlot(
            mealText = slotUi.displayedMealText,
            slotType = slotUi.mealSlotType
        )
    }

    val timeLabel = remember(slotUi.mealSlotType) {
        slotTimeLabel(slotUi.mealSlotType)
    }

    val footerNote = remember(slotUi) {
        when {
            slotUi.isActuallyCompletedThisWeek -> "✓ Completato"
            slotUi.displayState is SlotDisplayState.ConsumedWithReplacement -> "↔ Sostituito"
            slotUi.displayState == SlotDisplayState.OriginalMealAlreadyUsedElsewhere -> "Spostato altrove"
            slotUi.reassignedFromDayLabel != null && slotUi.reassignedFromMealSlotLabel != null ->
                "Da ${slotUi.reassignedFromDayLabel}"
            else -> null
        }
    }

    val cardShape = MaterialTheme.shapes.medium

    ElevatedCard(
        onClick = onManageClick,
        modifier = modifier.border(
            width = 1.dp,
            color = palette.border,
            shape = cardShape
        ),
        shape = cardShape,
        colors = CardDefaults.elevatedCardColors(
            containerColor = palette.container
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "$emoji  $timeLabel",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = palette.meta
            )

            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = palette.title,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (description.isNotBlank()) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.body,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            footerNote?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.meta,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private data class CalendarEventPalette(
    val container: Color,
    val border: Color,
    val title: Color,
    val body: Color,
    val meta: Color
)

@Composable
private fun calendarEventPalette(
    slotType: MealSlotType,
    displayState: SlotDisplayState,
    isCompleted: Boolean
): CalendarEventPalette {
    val baseContainer = when (slotType) {
        MealSlotType.BREAKFAST -> MaterialTheme.colorScheme.tertiaryContainer
        MealSlotType.MORNING_SNACK -> MaterialTheme.colorScheme.secondaryContainer
        MealSlotType.LUNCH -> MaterialTheme.colorScheme.primaryContainer
        MealSlotType.AFTERNOON_SNACK -> MaterialTheme.colorScheme.secondaryContainer
        MealSlotType.DINNER -> MaterialTheme.colorScheme.errorContainer
    }

    val baseContent = when (slotType) {
        MealSlotType.BREAKFAST -> MaterialTheme.colorScheme.onTertiaryContainer
        MealSlotType.MORNING_SNACK -> MaterialTheme.colorScheme.onSecondaryContainer
        MealSlotType.LUNCH -> MaterialTheme.colorScheme.onPrimaryContainer
        MealSlotType.AFTERNOON_SNACK -> MaterialTheme.colorScheme.onSecondaryContainer
        MealSlotType.DINNER -> MaterialTheme.colorScheme.onErrorContainer
    }

    val adjustedContainer = when {
        isCompleted -> baseContainer.copy(alpha = 0.62f)
        displayState == SlotDisplayState.OriginalMealAlreadyUsedElsewhere ->
            MaterialTheme.colorScheme.surfaceVariant
        else -> baseContainer
    }

    val adjustedBorder = when (displayState) {
        SlotDisplayState.Empty -> MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        SlotDisplayState.PlannedAvailable -> baseContent.copy(alpha = 0.18f)
        SlotDisplayState.ConsumedAsPlanned -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.35f)
        is SlotDisplayState.ConsumedWithReplacement -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f)
        SlotDisplayState.OriginalMealAlreadyUsedElsewhere -> MaterialTheme.colorScheme.outline.copy(alpha = 0.28f)
    }

    return CalendarEventPalette(
        container = adjustedContainer,
        border = adjustedBorder,
        title = baseContent,
        body = baseContent.copy(alpha = 0.92f),
        meta = baseContent.copy(alpha = 0.76f)
    )
}

private fun mealEmojiForSlot(
    mealText: String,
    slotType: MealSlotType
): String {
    val normalized = mealText.lowercase()

    return when {
        "pollo" in normalized || "tacchino" in normalized -> "🍗"
        "salmone" in normalized || "tonno" in normalized || "pesce" in normalized -> "🐟"
        "riso" in normalized -> "🍚"
        "pasta" in normalized || "spaghetti" in normalized -> "🍝"
        "uova" in normalized || "uovo" in normalized -> "🥚"
        "yogurt" in normalized || "latte" in normalized -> "🥛"
        "frutta" in normalized || "mela" in normalized || "banana" in normalized -> "🍎"
        "verdure" in normalized || "insalata" in normalized -> "🥗"
        "pane" in normalized || "toast" in normalized -> "🍞"
        "legumi" in normalized -> "🫘"
        "patate" in normalized -> "🥔"
        else -> when (slotType) {
            MealSlotType.BREAKFAST -> "🥣"
            MealSlotType.MORNING_SNACK -> "🍏"
            MealSlotType.LUNCH -> "🍽️"
            MealSlotType.AFTERNOON_SNACK -> "☕"
            MealSlotType.DINNER -> "🌙"
        }
    }
}