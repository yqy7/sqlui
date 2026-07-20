package io.github.yqy7.sqlui.viewmodel

import io.github.yqy7.sqlui.model.*
import io.github.yqy7.sqlui.service.DatabaseService
import io.github.yqy7.sqlui.util.AppScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 数据库浏览器 ViewModel —— 管理表树和表结构显示。
 */
class BrowserViewModel(private val databaseService: DatabaseService) {

    private val _tables = MutableStateFlow<List<TableInfo>>(emptyList())
    val tables: StateFlow<List<TableInfo>> = _tables.asStateFlow()

    private val _selectedTable = MutableStateFlow<TableInfo?>(null)
    val selectedTable: StateFlow<TableInfo?> = _selectedTable.asStateFlow()

    private val _columns = MutableStateFlow<List<ColumnInfo>>(emptyList())
    val columns: StateFlow<List<ColumnInfo>> = _columns.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        // 监听 DatabaseService 的表列表变化
        AppScope.scope.launch {
            databaseService.tables.collect { tableList ->
                _tables.value = tableList
            }
        }
    }

    fun selectTable(tableName: String) {
        val table = _tables.value.find { it.name == tableName }
        _selectedTable.value = table
        if (table != null) {
            loadColumns(table.name)
        } else {
            _columns.value = emptyList()
        }
    }

    fun refresh() {
        AppScope.scope.launch {
            _isLoading.value = true
            databaseService.refreshTables()
            _isLoading.value = false
        }
    }

    private fun loadColumns(tableName: String) {
        AppScope.scope.launch {
            _isLoading.value = true
            val cols = databaseService.getColumns(tableName)
            _columns.value = cols
            _isLoading.value = false
        }
    }
}
