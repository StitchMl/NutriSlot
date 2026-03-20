package it.lagioiaproductions.nutrislot.notifications.water

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object WaterReminderScheduler {
    private const val UNIQUE_WORK_NAME = "water_reminder_periodic_work"

    fun schedule(
        context: Context,
        intervalMinutes: Int
    ) {
        val normalizedInterval = intervalMinutes.coerceAtLeast(15)

        val request = PeriodicWorkRequestBuilder<WaterReminderWorker>(
            normalizedInterval.toLong(),
            TimeUnit.MINUTES
        )
            .setInitialDelay(normalizedInterval.toLong(), TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
            request
        )
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK_NAME)
    }
}