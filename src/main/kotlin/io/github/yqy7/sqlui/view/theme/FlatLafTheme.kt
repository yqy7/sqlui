package io.github.yqy7.sqlui.view.theme

import com.formdev.flatlaf.FlatLightLaf
import javax.swing.UIManager

/**
 * FlatLaf 主题初始化。
 */
object FlatLafTheme {

    private var initialized = false

    fun setup() {
        if (initialized) return
        FlatLightLaf.setup()

        // 自定义微调
        UIManager.put("Component.arc", 8)
        UIManager.put("TextComponent.arc", 6)
        UIManager.put("ScrollBar.showButtons", true)

        initialized = true
    }
}
