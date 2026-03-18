package it.lagioiaproductions.nutrislot.data.local.room

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "meal_rules",
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
        Index(value = ["planId", "mealSlotType"])
    ]
)
data class MealRuleEntity(
    @PrimaryKey
    val id: String,
    val planId: String,
    val mealSlotType: String,
    val label: String,
    val requiredComponentsSerialized: String,
    val pageNumber: Int?
)