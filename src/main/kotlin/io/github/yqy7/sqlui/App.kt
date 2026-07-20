package io.github.yqy7.sqlui

import io.github.yqy7.sqlui.view.MainWindow
import io.github.yqy7.sqlui.view.theme.FlatLafTheme
import javax.swing.SwingUtilities

/**
 * SQLui 应用入口。
 * 基于 Swing + FlatLaf 的桌面数据库管理工具。
 * 支持 H2 Database 和 SQLite。
 */
fun main() {
    FlatLafTheme.setup()
    SwingUtilities.invokeLater {
        MainWindow().isVisible = true
    }
}
