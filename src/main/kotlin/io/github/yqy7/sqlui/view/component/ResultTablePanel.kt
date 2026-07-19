package io.github.yqy7.sqlui.view.component

import io.github.yqy7.sqlui.model.QueryResult
import javafx.beans.property.SimpleObjectProperty
import javafx.scene.control.*
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox

/**
 * 查询结果表格面板 —— 动态列 TableView。
 */
class ResultTablePanel : VBox(4.0) {

    data class RowData(val values: List<Any?>)

    private val tableView = TableView<RowData>()
    private val infoLabel = Label("")

    val currentResult = SimpleObjectProperty<QueryResult?>(null)

    init {
        VBox.setVgrow(tableView, Priority.ALWAYS)
        children.addAll(infoLabel, tableView)

        tableView.columnResizePolicy = TableView.UNCONSTRAINED_RESIZE_POLICY

        currentResult.addListener { _, _, result ->
            displayResult(result)
        }
    }

    private fun displayResult(result: QueryResult?) {
        tableView.columns.clear()
        tableView.items.clear()

        when (result) {
            is QueryResult.Success -> {
                infoLabel.text = "${result.rowCount} 行, ${result.executionTimeMs}ms"
                buildDynamicColumns(result.columns)
                val rowDataList = result.rows.map { RowData(it) }
                tableView.items.addAll(rowDataList)
            }
            is QueryResult.UpdateSuccess -> {
                infoLabel.text = "已更新 ${result.affectedRows} 行, ${result.executionTimeMs}ms"
            }
            is QueryResult.Error -> {
                infoLabel.text = "错误: ${result.message}"
                infoLabel.style = "-fx-text-fill: red;"
            }
            null -> {
                infoLabel.text = ""
            }
        }
    }

    private fun buildDynamicColumns(columns: List<String>) {
        for ((index, colName) in columns.withIndex()) {
            val col = TableColumn<RowData, String>(colName)
            col.setCellValueFactory { cellData ->
                val row = cellData.value
                val value = row.values.getOrNull(index)
                javafx.beans.property.SimpleStringProperty(value?.toString() ?: "NULL")
            }
            col.prefWidth = 120.0
            tableView.columns.add(col)
        }
    }
}
