package it.lagioiaproductions.nutrislot.ui.weeklyplan

internal data class MealVisualInfo(
    val emoji: String,
    val semanticKey: String
)

internal data class ParsedMealSectionUi(
    val lines: List<String>,
    val visualInfo: MealVisualInfo
)

internal fun parseMealSections(text: String): List<List<String>> {
    return parseMealSectionVisuals(text).map { it.lines }
}

internal fun parseMealSectionVisuals(text: String): List<ParsedMealSectionUi> {
    val cleanedSource = text.stripMealNutritionBlock()

    val rawLines = protectConnectedMealPhrases(cleanedSource)
        .replace("\r\n", "\n")
        .replace("\r", "\n")
        .replace("•", "\n• ")
        .lines()
        .map { it.trim() }
        .filter { it.isNotBlank() }

    if (rawLines.isEmpty()) {
        return emptyList()
    }

    val sections = mutableListOf<List<String>>()
    var currentSection = mutableListOf<String>()

    fun flushSection() {
        val restoredLines = currentSection
            .map(::restoreConnectedMealPhrases)
            .map(String::normalizeMealUiLine)
            .filter { it.isNotBlank() }

        if (restoredLines.isNotEmpty()) {
            sections += restoredLines
        }
        currentSection = mutableListOf()
    }

    rawLines.forEach { rawLine ->
        val normalizedLine = rawLine
            .trim()
            .removePrefix("•")
            .removePrefix("-")
            .removePrefix("–")
            .removePrefix("—")
            .trim()
            .normalizeMealUiLine()

        if (normalizedLine.isBlank()) return@forEach
        if (normalizedLine == "+") {
            flushSection()
            return@forEach
        }
        if (isStandaloneMealHeading(normalizedLine)) {
            return@forEach
        }
        if (isNutritionLine(normalizedLine)) {
            flushSection()
            return@forEach
        }

        val previous = currentSection.lastOrNull()
        if (previous != null && shouldAppendMealContinuation(previous, normalizedLine)) {
            currentSection[currentSection.lastIndex] = "$previous $normalizedLine"
                .normalizeMealUiLine()
        } else {
            currentSection += normalizedLine
        }
    }

    flushSection()

    return sections.map { lines ->
        ParsedMealSectionUi(
            lines = lines,
            visualInfo = inferMealVisualInfo(lines.joinToString(separator = " "))
        )
    }
}

private fun String.normalizeMealUiLine(): String {
    return replace(Regex("\\s+"), " ").trim()
}

private fun protectConnectedMealPhrases(text: String): String {
    var result = text
    PROTECTED_MEAL_PHRASES.forEachIndexed { index, phrase ->
        val placeholder = "__MEAL_KEEP_${index}__"
        result = Regex(Regex.escape(phrase), RegexOption.IGNORE_CASE)
            .replace(result) { placeholder }
    }
    return result
}

private fun restoreConnectedMealPhrases(text: String): String {
    var result = text
    PROTECTED_MEAL_PHRASES.forEachIndexed { index, phrase ->
        result = result.replace("__MEAL_KEEP_${index}__", phrase)
    }
    return result
}

private fun shouldAppendMealContinuation(
    previous: String,
    current: String
): Boolean {
    val normalizedCurrent = current.normalizeMealUiLine().lowercase()
    if (
        normalizedCurrent.startsWith("oppure") ||
        normalizedCurrent.startsWith("in alternativa") ||
        normalizedCurrent.startsWith("alternativa") ||
        normalizedCurrent.startsWith("nb") ||
        normalizedCurrent.startsWith("max ") ||
        normalizedCurrent.startsWith("nutrienti") ||
        normalizedCurrent.startsWith("tot ")
    ) {
        return false
    }

    val firstChar = current.firstOrNull() ?: return false

    val currentLooksLikeContinuation =
        firstChar.isLowerCase() ||
                firstChar.isDigit() ||
                firstChar == '(' ||
                firstChar == '%' ||
                current.length <= 18

    val normalizedPrevious = previous.normalizeMealUiLine().lowercase()
    val previousLooksOpen =
        previous.endsWith(",") ||
                previous.endsWith(":") ||
                previous.endsWith("/") ||
                previous.endsWith("-") ||
                normalizedPrevious.endsWith(" o") ||
                normalizedPrevious.endsWith(" ed") ||
                normalizedPrevious.endsWith(" oppure")

    return currentLooksLikeContinuation || previousLooksOpen
}

