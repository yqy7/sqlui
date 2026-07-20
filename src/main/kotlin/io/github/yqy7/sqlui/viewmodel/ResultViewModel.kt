package io.github.yqy7.sqlui.viewmodel

import io.github.yqy7.sqlui.service.DatabaseService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 全局结果/状态 ViewModel —— 管理底部状态栏信息。
 */
class ResultViewModel(private val databaseService: DatabaseService) {

    private val _connectionStatusText = MutableStateFlow("未连接")
    val connectionStatusText: StateFlow<String> = _connectionStatusText.asStateFlow()

    private val _activeTabTitle = MutableStateFlow("")
    val activeTabTitle: StateFlow<String> = _activeTabTitle.asStateFlow()

    val isConnected: Boolean get() = databaseService.isConnected

    fun updateConnectionStatus(url: String?) {
        _connectionStatusText.value = if (url != null) "已连接: $url" else "未连接"
    }

    fun updateActiveTabTitle(title: String) {
        _activeTabTitle.value = title
    }
}
