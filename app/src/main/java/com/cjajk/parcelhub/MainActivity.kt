package com.cjajk.parcelhub

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Telephony
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cjajk.parcelhub.data.PackageStore
import com.cjajk.parcelhub.data.SmsParser
import com.cjajk.parcelhub.ui.screen.MainScreen
import com.cjajk.parcelhub.ui.theme.ParcelHubTheme
import com.cjajk.parcelhub.ui.viewmodel.MainViewModel
import kotlin.concurrent.thread

class MainActivity : ComponentActivity() {

    private val permLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if (result[Manifest.permission.READ_SMS] == true) {
            backscanSmsIfNeeded()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val perms = mutableListOf(
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_SMS
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perms.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        val need = perms.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (need.isNotEmpty()) {
            permLauncher.launch(need.toTypedArray())
        } else {
            backscanSmsIfNeeded()
        }

        setContent {
            ParcelHubTheme {
                val vm: MainViewModel = viewModel()
                MainScreen(vm)
            }
        }
    }

    /** 首次启动回扫最近 7 天的取件短信，只跑一次 */
    private fun backscanSmsIfNeeded() {
        val sp = getSharedPreferences("parcel_hub", Context.MODE_PRIVATE)
        if (sp.getBoolean("sms_backscanned", false)) return
        thread {
            var imported = 0
            try {
                val weekAgo = System.currentTimeMillis() - 7L * 24 * 3600 * 1000
                val cursor = contentResolver.query(
                    Telephony.Sms.CONTENT_URI,
                    arrayOf(Telephony.Sms.ADDRESS, Telephony.Sms.BODY),
                    "${Telephony.Sms.DATE} > ?",
                    arrayOf(weekAgo.toString()),
                    "${Telephony.Sms.DATE} DESC"
                )
                cursor?.use { c ->
                    val addrIdx = c.getColumnIndex(Telephony.Sms.ADDRESS)
                    val bodyIdx = c.getColumnIndex(Telephony.Sms.BODY)
                    while (c.moveToNext()) {
                        val sender = if (addrIdx >= 0) c.getString(addrIdx) ?: "" else ""
                        val body = if (bodyIdx >= 0) c.getString(bodyIdx) ?: "" else ""
                        if (body.isBlank()) continue
                        val record = SmsParser.parse(sender, body) ?: continue
                        PackageStore(this@MainActivity).add(record)
                        imported++
                    }
                }
            } catch (_: Exception) {
            } finally {
                sp.edit().putBoolean("sms_backscanned", true).apply()
            }
            if (imported > 0) {
                runOnUiThread {
                    Toast.makeText(this, "已回扫最近7天短信，导入 $imported 条", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
