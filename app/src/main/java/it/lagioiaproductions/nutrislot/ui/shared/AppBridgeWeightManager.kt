package it.lagioiaproductions.nutrislot.ui.shared

import kotlin.math.roundToInt

internal class AppBridgeWeightManager(
    private val store: AppBridgeWeightStore
) {

    val initialEntries: List<WeightEntryUi> = store.load()
    val initialSummary: WeightSummaryUi = buildWeightSummary(initialEntries)

    private var nextEntryId: Long = store.computeNextEntryId(initialEntries)

    fun addEntry(
        current: AppBridgeUiState,
        weightKg: Float,
        dateKey: String,
        note: String = ""
    ): AppBridgeUiState? {
        val normalizedWeight = normalizeWeight(weightKg) ?: return null
        val entry = WeightEntryUi(
            id = nextEntryId++,
            weightKg = normalizedWeight,
            dateKey = dateKey,
            note = note.trim(),
            createdAtEpochMillis = System.currentTimeMillis()
        )

        val updatedEntries = (listOf(entry) + current.weightEntries)
            .sortedByDescending { it.createdAtEpochMillis }

        store.persist(updatedEntries)
        return current.copy(
            weightEntries = updatedEntries,
            weightSummary = buildWeightSummary(updatedEntries)
        )
    }

    fun removeEntry(
        current: AppBridgeUiState,
        entryId: Long
    ): AppBridgeUiState? {
        val updatedEntries = current.weightEntries
            .filterNot { it.id == entryId }
            .sortedByDescending { it.createdAtEpochMillis }

        if (updatedEntries.size == current.weightEntries.size) return null

        store.persist(updatedEntries)
        return current.copy(
            weightEntries = updatedEntries,
            weightSummary = buildWeightSummary(updatedEntries)
        )
    }

    private fun normalizeWeight(value: Float): Float? {
        if (value.isNaN() || value.isInfinite()) return null
        if (value !in 20f..400f) return null
        return (value * 10f).roundToInt() / 10f
    }

    private fun buildWeightSummary(entries: List<WeightEntryUi>): WeightSummaryUi {
        val sorted = entries.sortedByDescending { it.createdAtEpochMillis }
        val latest = sorted.getOrNull(0)?.weightKg
        val previous = sorted.getOrNull(1)?.weightKg
        val delta = if (latest != null && previous != null) {
            ((latest - previous) * 10f).roundToInt() / 10f
        } else {
            null
        }

        return WeightSummaryUi(
            latestWeightKg = latest,
            previousWeightKg = previous,
            deltaFromPreviousKg = delta
        )
    }
}
