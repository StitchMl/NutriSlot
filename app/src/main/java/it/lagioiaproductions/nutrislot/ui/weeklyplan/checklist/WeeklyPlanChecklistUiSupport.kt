package it.lagioiaproductions.nutrislot.ui.weeklyplan.checklist

internal val WeeklyQuantityChecklistItemUi.isExactTarget: Boolean
    get() = minimumTargetValue != null && minimumTargetValue == maximumTargetValue

internal val WeeklyQuantityChecklistItemUi.targetCeiling: Int
    get() = (maximumTargetValue ?: minimumTargetValue ?: consumedValue).coerceAtLeast(1)

internal val WeeklyQuantityChecklistItemUi.progressRatio: Float
    get() = consumedValue.coerceAtMost(targetCeiling) / targetCeiling.toFloat()

internal val WeeklyQuantityChecklistItemUi.remainingMinimumValue: Int
    get() = minimumTargetValue
        ?.let { minimum -> (minimum - consumedValue).coerceAtLeast(0) }
        ?: 0

internal val WeeklyQuantityChecklistItemUi.targetDescription: String
    get() = when {
        minimumTargetValue != null && maximumTargetValue != null && minimumTargetValue == maximumTargetValue -> {
            checklistFrequencyLabel(
                value = minimumTargetValue,
                item = this
            )
        }

        minimumTargetValue != null && maximumTargetValue != null -> {
            buildChecklistRangeLabel(
                minimumValue = minimumTargetValue,
                maximumValue = maximumTargetValue,
                metric = metric,
                period = period
            )
        }

        minimumTargetValue != null -> {
            "Almeno ${checklistFrequencyLabel(minimumTargetValue, this)}"
        }

        maximumTargetValue != null -> {
            "Max ${checklistFrequencyLabel(maximumTargetValue, this)}"
        }

        else -> "Monitoraggio ${periodLabel(period)}"
    }

internal val WeeklyQuantityChecklistItemUi.progressLabel: String
    get() = "${formatChecklistValue(consumedValue, metric)}/${formatChecklistValue(targetCeiling, metric)}"

internal val WeeklyQuantityChecklistItemUi.status: WeeklyQuantityChecklistStatusUi
    get() = when {
        maximumTargetValue != null && consumedValue > maximumTargetValue -> {
            WeeklyQuantityChecklistStatusUi.OVER_LIMIT
        }

        minimumTargetValue != null && consumedValue < minimumTargetValue -> {
            WeeklyQuantityChecklistStatusUi.UNDER_TARGET
        }

        isExactTarget -> {
            WeeklyQuantityChecklistStatusUi.COMPLETED
        }

        minimumTargetValue != null && maximumTargetValue != null && consumedValue == maximumTargetValue -> {
            WeeklyQuantityChecklistStatusUi.LIMIT_REACHED
        }

        minimumTargetValue != null && maximumTargetValue != null -> {
            WeeklyQuantityChecklistStatusUi.ON_TRACK
        }

        minimumTargetValue != null -> {
            WeeklyQuantityChecklistStatusUi.COMPLETED
        }

        maximumTargetValue != null && consumedValue == maximumTargetValue -> {
            WeeklyQuantityChecklistStatusUi.LIMIT_REACHED
        }

        maximumTargetValue != null -> {
            WeeklyQuantityChecklistStatusUi.ON_TRACK
        }

        else -> {
            WeeklyQuantityChecklistStatusUi.COMPLETED
        }
    }

internal val WeeklyQuantityChecklistItemUi.statusLabel: String
    get() = when (status) {
        WeeklyQuantityChecklistStatusUi.UNDER_TARGET -> {
            val remainingText = checklistRemainingLabel(
                value = remainingMinimumValue,
                metric = metric
            )
            if (remainingMinimumValue == 1 && metric != WeeklyQuantityChecklistMetricUi.MILLILITERS) {
                "Manca $remainingText"
            } else {
                "Mancano $remainingText"
            }
        }

        WeeklyQuantityChecklistStatusUi.ON_TRACK -> {
            if (minimumTargetValue != null) "Nel range" else "Entro il limite"
        }

        WeeklyQuantityChecklistStatusUi.COMPLETED -> "Obiettivo raggiunto"
        WeeklyQuantityChecklistStatusUi.LIMIT_REACHED -> "Limite centrato"
        WeeklyQuantityChecklistStatusUi.OVER_LIMIT -> "Oltre il limite"
    }

