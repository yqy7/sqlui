package io.github.yqy7.sqlui.viewmodel

import io.github.yqy7.sqlui.model.*
import io.github.yqy7.sqlui.model.service.DatabaseService
import io.github.yqy7.sqlui.util.AppScope
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * 连接管理 ViewModel。
 */
class ConnectionViewModel(private val databaseService: DatabaseService) {

    val connectionState = SimpleObjectProperty<UiState<ConnectionInfo>>(UiState.Idle)
    val currentConnectionName = SimpleStringProperty("未连接")
    val isConnected = SimpleObjectProperty(false)

    /** 最近的连接信息列表（用于快速重连） */
    val recentConnections: ObservableList<ConnectionInfo> = FXCollections.observableArrayList()

    init {
        // 监听 DatabaseService 的连接状态
        AppScope.scope.launch {
            databaseService.connectionState.collect { state ->
                javafx.application.Platform.runLater {
                    connectionState.set(state)
                    when (state) {
                        is UiState.Success -> {
                            currentConnectionName.set(state.data.displayName)
                            isConnected.set(true)
                            addToRecent(state.data)
                        }
                        is UiState.Idle -> {
                            currentConnectionName.set("未连接")
                            isConnected.set(false)
                        }
                        else -> {}
                    }
                }
            }
        }
    }

    fun openConnection(info: ConnectionInfo) {
        AppScope.scope.launch {
            val result = databaseService.openConnection(info)
            javafx.application.Platform.runLater {
                connectionState.set(result)
            }
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
            javafx.application.Platform.runLater {
                connectionState.set(result)
            }
        }
    }

    private fun addToRecent(info: ConnectionInfo) {
        recentConnections.removeIf { it.filePath == info.filePath && it.databaseType == info.databaseType }
        recentConnections.add(0, info)
        if (recentConnections.size > 10) {
            recentConnections.removeAt(recentConnections.size - 1)
        }
    }
}
