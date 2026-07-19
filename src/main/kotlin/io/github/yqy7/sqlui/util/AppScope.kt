package io.github.yqy7.sqlui.util

import kotlinx.coroutines.*

/**
 * 全局协程作用域持有者。
 * 使用 SupervisorJob 确保单个协程失败不影响其他协程。
 */
@OptIn(ExperimentalCoroutinesApi::class)
object AppScope {
    private val job = SupervisorJob()
    private val exceptionHandler = CoroutineExceptionHandler { _, e ->
        System.err.println("未捕获的协程异常: ${e.message}")
        e.printStackTrace()
    }

    val scope: CoroutineScope = CoroutineScope(job + Dispatchers.Default + exceptionHandler)

    /** 数据库 IO 专用调度器（单线程，保证 H2/SQLite 串行写访问） */
    val dbDispatcher: CoroutineDispatcher = newSingleThreadContext("db-io")

    fun shutdown() {
        job.cancel()
        dbDispatcher.cancel()
    }
}
