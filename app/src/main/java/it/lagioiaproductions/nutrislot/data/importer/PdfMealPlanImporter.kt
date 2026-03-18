package it.lagioiaproductions.nutrislot.data.importer

import android.content.Context
import android.net.Uri
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import it.lagioiaproductions.nutrislot.domain.model.ImportedPlanDraft

class PdfMealPlanImporter {

    private val pageScanner = PdfPageScanner()
    private val weeklyTableParser = PdfWeeklyTableParser()
    private val additionalContentExtractor = PdfAdditionalContentExtractor()

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
            pageScanner.scanPage(
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

        val additionalOptions = additionalContentExtractor.extractAdditionalMealOptions(pageScans)
        val mealRules = additionalContentExtractor.extractMealRules(pageScans)
        val weeklyParseResult = weeklyTableParser.tryParseWeeklyTable(pageScans)

        return if (weeklyParseResult != null) {
            weeklyTableParser.buildDraftFromCollectedTexts(
                sourceFileName = sourceFileName,
                rawExtractedText = rawExtractedText,
                collectedTexts = weeklyParseResult.collectedTexts,
                additionalOptions = additionalOptions,
                mealRules = mealRules,
                warnings = weeklyParseResult.warnings
            )
        } else {
            weeklyTableParser.buildDraftWithSequentialFallback(
                sourceFileName = sourceFileName,
                rawExtractedText = rawExtractedText,
                additionalOptions = additionalOptions,
                mealRules = mealRules
            )
        }
    }
}