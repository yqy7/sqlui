package io.github.yqy7.sqlui.model.provider

import io.github.yqy7.sqlui.model.*
import java.sql.*

/**
 * H2 数据库 Provider 实现。
 * 支持文件模式、内存模式、TCP 服务器模式。
 */
class H2DatabaseProvider : DatabaseProvider {

    override val databaseType: DatabaseType = DatabaseType.H2
    override var isConnected: Boolean = false
        private set
    override var connectionUrl: String = ""
        private set

    private var connection: Connection? = null

    override fun connect(info: ConnectionInfo) {
        disconnect()

        val url = when {
            // 内存数据库
            info.filePath.isBlank() && info.databaseName.isNotBlank() ->
                "jdbc:h2:mem:${info.databaseName};DB_CLOSE_DELAY=-1"
            // 文件数据库
            info.filePath.isNotBlank() -> {
                val path = info.filePath.removeSuffix(".mv.db").removeSuffix(".h2.db")
                "jdbc:h2:file:$path"
            }
            else -> throw IllegalArgumentException("H2 连接需要指定文件路径或数据库名")
        }

        connection = DriverManager.getConnection(
            url,
            info.username.ifBlank { "sa" },
            info.password.ifBlank { "" }
        )
        connectionUrl = url
        isConnected = true
    }

    override fun disconnect() {
        connection?.close()
        connection = null
        isConnected = false
        connectionUrl = ""
    }

    override fun createInMemory(name: String) {
        connect(ConnectionInfo(databaseType = DatabaseType.H2, databaseName = name))
    }

    override fun getTables(): List<TableInfo> {
        val conn = checkConnection()
        val tables = mutableListOf<TableInfo>()

        conn.metaData.getTables(null, "PUBLIC", "%", arrayOf("TABLE", "VIEW", "SYSTEM TABLE"))
            .use { rs ->
                while (rs.next()) {
                    tables.add(
                        TableInfo(
                            name = rs.getString("TABLE_NAME"),
                            schema = rs.getString("TABLE_SCHEM") ?: "PUBLIC",
                            type = rs.getString("TABLE_TYPE") ?: "TABLE"
                        )
                    )
                }
            }

        return tables.sortedBy { it.name }
    }

    override fun getColumns(tableName: String): List<ColumnInfo> {
        val conn = checkConnection()

        // 获取列信息
        val columns = mutableListOf<ColumnInfo>()
        conn.metaData.getColumns(null, "PUBLIC", tableName, "%").use { rs ->
            while (rs.next()) {
                columns.add(
                    ColumnInfo(
                        name = rs.getString("COLUMN_NAME"),
                        dataType = rs.getString("TYPE_NAME"),
                        nullable = rs.getInt("NULLABLE") == DatabaseMetaData.columnNullable,
                        defaultValue = rs.getString("COLUMN_DEF"),
                        ordinalPosition = rs.getInt("ORDINAL_POSITION"),
                        size = rs.getInt("COLUMN_SIZE")
                    )
                )
            }
        }

        // 获取主键信息
        val pkColumns = mutableSetOf<String>()
        conn.metaData.getPrimaryKeys(null, "PUBLIC", tableName).use { rs ->
            while (rs.next()) {
                pkColumns.add(rs.getString("COLUMN_NAME"))
            }
        }

        return columns.map { col ->
            if (col.name in pkColumns) col.copy(isPrimaryKey = true) else col
        }.sortedBy { it.ordinalPosition }
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

    private fun checkConnection(): Connection {
        return connection ?: throw IllegalStateException("未连接到数据库")
    }
}
