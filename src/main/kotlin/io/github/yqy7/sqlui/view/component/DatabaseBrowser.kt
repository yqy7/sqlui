package io.github.yqy7.sqlui.view.component

import io.github.yqy7.sqlui.model.ColumnInfo
import io.github.yqy7.sqlui.model.TableInfo
import io.github.yqy7.sqlui.util.AppScope
import io.github.yqy7.sqlui.viewmodel.BrowserViewModel
import kotlinx.coroutines.launch
import java.awt.BorderLayout
import java.awt.Component
import javax.swing.*
import javax.swing.table.DefaultTableModel
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeCellRenderer
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreeSelectionModel

/**
 * 数据库浏览器面板。
 * 左侧：JTree 显示表/视图列表，按类型分组。
 * 下方：JTable 显示选中表的列结构。
 */
class DatabaseBrowser(
    private val browserViewModel: BrowserViewModel
) : JPanel(BorderLayout()) {

    private val rootNode = DefaultMutableTreeNode("数据库")
    private val tablesNode = DefaultMutableTreeNode("表 (TABLES)")
    private val viewsNode = DefaultMutableTreeNode("视图 (VIEWS)")

    private val treeModel = DefaultTreeModel(rootNode)
    private val tree = JTree(treeModel).apply {
        getSelectionModel().selectionMode = TreeSelectionModel.SINGLE_TREE_SELECTION
        isRootVisible = false
        cellRenderer = BrowserTreeCellRenderer()
    }

    private val columnTableModel = DefaultTableModel(arrayOf("列名", "类型", "可为空", "主键", "默认值"), 0)
    private val columnTable = JTable(columnTableModel).apply {
        autoResizeMode = JTable.AUTO_RESIZE_ALL_COLUMNS
    }

    private val refreshButton = JButton("刷新")

    init {
        rootNode.add(tablesNode)
        rootNode.add(viewsNode)
        treeModel.nodeStructureChanged(rootNode)

        // 上方：树
        val treeScroll = JScrollPane(tree)
        treeScroll.border = BorderFactory.createTitledBorder("数据库对象")

        // 操作栏
        val toolbar = JToolBar().apply {
            isFloatable = false
            add(refreshButton)
        }

        val topPanel = JPanel(BorderLayout())
        topPanel.add(toolbar, BorderLayout.NORTH)
        topPanel.add(treeScroll, BorderLayout.CENTER)

        // 下方：表结构
        val columnScroll = JScrollPane(columnTable)
        columnScroll.border = BorderFactory.createTitledBorder("表结构")
        columnScroll.preferredSize = java.awt.Dimension(260, 180)

        val splitPane = JSplitPane(JSplitPane.VERTICAL_SPLIT, topPanel, columnScroll).apply {
            dividerLocation = 320
            resizeWeight = 0.65
        }

        add(splitPane, BorderLayout.CENTER)

        // 事件
        refreshButton.addActionListener { browserViewModel.refresh() }
        tree.addTreeSelectionListener { e ->
            val node = e.path.lastPathComponent as? DefaultMutableTreeNode ?: return@addTreeSelectionListener
            val userObj = node.userObject
            if (userObj is TableInfo) {
                browserViewModel.selectTable(userObj.name)
            }
        }

        // 监听 ViewModel
        AppScope.scope.launch {
            browserViewModel.tables.collect { tables ->
                SwingUtilities.invokeLater { updateTree(tables) }
            }
        }
        AppScope.scope.launch {
            browserViewModel.columns.collect { columns ->
                SwingUtilities.invokeLater { updateColumns(columns) }
            }
        }
    }

    private fun updateTree(tables: List<TableInfo>) {
        tablesNode.removeAllChildren()
        viewsNode.removeAllChildren()

        for (table in tables) {
            val target = if (table.type.uppercase() == "VIEW") viewsNode else tablesNode
            target.add(DefaultMutableTreeNode(table))
        }

        treeModel.nodeStructureChanged(tablesNode)
        treeModel.nodeStructureChanged(viewsNode)

        // 展开节点
        tree.expandPath(javax.swing.tree.TreePath(tablesNode.path))
        tree.expandPath(javax.swing.tree.TreePath(viewsNode.path))
    }

    private fun updateColumns(columns: List<ColumnInfo>) {
        columnTableModel.setRowCount(0)
        for (col in columns) {
            columnTableModel.addRow(arrayOf(
                col.name,
                col.dataType,
                if (col.nullable) "✓" else "",
                if (col.isPrimaryKey) "✓" else "",
                col.defaultValue ?: ""
            ))
        }
    }

    fun getSelectedTableName(): String? {
        val node = tree.lastSelectedPathComponent as? DefaultMutableTreeNode ?: return null
        return (node.userObject as? TableInfo)?.name
    }
}

/**
 * 浏览器树节点渲染器：显示表名。
 */
private class BrowserTreeCellRenderer : DefaultTreeCellRenderer() {
    override fun getTreeCellRendererComponent(
        tree: JTree?, value: Any?, sel: Boolean, expanded: Boolean,
        leaf: Boolean, row: Int, hasFocus: Boolean
    ): Component {
        val comp = super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus)
        if (value is DefaultMutableTreeNode) {
            val userObj = value.userObject
            text = when (userObj) {
                is TableInfo -> userObj.name
                else -> userObj?.toString() ?: ""
            }
        }
        return comp
    }
}
