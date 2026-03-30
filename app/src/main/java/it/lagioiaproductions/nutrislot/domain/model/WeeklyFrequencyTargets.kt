package it.lagioiaproductions.nutrislot.domain.model

import java.text.Normalizer

data class ImportedWeeklyFrequencyTarget(
    val id: String,
    val title: String,
    val canonicalKey: String,
    val portionText: String? = null,
    val minimumTimesPerWeek: Int? = null,
    val maximumTimesPerWeek: Int? = null,
    val matchTerms: List<String> = emptyList(),
    val pageNumber: Int? = null,
    val sourceText: String? = null
)

data class WeeklyFrequencyTarget(
    val id: String,
    val planId: String,
    val title: String,
    val canonicalKey: String,
    val portionText: String? = null,
    val minimumTimesPerWeek: Int? = null,
    val maximumTimesPerWeek: Int? = null,
    val matchTerms: List<String> = emptyList(),
    val pageNumber: Int? = null,
    val sourceText: String? = null
)

internal object WeeklyFrequencyTargetSupport {
    enum class FrequencyTargetPeriod {
        DAY,
        WEEK
    }

    enum class FrequencyTargetMeasure {
        OCCURRENCES,
        PORTIONS,
        MILLILITERS
    }

    data class FrequencyTargetRule(
        val period: FrequencyTargetPeriod,
        val measure: FrequencyTargetMeasure,
        val minimumValue: Int? = null,
        val maximumValue: Int? = null
    )

    data class WeeklyFrequencyRule(
        val minimumTimesPerWeek: Int? = null,
        val maximumTimesPerWeek: Int? = null
    )

    private val aliasMap = mapOf(
        "acqua" to listOf("acqua"),
        "affettati" to listOf("affettati", "affettato", "prosciutto", "bresaola", "fesa di tacchino", "salume"),
        "carne bianca" to listOf("carne bianca", "pollo", "tacchino", "coniglio"),
        "carne rossa" to listOf("carne rossa", "manzo", "vitello", "hamburger"),
        "caffe e the" to listOf("caffe", "caffe'", "caffè", "the", "thè", "te", "tea"),
        "formaggi" to listOf("formaggi", "formaggio", "ricotta", "mozzarella", "primo sale", "feta", "robiola", "certosa", "philadelphia", "parmigiano", "grana"),
        "frutta e verdura" to listOf(
            "frutta",
            "frutto",
            "banana",
            "mela",
            "pera",
            "kiwi",
            "fragole",
            "arancia",
            "verdura",
            "verdure",
            "ortaggi",
            "insalata",
            "lattuga",
            "songino",
            "rughetta",
            "radicchio",
            "zucchine"
        ),
        "legumi" to listOf("legumi", "legume", "fagioli", "lenticchie", "ceci", "piselli", "fave", "lupini", "hummus", "humus"),
        "patate" to listOf("patate"),
        "pesce" to listOf("pesce", "salmone", "tonno", "sgombro", "orata", "spigola", "merluzzo", "trota", "nasello", "sogliola", "gamberetti", "polpo", "calamari"),
        "piatto unico" to listOf("piatto unico"),
        "uova" to listOf("uova", "uovo", "frittata")
    )

    private val carbohydrateTerms = listOf(
        "pasta", "riso", "couscous", "orzo", "farro", "pane", "patate", "gnocchi", "frisella", "piadina"
    )

    private val legumeTerms = aliasMap.getValue("legumi")

