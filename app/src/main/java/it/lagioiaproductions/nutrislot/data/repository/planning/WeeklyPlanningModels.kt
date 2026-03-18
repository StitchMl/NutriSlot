package it.lagioiaproductions.nutrislot.data.repository.planning

internal data class SourceUsage(
    val targetSlotId: String,
    val sourceSlotId: String
)

internal data class ActiveWeekPlanning(
    val actualSourceByTarget: Map<String, String>,
    val pendingSourceByTarget: Map<String, String>,
    val usedSourceByTarget: List<SourceUsage>
)