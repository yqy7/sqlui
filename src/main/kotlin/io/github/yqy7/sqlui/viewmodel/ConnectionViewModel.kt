package io.github.yqy7.sqlui.viewmodel

import io.github.yqy7.sqlui.model.*
import io.github.yqy7.sqlui.service.DatabaseService
import io.github.yqy7.sqlui.util.AppScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 连接管理 ViewModel。
 */
class ConnectionViewModel(private val databaseService: DatabaseService) {

    private val _connectionState = MutableStateFlow<UiState<ConnectionInfo>>(UiState.Idle)
    val connectionState: StateFlow<UiState<ConnectionInfo>> = _connectionState.asStateFlow()

    private val _currentConnectionName = MutableStateFlow("未连接")
    val currentConnectionName: StateFlow<String> = _currentConnectionName.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    /** 最近的连接信息列表（用于快速重连） */
    private val _recentConnections = MutableStateFlow<List<ConnectionInfo>>(emptyList())
    val recentConnections: StateFlow<List<ConnectionInfo>> = _recentConnections.asStateFlow()

    init {
        // 监听 DatabaseService 的连接状态
        AppScope.scope.launch {
            databaseService.connectionState.collect { state ->
                _connectionState.value = state
                when (state) {
                    is UiState.Success -> {
                        _currentConnectionName.value = state.data.displayName
                        _isConnected.value = true
                        addToRecent(state.data)
                    }
                    is UiState.Idle -> {
                        _currentConnectionName.value = "未连接"
                        _isConnected.value = false
                    }
                    else -> {}
                }
            }
        }
    }

    fun openConnection(info: ConnectionInfo) {
        AppScope.scope.launch {
            val result = databaseService.openConnection(info)
            _connectionState.value = result
        }
    }

    fun closeConnection() {
        AppScope.scope.launch {
            databaseService.closeConnection()
        }
    }

    fun createH2InMemory(name: String = "test") {
        AppScope.scope.launch {
            val result = databaseService.createH2InMemory(name)
            _connectionState.value = result
        }
    }

    private fun addToRecent(info: ConnectionInfo) {
        val current = _recentConnections.value.toMutableList()
        current.removeAll { it.filePath == info.filePath && it.databaseType == info.databaseType }
        current.add(0, info)
        if (current.size > 10) {
            current.removeAt(current.size - 1)
        }
        _recentConnections.value = current
    }
}
