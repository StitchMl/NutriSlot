package it.lagioiaproductions.nutrislot.domain.model

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

data class CurrentWeekWindow(
    val zoneId: ZoneId,
    val today: LocalDate,
    val weekStart: LocalDate,
    val nextWeekStart: LocalDate
) {
    val currentWeekDay: WeekDay
        get() = today.dayOfWeek.toWeekDay()

    fun contains(epochMillis: Long): Boolean {
        val date = Instant.ofEpochMilli(epochMillis).atZone(zoneId).toLocalDate()
        return !date.isBefore(weekStart) && date.isBefore(nextWeekStart)
    }

    fun dateFor(dayOfWeek: WeekDay): LocalDate {
        return weekStart.plusDays(dayOfWeek.sortOrder.toLong())
    }

    fun keyFor(dayOfWeek: WeekDay): String {
        return dateFor(dayOfWeek).format(DateTimeFormatter.ISO_LOCAL_DATE)
    }
}

fun currentWeekWindow(
    zoneId: ZoneId = ZoneId.systemDefault()
): CurrentWeekWindow {
    val today = LocalDate.now(zoneId)
    val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    return CurrentWeekWindow(
        zoneId = zoneId,
        today = today,
        weekStart = weekStart,
        nextWeekStart = weekStart.plusWeeks(1)
    )
}

fun DayOfWeek.toWeekDay(): WeekDay {
    return when (this) {
        DayOfWeek.MONDAY -> WeekDay.MONDAY
        DayOfWeek.TUESDAY -> WeekDay.TUESDAY
        DayOfWeek.WEDNESDAY -> WeekDay.WEDNESDAY
        DayOfWeek.THURSDAY -> WeekDay.THURSDAY
        DayOfWeek.FRIDAY -> WeekDay.FRIDAY
        DayOfWeek.SATURDAY -> WeekDay.SATURDAY
        DayOfWeek.SUNDAY -> WeekDay.SUNDAY
    }
}
