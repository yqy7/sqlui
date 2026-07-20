package io.github.yqy7.sqlui.util

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea
import org.fife.ui.rsyntaxtextarea.SyntaxConstants
import org.fife.ui.rsyntaxtextarea.Theme

/**
 * SQL 编辑器配置工具。
 * 基于 RSyntaxTextArea 提供 SQL 语法高亮、代码折叠、行号显示等功能。
 */
object SqlHighlighter {

    /**
     * 配置 RSyntaxTextArea 为 SQL 编辑器。
     * 设置语法样式、行号、代码折叠、制表符宽度等。
     */
    fun configure(editor: RSyntaxTextArea) {
        editor.apply {
            syntaxEditingStyle = SyntaxConstants.SYNTAX_STYLE_SQL
            isCodeFoldingEnabled = true
            isBracketMatchingEnabled = true
            isAutoIndentEnabled = true
            closeCurlyBraces = false
            tabSize = 4
            lineWrap = false
            antiAliasingEnabled = true
            paintTabLines = true
            highlightCurrentLine = true
        }
    }

    /**
     * 应用暗色主题（适配 FlatLaf Dark）。
     */
    fun applyDarkTheme(editor: RSyntaxTextArea) {
        try {
            val theme = Theme.load(RSyntaxTextArea::class.java.getResourceAsStream(
                "/org/fife/ui/rsyntaxtextarea/themes/dark.xml"
            ))
            theme.apply(editor)
        } catch (_: Exception) {
            // 主题加载失败时使用默认样式
        }
    }
}
