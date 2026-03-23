package it.lagioiaproductions.nutrislot.data.importer

import it.lagioiaproductions.nutrislot.domain.model.ImportedMealOption
import it.lagioiaproductions.nutrislot.domain.model.ImportedMealRule
import it.lagioiaproductions.nutrislot.domain.model.MealOptionSourceType
import it.lagioiaproductions.nutrislot.domain.model.MealSlotType

internal class PdfAdditionalContentExtractor {

    fun extractAdditionalMealOptions(
        pageScans: List<PageScan>
    ): List<ImportedMealOption> {
        val options = mutableListOf<ImportedMealOption>()

        pageScans.forEach { scan ->
            extractSupportedSections(scan.fullText).forEach { section ->
                val cleanedOptions = extractBulletBlocks(section.bodyLines)
                    .map(::cleanOptionBlock)
                    .filter { it.isNotBlank() }
                    .distinctBy { PdfImportTextNormalization.normalizeMealText(it) }

                cleanedOptions.forEachIndexed { index, optionText ->
                    section.targetSlots.forEach { slotType ->
                        options += ImportedMealOption(
                            id = "extra_p${scan.pageNumber}_${slotType.name}_${section.sourceType.name}_$index",
                            mealSlotType = slotType,
                            title = buildOptionTitle(
                                slotType = slotType,
                                sourceType = section.sourceType
                            ),
                            rawText = optionText,
                            normalizedText = PdfImportTextNormalization.normalizeMealText(optionText),
                            sourceType = section.sourceType,
                            tags = inferOptionTags(
                                optionText = optionText,
                                sourceType = section.sourceType,
                                slotType = slotType
                            ),
                            pageNumber = scan.pageNumber
                        )
                    }
                }
            }
        }

        return options.distinctBy { option ->
            "${option.mealSlotType.name}|${option.normalizedText}|${option.sourceType.name}"
        }
    }

    fun extractMealRules(
        pageScans: List<PageScan>
    ): List<ImportedMealRule> {
        pageScans.firstOrNull { scan ->
            scan.normalizedFullText.contains("schema di riferimento per la costruzione dei pasti")
        } ?: return emptyList()

        val rules = mutableListOf<ImportedMealRule>()

        MealSlotType.orderedValues().forEach { slot ->
            val components = when (slot) {
                MealSlotType.BREAKFAST -> listOf(
                    "Fonte glucidica",
                    "Fonte proteica o lipidica"
                )

                MealSlotType.MORNING_SNACK -> listOf(
                    "Fonte glucidica",
                    "Fonte proteica o lipidica"
                )

                MealSlotType.LUNCH -> listOf(
                    "Fonte glucidica",
                    "Fonte proteica",
                    "Fonte lipidica (olio EVO)",
                    "Verdure"
                )

                MealSlotType.AFTERNOON_SNACK -> listOf(
                    "Fonte glucidica",
                    "Fonte proteica o lipidica"
                )

                MealSlotType.DINNER -> listOf(
                    "Fonte glucidica",
                    "Fonte proteica",
                    "Fonte lipidica (olio EVO)",
                    "Verdure"
                )
            }

            rules += ImportedMealRule(
                id = slot.name.lowercase(),
                mealSlotType = slot,
                label = "Schema di riferimento ${slot.displayName}",
                requiredComponents = components
            )
        }

        return rules
    }

    private fun extractSupportedSections(
        fullText: String
    ): List<ParsedOptionSection> {
        val lines = fullText
            .replace("\r\n", "\n")
            .replace("\r", "\n")
            .lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }

        if (lines.isEmpty()) return emptyList()

        val sections = mutableListOf<ParsedOptionSection>()
        var currentDefinition: SectionDefinition? = null
        var currentBody = mutableListOf<String>()

        fun flushSection() {
            val definition = currentDefinition ?: return
            if (currentBody.isNotEmpty()) {
                sections += ParsedOptionSection(
                    sourceType = definition.sourceType,
                    targetSlots = definition.targetSlots,
                    bodyLines = currentBody.toList()
                )
            }
            currentDefinition = null
            currentBody = mutableListOf()
        }

