package it.lagioiaproductions.nutrislot.ui.weight.support

import it.lagioiaproductions.nutrislot.ui.shared.WeightEntryUi
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

internal data class WeightDateOption(
    val dateKey: String,
    val dayLabel: String,
    val dateLabel: String
)

internal fun buildRecentDateOptions(): List<WeightDateOption> {
    val keyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
    val dateFormat = SimpleDateFormat("dd MMM", Locale.getDefault())
    val calendar = Calendar.getInstance()

    return buildList {
        repeat(7) { offset ->
            val copy = calendar.clone() as Calendar
            copy.add(Calendar.DAY_OF_YEAR, -offset)
            val date = copy.time

            add(
                WeightDateOption(
                    dateKey = keyFormat.format(date),
                    dayLabel = dayFormat.format(date).replaceFirstChar { it.uppercase() },
                    dateLabel = dateFormat.format(date)
                )
            )
        }
    }
}

internal fun todayDateKey(): String {
    return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
}

internal fun formatWeight(value: Float): String {
    return String.format(Locale.getDefault(), "%.1f", value)
}

internal fun formatDelta(value: Float): String {
    val prefix = if (value > 0f) "+" else ""
    return "$prefix${formatWeight(value)} kg"
}

internal fun formatFullDate(dateKey: String): String {
    val source = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val target = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
    val parsed = runCatching { source.parse(dateKey) }.getOrNull()
    return parsed?.let(target::format) ?: dateKey
}

internal fun recentAverageWeight(
    entries: List<WeightEntryUi>,
    sampleSize: Int = 7
): Float? {
    val sample = entries
        .sortedByDescending { it.createdAtEpochMillis }
        .take(sampleSize)

    if (sample.isEmpty()) return null

    val average = sample.map { it.weightKg.toDouble() }.average()
    return (average * 10.0).roundToInt() / 10f
}

internal fun validateWeightInput(input: String): String? {
    val parsedWeight = input
        .trim()
        .replace(',', '.')
        .toFloatOrNull()

    return when (parsedWeight) {
        null -> "Inserisci un numero valido."
        !in 20f..400f -> "Il peso deve essere compreso tra 20 e 400 kg."
        else -> null
    }
}

internal fun parseWeightInput(input: String): Float {
    return input.trim().replace(',', '.').toFloat()
}
