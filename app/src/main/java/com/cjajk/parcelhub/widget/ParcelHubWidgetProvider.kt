package com.cjajk.parcelhub.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.cjajk.parcelhub.R
import com.cjajk.parcelhub.util.DeepLinks

class ParcelHubWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_OPEN_PLATFORM = "com.cjajk.parcelhub.OPEN_PLATFORM"
        const val EXTRA_PLATFORM = "platform"

        const val PLATFORM_CAINIAO = "cainiao"
        const val PLATFORM_TAOBAO = "taobao"
        const val PLATFORM_PDD = "pdd"
        const val PLATFORM_SF = "sf"
        const val PLATFORM_JD = "jd"
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_quick_entry)

            val cainiaoIntent = buildOpenIntent(context, PLATFORM_CAINIAO, appWidgetId)
            views.setOnClickPendingIntent(R.id.widget_cainiao, cainiaoIntent)

            val taobaoIntent = buildOpenIntent(context, PLATFORM_TAOBAO, appWidgetId)
            views.setOnClickPendingIntent(R.id.widget_taobao, taobaoIntent)

            val pddIntent = buildOpenIntent(context, PLATFORM_PDD, appWidgetId)
            views.setOnClickPendingIntent(R.id.widget_pdd, pddIntent)

            val sfIntent = buildOpenIntent(context, PLATFORM_SF, appWidgetId)
            views.setOnClickPendingIntent(R.id.widget_sf, sfIntent)

            val jdIntent = buildOpenIntent(context, PLATFORM_JD, appWidgetId)
            views.setOnClickPendingIntent(R.id.widget_jd, jdIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    private fun buildOpenIntent(context: Context, platform: String, widgetId: Int): PendingIntent {
        val intent = Intent(context, ParcelHubWidgetProvider::class.java).apply {
            action = ACTION_OPEN_PLATFORM
            putExtra(EXTRA_PLATFORM, platform)
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
        }
        return PendingIntent.getBroadcast(
            context,
            platform.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_OPEN_PLATFORM) {
            val platform = intent.getStringExtra(EXTRA_PLATFORM) ?: return
            when (platform) {
                PLATFORM_CAINIAO -> DeepLinks.openAlipayCainiao(context)
                PLATFORM_TAOBAO -> DeepLinks.openTaobaoIdentity(context)
                PLATFORM_PDD -> DeepLinks.openPddPickup(context)
                PLATFORM_SF -> DeepLinks.openSf(context)
                PLATFORM_JD -> DeepLinks.openJd(context)
            }
        }
    }
}