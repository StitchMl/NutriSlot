package it.lagioiaproductions.nutrislot.ui.weeklyplan

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.RemoveCircle
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import it.lagioiaproductions.nutrislot.domain.model.MealSlotType
import it.lagioiaproductions.nutrislot.domain.model.SlotDisplayState

@Composable
internal fun WeeklySlotCard(
    modifier: Modifier = Modifier,
    slotUi: WeeklySlotUi,
    onManageClick: () -> Unit,
    onEditClick: () -> Unit,
    onToggleCompletedClick: () -> Unit,
    onAddToShoppingClick: () -> Unit
) {
    val parsedSections = remember(slotUi.displayedMealText) {
        parseMealSectionVisuals(slotUi.displayedMealText)
    }

    val content = remember(parsedSections, slotUi.mealSlotType) {
        buildCalendarMealContent(
            parsedSections = parsedSections,
            fallbackTitle = slotUi.mealSlotType.displayName
        )
    }

    val visualStyle = remember(
        parsedSections.firstOrNull()?.visualInfo?.semanticKey,
        slotUi.mealSlotType,
        slotUi.displayState,
        slotUi.isActuallyCompletedThisWeek
    ) {
        foodVisualStyleForMeal(
            visualInfo = parsedSections.firstOrNull()?.visualInfo,
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

    val canToggleCompleted = remember(slotUi.displayState, slotUi.displayedMealText, slotUi.isActuallyCompletedThisWeek) {
        slotUi.isActuallyCompletedThisWeek ||
                (
                        slotUi.displayedMealText.isNotBlank() &&
                                slotUi.displayState != SlotDisplayState.Empty
                        )
    }

    val completionContainer = if (slotUi.isActuallyCompletedThisWeek) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        MaterialTheme.colorScheme.tertiaryContainer
    }

    val completionContent = if (slotUi.isActuallyCompletedThisWeek) {
        MaterialTheme.colorScheme.onSecondaryContainer
    } else {
        MaterialTheme.colorScheme.onTertiaryContainer
    }

    val showNutritionInline = remember(slotUi.nutritionSummary, content.detailLines.size, content.alternativeLines.size) {
        !slotUi.nutritionSummary.isNullOrBlank() &&
                content.detailLines.size <= 1 &&
                content.alternativeLines.isEmpty()
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
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(7.dp)
                    .background(visualStyle.accent)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = buildString {
                            content.primaryEmoji?.let {
                                append(it)
                                append("  ")
                            }
                            append(timeLabel)
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = visualStyle.meta
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilledIconButton(
                            onClick = onEditClick,
                            modifier = Modifier.size(32.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f),
                                contentColor = MaterialTheme.colorScheme.onSurface
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Modifica slot",
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        if (canToggleCompleted) {
                            FilledIconButton(
                                onClick = onToggleCompletedClick,
                                modifier = Modifier.size(32.dp),
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = completionContainer,
                                    contentColor = completionContent
                                )
                            ) {
                                Icon(
                                    imageVector = if (slotUi.isActuallyCompletedThisWeek) {
                                        Icons.Default.RemoveCircle
                                    } else {
                                        Icons.Default.CheckCircle
                                    },
                                    contentDescription = if (slotUi.isActuallyCompletedThisWeek) {
                                        "Annulla completamento"
                                    } else {
                                        "Segna completato"
                                    },
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        FilledIconButton(
                            onClick = onAddToShoppingClick,
                            modifier = Modifier.size(32.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f),
                                contentColor = MaterialTheme.colorScheme.onSurface
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShoppingCart,
                                contentDescription = "Aggiungi alla spesa",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Text(
                    text = content.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = visualStyle.title
                )

                content.detailLines.forEachIndexed { index, line ->
                    Text(
                        text = line,
                        style = if (index == 0) {
                            MaterialTheme.typography.bodySmall
                        } else {
                            MaterialTheme.typography.labelSmall
                        },
                        fontWeight = if (index == 0) FontWeight.Medium else FontWeight.Normal,
                        color = if (index == 0) visualStyle.body else visualStyle.meta
                    )
                }

                content.alternativeLines.forEach { alternative ->
                    CompactBadge(
                        text = alternative,
                        background = visualStyle.accent.copy(alpha = 0.10f),
                        content = visualStyle.meta
                    )
                }

                if (showNutritionInline) {
                    slotUi.nutritionSummary?.let { nutritionSummary ->
                        CompactBadge(
                            text = "Nutrienti: $nutritionSummary",
                            background = visualStyle.accent.copy(alpha = 0.10f),
                            content = visualStyle.meta
                        )
                    }
                }

                footerNote?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelSmall,
                        color = visualStyle.meta
                    )
                }

                if (slotUi.hasCustomizations) {
                    Text(
                        text = "Personalizzato",
                        style = MaterialTheme.typography.labelSmall,
                        color = visualStyle.meta,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun CompactBadge(
    text: String,
    background: Color,
    content: Color
) {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = background
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = content,
            fontWeight = FontWeight.Medium
        )
    }
}

private data class CalendarMealContentUi(
    val primaryEmoji: String?,
    val title: String,
    val detailLines: List<String>,
    val alternativeLines: List<String>
)

private fun buildCalendarMealContent(
    parsedSections: List<ParsedMealSectionUi>,
    fallbackTitle: String
): CalendarMealContentUi {
    if (parsedSections.isEmpty()) {
        return CalendarMealContentUi(
            primaryEmoji = null,
            title = fallbackTitle,
            detailLines = emptyList(),
            alternativeLines = emptyList()
        )
    }

    val primarySection = parsedSections.first()
    val primaryTitle = normalizeCalendarLine(
        primarySection.lines.firstOrNull().orEmpty()
    ).ifBlank { fallbackTitle }

    val primaryDetails = primarySection.lines
        .drop(1)
        .map(::normalizeCalendarLine)
        .filter { it.isNotBlank() }

    val alternativeLines = parsedSections
        .drop(1)
        .map { section ->
            val joined = section.lines
                .map(::normalizeCalendarLine)
                .filter { it.isNotBlank() }
                .joinToString(separator = " • ")

            "Alternativa: ${section.visualInfo.emoji} $joined"
        }
        .filter { it.isNotBlank() }

    return CalendarMealContentUi(
        primaryEmoji = primarySection.visualInfo.emoji,
        title = primaryTitle,
        detailLines = primaryDetails,
        alternativeLines = alternativeLines
    )
}

private fun normalizeCalendarLine(line: String): String {
    return line
        .stripMealNutritionBlock()
        .removePrefix("•")
        .removePrefix("-")
        .removePrefix("–")
        .removePrefix("—")
        .trim()
        .replace(Regex("\\s+"), " ")
        .removeSuffix(".")
        .trim()
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
    visualInfo: MealVisualInfo?,
    slotType: MealSlotType,
    displayState: SlotDisplayState,
    isCompleted: Boolean
): FoodVisualStyle {
    val base = when (visualInfo?.semanticKey) {
        "panino" ->
            baseFoodStyle(Color(0xFFF3E0C5), Color(0xFFC78A48), Color(0xFFC78A48), visualInfo.emoji)
        "piadina" ->
            baseFoodStyle(Color(0xFFFFDFC9), Color(0xFFE58D5E), Color(0xFFE58D5E), visualInfo.emoji)
        "frisella" ->
            baseFoodStyle(Color(0xFFF0E3CE), Color(0xFFB98A52), Color(0xFFB98A52), visualInfo.emoji)
        "insalata", "verdura", "avocado" ->
            baseFoodStyle(Color(0xFFD5F5D9), Color(0xFF54B868), Color(0xFF54B868), visualInfo.emoji)
        "cereale_primo" ->
            baseFoodStyle(Color(0xFFFFD8B0), Color(0xFFF08A24), Color(0xFFF08A24), visualInfo.emoji)
        "carne" ->
            baseFoodStyle(Color(0xFFFFCDC7), Color(0xFFE96A5F), Color(0xFFE96A5F), visualInfo.emoji)
        "pesce" ->
            baseFoodStyle(Color(0xFFCDEBFF), Color(0xFF4DA3FF), Color(0xFF4DA3FF), visualInfo.emoji)
        "uova" ->
            baseFoodStyle(Color(0xFFFFF2BA), Color(0xFFE0B400), Color(0xFFE0B400), visualInfo.emoji)
        "latticino", "formaggio" ->
            baseFoodStyle(Color(0xFFE1F0FF), Color(0xFF6BA4FF), Color(0xFF6BA4FF), visualInfo.emoji)
        "frutta", "banana", "mela", "pera" ->
            baseFoodStyle(Color(0xFFFFD4E1), Color(0xFFFF5C8A), Color(0xFFFF5C8A), visualInfo.emoji)
        "pane" ->
            baseFoodStyle(Color(0xFFF2DFC7), Color(0xFFC78A48), Color(0xFFC78A48), visualInfo.emoji)
        "colazione_secca", "pancake", "dolce_spalmabile", "caffe" ->
            baseFoodStyle(Color(0xFFFFE6D5), Color(0xFFFFA36C), Color(0xFFFFA36C), visualInfo.emoji)
        "olio" ->
            baseFoodStyle(Color(0xFFE6F3C8), Color(0xFF97B63E), Color(0xFF97B63E), visualInfo.emoji)
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
            "🥣"
        )
        MealSlotType.MORNING_SNACK -> baseFoodStyle(
            Color(0xFFE4F7E7),
            Color(0xFF6BCB77),
            Color(0xFF6BCB77),
            "🍏"
        )
        MealSlotType.LUNCH -> baseFoodStyle(
            Color(0xFFDDF0FF),
            Color(0xFF5AA9FF),
            Color(0xFF5AA9FF),
            "🍽️"
        )
        MealSlotType.AFTERNOON_SNACK -> baseFoodStyle(
            Color(0xFFFFE8C7),
            Color(0xFFFFB84D),
            Color(0xFFFFB84D),
            "🥜"
        )
        MealSlotType.DINNER -> baseFoodStyle(
            Color(0xFFE7E0FF),
            Color(0xFF8B7CFF),
            Color(0xFF8B7CFF),
            "🍽️"
        )
    }
}