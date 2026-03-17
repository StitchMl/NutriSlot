package it.lagioiaproductions.nutrislot.data.importer

import android.content.Context
import android.net.Uri
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import com.tom_roush.pdfbox.text.TextPosition
import it.lagioiaproductions.nutrislot.domain.model.CellRecognitionState
import it.lagioiaproductions.nutrislot.domain.model.ImportStatus
import it.lagioiaproductions.nutrislot.domain.model.ImportWarning
import it.lagioiaproductions.nutrislot.domain.model.ImportedMealCell
import it.lagioiaproductions.nutrislot.domain.model.ImportedPlanDraft
import it.lagioiaproductions.nutrislot.domain.model.MealSlotType
import it.lagioiaproductions.nutrislot.domain.model.WeekDay
import java.text.Normalizer
import kotlin.math.abs

class PdfMealPlanImporter {

    fun importFromUri(
        context: Context,
        uri: Uri,
        sourceFileName: String
    ): ImportedPlanDraft {
        PDFBoxResourceLoader.init(context.applicationContext)

        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            PDDocument.load(inputStream).use { document ->
                return buildDraftFromDocument(
                    document = document,
                    sourceFileName = sourceFileName
                )
            }
        }

        throw IllegalStateException("Impossibile leggere il file selezionato.")
    }

    private fun buildDraftFromDocument(
        document: PDDocument,
        sourceFileName: String
    ): ImportedPlanDraft {
        val pageScans = (0 until document.numberOfPages).map { pageIndex ->
            scanPage(
                document = document,
                zeroBasedPageIndex = pageIndex
            )
        }

        val rawExtractedText = pageScans.joinToString(separator = "\n\n") { scan ->
            buildString {
                append("---- PAGE ${scan.pageNumber} ----\n")
                append(scan.fullText)
            }
        }

        val weeklyParseResult = tryParseWeeklyTable(pageScans)

        return if (weeklyParseResult != null) {
            buildDraftFromCollectedTexts(
                sourceFileName = sourceFileName,
                rawExtractedText = rawExtractedText,
                collectedTexts = weeklyParseResult.collectedTexts,
                warnings = weeklyParseResult.warnings
            )
        } else {
            buildDraftWithSequentialFallback(
                sourceFileName = sourceFileName,
                rawExtractedText = rawExtractedText
            )
        }
    }

    private fun tryParseWeeklyTable(
        pageScans: List<PageScan>
    ): WeeklyParseResult? {
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
                    scan.mealSlotHeadingOccurrences >= CONTINUATION_MIN_SLOT_HEADINGS &&
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

    private fun scanPage(
        document: PDDocument,
        zeroBasedPageIndex: Int
    ): PageScan {
        val fullText = extractPageText(
            document = document,
            zeroBasedPageIndex = zeroBasedPageIndex
        )

        val normalized = normalizeForMatching(fullText)

        val pageWidth = document.getPage(zeroBasedPageIndex).cropBox.width

        val positionedWords = extractPositionedWords(
            document = document,
            zeroBasedPageIndex = zeroBasedPageIndex
        )

        return PageScan(
            zeroBasedIndex = zeroBasedPageIndex,
            pageNumber = zeroBasedPageIndex + 1,
            pageWidth = pageWidth,
            fullText = fullText,
            normalizedFullText = normalized,
            positionedWords = positionedWords,
            hasWeekdayHeader = countDistinctWeekDays(normalized) >= 5,
            mealSlotHeadingOccurrences = countMealSlotHeadingOccurrences(normalized),
            isReferencePage = normalized.contains("schema di riferimento per la costruzione dei pasti"),
            looksLikeReferenceTemplate = normalized.contains("fonte glucidica") &&
                    normalized.contains("fonte proteica") &&
                    normalized.contains("verdure"),
            isAppendixPage = normalized.contains("grammature di riferimento per pranzo e cena") ||
                    normalized.contains("esempi pratici di pasti bilanciati") ||
                    normalized.contains("fonte proteica") && normalized.contains("fonte lipidica: olio evo"),
            weeklyHeaderScore = computeWeeklyHeaderScore(normalized)
        )
    }

    private fun extractPageText(
        document: PDDocument,
        zeroBasedPageIndex: Int
    ): String {
        val stripper = PDFTextStripper().apply {
            sortByPosition = true
            startPage = zeroBasedPageIndex + 1
            endPage = zeroBasedPageIndex + 1
            lineSeparator = "\n"
            wordSeparator = " "
        }

        return stripper.getText(document)
    }

    private fun extractPositionedWords(
        document: PDDocument,
        zeroBasedPageIndex: Int
    ): List<PositionedWord> {
        val words = mutableListOf<PositionedWord>()

        val stripper = object : PDFTextStripper() {
            override fun writeString(
                text: String?,
                textPositions: MutableList<TextPosition>?
            ) {
                if (textPositions.isNullOrEmpty()) return

                val buffer = StringBuilder()
                var firstPosition: TextPosition? = null
                var lastPosition: TextPosition? = null

                fun flushCurrentWord() {
                    val word = buffer.toString().trim()
                    val first = firstPosition
                    val last = lastPosition

                    if (word.isNotBlank() && first != null && last != null) {
                        words += PositionedWord(
                            text = word,
                            xStart = first.xDirAdj,
                            xEnd = last.xDirAdj + last.widthDirAdj,
                            y = first.yDirAdj
                        )
                    }

                    buffer.clear()
                    firstPosition = null
                    lastPosition = null
                }

                textPositions.forEach { position ->
                    val unicode = position.unicode ?: ""

                    if (unicode.isBlank()) {
                        flushCurrentWord()
                    } else {
                        if (firstPosition == null) {
                            firstPosition = position
                        }
                        buffer.append(unicode)
                        lastPosition = position
                    }
                }

                flushCurrentWord()
            }
        }.apply {
            sortByPosition = true
            startPage = zeroBasedPageIndex + 1
            endPage = zeroBasedPageIndex + 1
            lineSeparator = "\n"
            wordSeparator = " "
        }

        stripper.getText(document)

        return words
    }

    private fun extractColumnLines(
        words: List<PositionedWord>,
        pageWidth: Float,
        day: WeekDay
    ): List<String> {
        val columnWidth = pageWidth / TOTAL_WEEK_DAYS
        val columnIndex = day.sortOrder
        val leftBoundary = columnIndex * columnWidth
        val rightBoundary = leftBoundary + columnWidth

        val wordsInColumn = words
            .filter { word ->
                val centerX = (word.xStart + word.xEnd) / 2f
                centerX in leftBoundary..<rightBoundary
            }
            .sortedWith(
                compareBy(
                    { it.y },
                    { it.xStart }
                )
            )

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
                if (abs(word.y - averageY) <= LINE_MERGE_TOLERANCE) {
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
            val normalizedLine = normalizeForMatching(line)

            if (isWeekDayHeaderLine(normalizedLine)) {
                return@forEach
            }

            val slotMatch = matchMealSlotHeading(
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

        val normalizedLine = normalizeMealText(line)
        if (normalizedLine.isBlank()) return

        val key = day to slot
        val bucket = collectedTexts.getOrPut(key) { mutableListOf() }

        if (bucket.none { normalizeMealText(it) == normalizedLine }) {
            bucket += normalizedLine
        }
    }

    private fun buildDraftFromCollectedTexts(
        sourceFileName: String,
        rawExtractedText: String,
        collectedTexts: Map<Pair<WeekDay, MealSlotType>, List<String>>,
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

            populatedCells < EXPECTED_WEEKLY_SLOTS -> {
                finalWarnings += ImportWarning(
                    message = "Riconoscimento parziale: trovate $populatedCells celle con contenuto su $EXPECTED_WEEKLY_SLOTS slot attesi."
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
            warnings = finalWarnings,
            status = status
        )
    }

    private fun buildDraftWithSequentialFallback(
        sourceFileName: String,
        rawExtractedText: String
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
            val normalizedForMatch = normalizeForMatching(originalLine)
            val dayAtStart = matchWeekDayAtStart(normalizedForMatch)
            val slotMatch = matchMealSlotHeading(
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
            warnings = warnings
        )
    }

    private fun buildCanonicalCells(
        collectedTexts: Map<Pair<WeekDay, MealSlotType>, List<String>>
    ): List<ImportedMealCell> {
        return WeekDay.orderedValues().flatMap { day ->
            MealSlotType.orderedValues().map { slot ->
                val key = day to slot
                val rawText = collectedTexts[key]
                    ?.joinToString(separator = "\n")
                    ?.trim()
                    .orEmpty()

                ImportedMealCell(
                    id = "${day.name}_${slot.name}",
                    dayOfWeek = day,
                    mealSlotType = slot,
                    rawText = rawText,
                    normalizedText = normalizeMealText(rawText),
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

    private fun normalizeMealText(text: String): String {
        return text
            .replace("\r\n", "\n")
            .replace("\r", "\n")
            .lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .joinToString(separator = "\n")
    }

    private fun normalizeForMatching(text: String): String {
        return Normalizer.normalize(text.lowercase(), Normalizer.Form.NFD)
            .replace("\\p{M}+".toRegex(), "")
            .replace('’', '\'')
            .replace('–', '-')
            .replace('—', '-')
            .replace('\u00A0', ' ')
            .replace("\\s+".toRegex(), " ")
            .trim()
    }

    private fun matchWeekDayAtStart(normalizedLine: String): WeekDay? {
        return weekDayAliases.entries.firstOrNull { entry ->
            entry.value.any { alias ->
                normalizedLine == alias ||
                        normalizedLine.startsWith("$alias ") ||
                        normalizedLine.startsWith("$alias:") ||
                        normalizedLine.startsWith("$alias -")
            }
        }?.key
    }

    private fun isWeekDayHeaderLine(normalizedLine: String): Boolean {
        return weekDayAliases.values.flatten().any { alias ->
            normalizedLine == alias
        }
    }

    private fun matchMealSlotHeading(
        originalLine: String,
        normalizedLine: String
    ): SlotHeadingMatch? {
        mealSlotAliases.forEach { (slot, aliases) ->
            aliases.forEach { alias ->
                if (normalizedLine == alias) {
                    return SlotHeadingMatch(
                        slot = slot,
                        inlineText = null
                    )
                }

                val prefixes = listOf(
                    "$alias:",
                    "$alias -",
                    "$alias –",
                    "$alias —"
                )

                prefixes.firstOrNull { prefix ->
                    normalizedLine.startsWith(prefix)
                }?.let { _ ->
                    val inlineText = originalLine
                        .substringAfter(":", missingDelimiterValue = originalLine)
                        .substringAfter(" - ", missingDelimiterValue = originalLine)
                        .substringAfter(" – ", missingDelimiterValue = originalLine)
                        .substringAfter(" — ", missingDelimiterValue = originalLine)
                        .trim()

                    return SlotHeadingMatch(
                        slot = slot,
                        inlineText = inlineText.takeIf { it.isNotBlank() }
                    )
                }
            }
        }

        return null
    }

    private fun countDistinctWeekDays(normalizedPageText: String): Int {
        return weekDayAliases.entries.count { (_, aliases) ->
            aliases.any { alias ->
                normalizedPageText.contains(alias)
            }
        }
    }

    private fun countMealSlotHeadingOccurrences(normalizedPageText: String): Int {
        return mealSlotAliases.values.flatten().sumOf { alias ->
            "\\b${Regex.escape(alias)}\\b".toRegex().findAll(normalizedPageText).count()
        }
    }

    private fun computeWeeklyHeaderScore(normalizedPageText: String): Int {
        val weekdaysScore = countDistinctWeekDays(normalizedPageText) * 10
        val slotsScore = countMealSlotHeadingOccurrences(normalizedPageText)
        return weekdaysScore + slotsScore
    }

    companion object {
        private const val EXPECTED_WEEKLY_SLOTS = 35
        private const val TOTAL_WEEK_DAYS = 7f
        private const val LINE_MERGE_TOLERANCE = 4.5f
        private const val CONTINUATION_MIN_SLOT_HEADINGS = 8

        private val weekDayAliases: Map<WeekDay, List<String>> = mapOf(
            WeekDay.MONDAY to listOf("lunedi", "lun"),
            WeekDay.TUESDAY to listOf("martedi", "mar"),
            WeekDay.WEDNESDAY to listOf("mercoledi", "mer"),
            WeekDay.THURSDAY to listOf("giovedi", "gio"),
            WeekDay.FRIDAY to listOf("venerdi", "ven"),
            WeekDay.SATURDAY to listOf("sabato", "sab"),
            WeekDay.SUNDAY to listOf("domenica", "dom")
        )

        private val mealSlotAliases: Map<MealSlotType, List<String>> = mapOf(
            MealSlotType.BREAKFAST to listOf(
                "colazione"
            ),
            MealSlotType.MORNING_SNACK to listOf(
                "spuntino mattina",
                "spuntino di meta mattina",
                "spuntino meta mattina",
                "meta mattina"
            ),
            MealSlotType.LUNCH to listOf(
                "pranzo"
            ),
            MealSlotType.AFTERNOON_SNACK to listOf(
                "spuntino pomeridiano",
                "spuntino pomeriggio",
                "spuntino del pomeriggio",
                "pomeriggio"
            ),
            MealSlotType.DINNER to listOf(
                "cena"
            )
        )
    }
}

private data class PositionedWord(
    val text: String,
    val xStart: Float,
    val xEnd: Float,
    val y: Float
)

private data class PageScan(
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

private data class WeeklyParseResult(
    val collectedTexts: Map<Pair<WeekDay, MealSlotType>, List<String>>,
    val warnings: List<ImportWarning>
)

private data class SlotHeadingMatch(
    val slot: MealSlotType,
    val inlineText: String?
)