private fun inferMealVisualInfo(text: String): MealVisualInfo {
    val normalized = text
        .lowercase()
        .replace("’", "'")
        .normalizeMealUiLine()

    return when {
        "panino" in normalized || "panini" in normalized ->
            MealVisualInfo(emoji = "🥪", semanticKey = "panino")

        "piadina" in normalized ->
            MealVisualInfo(emoji = "🌯", semanticKey = "piadina")

        "frisella" in normalized || "friselle" in normalized ->
            MealVisualInfo(emoji = "🥯", semanticKey = "frisella")

        "insalatona" in normalized || "insalata" in normalized ->
            MealVisualInfo(emoji = "🥗", semanticKey = "insalata")

        "pasta fredda" in normalized ||
                "pasta" in normalized ||
                "riso" in normalized ||
                "orzo" in normalized ||
                "farro" in normalized ||
                "couscous" in normalized ||
                "minestrone" in normalized ->
            MealVisualInfo(emoji = "🍝", semanticKey = "cereale_primo")

        "pancake" in normalized ->
            MealVisualInfo(emoji = "🥞", semanticKey = "pancake")

        "yogurt" in normalized || "latte" in normalized || "kefir" in normalized ->
            MealVisualInfo(emoji = "🥛", semanticKey = "latticino")

        "pollo" in normalized ||
                "tacchino" in normalized ||
                "hamburger" in normalized ||
                "carne" in normalized ||
                "bresaola" in normalized ||
                "prosciutto" in normalized ||
                "affettato" in normalized ->
            MealVisualInfo(emoji = "🍗", semanticKey = "carne")

        "pesce" in normalized ||
                "salmone" in normalized ||
                "tonno" in normalized ||
                "sgombro" in normalized ->
            MealVisualInfo(emoji = "🐟", semanticKey = "pesce")

        "uova" in normalized || "uovo" in normalized || "frittata" in normalized ->
            MealVisualInfo(emoji = "🥚", semanticKey = "uova")

        "pane" in normalized ->
            MealVisualInfo(emoji = "🍞", semanticKey = "pane")

        "cereali" in normalized ||
                "cornflakes" in normalized ||
                "muesli" in normalized ||
                "granola" in normalized ||
                "porridge" in normalized ||
                "fette biscottate" in normalized ||
                "biscotti" in normalized ->
            MealVisualInfo(emoji = "🥣", semanticKey = "colazione_secca")

        "banana" in normalized ->
            MealVisualInfo(emoji = "🍌", semanticKey = "banana")

        "mela" in normalized ->
            MealVisualInfo(emoji = "🍎", semanticKey = "mela")

        "pera" in normalized ->
            MealVisualInfo(emoji = "🍐", semanticKey = "pera")

        "frutta" in normalized ||
                "frutto" in normalized ||
                "fragole" in normalized ||
                "kiwi" in normalized ||
                "arancia" in normalized ->
            MealVisualInfo(emoji = "🍓", semanticKey = "frutta")

        "mandorle" in normalized ||
                "nocciole" in normalized ||
                "noci" in normalized ||
                "arachidi" in normalized ||
                "frutta secca" in normalized ->
            MealVisualInfo(emoji = "🥜", semanticKey = "frutta_secca")

        "avocado" in normalized ->
            MealVisualInfo(emoji = "🥑", semanticKey = "avocado")

        "parmigiano" in normalized ||
                "primo sale" in normalized ||
                "ricotta" in normalized ||
                "mozzarella" in normalized ||
                "philadelphia" in normalized ||
                "formaggio" in normalized ||
                "feta" in normalized ->
            MealVisualInfo(emoji = "🧀", semanticKey = "formaggio")

        "pomodoro" in normalized || "pomodorini" in normalized ->
            MealVisualInfo(emoji = "🍅", semanticKey = "pomodoro")

        "carota" in normalized || "carote" in normalized ->
            MealVisualInfo(emoji = "🥕", semanticKey = "carota")

        "verdura" in normalized ||
                "lattuga" in normalized ||
                "lattughino" in normalized ||
                "songino" in normalized ||
                "rughetta" in normalized ||
                "radicchio" in normalized ||
                "valeriana" in normalized ||
                "zucchine" in normalized ->
            MealVisualInfo(emoji = "🥬", semanticKey = "verdura")

        "olio" in normalized || "olive" in normalized ->
            MealVisualInfo(emoji = "🫒", semanticKey = "olio")

        "miele" in normalized || "marmellata" in normalized ->
            MealVisualInfo(emoji = "🍯", semanticKey = "dolce_spalmabile")

        "caffè" in normalized || "caffe" in normalized ->
            MealVisualInfo(emoji = "☕", semanticKey = "caffe")

        else ->
            MealVisualInfo(emoji = "🍽️", semanticKey = "meal_generic")
    }
}

