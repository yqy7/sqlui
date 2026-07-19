package io.github.yqy7.sqlui.view.component

import io.github.yqy7.sqlui.model.ColumnInfo
import io.github.yqy7.sqlui.viewmodel.BrowserViewModel
import javafx.scene.control.*
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox

/**
 * 表结构面板 —— TableView 显示选中表的列信息。
 */
class TableStructurePanel(private val viewModel: BrowserViewModel) : VBox(4.0) {

    private val titleLabel = Label("表结构")
    private val tableView = TableView<ColumnInfo>()

    init {
        titleLabel.styleClass.add("section-title")
        VBox.setVgrow(tableView, Priority.ALWAYS)

        buildColumns()
        children.addAll(titleLabel, tableView)

        // 双向绑定：选中表 → 显示列
        viewModel.columns.addListener { _: javafx.collections.ListChangeListener.Change<out ColumnInfo>? ->
            tableView.items.setAll(viewModel.columns)
        }
    }

    private fun buildColumns() {
        val nameCol = TableColumn<ColumnInfo, String>("列名").apply {
            setCellValueFactory { javafx.beans.property.SimpleStringProperty(it.value.name) }
            prefWidth = 120.0
        }

        val typeCol = TableColumn<ColumnInfo, String>("类型").apply {
            setCellValueFactory { javafx.beans.property.SimpleStringProperty(it.value.dataType) }
            prefWidth = 100.0
        }

        val nullableCol = TableColumn<ColumnInfo, String>("可为空").apply {
            setCellValueFactory {
                javafx.beans.property.SimpleStringProperty(if (it.value.nullable) "YES" else "NO")
            }
            prefWidth = 60.0
        }

        val pkCol = TableColumn<ColumnInfo, String>("主键").apply {
            setCellValueFactory {
                javafx.beans.property.SimpleStringProperty(if (it.value.isPrimaryKey) "PK" else "")
            }
            prefWidth = 50.0
        }

        val defaultCol = TableColumn<ColumnInfo, String>("默认值").apply {
            setCellValueFactory {
                javafx.beans.property.SimpleStringProperty(it.value.defaultValue ?: "")
            }
            prefWidth = 100.0
        }

        tableView.columns.setAll(nameCol, typeCol, nullableCol, pkCol, defaultCol)
        tableView.columnResizePolicy = TableView.UNCONSTRAINED_RESIZE_POLICY
    }
}
