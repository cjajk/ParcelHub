package com.cjajk.parcelhub.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cjajk.parcelhub.data.PackageRecord
import com.cjajk.parcelhub.data.PackageStore
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
    }

    fun refresh() {
        viewModelScope.launch {
            _packages.value = store.getAll().sortedByDescending { it.timestamp }
            PickupWidget.refresh(getApplication())
        }
    }

    fun togglePickedUp(record: PackageRecord) {
        store.markPickedUp(record.id, !record.pickedUp)
        refresh()
    }

    fun delete(record: PackageRecord) {
        store.delete(record.id)
        refresh()
    }

    fun addFake() {
        store.add(
            PackageRecord(
                code = "3-2-501",
                station = "菜鸟驿站·XX路店",
                carrier = "中通快递",
                rawSms = "测试数据"
            )
        )
        refresh()
    }
}
