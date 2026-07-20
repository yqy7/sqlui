package io.github.yqy7.sqlui.view

import io.github.yqy7.sqlui.model.ConnectionInfo
import io.github.yqy7.sqlui.model.DatabaseType
import io.github.yqy7.sqlui.service.DatabaseService
import io.github.yqy7.sqlui.util.AppScope
import io.github.yqy7.sqlui.view.component.*
import io.github.yqy7.sqlui.view.theme.FlatLafTheme
import io.github.yqy7.sqlui.viewmodel.BrowserViewModel
import io.github.yqy7.sqlui.viewmodel.ConnectionViewModel
import io.github.yqy7.sqlui.viewmodel.ResultViewModel
import kotlinx.coroutines.launch
import java.awt.*
import java.awt.datatransfer.DataFlavor
import java.awt.event.*
import java.io.File
import javax.swing.*

/**
 * 主窗口 —— 应用根布局。
 *
 * 布局结构:
 * - NORTH  → 连接工具栏
 * - WEST   → 数据库浏览器（JTree + 表结构）
 * - CENTER → 多标签页 SQL 编辑器（JTabbedPane）
 * - SOUTH  → 状态栏
 */
class MainWindow : JFrame("SQLui") {

    // 共享服务层
    private val databaseService = DatabaseService()

    // ViewModels
    private val connectionViewModel = ConnectionViewModel(databaseService)
    private val browserViewModel = BrowserViewModel(databaseService)
    private val resultViewModel = ResultViewModel(databaseService)

    // UI 组件
    private val connectionToolbar = ConnectionToolbar(connectionViewModel)
    private val databaseBrowser = DatabaseBrowser(browserViewModel)
    private val statusBar = StatusBar(resultViewModel)
    private val tabbedPane = JTabbedPane()

    private var tabCounter = 0

    init {
        FlatLafTheme.setup()

        defaultCloseOperation = EXIT_ON_CLOSE
        setSize(1280, 860)
        setLocationRelativeTo(null)

        // 菜单栏
        jMenuBar = createMenuBar()

        // 布局
        val contentPane = JPanel(BorderLayout())
        contentPane.add(connectionToolbar, BorderLayout.NORTH)

        // 中央：JSplitPane(数据库浏览器 | 标签页编辑器)
        val centerSplitPane = JSplitPane(JSplitPane.HORIZONTAL_SPLIT, databaseBrowser, tabbedPane).apply {
            dividerLocation = 300
            resizeWeight = 0.25
        }
        contentPane.add(centerSplitPane, BorderLayout.CENTER)
        contentPane.add(statusBar, BorderLayout.SOUTH)

        setContentPane(contentPane)

        // 新建第一个标签页
        newTab()

        // 监听连接状态更新状态栏
        AppScope.scope.launch {
            connectionViewModel.isConnected.collect { connected ->
                SwingUtilities.invokeLater {
                    if (connected) {
                        resultViewModel.updateConnectionStatus("已连接")
                    } else {
                        resultViewModel.updateConnectionStatus(null)
                    }
                }
            }
        }

        // 监听最近连接的 URL
        AppScope.scope.launch {
            databaseService.activeProvider.collect { provider ->
                SwingUtilities.invokeLater {
                    val url = provider?.connectionUrl
                    resultViewModel.updateConnectionStatus(url)
                }
            }
        }

        // 双击表名 → 生成 SELECT 查询
        addTableDoubleClickListener()
    }

    private fun createMenuBar(): JMenuBar {
        val menuBar = JMenuBar()

        // 文件菜单
        val fileMenu = JMenu("文件")
        fileMenu.mnemonic = KeyEvent.VK_F

        fileMenu.add(JMenuItem("新建连接").apply {
            accelerator = KeyStroke.getKeyStroke(KeyEvent.VK_N, KeyEvent.CTRL_DOWN_MASK)
            addActionListener { connectionToolbar.showConnectionDialog(this@MainWindow) }
        })
        fileMenu.add(JMenuItem("断开连接").apply {
            addActionListener { connectionViewModel.closeConnection() }
        })
        fileMenu.addSeparator()
        fileMenu.add(JMenuItem("退出").apply {
            accelerator = KeyStroke.getKeyStroke(KeyEvent.VK_Q, KeyEvent.CTRL_DOWN_MASK)
            addActionListener { dispose() }
        })

        // 查询菜单
        val queryMenu = JMenu("查询")
        queryMenu.mnemonic = KeyEvent.VK_Q

        queryMenu.add(JMenuItem("新建标签页").apply {
            accelerator = KeyStroke.getKeyStroke(KeyEvent.VK_T, KeyEvent.CTRL_DOWN_MASK)
            addActionListener { newTab() }
        })
        queryMenu.add(JMenuItem("关闭标签页").apply {
            accelerator = KeyStroke.getKeyStroke(KeyEvent.VK_W, KeyEvent.CTRL_DOWN_MASK)
            addActionListener { closeCurrentTab() }
        })
        queryMenu.addSeparator()
        queryMenu.add(JMenuItem("执行 SQL").apply {
            accelerator = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, KeyEvent.CTRL_DOWN_MASK)
            addActionListener { executeCurrentQuery() }
        })

