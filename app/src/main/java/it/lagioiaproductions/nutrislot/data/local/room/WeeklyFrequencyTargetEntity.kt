package it.lagioiaproductions.nutrislot.data.local.room

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "weekly_frequency_targets",
    foreignKeys = [
        ForeignKey(
            entity = WeeklyPlanEntity::class,
            parentColumns = ["id"],
            childColumns = ["planId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["planId"]),
        Index(value = ["planId", "canonicalKey"])
    ]
)
data class WeeklyFrequencyTargetEntity(
    @PrimaryKey
    val id: String,
    val planId: String,
    val title: String,
    val canonicalKey: String,
    val portionText: String?,
    val minimumTimesPerWeek: Int?,
    val maximumTimesPerWeek: Int?,
    val matchTermsSerialized: String,
    val pageNumber: Int?,
    val sourceText: String?
)
