package it.lagioiaproductions.nutrislot.data.local.room

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "meal_assignments",
    foreignKeys = [
        ForeignKey(
            entity = WeeklyPlanEntity::class,
            parentColumns = ["id"],
            childColumns = ["planId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = MealSlotEntity::class,
            parentColumns = ["id"],
            childColumns = ["targetSlotId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = MealSlotEntity::class,
            parentColumns = ["id"],
            childColumns = ["sourceSlotId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["planId"]),
        Index(value = ["targetSlotId"]),
        Index(value = ["sourceSlotId"])
    ]
)
data class MealAssignmentEntity(
    @PrimaryKey
    val id: String,
    val planId: String,
    val targetSlotId: String,
    val sourceSlotId: String,
    val assignedAtEpochMillis: Long
)