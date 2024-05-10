import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposePanel
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import component.ComboBox
import component.LineField
import component.VerticalListBox
import data.SubtitleSet
import util.FileDropTargetListener
import util.intFilter
import util.selectFiles
import util.fileDragAndDrop

@OptIn(ExperimentalMaterialApi::class, ExperimentalFoundationApi::class)
@Composable
fun SubtitleSetCard(
    index: Int,
    setsCount: Int,
    set: SubtitleSet,
    availableStyles: List<String>,
    parent: ComposePanel,
    modifier: Modifier = Modifier,
    dndListener: FileDropTargetListener,
    onRemoveSet: () -> Unit,
    onSetChanged: (SubtitleSet) -> Unit,
) {
    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .weight(1.0f)
                .fillMaxWidth()
        ) {
            var selectedIndex by remember { mutableIntStateOf(-1) }
            var dndPrompted by remember { mutableStateOf(false) }
            var dndDragging by remember { mutableStateOf(false) }
            var addedFiles by remember { mutableStateOf(emptyList<String>()) }

            // have to keep it as a LaunchedEffect to have DnD working correctly
            LaunchedEffect(addedFiles) {
                if (addedFiles.isEmpty())
                    return@LaunchedEffect

                val present = HashSet<String>()
                val newFiles = set.files.toMutableList()
                present.addAll(set.files.filterNotNull())
                addedFiles.forEach { f ->
                    if (present.add(f))
                        newFiles.add(f)
                }

                onSetChanged(set.copy(files = newFiles))
                addedFiles = emptyList()
            }

            Column (modifier = Modifier.weight(1.0f)) {
                Row (verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text("Set ${index + 1}")
                    if (setsCount > 1) {
                        TextButton(onClick = onRemoveSet) {
                            Text("Remove")
                        }
                    }
                    Spacer(modifier = Modifier.weight(1.0f))
                    TextButton(onClick = {
                        addedFiles = selectFiles(parent)
                    }) {
                        Text("Add files")
                    }
                }
                VerticalListBox(
                    verticalArrangement = Arrangement.spacedBy(0.dp),
                    isEmpty = set.files.isEmpty(),
                    modifier = Modifier
                        .border(2.dp, MaterialTheme.colors.primary, shape = RoundedCornerShape(3.dp))
                        .padding(5.dp)
                        .fillMaxWidth()
                        .weight(1.0f)
                        .background(if (!dndDragging) MaterialTheme.colors.background else MaterialTheme.colors.primary.copy(alpha = 0.3f))
                        .fileDragAndDrop(keyValue = set,
                            listener = dndListener,
                            onDragStart = {
                                dndDragging = true
                            },
                            onDragExit = {
                                dndDragging = false
                            },
                            onPromptStart = {
                                dndPrompted = true
                            },
                            onPromptEnd = {
                                dndPrompted = false
                            },
                            onDrop = { value ->
                                addedFiles = value.data.map { f -> f.toString() }
                            },
                            fileFilter = { file -> file.extension in arrayOf("srt", "ass", "ssa") }
                        ),
                    emptyContent = {
                        Column(
                            modifier = Modifier.align(Alignment.Center).alpha(0.5f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (dndPrompted) {
                                Icon(Icons.Outlined.Add, contentDescription = "Drop files", Modifier.size(50.dp))
                                Text("Drop subtitle\nfiles here", textAlign = TextAlign.Center)
                            } else {
                                Icon(Icons.Outlined.Info, contentDescription = "Add files", Modifier.size(50.dp))
                                Text("Add subtitle\nfiles here", textAlign = TextAlign.Center)
                            }
                        }
                    }
                ) {
                    for (i in 0..< set.files.size) {
                        val filePath = set.files[i]
                        val isCurrentSelected = selectedIndex == i

                        val background = if (!isCurrentSelected)
                            TextStyle.Default.background
                        else
                            Color.LightGray

                        TooltipArea(tooltip = {
                            Text(
                                filePath ?: "[ Blank ]",
                                Modifier
                                    .background(Color(0xfff0f0f0))
                                    .border(0.dp, Color.LightGray, RoundedCornerShape(5.dp)).padding(5.dp)
                            )
                        }) {
                            ListItem(
                                modifier = Modifier
                                    .clipToBounds()
                                    .fillMaxWidth()
                                    .wrapContentHeight(align = Alignment.CenterVertically)
                                    .height(25.dp)
                                    .background(background)
                                    .selectable(isCurrentSelected, onClick = {
                                        selectedIndex = if (isCurrentSelected) -1 else i
                                    })
                            ) {
                                val fileName: String = filePath?.split("/")?.last() ?: "[ Blank ]"
                                Text(fileName, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    var offset by remember { mutableStateOf(TextFieldValue("${set.offset} ms")) }

                    key(set) {
                        if (offset.text != "${set.offset} ms")
                            offset = TextFieldValue("${set.offset} ms")
                    }

                    Text("Offset: ")
                    LineField(
                        value = offset,
                        width = 100.dp,
                        onValueChange = { newValue ->
                            val oldOffset = offset
                            val newOffset = intFilter(offset, newValue, suffix = " ms")

                            offset = newOffset.first
                            if (newOffset.first.text != oldOffset.text)
                                onSetChanged(set.copy(offset = newOffset.second))
                        }
                    )
                    Spacer(modifier = Modifier.weight(1.0f))
                    Text("Style: ")
                    ComboBox(
                        data = availableStyles,
                        currentText = set.style,
                        onItemSelected = { _, style -> onSetChanged(set.copy(style = style)) },
                        header = "Choose a style"
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    shape = RectangleShape,
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.size(30.dp).align(Alignment.CenterHorizontally),
                    onClick = {
                        val newFiles = set.files.toMutableList()

                        if (selectedIndex != -1) {
                            newFiles.add(selectedIndex, null)
                            selectedIndex++
                        } else {
                            newFiles.add(0, null)
                        }

                        onSetChanged(set.copy(files = newFiles))
                    }
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = "Add Blank")
                }
                Column {
                    Button(
                        shape = RectangleShape,
                        contentPadding = PaddingValues(0.dp),
                        enabled = (selectedIndex != -1),
                        modifier = Modifier.size(30.dp).align(Alignment.CenterHorizontally),
                        onClick = {
                            if (selectedIndex > 0) {
                                val newFiles = set.files.toMutableList()
                                val item = newFiles[selectedIndex]
                                newFiles.removeAt(selectedIndex)
                                newFiles.add(selectedIndex - 1, item)

                                selectedIndex -= 1
                                onSetChanged(set.copy(files = newFiles))
                            }
                        }
                    ) {
                        Icon(Icons.Rounded.KeyboardArrowUp, contentDescription = "Move up")
                    }
                    Button(
                        shape = RectangleShape,
                        contentPadding = PaddingValues(0.dp),
                        enabled = (selectedIndex != -1),
                        modifier = Modifier.size(30.dp).align(Alignment.CenterHorizontally),
                        onClick = {
                            if (selectedIndex < set.files.size - 1) {
                                val newFiles = set.files.toMutableList()
                                val item = newFiles[selectedIndex]
                                newFiles.removeAt(selectedIndex)
                                newFiles.add(selectedIndex + 1, item)

                                selectedIndex += 1
                                onSetChanged(set.copy(files = newFiles))
                            }
                        }
                    ) {
                        Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "Move down")
                    }
                }
                Button(
                    shape = RectangleShape,
                    contentPadding = PaddingValues(0.dp),
                    enabled = (selectedIndex != -1),
                    modifier = Modifier.size(30.dp).align(Alignment.CenterHorizontally),
                    onClick = {
                        val newFiles = set.files.toMutableList()
                        newFiles.removeAt(selectedIndex)

                        selectedIndex = -1
                        onSetChanged(set.copy(files = newFiles))
                    }
                ) {
                    Icon(Icons.Rounded.Delete, contentDescription = "Delete File")
                }
            }
        }
    }
}