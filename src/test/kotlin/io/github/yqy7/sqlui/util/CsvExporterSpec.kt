package io.github.yqy7.sqlui.util

import io.github.yqy7.sqlui.model.QueryResult
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import java.io.File

class CsvExporterSpec : ShouldSpec({

    context("CsvExporter") {
        should("导出查询结果为 CSV 文件") {
            val result = QueryResult.Success(
                columns = listOf("ID", "NAME", "EMAIL"),
                rows = listOf(
                    listOf(1, "Alice", "alice@example.com"),
                    listOf(2, "Bob", "bob@test.com"),
                    listOf(3, "Charlie, Jr.", null)
                ),
                rowCount = 3,
                executionTimeMs = 42
            )

            val file = File.createTempFile("test_export_", ".csv")
            file.deleteOnExit()

            val success = CsvExporter.export(result, file)
            success shouldBe true

            val content = file.readText()
            content shouldBe
                "ID,NAME,EMAIL\n" +
                "1,Alice,alice@example.com\n" +
                "2,Bob,bob@test.com\n" +
                "3,\"Charlie, Jr.\",NULL\n"
        }

        should("空结果集导出成功") {
            val result = QueryResult.Success(
                columns = listOf("ID", "NAME"),
                rows = emptyList(),
                rowCount = 0,
                executionTimeMs = 0
            )

            val file = File.createTempFile("test_empty_", ".csv")
            file.deleteOnExit()

            val success = CsvExporter.export(result, file)
            success shouldBe true

            val content = file.readText()
            content shouldBe "ID,NAME\n"
        }
    }
})
