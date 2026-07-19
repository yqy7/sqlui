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
 * 数据库浏览器 ViewModel —— 管理表树和表结构显示。
 */
class BrowserViewModel(private val databaseService: DatabaseService) {

    /** 树节点数据：数据库 → 表/视图分组 */
    val tables: ObservableList<TableInfo> = FXCollections.observableArrayList()
    val selectedTable = SimpleObjectProperty<TableInfo?>(null)
    val columns: ObservableList<ColumnInfo> = FXCollections.observableArrayList()
    val isLoading = SimpleObjectProperty(false)

    /** 当前选中表名（用于 UI 绑定） */
    val selectedTableName = SimpleStringProperty("")

    init {
        // 监听 DatabaseService 的表列表变化
        AppScope.scope.launch {
            databaseService.tables.collect { tableList ->
                javafx.application.Platform.runLater {
                    tables.setAll(tableList)
                }
            }
        }

        // 监听选中表变化，加载列信息
        selectedTable.addListener { _, _, newTable ->
            if (newTable != null) {
                selectedTableName.set(newTable.name)
                loadColumns(newTable.name)
            } else {
                selectedTableName.set("")
                columns.clear()
            }
        }
    }

    fun selectTable(tableName: String) {
        val table = tables.find { it.name == tableName }
        selectedTable.set(table)
    }

    fun refresh() {
        AppScope.scope.launch {
            isLoading.set(true)
            databaseService.refreshTables()
            javafx.application.Platform.runLater {
                isLoading.set(false)
            }
        }
    }

    private fun loadColumns(tableName: String) {
        AppScope.scope.launch {
            isLoading.set(true)
            val cols = databaseService.getColumns(tableName)
            javafx.application.Platform.runLater {
                columns.setAll(cols)
                isLoading.set(false)
            }
        }
    }
}
