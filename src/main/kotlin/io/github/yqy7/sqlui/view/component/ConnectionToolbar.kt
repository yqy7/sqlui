package io.github.yqy7.sqlui.view.component

import io.github.yqy7.sqlui.model.ConnectionInfo
import io.github.yqy7.sqlui.util.AppScope
import io.github.yqy7.sqlui.viewmodel.ConnectionViewModel
import kotlinx.coroutines.launch
import java.awt.FlowLayout
import javax.swing.*

/**
 * 连接工具栏。
 * 提供新建连接、关闭连接按钮和连接状态指示。
 */
class ConnectionToolbar(
    private val connectionViewModel: ConnectionViewModel
) : JToolBar() {

    private val connectButton = JButton("新建连接")
    private val closeButton = JButton("断开连接")
    private val statusLabel = JLabel("未连接")
    private val recentMenu = JMenu("最近连接")

    init {
        isFloatable = false
        layout = FlowLayout(FlowLayout.LEFT, 4, 2)

        add(connectButton)
        add(closeButton)
        addSeparator()
        add(statusLabel)

        // 最近连接下拉菜单
        val menuBar = JMenuBar()
        menuBar.add(recentMenu)
        add(menuBar)

        closeButton.isEnabled = false

        // 事件绑定
        connectButton.addActionListener { showConnectionDialog() }
        closeButton.addActionListener { connectionViewModel.closeConnection() }

        // 监听 ViewModel 状态变化
        AppScope.scope.launch {
            connectionViewModel.isConnected.collect { connected ->
                SwingUtilities.invokeLater {
                    connectButton.isEnabled = !connected
                    closeButton.isEnabled = connected
                }
            }
        }
        AppScope.scope.launch {
            connectionViewModel.currentConnectionName.collect { name ->
                SwingUtilities.invokeLater { statusLabel.text = name }
            }
        }
        AppScope.scope.launch {
            connectionViewModel.recentConnections.collect { connections ->
                SwingUtilities.invokeLater { updateRecentMenu(connections) }
            }
        }
    }

    fun showConnectionDialog(owner: JFrame? = null) {
        val frame = owner ?: (SwingUtilities.getWindowAncestor(this) as? JFrame) ?: return
        val dialog = ConnectionDialog(frame) { info ->
            connectionViewModel.openConnection(info)
        }
        dialog.isVisible = true
    }

    private fun updateRecentMenu(connections: List<ConnectionInfo>) {
        recentMenu.removeAll()
        if (connections.isEmpty()) {
            recentMenu.add(JMenuItem("（无最近连接）").apply { isEnabled = false })
        } else {
            for (info in connections) {
                recentMenu.add(JMenuItem(info.displayName).apply {
                    addActionListener { connectionViewModel.openConnection(info) }
                })
            }
        }
    }
}
