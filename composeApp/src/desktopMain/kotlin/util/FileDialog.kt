package util

import java.awt.Component
import javax.swing.JFileChooser

fun selectFiles(parent: Component): List<String> {
    val dialog = JFileChooser()
    dialog.isMultiSelectionEnabled = true
    dialog.fileSelectionMode = JFileChooser.FILES_ONLY

    val result = dialog.showOpenDialog(parent)
    return if (result == JFileChooser.APPROVE_OPTION)
        dialog.selectedFiles.map { f -> f.path }
    else
        emptyList()
}

fun selectFile(parent: Component): String? {
    val dialog = JFileChooser()
    dialog.isMultiSelectionEnabled = false
    dialog.fileSelectionMode = JFileChooser.FILES_ONLY

    dialog.showOpenDialog(parent)
    return dialog.selectedFile?.path
}

fun selectFolder(parent: Component): String? {
    val dialog = JFileChooser()
    dialog.isMultiSelectionEnabled = false
    dialog.fileSelectionMode = JFileChooser.DIRECTORIES_ONLY

    dialog.showOpenDialog(parent)
    return dialog.selectedFile?.path
}