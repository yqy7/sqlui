package io.github.yqy7.sqlui.model

/**
 * 数据库表/视图元信息。
 */
data class TableInfo(
    val name: String,
    val schema: String = "PUBLIC",
    val type: String = "TABLE"  // TABLE, VIEW, SYSTEM TABLE, etc.
)
