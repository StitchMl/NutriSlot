package it.lagioiaproductions.nutrislot.ui.shared

internal class AppBridgeCalorieJournalManager(
    private val store: AppBridgeCalorieJournalStore
) {

    val initialJournal: Map<String, CalorieDayLogUi> = store.load()

    private var nextEntryId: Long = store.computeNextEntryId(initialJournal)

    fun consumePendingProduct(
        current: AppBridgeUiState,
        dayKey: String
    ): AppBridgeUiState? {
        val product = current.pendingCalorieProduct ?: return null
        val section = AppBridgeSupport.inferSectionFromScannerProduct(product)

        val entry = CalorieJournalEntryUi(
            id = nextEntryId++,
            title = product.name.ifBlank { "Prodotto scannerizzato" },
            subtitle = product.subtitle,
            calories = product.calories,
            protein = product.protein,
            carbs = product.carbs,
            fibre = product.fibre,
            section = section,
            timeLabel = AppBridgeSupport.currentTimeLabel(),
            sourceLabel = "Scanner"
        )

        val updatedJournal = appendEntry(
            currentJournal = current.calorieJournalByDate,
            dayKey = dayKey,
            entry = entry
        )

        return current.copy(
            pendingCalorieProduct = null,
            calorieJournalByDate = updatedJournal
        )
    }

    fun addWeeklyPlanConsumption(
        current: AppBridgeUiState,
        dayKey: String,
        consumptionId: String,
        mealText: String,
        mealSlotLabel: String
    ): AppBridgeUiState {
        val parsed = AppBridgeSupport.parseNutritionFromMealText(mealText)
        val section = AppBridgeSupport.inferSectionFromSlotLabel(mealSlotLabel)
        val titleSubtitle = AppBridgeSupport.splitMealText(mealText)

        val entry = CalorieJournalEntryUi(
            id = nextEntryId++,
            title = titleSubtitle.first.ifBlank { "${section.label} dal planner" },
            subtitle = titleSubtitle.second.ifBlank { "Da Weekly Plan" },
            calories = parsed.calories,
            protein = parsed.protein,
            carbs = parsed.carbs,
            fibre = parsed.fibre,
            section = section,
            timeLabel = AppBridgeSupport.currentTimeLabel(),
            sourceLabel = "Weekly Plan",
            plannerConsumptionId = consumptionId
        )

        return current.copy(
            calorieJournalByDate = appendEntry(
                currentJournal = current.calorieJournalByDate,
                dayKey = dayKey,
                entry = entry
            )
        )
    }

    fun removeWeeklyPlanConsumption(
        current: AppBridgeUiState,
        consumptionId: String
    ): AppBridgeUiState? {
        if (consumptionId.isBlank()) return null

        var changed = false
        val updatedJournal = current.calorieJournalByDate
            .mapValues { (_, dayLog) ->
                val filteredEntries = dayLog.entries.filterNot { entry ->
                    entry.plannerConsumptionId == consumptionId
                }

                if (filteredEntries.size != dayLog.entries.size) {
                    changed = true
                }

                dayLog.copy(entries = filteredEntries)
            }
            .filterValues { dayLog ->
                dayLog.goalKcal != null || dayLog.entries.isNotEmpty()
            }

        if (!changed) return null

        store.persist(updatedJournal)
        return current.copy(calorieJournalByDate = updatedJournal)
    }

    fun setGoalForDay(
        current: AppBridgeUiState,
        dayKey: String,
        goalKcal: Int?
    ): AppBridgeUiState {
        val normalizedGoal = goalKcal?.takeIf { it > 0 }
        val mutableJournal = current.calorieJournalByDate.toMutableMap()
        val currentDay = mutableJournal[dayKey] ?: CalorieDayLogUi()

        if (normalizedGoal == null && currentDay.entries.isEmpty()) {
            mutableJournal.remove(dayKey)
        } else {
            mutableJournal[dayKey] = currentDay.copy(goalKcal = normalizedGoal)
        }

        val updatedJournal = mutableJournal.toMap()
        store.persist(updatedJournal)
        return current.copy(calorieJournalByDate = updatedJournal)
    }

    fun removeEntry(
        current: AppBridgeUiState,
        dayKey: String,
        entryId: Long
    ): AppBridgeUiState? {
        val mutableJournal = current.calorieJournalByDate.toMutableMap()
        val currentDay = mutableJournal[dayKey] ?: return null
        val updatedEntries = currentDay.entries.filterNot { it.id == entryId }

        if (updatedEntries.size == currentDay.entries.size) return null

        if (updatedEntries.isEmpty() && currentDay.goalKcal == null) {
            mutableJournal.remove(dayKey)
        } else {
            mutableJournal[dayKey] = currentDay.copy(entries = updatedEntries)
        }

        val updatedJournal = mutableJournal.toMap()
        store.persist(updatedJournal)
        return current.copy(calorieJournalByDate = updatedJournal)
    }

    fun resetDay(
        current: AppBridgeUiState,
        dayKey: String
    ): AppBridgeUiState? {
        val mutableJournal = current.calorieJournalByDate.toMutableMap()
        val currentDay = mutableJournal[dayKey] ?: return null

        if (currentDay.goalKcal == null) {
            mutableJournal.remove(dayKey)
        } else {
            mutableJournal[dayKey] = currentDay.copy(entries = emptyList())
        }

        val updatedJournal = mutableJournal.toMap()
        store.persist(updatedJournal)
        return current.copy(calorieJournalByDate = updatedJournal)
    }

    private fun appendEntry(
        currentJournal: Map<String, CalorieDayLogUi>,
        dayKey: String,
        entry: CalorieJournalEntryUi
    ): Map<String, CalorieDayLogUi> {
        val mutableJournal = currentJournal.toMutableMap()
        val currentDay = mutableJournal[dayKey] ?: CalorieDayLogUi()

        mutableJournal[dayKey] = currentDay.copy(
            entries = listOf(entry) + currentDay.entries
        )

        val updatedJournal = mutableJournal.toMap()
        store.persist(updatedJournal)
        return updatedJournal
    }
}
