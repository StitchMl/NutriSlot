package it.lagioiaproductions.nutrislot.data.importer

import it.lagioiaproductions.nutrislot.domain.model.ImportedWeeklyFrequencyTarget
import it.lagioiaproductions.nutrislot.domain.model.WeeklyFrequencyTargetSupport

internal class PdfWeeklyFrequencyTargetExtractor {

    fun extractWeeklyTargets(
        pageScans: List<PageScan>
    ): List<ImportedWeeklyFrequencyTarget> {
        return pageScans.asSequence()
            .filter { scan ->
                scan.isAppendixPage || frequencyKeywordRegex.containsMatchIn(scan.normalizedFullText)
            }
            .flatMap { scan -> extractTargetsFromPage(scan).asSequence() }
            .groupBy { candidate -> candidate.target.canonicalKey }
            .values
            .mapNotNull { grouped ->
                grouped.maxByOrNull { candidate -> candidate.score }?.target
            }
            .sortedBy { target -> target.title }
            .toList()
    }

    private fun extractTargetsFromPage(
        pageScan: PageScan
    ): List<TargetCandidate> {
        return extractCandidateSegments(pageScan.fullText)
            .mapNotNull { segment ->
                buildCandidateFromSegment(
                    segment = segment,
                    pageNumber = pageScan.pageNumber
                )
            }
    }

    private fun extractCandidateSegments(
        fullText: String
    ): List<String> {
        val lines = fullText
            .replace("\r\n", "\n")
            .replace("\r", "\n")
            .lines()
            .map { line ->
                PdfImportTextNormalization.repairExtractedText(line)
                    .replace(Regex("\\s+"), " ")
                    .trim()
            }
            .filter { it.isNotBlank() }

        if (lines.isEmpty()) return emptyList()

        val blocks = mutableListOf<String>()
        val current = mutableListOf<String>()

        fun flushCurrent() {
            if (current.isEmpty()) return

            val block = current.joinToString(separator = " ")
                .replace(Regex("\\s+"), " ")
                .trim()

            if (frequencyKeywordRegex.containsMatchIn(block)) {
                blocks += block
            }

            current.clear()
        }

        lines.forEach { line ->
            val strippedLine = line.removeBulletPrefix().trim()
            if (strippedLine.isBlank()) {
                flushCurrent()
                return@forEach
            }

            val startsBullet = bulletPrefixRegex.containsMatchIn(line)
            val containsFrequencyRule = frequencyKeywordRegex.containsMatchIn(strippedLine)

            when {
                startsBullet -> {
                    flushCurrent()
                    current += strippedLine
                }

                current.isNotEmpty() -> {
                    if (containsFrequencyRule && looksLikeStandaloneFrequencyTarget(strippedLine)) {
                        flushCurrent()
                        current += strippedLine
                    } else {
                        current += strippedLine
                    }
                }

                containsFrequencyRule -> {
                    current += strippedLine
                }
            }
        }

        flushCurrent()

        return blocks
            .flatMap { block ->
                block.split(segmentSeparatorRegex)
                    .map { segment -> segment.trim().trim(';', '.', '-') }
            }
            .filter { segment ->
                segment.isNotBlank() && frequencyKeywordRegex.containsMatchIn(segment)
            }
    }

    private fun buildCandidateFromSegment(
        segment: String,
        pageNumber: Int
    ): TargetCandidate? {
        val rule = WeeklyFrequencyTargetSupport.parseFrequencyTargetRule(segment) ?: return null
        val rawTitle = extractTargetTitle(segment).takeIf { it.isNotBlank() } ?: return null
        val canonicalKey = WeeklyFrequencyTargetSupport.resolveKnownCanonicalKey(rawTitle) ?: return null
        if (!WeeklyFrequencyTargetSupport.isReasonableKnownTargetTitle(rawTitle, canonicalKey)) {
            return null
        }

        val title = WeeklyFrequencyTargetSupport.formatTitle(canonicalKey)

        val sourceSummary = segment
            .replace(Regex("\\s+"), " ")
            .trim()

        val matchTerms = WeeklyFrequencyTargetSupport.resolveMatchTerms(
            title = title,
            sourceText = sourceSummary
        )

        return TargetCandidate(
            target = ImportedWeeklyFrequencyTarget(
                id = "weekly_target_${canonicalKey.ifBlank { title.lowercase() }}",
                title = title,
                canonicalKey = canonicalKey,
                portionText = extractPortionText(segment, rule),
                minimumTimesPerWeek = rule.minimumValue,
                maximumTimesPerWeek = rule.maximumValue,
                matchTerms = matchTerms,
                pageNumber = pageNumber,
                sourceText = sourceSummary
            ),
            score = computeCandidateScore(
                rule = rule,
                segment = segment,
                rawTitle = rawTitle,
                canonicalKey = canonicalKey
            )
        )
    }

    private fun extractTargetTitle(
        segment: String
    ): String {
        val normalizedSegment = WeeklyFrequencyTargetSupport.normalizeKey(segment)

        val forcedTitle = when {
            normalizedSegment.contains("frutta") && normalizedSegment.contains("verdura") -> {
                "Frutta e verdura"
            }

            normalizedSegment.contains("acqua") -> {
                "Acqua"
            }

            normalizedSegment.contains("caffe") || normalizedSegment.contains("the") || normalizedSegment.contains("tea") -> {
                "Caffe e The"
            }

            else -> {
                null
            }
        }
        if (forcedTitle != null) return forcedTitle

        leadingRulePattern.matchEntire(segment)?.let { match ->
            val rawSubject = match.groupValues[2]
            return cleanupTargetTitle(rawSubject)
        }

        subjectBeforeRulePattern.matchEntire(segment)?.let { match ->
            val rawSubject = match.groupValues[1]
            return cleanupTargetTitle(rawSubject)
        }

        return cleanupTargetTitle(
            segment.substringBefore(" per ")
                .substringBefore(",")
        )
    }

