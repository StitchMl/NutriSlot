package it.lagioiaproductions.nutrislot.ui.water

import kotlin.math.roundToInt

data class WaterTrackerUiState(
    val targetMl: Int,
    val consumedMl: Int,
    val remindersEnabled: Boolean,
    val reminderIntervalMinutes: Int
) {
    val progressRaw: Float
        get() = if (targetMl <= 0) 0f else consumedMl.toFloat() / targetMl.toFloat()

    val progressVisual: Float
        get() = progressRaw.coerceIn(0f, 1f)

    val progressPercent: Int
        get() = (progressRaw * 100f).roundToInt().coerceAtLeast(0)

    val remainingMl: Int
        get() = (targetMl - consumedMl).coerceAtLeast(0)

    val isGoalReached: Boolean
        get() = targetMl in 1..consumedMl

    val hydrationStatus: HydrationStatus
        get() = when {
            progressRaw < 0.35f -> HydrationStatus.Low
            progressRaw < 0.8f -> HydrationStatus.Normal
            else -> HydrationStatus.Great
        }
}

enum class HydrationStatus(val label: String) {
    Low("Low"),
    Normal("Normal"),
    Great("Great")
}