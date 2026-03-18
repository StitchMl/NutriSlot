package it.lagioiaproductions.nutrislot.ui.weeklyplan

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
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
    onManageClick: () -> Unit,
    onAddToShoppingClick: () -> Unit
) {
    val flattenedLines = remember(slotUi.displayedMealText) {
        parseMealSections(slotUi.displayedMealText)
            .flatten()
            .map { it.trim() }
            .filter { it.isNotBlank() }
    }

    val title = remember(flattenedLines, slotUi.mealSlotType) {
        flattenedLines.firstOrNull() ?: slotUi.mealSlotType.displayName
    }

    val description = remember(flattenedLines) {
        flattenedLines.drop(1).joinToString(" • ")
    }

    val visualStyle = remember(
        slotUi.displayedMealText,
        slotUi.mealSlotType,
        slotUi.displayState,
        slotUi.isActuallyCompletedThisWeek
    ) {
        foodVisualStyleForMeal(
            mealText = slotUi.displayedMealText,
            slotType = slotUi.mealSlotType,
            displayState = slotUi.displayState,
            isCompleted = slotUi.isActuallyCompletedThisWeek
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
            color = visualStyle.border,
            shape = cardShape
        ),
        shape = cardShape,
        colors = CardDefaults.elevatedCardColors(
            containerColor = visualStyle.container
        )
    ) {
        Row(
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(6.dp)
                    .background(visualStyle.accent)
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = buildString {
                            visualStyle.emoji?.let {
                                append(it)
                                append("  ")
                            }
                            append(timeLabel)
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = visualStyle.meta
                    )

                    Surface(
                        shape = MaterialTheme.shapes.extraLarge,
                        color = visualStyle.accent.copy(alpha = 0.14f),
                        modifier = Modifier.clickable(onClick = onAddToShoppingClick)
                    ) {
                        Text(
                            text = "🛒",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = visualStyle.title,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (description.isNotBlank()) {
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = visualStyle.body,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                footerNote?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelSmall,
                        color = visualStyle.meta,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

private data class FoodVisualStyle(
    val container: Color,
    val border: Color,
    val accent: Color,
    val title: Color,
    val body: Color,
    val meta: Color,
    val emoji: String?
)

private fun foodVisualStyleForMeal(
    mealText: String,
    slotType: MealSlotType,
    displayState: SlotDisplayState,
    isCompleted: Boolean
): FoodVisualStyle {
    val normalized = mealText.lowercase()

    val base = when {
        "pasta" in normalized || "spaghetti" in normalized || "penne" in normalized || "gnocchi" in normalized ->
            baseFoodStyle(Color(0xFFFFD8B0), Color(0xFFF08A24), Color(0xFFF08A24), "🍝")

        "riso" in normalized || "risotto" in normalized ->
            baseFoodStyle(Color(0xFFFFEEAE), Color(0xFFD6A400), Color(0xFFD6A400), "🍚")

        "pollo" in normalized || "tacchino" in normalized || "carne" in normalized || "hamburger" in normalized ->
            baseFoodStyle(Color(0xFFFFCDC7), Color(0xFFE96A5F), Color(0xFFE96A5F), "🍗")

        "salmone" in normalized || "tonno" in normalized || "pesce" in normalized || "orata" in normalized ->
            baseFoodStyle(Color(0xFFCDEBFF), Color(0xFF4DA3FF), Color(0xFF4DA3FF), "🐟")

        "uovo" in normalized || "uova" in normalized ->
            baseFoodStyle(Color(0xFFFFF2BA), Color(0xFFE0B400), Color(0xFFE0B400), "🥚")

        "yogurt" in normalized || "latte" in normalized || "fiocchi di latte" in normalized ->
            baseFoodStyle(Color(0xFFE1F0FF), Color(0xFF6BA4FF), Color(0xFF6BA4FF), "🥛")

        "frutta" in normalized || "mela" in normalized || "banana" in normalized || "pera" in normalized || "kiwi" in normalized ->
            baseFoodStyle(Color(0xFFFFD4E1), Color(0xFFFF5C8A), Color(0xFFFF5C8A), "🍎")

        "verdure" in normalized || "insalata" in normalized || "zucchine" in normalized || "broccoli" in normalized ->
            baseFoodStyle(Color(0xFFD5F5D9), Color(0xFF54B868), Color(0xFF54B868), "🥗")

        "pane" in normalized || "toast" in normalized || "fette biscottate" in normalized ->
            baseFoodStyle(Color(0xFFF2DFC7), Color(0xFFC78A48), Color(0xFFC78A48), "🍞")

        "legumi" in normalized || "lenticchie" in normalized || "ceci" in normalized || "fagioli" in normalized ->
            baseFoodStyle(Color(0xFFE0D3FF), Color(0xFF8D63FF), Color(0xFF8D63FF), "🫘")

        "patate" in normalized ->
            baseFoodStyle(Color(0xFFF5E2B9), Color(0xFFD29A31), Color(0xFFD29A31), "🥔")

        "zuppa" in normalized || "minestra" in normalized || "vellutata" in normalized ->
            baseFoodStyle(Color(0xFFFFE2B8), Color(0xFFF59E0B), Color(0xFFF59E0B), "🍲")

        "pizza" in normalized ->
            baseFoodStyle(Color(0xFFFFD6BF), Color(0xFFFF7A45), Color(0xFFFF7A45), "🍕")

        else -> fallbackStyleForSlot(slotType)
    }

    if (displayState == SlotDisplayState.OriginalMealAlreadyUsedElsewhere) {
        return FoodVisualStyle(
            container = Color(0xFFE9E6EC),
            border = Color(0xFFAAA2B1),
            accent = Color(0xFFAAA2B1),
            title = Color(0xFF3A3440),
            body = Color(0xFF4A4351),
            meta = Color(0xFF6A6271),
            emoji = base.emoji
        )
    }

    if (isCompleted) {
        return base.copy(
            container = base.container.copy(alpha = 0.78f),
            border = base.border.copy(alpha = 0.75f),
            accent = base.accent.copy(alpha = 0.78f),
            meta = base.meta.copy(alpha = 0.85f)
        )
    }

    return base
}

private fun baseFoodStyle(
    container: Color,
    border: Color,
    accent: Color,
    emoji: String?
): FoodVisualStyle {
    val title = Color(0xFF1F1A1A)
    val body = Color(0xFF2F2727)
    val meta = Color(0xFF5A4C4C)

    return FoodVisualStyle(
        container = container,
        border = border,
        accent = accent,
        title = title,
        body = body,
        meta = meta,
        emoji = emoji
    )
}

private fun fallbackStyleForSlot(
    slotType: MealSlotType
): FoodVisualStyle {
    return when (slotType) {
        MealSlotType.BREAKFAST -> baseFoodStyle(
            Color(0xFFFFE6D5),
            Color(0xFFFFA36C),
            Color(0xFFFFA36C),
            null
        )

        MealSlotType.MORNING_SNACK -> baseFoodStyle(
            Color(0xFFE4F7E7),
            Color(0xFF6BCB77),
            Color(0xFF6BCB77),
            null
        )

        MealSlotType.LUNCH -> baseFoodStyle(
            Color(0xFFDDF0FF),
            Color(0xFF5AA9FF),
            Color(0xFF5AA9FF),
            null
        )

        MealSlotType.AFTERNOON_SNACK -> baseFoodStyle(
            Color(0xFFFFE8C7),
            Color(0xFFFFB84D),
            Color(0xFFFFB84D),
            null
        )

        MealSlotType.DINNER -> baseFoodStyle(
            Color(0xFFE7E0FF),
            Color(0xFF8B7CFF),
            Color(0xFF8B7CFF),
            null
        )
    }
}