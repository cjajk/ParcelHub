package com.cjajk.parcelhub.data

import java.util.regex.Pattern

object SmsParser {

    // 取件码后允许省略"为/是/："，直接跟码值；兼容 "取件码C5-1227"、"取件码123456"
    private val guidePattern = Pattern.compile(
        "(?:取件码|取件密码|提货码|领取码|密码)\\s*(?:为|是|:|：|就是)?\\s*([A-Za-z0-9]{1,4}-[A-Za-z0-9]{1,4}(?:-[A-Za-z0-9]{1,6})?|[A-Za-z0-9]{4,12})"
    )

    private val codePattern = Pattern.compile(
        "\\b([A-Za-z]?\\d{1,3}-[A-Za-z]?\\d{1,4}-[A-Za-z0-9]{1,6})\\b"
    )

    // 【】签名里的关键词到承运商的映射
    private val signToCarrier = listOf(
        "顺丰" to "顺丰速运",
        "中通" to "中通快递",
        "圆通" to "圆通速递",
        "韵达" to "韵达快递",
        "申通" to "申通快递",
        "百世" to "百世快递",
        "极兔" to "极兔速递",
        "京东" to "京东物流",
        "邮政" to "邮政快递",
        "EMS" to "EMS",
        "德邦" to "德邦快递",
        "丹鸟" to "丹鸟",
        "菜鸟" to "菜鸟驿站",
        "丰巢" to "丰巢柜",
        "妈妈驿站" to "妈妈驿站"
    )

    // 短缩写用词边界匹配，避免 EMS 命中 items、SF 命中链接
    private val shortCodePatterns = listOf(
        Pattern.compile("\\bSF\\b") to "顺丰速运",
        Pattern.compile("\\bEMS\\b") to "EMS",
        Pattern.compile("\\bJD\\b") to "京东物流"
    )

    private val stationKeywords = listOf(
        "菜鸟驿站", "丰巢", "妈妈驿站", "驿收发", "日日顺",
        "快递柜", "快递站", "便利店", "服务点"
    )

    fun parse(sender: String?, body: String): PackageRecord? {
        val text = body.trim()
        val carrier = detectCarrier(text)
        val looksExpress = carrier != null ||
                text.contains("取件") ||
                text.contains("快递") ||
                text.contains("包裹") ||
                stationKeywords.any { text.contains(it) }

        if (!looksExpress) return null

        var code: String? = null
        val m1 = guidePattern.matcher(text)
        if (m1.find()) code = m1.group(1)

        if (code == null) {
            val m2 = codePattern.matcher(text)
            if (m2.find()) code = m2.group(1)
        }

        if (code == null) return null
        val station = detectStation(text)

        return PackageRecord(
            code = code,
            station = station,
            carrier = carrier ?: "",
            rawSms = "[$sender] $text"
        )
    }

    private fun detectCarrier(text: String): String? {
        // 1. 优先匹配【】签名里的内容
        val sign = Pattern.compile("【(.+?)】").matcher(text)
        if (sign.find()) {
            val s = sign.group(1)
            signToCarrier.forEach { (key, name) ->
                if (s.contains(key)) return name
            }
        }
        // 2. 短缩写用词边界
        shortCodePatterns.forEach { (p, name) ->
            if (p.matcher(text).find()) return name
        }
        // 3. 中文关键词全文匹配
        signToCarrier.forEach { (key, name) ->
            if (text.contains(key)) return name
        }
        return null
    }

    private fun detectStation(text: String): String {
        stationKeywords.forEach { kw ->
            val idx = text.indexOf(kw)
            if (idx >= 0) {
                val start = maxOf(0, idx - 4)
                val end = minOf(text.length, idx + kw.length + 12)
                return text.substring(start, end).replace("\n", " ").trim()
            }
        }
        return ""
    }
}