    fun normalizeKey(text: String): String {
        return Normalizer.normalize(text.lowercase(), Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .replace("'", " ")
            .replace(Regex("[^a-z0-9 ]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    fun formatTitle(raw: String): String {
        val cleaned = normalizeKey(raw)
            .replace(Regex("^(il|lo|la|i|gli|le)\\s+"), "")
            .trim()

        return cleaned.split(" ")
            .filter { it.isNotBlank() }
            .joinToString(" ") { token ->
                token.replaceFirstChar { char ->
                    if (char.isLowerCase()) char.titlecase() else char.toString()
                }
            }
    }

    fun resolveMatchTerms(
        title: String,
        sourceText: String? = null
    ): List<String> {
        val canonicalKey = normalizeKey(title)
        val directAliases = aliasMap[canonicalKey]
        if (directAliases != null) return directAliases

        sourceText
            ?.let(::normalizeKey)
            ?.let { normalizedSource ->
                aliasMap.entries.firstOrNull { (_, aliases) ->
                    aliases.any { alias -> normalizedSource.contains(normalizeKey(alias)) }
                }?.value
            }
            ?.let { return it }

        return listOf(canonicalKey)
    }

    fun parseFrequencyRule(
        text: String
    ): WeeklyFrequencyRule? {
        val rule = parseFrequencyTargetRule(text) ?: return null
        if (rule.period != FrequencyTargetPeriod.WEEK) return null
        if (rule.measure == FrequencyTargetMeasure.MILLILITERS) {
            return null
        }

        return WeeklyFrequencyRule(
            minimumTimesPerWeek = rule.minimumValue,
            maximumTimesPerWeek = rule.maximumValue
        )
    }

    fun parseFrequencyTargetRule(
        text: String
    ): FrequencyTargetRule? {
        val normalized = normalizeForRuleParsing(text)
        val period = detectFrequencyPeriod(normalized) ?: return null
        val measure = detectFrequencyMeasure(normalized)

        if (measure == FrequencyTargetMeasure.MILLILITERS) {
            return parseVolumeRule(
                normalized = normalized,
                period = period
            )
        }

        if (!containsCountLikeRule(normalized)) return null

        parseMaximumCountRule(normalized)?.let { maximum ->
            return FrequencyTargetRule(
                period = period,
                measure = measure,
                minimumValue = null,
                maximumValue = maximum
            )
        }

        parseMinimumCountRule(normalized)?.let { minimum ->
            return FrequencyTargetRule(
                period = period,
                measure = measure,
                minimumValue = minimum,
                maximumValue = null
            )
        }

        parseRangeCountRule(normalized)?.let { (minimum, maximum) ->
            return FrequencyTargetRule(
                period = period,
                measure = measure,
                minimumValue = minimum,
                maximumValue = maximum
            )
        }

        parseExactCountRule(normalized)?.let { exactValue ->
            return FrequencyTargetRule(
                period = period,
                measure = measure,
                minimumValue = exactValue,
                maximumValue = exactValue
            )
        }

        return null
    }

    fun resolveTargetRule(
        target: WeeklyFrequencyTarget
    ): FrequencyTargetRule? {
        val source = target.sourceText?.takeIf { it.isNotBlank() }
            ?: buildFallbackRuleText(target)
        return parseFrequencyTargetRule(source)
    }

    fun matchesMealText(
        mealText: String,
        target: WeeklyFrequencyTarget
    ): Boolean {
        val normalizedMeal = normalizeKey(mealText)
        if (normalizedMeal.isBlank()) return false

        return when (target.canonicalKey) {
            "piatto unico" -> {
                containsAnyTerm(normalizedMeal, legumeTerms) &&
                        containsAnyTerm(normalizedMeal, carbohydrateTerms)
            }

            else -> containsAnyTerm(normalizedMeal, target.matchTerms.ifEmpty {
                resolveMatchTerms(target.title, target.sourceText)
            })
        }
    }

    private fun containsAnyTerm(
        normalizedMeal: String,
        terms: List<String>
    ): Boolean {
        return terms.any { term ->
            val normalizedTerm = normalizeKey(term)
            normalizedTerm.isNotBlank() && (
                normalizedMeal == normalizedTerm ||
                        normalizedMeal.startsWith("$normalizedTerm ") ||
                        normalizedMeal.contains(" $normalizedTerm ") ||
                        normalizedMeal.endsWith(" $normalizedTerm")
                )
        }
    }

    private fun normalizeForRuleParsing(
        text: String
    ): String {
        return Normalizer.normalize(text.lowercase(), Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .replace('\u00A0', ' ')
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun detectFrequencyPeriod(
        normalized: String
    ): FrequencyTargetPeriod? {
        return when {
            normalized.contains("settimana") -> FrequencyTargetPeriod.WEEK
            dailyPeriodRegex.containsMatchIn(normalized) -> FrequencyTargetPeriod.DAY
            else -> null
        }
    }

    private fun detectFrequencyMeasure(
        normalized: String
    ): FrequencyTargetMeasure {
        return when {
            volumeRuleRegex.containsMatchIn(normalized) && normalized.contains("acqua") -> {
                FrequencyTargetMeasure.MILLILITERS
            }

            normalized.contains("porzione") || normalized.contains("porzioni") -> {
                FrequencyTargetMeasure.PORTIONS
            }

            else -> {
                FrequencyTargetMeasure.OCCURRENCES
            }
        }
    }

    private fun containsCountLikeRule(
        normalized: String
    ): Boolean {
        return normalized.contains("volta") ||
                normalized.contains("volte") ||
                normalized.contains("porzione") ||
                normalized.contains("porzioni") ||
                maxRuleRegex.containsMatchIn(normalized) ||
                minimumRuleRegex.containsMatchIn(normalized)
    }

    private fun parseMaximumCountRule(
        normalized: String
    ): Int? {
        return maxRuleRegex.find(normalized)
            ?.groupValues
            ?.getOrNull(1)
            ?.toIntOrNull()
    }

    private fun parseMinimumCountRule(
        normalized: String
    ): Int? {
        return minimumRuleRegex.find(normalized)
            ?.groupValues
            ?.getOrNull(1)
            ?.toIntOrNull()
    }

    private fun parseRangeCountRule(
        normalized: String
    ): Pair<Int, Int>? {
        val match = rangeRuleRegex.find(normalized) ?: return null
        val first = match.groupValues[1].toIntOrNull() ?: return null
        val second = match.groupValues[2].toIntOrNull() ?: return null
        return minOf(first, second) to maxOf(first, second)
    }

    private fun parseExactCountRule(
        normalized: String
    ): Int? {
        return exactRuleRegex.find(normalized)
            ?.groupValues
            ?.getOrNull(1)
            ?.toIntOrNull()
    }

    private fun parseVolumeRule(
        normalized: String,
        period: FrequencyTargetPeriod
    ): FrequencyTargetRule? {
        val match = volumeRuleRegex.find(normalized) ?: return null
        val rawValue = match.groupValues[1].replace(',', '.')
        val unit = match.groupValues[2]
        val parsedValue = rawValue.toDoubleOrNull() ?: return null
        val valueInMl = when {
            unit.startsWith("ml") -> parsedValue.toInt()
            else -> (parsedValue * 1000).toInt()
        }.coerceAtLeast(0)

        if (valueInMl == 0) return null

        return when {
            maxRuleRegex.containsMatchIn(normalized) -> FrequencyTargetRule(
                period = period,
                measure = FrequencyTargetMeasure.MILLILITERS,
                minimumValue = null,
                maximumValue = valueInMl
            )

            minimumRuleRegex.containsMatchIn(normalized) -> FrequencyTargetRule(
                period = period,
                measure = FrequencyTargetMeasure.MILLILITERS,
                minimumValue = valueInMl,
                maximumValue = null
            )

            else -> FrequencyTargetRule(
                period = period,
                measure = FrequencyTargetMeasure.MILLILITERS,
                minimumValue = valueInMl,
                maximumValue = valueInMl
            )
        }
    }

    private fun buildFallbackRuleText(
        target: WeeklyFrequencyTarget
    ): String {
        return when {
            target.minimumTimesPerWeek != null && target.maximumTimesPerWeek != null &&
                    target.minimumTimesPerWeek == target.maximumTimesPerWeek -> {
                "${target.title} ${target.minimumTimesPerWeek} volte a settimana"
            }

            target.minimumTimesPerWeek != null && target.maximumTimesPerWeek != null -> {
                "${target.title} ${target.minimumTimesPerWeek}-${target.maximumTimesPerWeek} volte a settimana"
            }

            target.minimumTimesPerWeek != null -> {
                "${target.title} almeno ${target.minimumTimesPerWeek} volte a settimana"
            }

            target.maximumTimesPerWeek != null -> {
                "${target.title} massimo ${target.maximumTimesPerWeek} volte a settimana"
            }

            else -> {
                target.title
            }
        }
    }

    private val maxRuleRegex = Regex(
        pattern = """(?:max|massimo|al massimo|non piu di|non piu)\s*n?\.?\s*(\d+)""",
        options = setOf(RegexOption.IGNORE_CASE)
    )

    private val minimumRuleRegex = Regex(
        pattern = """(?:almeno|minimo)\s*n?\.?\s*(\d+)""",
        options = setOf(RegexOption.IGNORE_CASE)
    )

    private val rangeRuleRegex = Regex(
        pattern = """n?\.?\s*(\d+)\s*[-/]\s*n?\.?\s*(\d+)\s+(?:volta|volte|porzione|porzioni)\s+(?:a\s+settimana|al\s+giorno|\b(?:giorno|die)\b)""",
        options = setOf(RegexOption.IGNORE_CASE)
    )

    private val exactRuleRegex = Regex(
        pattern = """(?:^|\s)n?\.?\s*(\d+)\s+(?:volta|volte|porzione|porzioni)\s+(?:a\s+settimana|al\s+giorno|\b(?:giorno|die)\b)""",
        options = setOf(RegexOption.IGNORE_CASE)
    )

    private val volumeRuleRegex = Regex(
        pattern = """(?:almeno|minimo|max|massimo|al massimo|non piu di|non piu)?\s*n?\.?\s*(\d+(?:[.,]\d+)?)\s*(ml|l|lt|litro|litri)\b""",
        options = setOf(RegexOption.IGNORE_CASE)
    )

    private val dailyPeriodRegex = Regex(
        pattern = """\b(?:al giorno|giorno|giornalmente|die)\b""",
        options = setOf(RegexOption.IGNORE_CASE)
    )
}
