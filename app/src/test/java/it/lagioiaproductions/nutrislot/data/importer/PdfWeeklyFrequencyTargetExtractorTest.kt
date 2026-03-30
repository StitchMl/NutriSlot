package it.lagioiaproductions.nutrislot.data.importer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class PdfWeeklyFrequencyTargetExtractorTest {

    private val extractor = PdfWeeklyFrequencyTargetExtractor()

    @Test
    fun `extractWeeklyTargets picks all expected targets from a bullet list`() {
        val fullText = """
            - Consumare almeno 5 porzioni di frutta e verdura al giorno per assicurarsi un quantitativo sufficiente di antiossidanti
            in grado di proteggerci dai radicali liberi.
            - Bere almeno 2L di acqua/die per favorire l'escrezione renale
            - Consumare massimo N.3 caffe/the al giorno
            - Carne bianca 2-3 volte a settimana
            - Carne rossa 1 volta a settimana
            - Affettati 1 volta a settimana
            - Uova 1 porzione a settimana (N. 2 uova)
            - Formaggi 2 volte a settimana
            - Patate 1 volta a settimana
            - Piatto unico 2-3 volte a settimana (legumi + cereal)
            - Pesce 3-4 volte a settimana
        """.trimIndent()

        val pageScan = PageScan(
            zeroBasedIndex = 0,
            pageNumber = 1,
            pageWidth = 595f,
            fullText = fullText,
            normalizedFullText = PdfImportTextNormalization.normalizeForMatching(fullText),
            positionedWords = emptyList(),
            hasWeekdayHeader = false,
            mealSlotHeadingOccurrences = 0,
            isReferencePage = false,
            looksLikeReferenceTemplate = false,
            isAppendixPage = true,
            weeklyHeaderScore = 0
        )

        val targets = extractor.extractWeeklyTargets(listOf(pageScan))
        val byKey = targets.associateBy { it.canonicalKey }

        assertEquals(11, targets.size)

        assertTarget(
            byKey = byKey,
            key = "frutta e verdura",
            title = "Frutta E Verdura",
            min = 5,
            max = null,
            portionText = null
        )
        assertTarget(
            byKey = byKey,
            key = "acqua",
            title = "Acqua",
            min = 2000,
            max = null,
            portionText = "2 l"
        )
        assertTarget(
            byKey = byKey,
            key = "caffe e the",
            title = "Caffe E The",
            min = null,
            max = 3,
            portionText = null
        )
        assertTarget(
            byKey = byKey,
            key = "carne bianca",
            title = "Carne Bianca",
            min = 2,
            max = 3,
            portionText = null
        )
        assertTarget(
            byKey = byKey,
            key = "carne rossa",
            title = "Carne Rossa",
            min = 1,
            max = 1,
            portionText = null
        )
        assertTarget(
            byKey = byKey,
            key = "affettati",
            title = "Affettati",
            min = 1,
            max = 1,
            portionText = null
        )
        assertTarget(
            byKey = byKey,
            key = "uova",
            title = "Uova",
            min = 1,
            max = 1,
            portionText = "N. 2 uova"
        )
        assertTarget(
            byKey = byKey,
            key = "formaggi",
            title = "Formaggi",
            min = 2,
            max = 2,
            portionText = null
        )
        assertTarget(
            byKey = byKey,
            key = "patate",
            title = "Patate",
            min = 1,
            max = 1,
            portionText = null
        )
        assertTarget(
            byKey = byKey,
            key = "piatto unico",
            title = "Piatto Unico",
            min = 2,
            max = 3,
            portionText = "legumi + cereal"
        )
        assertTarget(
            byKey = byKey,
            key = "pesce",
            title = "Pesce",
            min = 3,
            max = 4,
            portionText = null
        )
    }

    @Test
    fun `extractWeeklyTargets ignores noisy notes from real extracted appendix pages`() {
        val page14 = """
            Consigli per creare solide fondamenta
            • Si consiglia di mantenersi il piu? attivo possibile, cercando di fare almeno 150 minuti di attività fisica a settimana
            • Ridurre il consumo di carne rossa, alimenti processati, carboidrati raffinati presenti nei prodotti da forno
            • Prediligere il consumo di cereali integrali, frutta fresca e verdura di stagione
            • Preferire il consumo di olio EVO
            • Consumare almeno 5 porzioni di frutta e verdura al giorno per assicurarsi un quantitativo sufficiente di antiossidanti
            in grado di proteggerci dai radicali liberi.
            • Bere almeno 2L di acqua/die per favorire l'escrezione renale
            • Consumare con moderazione pesci grassi (tonno, salmone)
            • Consumare massimo N.3 caffè/thè al giorno
        """.trimIndent()

        val page15 = """
            Gli alimenti:
            Carne Frequenze di consumo
            - Carne bianca: pollo, tacchino, coniglio, faraona senza pelle
            • Carne bianca 2-3 volte a settimana
            - Carne rossa: vitello magro, manzo, maiale
            • Carne rossa 1 volta a settimana
            - Affettati: prosciutto cotto e crudo, fesa di tacchino, bresaola
            • Affettati 1 volta a settimana
            Pesce
            • Uova 1 porzione a settimana (N. 2 uova)
            - Pesci magri: merluzzo, spigola, sogliola, orata, nasello, triglia, rombo, trota
            • Formaggi 2 volte a settimana
            - Pesci semi-grassi: sgombro, tonno salmone
            • Patate 1 volta a settimana
            - Conservato: tonno sott’olio, al naturale o sgombro • Piatto unico 2-3 volte a settimana (legumi + cereal)
            Formaggi: • Pesce 3-4 volte a settimana
        """.trimIndent()

        val targets = extractor.extractWeeklyTargets(
            listOf(
                buildPageScan(page14, pageNumber = 14),
                buildPageScan(page15, pageNumber = 15)
            )
        )

        assertEquals(
            setOf(
                "acqua",
                "affettati",
                "caffe e the",
                "carne bianca",
                "carne rossa",
                "formaggi",
                "frutta e verdura",
                "patate",
                "pesce",
                "piatto unico",
                "uova"
            ),
            targets.map { it.canonicalKey }.toSet()
        )
    }

    private fun buildPageScan(
        text: String,
        pageNumber: Int
    ): PageScan {
        return PageScan(
            zeroBasedIndex = pageNumber - 1,
            pageNumber = pageNumber,
            pageWidth = 595f,
            fullText = text,
            normalizedFullText = PdfImportTextNormalization.normalizeForMatching(text),
            positionedWords = emptyList(),
            hasWeekdayHeader = false,
            mealSlotHeadingOccurrences = 0,
            isReferencePage = false,
            looksLikeReferenceTemplate = false,
            isAppendixPage = true,
            weeklyHeaderScore = 0
        )
    }

    private fun assertTarget(
        byKey: Map<String, it.lagioiaproductions.nutrislot.domain.model.ImportedWeeklyFrequencyTarget>,
        key: String,
        title: String,
        min: Int?,
        max: Int?,
        portionText: String?
    ) {
        val target = byKey[key]
        assertNotNull("Target mancante: $key", target)
        assertEquals(title, target?.title)
        assertEquals(min, target?.minimumTimesPerWeek)
        assertEquals(max, target?.maximumTimesPerWeek)
        assertEquals(portionText, target?.portionText)
    }
}
