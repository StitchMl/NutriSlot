package it.lagioiaproductions.nutrislot.data.local.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        WeeklyPlanEntity::class,
        MealSlotEntity::class,
        MealConsumptionEntity::class,
        MealAssignmentEntity::class,
        MealOptionEntity::class,
        MealRuleEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class NutriSlotDatabase : RoomDatabase() {

    abstract fun weeklyPlanDao(): WeeklyPlanDao

    companion object {
        @Volatile
        private var INSTANCE: NutriSlotDatabase? = null

        fun getInstance(context: Context): NutriSlotDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    NutriSlotDatabase::class.java,
                    "nutrislot.db"
                )
                    .fallbackToDestructiveMigration(false)
                    .build()
                    .also { database ->
                        INSTANCE = database
                    }
            }
        }
    }
}