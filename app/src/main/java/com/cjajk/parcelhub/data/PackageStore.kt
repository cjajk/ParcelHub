package com.cjajk.parcelhub.data

import android.content.Context
import org.json.JSONArray

class PackageStore(context: Context) {
    private val sp = context.getSharedPreferences("parcel_hub", Context.MODE_PRIVATE)
    private val lock = Any()

    // 内存缓存：避免每次操作都从 SP 读全量 JSON
    @Volatile
    private var cache: MutableList<PackageRecord>? = null

    private fun load(): MutableList<PackageRecord> {
        cache?.let { return it }
        val raw = sp.getString("packages", "[]") ?: "[]"
        val arr = JSONArray(raw)
        val list = mutableListOf<PackageRecord>()
        for (i in 0 until arr.length()) {
            list.add(PackageRecord.fromJson(arr.getJSONObject(i)))
        }
        cache = list
        return list
    }

    private fun persist() {
        val list = cache ?: return
        val arr = JSONArray()
        list.forEach { arr.put(it.toJson()) }
        sp.edit().putString("packages", arr.toString()).apply()
    }

    fun getAll(): MutableList<PackageRecord> = synchronized(lock) {
        load().toMutableList()
    }

    fun add(record: PackageRecord) = synchronized(lock) {
        addAll(listOf(record))
    }

    /** 批量加入：只写一次磁盘 */
    fun addAll(records: List<PackageRecord>) = synchronized(lock) {
        val list = load()
        var changed = false
        records.forEach { record ->
            val dup = list.any { !it.pickedUp && it.code == record.code && it.station == record.station }
            if (dup) return@forEach
            var r = record
            while (list.any { it.id == r.id }) {
                r = r.copy(id = r.id + 1)
            }
            list.add(0, r)
            changed = true
        }
        if (changed) persist()
    }

    fun markPickedUp(id: Long, picked: Boolean) = synchronized(lock) {
        val list = load()
        val idx = list.indexOfFirst { it.id == id }
        if (idx >= 0) {
            list[idx] = list[idx].copy(pickedUp = picked)
            persist()
        }
    }

    fun delete(id: Long) = synchronized(lock) {
        val list = load()
        if (list.removeAll { it.id == id }) persist()
    }
}
