package io.github.yqy7.sqlui.view.component

import io.github.yqy7.sqlui.viewmodel.ResultViewModel
import javafx.geometry.Insets
import javafx.scene.control.Label
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority

/**
 * 底部状态栏。
 */
class StatusBar(private val viewModel: ResultViewModel) : HBox(20.0) {

    private val connectionLabel = Label("未连接")
    private val infoLabel = Label("")

    init {
        padding = Insets(4.0, 12.0, 4.0, 12.0)
        styleClass.add("status-bar")

        val spacer = javafx.scene.layout.Region()
        HBox.setHgrow(spacer, Priority.ALWAYS)

        children.addAll(connectionLabel, spacer, infoLabel)

        // 监听连接状态
        viewModel.connectionStatusText.addListener { _, _, text ->
            connectionLabel.text = text
        }
    }

    fun setInfo(text: String) {
        infoLabel.text = text
    }
}
