package com.cjajk.parcelhub.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.cjajk.parcelhub.data.PackageStore
import com.cjajk.parcelhub.data.SmsParser

class SmsReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_PACKAGE_ARRIVED = "com.cjajk.parcelhub.PACKAGE_ARRIVED"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent?.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
        val sender = messages.firstOrNull()?.originatingAddress ?: "未知"
        val body = messages.joinToString("") { it.displayMessageBody ?: "" }
        Log.d("SmsReceiver", "短信 from=$sender body=$body")
        val record = SmsParser.parse(sender, body) ?: return
        PackageStore(context).add(record)
        context.sendBroadcast(Intent(ACTION_PACKAGE_ARRIVED).setPackage(context.packageName))
    }
}
