package io.github.yqy7.sqlui.viewmodel

import io.github.yqy7.sqlui.model.QueryResult
import io.github.yqy7.sqlui.model.service.DatabaseService
import io.github.yqy7.sqlui.util.AppScope
import javafx.beans.property.*
import kotlinx.coroutines.launch

/**
 * SQL 编辑器 ViewModel —— 每个查询标签页持有一个实例。
 */
class EditorViewModel(
    private val databaseService: DatabaseService,
    val tabTitle: String = "Query"
) {

    /** SQL 文本 */
    val sqlText = SimpleStringProperty("")

    /** 是否正在执行 */
    val isExecuting = SimpleBooleanProperty(false)

    /** 最近一次查询结果 */
    val queryResult = SimpleObjectProperty<QueryResult?>(null)

    /** 执行耗时（毫秒），显示在状态栏 */
    val executionTimeMs = SimpleLongProperty(0)

    /** 结果行数 */
    val resultRowCount = SimpleIntegerProperty(0)

    /** 错误消息 */
    val errorMessage = SimpleStringProperty("")

    /** 执行 SQL 查询 */
    fun executeQuery() {
        val sql = sqlText.get().trim()
        if (sql.isEmpty() || !databaseService.isConnected) return

        AppScope.scope.launch {
            isExecuting.set(true)
            errorMessage.set("")

            val result = databaseService.executeQuery(sql)
            javafx.application.Platform.runLater {
                queryResult.set(result)
                isExecuting.set(false)

                when (result) {
                    is QueryResult.Success -> {
                        executionTimeMs.set(result.executionTimeMs)
                        resultRowCount.set(result.rowCount)
                        errorMessage.set("")
                    }
                    is QueryResult.UpdateSuccess -> {
                        executionTimeMs.set(result.executionTimeMs)
                        resultRowCount.set(result.affectedRows)
                        errorMessage.set("")
                    }
                    is QueryResult.Error -> {
                        executionTimeMs.set(0)
                        resultRowCount.set(0)
                        errorMessage.set(result.message)
                    }
                }
            }
        }
    }

    /** 在 SQL 编辑器中插入表名（用于手动拼接 SQL） */
    fun insertTableName(tableName: String) {
        val current = sqlText.get()
        sqlText.set(if (current.isBlank()) tableName else "$current $tableName")
    }

    /** 从表名生成 SELECT 查询并填入编辑器（仅在编辑器为空时生效） */
    fun generateSelectQuery(tableName: String) {
        if (sqlText.get().isBlank()) {
            sqlText.set("SELECT * FROM \"$tableName\" LIMIT 100;")
        }
    }
}
