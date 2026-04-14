package it.lagioiaproductions.nutrislot.ui.water.state

data class WaterTrackerUiState(
    val targetMl: Int = 2000,
    val consumedMl: Int = 0,
    val remindersEnabled: Boolean = false,
    val reminderIntervalMinutes: Int = 90,
    val containerPresets: List<Int> = listOf(250, 500, 600)
) {
    val isGoalConfigured: Boolean
        get() = targetMl > 0

    val progressRaw: Float
        get() = if (!isGoalConfigured) 0f else consumedMl.toFloat() / targetMl.toFloat()

    val progressVisual: Float
        get() = progressRaw.coerceIn(0f, 1f)

    val remainingMl: Int
        get() = if (!isGoalConfigured) 0 else (targetMl - consumedMl).coerceAtLeast(0)

    val isGoalReached: Boolean
        get() = isGoalConfigured && consumedMl >= targetMl

    val hydrationStatusLabel: String
        get() = when {
            !isGoalConfigured -> "Goal non impostato"
            progressRaw < 0.35f -> "Low"
            progressRaw < 0.8f -> "Normal"
            else -> "Great"
        }

    val mainPresetMl: Int
        get() = containerPresets.lastOrNull() ?: 600
}

fun formatWaterAmount(amountMl: Int): String {
    val normalized = amountMl.coerceAtLeast(0)
    return if (normalized >= 1000 && normalized % 1000 == 0) {
        "${normalized / 1000} L"
    } else {
        "$normalized ml"
    }
}