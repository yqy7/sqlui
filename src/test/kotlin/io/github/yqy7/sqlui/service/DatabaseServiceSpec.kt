package io.github.yqy7.sqlui.service

import io.github.yqy7.sqlui.model.ConnectionInfo
import io.github.yqy7.sqlui.model.DatabaseType
import io.github.yqy7.sqlui.model.QueryResult
import io.github.yqy7.sqlui.model.UiState
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.beInstanceOf
import kotlinx.coroutines.test.runTest

class DatabaseServiceSpec : ShouldSpec({

    context("DatabaseService") {
        should("通过 openConnection 连接 H2 内存数据库") {
            runTest {
                val service = DatabaseService()
                val result = service.openConnection(
                    ConnectionInfo(databaseType = DatabaseType.H2, databaseName = "svc_conn")
                )
                result shouldBe beInstanceOf<UiState.Success<ConnectionInfo>>()
                service.isConnected shouldBe true
                service.closeConnection()
            }
        }

        should("连接后获取表列表为空") {
            runTest {
                val service = DatabaseService()
                service.openConnection(
                    ConnectionInfo(databaseType = DatabaseType.H2, databaseName = "svc_empty")
                )
                val tables = service.refreshTables()
                tables shouldHaveSize 0
                service.closeConnection()
            }
        }

        should("执行 CREATE TABLE + SELECT 查询") {
            runTest {
                val service = DatabaseService()
                service.openConnection(
                    ConnectionInfo(databaseType = DatabaseType.H2, databaseName = "svc_query")
                )
                service.executeUpdate("CREATE TABLE svc_test (id INT PRIMARY KEY, val VARCHAR(50))")
                service.executeUpdate("INSERT INTO svc_test VALUES (1, 'hello')")

                val result = service.executeQuery("SELECT * FROM svc_test")
                result shouldBe beInstanceOf<QueryResult.Success>()
                (result as QueryResult.Success).rows shouldHaveSize 1
                result.columns shouldBe listOf("ID", "VAL")

                service.closeConnection()
            }
        }

        should("closeConnection 后 isConnected 为 false") {
            runTest {
                val service = DatabaseService()
                service.openConnection(
                    ConnectionInfo(databaseType = DatabaseType.H2, databaseName = "svc_close")
                )
                service.closeConnection()
                service.isConnected shouldBe false
            }
        }

        should("createH2InMemory 快捷方法正常连接") {
            runTest {
                val service = DatabaseService()
                val result = service.createH2InMemory("svc_quick")
                result shouldBe beInstanceOf<UiState.Success<ConnectionInfo>>()
                service.isConnected shouldBe true
                service.closeConnection()
            }
        }

        should("未连接时 executeQuery 返回 Error") {
            runTest {
                val service = DatabaseService()
                val result = service.executeQuery("SELECT 1")
                result shouldBe beInstanceOf<QueryResult.Error>()
                (result as QueryResult.Error).message shouldBe "未连接到数据库"
            }
        }
    }
})
