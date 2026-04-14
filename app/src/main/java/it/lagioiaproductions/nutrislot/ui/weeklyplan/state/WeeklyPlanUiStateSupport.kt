package it.lagioiaproductions.nutrislot.ui.weeklyplan.state

import it.lagioiaproductions.nutrislot.domain.model.WeekDay

internal val WeeklyPlanUiState.isEmpty: Boolean
    get() = hasLoadedOnce && slots.isEmpty() && errorMessage == null

internal val orderedCalendarDays: List<WeekDay>
    get() = WeekDay.orderedValues()
