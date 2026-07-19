package io.github.yqy7.sqlui

import io.github.yqy7.sqlui.view.MainWindow
import javafx.application.Application

/**
 * SQLui 应用入口。
 * 支持 H2 Database 和 SQLite 的数据库管理工具。
 */
fun main(args: Array<String>) {
    Application.launch(MainWindow::class.java, *args)
}
