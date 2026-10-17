package com.cjajk.parcelhub.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import androidx.core.app.NotificationCompat
import com.cjajk.parcelhub.MainActivity
import com.cjajk.parcelhub.R
import com.cjajk.parcelhub.data.PackageStore
import com.cjajk.parcelhub.data.SmsParser
import com.cjajk.parcelhub.widget.PickupWidget

class SmsReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_PACKAGE_ARRIVED = "com.cjajk.parcelhub.PACKAGE_ARRIVED"
        const val CHANNEL_ID = "parcel_new"
        const val NOTIF_ID = 1001
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent?.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
        val sender = messages.firstOrNull()?.originatingAddress ?: "未知"
        val body = messages.joinToString("\n") { it.displayMessageBody ?: "" }

        Log.d("SmsReceiver", "短信 from=$sender body=$body")

        val record = SmsParser.parse(sender, body) ?: return

        PackageStore(context).add(record)
        PickupWidget.refresh(context)
        context.sendBroadcast(Intent(ACTION_PACKAGE_ARRIVED).setPackage(context.packageName))
        showNotification(context, record.code, record.station)
    }

    private fun showNotification(context: Context, code: String, station: String) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(CHANNEL_ID, "新取件码", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "收到取件短信时通知"
        }
        nm.createNotificationChannel(channel)

        val openIntent = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        val notif = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("取件码：$code")
            .setContentText(station.ifBlank { "请取快递" })
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(openIntent)
            .setAutoCancel(true)
            .build()

        nm.notify(NOTIF_ID, notif)
    }
}