    private fun extractPortionText(
        segment: String,
        rule: WeeklyFrequencyTargetSupport.FrequencyTargetRule
    ): String? {
        parentheticalDetailRegex.find(segment)
            ?.groupValues
            ?.getOrNull(1)
            ?.replace(Regex("\\s+"), " ")
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?.let { return it }

        return when (rule.measure) {
            WeeklyFrequencyTargetSupport.FrequencyTargetMeasure.MILLILITERS -> {
                volumeRuleRegex.find(segment)
                    ?.groupValues
                    ?.drop(1)
                    ?.take(2)
                    ?.let { groups ->
                        val value = groups[0]
                        val unit = groups[1]
                        "$value ${unit.lowercase()}"
                    }
            }

            else -> null
        }
    }

    private fun cleanupTargetTitle(
        rawSubject: String
    ): String {
        val cleaned = rawSubject
            .substringAfterLast(":")
            .replace(Regex("^(consumare|consuma|bere|bevi)\\s+", RegexOption.IGNORE_CASE), "")
            .replace(
                Regex(
                    "^(?:almeno|minimo|max|massimo|al massimo|non piu di|non piu)\\s+n?\\.?\\s*\\d+(?:[.,]\\d+)?\\s*(?:ml|l|lt|litro|litri)?\\s*",
                    RegexOption.IGNORE_CASE
                ),
                ""
            )
            .replace(Regex("^n?\\.?\\s*\\d+\\s+(?:porzioni?|volte?)\\s+di\\s+", RegexOption.IGNORE_CASE), "")
            .replace(Regex("^(?:il|lo|la|i|gli|le|di|del|della|dei|degli|delle)\\s+", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\b(?:al giorno|a settimana|giorno|die)\\b.*$", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\b(?:per|in grado di|per favorire)\\b.*$", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\s+"), " ")
            .trim()
            .trim(',', ';', '.', ':', '-', ' ')

        return WeeklyFrequencyTargetSupport.formatTitle(cleaned)
    }

    private fun computeCandidateScore(
        rule: WeeklyFrequencyTargetSupport.FrequencyTargetRule,
        segment: String,
        rawTitle: String,
        canonicalKey: String
    ): Int {
        val normalizedTitle = WeeklyFrequencyTargetSupport.normalizeKey(rawTitle)
        val titleWordCount = normalizedTitle.split(" ").count { it.isNotBlank() }

        return (if (rule.period == WeeklyFrequencyTargetSupport.FrequencyTargetPeriod.DAY) 3 else 2) +
                (if (rule.measure == WeeklyFrequencyTargetSupport.FrequencyTargetMeasure.MILLILITERS) 2 else 0) +
                (if (parentheticalDetailRegex.containsMatchIn(segment)) 1 else 0) +
                (if (segment.contains("almeno", ignoreCase = true) || segment.contains("massimo", ignoreCase = true)) 1 else 0) +
                (if (normalizedTitle == canonicalKey) 4 else 0) +
                (if (titleWordCount <= 2) 1 else 0) -
                (if (segment.contains('+')) 4 else 0) -
                (if (segment.length > 180) 2 else 0)
    }

    private fun looksLikeStandaloneFrequencyTarget(
        line: String
    ): Boolean {
        return line.firstOrNull()?.isLetter() == true &&
                frequencyKeywordRegex.containsMatchIn(line)
    }

    private fun String.removeBulletPrefix(): String {
        return replace(bulletPrefixRegex, "").trim()
    }

    private data class TargetCandidate(
        val target: ImportedWeeklyFrequencyTarget,
        val score: Int
    )

    private companion object {
        private val bulletPrefixRegex = Regex("""^[\u2022•\-\u2013\u2014]+\s*""")
        private val frequencyKeywordRegex = Regex(
            pattern = """\b(?:settimana|al giorno|giorno|die)\b""",
            options = setOf(RegexOption.IGNORE_CASE)
        )
        private val segmentSeparatorRegex = Regex("""\s+[•\u2022]\s+|\s+-\s+(?=[\p{L}])""")
        private const val RULE_TOKEN =
            """(?:max|massimo|al massimo|non piu di|non piu|almeno|minimo)\s*n?\.?\s*\d+(?:[.,]\d+)?(?:\s*(?:ml|l|lt|litro|litri))?|n?\.?\s*\d+\s*[-/]\s*n?\.?\s*\d+|n?\.?\s*\d+(?:[.,]\d+)?(?:\s*(?:ml|l|lt|litro|litri))?"""

        private val leadingRulePattern = Regex(
            pattern = """^(?:consumare|consuma|bere|bevi)\s+($RULE_TOKEN)\s+(.+?)\s+(?:a\s+settimana|al\s+giorno|\b(?:giorno|die)\b).*$""",
            options = setOf(RegexOption.IGNORE_CASE)
        )

        private val subjectBeforeRulePattern = Regex(
            pattern = """^(.+?)\s+($RULE_TOKEN)\s*(?:(?:volta|volte|porzione|porzioni)\s+)?(?:a\s+settimana|al\s+giorno|\b(?:giorno|die)\b).*$""",
            options = setOf(RegexOption.IGNORE_CASE)
        )

        private val parentheticalDetailRegex = Regex(
            pattern = """\(([^)\n]{1,120})\)""",
            options = setOf(RegexOption.IGNORE_CASE)
        )

        private val volumeRuleRegex = Regex(
            pattern = """(\d+(?:[.,]\d+)?)\s*(ml|l|lt|litro|litri)\b""",
            options = setOf(RegexOption.IGNORE_CASE)
        )
    }
}
