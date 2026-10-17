package com.cjajk.parcelhub.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.cjajk.parcelhub.MainActivity
import com.cjajk.parcelhub.R
import com.cjajk.parcelhub.data.PackageStore

class PickupWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        manager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val store = PackageStore(context)
        val latest = store.getAll().firstOrNull { !it.pickedUp }

        for (id in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_pickup)
            views.setTextViewText(R.id.widget_code, latest?.code ?: "--")
            views.setTextViewText(R.id.widget_station, latest?.station ?: "没有待取快递")

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            val pi = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_code, pi)

            manager.updateAppWidget(id, views)
        }
    }

    companion object {
        fun refresh(context: Context) {
            val mgr = AppWidgetManager.getInstance(context)
            val ids = mgr.getAppWidgetIds(ComponentName(context, PickupWidget::class.java))
            val intent = Intent(context, PickupWidget::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            }
            context.sendBroadcast(intent)
        }
    }
}
