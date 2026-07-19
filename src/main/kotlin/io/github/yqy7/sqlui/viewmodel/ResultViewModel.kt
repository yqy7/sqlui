package io.github.yqy7.sqlui.viewmodel

import io.github.yqy7.sqlui.model.service.DatabaseService
import javafx.beans.property.*

/**
 * 全局结果/状态 ViewModel —— 管理底部状态栏信息。
 */
class ResultViewModel(private val databaseService: DatabaseService) {

    /** 连接 URL 状态文本 */
    val connectionStatusText = SimpleStringProperty("未连接")

    /** 当前激活的编辑器标签页标题 */
    val activeTabTitle = SimpleStringProperty("")

    val isConnected: Boolean get() = databaseService.isConnected

    fun updateConnectionStatus(url: String?) {
        connectionStatusText.set(if (url != null) "已连接: $url" else "未连接")
    }
}
