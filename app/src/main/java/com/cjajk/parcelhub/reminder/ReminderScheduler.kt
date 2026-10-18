package com.cjajk.parcelhub.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.cjajk.parcelhub.data.PackageRecord
import com.cjajk.parcelhub.data.PackageStore

/** 单个快递到点提醒 + 每日统一提醒 */
object ReminderScheduler {
    const val ACTION_SINGLE_REMIND = "com.cjajk.parcelhub.SINGLE_REMIND"
    const val ACTION_DAILY_REMIND = "com.cjajk.parcelhub.DAILY_REMIND"
    const val EXTRA_RECORD_ID = "record_id"

    private const val SP = "parcel_hub"
    private const val SP_DAILY_HOUR = "daily_remind_hour"
    private const val SP_DAILY_MINUTE = "daily_remind_minute"

    private fun alarmManager(context: Context) =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    // ---------- 单个快递提醒 ----------

    fun schedule(context: Context, record: PackageRecord) {
        val time = record.remindAt ?: return
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_SINGLE_REMIND
            putExtra(EXTRA_RECORD_ID, record.id)
        }
        val pi = PendingIntent.getBroadcast(
            context, record.id.toInt(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager(context).setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pi)
    }

    fun cancel(context: Context, recordId: Long) {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_SINGLE_REMIND
            putExtra(EXTRA_RECORD_ID, recordId)
        }
        val pi = PendingIntent.getBroadcast(
            context, recordId.toInt(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager(context).cancel(pi)
    }

    // ---------- 每日统一提醒 ----------

    fun getDailyTime(context: Context): Pair<Int, Int>? {
        val sp = context.getSharedPreferences(SP, Context.MODE_PRIVATE)
        val hour = sp.getInt(SP_DAILY_HOUR, -1)
        val minute = sp.getInt(SP_DAILY_MINUTE, -1)
        return if (hour >= 0) hour to minute else null
    }

    fun setDailyTime(context: Context, hour: Int, minute: Int) {
        context.getSharedPreferences(SP, Context.MODE_PRIVATE).edit()
            .putInt(SP_DAILY_HOUR, hour)
            .putInt(SP_DAILY_MINUTE, minute)
            .apply()
        scheduleDaily(context, hour, minute)
    }

    fun clearDailyTime(context: Context) {
        context.getSharedPreferences(SP, Context.MODE_PRIVATE).edit()
            .putInt(SP_DAILY_HOUR, -1)
            .putInt(SP_DAILY_MINUTE, -1)
            .apply()
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_DAILY_REMIND
        }
        val pi = PendingIntent.getBroadcast(
            context, 9999, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager(context).cancel(pi)
    }

    private fun scheduleDaily(context: Context, hour: Int, minute: Int) {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_DAILY_REMIND
        }
        val pi = PendingIntent.getBroadcast(
            context, 9999, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val cal = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, hour)
            set(java.util.Calendar.MINUTE, minute)
            set(java.util.Calendar.SECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(java.util.Calendar.DAY_OF_YEAR, 1)
            }
        }
        alarmManager(context).setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            cal.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pi
        )
    }

    /** 重启后 / App 启动时恢复所有未触发的提醒 */
    fun rescheduleAll(context: Context) {
        val store = PackageStore(context)
        store.getAll().filter { !it.pickedUp && it.remindAt != null && it.remindAt > System.currentTimeMillis() }
            .forEach { schedule(context, it) }
        getDailyTime(context)?.let { (h, m) -> scheduleDaily(context, h, m) }
    }
}
