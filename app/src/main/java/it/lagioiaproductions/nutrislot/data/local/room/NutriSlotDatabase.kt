package it.lagioiaproductions.nutrislot.data.local.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        WeeklyPlanEntity::class,
        MealSlotEntity::class,
        MealConsumptionEntity::class,
        MealAssignmentEntity::class,
        MealOptionEntity::class,
        MealRuleEntity::class,
        WeeklyFrequencyTargetEntity::class,
        ShoppingListItemEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class NutriSlotDatabase : RoomDatabase() {

    abstract fun weeklyPlanDao(): WeeklyPlanDao

    companion object {
        @Volatile
        private var INSTANCE: NutriSlotDatabase? = null

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `shopping_list_items` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `isPurchased` INTEGER NOT NULL,
                        `createdAtEpochMillis` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS `index_shopping_list_items_createdAtEpochMillis`
                    ON `shopping_list_items` (`createdAtEpochMillis`)
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS `index_shopping_list_items_isPurchased_createdAtEpochMillis`
                    ON `shopping_list_items` (`isPurchased`, `createdAtEpochMillis`)
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `weekly_frequency_targets` (
                        `id` TEXT NOT NULL,
                        `planId` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `canonicalKey` TEXT NOT NULL,
                        `portionText` TEXT,
                        `minimumTimesPerWeek` INTEGER,
                        `maximumTimesPerWeek` INTEGER,
                        `matchTermsSerialized` TEXT NOT NULL,
                        `pageNumber` INTEGER,
                        `sourceText` TEXT,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`planId`) REFERENCES `weekly_plans`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS `index_weekly_frequency_targets_planId`
                    ON `weekly_frequency_targets` (`planId`)
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS `index_weekly_frequency_targets_planId_canonicalKey`
                    ON `weekly_frequency_targets` (`planId`, `canonicalKey`)
                    """.trimIndent()
                )
            }
        }

        fun getInstance(context: Context): NutriSlotDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    NutriSlotDatabase::class.java,
                    "nutrislot.db"
                )
                    .addMigrations(MIGRATION_3_4, MIGRATION_4_5)
                    .build()
                    .also { database ->
                        INSTANCE = database
                    }
            }
        }
    }
}
