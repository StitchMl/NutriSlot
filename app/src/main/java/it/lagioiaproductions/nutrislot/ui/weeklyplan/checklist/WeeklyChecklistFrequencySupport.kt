package it.lagioiaproductions.nutrislot.ui.weeklyplan

import it.lagioiaproductions.nutrislot.domain.model.WeeklyFrequencyTarget
import it.lagioiaproductions.nutrislot.domain.model.WeeklyFrequencyTargetSupport

internal data class WeeklyChecklistTargetSpec(
    val canonicalKey: String,
    val title: String,
    val portionText: String? = null,
    val minimumTargetValue: Int? = null,
    val maximumTargetValue: Int? = null,
    val period: WeeklyQuantityChecklistPeriodUi = WeeklyQuantityChecklistPeriodUi.WEEKLY,
    val metric: WeeklyQuantityChecklistMetricUi = WeeklyQuantityChecklistMetricUi.OCCURRENCES,
    val matchTerms: List<String> = emptyList(),
    val sourceLabel: String,
    val sourceText: String? = null
) {
    fun mergeWith(
        other: WeeklyChecklistTargetSpec
    ): WeeklyChecklistTargetSpec {
        return copy(
            portionText = portionText ?: other.portionText,
            minimumTargetValue = minimumTargetValue ?: other.minimumTargetValue,
            maximumTargetValue = maximumTargetValue ?: other.maximumTargetValue,
            period = period.takeUnless { it == WeeklyQuantityChecklistPeriodUi.WEEKLY }
                ?: other.period,
            metric = metric.takeUnless { it == WeeklyQuantityChecklistMetricUi.OCCURRENCES }
                ?: other.metric,
            matchTerms = (matchTerms + other.matchTerms).distinct(),
            sourceLabel = when {
                sourceLabel == other.sourceLabel -> sourceLabel
                sourceLabel.contains(other.sourceLabel, ignoreCase = true) -> sourceLabel
                other.sourceLabel.contains(sourceLabel, ignoreCase = true) -> other.sourceLabel
                else -> "$sourceLabel + ${other.sourceLabel}"
            },
            sourceText = sourceText ?: other.sourceText
        )
    }

    fun toDomainTarget(): WeeklyFrequencyTarget {
        return WeeklyFrequencyTarget(
            id = "weekly_checklist_$canonicalKey",
            planId = "",
            title = title,
            canonicalKey = canonicalKey,
            portionText = portionText,
            minimumTimesPerWeek = minimumTargetValue,
            maximumTimesPerWeek = maximumTargetValue,
            matchTerms = matchTerms,
            sourceText = sourceText
        )
    }
}

private val ignoredChecklistKeys = setOf(
    "olio",
    "olio evo"
)

internal fun buildTrackableChecklistTargetSpecs(
    importedTargets: List<WeeklyFrequencyTarget>,
    slots: List<WeeklySlotUi>
): List<WeeklyChecklistTargetSpec> {
    return mergeWeeklyChecklistTargets(
        importedTargets = importedTargets,
        inlineTargets = extractInlineWeeklyChecklistTargets(slots)
    ).filter(::isTrackableChecklistTarget)
}

internal fun isTrackableChecklistTarget(
    target: WeeklyChecklistTargetSpec
): Boolean {
    val hasWeeklyRule = target.minimumTargetValue != null || target.maximumTargetValue != null
    if (!hasWeeklyRule) return false
    if (target.canonicalKey.isBlank()) return false
    return target.canonicalKey !in ignoredChecklistKeys
}

internal fun WeeklyChecklistTargetSpec.isWaterTarget(): Boolean {
    return canonicalKey == "acqua" ||
        matchTerms.any { term ->
            WeeklyFrequencyTargetSupport.normalizeKey(term) == "acqua"
        }
}

