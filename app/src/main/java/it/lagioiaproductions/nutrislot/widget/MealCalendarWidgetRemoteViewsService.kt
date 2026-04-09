package it.lagioiaproductions.nutrislot.widget

import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import it.lagioiaproductions.nutrislot.R
import it.lagioiaproductions.nutrislot.domain.model.MealSlotType

class MealCalendarWidgetRemoteViewsService : RemoteViewsService() {

    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return MealCalendarWidgetFactory(applicationContext)
    }
}

private class MealCalendarWidgetFactory(
    private val context: Context
) : RemoteViewsService.RemoteViewsFactory {
    private var items: List<MealCalendarWidgetItem> = emptyList()

    override fun onCreate() = Unit

    override fun onDataSetChanged() {
        items = MealCalendarWidgetSupport.loadItems(context)
    }

    override fun onDestroy() {
        items = emptyList()
    }

    override fun getCount(): Int = items.size

    override fun getViewAt(position: Int): RemoteViews? {
        val item = items.getOrNull(position) ?: return null

        val dayBackgroundRes = if (item.isToday) {
            R.drawable.widget_calendar_day_today_background
        } else {
            R.drawable.widget_calendar_day_background
        }
        val dayTextColor = context.getColor(
            if (item.isToday) {
                R.color.widget_day_today_text
            } else {
                R.color.widget_day_text
            }
        )
        val metaTextColor = context.getColor(
            if (item.isDimmed) {
                R.color.widget_text_muted
            } else {
                R.color.widget_text_secondary
            }
        )
        val mealTextColor = context.getColor(
            if (item.isDimmed) {
                R.color.widget_text_secondary
            } else {
                R.color.widget_text_primary
            }
        )

        return RemoteViews(context.packageName, R.layout.widget_meal_calendar_item).apply {
            setTextViewText(R.id.widget_day_label, item.dayLabel)
            setTextViewText(R.id.widget_date_label, item.dateLabel)
            setTextViewText(R.id.widget_meta_text, item.metaText)
            setTextViewText(R.id.widget_meal_text, item.mealText)

            setTextColor(R.id.widget_day_label, dayTextColor)
            setTextColor(R.id.widget_date_label, dayTextColor)
            setTextColor(R.id.widget_meta_text, metaTextColor)
            setTextColor(R.id.widget_meal_text, mealTextColor)

            setInt(R.id.widget_day_container, "setBackgroundResource", dayBackgroundRes)
            setInt(
                R.id.widget_meal_indicator,
                "setBackgroundResource",
                indicatorResFor(item.mealSlotType)
            )
            setOnClickFillInIntent(
                R.id.widget_item_root,
                Intent().putExtra(EXTRA_SLOT_ID, item.slotId)
            )
        }
    }

    override fun getLoadingView(): RemoteViews {
        return RemoteViews(context.packageName, R.layout.widget_meal_calendar_loading_item)
    }

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long {
        return items.getOrNull(position)?.stableId ?: position.toLong()
    }

    override fun hasStableIds(): Boolean = true

    private fun indicatorResFor(slotType: MealSlotType): Int {
        return when (slotType) {
            MealSlotType.BREAKFAST -> R.drawable.widget_meal_indicator_breakfast
            MealSlotType.MORNING_SNACK,
            MealSlotType.AFTERNOON_SNACK -> R.drawable.widget_meal_indicator_snack
            MealSlotType.LUNCH -> R.drawable.widget_meal_indicator_lunch
            MealSlotType.DINNER -> R.drawable.widget_meal_indicator_dinner
        }
    }

    private companion object {
        const val EXTRA_SLOT_ID = "slot_id"
    }
}
