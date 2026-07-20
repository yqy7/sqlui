package io.github.yqy7.sqlui.view.component

import io.github.yqy7.sqlui.model.ConnectionInfo
import io.github.yqy7.sqlui.model.DatabaseType
import java.awt.*
import javax.swing.*

/**
 * 数据库连接对话框。
 * 支持选择 H2（文件/内存模式）或 SQLite 文件。
 */
class ConnectionDialog(
    owner: JFrame,
    private val onConnect: (ConnectionInfo) -> Unit
) : JDialog(owner, "新建数据库连接", true) {

    private val typeCombo = JComboBox(DatabaseType.entries.toTypedArray())
    private val filePathField = JTextField(30)
    private val browseButton = JButton("浏览...")
    private val dbNameField = JTextField(15)
    private val dbNameLabel = JLabel("数据库名:")
    private val usernameField = JTextField("sa", 15)
    private val passwordField = JPasswordField(15)
    private val h2HintLabel = JLabel("留空文件路径，输入数据库名即启用 H2 内存模式")
    private val sqliteHintLabel = JLabel("选择或输入 .db / .sqlite 文件路径")

    init {
        defaultCloseOperation = DISPOSE_ON_CLOSE
        isResizable = false

        val content = JPanel(BorderLayout(10, 10))
        content.border = BorderFactory.createEmptyBorder(16, 16, 16, 16)

        // 表单面板
        val formPanel = JPanel(GridBagLayout())
        val gbc = GridBagConstraints().apply {
            insets = Insets(4, 4, 4, 4)
            fill = GridBagConstraints.HORIZONTAL
        }

        addRow(formPanel, gbc, 0, "数据库类型:", typeCombo)

        // 文件路径
        val filePanel = JPanel(BorderLayout(4, 0))
        filePanel.add(filePathField, BorderLayout.CENTER)
        filePanel.add(browseButton, BorderLayout.EAST)
        addRow(formPanel, gbc, 1, "文件路径:", filePanel)

        addRow(formPanel, gbc, 2, dbNameLabel, dbNameField)

        addRow(formPanel, gbc, 3, "用户名:", usernameField)
        addRow(formPanel, gbc, 4, "密码:", passwordField)

        // 提示标签
        gbc.gridy = 5
        gbc.gridx = 0
        gbc.gridwidth = 2
        val hintPanel = JPanel(BorderLayout())
        h2HintLabel.foreground = Color.GRAY
        h2HintLabel.font = h2HintLabel.font.deriveFont(11f)
        sqliteHintLabel.foreground = Color.GRAY
        sqliteHintLabel.font = sqliteHintLabel.font.deriveFont(11f)
        hintPanel.add(h2HintLabel, BorderLayout.NORTH)
        hintPanel.add(sqliteHintLabel, BorderLayout.SOUTH)
        formPanel.add(hintPanel, gbc)

        content.add(formPanel, BorderLayout.CENTER)

        // 按钮面板
        val buttonPanel = JPanel(FlowLayout(FlowLayout.RIGHT))
        buttonPanel.add(JButton("连接").apply { addActionListener { doConnect() } })
        buttonPanel.add(JButton("取消").apply { addActionListener { dispose() } })
        content.add(buttonPanel, BorderLayout.SOUTH)

        // 事件
        typeCombo.addActionListener { updateVisibility() }
        browseButton.addActionListener { browseFile() }
        filePathField.document.addDocumentListener(object : javax.swing.event.DocumentListener {
            override fun insertUpdate(e: javax.swing.event.DocumentEvent?) = updateVisibility()
            override fun removeUpdate(e: javax.swing.event.DocumentEvent?) = updateVisibility()
            override fun changedUpdate(e: javax.swing.event.DocumentEvent?) = updateVisibility()
        })

        contentPane = content
        updateVisibility()
        pack()
        setLocationRelativeTo(owner)
    }

    private fun addRow(panel: JPanel, gbc: GridBagConstraints, row: Int, label: String, comp: JComponent) {
        gbc.gridy = row; gbc.gridx = 0; gbc.gridwidth = 1; gbc.weightx = 0.0
        panel.add(JLabel(label), gbc)
        gbc.gridx = 1; gbc.weightx = 1.0
        panel.add(comp, gbc)
    }

    private fun addRow(panel: JPanel, gbc: GridBagConstraints, row: Int, label: JLabel, comp: JComponent) {
        gbc.gridy = row; gbc.gridx = 0; gbc.gridwidth = 1; gbc.weightx = 0.0
        panel.add(label, gbc)
        gbc.gridx = 1; gbc.weightx = 1.0
        panel.add(comp, gbc)
    }

    private fun updateVisibility() {
        val type = typeCombo.selectedItem as? DatabaseType ?: return
        val isH2 = type == DatabaseType.H2
        val filePathBlank = filePathField.text.isBlank()

        dbNameLabel.isVisible = isH2 && filePathBlank
        dbNameField.isVisible = isH2 && filePathBlank
        h2HintLabel.isVisible = isH2
        sqliteHintLabel.isVisible = !isH2
        usernameField.isEnabled = isH2
        passwordField.isEnabled = isH2

        pack()
    }

    private fun browseFile() {
        val chooser = JFileChooser().apply {
            dialogTitle = "选择数据库文件"
            fileSelectionMode = JFileChooser.FILES_ONLY
        }
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            filePathField.text = chooser.selectedFile.absolutePath
            updateVisibility()
        }
    }

    private fun doConnect() {
        val type = typeCombo.selectedItem as? DatabaseType ?: return
        val info = ConnectionInfo(
            databaseType = type,
            filePath = filePathField.text.trim(),
            databaseName = dbNameField.text.trim(),
            username = usernameField.text.trim(),
            password = String(passwordField.password)
        )
        onConnect(info)
        dispose()
    }
}
