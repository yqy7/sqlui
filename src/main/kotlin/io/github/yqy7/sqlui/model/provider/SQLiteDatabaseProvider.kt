package io.github.yqy7.sqlui.model.provider

import io.github.yqy7.sqlui.model.*
import java.sql.*

/**
 * SQLite 数据库 Provider 实现。
 * 仅支持文件模式 —— SQLite 不支持内存数据库的多连接共享。
 */
class SQLiteDatabaseProvider : DatabaseProvider {

    override val databaseType: DatabaseType = DatabaseType.SQLITE
    override var isConnected: Boolean = false
        private set
    override var connectionUrl: String = ""
        private set

    private var connection: Connection? = null

    override fun connect(info: ConnectionInfo) {
        disconnect()

        if (info.filePath.isBlank()) {
            throw IllegalArgumentException("SQLite 需要指定文件路径")
        }

        val url = "jdbc:sqlite:${info.filePath}"
        connection = DriverManager.getConnection(url)
        connectionUrl = url
        isConnected = true
    }

    override fun disconnect() {
        connection?.close()
        connection = null
        isConnected = false
        connectionUrl = ""
    }

    override fun getTables(): List<TableInfo> {
        val conn = checkConnection()
        val tables = mutableListOf<TableInfo>()

        // SQLite JDBC 驱动的 getTables 可能行为不一致，使用查询方式
        conn.createStatement().use { stmt ->
            stmt.executeQuery(
                "SELECT name, type FROM sqlite_master " +
                    "WHERE type IN ('table', 'view') AND name NOT LIKE 'sqlite_%' " +
                    "ORDER BY name"
            ).use { rs ->
                while (rs.next()) {
                    tables.add(
                        TableInfo(
                            name = rs.getString("name"),
                            schema = "main",
                            type = rs.getString("type").uppercase()
                        )
                    )
                }
            }
        }

        return tables
    }

    override fun getColumns(tableName: String): List<ColumnInfo> {
        val conn = checkConnection()
        val columns = mutableListOf<ColumnInfo>()

        // 使用 PRAGMA table_info 获取列信息
        conn.createStatement().use { stmt ->
            stmt.executeQuery("PRAGMA table_info(\"$tableName\")").use { rs ->
                var pos = 0
                while (rs.next()) {
                    columns.add(
                        ColumnInfo(
                            name = rs.getString("name"),
                            dataType = rs.getString("type") ?: "",
                            nullable = rs.getInt("notnull") == 0,
                            isPrimaryKey = rs.getInt("pk") > 0,
                            defaultValue = rs.getString("dflt_value"),
                            ordinalPosition = pos++
                        )
                    )
                }
            }
        }

        return columns
    }

    override fun executeQuery(sql: String): QueryResult {
        val conn = checkConnection()
        val startTime = System.currentTimeMillis()

        return try {
            conn.createStatement().use { stmt ->
                stmt.fetchSize = 1000
                val isResultSet = stmt.execute(sql)

                if (isResultSet) {
                    stmt.resultSet.use { rs ->
                        val meta = rs.metaData
                        val columnCount = meta.columnCount
                        val columns = (1..columnCount).map { meta.getColumnName(it) }
                        val rows = mutableListOf<List<Any?>>()

                        var count = 0
                        while (rs.next() && count < 1000) {
                            rows.add((1..columnCount).map { rs.getObject(it) })
                            count++
                        }

                        QueryResult.Success(
                            columns = columns,
                            rows = rows,
                            rowCount = rows.size,
                            executionTimeMs = System.currentTimeMillis() - startTime
                        )
                    }
                } else {
                    QueryResult.UpdateSuccess(
                        affectedRows = stmt.updateCount,
                        executionTimeMs = System.currentTimeMillis() - startTime
                    )
                }
            }
        } catch (e: SQLException) {
            QueryResult.Error(
                message = e.message ?: "未知错误",
                sqlState = e.sqlState
            )
        }
    }

    override fun executeUpdate(sql: String): Int {
        val conn = checkConnection()
        conn.createStatement().use { stmt ->
            return stmt.executeUpdate(sql)
        }
    }

    override fun createInMemory(name: String) {
        throw UnsupportedOperationException("SQLite 不支持内存数据库模式。请使用 H2 内存数据库")
    }

    private fun checkConnection(): Connection {
        return connection ?: throw IllegalStateException("未连接到数据库")
    }
}
