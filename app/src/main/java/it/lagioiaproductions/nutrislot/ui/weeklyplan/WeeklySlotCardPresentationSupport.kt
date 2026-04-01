package it.lagioiaproductions.nutrislot.ui.weeklyplan

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import it.lagioiaproductions.nutrislot.domain.model.SlotDisplayState

internal data class CalendarMealContentUi(
    val primaryEmoji: String?,
    val title: String,
    val detailLines: List<String>,
    val alternativeLines: List<String>
)

internal data class WeeklySlotCardPresentation(
    val content: CalendarMealContentUi,
    val visualStyle: FoodVisualStyle,
    val timeLabel: String,
    val footerNote: String?,
    val isCompletedState: Boolean,
    val canToggleCompleted: Boolean,
    val completionContainer: Color,
    val completionContent: Color,
    val showNutritionInline: Boolean
)

@Composable
internal fun rememberWeeklySlotCardPresentation(
    slotUi: WeeklySlotUi
): WeeklySlotCardPresentation {
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

    val isCompletedState = slotUi.isActuallyCompletedThisWeek
    val completionContainer = if (isCompletedState) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        MaterialTheme.colorScheme.tertiaryContainer
    }
    val completionContent = if (isCompletedState) {
        MaterialTheme.colorScheme.onSecondaryContainer
    } else {
        MaterialTheme.colorScheme.onTertiaryContainer
    }

    return remember(
        slotUi,
        content,
        visualStyle,
        isCompletedState,
        completionContainer,
        completionContent
    ) {
        WeeklySlotCardPresentation(
            content = content,
            visualStyle = visualStyle,
            timeLabel = slotTimeLabel(slotUi.mealSlotType),
            footerNote = buildWeeklySlotFooterNote(slotUi),
            isCompletedState = isCompletedState,
            canToggleCompleted = canToggleWeeklySlotCompleted(slotUi),
            completionContainer = completionContainer,
            completionContent = completionContent,
            showNutritionInline = shouldShowInlineNutrition(slotUi, content)
        )
    }
}

internal fun buildCalendarMealContent(
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
                .joinToString(separator = " - ")

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

internal fun normalizeCalendarLine(line: String): String {
    return line
        .stripMealNutritionBlock()
        .removePrefix("\u2022")
        .removePrefix("-")
        .removePrefix("\u2013")
        .removePrefix("\u2014")
        .trim()
        .replace(Regex("\\s+"), " ")
        .removeSuffix(".")
        .trim()
}

private fun buildWeeklySlotFooterNote(slotUi: WeeklySlotUi): String? {
    return when {
        slotUi.isActuallyCompletedThisWeek -> "Completato"
        slotUi.displayState is SlotDisplayState.ConsumedWithReplacement -> "Sostituito"
        slotUi.displayState == SlotDisplayState.OriginalMealAlreadyUsedElsewhere -> "Spostato altrove"
        slotUi.reassignedFromDayLabel != null && slotUi.reassignedFromMealSlotLabel != null ->
            "Da ${slotUi.reassignedFromDayLabel}"
        else -> null
    }
}

private fun canToggleWeeklySlotCompleted(slotUi: WeeklySlotUi): Boolean {
    return slotUi.isActuallyCompletedThisWeek || slotUi.displayedMealText.isNotBlank()
}

@Suppress("unused")
private fun shouldShowInlineNutrition(
    slotUi: WeeklySlotUi,
    content: CalendarMealContentUi
): Boolean {
    return !slotUi.nutritionSummary.isNullOrBlank()
}
