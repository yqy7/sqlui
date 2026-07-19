# SQLui

基于 Kotlin + JavaFX 的桌面数据库管理工具，支持 **H2 Database** 和 **SQLite** 两种文件型数据库。

## 功能特性

- 🔌 支持 H2（文件/内存模式）和 SQLite
- ✍️ SQL 编辑器，语法高亮（RichTextFX）
- 📑 多标签页，同时编辑多个查询
- 📊 查询结果动态表格展示
- 📋 表结构浏览（列名、类型、主键、默认值）
- 📥 拖拽 .db / .sqlite / .mv.db 文件直接打开
- 📤 查询结果导出 CSV
- ⌨️ 快捷键支持（Ctrl+Enter 执行、Ctrl+T 新建标签、Ctrl+N 新建连接）
- 🎨 纯代码构建 GUI，无 FXML

## 快速开始

### 环境要求

- JDK 25+
- Gradle 9.x（自动通过 wrapper 下载）

### 构建 & 运行

```bash
# 编译
./gradlew build

# 启动
./gradlew run
```

### 打包独立应用

```bash
# 方式一：独立分发包（JVM 模式，build/install/sqlui/）
./gradlew installDist

# 方式二：macOS .app 应用包（自带 JVM，build/jpackage/SQLui.app）
./gradlew jpackageApp
```

> **注意**：由于 JDK 25 + JavaFX 26 暂时不被 GraalVM Native Image 完整支持（`JavaFXFeature` 与 JavaFX 26 存在兼容性问题），目前使用 `jpackage` 创建自带 JVM 的独立应用作为替代方案。待 GraalVM 原生镜像工具更新后，可重新启用 AOT 编译。

### 使用

1. 点击「新建连接」或按 `Ctrl+N`
2. 选择数据库类型（H2 / SQLite），选择或输入数据库文件路径
3. 点击确定，左侧树形列表显示数据库中的表和视图
4. 在 SQL 编辑器中输入查询，按 `Ctrl+Enter` 执行
5. 双击左侧表名可快速生成 `SELECT * FROM table LIMIT 100`

### 快捷键

| 快捷键 | 功能 |
|--------|------|
| Ctrl+Enter | 执行当前 SQL |
| Ctrl+T | 新建查询标签页 |
| Ctrl+W | 关闭当前标签页 |
| Ctrl+N | 新建数据库连接 |

## 扩展新数据库

1. 在 `DatabaseType` 枚举中添加新值
2. 实现 `DatabaseProvider` 接口
3. 在 `DatabaseService.createProvider()` 中注册
4. 添加对应 JDBC 驱动依赖

## 技术栈

| 组件 | 版本 |
|------|------|
| Kotlin JVM | 2.3.21 |
| JavaFX | 26.0.1 |
| H2 | 2.4.240 |
| SQLite JDBC | 3.53.2.0 |
| RichTextFX | 0.11.7 |
| kotlinx-coroutines | 1.10.1 |

## License

MIT
