package it.lagioiaproductions.nutrislot.ui.weeklyplan.parser

private const val BREAD_QUALIFIER_SLASH_PLACEHOLDER = "__BREAD_QUALIFIER_SLASH__"

private val BREAD_WITH_OR_REGEX = Regex(
    pattern = """((?:\d+(?:[.,]\d+)?\s*(?:kg|g|gr|grammi?|mg|ml|cl|l)\s+di\s+)?(?:pane|panino)\s+)(scuro|integrale)\s+o\s+(?:(?:pane|panino)\s+)?(scuro|integrale)(.*)""",
    options = setOf(RegexOption.IGNORE_CASE)
)

private val BREAD_WITH_SLASH_REGEX = Regex(
    pattern = """((?:\d+(?:[.,]\d+)?\s*(?:kg|g|gr|grammi?|mg|ml|cl|l)\s+di\s+)?(?:pane|panino)\s+)(scuro|integrale)\s*/\s*(?:(?:pane|panino)\s+)?(scuro|integrale)(.*)""",
    options = setOf(RegexOption.IGNORE_CASE)
)

private val IMPLICIT_BREAD_SHORTHAND_REGEX = Regex(
    pattern = """((?:\d+(?:[.,]\d+)?\s*(?:kg|g|gr|grammi?|mg|ml|cl|l)\s+di\s+)?(?:pane|panino)\s+)scuro\s+integrale(.*)""",
    options = setOf(RegexOption.IGNORE_CASE)
)

private val CANONICAL_BREAD_QUALIFIER_REGEX = Regex(
    pattern = """((?:\d+(?:[.,]\d+)?\s*(?:kg|g|gr|grammi?|mg|ml|cl|l)\s+di\s+)?(?:pane|panino)\s+)(scuro|integrale)/(scuro|integrale)(.*)""",
    options = setOf(RegexOption.IGNORE_CASE)
)

internal fun String.normalizeBreadQualifierShorthand(): String {
    val slashNormalized = BREAD_WITH_SLASH_REGEX.replace(this) { match ->
        val prefix = match.groupValues[1]
        val first = match.groupValues[2]
        val second = match.groupValues[3]
        val suffix = match.groupValues[4]
        "$prefix$first/$second$suffix"
    }

    val orNormalized = BREAD_WITH_OR_REGEX.replace(slashNormalized) { match ->
        val prefix = match.groupValues[1]
        val first = match.groupValues[2]
        val second = match.groupValues[3]
        val suffix = match.groupValues[4]
        "$prefix$first/$second$suffix"
    }

    return IMPLICIT_BREAD_SHORTHAND_REGEX.replace(orNormalized) { match ->
        val prefix = match.groupValues[1]
        val suffix = match.groupValues[2]
        prefix + "scuro/integrale" + suffix
    }
        .replace(Regex("\\s+"), " ")
        .trim()
}

internal fun protectBreadQualifierSlash(text: String): String {
    return CANONICAL_BREAD_QUALIFIER_REGEX.replace(text) { match ->
        val prefix = match.groupValues[1]
        val first = match.groupValues[2]
        val second = match.groupValues[3]
        val suffix = match.groupValues[4]
        "$prefix$first$BREAD_QUALIFIER_SLASH_PLACEHOLDER$second$suffix"
    }
}

internal fun restoreProtectedBreadQualifierSlash(text: String): String {
    return text.replace(BREAD_QUALIFIER_SLASH_PLACEHOLDER, "/")
}