        lines.forEach { line ->
            val normalizedLine = PdfImportTextNormalization.normalizeForMatching(line)
            val matchedDefinition = SECTION_DEFINITIONS.firstOrNull { definition ->
                definition.headingAliases.any { alias ->
                    normalizedLine == alias ||
                            normalizedLine.startsWith("$alias:") ||
                            normalizedLine.startsWith("$alias ")
                }
            }

            if (matchedDefinition != null) {
                flushSection()
                currentDefinition = matchedDefinition
                return@forEach
            }

            if (currentDefinition != null) {
                currentBody += line
            }
        }

        flushSection()
        return sections
    }

    private fun extractBulletBlocks(
        bodyLines: List<String>
    ): List<String> {
        val blocks = mutableListOf<String>()
        var currentParts = mutableListOf<String>()

        fun flushCurrent() {
            val text = currentParts
                .joinToString(separator = " ")
                .replace(Regex("\\s+"), " ")
                .replace(Regex("(\\d)\\s*-\\s*(\\d)"), "$1-$2")
                .trim()

            if (text.isNotBlank()) {
                blocks += text
            }
            currentParts = mutableListOf()
        }

        bodyLines.forEach { rawLine ->
            val line = rawLine.trim()
            if (line.isBlank()) return@forEach

            val normalizedLine = PdfImportTextNormalization.normalizeForMatching(line)
            val isBulletLine = BULLET_PREFIX_REGEX.containsMatchIn(line)

            when {
                isBulletLine -> {
                    flushCurrent()
                    currentParts += line.removeBulletPrefix()
                }

                currentParts.isNotEmpty() -> {
                    if (!looksLikeStandaloneHeading(normalizedLine)) {
                        currentParts += line
                    }
                }
            }
        }

        flushCurrent()
        return blocks
    }

    private fun cleanOptionBlock(
        rawBlock: String
    ): String {
        val normalized = rawBlock
            .removeBulletPrefix()
            .replace(Regex("\\s+"), " ")
            .replace(Regex("\\s+([,;:.])"), "$1")
            .replace(Regex(":(?=\\S)"), ": ")
            .replace(Regex("(\\d)\\s*-\\s*(\\d)"), "$1-$2")
            .trim()
            .trim('.', ';', ' ')

        if (normalized == ".") return ""

        val normalizedForMatch = PdfImportTextNormalization.normalizeForMatching(normalized)
        if (normalizedForMatch.startsWith("nb") && !ALL_QUANTITY_REGEX.containsMatchIn(normalized)) {
            return ""
        }

        return normalized
    }

    private fun inferOptionTags(
        optionText: String,
        sourceType: MealOptionSourceType,
        slotType: MealSlotType
    ): List<String> {
        val normalized = PdfImportTextNormalization.normalizeForMatching(optionText)
        val tags = linkedSetOf<String>()

        tags += slotType.displayName

        when (sourceType) {
            MealOptionSourceType.BREAKFAST_ALTERNATIVE -> tags += "colazione extra"
            MealOptionSourceType.SNACK_ALTERNATIVE -> tags += "spuntino extra"
            MealOptionSourceType.PRE_WORKOUT -> tags += "pre allenamento"
            MealOptionSourceType.LUNCH_DINNER_ALTERNATIVE -> tags += "pranzo/cena"
            MealOptionSourceType.FUORI_CASA -> tags += "fuori casa"
            MealOptionSourceType.WEEKLY_APPENDIX -> tags += "ricetta"
            MealOptionSourceType.OTHER -> Unit
        }

        if ("panino" in normalized || "panini" in normalized) tags += "panino"
        if ("piadina" in normalized) tags += "piadina"
        if ("frisella" in normalized || "friselle" in normalized) tags += "frisella"
        if ("insalatona" in normalized || "insalata" in normalized) tags += "insalata"
        if ("pasta fredda" in normalized) tags += "pasta fredda"
        if ("insalata di riso" in normalized) tags += "insalata di riso"
        if ("yogurt" in normalized) tags += "yogurt"
        if ("pancake" in normalized) tags += "pancake"

        return tags.toList()
    }

    private fun buildOptionTitle(
        slotType: MealSlotType,
        sourceType: MealOptionSourceType
    ): String {
        val sourceLabel = when (sourceType) {
            MealOptionSourceType.BREAKFAST_ALTERNATIVE -> "Alternative"
            MealOptionSourceType.SNACK_ALTERNATIVE -> "Alternative"
            MealOptionSourceType.PRE_WORKOUT -> "Pre-allenamento"
            MealOptionSourceType.LUNCH_DINNER_ALTERNATIVE -> "Alternative"
            MealOptionSourceType.FUORI_CASA -> "Fuori casa"
            MealOptionSourceType.WEEKLY_APPENDIX -> "Ricette"
            MealOptionSourceType.OTHER -> "Extra"
        }

        return "${slotType.displayName} • $sourceLabel"
    }

    private fun looksLikeStandaloneHeading(
        normalizedLine: String
    ): Boolean {
        return SECTION_DEFINITIONS.any { definition ->
            definition.headingAliases.any { alias ->
                normalizedLine == alias || normalizedLine.startsWith("$alias:")
            }
        }
    }

    private fun String.removeBulletPrefix(): String {
        return replace(BULLET_PREFIX_REGEX, "")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private data class SectionDefinition(
        val headingAliases: List<String>,
        val sourceType: MealOptionSourceType,
        val targetSlots: List<MealSlotType>
    )

    private data class ParsedOptionSection(
        val sourceType: MealOptionSourceType,
        val targetSlots: List<MealSlotType>,
        val bodyLines: List<String>
    )

    companion object {
        private val BULLET_PREFIX_REGEX = Regex("^[•\\-–—]\\s*")

        private val ALL_QUANTITY_REGEX = Regex(
            pattern = "(N\\.?\\s*\\d+|\\d+[.,]?\\d*\\s*(kg|g|mg|l|ml|cl|pz))\\b",
            option = RegexOption.IGNORE_CASE
        )

        private val SECTION_DEFINITIONS = listOf(
            SectionDefinition(
                headingAliases = listOf(
                    "ulteriori alternative per la colazione"
                ),
                sourceType = MealOptionSourceType.BREAKFAST_ALTERNATIVE,
                targetSlots = listOf(MealSlotType.BREAKFAST)
            ),
            SectionDefinition(
                headingAliases = listOf(
                    "ulteriori alternative per gli spuntini",
                    "ulteriori alternative per gli spuntini pomeridiani",
                    "ulteriori alternative per gli spuntini pomeridiani (allenamento no)",
                    "ulteriori alternative per gli spuntini pomeridiani allenamento no"
                ),
                sourceType = MealOptionSourceType.SNACK_ALTERNATIVE,
                targetSlots = listOf(MealSlotType.AFTERNOON_SNACK)
            ),
            SectionDefinition(
                headingAliases = listOf(
                    "allenamento pomeridiano: spuntino pre-allenamento",
                    "allenamento pomeridiano spuntino pre-allenamento"
                ),
                sourceType = MealOptionSourceType.PRE_WORKOUT,
                targetSlots = listOf(MealSlotType.AFTERNOON_SNACK)
            ),
            SectionDefinition(
                headingAliases = listOf(
                    "ulteriori ricette di pranzi bilanciati"
                ),
                sourceType = MealOptionSourceType.WEEKLY_APPENDIX,
                targetSlots = listOf(MealSlotType.LUNCH)
            ),
            SectionDefinition(
                headingAliases = listOf(
                    "ulteriori alternative per il pranzo/cena",
                    "ulteriori alternative per pranzo/cena"
                ),
                sourceType = MealOptionSourceType.LUNCH_DINNER_ALTERNATIVE,
                targetSlots = listOf(MealSlotType.LUNCH, MealSlotType.DINNER)
            ),
            SectionDefinition(
                headingAliases = listOf(
                    "possibili opzioni da consumare al bar"
                ),
                sourceType = MealOptionSourceType.FUORI_CASA,
                targetSlots = listOf(MealSlotType.LUNCH, MealSlotType.DINNER)
            )
        )
    }
}