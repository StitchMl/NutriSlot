package it.lagioiaproductions.nutrislot.data.water

data class WaterStoredPreferences(
    val targetMl: Int = 2000,
    val consumedMl: Int = 0,
    val remindersEnabled: Boolean = false,
    val reminderIntervalMinutes: Int = 90,
    val containerPresets: List<Int> = listOf(250, 500, 600),
    val lastTrackedDayKey: String = ""
)