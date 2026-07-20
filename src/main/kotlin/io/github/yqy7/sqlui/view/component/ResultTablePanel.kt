package io.github.yqy7.sqlui.view.component

import io.github.yqy7.sqlui.model.QueryResult
import io.github.yqy7.sqlui.util.CsvExporter
import java.awt.BorderLayout
import java.io.File
import javax.swing.*
import javax.swing.table.DefaultTableModel

/**
 * 查询结果表格面板。
 * 使用 JTable 展示 SELECT 查询结果，支持 CSV 导出。
 */
class ResultTablePanel : JPanel(BorderLayout()) {

    private val tableModel = DefaultTableModel()
    private val table = JTable(tableModel).apply {
        autoResizeMode = JTable.AUTO_RESIZE_OFF
        autoCreateRowSorter = true
        setFillsViewportHeight(true)
    }
    private val scrollPane = JScrollPane(table)
    private val exportButton = JButton("导出 CSV")

    private val infoLabel = JLabel(" ").apply {
        border = BorderFactory.createEmptyBorder(2, 4, 2, 4)
    }

    private var currentResult: QueryResult.Success? = null

    init {
        val topPanel = JPanel(BorderLayout())
        topPanel.add(infoLabel, BorderLayout.CENTER)
        topPanel.add(exportButton, BorderLayout.EAST)
        add(topPanel, BorderLayout.NORTH)
        add(scrollPane, BorderLayout.CENTER)

        exportButton.isEnabled = false
        exportButton.addActionListener { exportCsv() }
    }

    /** 显示查询结果 */
    fun showResult(result: QueryResult) {
        when (result) {
            is QueryResult.Success -> showSuccessResult(result)
            is QueryResult.UpdateSuccess -> showUpdateResult(result)
            is QueryResult.Error -> showErrorResult(result)
            else -> clear()
        }
    }

    private fun showSuccessResult(result: QueryResult.Success) {
        currentResult = result
        tableModel.setDataVector(
            result.rows.map { it.toTypedArray() }.toTypedArray(),
            result.columns.toTypedArray()
        )
        infoLabel.text = " ${result.rowCount} 行, 耗时 ${result.executionTimeMs}ms"
        exportButton.isEnabled = result.rows.isNotEmpty()
    }

    private fun showUpdateResult(result: QueryResult.UpdateSuccess) {
        currentResult = null
        tableModel.setDataVector(emptyArray(), emptyArray())
        infoLabel.text = " 影响了 ${result.affectedRows} 行, 耗时 ${result.executionTimeMs}ms"
        exportButton.isEnabled = false
    }

    private fun showErrorResult(result: QueryResult.Error) {
        currentResult = null
        tableModel.setDataVector(emptyArray(), emptyArray())
        infoLabel.text = " 错误: ${result.message}"
        exportButton.isEnabled = false
    }

    fun clear() {
        currentResult = null
        tableModel.setDataVector(emptyArray(), emptyArray())
        infoLabel.text = " "
        exportButton.isEnabled = false
    }

    private fun exportCsv() {
        val data = currentResult ?: return
        val chooser = JFileChooser().apply {
            dialogTitle = "导出 CSV"
            selectedFile = File("export.csv")
        }
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            val file = chooser.selectedFile
            val success = CsvExporter.export(data, file)
            if (success) {
                JOptionPane.showMessageDialog(this, "已导出到: ${file.absolutePath}", "导出成功", JOptionPane.INFORMATION_MESSAGE)
            } else {
                JOptionPane.showMessageDialog(this, "导出失败", "导出错误", JOptionPane.ERROR_MESSAGE)
            }
        }
    }
}
