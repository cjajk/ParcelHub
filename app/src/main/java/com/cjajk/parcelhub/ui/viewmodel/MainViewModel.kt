package com.cjajk.parcelhub.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cjajk.parcelhub.data.PackageRecord
import com.cjajk.parcelhub.data.PackageStore
import com.cjajk.parcelhub.reminder.ReminderScheduler
import com.cjajk.parcelhub.widget.PickupWidget
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val store = PackageStore(app)

    private val _packages = MutableStateFlow<List<PackageRecord>>(emptyList())
    val packages: StateFlow<List<PackageRecord>> = _packages

    init {
        refresh()
        ReminderScheduler.rescheduleAll(app)
    }

    fun refresh() {
        viewModelScope.launch {
            _packages.value = store.getAll().sortedByDescending { it.timestamp }
            PickupWidget.refresh(getApplication())
        }
    }

    fun togglePickedUp(record: PackageRecord) {
        store.markPickedUp(record.id, !record.pickedUp)
        // 已取走就取消提醒
        if (!record.pickedUp) ReminderScheduler.cancel(getApplication(), record.id)
        refresh()
    }

    fun delete(record: PackageRecord) {
        ReminderScheduler.cancel(getApplication(), record.id)
        store.delete(record.id)
        refresh()
    }

    fun addManual(code: String, station: String, carrier: String) {
        store.add(
            PackageRecord(
                code = code,
                station = station,
                carrier = carrier,
                rawSms = "手动添加"
            )
        )
        refresh()
    }

    fun setRemindAt(record: PackageRecord, timeMs: Long?) {
        val list = store.getAll()
        val idx = list.indexOfFirst { it.id == record.id }
        if (idx >= 0) {
            list[idx] = list[idx].copy(remindAt = timeMs)
            // 触发 save
            store.delete(record.id)
            store.add(list[idx])
            if (timeMs != null) {
                ReminderScheduler.schedule(getApplication(), list[idx])
            } else {
                ReminderScheduler.cancel(getApplication(), record.id)
            }
        }
        refresh()
    }
}
