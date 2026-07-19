package io.github.yqy7.sqlui.model.provider

import io.github.yqy7.sqlui.model.*

/**
 * 数据库 Provider 接口 —— 所有数据库支持的扩展点。
 *
 * 添加新数据库支持只需：
 * 1. 在 [DatabaseType] 中添加枚举值
 * 2. 实现本接口
 * 3. 在 DatabaseService 工厂中注册
 */
interface DatabaseProvider {
    val databaseType: DatabaseType
    val isConnected: Boolean
    val connectionUrl: String

    /** 建立连接 */
    fun connect(info: ConnectionInfo)

    /** 断开连接 */
    fun disconnect()

    /** 获取所有表（含视图） */
    fun getTables(): List<TableInfo>

    /** 获取指定表的列信息 */
    fun getColumns(tableName: String): List<ColumnInfo>

    /** 执行查询（SELECT），返回结果集 */
    fun executeQuery(sql: String): QueryResult

    /** 执行更新（INSERT/UPDATE/DELETE/DDL），返回影响行数 */
    fun executeUpdate(sql: String): Int

    /** 创建 H2 内存数据库快捷方法（仅 H2 有效，SQLite 抛异常） */
    fun createInMemory(name: String) {
        throw UnsupportedOperationException("${databaseType.displayName} 不支持内存数据库")
    }
}
