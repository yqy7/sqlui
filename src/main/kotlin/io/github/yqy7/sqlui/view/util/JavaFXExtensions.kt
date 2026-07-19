package io.github.yqy7.sqlui.view.util

import javafx.event.EventHandler
import javafx.scene.Node
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox

/**
 * JavaFX 布局 DSL 扩展函数。
 */

/** 创建 ToolBar 的 DSL */
fun toolBar(block: ToolBar.() -> Unit): ToolBar = ToolBar().apply(block)

/** ToolBar 中添加按钮 */
fun ToolBar.button(text: String, action: () -> Unit): Button {
    val btn = Button(text)
    btn.setOnAction { action() }
    items.add(btn)
    return btn
}

/** ToolBar 中添加分隔符 */
fun ToolBar.separator() {
    items.add(Separator())
}

/** ToolBar 中添加空白占位 */
fun ToolBar.spacer() {
    val spacer = javafx.scene.layout.Region()
    HBox.setHgrow(spacer, Priority.ALWAYS)
    items.add(spacer)
}

/** ToolBar 中添加标签 */
fun ToolBar.label(text: String): Label {
    val lbl = Label(text)
    items.add(lbl)
    return lbl
}

/** VBox 快速构建 */
fun vBox(spacing: Double = 5.0, block: VBox.() -> Unit): VBox = VBox(spacing).apply(block)

/** HBox 快速构建 */
fun hBox(spacing: Double = 5.0, block: HBox.() -> Unit): HBox = HBox(spacing).apply(block)

/** 设置节点为 VBox 可伸缩 */
fun Node.vgrow(priority: Priority = Priority.ALWAYS) {
    VBox.setVgrow(this, priority)
}

/** 设置节点为 HBox 可伸缩 */
fun Node.hgrow(priority: Priority = Priority.ALWAYS) {
    HBox.setHgrow(this, priority)
}

/** TreeView 快捷：设置根节点并注册事件 */
fun <T> TreeView<T>.treeItem(value: T, block: TreeItem<T>.() -> Unit = {}): TreeItem<T> {
    val item = TreeItem(value)
    item.block()
    return item
}

/** 列快捷配置 */
fun <S, T> TableColumn<S, T>.configure(block: TableColumn<S, T>.() -> Unit): TableColumn<S, T> {
    block()
    return this
}

/** MenuBar 中添加 Menu */
fun MenuBar.menu(text: String, block: Menu.() -> Unit): Menu {
    val menu = Menu(text)
    menu.block()
    menus.add(menu)
    return menu
}

/** Menu 中添加 MenuItem */
fun Menu.item(text: String, action: () -> Unit): MenuItem {
    val item = MenuItem(text)
    item.setOnAction { action() }
    items.add(item)
    return item
}

/** 设置 Scene 级别事件处理器 */
fun Scene.onEvent(eventType: javafx.event.EventType<in javafx.event.Event>, handler: EventHandler<in javafx.event.Event>) {
    addEventFilter(eventType, handler)
}
