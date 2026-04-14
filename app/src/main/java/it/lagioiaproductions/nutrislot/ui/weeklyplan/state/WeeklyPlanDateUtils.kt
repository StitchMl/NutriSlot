package it.lagioiaproductions.nutrislot.ui.weeklyplan.state

import it.lagioiaproductions.nutrislot.domain.model.WeekDay
import it.lagioiaproductions.nutrislot.domain.model.currentWeekWindow

internal fun currentWeekDay(): WeekDay {
    return currentWeekWindow().currentWeekDay
}

internal fun isInCurrentWeek(epochMillis: Long): Boolean {
    return currentWeekWindow().contains(epochMillis)
}

internal fun dayKeyForCurrentWeek(dayOfWeek: WeekDay): String {
    return currentWeekWindow().keyFor(dayOfWeek)
}

