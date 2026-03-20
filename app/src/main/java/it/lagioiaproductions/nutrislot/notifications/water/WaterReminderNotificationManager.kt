package it.lagioiaproductions.nutrislot.notifications.water

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context

object WaterReminderNotificationManager {
    const val CHANNEL_ID = "water_reminders"
    const val CHANNEL_NAME = "Water reminders"
    const val NOTIFICATION_ID = 1001

    fun createChannel(context: Context) {

        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Hydration reminders"
        }

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }
}