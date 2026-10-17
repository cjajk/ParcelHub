package com.cjajk.parcelhub.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

data class TraceItem(val time: String, val context: String)

data class ExpressResult(
    val com: String,
    val comCode: String,
    val nu: String,
    val state: Int,
    val latestContext: String,
    val latestTime: String,
    val traces: List<TraceItem> = emptyList(),
    val estimatedDays: Int? = null
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
            7 -> "转单"
            8 -> "清关中"
            14 -> "拒签"
            else -> "未知"
        }
}

sealed class ExpressQueryException(message: String) : Exception(message) {
    object InvalidNumber : ExpressQueryException("快递单号格式不正确")
    object NeedSelectCompany : ExpressQueryException("无法识别快递公司，请手动选择")
    object NeedPhone : ExpressQueryException("顺丰/京东隐私单号需填写手机号后4位")
    object NotFound : ExpressQueryException("暂无物流轨迹，请确认单号")
    class ServerError(msg: String) : ExpressQueryException(msg)
    class NetworkError(cause: Throwable? = null) : ExpressQueryException("网络连接失败")
}

val COM_LIST = listOf(
    "shunfeng" to "顺丰",
    "yuantong" to "圆通",
    "zhongtong" to "中通",
    "yunda" to "韵达",
    "shentong" to "申通",
    "jtexpress" to "极兔速递",
    "jd" to "京东物流",
    "ems" to "EMS",
    "youzhengguonei" to "邮政包裹",
    "debangwuliu" to "德邦",
    "annengwuliu" to "安能物流",
    "huitongkuaidi" to "百世快递"
)

private val COM_NAME = COM_LIST.toMap()

val CARRIER_NAME_TO_CODE = mapOf(
    "顺丰" to "shunfeng", "圆通" to "yuantong", "中通" to "zhongtong",
    "韵达" to "yunda", "申通" to "shentong", "极兔" to "jtexpress",
    "京东" to "jd", "EMS" to "ems", "邮政" to "youzhengguonei",
    "德邦" to "debangwuliu", "安能" to "annengwuliu", "百世" to "huitongkuaidi"
)

fun guessComCode(nu: String): String? = when {
    nu.startsWith("SF") -> "shunfeng"
    nu.startsWith("YT") -> "yuantong"
    nu.startsWith("JT") -> "jtexpress"
    nu.startsWith("JD") -> "jd"
    nu.matches(Regex("9[58]\\d{11}")) -> "youzhengguonei"
    else -> null
}

object ExpressQuery {
    private const val URL = "https://poll.kuaidi100.com/poll/query.do"
    private const val CUSTOMER = "626FD65B46E29ACE3C1DEAC8F7146C85"
    private const val KEY = "nWwfDOnd3418"

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun query(nu: String, comCode: String?, phoneTail: String? = null): ExpressResult =
        withContext(Dispatchers.IO) {
            val cleanNu = nu.trim().replace(Regex("\\s"), "").uppercase()
            if (!cleanNu.matches(Regex("[A-Za-z0-9]{8,30}"))) throw ExpressQueryException.InvalidNumber

            val code = comCode ?: guessComCode(cleanNu) ?: throw ExpressQueryException.NeedSelectCompany

            val param = JSONObject().apply {
                put("com", code)
                put("num", cleanNu)
                put("resultv2", "4")
                if (!phoneTail.isNullOrBlank()) put("phone", phoneTail.trim())
            }.toString()

            val sign = md5(param + KEY + CUSTOMER)

            val body = FormBody.Builder()
                .add("customer", CUSTOMER)
                .add("sign", sign)
                .add("param", param)
                .build()

            val req = Request.Builder()
                .url(URL)
                .post(body)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .build()

            val resp = client.newCall(req).execute()
            val text = resp.body?.string() ?: throw ExpressQueryException.NetworkError()
            val json = JSONObject(text)

            if (json.optString("status") != "200") {
                val msg = json.optString("message", "查询失败")
                throw when {
                    msg.contains("手机") || msg.contains("phone") -> ExpressQueryException.NeedPhone
                    msg.contains("无结果") || msg.contains("查无") -> ExpressQueryException.NotFound
                    else -> ExpressQueryException.ServerError(msg)
                }
            }

            parseResult(cleanNu, code, json)
        }

    private fun parseResult(nu: String, comCode: String, json: JSONObject): ExpressResult {
        val arr = json.getJSONArray("data")
        val traces = buildList {
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                add(TraceItem(o.optString("time", ""), o.optString("context", "")))
            }
        }
        val latest = traces.firstOrNull() ?: TraceItem("", "")
        val state = json.optInt("state", 0)
        return ExpressResult(
            com = COM_NAME[comCode] ?: comCode,
            comCode = comCode,
            nu = nu,
            state = state,
            latestContext = latest.context,
            latestTime = latest.time,
            traces = traces,
            estimatedDays = estimateDays(state, arr)
        )
    }

    private fun estimateDays(state: Int, dataArr: org.json.JSONArray): Int? = when (state) {
        3 -> 0
        5 -> 1
        1, 0 -> try {
            val fmt = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US)
            val first = dataArr.getJSONObject(dataArr.length() - 1).optString("time")
            val last = dataArr.getJSONObject(0).optString("time")
            val t1 = fmt.parse(first)?.time ?: return null
            val t2 = fmt.parse(last)?.time ?: return null
            val elapsedDays = ((t2 - t1) / (1000L * 60 * 60 * 24)).toInt()
            when {
                elapsedDays >= 3 -> 1
                elapsedDays >= 1 -> 2
                else -> 3
            }
        } catch (e: Exception) { null }
        else -> null
    }

    private fun md5(s: String): String =
        MessageDigest.getInstance("MD5")
            .digest(s.toByteArray())
            .joinToString("") { "%02X".format(it) }
}