internal fun mergeWeeklyChecklistTargets(
    importedTargets: List<WeeklyFrequencyTarget>,
    inlineTargets: List<WeeklyChecklistTargetSpec>
): List<WeeklyChecklistTargetSpec> {
    val mergedTargets = linkedMapOf<String, WeeklyChecklistTargetSpec>()

    importedTargets
        .map(::toChecklistTargetSpec)
        .forEach { target ->
            mergedTargets[target.canonicalKey] = target
        }

    inlineTargets.forEach { target ->
        val existing = mergedTargets[target.canonicalKey]
        mergedTargets[target.canonicalKey] = existing?.mergeWith(target) ?: target
    }

    return mergedTargets.values.toList()
}

internal fun extractInlineWeeklyChecklistTargets(
    slots: List<WeeklySlotUi>
): List<WeeklyChecklistTargetSpec> {
    return slots.asSequence()
        .flatMap { slot ->
            parseMealStructuredSections(slot.displayedMealText).asSequence()
        }
        .flatMap { section -> section.components.asSequence() }
        .flatMap { component ->
            component.weeklyQuantityNotes.asSequence().mapNotNull { note ->
                buildInlineTargetSpec(
                    component = component,
                    note = note
                )
            }
        }
        .groupBy { target -> target.canonicalKey }
        .values
        .map { groupedTargets ->
            groupedTargets.reduce { acc, next -> acc.mergeWith(next) }
        }
        .sortedBy { target -> target.title }
}

private fun toChecklistTargetSpec(
    target: WeeklyFrequencyTarget
): WeeklyChecklistTargetSpec {
    val parsedRule = WeeklyFrequencyTargetSupport.resolveTargetRule(target)
    return WeeklyChecklistTargetSpec(
        canonicalKey = target.canonicalKey,
        title = target.title,
        portionText = target.portionText,
        minimumTargetValue = parsedRule?.minimumValue ?: target.minimumTimesPerWeek,
        maximumTargetValue = parsedRule?.maximumValue ?: target.maximumTimesPerWeek,
        period = when (parsedRule?.period) {
            WeeklyFrequencyTargetSupport.FrequencyTargetPeriod.DAY -> WeeklyQuantityChecklistPeriodUi.DAILY
            else -> WeeklyQuantityChecklistPeriodUi.WEEKLY
        },
        metric = when (parsedRule?.measure) {
            WeeklyFrequencyTargetSupport.FrequencyTargetMeasure.MILLILITERS -> WeeklyQuantityChecklistMetricUi.MILLILITERS
            WeeklyFrequencyTargetSupport.FrequencyTargetMeasure.PORTIONS -> WeeklyQuantityChecklistMetricUi.PORTIONS
            else -> WeeklyQuantityChecklistMetricUi.OCCURRENCES
        },
        matchTerms = target.matchTerms,
        sourceLabel = "Guida PDF",
        sourceText = target.sourceText
    )
}

private fun buildInlineTargetSpec(
    component: ParsedMealComponent,
    note: String
): WeeklyChecklistTargetSpec? {
    val rule = WeeklyFrequencyTargetSupport.parseFrequencyTargetRule(note) ?: return null

    val rawTitle = extractTitleFromWeeklyNote(note)
        ?: component.alternatives
            .asSequence()
            .map(::cleanupWeeklyTargetTitle)
            .firstOrNull { it.isNotBlank() }
        ?: return null

    val canonicalKey = WeeklyFrequencyTargetSupport.resolveKnownCanonicalKey(rawTitle) ?: return null
    if (!WeeklyFrequencyTargetSupport.isReasonableKnownTargetTitle(rawTitle, canonicalKey)) {
        return null
    }

    val resolvedTitle = WeeklyFrequencyTargetSupport.formatTitle(canonicalKey)

    val sourceText = buildString {
        append(resolvedTitle)
        append(" - ")
        append(note)
    }

    val matchTerms = WeeklyFrequencyTargetSupport.resolveMatchTerms(
        title = resolvedTitle,
        sourceText = sourceText
    )

    return WeeklyChecklistTargetSpec(
        canonicalKey = canonicalKey,
        title = resolvedTitle,
        portionText = component.mealQuantityNotes.firstOrNull()?.normalizeForChecklistLabel(),
        minimumTargetValue = rule.minimumValue,
        maximumTargetValue = rule.maximumValue,
        period = when (rule.period) {
            WeeklyFrequencyTargetSupport.FrequencyTargetPeriod.DAY -> WeeklyQuantityChecklistPeriodUi.DAILY
            WeeklyFrequencyTargetSupport.FrequencyTargetPeriod.WEEK -> WeeklyQuantityChecklistPeriodUi.WEEKLY
        },
        metric = when (rule.measure) {
            WeeklyFrequencyTargetSupport.FrequencyTargetMeasure.PORTIONS -> WeeklyQuantityChecklistMetricUi.PORTIONS
            WeeklyFrequencyTargetSupport.FrequencyTargetMeasure.MILLILITERS -> WeeklyQuantityChecklistMetricUi.MILLILITERS
            WeeklyFrequencyTargetSupport.FrequencyTargetMeasure.OCCURRENCES -> WeeklyQuantityChecklistMetricUi.OCCURRENCES
        },
        matchTerms = matchTerms,
        sourceLabel = "Nota del piano",
        sourceText = sourceText
    )
}

