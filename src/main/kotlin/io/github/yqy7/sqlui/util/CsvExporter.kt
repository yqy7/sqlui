package io.github.yqy7.sqlui.util

import io.github.yqy7.sqlui.model.QueryResult
import java.io.File

/**
 * 将查询结果导出为 CSV 文件。
 */
object CsvExporter {

    /**
     * 导出查询结果到 CSV 文件。
     * @return 导出成功返回文件路径，失败返回 null
     */
    fun export(result: QueryResult.Success, file: File): Boolean {
        return try {
            file.bufferedWriter().use { writer ->
                // 写列头
                writer.write(result.columns.joinToString(",") { escapeCsvField(it) })
                writer.newLine()

                // 写数据行
                for (row in result.rows) {
                    writer.write(
                        row.joinToString(",") { cell ->
                            escapeCsvField(cell?.toString() ?: "NULL")
                        }
                    )
                    writer.newLine()
                }
            }
            true
        } catch (e: Exception) {
            System.err.println("CSV 导出失败: ${e.message}")
            false
        }
    }

    /** CSV 字段转义：包含逗号、引号或换行时用引号包裹 */
    private fun escapeCsvField(field: String): String {
        return when {
            field.contains(",") || field.contains("\"") ||
                field.contains("\n") || field.contains("\r") ->
                "\"${field.replace("\"", "\"\"")}\""
            else -> field
        }
    }
}
