package io.github.yqy7.sqlui.model

/**
 * 查询 / 更新执行结果。
 */
sealed class QueryResult {

    /** SELECT 类查询成功 */
    data class Success(
        val columns: List<String>,
        val rows: List<List<Any?>>,
        val rowCount: Int,
        val executionTimeMs: Long
    ) : QueryResult()

    /** INSERT / UPDATE / DELETE 类操作成功 */
    data class UpdateSuccess(
        val affectedRows: Int,
        val executionTimeMs: Long
    ) : QueryResult()

    /** 查询失败 */
    data class Error(
        val message: String,
        val sqlState: String? = null
    ) : QueryResult()
}
