package it.lagioiaproductions.nutrislot.ui.weeklyplan

import it.lagioiaproductions.nutrislot.domain.model.WeekDay
import it.lagioiaproductions.nutrislot.domain.model.WeeklyFrequencyTarget
import it.lagioiaproductions.nutrislot.domain.model.WeeklyFrequencyTargetSupport

internal data class WeeklyChecklistHydrationSnapshot(
    val consumedMl: Int,
    val targetMl: Int
)

internal object WeeklyQuantityChecklistBuilder {
    private val ignoredChecklistKeys = setOf(
        "olio",
        "olio evo"
    )

    fun build(
        slots: List<WeeklySlotUi>,
        weeklyTargets: List<WeeklyFrequencyTarget>,
        referenceDay: WeekDay,
        hydrationSnapshot: WeeklyChecklistHydrationSnapshot? = null
    ): List<WeeklyQuantityChecklistItemUi> {
        if (slots.isEmpty()) return emptyList()

        val targetSpecs = mergeWeeklyChecklistTargets(
            importedTargets = weeklyTargets,
            inlineTargets = extractInlineWeeklyChecklistTargets(slots)
        ).filter(::shouldShowChecklistTarget)

        if (targetSpecs.isEmpty()) return emptyList()

        return targetSpecs
            .mapNotNull { spec ->
                buildChecklistItem(
                    spec = spec,
                    slots = slots,
                    referenceDay = referenceDay,
                    hydrationSnapshot = hydrationSnapshot
                )
            }
            .sortedWith(
                compareBy(
                    { it.statusSortOrder },
                    { if (it.period == WeeklyQuantityChecklistPeriodUi.DAILY) 0 else 1 },
                    { it.remainingMinimumValue },
                    { it.title }
                )
            )
    }

    private fun shouldShowChecklistTarget(
        target: WeeklyChecklistTargetSpec
    ): Boolean {
        val hasWeeklyRule = target.minimumTargetValue != null || target.maximumTargetValue != null
        if (!hasWeeklyRule) return false
        if (target.canonicalKey.isBlank()) return false
        return target.canonicalKey !in ignoredChecklistKeys
    }

    private fun buildChecklistItem(
        spec: WeeklyChecklistTargetSpec,
        slots: List<WeeklySlotUi>,
        referenceDay: WeekDay,
        hydrationSnapshot: WeeklyChecklistHydrationSnapshot?
    ): WeeklyQuantityChecklistItemUi? {
        val normalizedSourceLabel = if (spec.isWaterTarget() && hydrationSnapshot != null) {
            listOf(spec.sourceLabel, "Scheda acqua").distinct().joinToString(separator = " + ")
        } else {
            spec.sourceLabel
        }

        if (spec.isWaterTarget()) {
            val resolvedMinimumValue = when {
                spec.minimumTargetValue != null -> spec.minimumTargetValue
                spec.maximumTargetValue == null && hydrationSnapshot != null && hydrationSnapshot.targetMl > 0 -> {
                    hydrationSnapshot.targetMl
                }

                else -> null
            }

            val resolvedMaximumValue = spec.maximumTargetValue

            if (resolvedMinimumValue == null && resolvedMaximumValue == null) return null

            return WeeklyQuantityChecklistItemUi(
                id = spec.canonicalKey,
                title = spec.title,
                portionText = spec.portionText,
                minimumTargetValue = resolvedMinimumValue,
                maximumTargetValue = resolvedMaximumValue,
                consumedValue = hydrationSnapshot?.consumedMl ?: 0,
                sourceLabel = normalizedSourceLabel,
                period = spec.period,
                metric = spec.metric
            )
        }

        val target = spec.toDomainTarget()
        val consumedValue = slots.count { slot ->
            slot.isActuallyCompletedThisWeek &&
                    slot.matchesChecklistPeriod(referenceDay, spec.period) &&
                    WeeklyFrequencyTargetSupport.matchesMealText(
                        mealText = slot.displayedMealText.stripMealNutritionBlock(),
                        target = target
                    )
        }

        return WeeklyQuantityChecklistItemUi(
            id = spec.canonicalKey,
            title = spec.title,
            portionText = spec.portionText,
            minimumTargetValue = spec.minimumTargetValue,
            maximumTargetValue = spec.maximumTargetValue,
            consumedValue = consumedValue,
            sourceLabel = normalizedSourceLabel,
            period = spec.period,
            metric = spec.metric
        )
    }

    private fun WeeklyChecklistTargetSpec.isWaterTarget(): Boolean {
        return canonicalKey == "acqua" ||
                matchTerms.any { term ->
                    WeeklyFrequencyTargetSupport.normalizeKey(term) == "acqua"
                }
    }

    private fun WeeklySlotUi.matchesChecklistPeriod(
        referenceDay: WeekDay,
        period: WeeklyQuantityChecklistPeriodUi
    ): Boolean {
        return when (period) {
            WeeklyQuantityChecklistPeriodUi.DAILY -> dayOfWeek == referenceDay
            WeeklyQuantityChecklistPeriodUi.WEEKLY -> true
        }
    }
}
