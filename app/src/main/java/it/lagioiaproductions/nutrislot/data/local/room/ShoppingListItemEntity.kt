package it.lagioiaproductions.nutrislot.data.local.room

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "shopping_list_items",
    indices = [
        Index(value = ["createdAtEpochMillis"]),
        Index(value = ["isPurchased", "createdAtEpochMillis"])
    ]
)
data class ShoppingListItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val isPurchased: Boolean = false,
    val createdAtEpochMillis: Long
)