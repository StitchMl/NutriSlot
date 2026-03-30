package it.lagioiaproductions.nutrislot.data.importer

import it.lagioiaproductions.nutrislot.domain.model.ImportWarning
import it.lagioiaproductions.nutrislot.domain.model.MealSlotType
import it.lagioiaproductions.nutrislot.domain.model.WeekDay

internal data class PositionedWord(
    val text: String,
    val xStart: Float,
    val xEnd: Float,
    val y: Float
)

internal data class PageScan(
    val zeroBasedIndex: Int,
    val pageNumber: Int,
    val pageWidth: Float,
    val fullText: String,
    val normalizedFullText: String,
    val positionedWords: List<PositionedWord>,
    val hasWeekdayHeader: Boolean,
    val mealSlotHeadingOccurrences: Int,
    val isReferencePage: Boolean,
    val looksLikeReferenceTemplate: Boolean,
    val isAppendixPage: Boolean,
    val weeklyHeaderScore: Int
)

internal data class WeeklyParseResult(
    val collectedTexts: Map<Pair<WeekDay, MealSlotType>, List<String>>,
    val warnings: List<ImportWarning>
)

internal data class SlotHeadingMatch(
    val slot: MealSlotType,
    val inlineText: String?
)

