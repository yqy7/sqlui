package io.github.yqy7.sqlui.model

/**
 * 通用 UI 状态封装。
 */
sealed class UiState<out T> {
    /** 空闲 / 初始状态 */
    data object Idle : UiState<Nothing>()

    /** 加载中 */
    data object Loading : UiState<Nothing>()

    /** 成功 */
    data class Success<T>(val data: T) : UiState<T>()

    /** 失败 */
    data class Error(val message: String) : UiState<Nothing>()
}
