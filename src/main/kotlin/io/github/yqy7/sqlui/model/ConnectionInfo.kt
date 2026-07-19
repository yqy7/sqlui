package io.github.yqy7.sqlui.model

/**
 * 数据库连接信息。
 * filePath 用于文件型数据库（H2文件模式、SQLite）；
 * host/port 预留给未来客户端-服务器型数据库。
 */
data class ConnectionInfo(
    val databaseType: DatabaseType,
    val filePath: String = "",
    val host: String = "localhost",
    val port: Int = databaseType.defaultPort,
    val databaseName: String = "",
    val username: String = "",
    val password: String = ""
) {
    /** 连接显示名称 */
    val displayName: String
        get() = when {
            filePath.isNotBlank() -> "${databaseType.displayName}: ${filePath.substringAfterLast('/')}"
            databaseName.isNotBlank() -> "${databaseType.displayName}: $databaseName"
            else -> databaseType.displayName
        }
}
