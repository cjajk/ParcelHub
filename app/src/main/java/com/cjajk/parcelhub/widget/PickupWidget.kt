package com.cjajk.parcelhub.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.Toast
import com.cjajk.parcelhub.MainActivity
import com.cjajk.parcelhub.R
import com.cjajk.parcelhub.data.PackageStore

class PickupWidget : AppWidgetProvider() {

    companion object {
        const val ACTION_COPY_CODE = "com.cjajk.parcelhub.widget.COPY_CODE"
        const val EXTRA_CODE = "extra_code"

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

    override fun onUpdate(
        context: Context,
        manager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val store = PackageStore(context)
        val waiting = store.getAll().filter { !it.pickedUp }.sortedByDescending { it.timestamp }
        val latest = waiting.firstOrNull()

        for (id in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_pickup)
            views.setTextViewText(R.id.widget_title, "待取 ${waiting.size}")
            views.setTextViewText(R.id.widget_code, latest?.code ?: "--")
            views.setTextViewText(R.id.widget_station, latest?.station ?: "没有待取快递")

            // 有待取：点码复制；无待取：点码打开 App
            if (latest != null) {
                val copyIntent = Intent(context, PickupWidget::class.java).apply {
                    action = ACTION_COPY_CODE
                    putExtra(EXTRA_CODE, latest.code)
                }
                val copyPi = PendingIntent.getBroadcast(
                    context, latest.code.hashCode(), copyIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_code, copyPi)
            } else {
                val openIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                val pi = PendingIntent.getActivity(
                    context, 0, openIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_code, pi)
            }

            manager.updateAppWidget(id, views)
        }
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        super.onReceive(context, intent)
        if (context == null || intent?.action != ACTION_COPY_CODE) return
        val code = intent.getStringExtra(EXTRA_CODE) ?: return
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("取件码", code))
        Toast.makeText(context, "已复制 $code", Toast.LENGTH_SHORT).show()
    }
}
