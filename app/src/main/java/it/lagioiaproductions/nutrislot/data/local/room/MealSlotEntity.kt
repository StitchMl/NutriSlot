package it.lagioiaproductions.nutrislot.data.local.room

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "meal_slots",
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
        Index(value = ["planId", "dayOfWeek", "mealSlotType"], unique = true)
    ]
)
data class MealSlotEntity(
    @PrimaryKey
    val id: String,
    val planId: String,
    val dayOfWeek: String,
    val mealSlotType: String,
    val plannedMealText: String
)