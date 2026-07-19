package io.github.yqy7.sqlui.util

import org.fxmisc.richtext.CodeArea
import org.fxmisc.richtext.model.StyleSpans
import org.fxmisc.richtext.model.StyleSpansBuilder
import java.util.regex.Pattern

/**
 * SQL 语法高亮器 —— 为 RichTextFX CodeArea 配置关键字/字符串/数字/注释着色。
 */
class SqlHighlighter {

    private val keywords = setOf(
        "SELECT", "FROM", "WHERE", "AND", "OR", "NOT", "IN", "IS", "NULL",
        "INSERT", "INTO", "VALUES", "UPDATE", "SET", "DELETE",
        "CREATE", "ALTER", "DROP", "TABLE", "INDEX", "VIEW", "DATABASE",
        "JOIN", "INNER", "LEFT", "RIGHT", "OUTER", "CROSS", "ON",
        "ORDER", "BY", "GROUP", "HAVING", "ASC", "DESC",
        "LIMIT", "OFFSET", "AS", "DISTINCT", "ALL", "UNION",
        "COUNT", "SUM", "AVG", "MAX", "MIN",
        "CASE", "WHEN", "THEN", "ELSE", "END",
        "LIKE", "BETWEEN", "EXISTS",
        "PRIMARY", "KEY", "FOREIGN", "REFERENCES", "CONSTRAINT",
        "DEFAULT", "CHECK", "UNIQUE", "CASCADE",
        "INTEGER", "INT", "VARCHAR", "TEXT", "BOOLEAN", "BIGINT",
        "FLOAT", "DOUBLE", "DECIMAL", "DATE", "TIMESTAMP", "BLOB",
        "AUTO_INCREMENT", "SERIAL", "IDENTITY",
        "IF", "COMMIT", "ROLLBACK", "BEGIN", "TRANSACTION",
        "EXPLAIN", "ANALYZE", "VACUUM", "PRAGMA"
    )

    private val keywordPattern: Pattern = Pattern.compile(
        "\\b(${keywords.joinToString("|")})\\b",
        Pattern.CASE_INSENSITIVE
    )
    private val stringPattern: Pattern = Pattern.compile("'[^']*'")
    private val numberPattern: Pattern = Pattern.compile("\\b\\d+(\\.\\d+)?\\b")
    private val commentPattern: Pattern = Pattern.compile("--[^\n]*")
    private val blockCommentPattern: Pattern = Pattern.compile("/\\*[\\s\\S]*?\\*/")
    private val parenPattern: Pattern = Pattern.compile("[()]")
    private val semicolonPattern: Pattern = Pattern.compile(";")

    private val combinedPattern: Pattern = Pattern.compile(
        "(?<BLOCKCOMMENT>${blockCommentPattern.pattern()})" +
            "|(?<COMMENT>${commentPattern.pattern()})" +
            "|(?<STRING>${stringPattern.pattern()})" +
            "|(?<KEYWORD>${keywordPattern.pattern()})" +
            "|(?<NUMBER>${numberPattern.pattern()})" +
            "|(?<PAREN>${parenPattern.pattern()})" +
            "|(?<SEMICOLON>${semicolonPattern.pattern()})"
    )

    /** 将语法高亮应用到指定 CodeArea */
    fun applyTo(codeArea: CodeArea) {
        codeArea.multiPlainChanges()
            .subscribe { _ ->
                try {
                    codeArea.setStyleSpans(0, computeHighlighting(codeArea.text))
                } catch (_: Exception) {
                    // 忽略高亮异常
                }
            }
    }

    /** 计算整个文本的 StyleSpans */
    fun computeHighlighting(text: String): StyleSpans<Collection<String>> {
        val matcher = combinedPattern.matcher(text)
        val spansBuilder = StyleSpansBuilder<Collection<String>>()
        var end = 0

        while (matcher.find()) {
            // 默认样式文本
            if (matcher.start() > end) {
                spansBuilder.add(emptyList<String>(), matcher.start() - end)
            }

            val styleClass: Collection<String> = when {
                matcher.group("BLOCKCOMMENT") != null -> listOf("comment")
                matcher.group("COMMENT") != null -> listOf("comment")
                matcher.group("STRING") != null -> listOf("string")
                matcher.group("KEYWORD") != null -> listOf("keyword")
                matcher.group("NUMBER") != null -> listOf("number")
                matcher.group("PAREN") != null -> listOf("paren")
                matcher.group("SEMICOLON") != null -> listOf("semicolon")
                else -> listOf<String>()
            }

            spansBuilder.add(styleClass, matcher.end() - matcher.start())
            end = matcher.end()
        }

        spansBuilder.add(emptyList<String>(), text.length - end)
        return spansBuilder.create()
    }
}
