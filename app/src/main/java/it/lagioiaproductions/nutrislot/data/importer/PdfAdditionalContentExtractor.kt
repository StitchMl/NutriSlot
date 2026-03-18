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
            val section = classifyAdditionalOptionsSection(scan.normalizedFullText) ?: return@forEach
            val slotType = section.slotType
            val sourceType = section.sourceType

            val normalizedLines = scan.fullText
                .replace("\r\n", "\n")
                .replace("\r", "\n")
                .lines()
                .map { it.trim() }
                .filter { it.isNotBlank() }

            val bulletLines = normalizedLines.filter { line ->
                val trimmed = line.trim()
                trimmed.startsWith("•") || trimmed.startsWith("-")
            }

            val cleanedOptions = bulletLines
                .map { it.removePrefix("•").removePrefix("-").trim() }
                .filter { it.isNotBlank() }
                .filterNot { line ->
                    val normalized = PdfImportTextNormalization.normalizeForMatching(line)
                    PdfImportTextNormalization.mealSlotAliases.values.flatten().any { alias -> normalized == alias } ||
                            normalized.startsWith("nb") ||
                            normalized == "."
                }
                .distinctBy { PdfImportTextNormalization.normalizeMealText(it) }

            cleanedOptions.forEachIndexed { index, optionText ->
                options += ImportedMealOption(
                    id = "extra_p${scan.pageNumber}_${slotType.name}_$index",
                    mealSlotType = slotType,
                    title = buildOptionTitle(
                        slotType = slotType,
                        pageNumber = scan.pageNumber,
                        index = index + 1
                    ),
                    rawText = optionText,
                    normalizedText = PdfImportTextNormalization.normalizeMealText(optionText),
                    sourceType = sourceType,
                    pageNumber = scan.pageNumber
                )
            }
        }

        return options.distinctBy { option ->
            "${option.mealSlotType.name}|${option.normalizedText}|${option.sourceType.name}"
        }
    }

    fun extractMealRules(
        pageScans: List<PageScan>
    ): List<ImportedMealRule> {
        val referencePage = pageScans.firstOrNull { scan ->
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
                id = "rule_${slot.name.lowercase()}_page_${referencePage.pageNumber}",
                mealSlotType = slot,
                label = "Schema di riferimento ${slot.displayName}",
                requiredComponents = components,
                pageNumber = referencePage.pageNumber
            )
        }

        return rules
    }

    private fun classifyAdditionalOptionsSection(
        normalizedPageText: String
    ): AdditionalOptionSection? {
        return when {
            normalizedPageText.contains("ulteriori alternative per la colazione") -> {
                AdditionalOptionSection(
                    slotType = MealSlotType.BREAKFAST,
                    sourceType = MealOptionSourceType.BREAKFAST_ALTERNATIVE
                )
            }
            normalizedPageText.contains("ulteriori alternative per gli spuntini pomeridiani") -> {
                AdditionalOptionSection(
                    slotType = MealSlotType.AFTERNOON_SNACK,
                    sourceType = MealOptionSourceType.SNACK_ALTERNATIVE
                )
            }
            normalizedPageText.contains("allenamento pomeridiano") &&
                    normalizedPageText.contains("spuntino pre-allenamento") -> {
                AdditionalOptionSection(
                    slotType = MealSlotType.AFTERNOON_SNACK,
                    sourceType = MealOptionSourceType.PRE_WORKOUT
                )
            }
            normalizedPageText.contains("ulteriori ricette di pranzi bilanciati") -> {
                AdditionalOptionSection(
                    slotType = MealSlotType.LUNCH,
                    sourceType = MealOptionSourceType.WEEKLY_APPENDIX
                )
            }
            normalizedPageText.contains("ulteriori alternative per il pranzo/cena") -> {
                AdditionalOptionSection(
                    slotType = MealSlotType.LUNCH,
                    sourceType = MealOptionSourceType.LUNCH_DINNER_ALTERNATIVE
                )
            }
            normalizedPageText.contains("possibili opzioni da consumare al bar") -> {
                AdditionalOptionSection(
                    slotType = MealSlotType.LUNCH,
                    sourceType = MealOptionSourceType.FUORI_CASA
                )
            }
            else -> null
        }
    }

    private fun buildOptionTitle(
        slotType: MealSlotType,
        pageNumber: Int,
        index: Int
    ): String {
        return "${slotType.displayName} extra • pag. $pageNumber • opzione $index"
    }
}