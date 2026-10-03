package com.example.revgfw.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.revgfw.data.AppRepository
import com.example.revgfw.data.AppAudit
import com.example.revgfw.data.ScanSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 主 ViewModel：统一管理"扫描结果 + 加载状态 + 选中应用"。
 * UI 只订阅 StateFlow，不做业务。
 */
class MainViewModel(private val repo: AppRepository) : ViewModel() {

    private val _summary = MutableStateFlow<ScanSummary?>(null)
    val summary: StateFlow<ScanSummary?> = _summary.asStateFlow()

    private val _scanning = MutableStateFlow(false)
    val scanning: StateFlow<Boolean> = _scanning.asStateFlow()

    private val _selected = MutableStateFlow<AppAudit?>(null)
    val selected: StateFlow<AppAudit?> = _selected.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun scan() {
        if (_scanning.value) return
        viewModelScope.launch {
            _scanning.value = true
            runCatching { repo.scanAll() }
                .onSuccess { _summary.value = it }
                .onFailure { _message.value = "扫描失败：${it.message}" }
            _scanning.value = false
        }
    }

    fun select(app: AppAudit?) { _selected.value = app }

    fun notify(msg: String) { _message.value = msg }

    fun consumeMessage() { _message.value = null }
}