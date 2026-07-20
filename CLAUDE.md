# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

SQLui — 基于 Kotlin + Swing (FlatLaf) 的桌面数据库管理工具，支持 H2 Database 和 SQLite，纯代码构建 GUI。

## 构建命令

```bash
./gradlew build          # 编译
./gradlew run            # 启动应用
./gradlew test           # 运行测试
./gradlew clean build    # 清理编译
./gradlew nativeCompile  # AOT 原生编译 → build/native/nativeCompile/sqlui
```

### Native Image 编译

需要 GraalVM JDK（推荐 Liberica NIK 25+）。编译产物在 `build/native/nativeCompile/`：

```bash
./gradlew nativeCompile
# 用启动脚本运行（自动设置 DYLD_LIBRARY_PATH）
./build/native/nativeCompile/sqlui.sh
# 或手动设置
DYLD_LIBRARY_PATH=build/native/nativeCompile ./build/native/nativeCompile/sqlui
```

**原理**: Native Image 不包含 HotSpot JVM，但 AWT 原生库依赖 `libjvm.dylib`。构建脚本自动：
1. 从 JDK `lib/` 和 `lib/server/` 复制需要的 `.dylib` 文件到输出目录
2. 生成 `sqlui.sh` 启动脚本设置 `DYLD_LIBRARY_PATH`

## 技术栈

- Kotlin JVM 2.3.21, JDK 25
- Swing + FlatLaf 3.5.4（现代化 Look & Feel）
- RSyntaxTextArea 3.5.2（SQL 语法高亮、代码折叠）
- H2 2.4.240, SQLite JDBC 3.53.2.0
- kotlinx-coroutines 1.10.1（异步查询 + Swing 线程桥接）
- Kotest 5.9.1（BDD 风格测试）

## 架构

MVVM + Service 分层：

```
View (Swing Components) → ViewModel (StateFlow) → Service (业务逻辑) → Provider (JDBC 抽象)
```

- **Provider**: `DatabaseProvider` 接口是扩展点，新增数据库类型只需实现接口 + 注册工厂
- **Service**: `DatabaseService` 通过 Mutex 串行化所有 DB 访问，保证 H2/SQLite 写安全
- **ViewModel**: 暴露 Kotlin `StateFlow`，View 层在协程中 collect 并切到 EDT 更新 UI
- **View**: 纯代码构建 Swing UI（`view/component/`），FlatLaf 提供现代主题

## 线程模型

| 线程 | 用途 |
|------|------|
| EDT (Event Dispatch Thread) | 所有 Swing 组件操作 |
| `AppScope.dbDispatcher` | 单线程 DB IO（保证 H2/SQLite 写安全） |
| `AppScope.scope` | ViewModel collect 协程 |
| `Dispatchers.Swing` | kotlinx-coroutines-swing 提供，自动切 EDT |

## 包结构

```
io.github.yqy7.sqlui
├── App.kt                          # 入口（SwingUtilities.invokeLater 启 MainWindow）
├── model/                          # 数据模型（纯 Kotlin，无 GUI 依赖）
│   ├── ConnectionInfo.kt           # 数据库连接信息
│   ├── DatabaseType.kt             # 支持的数据库类型枚举
│   ├── QueryResult.kt              # 查询结果 sealed class
│   ├── TableInfo.kt                # 表/视图元信息
│   ├── ColumnInfo.kt               # 列元信息
│   ├── UiState.kt                  # 通用 UI 状态封装
│   └── provider/
│       ├── DatabaseProvider.kt     # 数据库扩展接口
│       ├── H2DatabaseProvider.kt   # H2 实现（文件/内存模式）
│       └── SQLiteDatabaseProvider.kt  # SQLite 实现（文件模式）
├── service/
│   └── DatabaseService.kt          # 核心协调层（Mutex + StateFlow）
├── view/
│   ├── MainWindow.kt               # JFrame 主窗口（菜单栏 + 拖拽 + 快捷键）
│   ├── component/
│   │   ├── ConnectionDialog.kt     # 连接对话框 JDialog
│   │   ├── ConnectionToolbar.kt    # 工具栏 JToolBar
│   │   ├── DatabaseBrowser.kt      # 数据库浏览器（JTree + 表结构 JTable）
│   │   ├── SqlEditorTab.kt         # 单标签页 = RSyntaxTextArea + 结果 JTable
│   │   ├── ResultTablePanel.kt     # 查询结果表格
│   │   └── StatusBar.kt            # 底部状态栏
│   └── theme/
│       └── FlatLafTheme.kt         # FlatLaf 主题初始化
├── viewmodel/
│   ├── ConnectionViewModel.kt      # 连接管理（StateFlow）
│   ├── BrowserViewModel.kt         # 数据库浏览器（StateFlow）
│   ├── EditorViewModel.kt          # SQL 编辑器（StateFlow）
│   └── ResultViewModel.kt          # 状态栏信息（StateFlow）
└── util/
    ├── AppScope.kt                 # 全局协程作用域 + 调度器
    ├── CsvExporter.kt              # CSV 导出
    └── SqlHighlighter.kt           # RSyntaxTextArea SQL 配置
```

## 关键文件

| 文件 | 作用 |
|------|------|
| `App.kt` | 入口，初始化 FlatLaf 主题，启动 MainWindow |
| `view/MainWindow.kt` | 根布局（BorderLayout），菜单栏，拖拽支持，快捷键 |
| `service/DatabaseService.kt` | 核心协调层，Mutex 串行化 |
| `model/provider/DatabaseProvider.kt` | 数据库扩展接口 |
| `view/component/SqlEditorTab.kt` | 单标签页 = SQL 编辑器 + 结果表，Ctrl+Enter 执行 |
| `view/component/ConnectionDialog.kt` | 连接对话框（H2 文件/内存，SQLite 文件） |
| `util/SqlHighlighter.kt` | RSyntaxTextArea SQL 语法高亮配置 |
| `util/CsvExporter.kt` | CSV 导出 |

## 扩展新数据库

1. `DatabaseType` 枚举加值
2. 实现 `DatabaseProvider` 接口
3. 在 `DatabaseService.createProvider()` 的 `when` 分支中注册
4. 添加 JDBC 驱动依赖

## 测试

使用 Kotest ShouldSpec BDD 风格：

```kotlin
class H2DatabaseProviderSpec : ShouldSpec({
    context("H2DatabaseProvider") {
        should("成功连接 H2 内存数据库") {
            val provider = H2DatabaseProvider()
            provider.createInMemory("test")
            provider.isConnected shouldBe true
        }
    }
})
```

测试覆盖：
- `model/` — DatabaseType 枚举、ConnectionInfo
- `provider/` — H2DatabaseProvider（内存数据库，无需外部依赖）
- `service/` — DatabaseService（集成测试）
- `util/` — CsvExporter
