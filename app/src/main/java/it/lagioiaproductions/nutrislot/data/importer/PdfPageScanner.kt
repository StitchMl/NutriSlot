package it.lagioiaproductions.nutrislot.data.importer

import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import com.tom_roush.pdfbox.text.TextPosition

internal class PdfPageScanner {

    fun scanPage(
        document: PDDocument,
        zeroBasedPageIndex: Int
    ): PageScan {
        val fullText = extractPageText(
            document = document,
            zeroBasedPageIndex = zeroBasedPageIndex
        )

        val normalized = PdfImportTextNormalization.normalizeForMatching(fullText)
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
            hasWeekdayHeader = PdfImportTextNormalization.countDistinctWeekDays(normalized) >= 5,
            mealSlotHeadingOccurrences = PdfImportTextNormalization.countMealSlotHeadingOccurrences(normalized),
            isReferencePage = normalized.contains("schema di riferimento per la costruzione dei pasti"),
            looksLikeReferenceTemplate = normalized.contains("fonte glucidica") &&
                    normalized.contains("fonte proteica") &&
                    normalized.contains("verdure"),
            isAppendixPage = normalized.contains("grammature di riferimento per pranzo e cena") ||
                    normalized.contains("esempi pratici di pasti bilanciati") ||
                    normalized.contains("fonte proteica") && normalized.contains("fonte lipidica: olio evo"),
            weeklyHeaderScore = PdfImportTextNormalization.computeWeeklyHeaderScore(normalized)
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
}