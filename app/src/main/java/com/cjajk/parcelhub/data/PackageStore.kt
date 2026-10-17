package com.cjajk.parcelhub.data

import android.content.Context
import org.json.JSONArray

class PackageStore(context: Context) {
    private val sp = context.getSharedPreferences("parcel_hub", Context.MODE_PRIVATE)

    fun getAll(): MutableList<PackageRecord> {
        val raw = sp.getString("packages", "[]") ?: "[]"
        val arr = JSONArray(raw)
        val list = mutableListOf<PackageRecord>()
        for (i in 0 until arr.length()) {
            list.add(PackageRecord.fromJson(arr.getJSONObject(i)))
        }
        return list
    }

    fun save(list: List<PackageRecord>) {
        val arr = JSONArray()
        list.forEach { arr.put(it.toJson()) }
        sp.edit().putString("packages", arr.toString()).apply()
    }

    fun add(record: PackageRecord) {
        val list = getAll()
        if (list.none { it.code == record.code && it.station == record.station }) {
            list.add(0, record)
            save(list)
        }
    }

    fun markPickedUp(id: Long, picked: Boolean) {
        val list = getAll()
        val idx = list.indexOfFirst { it.id == id }
        if (idx >= 0) {
            list[idx] = list[idx].copy(pickedUp = picked)
            save(list)
        }
    }

    fun delete(id: Long) {
        val list = getAll()
        list.removeAll { it.id == id }
        save(list)
    }
}
