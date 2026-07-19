package io.github.yqy7.sqlui.model

/**
 * 支持的数据库类型枚举。
 * 扩展新数据库时在此添加新值。
 */
enum class DatabaseType(
    val displayName: String,
    val driverClass: String,
    val defaultPort: Int = 0,
    val fileExtensions: List<String> = emptyList()
) {
    H2(
        displayName = "H2 Database",
        driverClass = "org.h2.Driver",
        fileExtensions = listOf("mv.db", "h2.db")
    ),
    SQLITE(
        displayName = "SQLite",
        driverClass = "org.sqlite.JDBC",
        fileExtensions = listOf("db", "sqlite", "sqlite3", "s3db")
    );

    companion object {
        /**
         * 根据文件名推断数据库类型。
         * 使用完整文件名匹配（而非仅扩展名），以正确识别 .mv.db 这样的多段扩展名。
         * 按扩展名长度降序匹配，优先匹配更具体的模式。
         */
        fun fromFileName(fileName: String): DatabaseType? {
            val lower = fileName.lowercase()
            // 按扩展名长度降序排列，优先匹配 "mv.db" 再匹配 "db"
            val allTypes = entries.flatMap { type ->
                type.fileExtensions.map { ext -> type to ext }
            }.sortedByDescending { it.second.length }

            return allTypes.firstOrNull { (_, ext) ->
                lower.endsWith(".$ext")
            }?.first
        }
    }
}
