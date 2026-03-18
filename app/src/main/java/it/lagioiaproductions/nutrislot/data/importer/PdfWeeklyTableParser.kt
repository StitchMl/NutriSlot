package it.lagioiaproductions.nutrislot.data.importer

import it.lagioiaproductions.nutrislot.domain.model.CellRecognitionState
import it.lagioiaproductions.nutrislot.domain.model.ImportWarning
import it.lagioiaproductions.nutrislot.domain.model.ImportedMealCell
import it.lagioiaproductions.nutrislot.domain.model.ImportStatus
import it.lagioiaproductions.nutrislot.domain.model.ImportedMealOption
import it.lagioiaproductions.nutrislot.domain.model.ImportedMealRule
import it.lagioiaproductions.nutrislot.domain.model.ImportedPlanDraft
import it.lagioiaproductions.nutrislot.domain.model.MealSlotType
import it.lagioiaproductions.nutrislot.domain.model.WeekDay
import kotlin.math.abs

internal class PdfWeeklyTableParser {

    fun tryParseWeeklyTable(pageScans: List<PageScan>): WeeklyParseResult? {
        val weeklyCandidates = pageScans
            .filter { scan ->
                scan.hasWeekdayHeader &&
                        !scan.isReferencePage &&
                        !scan.isAppendixPage
            }
            .sortedByDescending { scan ->
                scan.weeklyHeaderScore
            }

        val primaryWeeklyPage = weeklyCandidates.firstOrNull() ?: return null
        val pagesToParse = mutableListOf(primaryWeeklyPage)
        val warnings = mutableListOf<ImportWarning>()
        var selectedContinuationPages = 0

        for (pageIndex in (primaryWeeklyPage.zeroBasedIndex + 1) until pageScans.size) {
            val scan = pageScans[pageIndex]

            if (scan.isReferencePage || scan.isAppendixPage) {
                break
            }

            val isContinuationPage = !scan.hasWeekdayHeader &&
                    scan.mealSlotHeadingOccurrences >= PdfImportTextNormalization.CONTINUATION_MIN_SLOT_HEADINGS &&
                    !scan.looksLikeReferenceTemplate

            if (isContinuationPage) {
                pagesToParse += scan
                selectedContinuationPages += 1
            } else if (scan.hasWeekdayHeader) {
                break
            }
        }

        warnings += ImportWarning(
            message = "Parser tabellare attivato sulla pagina settimanale ${primaryWeeklyPage.pageNumber}."
        )

        if (selectedContinuationPages > 0) {
            warnings += ImportWarning(
                message = "Rilevate $selectedContinuationPages pagine di continuazione del piano settimanale."
            )
        }

        val collectedTexts = linkedMapOf<Pair<WeekDay, MealSlotType>, MutableList<String>>()

        pagesToParse.forEach { scan ->
            WeekDay.orderedValues().forEach { day ->
                val columnLines = extractColumnLines(
                    words = scan.positionedWords,
                    pageWidth = scan.pageWidth,
                    day = day
                )

                appendColumnLinesToSlots(
                    day = day,
                    columnLines = columnLines,
                    collectedTexts = collectedTexts
                )
            }
        }

        return WeeklyParseResult(
            collectedTexts = collectedTexts,
            warnings = warnings
        )
    }

    fun buildDraftFromCollectedTexts(
        sourceFileName: String,
        rawExtractedText: String,
        collectedTexts: Map<Pair<WeekDay, MealSlotType>, List<String>>,
        additionalOptions: List<ImportedMealOption>,
        mealRules: List<ImportedMealRule>,
        warnings: List<ImportWarning>
    ): ImportedPlanDraft {
        val cells = buildCanonicalCells(collectedTexts)
        val populatedCells = cells.count { it.rawText.isNotBlank() }
        val finalWarnings = warnings.toMutableList()

        when {
            populatedCells == 0 -> {
                finalWarnings += ImportWarning(
                    message = "Nessun pasto riconosciuto automaticamente. Il layout del PDF potrebbe non essere supportato."
                )
            }
            populatedCells < PdfImportTextNormalization.EXPECTED_WEEKLY_SLOTS -> {
                finalWarnings += ImportWarning(
                    message = "Riconoscimento parziale: trovate $populatedCells celle con contenuto su ${PdfImportTextNormalization.EXPECTED_WEEKLY_SLOTS} slot attesi."
                )
            }
        }

        val status = when {
            populatedCells == 0 -> ImportStatus.UNSUPPORTED
            finalWarnings.isNotEmpty() -> ImportStatus.PARTIAL
            else -> ImportStatus.SUCCESS
        }

        return ImportedPlanDraft(
            sourceFileName = sourceFileName,
            rawExtractedText = rawExtractedText,
            cells = cells,
            additionalOptions = additionalOptions,
            mealRules = mealRules,
            warnings = finalWarnings,
            status = status
        )
    }

