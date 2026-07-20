package io.github.yqy7.sqlui.view.component

import io.github.yqy7.sqlui.viewmodel.ResultViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import io.github.yqy7.sqlui.util.AppScope
import java.awt.BorderLayout
import java.awt.FlowLayout
import javax.swing.*

/**
 * 底部状态栏。
 * 显示连接状态和当前标签页信息。
 */
class StatusBar(
    private val resultViewModel: ResultViewModel
) : JPanel(BorderLayout()) {

    private val connectionLabel = JLabel("未连接")
    private val tabLabel = JLabel("")

    init {
        border = BorderFactory.createEmptyBorder(2, 8, 2, 8)

        val leftPanel = JPanel(FlowLayout(FlowLayout.LEFT, 16, 0))
        leftPanel.add(JLabel("状态:")).apply { /* keep reference */ }
        leftPanel.add(connectionLabel)
        leftPanel.add(JSeparator(SwingConstants.VERTICAL))
        leftPanel.add(tabLabel)

        add(leftPanel, BorderLayout.WEST)

        // 监听状态变化
        AppScope.scope.launch {
            resultViewModel.connectionStatusText.collectLatest { text ->
                SwingUtilities.invokeLater { connectionLabel.text = text }
            }
        }
        AppScope.scope.launch {
            resultViewModel.activeTabTitle.collectLatest { title ->
                SwingUtilities.invokeLater { tabLabel.text = title }
            }
        }
    }
}
