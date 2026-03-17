package it.lagioiaproductions.nutrislot.data.local.room

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction

@Entity(
    tableName = "weekly_plans"
)
data class WeeklyPlanEntity(
    @PrimaryKey
    val id: String,
    val title: String?,
    val sourceFileName: String?,
    val createdAtEpochMillis: Long
)

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

@Entity(
    tableName = "meal_consumptions",
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
data class MealConsumptionEntity(
    @PrimaryKey
    val id: String,
    val planId: String,
    val targetSlotId: String,
    val sourceSlotId: String,
    val consumedAtEpochMillis: Long
)

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

@Dao
interface WeeklyPlanDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeeklyPlan(plan: WeeklyPlanEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMealSlots(slots: List<MealSlotEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMealConsumptions(consumptions: List<MealConsumptionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMealAssignments(assignments: List<MealAssignmentEntity>)

    @Transaction
    suspend fun insertImportedPlan(
        plan: WeeklyPlanEntity,
        slots: List<MealSlotEntity>
    ) {
        insertWeeklyPlan(plan)
        insertMealSlots(slots)
    }

    @Query("SELECT * FROM weekly_plans WHERE id = :planId LIMIT 1")
    suspend fun getPlanById(planId: String): WeeklyPlanEntity?

    @Query("SELECT * FROM weekly_plans ORDER BY createdAtEpochMillis DESC LIMIT 1")
    suspend fun getLatestPlan(): WeeklyPlanEntity?

    @Query("SELECT * FROM meal_slots WHERE planId = :planId")
    suspend fun getSlotsForPlan(planId: String): List<MealSlotEntity>

    @Query("SELECT * FROM meal_consumptions WHERE planId = :planId")
    suspend fun getConsumptionsForPlan(planId: String): List<MealConsumptionEntity>

    @Query("SELECT * FROM meal_assignments WHERE planId = :planId")
    suspend fun getAssignmentsForPlan(planId: String): List<MealAssignmentEntity>

    @Query("DELETE FROM meal_assignments WHERE id IN (:assignmentIds)")
    suspend fun deleteMealAssignmentsByIds(assignmentIds: List<String>)
}

@Database(
    entities = [
        WeeklyPlanEntity::class,
        MealSlotEntity::class,
        MealConsumptionEntity::class,
        MealAssignmentEntity::class
    ],
    version = 2,
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