    fun buildDraftWithSequentialFallback(
        sourceFileName: String,
        rawExtractedText: String,
        additionalOptions: List<ImportedMealOption>,
        mealRules: List<ImportedMealRule>
    ): ImportedPlanDraft {
        val normalizedLines = rawExtractedText
            .replace("\r\n", "\n")
            .replace("\r", "\n")
            .replace('\u00A0', ' ')
            .lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }

        if (normalizedLines.isEmpty()) {
            return ImportedPlanDraft(
                sourceFileName = sourceFileName,
                rawExtractedText = rawExtractedText,
                cells = buildEmptyCanonicalCells(),
                additionalOptions = additionalOptions,
                mealRules = mealRules,
                warnings = listOf(
                    ImportWarning(
                        message = "Il PDF non contiene testo estraibile oppure è scannerizzato."
                    )
                ),
                status = ImportStatus.UNSUPPORTED
            )
        }

        val collectedTexts = linkedMapOf<Pair<WeekDay, MealSlotType>, MutableList<String>>()
        val warnings = mutableListOf(
            ImportWarning(
                message = "Parser tabellare non attivato: usato fallback lineare, meno affidabile su PDF a colonne."
            )
        )

        var currentDay: WeekDay? = null
        var currentSlot: MealSlotType? = null
        var orphanLineCount = 0

        normalizedLines.forEach { originalLine ->
            val normalizedForMatch = PdfImportTextNormalization.normalizeForMatching(originalLine)
            val dayAtStart = PdfImportTextNormalization.matchWeekDayAtStart(normalizedForMatch)
            val slotMatch = PdfImportTextNormalization.matchMealSlotHeading(
                originalLine = originalLine,
                normalizedLine = normalizedForMatch
            )

            when {
                dayAtStart != null && slotMatch != null -> {
                    currentDay = dayAtStart
                    currentSlot = slotMatch.slot

                    slotMatch.inlineText?.takeIf { it.isNotBlank() }?.let { inlineText ->
                        appendUniqueMealLine(
                            collectedTexts = collectedTexts,
                            day = currentDay!!,
                            slot = currentSlot,
                            line = inlineText
                        )
                    }
                }
                dayAtStart != null -> {
                    currentDay = dayAtStart
                    currentSlot = null
                }
                slotMatch != null && currentDay != null -> {
                    currentSlot = slotMatch.slot

                    slotMatch.inlineText?.takeIf { it.isNotBlank() }?.let { inlineText ->
                        appendUniqueMealLine(
                            collectedTexts = collectedTexts,
                            day = currentDay,
                            slot = currentSlot,
                            line = inlineText
                        )
                    }
                }
                currentDay != null && currentSlot != null -> {
                    appendUniqueMealLine(
                        collectedTexts = collectedTexts,
                        day = currentDay,
                        slot = currentSlot,
                        line = originalLine
                    )
                }
                else -> {
                    orphanLineCount += 1
                }
            }
        }

        if (orphanLineCount > 0) {
            warnings += ImportWarning(
                message = "Sono state ignorate $orphanLineCount righe fuori dal contesto giorno/slot."
            )
        }

