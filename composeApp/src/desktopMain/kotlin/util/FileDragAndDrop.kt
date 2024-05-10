package util

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.IntSize
import java.awt.Point
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.dnd.DnDConstants
import java.awt.dnd.DropTargetDragEvent
import java.awt.dnd.DropTargetDropEvent
import java.awt.dnd.DropTargetEvent
import java.awt.dnd.DropTargetListener
import java.io.File
import java.io.IOException

data class DragValue(val location: Point, val data: List<File>)

class DragAndDropComponent(
    val checkDrag    : (DragValue) -> Boolean,
    val onDragStart  : (DragValue) -> Unit,
    val onDrag       : (DragValue) -> Unit,
    val onDrop       : (DragValue) -> Unit,
    val onDragExit   : ()          -> Unit,

    val onPromptStart: (DragValue) -> Unit,
    val onPromptEnd  : ()          -> Unit,
    val fileFilter   : (File)      -> Boolean
) {
    var dragState: Boolean = false
    var isPrompted: Boolean = false
}

class FileDropTargetListener : DropTargetListener {
    private val components = mutableListOf<DragAndDropComponent>()

    fun attachComponent(component: DragAndDropComponent) {
        components.add(component)
    }

    private fun convertData(transferable: Transferable): List<File>? {
        var flavor: DataFlavor? = null
        for (f in transferable.transferDataFlavors) {
            val tokens = f.mimeType.split(";")
            val name = tokens[0].trim()
            val className = tokens[1].split("=")[1].trim()

            if (name == "application/x-java-file-list" && className == "java.util.List") {
                flavor = f
                break
            }
        }

        flavor ?: return null

        var data: List<File>? = null
        try {
            data = transferable.getTransferData(flavor) as List<File>?
        } catch (_: IOException) {}

        return data
    }

    override fun dragEnter(dtde: DropTargetDragEvent?) {
        dtde ?: return

        val data = convertData(dtde.transferable)
        data ?: return

        val dragValue = DragValue(dtde.location, data)
        for (c in components) {
            c.isPrompted = true
            c.onPromptStart(dragValue)
        }
    }

    override fun dragOver(dtde: DropTargetDragEvent?) {
        dtde ?: return

        val data = convertData(dtde.transferable)
        data ?: return

        var isAccepted = false

        for (c in components) {
            val filteredData = data.filter { f -> c.fileFilter(f) }
            if (filteredData.isEmpty()) {
                if (c.dragState) {
                    c.dragState = false
                    c.onDragExit()
                }

                if (c.isPrompted) {
                    c.isPrompted = false
                    c.onPromptEnd()
                }

                continue
            }

            val dragValue = DragValue(dtde.location, filteredData)
            if (!c.isPrompted) {
                c.isPrompted = true
                c.onPromptStart(dragValue)
            }

            if (c.checkDrag(dragValue)) {
                if (!c.dragState) {
                    isAccepted = true
                    c.dragState = true
                    c.onDragStart(dragValue)
                } else {
                    isAccepted = true
                    c.onDrag(dragValue)
                }
            } else if (c.dragState) {
                c.dragState = false
                c.onDragExit()
            }
        }

        if (isAccepted)
            dtde.acceptDrag(DnDConstants.ACTION_REFERENCE)
        else
            dtde.rejectDrag()
    }

    override fun dropActionChanged(dtde: DropTargetDragEvent?) {}

    override fun dragExit(dte: DropTargetEvent?) {
        dte ?: return

        for (c in components) {
            if (c.isPrompted) {
                c.onPromptEnd()
            }
            c.isPrompted = false
            if (c.dragState) {
                c.onDragExit()
            }
            c.dragState = false
        }
    }

    override fun drop(dtde: DropTargetDropEvent?) {
        dtde ?: return

        dtde.acceptDrop(DnDConstants.ACTION_REFERENCE)
        val data = convertData(dtde.transferable)
        data ?: return

        val dragValue = DragValue(dtde.location, data)
        for (c in components) {
            if (c.checkDrag(dragValue)) {
                c.onDrop(dragValue)
                break
            }
        }

        for (c in components) {
            c.dragState = false
            c.onDragExit()
            c.onPromptEnd()
        }
    }
}

fun <T> Modifier.fileDragAndDrop(
    keyValue: T,
    listener: FileDropTargetListener,
    onDragStart  : (DragValue) -> Unit = {},
    onDrag       : (DragValue) -> Unit = {},
    onDrop       : (DragValue) -> Unit = {},
    onDragExit   : ()          -> Unit = {},
    onPromptStart: (DragValue) -> Unit = {},
    onPromptEnd  : ()          -> Unit = {},
    fileFilter   : (File)      -> Boolean = { true }
): Modifier = composed {
    var size by remember(keyValue) { mutableStateOf(IntSize(0, 0)) }
    var position by remember(keyValue) { mutableStateOf(Offset(0f, 0f)) }

    key (keyValue) {
        val component = DragAndDropComponent(
            checkDrag = { value ->
                value.location.x >= position.x && value.location.x <= position.x + size.width
                        && value.location.y >= position.y && value.location.y <= position.y + size.height
            },
            onDragStart = onDragStart,
            onDrag = onDrag,
            onDrop = onDrop,
            onDragExit = onDragExit,
            onPromptStart = onPromptStart,
            onPromptEnd = onPromptEnd,
            fileFilter = fileFilter
        )

        listener.attachComponent(component)
    }

    return@composed Modifier.onGloballyPositioned { coordinates ->
        size = coordinates.size
        position = coordinates.positionInWindow()
    }
}