private fun String.stripMealNutritionBlock(): String {
    val normalized = this
        .replace("\r\n", "\n")
        .replace("\r", "\n")

    val lines = normalized.lines()
    if (lines.size > 1) {
        val keptLines = mutableListOf<String>()

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isBlank()) {
                keptLines += line
                continue
            }

            if (isNutritionLine(trimmed)) {
                break
            }

            keptLines += line
        }

        val joined = keptLines.joinToString(separator = "\n").trim()
        if (joined.isNotBlank()) {
            return joined
        }
    }

    return normalized
        .replace(Regex("(?is)\\bNutrienti\\s*:.*$"), "")
        .replace(
            Regex(
                "(?is)\\bTot\\.?\\s*(?:kcal|g\\s+proteine|g\\s+carboidrati|g\\s+fibre|g\\s+grassi|g\\s+lipidi)\\b.*$"
            ),
            ""
        )
        .trim()
}

private fun isNutritionLine(line: String): Boolean {
    val normalized = line
        .lowercase()
        .replace("’", "'")
        .replace(Regex("\\s+"), " ")
        .trim()

    return normalized.startsWith("nutrienti") ||
            normalized.startsWith("tot kcal") ||
            normalized.startsWith("tot. kcal") ||
            normalized.startsWith("tot g proteine") ||
            normalized.startsWith("tot. g proteine") ||
            normalized.startsWith("tot g carboidrati") ||
            normalized.startsWith("tot. g carboidrati") ||
            normalized.startsWith("tot g fibre") ||
            normalized.startsWith("tot. g fibre") ||
            normalized.startsWith("tot g grassi") ||
            normalized.startsWith("tot. g grassi") ||
            normalized.startsWith("tot g lipidi") ||
            normalized.startsWith("tot. g lipidi")
}

private fun isStandaloneMealHeading(line: String): Boolean {
    return when (line.lowercase()) {
        "colazione",
        "spuntino mattina",
        "spuntino di meta mattina",
        "spuntino meta mattina",
        "meta mattina",
        "pranzo",
        "spuntino pomeridiano",
        "spuntino pomeriggio",
        "spuntino del pomeriggio",
        "pomeriggio",
        "cena" -> true
        else -> false
    }
}

private val PROTECTED_MEAL_PHRASES = listOf(
    "scuro o integrale",
    "integrale o scuro",
    "pane scuro o integrale",
    "pane integrale o scuro",
    "cotta e/o cruda",
    "cotte e/o crude",
    "cotto e/o crudo",
    "cotti e/o crudi",
    "cruda e/o cotta",
    "crude e/o cotte",
    "caffè latte"
)