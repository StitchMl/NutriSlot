package it.lagioiaproductions.nutrislot.ui.weeklyplan.state

import it.lagioiaproductions.nutrislot.data.water.WaterStoredPreferences
import it.lagioiaproductions.nutrislot.ui.weeklyplan.checklist.WeeklyChecklistHydrationSnapshot

internal fun WaterStoredPreferences.toChecklistHydrationSnapshot(): WeeklyChecklistHydrationSnapshot {
    return WeeklyChecklistHydrationSnapshot(
        consumedMl = consumedMl.coerceAtLeast(0),
        targetMl = targetMl.coerceAtLeast(0)
    )
}
