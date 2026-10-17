package com.cjajk.parcelhub.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast

object DeepLinks {

    fun openTaobaoIdentity(context: Context) {
        val h5 = "https://pages-fast.m.taobao.com/wow/z/uniapp/1100333/last-mile-fe/m-end-school-tab/home"
        val tbopen = "tbopen://m.taobao.com/tbopen/index.html?h5Url=${Uri.encode(h5)}"
        if (tryIntent(context, tbopen)) return
        if (tryIntent(context, "taobao://")) return
        fallbackToPackage(context, "com.taobao.taobao")
    }

    fun openPddPickup(context: Context) {
        val scheme = "pinduoduo://com.xunmeng.pinduoduo/psnl_goods_help.html?_t_module_name=assistant_pickup"
        if (tryIntent(context, scheme)) return
        if (tryIntent(context, "pinduoduo://com.xunmeng.pinduoduo/")) return
        fallbackToPackage(context, "com.xunmeng.pinduoduo")
    }

    fun openAlipayCainiao(context: Context) {
        val scheme = "alipays://platformapi/startapp?appId=20000067&url=${
            Uri.encode("https://market.m.taobao.com/app/cn-yz/multi-activity/authCode.html?bizEntry=ALIPAY_GUOGUO")
        }"
        if (tryIntent(context, scheme)) return
        fallbackToPackage(context, "com.eg.android.AlipayGphone")
    }

    fun openSf(context: Context) {
        if (tryIntent(context, "sf-express://")) return
        openWechatMiniProgram(context, "wxd4185d00bf7e08ac")
    }

    fun openJd(context: Context) {
        if (tryIntent(context, "openapp.jdmobile://")) return
        openWechatMiniProgram(context, "wx73247c7819d61796")
    }

    private fun openWechatMiniProgram(context: Context, appId: String) {
        val withPath = "weixin://dl/business/?appid=$appId&path=pages/index/index"
        if (tryIntent(context, withPath)) return
        val onlyAppId = "weixin://dl/business/?appid=$appId"
        if (tryIntent(context, onlyAppId)) return
        fallbackToPackage(context, "com.tencent.mm")
    }

    private fun tryIntent(context: Context, uri: String): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: ActivityNotFoundException) {
            false
        } catch (e: Exception) {
            false
        }
    }

    private fun fallbackToPackage(context: Context, pkg: String) {
        val intent = context.packageManager.getLaunchIntentForPackage(pkg)
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } else {
            Toast.makeText(context, "未安装对应 App", Toast.LENGTH_SHORT).show()
        }
    }
}
