package io.github.yqy7.sqlui.view.component

import io.github.yqy7.sqlui.model.QueryResult
import io.github.yqy7.sqlui.service.DatabaseService
import io.github.yqy7.sqlui.util.AppScope
import io.github.yqy7.sqlui.util.SqlHighlighter
import io.github.yqy7.sqlui.viewmodel.BrowserViewModel
import io.github.yqy7.sqlui.viewmodel.EditorViewModel
import kotlinx.coroutines.launch
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea
import org.fife.ui.rtextarea.RTextScrollPane
import java.awt.BorderLayout
import java.awt.event.ActionEvent
import java.awt.event.KeyEvent
import javax.swing.*

/**
 * 单个 SQL 查询标签页。
 * 包含 SQL 编辑器（RSyntaxTextArea）和结果表格（ResultTablePanel）。
 * 每个标签页持有独立的 EditorViewModel 实例。
 */
class SqlEditorTab(
    private val databaseService: DatabaseService,
    private val browserViewModel: BrowserViewModel,
    tabTitle: String = "Query"
) : JPanel(BorderLayout()) {

    val editorViewModel = EditorViewModel(databaseService, tabTitle)

    private val sqlEditor = RSyntaxTextArea(20, 60).apply {
        SqlHighlighter.configure(this)
    }
    private val editorScrollPane = RTextScrollPane(sqlEditor).apply {
        lineNumbersEnabled = true
    }
    private val resultPanel = ResultTablePanel()

    init {
        val splitPane = JSplitPane(JSplitPane.VERTICAL_SPLIT, editorScrollPane, resultPanel).apply {
            dividerLocation = 280
            resizeWeight = 0.5
        }
        add(splitPane, BorderLayout.CENTER)

        // 快捷键：Ctrl+Enter 执行 SQL
        val inputMap = sqlEditor.getInputMap(JComponent.WHEN_FOCUSED)
        val actionMap = sqlEditor.getActionMap()
        val executeKey = "executeSql"
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, KeyEvent.CTRL_DOWN_MASK), executeKey)
        actionMap.put(executeKey, object : AbstractAction() {
            override fun actionPerformed(e: ActionEvent?) {
                executeQuery()
            }
        })

        // Ctrl+S 保存 SQL
        val saveKey = "saveSql"
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_DOWN_MASK), saveKey)
        actionMap.put(saveKey, object : AbstractAction() {
            override fun actionPerformed(e: ActionEvent?) {
                saveSqlToFile()
            }
        })

        // 编辑器文本变化 → ViewModel
        sqlEditor.document.addDocumentListener(object : javax.swing.event.DocumentListener {
            override fun insertUpdate(e: javax.swing.event.DocumentEvent?) = syncTextToViewModel()
            override fun removeUpdate(e: javax.swing.event.DocumentEvent?) = syncTextToViewModel()
            override fun changedUpdate(e: javax.swing.event.DocumentEvent?) = syncTextToViewModel()
        })

        // 监听 ViewModel 状态变化 → 更新 UI
        AppScope.scope.launch {
            editorViewModel.queryResult.collect { result ->
                SwingUtilities.invokeLater {
                    if (result != null) {
                        resultPanel.showResult(result)
                    }
                }
            }
        }
        AppScope.scope.launch {
            editorViewModel.isExecuting.collect { executing ->
                SwingUtilities.invokeLater {
                    sqlEditor.isEnabled = !executing
                }
            }
        }
    }

    /** 执行 SQL 编辑器中的查询 */
    fun executeQuery() {
        // 同步编辑器内容到 ViewModel
        editorViewModel.setSqlText(sqlEditor.text)
        editorViewModel.executeQuery()
    }

    /** 向编辑器插入表名 */
    fun insertTableName(tableName: String) {
        val pos = sqlEditor.caretPosition
        sqlEditor.insert(tableName, pos)
        sqlEditor.requestFocusInWindow()
    }

    /** 在编辑器填入 SELECT 查询 */
    fun generateSelectQuery(tableName: String) {
        if (sqlEditor.text.isBlank()) {
            sqlEditor.text = "SELECT * FROM \"$tableName\" LIMIT 100;"
            editorViewModel.setSqlText(sqlEditor.text)
        }
    }

    /** 获取标签页标题 */
    fun getTitle(): String = editorViewModel.tabTitle

    private fun syncTextToViewModel() {
        editorViewModel.setSqlText(sqlEditor.text)
    }

    private fun saveSqlToFile() {
        val chooser = JFileChooser().apply {
            dialogTitle = "保存 SQL 文件"
            selectedFile = java.io.File("query.sql")
        }
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                chooser.selectedFile.writeText(sqlEditor.text)
            } catch (e: Exception) {
                JOptionPane.showMessageDialog(this, "保存失败: ${e.message}", "错误", JOptionPane.ERROR_MESSAGE)
            }
        }
    }
}
