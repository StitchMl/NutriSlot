@file:Suppress("DEPRECATION")

package it.lagioiaproductions.nutrislot.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.RemoteViews
import androidx.core.net.toUri
import it.lagioiaproductions.nutrislot.MainActivity
import it.lagioiaproductions.nutrislot.R

class MealCalendarWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        updateWidgets(
            context = context,
            appWidgetManager = appWidgetManager,
            appWidgetIds = appWidgetIds
        )
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        updateWidgets(
            context = context,
            appWidgetManager = appWidgetManager,
            appWidgetIds = intArrayOf(appWidgetId)
        )
    }

    override fun onReceive(
        context: Context,
        intent: Intent
    ) {
        super.onReceive(context, intent)

        when (intent.action) {
            Intent.ACTION_DATE_CHANGED,
            Intent.ACTION_LOCALE_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED -> refresh(context)
        }
    }

    companion object {
        fun refresh(context: Context) {
            val appContext = context.applicationContext
            val appWidgetManager = AppWidgetManager.getInstance(appContext)
            val componentName = ComponentName(appContext, MealCalendarWidgetProvider::class.java)
            val widgetIds = appWidgetManager.getAppWidgetIds(componentName)
            if (widgetIds.isEmpty()) return

            updateWidgets(
                context = appContext,
                appWidgetManager = appWidgetManager,
                appWidgetIds = widgetIds
            )
        }

        private fun updateWidgets(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetIds: IntArray
        ) {
            if (appWidgetIds.isEmpty()) return

            val subtitle = context.getString(
                R.string.meal_calendar_widget_subtitle_format,
                MealCalendarWidgetSupport.weekRangeLabel()
            )

            appWidgetIds.forEach { appWidgetId ->
                val openAppPendingIntent = openAppPendingIntent(context)
                val views = RemoteViews(context.packageName, R.layout.widget_meal_calendar).apply {
                    setTextViewText(
                        R.id.widget_title,
                        context.getString(R.string.meal_calendar_widget_title)
                    )
                    setTextViewText(R.id.widget_subtitle, subtitle)
                    setTextViewText(
                        R.id.widget_empty,
                        context.getString(R.string.meal_calendar_widget_empty)
                    )

                    setOnClickPendingIntent(R.id.widget_header, openAppPendingIntent)
                    setOnClickPendingIntent(R.id.widget_empty, openAppPendingIntent)
                    setPendingIntentTemplate(R.id.widget_list, openAppPendingIntent)
                    setEmptyView(R.id.widget_list, R.id.widget_empty)
                    setRemoteAdapter(
                        R.id.widget_list,
                        Intent(context, MealCalendarWidgetRemoteViewsService::class.java).apply {
                            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                            data = toUri(Intent.URI_INTENT_SCHEME).toUri()
                        }
                    )
                }

                appWidgetManager.updateAppWidget(appWidgetId, views)
            }

            appWidgetManager.notifyAppWidgetViewDataChanged(
                appWidgetIds,
                R.id.widget_list
            )
        }

        private fun openAppPendingIntent(context: Context): PendingIntent {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }

            return PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }
}
