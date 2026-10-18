package com.cjajk.parcelhub.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** 手机重启后恢复闹钟 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent?.action != Intent.ACTION_BOOT_COMPLETED) return
        ReminderScheduler.rescheduleAll(context)
    }
}
