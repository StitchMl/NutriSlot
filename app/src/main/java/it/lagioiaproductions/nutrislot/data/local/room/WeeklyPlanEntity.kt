package it.lagioiaproductions.nutrislot.data.local.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "weekly_plans")
data class WeeklyPlanEntity(
    @PrimaryKey
    val id: String,
    val title: String?,
    val sourceFileName: String?,
    val createdAtEpochMillis: Long
)