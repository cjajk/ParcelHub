package com.cjajk.parcelhub.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast

object DeepLinks {

    private const val TAG = "DeepLinks"

    fun openTaobaoIdentity(context: Context) {
        val pkg = "com.taobao.taobao"
        val h5 = "https://pages-fast.m.taobao.com/wow/z/uniapp/1100333/last-mile-fe/m-end-school-tab/home"
        val schemes = listOf(
            "tbopen://m.taobao.com/tbopen/index.html?h5Url=${Uri.encode(h5)}",
            "taobao://"
        )
        for (s in schemes) {
            if (tryIntent(context, s)) return
        }
        try {
            val i = Intent(Intent.ACTION_VIEW, Uri.parse(h5)).apply {
                setClassName(pkg, "com.taobao.browser.BrowserActivity")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(i)
            return
        } catch (_: Exception) {}
        fallbackToPackage(context, pkg, "淘宝")
    }

    /** 淘宝待取快递列表 */
    fun openTaobaoWaiting(context: Context) {
        // 复用身份码页，里面有待取列表入口
        openTaobaoIdentity(context)
    }

    fun openPddPickup(context: Context) {
        val pkg = "com.xunmeng.pinduoduo"
        val schemes = listOf(
            "pinduoduo://com.xunmeng.pinduoduo/mdkd/package?tab=ID_CODE&entry_source=11",
            "pinduoduo://com.xunmeng.pinduoduo/mdkd/package",
            "pinduoduo://com.xunmeng.pinduoduo/",
            "pinduoduo://"
        )
        for (s in schemes) {
            if (tryIntent(context, s)) return
        }
        fallbackToPackage(context, pkg, "拼多多")
    }

    /** 拼多多待取包裹列表 */
    fun openPddWaiting(context: Context) {
        val pkg = "com.xunmeng.pinduoduo"
        val schemes = listOf(
            "pinduoduo://com.xunmeng.pinduoduo/mdkd/package?tab=UNCLAIMED",
            "pinduoduo://com.xunmeng.pinduoduo/mdkd/package"
        )
        for (s in schemes) {
            if (tryIntent(context, s)) return
        }
        fallbackToPackage(context, pkg, "拼多多")
    }

    fun openAlipayCainiao(context: Context) {
        val pkg = "com.eg.android.AlipayGphone"
        val authUrl = Uri.encode(
            "https://market.m.taobao.com/app/cn-yz/multi-activity/authCode.html?bizEntry=ALIPAY_GUOGUO"
        )
        val schemes = listOf(
            "alipays://platformapi/startapp?appId=20000067&url=$authUrl",
            "alipays://platformapi/startapp?saId=2021001141626787"
        )
        for (s in schemes) {
            if (tryIntent(context, s)) return
        }
        fallbackToPackage(context, pkg, "支付宝")
    }

    fun openSf(context: Context) {
        val schemes = listOf(
            "sf-express://",
            "sfexpress://"
        )
        for (s in schemes) {
            if (tryIntent(context, s)) return
        }
        val intent = context.packageManager.getLaunchIntentForPackage("com.sf.activity")
            ?: context.packageManager.getLaunchIntentForPackage("com.sfexpress.activity")
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } else {
            // 未装顺丰 App，提示微信小程序
            try {
                val weixin = Intent(Intent.ACTION_VIEW, Uri.parse("weixin://")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(weixin)
                Toast.makeText(context, "已打开微信，请搜索「顺丰速运+」小程序查看身份码", Toast.LENGTH_LONG).show()
            } catch (_: Exception) {
                Toast.makeText(context, "未安装顺丰 App 或微信", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun openJd(context: Context) {
        val schemes = listOf(
            "openapp.jdmobile://virtual?params=%7B%22desc%22%3A%22orderlist%22%7D",
            "openapp.jdmobile://",
            "openapp.jdmobile://virtual?params=%7B%22desc%22%3A%22jd%22%7D"
        )
        for (s in schemes) {
            if (tryIntent(context, s)) return
        }
        val intent = context.packageManager.getLaunchIntentForPackage("com.jingdong.app.mall")
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } else {
            Toast.makeText(context, "未安装京东 App", Toast.LENGTH_SHORT).show()
        }
    }

    private fun tryIntent(context: Context, uri: String): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (_: ActivityNotFoundException) {
            false
        } catch (e: Exception) {
            Log.e(TAG, "error launching $uri", e)
            false
        }
    }

    private fun fallbackToPackage(context: Context, pkg: String, name: String) {
        val intent = context.packageManager.getLaunchIntentForPackage(pkg)
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } else {
            Toast.makeText(context, "未安装$name App", Toast.LENGTH_SHORT).show()
        }
    }
}
