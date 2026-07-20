package io.github.yqy7.sqlui.viewmodel

import io.github.yqy7.sqlui.model.QueryResult
import io.github.yqy7.sqlui.service.DatabaseService
import io.github.yqy7.sqlui.util.AppScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * SQL 编辑器 ViewModel —— 每个查询标签页持有一个实例。
 */
class EditorViewModel(
    private val databaseService: DatabaseService,
    val tabTitle: String = "Query"
) {

    private val _sqlText = MutableStateFlow("")
    val sqlText: StateFlow<String> = _sqlText.asStateFlow()

    private val _isExecuting = MutableStateFlow(false)
    val isExecuting: StateFlow<Boolean> = _isExecuting.asStateFlow()

    private val _queryResult = MutableStateFlow<QueryResult?>(null)
    val queryResult: StateFlow<QueryResult?> = _queryResult.asStateFlow()

    private val _executionTimeMs = MutableStateFlow(0L)
    val executionTimeMs: StateFlow<Long> = _executionTimeMs.asStateFlow()

    private val _resultRowCount = MutableStateFlow(0)
    val resultRowCount: StateFlow<Int> = _resultRowCount.asStateFlow()

    private val _errorMessage = MutableStateFlow("")
    val errorMessage: StateFlow<String> = _errorMessage.asStateFlow()

    /** 更新 SQL 文本（由编辑器调用） */
    fun setSqlText(text: String) {
        _sqlText.value = text
    }

    /** 执行 SQL 查询 */
    fun executeQuery() {
        val sql = _sqlText.value.trim()
        if (sql.isEmpty() || !databaseService.isConnected) return

        AppScope.scope.launch {
            _isExecuting.value = true
            _errorMessage.value = ""

            val result = databaseService.executeQuery(sql)
            _queryResult.value = result
            _isExecuting.value = false

            when (result) {
                is QueryResult.Success -> {
                    _executionTimeMs.value = result.executionTimeMs
                    _resultRowCount.value = result.rowCount
                    _errorMessage.value = ""
                }
                is QueryResult.UpdateSuccess -> {
                    _executionTimeMs.value = result.executionTimeMs
                    _resultRowCount.value = result.affectedRows
                    _errorMessage.value = ""
                }
                is QueryResult.Error -> {
                    _executionTimeMs.value = 0
                    _resultRowCount.value = 0
                    _errorMessage.value = result.message
                }
            }
        }
    }

    /** 在 SQL 编辑器中插入表名 */
    fun insertTableName(tableName: String) {
        val current = _sqlText.value
        _sqlText.value = if (current.isBlank()) tableName else "$current $tableName"
    }

    /** 从表名生成 SELECT 查询并填入编辑器（仅在编辑器为空时生效） */
    fun generateSelectQuery(tableName: String) {
        if (_sqlText.value.isBlank()) {
            _sqlText.value = "SELECT * FROM \"$tableName\" LIMIT 100;"
        }
    }
}
