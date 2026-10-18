package com.cjajk.parcelhub.data

import org.json.JSONObject

data class PackageRecord(
    val id: Long = System.currentTimeMillis(),
    val code: String,
    val station: String = "",
    val carrier: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    var pickedUp: Boolean = false,
    val rawSms: String = "",
    val remindAt: Long? = null
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("code", code)
        put("station", station)
        put("carrier", carrier)
        put("timestamp", timestamp)
        put("pickedUp", pickedUp)
        put("rawSms", rawSms)
        if (remindAt != null) put("remindAt", remindAt)
    }

    companion object {
        fun fromJson(o: JSONObject): PackageRecord = PackageRecord(
            id = o.getLong("id"),
            code = o.getString("code"),
            station = o.optString("station", ""),
            carrier = o.optString("carrier", ""),
            timestamp = o.getLong("timestamp"),
            pickedUp = o.optBoolean("pickedUp", false),
            rawSms = o.optString("rawSms", ""),
            remindAt = if (o.isNull("remindAt")) null else o.optLong("remindAt")
        )
    }
}
