package io.github.yqy7.sqlui.view.component

import io.github.yqy7.sqlui.model.ConnectionInfo
import io.github.yqy7.sqlui.model.DatabaseType
import io.github.yqy7.sqlui.viewmodel.ConnectionViewModel
import javafx.geometry.Insets
import javafx.scene.control.*
import javafx.scene.layout.GridPane
import javafx.stage.FileChooser
import java.io.File

/**
 * 新建/打开数据库连接对话框。
 */
class ConnectionDialog(private val viewModel: ConnectionViewModel) : Dialog<ConnectionInfo>() {

    private val dbTypeCombo = ComboBox<DatabaseType>().apply {
        items.addAll(DatabaseType.entries)
        selectionModel.selectFirst()
    }

    private val filePathField = TextField().apply {
        promptText = "选择或输入数据库文件路径..."
        prefWidth = 350.0
    }

    private val dbNameField = TextField().apply {
        promptText = "数据库名（H2 内存模式）"
        isDisable = true
    }

    private val usernameField = TextField().apply {
        promptText = "用户名（可选）"
    }

    private val passwordField = PasswordField().apply {
        promptText = "密码（可选）"
    }

    init {
        title = "连接数据库"
        headerText = "选择数据库类型并指定连接参数"

        dialogPane.buttonTypes.addAll(ButtonType.OK, ButtonType.CANCEL)

        // 仅当输入有效时才启用 OK 按钮
        val okButton = dialogPane.lookupButton(ButtonType.OK)
        okButton.isDisable = true

        buildForm()

        // 类型切换时更新表单状态
        dbTypeCombo.valueProperty().addListener { _, _, newType ->
            when (newType) {
                DatabaseType.H2 -> {
                    filePathField.isDisable = false
                    dbNameField.isDisable = true
                }
                DatabaseType.SQLITE -> {
                    filePathField.isDisable = false
                    dbNameField.isDisable = true
                }
            }
            validateInput()
        }

        // 路径变化时校验
        filePathField.textProperty().addListener { _, _, _ -> validateInput() }

        // 结果转换
        setResultConverter { buttonType ->
            if (buttonType == ButtonType.OK) {
                ConnectionInfo(
                    databaseType = dbTypeCombo.value,
                    filePath = filePathField.text.trim(),
                    databaseName = dbNameField.text.trim(),
                    username = usernameField.text.trim(),
                    password = passwordField.text
                )
            } else null
        }
    }

    private fun buildForm() {
        val grid = GridPane().apply {
            hgap = 10.0
            vgap = 10.0
            padding = Insets(20.0)
        }

        // 数据库类型
        grid.add(Label("数据库类型:"), 0, 0)
        grid.add(dbTypeCombo, 1, 0)

        // 文件路径
        grid.add(Label("文件路径:"), 0, 1)
        val fileRow = javafx.scene.layout.HBox(8.0).apply {
            children.add(filePathField)
            val browseBtn = Button("浏览...")
            browseBtn.setOnAction { browseFile() }
            children.add(browseBtn)
        }
        grid.add(fileRow, 1, 1)

        // 数据库名（H2 内存）
        grid.add(Label("数据库名:"), 0, 2)
        grid.add(dbNameField, 1, 2)

        // 用户名
        grid.add(Label("用户名:"), 0, 3)
        grid.add(usernameField, 1, 3)

        // 密码
        grid.add(Label("密码:"), 0, 4)
        grid.add(passwordField, 1, 4)

        dialogPane.content = grid
    }

    private fun browseFile() {
        val type = dbTypeCombo.value
        val chooser = FileChooser().apply {
            title = "选择${type.displayName}数据库文件"
            extensionFilters.addAll(
                when (type) {
                    DatabaseType.H2 -> FileChooser.ExtensionFilter(
                        "H2 数据库文件", listOf("*.mv.db", "*.h2.db", "*.db")
                    )
                    DatabaseType.SQLITE -> FileChooser.ExtensionFilter(
                        "SQLite 数据库文件", listOf("*.db", "*.sqlite", "*.sqlite3", "*.s3db")
                    )
                },
                FileChooser.ExtensionFilter("所有文件", listOf("*.*"))
            )
        }

        val file = chooser.showOpenDialog(dialogPane.scene.window)
        if (file != null) {
            filePathField.text = file.absolutePath
        }
    }

    private fun validateInput() {
        dialogPane.lookupButton(ButtonType.OK)?.let { okBtn ->
            val hasFile = filePathField.text.isNotBlank()
            val hasDbName = dbNameField.text.isNotBlank()
            okBtn.isDisable = !hasFile && !hasDbName
        }
    }
}
