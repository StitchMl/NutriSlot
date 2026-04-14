package it.lagioiaproductions.nutrislot.ui.weeklyplan

import it.lagioiaproductions.nutrislot.data.water.WaterStoredPreferences

internal fun WaterStoredPreferences.toChecklistHydrationSnapshot(): WeeklyChecklistHydrationSnapshot {
    return WeeklyChecklistHydrationSnapshot(
        consumedMl = consumedMl.coerceAtLeast(0),
        targetMl = targetMl.coerceAtLeast(0)
    )
}
