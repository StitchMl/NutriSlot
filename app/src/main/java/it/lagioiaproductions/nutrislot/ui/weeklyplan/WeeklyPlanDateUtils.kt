package it.lagioiaproductions.nutrislot.ui.weeklyplan

import it.lagioiaproductions.nutrislot.domain.model.WeekDay
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

internal fun currentWeekDay(): WeekDay {
    return when (LocalDate.now(ZoneId.systemDefault()).dayOfWeek) {
        DayOfWeek.MONDAY -> WeekDay.MONDAY
        DayOfWeek.TUESDAY -> WeekDay.TUESDAY
        DayOfWeek.WEDNESDAY -> WeekDay.WEDNESDAY
        DayOfWeek.THURSDAY -> WeekDay.THURSDAY
        DayOfWeek.FRIDAY -> WeekDay.FRIDAY
        DayOfWeek.SATURDAY -> WeekDay.SATURDAY
        DayOfWeek.SUNDAY -> WeekDay.SUNDAY
    }
}

internal fun isInCurrentWeek(epochMillis: Long): Boolean {
    val zoneId = ZoneId.systemDefault()
    val date = Instant.ofEpochMilli(epochMillis).atZone(zoneId).toLocalDate()
    val today = LocalDate.now(zoneId)
    val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val nextWeekStart = weekStart.plusWeeks(1)

    return !date.isBefore(weekStart) && date.isBefore(nextWeekStart)
}