private fun extractTitleFromWeeklyNote(
    note: String
): String? {
    val normalized = note
        .replace(Regex("\\s+"), " ")
        .trim()

    val normalizedKey = WeeklyFrequencyTargetSupport.normalizeKey(normalized)
    when {
        normalizedKey.contains("frutta") && normalizedKey.contains("verdura") -> {
            return "Frutta e verdura"
        }

        normalizedKey.contains("acqua") -> {
            return "Acqua"
        }

        normalizedKey.contains("caffe") || normalizedKey.contains("the") || normalizedKey.contains("tea") -> {
            return "Caffe e The"
        }
    }

    val subject = weeklyRulePhraseRegex.find(normalized)
        ?.range
        ?.first
        ?.let { normalized.substring(0, it) }
        ?.trim()
        .orEmpty()

    return cleanupWeeklyTargetTitle(subject).takeIf { it.isNotBlank() }
}

private fun cleanupWeeklyTargetTitle(
    raw: String
): String {
    val cleaned = raw
        .substringBefore(" con ")
        .substringBefore(" accompagnato")
        .substringBefore("(")
        .replace(Regex("""\bn\.?\s*\d+\b""", RegexOption.IGNORE_CASE), " ")
        .replace(
            Regex(
                """\b\d+(?:[.,]\d+)?\s*(?:kg|g|gr|grammi?|mg|ml|cl|l|fette?|cucchiai?|cucchiaini?|vasetti?|scatolette?)\b""",
                RegexOption.IGNORE_CASE
            ),
            " "
        )
        .replace(Regex("""^(di|del|della|dei|degli|delle)\s+""", RegexOption.IGNORE_CASE), "")
        .replace(Regex("""\b(?:oppure|in alternativa|alternativa)\b.*$""", RegexOption.IGNORE_CASE), "")
        .replace(Regex("\\s+"), " ")
        .trim()
        .trim(',', ';', '.', ':', '-', ' ')

    return WeeklyFrequencyTargetSupport.formatTitle(cleaned)
}

private fun String.normalizeForChecklistLabel(): String {
    return replace(Regex("\\s+"), " ")
        .trim()
        .trim(',', ';')
}

private val weeklyRulePhraseRegex = Regex(
    pattern = """(?:(?:max|massimo|al massimo|non piu di|non piu|almeno|minimo)\s*n?\.?\s*\d+(?:[.,]\d+)?(?:\s*(?:ml|l|lt|litro|litri))?|n?\.?\s*\d+\s*[-/]\s*n?\.?\s*\d+|n?\.?\s*\d+(?:[.,]\d+)?(?:\s*(?:ml|l|lt|litro|litri))?)\s*(?:(?:volta|volte|porzione|porzioni)\s+)?(?:a\s+settimana|al\s+giorno|\b(?:giorno|die)\b)""",
    options = setOf(RegexOption.IGNORE_CASE)
)
