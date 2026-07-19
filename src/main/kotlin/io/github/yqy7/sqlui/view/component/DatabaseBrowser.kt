package io.github.yqy7.sqlui.view.component

import io.github.yqy7.sqlui.model.TableInfo
import io.github.yqy7.sqlui.viewmodel.BrowserViewModel
import javafx.scene.control.TreeItem
import javafx.scene.control.TreeView

/**
 * 左侧数据库浏览器 —— TreeView 显示数据库对象树。
 *
 * 结构：
 * - 📁 表 (Tables)
 *   - table_name_1
 *   - table_name_2
 * - 📁 视图 (Views)
 *   - view_name_1
 *
 * 交互：
 * - 单击：显示表结构
 * - 双击：生成 SELECT 查询（通过 [onTableDoubleClick] 回调）
 */
class DatabaseBrowser(private val viewModel: BrowserViewModel) : TreeView<String>() {

    private val rootItem = TreeItem<String>("数据库对象").apply { isExpanded = true }

    /** 双击表名时的回调 */
    var onTableDoubleClick: ((String) -> Unit)? = null

    init {
        isShowRoot = true
        root = rootItem

        // 监听表列表变化，重建树
        viewModel.tables.addListener { _: javafx.collections.ListChangeListener.Change<out TableInfo>? ->
            rebuildTree()
        }

        // 单击：显示表结构
        selectionModel.selectedItemProperty().addListener { _, _, selected ->
            val tableName = selected?.value
            if (tableName != null && selected.parent?.value == "表") {
                viewModel.selectTable(tableName)
            }
        }

        // 双击：生成 SELECT 查询
        setOnMouseClicked { event ->
            if (event.clickCount == 2) {
                val selected = selectionModel.selectedItem
                val tableName = selected?.value
                if (tableName != null && selected.parent?.value == "表") {
                    onTableDoubleClick?.invoke(tableName)
                }
            }
        }
    }

    private fun rebuildTree() {
        val tables = viewModel.tables

        val tablesGroup = TreeItem<String>("表").apply { isExpanded = true }
        val viewsGroup = TreeItem<String>("视图").apply { isExpanded = true }

        for (table in tables) {
            when (table.type.uppercase()) {
                "VIEW" -> viewsGroup.children.add(TreeItem(table.name))
                else -> tablesGroup.children.add(TreeItem(table.name))
            }
        }

        rootItem.children.setAll(tablesGroup, viewsGroup)
    }
}
