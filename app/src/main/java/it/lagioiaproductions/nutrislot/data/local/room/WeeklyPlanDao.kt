package it.lagioiaproductions.nutrislot.data.local.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMealOptions(options: List<MealOptionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMealRules(rules: List<MealRuleEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeeklyFrequencyTargets(targets: List<WeeklyFrequencyTargetEntity>)

    @Transaction
    suspend fun insertImportedPlan(
        plan: WeeklyPlanEntity,
        slots: List<MealSlotEntity>,
        options: List<MealOptionEntity>,
        rules: List<MealRuleEntity>,
        weeklyTargets: List<WeeklyFrequencyTargetEntity>
    ) {
        insertWeeklyPlan(plan)
        insertMealSlots(slots)
        insertMealOptions(options)
        insertMealRules(rules)
        insertWeeklyFrequencyTargets(weeklyTargets)
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

    @Query("SELECT * FROM meal_option_catalog WHERE planId = :planId")
    suspend fun getMealOptionsForPlan(planId: String): List<MealOptionEntity>

    @Query("SELECT * FROM meal_rules WHERE planId = :planId")
    suspend fun getMealRulesForPlan(planId: String): List<MealRuleEntity>

    @Query("SELECT * FROM weekly_frequency_targets WHERE planId = :planId")
    suspend fun getWeeklyFrequencyTargetsForPlan(planId: String): List<WeeklyFrequencyTargetEntity>

    @Query("DELETE FROM meal_assignments WHERE id IN (:assignmentIds)")
    suspend fun deleteMealAssignmentsByIds(assignmentIds: List<String>)

    @Query("DELETE FROM meal_consumptions WHERE id IN (:consumptionIds)")
    suspend fun deleteMealConsumptionsByIds(consumptionIds: List<String>)

    @Query(
        """
        SELECT * FROM shopping_list_items
        ORDER BY isPurchased ASC, createdAtEpochMillis ASC, id ASC
        """
    )
    fun observeShoppingListItems(): Flow<List<ShoppingListItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShoppingListItem(item: ShoppingListItemEntity): Long

    @Query("UPDATE shopping_list_items SET isPurchased = NOT isPurchased WHERE id = :itemId")
    suspend fun toggleShoppingListItemPurchased(itemId: Long)

    @Query("DELETE FROM shopping_list_items WHERE id = :itemId")
    suspend fun deleteShoppingListItemById(itemId: Long)

    @Query("DELETE FROM shopping_list_items")
    suspend fun clearShoppingList()
}
