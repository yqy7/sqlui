package io.github.yqy7.sqlui.model.service

import io.github.yqy7.sqlui.model.*
import io.github.yqy7.sqlui.model.provider.*
import io.github.yqy7.sqlui.util.AppScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 数据库服务 —— 连接管理、查询执行的中央协调层。
 * 所有数据库访问通过 dbDispatcher 串行化，保证 H2/SQLite 的写安全。
 */
class DatabaseService {
    private val mutex = kotlinx.coroutines.sync.Mutex()

    private val _activeProvider = MutableStateFlow<DatabaseProvider?>(null)
    val activeProvider: StateFlow<DatabaseProvider?> = _activeProvider.asStateFlow()

    private val _connectionState = MutableStateFlow<UiState<ConnectionInfo>>(UiState.Idle)
    val connectionState: StateFlow<UiState<ConnectionInfo>> = _connectionState.asStateFlow()

    private val _tables = MutableStateFlow<List<TableInfo>>(emptyList())
    val tables: StateFlow<List<TableInfo>> = _tables.asStateFlow()

    val isConnected: Boolean get() = _activeProvider.value?.isConnected == true

    /** 打开数据库连接 */
    suspend fun openConnection(info: ConnectionInfo): UiState<ConnectionInfo> {
        _connectionState.value = UiState.Loading

        return withContext(AppScope.dbDispatcher) {
            mutex.lock()
            try {
                val provider = createProvider(info.databaseType)
                provider.connect(info)
                _activeProvider.value = provider
                _tables.value = provider.getTables()
                _connectionState.value = UiState.Success(info)
                UiState.Success(info)
            } catch (e: Exception) {
                val errorMsg = "连接失败: ${e.message}"
                _connectionState.value = UiState.Error(errorMsg)
                UiState.Error(errorMsg)
            } finally {
                mutex.unlock()
            }
        }
    }

    /** 关闭当前连接 */
    suspend fun closeConnection() {
        withContext(AppScope.dbDispatcher) {
            mutex.lock()
            try {
                _activeProvider.value?.disconnect()
                _activeProvider.value = null
                _tables.value = emptyList()
                _connectionState.value = UiState.Idle
            } finally {
                mutex.unlock()
            }
        }
    }

    /** 刷新表列表 */
    suspend fun refreshTables(): List<TableInfo> {
        return withContext(AppScope.dbDispatcher) {
            mutex.lock()
            try {
                val tables = _activeProvider.value?.getTables() ?: emptyList()
                _tables.value = tables
                tables
            } finally {
                mutex.unlock()
            }
        }
    }

    /** 获取表列信息 */
    suspend fun getColumns(tableName: String): List<ColumnInfo> {
        return withContext(AppScope.dbDispatcher) {
            mutex.lock()
            try {
                _activeProvider.value?.getColumns(tableName) ?: emptyList()
            } finally {
                mutex.unlock()
            }
        }
    }

    /** 执行 SQL 查询（异步，自动判断 SELECT / DML） */
    suspend fun executeQuery(sql: String): QueryResult {
        return withContext(AppScope.dbDispatcher) {
            mutex.lock()
            try {
                val provider = _activeProvider.value
                    ?: return@withContext QueryResult.Error("未连接到数据库")

                val result = provider.executeQuery(sql)
                // DML 操作后自动刷新表列表
                if (result is QueryResult.UpdateSuccess) {
                    _tables.value = provider.getTables()
                }
                result
            } catch (e: Exception) {
                QueryResult.Error(e.message ?: "执行查询时发生未知错误")
            } finally {
                mutex.unlock()
            }
        }
    }

    /** 快速执行更新（不返回详细结果） */
    suspend fun executeUpdate(sql: String): Int {
        return withContext(AppScope.dbDispatcher) {
            mutex.lock()
            try {
                val result = _activeProvider.value?.executeUpdate(sql) ?: 0
                _activeProvider.value?.let { _tables.value = it.getTables() }
                result
            } finally {
                mutex.unlock()
            }
        }
    }

    /** 创建 H2 内存数据库（快捷方法） */
    suspend fun createH2InMemory(name: String = "test"): UiState<ConnectionInfo> {
        return openConnection(
            ConnectionInfo(databaseType = DatabaseType.H2, databaseName = name)
        )
    }

    private fun createProvider(type: DatabaseType): DatabaseProvider = when (type) {
        DatabaseType.H2 -> H2DatabaseProvider()
        DatabaseType.SQLITE -> SQLiteDatabaseProvider()
    }
}
