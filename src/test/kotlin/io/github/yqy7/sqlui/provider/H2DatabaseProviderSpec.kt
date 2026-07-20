package io.github.yqy7.sqlui.provider

import io.github.yqy7.sqlui.model.DatabaseType
import io.github.yqy7.sqlui.model.QueryResult
import io.github.yqy7.sqlui.model.provider.H2DatabaseProvider
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.beInstanceOf

class H2DatabaseProviderSpec : ShouldSpec({

    context("H2DatabaseProvider") {

        should("成功连接 H2 内存数据库") {
            val provider = H2DatabaseProvider()
            provider.createInMemory("conn_test")
            provider.isConnected shouldBe true
            provider.databaseType shouldBe DatabaseType.H2
            provider.disconnect()
        }

        should("获取表列表为空（空库）") {
            val provider = H2DatabaseProvider()
            provider.createInMemory("empty_test")
            provider.getTables() shouldHaveSize 0
            provider.disconnect()
        }

        should("创建表后获取表列表") {
            val provider = H2DatabaseProvider()
            provider.createInMemory("table_test")
            provider.executeUpdate("CREATE TABLE t_users (id INT PRIMARY KEY, name VARCHAR(100))")
            val tables = provider.getTables()
            tables shouldHaveSize 1
            tables[0].name shouldBe "T_USERS"
            // H2 返回 "BASE TABLE" 而非 "TABLE"
            tables[0].type shouldBe "BASE TABLE"
            provider.disconnect()
        }

        should("获取表的列信息") {
            val provider = H2DatabaseProvider()
            provider.createInMemory("column_test")
            provider.executeUpdate("CREATE TABLE t_cols (id INT PRIMARY KEY, name VARCHAR(100), email VARCHAR(200))")

            val columns = provider.getColumns("T_COLS")
            columns shouldHaveSize 3
            columns[0].name shouldBe "ID"
            columns[0].isPrimaryKey shouldBe true
            columns[1].name shouldBe "NAME"
            columns[2].name shouldBe "EMAIL"
            provider.disconnect()
        }

        should("执行 SELECT 查询返回正确结果") {
            val provider = H2DatabaseProvider()
            provider.createInMemory("select_test")
            provider.executeUpdate("CREATE TABLE t_sel (id INT PRIMARY KEY, name VARCHAR(100))")
            provider.executeUpdate("INSERT INTO t_sel VALUES (1, 'Alice')")
            provider.executeUpdate("INSERT INTO t_sel VALUES (2, 'Bob')")

            val result = provider.executeQuery("SELECT * FROM t_sel ORDER BY id")
            result shouldBe beInstanceOf<QueryResult.Success>()
            (result as QueryResult.Success).columns shouldBe listOf("ID", "NAME")
            result.rows shouldHaveSize 2
            result.rowCount shouldBe 2
            provider.disconnect()
        }

        should("执行 UPDATE 返回 UpdateSuccess") {
            val provider = H2DatabaseProvider()
            provider.createInMemory("update_test")
            provider.executeUpdate("CREATE TABLE t_upd (id INT PRIMARY KEY, name VARCHAR(100))")
            provider.executeUpdate("INSERT INTO t_upd VALUES (1, 'Alice')")

            val result = provider.executeQuery("UPDATE t_upd SET name = 'Bob' WHERE id = 1")
            result shouldBe beInstanceOf<QueryResult.UpdateSuccess>()
            (result as QueryResult.UpdateSuccess).affectedRows shouldBe 1
            provider.disconnect()
        }

        should("执行错误的 SQL 返回 Error") {
            val provider = H2DatabaseProvider()
            provider.createInMemory("err_test")
            val result = provider.executeQuery("SELECT * FROM nonexistent_table")
            result shouldBe beInstanceOf<QueryResult.Error>()
            (result as QueryResult.Error).message shouldContain "not found"
            provider.disconnect()
        }

        should("断开连接后 isConnected 为 false") {
            val provider = H2DatabaseProvider()
            provider.createInMemory("disc_test")
            provider.disconnect()
            provider.isConnected shouldBe false
        }

        should("未连接时 executeQuery 抛异常") {
            val provider = H2DatabaseProvider()
            shouldThrow<IllegalStateException> {
                provider.executeQuery("SELECT 1")
            }
        }
    }
})
