package it.lagioiaproductions.nutrislot.ui.calories

import it.lagioiaproductions.nutrislot.ui.shared.CalorieDayLogUi
import it.lagioiaproductions.nutrislot.ui.shared.CalorieJournalEntryUi
import it.lagioiaproductions.nutrislot.ui.shared.CalorieJournalSection
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.roundToInt

internal data class CalorieTrackerSectionUi(
    val section: CalorieJournalSection,
    val entries: List<CalorieJournalEntryUi>
)

internal data class CalorieTrackerDayUi(
    val dayKey: String,
    val dateTitle: String,
    val weekdayLabel: String,
    val goalKcal: Int?,
    val calories: Int,
    val protein: Int,
    val carbs: Int,
    val fibre: Int,
    val progress: Float,
    val progressPercent: Int,
    val sectionEntries: List<CalorieTrackerSectionUi>
)

internal fun buildCalorieTrackerDayUi(
    selectedDayOffset: Int,
    calorieJournalByDate: Map<String, CalorieDayLogUi>
): CalorieTrackerDayUi {
    val dayKey = dayKeyForOffset(selectedDayOffset)
    val dayLog = calorieJournalByDate[dayKey] ?: CalorieDayLogUi()
    val entries = dayLog.entries.sortedByDescending { it.id }

    val calories = entries.sumOf { it.calories }
    val protein = entries.sumOf { it.protein }
    val carbs = entries.sumOf { it.carbs }
    val fibre = entries.sumOf { it.fibre }

    val goalKcal = dayLog.goalKcal
    val progress = if (goalKcal != null && goalKcal > 0) {
        (calories.toFloat() / goalKcal.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    val sectionEntries = CalorieJournalSection.entries.mapNotNull { section ->
        val sectionItems = entries.filter { it.section == section }
        sectionItems.takeIf { it.isNotEmpty() }?.let {
            CalorieTrackerSectionUi(
                section = section,
                entries = it
            )
        }
    }

    return CalorieTrackerDayUi(
        dayKey = dayKey,
        dateTitle = longDateForOffset(selectedDayOffset),
        weekdayLabel = shortWeekdayForOffset(selectedDayOffset),
        goalKcal = goalKcal,
        calories = calories,
        protein = protein,
        carbs = carbs,
        fibre = fibre,
        progress = progress,
        progressPercent = (progress * 100f).roundToInt(),
        sectionEntries = sectionEntries
    )
}

private fun calendarForOffset(offset: Int): Calendar {
    return Calendar.getInstance().apply {
        add(Calendar.DAY_OF_YEAR, offset)
    }
}

private fun dayKeyForOffset(offset: Int): String {
    return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        .format(calendarForOffset(offset).time)
}

private fun longDateForOffset(offset: Int): String {
    return SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
        .format(calendarForOffset(offset).time)
}

private fun shortWeekdayForOffset(offset: Int): String {
    return SimpleDateFormat("EEE", Locale.getDefault())
        .format(calendarForOffset(offset).time)
}
