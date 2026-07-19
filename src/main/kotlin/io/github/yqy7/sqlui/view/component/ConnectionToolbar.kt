package io.github.yqy7.sqlui.view.component

import io.github.yqy7.sqlui.viewmodel.ConnectionViewModel
import io.github.yqy7.sqlui.view.util.*
import javafx.scene.control.ToolBar

/**
 * 顶部连接工具栏。
 */
class ConnectionToolbar(private val viewModel: ConnectionViewModel) : ToolBar() {

    init {
        buildUI()
    }

    private fun buildUI() {
        button("新建连接") {
            val result = ConnectionDialog(viewModel).showAndWait()
            result.ifPresent { info -> viewModel.openConnection(info) }
        }

        button("关闭连接") {
            viewModel.closeConnection()
        }

        separator()

        // 连接状态标签
        val statusLabel = label("未连接")
        statusLabel.styleClass.add("status-label")

        // 绑定状态变化
        viewModel.currentConnectionName.addListener { _, _, name ->
            statusLabel.text = name
        }

        // 快捷键提示
        spacer()
        label("Ctrl+Enter 执行 | Ctrl+T 新建标签页")
    }
}
