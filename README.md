# SQLui

基于 Kotlin + Swing (FlatLaf) 的桌面数据库管理工具，支持 **H2 Database** 和 **SQLite** 两种数据库。

## 功能特性

- 🔌 支持 H2（文件/内存模式）和 SQLite
- ✍️ SQL 编辑器，语法高亮（RSyntaxTextArea），代码折叠
- 📑 多标签页，同时编辑多个查询
- 📊 查询结果表格展示（JTable），支持排序
- 📋 表结构浏览（列名、类型、主键、默认值）
- 📥 拖拽 .db / .sqlite / .mv.db 文件直接打开
- 📤 查询结果导出 CSV
- ⌨️ 快捷键支持（Ctrl+Enter 执行、Ctrl+T 新建标签、Ctrl+W 关闭标签、Ctrl+N 新建连接）
- 🎨 FlatLaf 现代化主题，纯代码构建 GUI

## 快速开始

### 环境要求

- JDK 25+
- Gradle（自动通过 wrapper 下载）

### 构建 & 运行

```bash
# 编译
./gradlew build

# 启动
./gradlew run

# 运行测试
./gradlew test

# GraalVM Native Image AOT 编译（生成独立原生可执行文件）
./gradlew nativeCompile
# 运行
./build/native/nativeCompile/sqlui.sh
```

> **Native Image 编译**：需要 Liberica NIK 25+ 或 GraalVM JDK 22+。Swing 是 JDK 内置框架，编译后无需 JVM 即可运行（~80MB 可执行文件 + ~24MB 原生库）。
> ```bash
> sdk install java 25.0.3.r25-nik
> ./gradlew nativeCompile
> ./build/native/nativeCompile/sqlui.sh
> ```

### 使用

1. 点击「新建连接」或按 `Ctrl+N`
2. 选择数据库类型（H2 / SQLite），选择或输入数据库文件路径
   - H2 内存模式：留空文件路径，输入数据库名即可
3. 点击连接，左侧树形列表显示数据库中的表和视图
4. 在 SQL 编辑器中输入查询，按 `Ctrl+Enter` 执行
5. 双击左侧表名可快速生成 `SELECT * FROM table LIMIT 100`

### 快捷键

| 快捷键 | 功能 |
|--------|------|
| Ctrl+Enter | 执行当前 SQL |
| Ctrl+T | 新建查询标签页 |
| Ctrl+W | 关闭当前标签页 |
| Ctrl+N | 新建数据库连接 |
| Ctrl+S | 保存 SQL 到文件 |
| Ctrl+Q | 退出应用 |

## 扩展新数据库

1. 在 `DatabaseType` 枚举中添加新值
2. 实现 `DatabaseProvider` 接口
3. 在 `DatabaseService.createProvider()` 中注册
4. 添加对应 JDBC 驱动依赖

## 技术栈

| 组件 | 版本 | 用途 |
|------|------|------|
| Kotlin JVM | 2.3.21 | 编程语言 |
| Swing | JDK 内置 | GUI 框架 |
| FlatLaf | 3.5.4 | 现代化 Look & Feel |
| RSyntaxTextArea | 3.5.2 | SQL 语法高亮 |
| H2 | 2.4.240 | H2 数据库引擎 |
| SQLite JDBC | 3.53.2.0 | SQLite JDBC 驱动 |
| kotlinx-coroutines | 1.10.1 | 异步协程 |
| Kotest | 5.9.1 | BDD 测试框架 |

## 架构

MVVM + Service 分层：

```
View (Swing Components) → ViewModel (StateFlow) → Service (业务逻辑) → Provider (JDBC 抽象)
```

- **View**: Swing 组件（JFrame / JPanel / JTable / JTree / RSyntaxTextArea），纯代码构建
- **ViewModel**: 暴露 Kotlin StateFlow，View 在协程中 collect 并切 EDT 更新
- **Service**: Mutex 串行化 DB 访问，StateFlow 广播状态
- **Provider**: JDBC 抽象层，新增数据库只需实现接口

## License

MIT
