package io.github.yqy7.sqlui.view

import io.github.yqy7.sqlui.model.ConnectionInfo
import io.github.yqy7.sqlui.model.DatabaseType
import io.github.yqy7.sqlui.model.service.DatabaseService
import io.github.yqy7.sqlui.view.component.*
import io.github.yqy7.sqlui.viewmodel.BrowserViewModel
import io.github.yqy7.sqlui.viewmodel.ConnectionViewModel
import io.github.yqy7.sqlui.viewmodel.ResultViewModel
import io.github.yqy7.sqlui.util.AppScope
import javafx.application.Application
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyCodeCombination
import javafx.scene.input.KeyCombination
import javafx.scene.input.TransferMode
import javafx.scene.layout.BorderPane
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import javafx.stage.Stage
import kotlinx.coroutines.launch
import java.io.File

/**
 * 主窗口 —— 应用根布局。
 */
class MainWindow : Application() {

    // 共享服务层
    private val databaseService = DatabaseService()

    // ViewModels
    private val connectionViewModel = ConnectionViewModel(databaseService)
    private val browserViewModel = BrowserViewModel(databaseService)
    private val resultViewModel = ResultViewModel(databaseService)

    // UI 组件
    private val connectionToolbar = ConnectionToolbar(connectionViewModel)
    private val databaseBrowser = DatabaseBrowser(browserViewModel)
    private val tableStructurePanel = TableStructurePanel(browserViewModel)
    private val statusBar = StatusBar(resultViewModel)
    private val tabPane = TabPane()

    // 语法高亮器（延迟初始化）
    private val sqlHighlighter by lazy {
        io.github.yqy7.sqlui.util.SqlHighlighter()
    }

    private var tabCounter = 0

    override fun start(primaryStage: Stage) {
        val root = BorderPane()

        // 顶部：连接工具栏
        root.top = connectionToolbar

        // 左侧：数据库浏览器 + 表结构
        val leftPane = VBox(4.0).apply {
            children.addAll(databaseBrowser, tableStructurePanel)
            VBox.setVgrow(tableStructurePanel, Priority.ALWAYS)
            prefWidth = 280.0
        }

        // 中心：多标签页 SQL 编辑器
        setupTabPane()

        // 中间分隔面板
        val splitPane = javafx.scene.control.SplitPane().apply {
            items.addAll(leftPane, tabPane)
            setDividerPositions(0.25)
        }
        root.center = splitPane

        // 底部：状态栏
        root.bottom = statusBar

        // 场景
        val scene = Scene(root, 1280.0, 860.0).apply {
            // 加载样式表
            val styleUrl = javaClass.getResource("/io/github/yqy7/sqlui/view/theme/style.css")
            if (styleUrl != null) {
                stylesheets.add(styleUrl.toExternalForm())
            }

            // 全局快捷键
            setupGlobalShortcuts()
        }

        // 拖拽文件支持
        setupDragAndDrop(scene)

        // 双击表名：生成 SELECT 查询到当前编辑器
        databaseBrowser.onTableDoubleClick = { tableName ->
            val activeTab = tabPane.selectionModel.selectedItem as? SqlEditorTab
            activeTab?.editorViewModel?.generateSelectQuery(tableName)
        }

        // 监听连接状态更新
        connectionViewModel.isConnected.addListener { _, _, connected ->
            resultViewModel.updateConnectionStatus(
                if (connected) databaseService.activeProvider.value?.connectionUrl else null
            )
        }

        primaryStage.apply {
            title = "SQLui - 数据库管理工具"
            this.scene = scene
            minWidth = 900.0
            minHeight = 600.0
            show()
            centerOnScreen()
        }

        // 创建初始空白标签页
        addNewTab()
    }

    private fun setupTabPane() {
        tabPane.tabClosingPolicy = TabPane.TabClosingPolicy.ALL_TABS
        tabPane.isFocusTraversable = false

        // 双击空白区域创建新标签页
        tabPane.setOnMouseClicked { event ->
            if (event.clickCount == 2 && tabPane.selectionModel.selectedItem == null) {
                addNewTab()
            }
        }
    }

    private fun setupGlobalShortcuts() {
        tabPane.sceneProperty().addListener { _, _, newScene ->
            newScene?.let { scene ->
                // Ctrl+T: 新建标签页
                scene.accelerators[KeyCodeCombination(KeyCode.T, KeyCombination.CONTROL_DOWN)] = Runnable {
                    addNewTab()
                }

                // Ctrl+W: 关闭当前标签页
                scene.accelerators[KeyCodeCombination(KeyCode.W, KeyCombination.CONTROL_DOWN)] = Runnable {
                    val activeTab = tabPane.selectionModel.selectedItem
                    if (activeTab != null && tabPane.tabs.size > 1) {
                        tabPane.tabs.remove(activeTab)
                    }
                }

                // Ctrl+N: 新建连接
                scene.accelerators[KeyCodeCombination(KeyCode.N, KeyCombination.CONTROL_DOWN)] = Runnable {
                    ConnectionDialog(connectionViewModel).showAndWait()
                }
            }
        }
    }

    private fun setupDragAndDrop(scene: Scene) {
        scene.setOnDragOver { event ->
            if (event.dragboard.hasFiles()) {
                val hasDbFile = event.dragboard.files.any { file ->
                    DatabaseType.fromFileName(file.name) != null
                }
                if (hasDbFile) {
                    event.acceptTransferModes(TransferMode.LINK)
                }
            }
            event.consume()
        }

        scene.setOnDragDropped { event ->
            val db = event.dragboard
            var success = false

            if (db.hasFiles()) {
                for (file in db.files) {
                    val type = DatabaseType.fromFileName(file.name)
                    if (type != null) {
                        val info = ConnectionInfo(
                            databaseType = type,
                            filePath = file.absolutePath
                        )

                        // 弹出确认对话框
                        val alert = Alert(
                            Alert.AlertType.CONFIRMATION,
                            "拖拽打开数据库:\n" +
                                "类型: ${type.displayName}\n" +
                                "文件: ${file.absolutePath}"
                        )
                        alert.title = "打开数据库"
                        alert.headerText = "确认打开数据库文件？"

                        val result = alert.showAndWait()
                        if (result.isPresent && result.get() == ButtonType.OK) {
                            connectionViewModel.openConnection(info)
                            success = true
                        }
                    }
                }
            }

            event.isDropCompleted = success
            event.consume()
        }
    }

    /** 添加新的 SQL 编辑器标签页 */
    fun addNewTab(): SqlEditorTab {
        tabCounter++
        val tab = SqlEditorTab(databaseService, "Query $tabCounter")
        tab.configureHighlighting(sqlHighlighter)
        tab.handleExecuteShortcut()

        // 关闭标签页时的处理：保留至少一个标签页
        tab.setOnClosed {
            if (tabPane.tabs.isEmpty()) {
                addNewTab()
            }
        }

        tabPane.tabs.add(tab)
        tabPane.selectionModel.select(tab)
        return tab
    }

    override fun stop() {
        AppScope.scope.launch {
            databaseService.closeConnection()
        }
        AppScope.shutdown()
    }
}
