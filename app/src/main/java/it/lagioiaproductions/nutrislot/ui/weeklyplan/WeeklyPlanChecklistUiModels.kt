package it.lagioiaproductions.nutrislot.ui.weeklyplan

enum class WeeklyQuantityChecklistStatusUi {
    UNDER_TARGET,
    ON_TRACK,
    COMPLETED,
    LIMIT_REACHED,
    OVER_LIMIT
}

enum class WeeklyQuantityChecklistPeriodUi {
    DAILY,
    WEEKLY
}

enum class WeeklyQuantityChecklistMetricUi {
    OCCURRENCES,
    PORTIONS,
    MILLILITERS
}

data class WeeklyQuantityChecklistItemUi(
    val id: String,
    val title: String,
    val portionText: String?,
    val minimumTargetValue: Int?,
    val maximumTargetValue: Int?,
    val consumedValue: Int,
    val sourceLabel: String,
    val period: WeeklyQuantityChecklistPeriodUi,
    val metric: WeeklyQuantityChecklistMetricUi
)
