package com.cjajk.parcelhub.data

import android.content.Context
import org.json.JSONArray

class PackageStore(context: Context) {
    private val sp = context.getSharedPreferences("parcel_hub", Context.MODE_PRIVATE)
    private val lock = Any()

    fun getAll(): MutableList<PackageRecord> = synchronized(lock) {
        val raw = sp.getString("packages", "[]") ?: "[]"
        val arr = JSONArray(raw)
        val list = mutableListOf<PackageRecord>()
        for (i in 0 until arr.length()) {
            list.add(PackageRecord.fromJson(arr.getJSONObject(i)))
        }
        list
    }

    private fun save(list: List<PackageRecord>) = synchronized(lock) {
        val arr = JSONArray()
        list.forEach { arr.put(it.toJson()) }
        sp.edit().putString("packages", arr.toString()).apply()
    }

    fun add(record: PackageRecord) = synchronized(lock) {
        val list = getAll()
        // 只和"未取走"的同码同站记录去重；已取走的同码记录不吞新包裹
        val dup = list.any { !it.pickedUp && it.code == record.code && it.station == record.station }
        if (dup) return@synchronized
        // id 碰撞保护：Receiver 和 ViewModel 同时 new 可能撞 currentTimeMillis
        var r = record
        while (list.any { it.id == r.id }) {
            r = r.copy(id = r.id + 1)
        }
        list.add(0, r)
        save(list)
    }

    fun markPickedUp(id: Long, picked: Boolean) = synchronized(lock) {
        val list = getAll()
        val idx = list.indexOfFirst { it.id == id }
        if (idx >= 0) {
            list[idx] = list[idx].copy(pickedUp = picked)
            save(list)
        }
    }

    fun delete(id: Long) = synchronized(lock) {
        val list = getAll()
        list.removeAll { it.id == id }
        save(list)
    }
}
