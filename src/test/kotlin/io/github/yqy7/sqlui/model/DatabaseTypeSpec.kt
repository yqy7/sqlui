package io.github.yqy7.sqlui.model

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class DatabaseTypeSpec : ShouldSpec({

    context("DatabaseType.fromFileName") {
        should("识别 .mv.db 文件为 H2") {
            DatabaseType.fromFileName("test.mv.db") shouldBe DatabaseType.H2
        }

        should("识别 .h2.db 文件为 H2") {
            DatabaseType.fromFileName("mydb.h2.db") shouldBe DatabaseType.H2
        }

        should("识别 .db 文件为 SQLite") {
            DatabaseType.fromFileName("data.db") shouldBe DatabaseType.SQLITE
        }

        should("识别 .sqlite 文件为 SQLite") {
            DatabaseType.fromFileName("app.sqlite") shouldBe DatabaseType.SQLITE
        }

        should("识别 .sqlite3 文件为 SQLite") {
            DatabaseType.fromFileName("db.sqlite3") shouldBe DatabaseType.SQLITE
        }

        should("不识别未知扩展名") {
            DatabaseType.fromFileName("readme.txt") shouldBe null
        }

        should("优先匹配更具体的扩展名 (.mv.db 优先于 .db)") {
            DatabaseType.fromFileName("test.mv.db") shouldBe DatabaseType.H2
        }
    }

    context("ConnectionInfo.displayName") {
        should("文件路径模式显示文件名") {
            val info = ConnectionInfo(
                databaseType = DatabaseType.SQLITE,
                filePath = "/Users/test/mydb.sqlite"
            )
            info.displayName shouldBe "SQLite: mydb.sqlite"
        }

        should("内存数据库模式显示数据库名") {
            val info = ConnectionInfo(
                databaseType = DatabaseType.H2,
                databaseName = "testdb"
            )
            info.displayName shouldBe "H2 Database: testdb"
        }

        should("无路径无数据库名时显示类型名") {
            val info = ConnectionInfo(DatabaseType.H2)
            info.displayName shouldBe "H2 Database"
        }
    }
})
