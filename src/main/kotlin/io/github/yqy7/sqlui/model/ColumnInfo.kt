package io.github.yqy7.sqlui.model

/**
 * 表列元信息。
 */
data class ColumnInfo(
    val name: String,
    val dataType: String,
    val nullable: Boolean = true,
    val isPrimaryKey: Boolean = false,
    val defaultValue: String? = null,
    val ordinalPosition: Int = 0,
    val size: Int = 0
)
