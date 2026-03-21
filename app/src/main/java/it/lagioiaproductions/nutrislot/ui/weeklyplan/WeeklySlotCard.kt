package it.lagioiaproductions.nutrislot.ui.weeklyplan

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
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
    onEditClick: () -> Unit,
    onAddToShoppingClick: () -> Unit
) {
    val parsedSections = remember(slotUi.displayedMealText) {
        parseMealSectionVisuals(slotUi.displayedMealText)
    }

    val preview = remember(parsedSections, slotUi.mealSlotType) {
        buildMealPreview(
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
                    .width(7.dp)
                    .background(visualStyle.accent)
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 10.dp, vertical = 7.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
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

                        Text(
                            text = buildString {
                                preview.primaryEmoji?.let {
                                    append(it)
                                    append("  ")
                                }
                                append(timeLabel)
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = visualStyle.meta
                        )
                    }

                    IconButton(onClick = onAddToShoppingClick) {
                        Icon(
                            imageVector = Icons.Default.ShoppingCart,
                            contentDescription = "Aggiungi alla spesa",
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Text(
                    text = preview.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = visualStyle.title,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                preview.sectionChips.take(2).forEach { chip ->
                    SectionChip(
                        chip = chip,
                        accent = visualStyle.accent,
                        contentColor = visualStyle.meta
                    )
                }

                preview.supportingLines.forEachIndexed { index, line ->
                    Text(
                        text = line,
                        style = if (index == 0) {
                            MaterialTheme.typography.bodySmall
                        } else {
                            MaterialTheme.typography.labelSmall
                        },
                        fontWeight = if (index == 0) FontWeight.Medium else FontWeight.Normal,
                        color = if (index == 0) visualStyle.body else visualStyle.meta,
                        maxLines = if (index == 0) 2 else 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                slotUi.nutritionSummary
                    ?.takeIf { it.isNotBlank() }
                    ?.let { nutritionSummary ->
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = visualStyle.accent.copy(alpha = 0.10f)
                        ) {
                            Text(
                                text = "Nutrienti: $nutritionSummary",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = visualStyle.meta,
                                fontWeight = FontWeight.Medium,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                footerNote?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelSmall,
                        color = visualStyle.meta,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
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
private fun SectionChip(
    chip: SectionChipUi,
    accent: Color,
    contentColor: Color
) {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = accent.copy(alpha = 0.10f)
    ) {
        Text(
            text = "${chip.emoji} ${chip.label}",
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private data class MealPreviewUi(
    val primaryEmoji: String?,
    val title: String,
    val supportingLines: List<String>,
    val sectionChips: List<SectionChipUi>
)

private data class SectionChipUi(
    val emoji: String,
    val label: String
)

private fun buildMealPreview(
    parsedSections: List<ParsedMealSectionUi>,
    fallbackTitle: String
): MealPreviewUi {
    if (parsedSections.isEmpty()) {
        return MealPreviewUi(
            primaryEmoji = null,
            title = fallbackTitle,
            supportingLines = emptyList(),
            sectionChips = emptyList()
        )
    }

    val primarySection = parsedSections.first()
    val firstLineSplit = splitPrimaryLine(primarySection.lines.firstOrNull().orEmpty())
    val title = firstLineSplit.title.ifBlank { fallbackTitle }

    val candidateSupportLines = buildList {
        firstLineSplit.remainder?.let { add(it) }
        addAll(primarySection.lines.drop(1))
    }
        .map(::normalizePreviewLine)
        .filter { it.isNotBlank() }
        .distinctBy { it.lowercase() }
        .take(2)

    val sectionChips = parsedSections
        .take(3)
        .map { section ->
            SectionChipUi(
                emoji = section.visualInfo.emoji,
                label = ellipsize(
                    text = section.lines.firstOrNull()
                        ?.takeIf { it.isNotBlank() }
                        ?: mealSemanticLabel(section.visualInfo.semanticKey),
                    maxLength = 22
                )
            )
        }
        .distinctBy { "${it.emoji}|${it.label}" }

    return MealPreviewUi(
        primaryEmoji = primarySection.visualInfo.emoji,
        title = ellipsize(title, 52),
        supportingLines = candidateSupportLines,
        sectionChips = sectionChips
    )
}

private data class PrimaryLineSplit(
    val title: String,
    val remainder: String?
)

private fun splitPrimaryLine(line: String): PrimaryLineSplit {
    val normalized = normalizePreviewLine(line)
    if (normalized.isBlank()) {
        return PrimaryLineSplit("", null)
    }

    val markers = listOf(" oppure ", " + ")
    val splitIndex = markers
        .map { marker -> normalized.indexOf(marker) }
        .filter { it > 28 }
        .minOrNull()

    if (splitIndex == null) {
        return PrimaryLineSplit(normalized, null)
    }

    val title = normalized.substring(0, splitIndex).trim().let {
        if (it.endsWith("…")) it else "$it…"
    }

    val rawRemainder = normalized.substring(splitIndex).trim()
    val remainder = rawRemainder
        .removePrefix("oppure")
        .removePrefix("+")
        .trim()

    return PrimaryLineSplit(
        title = title.ifBlank { normalized },
        remainder = remainder.ifBlank { null }
    )
}

private fun normalizePreviewLine(line: String): String {
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

private fun ellipsize(text: String, maxLength: Int): String {
    if (text.length <= maxLength) return text
    return text.take(maxLength - 1).trimEnd() + "…"
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