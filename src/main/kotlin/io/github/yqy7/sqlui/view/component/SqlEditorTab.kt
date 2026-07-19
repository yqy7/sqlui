package io.github.yqy7.sqlui.view.component

import io.github.yqy7.sqlui.model.QueryResult
import io.github.yqy7.sqlui.model.service.DatabaseService
import io.github.yqy7.sqlui.util.CsvExporter
import io.github.yqy7.sqlui.viewmodel.EditorViewModel
import javafx.scene.control.*
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyCodeCombination
import javafx.scene.input.KeyCombination
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import javafx.stage.FileChooser
import org.fxmisc.richtext.CodeArea
import java.io.File

/**
 * 单个 SQL 查询标签页 —— 包含编辑器、工具栏和结果表。
 */
class SqlEditorTab(
    private val databaseService: DatabaseService,
    tabTitle: String = "Query"
) : Tab(tabTitle) {

    val editorViewModel = EditorViewModel(databaseService, tabTitle)

    private val codeArea = CodeArea()
    private val resultPanel = ResultTablePanel()

    private var syncing = false

    init {
        isClosable = true
        buildContent()

        // 结果 → 面板
        editorViewModel.queryResult.addListener { _, _, result ->
            resultPanel.currentResult.set(result)
        }

        // 双向绑定：CodeArea ↔ ViewModel.sqlText
        codeArea.textProperty().addListener { _, _, newText ->
            if (!syncing) {
                syncing = true
                editorViewModel.sqlText.set(newText)
                syncing = false
            }
        }
        editorViewModel.sqlText.addListener { _, _, newText ->
            if (!syncing) {
                syncing = true
                codeArea.replaceText(newText ?: "")
                syncing = false
            }
        }
    }

    private fun buildContent() {
        val container = VBox(4.0)

        // 工具栏
        val toolBar = buildToolBar()

        // SQL 编辑器
        val editorBox = VBox(0.0).apply {
            VBox.setVgrow(codeArea, Priority.ALWAYS)
            children.add(codeArea)
            style = "-fx-border-color: #ddd; -fx-border-width: 1;"
        }

        container.children.addAll(toolBar, editorBox, resultPanel)
        VBox.setVgrow(editorBox, Priority.ALWAYS)

        content = container
    }

    private fun buildToolBar(): ToolBar {
        val bar = ToolBar()

        val executeBtn = Button("▶ 执行")
        executeBtn.setOnAction { editorViewModel.executeQuery() }
        executeBtn.styleClass.add("execute-btn")

        val exportBtn = Button("导出 CSV")
        exportBtn.setOnAction { exportToCsv() }

        val formatBtn = Button("格式化 SQL")
        formatBtn.setOnAction { formatSql() }

        bar.items.addAll(executeBtn, formatBtn, exportBtn)
        return bar
    }

    /** 配置 CodeArea 的语法高亮 */
    fun configureHighlighting(highlighter: io.github.yqy7.sqlui.util.SqlHighlighter) {
        highlighter.applyTo(codeArea)
    }

    /** Ctrl+Enter 执行查询 */
    fun handleExecuteShortcut() {
        codeArea.setOnKeyPressed { event ->
            if (KeyCodeCombination(KeyCode.ENTER, KeyCombination.CONTROL_DOWN).match(event)) {
                editorViewModel.executeQuery()
                event.consume()
            }
        }
    }

    private fun exportToCsv() {
        val result = editorViewModel.queryResult.get()
        if (result !is QueryResult.Success) {
            Alert(Alert.AlertType.WARNING, "没有可导出的查询结果").showAndWait()
            return
        }

        val chooser = FileChooser().apply {
            title = "导出 CSV"
            initialFileName = "query_result.csv"
            extensionFilters.add(FileChooser.ExtensionFilter("CSV 文件", listOf("*.csv")))
        }

        val file = chooser.showSaveDialog(tabPane?.scene?.window)
        if (file != null) {
            val success = CsvExporter.export(result, file)
            if (success) {
                Alert(Alert.AlertType.INFORMATION, "已导出到: ${file.absolutePath}").showAndWait()
            } else {
                Alert(Alert.AlertType.ERROR, "导出失败").showAndWait()
            }
        }
    }

    private fun formatSql() {
        val sql = editorViewModel.sqlText.get()
        if (sql.isBlank()) return

        // 简单的格式化：将主要关键字转为大写并在其前换行
        val keywords = listOf(
            "SELECT", "FROM", "WHERE", "AND", "OR", "ORDER BY", "GROUP BY",
            "HAVING", "JOIN", "INNER JOIN", "LEFT JOIN", "RIGHT JOIN",
            "ON", "INSERT INTO", "VALUES", "UPDATE", "SET", "DELETE FROM",
            "CREATE TABLE", "ALTER TABLE", "DROP TABLE", "LIMIT", "OFFSET"
        )

        var formatted = sql.trim()
        for (kw in keywords.sortedByDescending { it.length }) {
            val regex = Regex("\\b${Regex.escape(kw)}\\b", RegexOption.IGNORE_CASE)
            formatted = formatted.replace(regex) { "\n${kw.uppercase()}" }
        }
        formatted = formatted.replace(Regex("\n+"), "\n").trimStart('\n')

        editorViewModel.sqlText.set(formatted)
    }
}
