package com.cjajk.parcelhub.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.*
import java.util.concurrent.TimeUnit

data class ExpressResult(
    val com: String,
    val nu: String,
    val state: Int,
    val latestContext: String,
    val latestTime: String,
    val estimatedDays: Int?
) {
    val stateText: String
        get() = when (state) {
            0 -> "运输中"
            1 -> "已揽收"
            2 -> "问题件"
            3 -> "已签收"
            4 -> "已退签"
            5 -> "派件中"
            6 -> "已退回"
            else -> "未知"
        }
}

object ExpressQuery {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun query(nu: String): ExpressResult? = withContext(Dispatchers.IO) {
        try {
            val url = "https://www.kuaidi100.com/query?type=auto&postid=${URLEncoder.encode(nu, "UTF-8")}&temp=${System.currentTimeMillis() / 1000}"
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 12) AppleWebKit/537.36 Chrome/120.0.0.0 Mobile Safari/537.36")
                .header("Referer", "https://www.kuaidi100.com/")
                .build()
            client.newCall(req).execute().use { resp ->
                val body = resp.body?.string() ?: return@withContext null
                val json = JSONObject(body)
                if (json.optString("status") != "200") return@withContext null
                val dataArr = json.optJSONArray("data") ?: return@withContext null
                if (dataArr.length() == 0) return@withContext null
                val latest = dataArr.getJSONObject(0)
                val state = json.optInt("state", 0)
                val comCode = json.optString("com", "")
                val comName = comNameMap[comCode] ?: comCode
                val estimatedDays = estimateDays(state, dataArr)
                ExpressResult(
                    com = comName,
                    nu = nu,
                    state = state,
                    latestContext = latest.optString("context", ""),
                    latestTime = latest.optString("time", ""),
                    estimatedDays = estimatedDays
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun estimateDays(state: Int, dataArr: org.json.JSONArray): Int? {
        return when (state) {
            3 -> 0
            5 -> 1
            1, 0 -> {
                try {
                    val fmt = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                    val first = dataArr.getJSONObject(dataArr.length() - 1).optString("time")
                    val last = dataArr.getJSONObject(0).optString("time")
                    val t1 = fmt.parse(first)?.time ?: return null
                    val t2 = fmt.parse(last)?.time ?: return null
                    val elapsedDays = ((t2 - t1) / (1000 * 60 * 60 * 24)).toInt()
                    when {
                        elapsedDays >= 3 -> 1
                        elapsedDays >= 1 -> 2
                        else -> 3
                    }
                } catch (e: Exception) { null }
            }
            else -> null
        }
    }

    private val comNameMap = mapOf(
        "shunfeng" to "顺丰",
        "yuantong" to "圆通",
        "zhongtong" to "中通",
        "yunda" to "韵达",
        "shentong" to "申通",
        "ems" to "EMS",
        "youzhengguonei" to "邮政包裹",
        "jd" to "京东物流",
        "jdwl" to "京东物流",
        "best" to "百世快递",
        "debang" to "德邦",
        "tiantian" to "天天快递",
        "annengwuliu" to "安能物流",
        "sue" to "苏宁物流",
        "cainiao" to "菜鸟裹裹"
    )
}
