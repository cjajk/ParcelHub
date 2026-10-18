package com.cjajk.parcelhub.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.cjajk.parcelhub.MainActivity
import com.cjajk.parcelhub.R
import com.cjajk.parcelhub.data.PackageStore

class ReminderReceiver : BroadcastReceiver() {
    companion object {
        const val CHANNEL_ID = "parcel_remind"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent?.action == null) return
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(CHANNEL_ID, "快递提醒", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "到点提醒取快递"
        }
        nm.createNotificationChannel(channel)

        val openIntent = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        when (intent.action) {
            ReminderScheduler.ACTION_SINGLE_REMIND -> {
                val recordId = intent.getLongExtra(ReminderScheduler.EXTRA_RECORD_ID, -1L)
                val record = PackageStore(context).getAll().firstOrNull { it.id == recordId }
                    ?: return
                val notif = NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle("该取快递了：${record.code}")
                    .setContentText(record.station.ifBlank { record.carrier.ifBlank { "" } })
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setContentIntent(openIntent)
                    .setAutoCancel(true)
                    .build()
                nm.notify(recordId.toInt(), notif)
            }
            ReminderScheduler.ACTION_DAILY_REMIND -> {
                val waiting = PackageStore(context).getAll().filter { !it.pickedUp }
                if (waiting.isEmpty()) return
                val notif = NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle("你有 ${waiting.size} 个待取快递")
                    .setContentText(waiting.take(3).joinToString("、") { it.code })
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setContentIntent(openIntent)
                    .setAutoCancel(true)
                    .build()
                nm.notify(9999, notif)
            }
        }
    }
}
