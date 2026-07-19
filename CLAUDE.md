# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

SQLui — 基于 Kotlin + JavaFX 的桌面数据库管理工具，支持 H2 Database 和 SQLite，纯代码构建 GUI（不使用 FXML）。

## 构建命令

```bash
./gradlew build          # 编译
./gradlew run            # 启动应用（JVM 模式）
./gradlew clean build    # 清理编译
./gradlew nativeCompile  # GraalVM Native Image 编译（需 GraalVM JDK）
```

Native Image 产物位于 `build/native/nativeCompile/sqlui`。
Native Image 配置文件位于 `src/main/resources/META-INF/native-image/io.github.yqy7/sqlui/`。

## 技术栈

- Kotlin JVM 2.3.21, JDK 25
- JavaFX 26.0.1 (javafx.controls, javafx.graphics, javafx.base)
- H2 2.4.240, SQLite JDBC 3.53.2.0
- RichTextFX 0.11.7 (SQL 语法高亮)
- kotlinx-coroutines 1.10.1 (异步查询 + JavaFX 线程桥接)

## 架构

MVVM + Service 分层：

```
View (JavaFX Components) → ViewModel (Observable) → Service (业务逻辑) → Provider (JDBC 抽象)
```

- **Provider**: `DatabaseProvider` 接口是扩展点，新增数据库类型只需实现接口 + 注册工厂
- **Service**: `DatabaseService` 通过 Mutex 串行化所有 DB 访问，保证 H2/SQLite 写安全
- **ViewModel**: 暴露 JavaFX Observable 属性，通过 `AppScope.scope` 启动协程
- **View**: 全部用代码构建 UI（`view/component/`），辅助扩展函数在 `view/util/JavaFXExtensions.kt`

## 关键文件

| 文件 | 作用 |
|------|------|
| `App.kt` | 入口，启动 JavaFX |
| `view/MainWindow.kt` | 根布局，拖拽支持，快捷键 |
| `model/service/DatabaseService.kt` | 核心协调层 |
| `model/provider/DatabaseProvider.kt` | 数据库扩展接口 |
| `view/component/SqlEditorTab.kt` | 单标签页 = 编辑器 + 结果表 |
| `view/component/ConnectionDialog.kt` | 连接对话框 |
| `util/SqlHighlighter.kt` | SQL 语法高亮（RichTextFX） |
| `util/CsvExporter.kt` | CSV 导出 |

## 扩展新数据库

1. `DatabaseType` 枚举加值
2. 实现 `DatabaseProvider` 接口
3. 在 `DatabaseService.createProvider()` 的 `when` 分支中注册
4. 添加 JDBC 驱动依赖