internal val WeeklyQuantityChecklistItemUi.statusSortOrder: Int
    get() = when (status) {
        WeeklyQuantityChecklistStatusUi.OVER_LIMIT -> 0
        WeeklyQuantityChecklistStatusUi.UNDER_TARGET -> 1
        WeeklyQuantityChecklistStatusUi.ON_TRACK -> 2
        WeeklyQuantityChecklistStatusUi.LIMIT_REACHED -> 3
        WeeklyQuantityChecklistStatusUi.COMPLETED -> 4
    }

internal val WeeklyQuantityChecklistItemUi.isSatisfied: Boolean
    get() = status != WeeklyQuantityChecklistStatusUi.UNDER_TARGET &&
            status != WeeklyQuantityChecklistStatusUi.OVER_LIMIT

private fun checklistFrequencyLabel(
    value: Int,
    item: WeeklyQuantityChecklistItemUi
): String {
    return when (item.metric) {
        WeeklyQuantityChecklistMetricUi.MILLILITERS -> {
            "${formatChecklistValue(value, item.metric)} ${periodLabel(item.period)}"
        }

        else -> {
            "${formatChecklistValue(value, item.metric)} ${targetUnitLabel(item.metric)} ${periodLabel(item.period)}"
        }
    }
}

private fun formatChecklistValue(
    value: Int,
    metric: WeeklyQuantityChecklistMetricUi
): String {
    return when (metric) {
        WeeklyQuantityChecklistMetricUi.MILLILITERS -> {
            if (value >= 1000 && value % 1000 == 0) {
                "${value / 1000} L"
            } else {
                "$value ml"
            }
        }

        else -> value.toString()
    }
}

private fun targetUnitLabel(
    metric: WeeklyQuantityChecklistMetricUi
): String {
    return when (metric) {
        WeeklyQuantityChecklistMetricUi.OCCURRENCES -> "volte"
        WeeklyQuantityChecklistMetricUi.PORTIONS -> "porzioni"
        WeeklyQuantityChecklistMetricUi.MILLILITERS -> ""
    }.trim()
}

private fun periodLabel(
    period: WeeklyQuantityChecklistPeriodUi
): String {
    return when (period) {
        WeeklyQuantityChecklistPeriodUi.DAILY -> "al giorno"
        WeeklyQuantityChecklistPeriodUi.WEEKLY -> "a settimana"
    }
}

private fun checklistRemainingLabel(
    value: Int,
    metric: WeeklyQuantityChecklistMetricUi
): String {
    return when (metric) {
        WeeklyQuantityChecklistMetricUi.MILLILITERS -> formatChecklistValue(value, metric)
        WeeklyQuantityChecklistMetricUi.PORTIONS -> {
            if (value == 1) "1 porzione" else "$value porzioni"
        }

        WeeklyQuantityChecklistMetricUi.OCCURRENCES -> {
            if (value == 1) "1 volta" else "$value volte"
        }
    }
}

internal val WeeklyQuantityChecklistItemUi.periodDescription: String
    get() = when (period) {
        WeeklyQuantityChecklistPeriodUi.DAILY -> "oggi"
        WeeklyQuantityChecklistPeriodUi.WEEKLY -> "questa settimana"
    }

internal val WeeklyQuantityChecklistItemUi.progressTrackingLabel: String
    get() = when (metric) {
        WeeklyQuantityChecklistMetricUi.MILLILITERS -> "ml registrati"
        WeeklyQuantityChecklistMetricUi.PORTIONS -> "porzioni rilevate"
        WeeklyQuantityChecklistMetricUi.OCCURRENCES -> "pasti compatibili"
    }

internal val WeeklyQuantityChecklistItemUi.hasLinkedWaterTracking: Boolean
    get() = metric == WeeklyQuantityChecklistMetricUi.MILLILITERS

private fun buildChecklistRangeLabel(
    minimumValue: Int,
    maximumValue: Int,
    metric: WeeklyQuantityChecklistMetricUi,
    period: WeeklyQuantityChecklistPeriodUi
): String {
    return when (metric) {
        WeeklyQuantityChecklistMetricUi.MILLILITERS -> {
            "${formatChecklistValue(minimumValue, metric)}-${formatChecklistValue(maximumValue, metric)} ${periodLabel(period)}"
        }

        else -> {
            "$minimumValue-$maximumValue ${targetUnitLabel(metric)} ${periodLabel(period)}"
        }
    }
}