        // 工具菜单
        val toolsMenu = JMenu("工具")
        toolsMenu.add(JMenuItem("H2 内存数据库").apply {
            addActionListener { connectionViewModel.createH2InMemory("quick") }
        })

        menuBar.add(fileMenu)
        menuBar.add(queryMenu)
        menuBar.add(toolsMenu)

        return menuBar
    }

    /** 创建新标签页 */
    fun newTab() {
        tabCounter++
        val tab = SqlEditorTab(databaseService, browserViewModel, "Query $tabCounter")
        tabbedPane.addTab(tab.getTitle(), tab)
        tabbedPane.selectedIndex = tabbedPane.tabCount - 1
        tabbedPane.setTabComponentAt(tabbedPane.tabCount - 1, createTabHeader(tab))

        // 双击表名时插入表名或生成查询
        addTabTableListener(tab)
    }

    private fun createTabHeader(tab: SqlEditorTab): JPanel {
        val panel = JPanel(FlowLayout(FlowLayout.LEFT, 0, 0))
        panel.isOpaque = false
        panel.add(JLabel(tab.getTitle()))
        panel.add(Box.createHorizontalStrut(4))
        panel.add(JButton("✕").apply {
            isContentAreaFilled = false
            isBorderPainted = false
            font = font.deriveFont(10f)
            preferredSize = Dimension(16, 16)
            addActionListener {
                tabbedPane.remove(tab)
                if (tabbedPane.tabCount == 0) newTab()
            }
        })
        return panel
    }

    /** 关闭当前标签页 */
    fun closeCurrentTab() {
        val index = tabbedPane.selectedIndex
        if (index >= 0) {
            tabbedPane.removeTabAt(index)
            if (tabbedPane.tabCount == 0) newTab()
        }
    }

    /** 执行当前标签页的 SQL 查询 */
    fun executeCurrentQuery() {
        val tab = tabbedPane.selectedComponent as? SqlEditorTab ?: return
        tab.executeQuery()
    }

    /** 双击表名时插入表名并生成查询 */
    private fun addTableDoubleClickListener() {
        // 在 DatabaseBrowser 中双击表名时通知当前标签页
        AppScope.scope.launch {
            browserViewModel.selectedTable.collect { tableInfo ->
                SwingUtilities.invokeLater {
                    if (tableInfo != null) {
                        val tab = tabbedPane.selectedComponent as? SqlEditorTab ?: return@invokeLater
                        tab.generateSelectQuery(tableInfo.name)
                        resultViewModel.updateActiveTabTitle("${tab.getTitle()} — ${tableInfo.name}")
                    }
                }
            }
        }
    }

    private fun addTabTableListener(tab: SqlEditorTab) {
        // 每个标签页创建独立的监听
        AppScope.scope.launch {
            browserViewModel.selectedTable.collect { tableInfo ->
                SwingUtilities.invokeLater {
                    if (tableInfo != null && tabbedPane.selectedComponent == tab) {
                        tab.generateSelectQuery(tableInfo.name)
                    }
                }
            }
        }
    }

    companion object {
        /**
         * 通过拖拽 .db/.sqlite/.mv.db 文件快速打开数据库。
         * 设置 TransferHandler 支持文件拖放。
         */
        fun enableFileDrop(frame: JFrame, onDrop: (File) -> Unit) {
            frame.transferHandler = object : TransferHandler() {
                override fun canImport(support: TransferSupport): Boolean {
                    return support.isDataFlavorSupported(DataFlavor.javaFileListFlavor)
                }

                @Suppress("unchecked")
                override fun importData(support: TransferSupport): Boolean {
                    if (!canImport(support)) return false
                    try {
                        val files = support.transferable
                            .getTransferData(DataFlavor.javaFileListFlavor) as? List<File>
                            ?: return false
                        val dbFile = files.firstOrNull { file ->
                            val ext = file.name.lowercase()
                            DatabaseType.entries.any { dbType ->
                                dbType.fileExtensions.any { ext.endsWith(".$it") }
                            }
                        } ?: return false
                        onDrop(dbFile)
                        return true
                    } catch (_: Exception) {
                        return false
                    }
                }
            }
        }
    }
}