        return buildDraftFromCollectedTexts(
            sourceFileName = sourceFileName,
            rawExtractedText = rawExtractedText,
            collectedTexts = collectedTexts,
            additionalOptions = additionalOptions,
            mealRules = mealRules,
            warnings = warnings
        )
    }

    private fun extractColumnLines(
        words: List<PositionedWord>,
        pageWidth: Float,
        day: WeekDay
    ): List<String> {
        val columnWidth = pageWidth / PdfImportTextNormalization.TOTAL_WEEK_DAYS
        val columnIndex = day.sortOrder
        val leftBoundary = columnIndex * columnWidth
        val rightBoundary = leftBoundary + columnWidth

        val wordsInColumn = words
            .filter { word ->
                val centerX = (word.xStart + word.xEnd) / 2f
                centerX in leftBoundary..<rightBoundary
            }
            .sortedWith(compareBy({ it.y }, { it.xStart }))

        if (wordsInColumn.isEmpty()) {
            return emptyList()
        }

        val lineBuckets = mutableListOf<MutableList<PositionedWord>>()
        wordsInColumn.forEach { word ->
            val lastBucket = lineBuckets.lastOrNull()

            if (lastBucket == null) {
                lineBuckets += mutableListOf(word)
            } else {
                val averageY = lastBucket.map { it.y }.average().toFloat()
                if (abs(word.y - averageY) <= PdfImportTextNormalization.LINE_MERGE_TOLERANCE) {
                    lastBucket += word
                } else {
                    lineBuckets += mutableListOf(word)
                }
            }
        }

        return lineBuckets.mapNotNull { bucket ->
            val line = bucket
                .sortedBy { it.xStart }
                .joinToString(separator = " ") { it.text }
                .replace("\\s+".toRegex(), " ")
                .trim()

            line.takeIf { it.isNotBlank() }
        }
    }

    private fun appendColumnLinesToSlots(
        day: WeekDay,
        columnLines: List<String>,
        collectedTexts: MutableMap<Pair<WeekDay, MealSlotType>, MutableList<String>>
    ) {
        var currentSlot: MealSlotType? = null

        columnLines.forEach { line ->
            val normalizedLine = PdfImportTextNormalization.normalizeForMatching(line)

            if (PdfImportTextNormalization.isWeekDayHeaderLine(normalizedLine)) {
                return@forEach
            }

            val slotMatch = PdfImportTextNormalization.matchMealSlotHeading(
                originalLine = line,
                normalizedLine = normalizedLine
            )

            if (slotMatch != null) {
                currentSlot = slotMatch.slot

                slotMatch.inlineText?.takeIf { it.isNotBlank() }?.let { inlineText ->
                    appendUniqueMealLine(
                        collectedTexts = collectedTexts,
                        day = day,
                        slot = currentSlot,
                        line = inlineText
                    )
                }
                return@forEach
            }

            if (currentSlot != null) {
                appendUniqueMealLine(
                    collectedTexts = collectedTexts,
                    day = day,
                    slot = currentSlot,
                    line = line
                )
            }
        }
    }

    private fun appendUniqueMealLine(
        collectedTexts: MutableMap<Pair<WeekDay, MealSlotType>, MutableList<String>>,
        day: WeekDay,
        slot: MealSlotType?,
        line: String
    ) {
        if (slot == null) return

        val normalizedLine = PdfImportTextNormalization.normalizeMealText(line)
        if (normalizedLine.isBlank()) return

        val key = day to slot
        val bucket = collectedTexts.getOrPut(key) { mutableListOf() }

        if (bucket.none { PdfImportTextNormalization.normalizeMealText(it) == normalizedLine }) {
            bucket += normalizedLine
        }
    }

    private fun buildCanonicalCells(
        collectedTexts: Map<Pair<WeekDay, MealSlotType>, List<String>>
    ): List<ImportedMealCell> {
        return WeekDay.orderedValues().flatMap { day ->
            MealSlotType.orderedValues().map { slot ->
                val rawText = collectedTexts[day to slot]
                    ?.joinToString(separator = "\n")
                    ?.trim()
                    .orEmpty()

                ImportedMealCell(
                    id = "${day.name}_${slot.name}",
                    dayOfWeek = day,
                    mealSlotType = slot,
                    rawText = rawText,
                    normalizedText = PdfImportTextNormalization.normalizeMealText(rawText),
                    recognitionState = if (rawText.isBlank()) {
                        CellRecognitionState.EMPTY
                    } else {
                        CellRecognitionState.RECOGNIZED
                    }
                )
            }
        }
    }

    private fun buildEmptyCanonicalCells(): List<ImportedMealCell> {
        return WeekDay.orderedValues().flatMap { day ->
            MealSlotType.orderedValues().map { slot ->
                ImportedMealCell(
                    id = "${day.name}_${slot.name}",
                    dayOfWeek = day,
                    mealSlotType = slot,
                    rawText = "",
                    normalizedText = "",
                    recognitionState = CellRecognitionState.EMPTY
                )
            }
        }
    }
}