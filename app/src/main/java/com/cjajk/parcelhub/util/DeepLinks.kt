package com.cjajk.parcelhub.util

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast

object DeepLinks {

    private const val TAG = "DeepLinks"

    /** 淘宝身份码 */
    fun openTaobaoIdentity(context: Context) {
        val pkg = "com.taobao.taobao"
        val h5 = "https://pages-fast.m.taobao.com/wow/z/uniapp/1011717/last-mile-fe/end-collect-platform/identity-code?x-ssr=true"
        val uris = listOf(
            "taobao://m.taobao.com/tbopen/index.html?h5Url=${Uri.encode(h5)}",
            "tbopen://m.taobao.com/tbopen/index.html?h5Url=${Uri.encode(h5)}"
        )
        for (u in uris) {
            if (tryOpen(context, u, pkg)) return
        }
        if (tryOpen(context, h5, pkg)) return
        fallbackToPackage(context, pkg, "淘宝")
    }

    /** 淘宝待取快递 */
    fun openTaobaoWaiting(context: Context) {
        val pkg = "com.taobao.taobao"
        val h5 = "https://pages-fast.m.taobao.com/wow/z/uniapp/1100333/last-mile-fe/m-end-school-tab/home"
        val uris = listOf(
            "taobao://m.taobao.com/tbopen/index.html?h5Url=${Uri.encode(h5)}",
            "tbopen://m.taobao.com/tbopen/index.html?h5Url=${Uri.encode(h5)}"
        )
        for (u in uris) {
            if (tryOpen(context, u, pkg)) return
        }
        if (tryOpen(context, h5, pkg)) return
        fallbackToPackage(context, pkg, "淘宝")
    }

    /** 拼多多身份码 */
    fun openPddPickup(context: Context) {
        val pkg = "com.xunmeng.pinduoduo"
        val h5 = "https://m.pinduoduo.net/mdkd/package?entry_source=18&extra_params=from_wx_jump%3D1&p_channel=0"
        val uris = listOf(
            "pinduoduo://com.xunmeng.pinduoduo/mdkd/package?tab=ID_CODE&entry_source=11&refer_page_name=login&refer_page_sn=10169",
            "pinduoduo://com.xunmeng.pinduoduo/mdkd/package?tab=ID_CODE&entry_source=11",
            "pinduoduo://com.xunmeng.pinduoduo/mdkd/package"
        )
        for (u in uris) {
            if (tryOpen(context, u, pkg)) return
        }
        if (tryOpen(context, h5, pkg)) return
        fallbackToPackage(context, pkg, "拼多多")
    }

    /** 拼多多待取快递 */
    fun openPddWaiting(context: Context) {
        val pkg = "com.xunmeng.pinduoduo"
        val h5 = "https://m.pinduoduo.net/mdkd/package?entry_source=18&extra_params=from_wx_jump%3D1&p_channel=0"
        val uris = listOf(
            "pinduoduo://com.xunmeng.pinduoduo/mdkd/package"
        )
        for (u in uris) {
            if (tryOpen(context, u, pkg)) return
        }
        if (tryOpen(context, h5, pkg)) return
        fallbackToPackage(context, pkg, "拼多多")
    }

    /** 菜鸟身份码（直接跳支付宝内菜鸟） */
    fun openAlipayCainiao(context: Context) {
        val alipayPkg = "com.eg.android.AlipayGphone"
        val web = "https://market.m.taobao.com/app/cn-yz/multi-activity/authCode.html?bizEntry=ALIPAY_GUOGUO"
        if (tryOpen(context, web, alipayPkg)) return
        if (tryOpenInstalledApp(context, alipayPkg)) {
            Toast.makeText(context, "已打开支付宝，请在小程序中找到菜鸟身份码", Toast.LENGTH_LONG).show()
            return
        }
        Toast.makeText(context, "未安装支付宝", Toast.LENGTH_SHORT).show()
    }

    /** 顺丰 */
    fun openSf(context: Context) {
        val pkg = "com.sf.activity"
        val pkg2 = "com.sfexpress.activity"
        val uris = listOf(
            "sf-express://",
            "sfexpress://"
        )
        for (u in uris) {
            if (tryOpen(context, u, pkg)) return
            if (tryOpen(context, u, pkg2)) return
        }
        if (tryOpenInstalledApp(context, pkg)) return
        if (tryOpenInstalledApp(context, pkg2)) return
        try {
            val weixin = Intent(Intent.ACTION_VIEW, Uri.parse("weixin://")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(weixin)
            Toast.makeText(context, "已打开微信，请搜索「顺丰速运+」小程序", Toast.LENGTH_LONG).show()
        } catch (_: Exception) {
            Toast.makeText(context, "未安装顺丰 App 或微信", Toast.LENGTH_SHORT).show()
        }
    }

    /** 京东待取快递 */
    fun openJd(context: Context) {
        val pkg = "com.jingdong.app.mall"
        val web = "https://order.jd.com/center/list.action"
        val jdParams = Uri.encode("{\"category\":\"jump\",\"des\":\"orderlist\"}")
        val uris = listOf(
            "openjd://virtual?params=$jdParams"
        )
        for (u in uris) {
            if (tryOpen(context, u, pkg)) return
        }
        if (tryOpen(context, web, pkg)) return
        fallbackToPackage(context, pkg, "京东")
    }

    private fun tryOpen(context: Context, uriStr: String, pkg: String?): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uriStr)).apply {
                if (pkg != null) setPackage(pkg)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                if (context !is android.app.Activity) {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
            val resolved: ComponentName? = intent.resolveActivity(context.packageManager)
            if (resolved == null) {
                Log.i(TAG, "No activity resolved for $uriStr with package $pkg")
                return false
            }
            context.startActivity(intent)
            Log.i(TAG, "Opened $uriStr with ${resolved.flattenToShortString()}")
            true
        } catch (e: ActivityNotFoundException) {
            Log.w(TAG, "Activity not found for $uriStr", e)
            false
        } catch (e: SecurityException) {
            Log.w(TAG, "Security exception for $uriStr", e)
            false
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "Illegal argument for $uriStr", e)
            false
        }
    }

    private fun tryOpenInstalledApp(context: Context, pkg: String): Boolean {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(pkg) ?: return false
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        if (context !is android.app.Activity) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(launchIntent)
            true
        } catch (e: Exception) {
            Log.w(TAG, "Could not open app $pkg", e)
            false
        }
    }

    private fun fallbackToPackage(context: Context, pkg: String, name: String) {
        if (tryOpenInstalledApp(context, pkg)) return
        Toast.makeText(context, "未安装$name App", Toast.LENGTH_SHORT).show()
    }
}