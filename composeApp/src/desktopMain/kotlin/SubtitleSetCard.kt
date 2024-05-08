import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import component.LineField
import component.VerticalListBox
import data.SubtitleSet
import util.intFilter
import javax.swing.JFileChooser

@OptIn(ExperimentalMaterialApi::class, ExperimentalFoundationApi::class)
@Composable
fun SubtitleSetCard(
    index: Int,
    set: SubtitleSet,
    availableStyles: List<String>,
    parent: ComposePanel,
    modifier: Modifier = Modifier,
    onSetChanged: (SubtitleSet) -> Unit
) {
    var isFileListEmpty = set.files.isEmpty()

    var selectedIndex by remember { mutableIntStateOf(-1) }
    var offset by remember { mutableStateOf(TextFieldValue("${set.offset} ms")) }
    var styleMenuExpanded by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .weight(1.0f)
                .fillMaxWidth()
        ) {
            Column (modifier = Modifier.weight(1.0f)) {
                Row (verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text("Set ${index + 1}")
                    Spacer(modifier = Modifier.weight(1.0f))
                    TextButton(onClick = {
                        val dialog = JFileChooser()
                        dialog.isMultiSelectionEnabled = true
                        dialog.fileSelectionMode = JFileChooser.FILES_ONLY

                        val result = dialog.showOpenDialog(parent)
                        if (result != JFileChooser.APPROVE_OPTION)
                            return@TextButton

                        val present = HashSet<String>()
                        val newFiles = set.files.toMutableList()
                        present.addAll(set.files.filterNotNull())
                        dialog.selectedFiles.forEach { f ->
                            if (present.add(f.path))
                                newFiles.add(f.path)
                        }

                        onSetChanged(set.copy(files = newFiles))
                    }) {
                        Text("Add files")
                    }
                }
                VerticalListBox(
                    verticalArrangement = Arrangement.spacedBy(0.dp),
                    modifier = Modifier
                        .border(2.dp, MaterialTheme.colors.primary, shape = RoundedCornerShape(3.dp))
                        .padding(5.dp)
                        .fillMaxWidth()
                        .weight(1.0f),
                    isEmpty = set.files.isEmpty(),
                    emptyContent = {
                        Column(
                            modifier = Modifier.align(Alignment.Center).alpha(0.5f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Outlined.Info, contentDescription = "Add files", Modifier.size(50.dp))
                            Text("Add subtitle\nfiles here", textAlign = TextAlign.Center)
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
                    Text("Offset: ")
                    LineField(
                        value = offset,
                        width = 100.dp,
                        onValueChange = { newValue ->
                            val newOffset = intFilter(offset, newValue, suffix = " ms")

                            if (newOffset.first.text != offset.text) {
                                offset = newOffset.first
                                onSetChanged(set.copy(offset = newOffset.second))
                            }
                        }
                    )
                    Spacer(modifier = Modifier.weight(1.0f))
                    Text("Style: ")
                    ExposedDropdownMenuBox(
                        modifier = Modifier.width(120.dp),
                        expanded = styleMenuExpanded,
                        onExpandedChange = { expanded ->
                            styleMenuExpanded = expanded && availableStyles.isNotEmpty()
                        }
                    ) {
                        OutlinedButton(onClick = {}, Modifier.width(120.dp)) {
                            Text(set.style ?: "None")
                        }
                        ExposedDropdownMenu(
                            expanded = styleMenuExpanded,
                            onDismissRequest = { styleMenuExpanded = false }
                        ) {
                            Text("Choose a style", Modifier.padding(5.dp), fontWeight = FontWeight.Bold)
                            Divider(modifier = Modifier.padding(top = 5.dp))

                            for (style in availableStyles) {
                                DropdownMenuItem(
                                    modifier = Modifier,
                                    enabled = true,
                                    contentPadding = PaddingValues(5.dp),
                                    interactionSource = MutableInteractionSource(),
                                    onClick = {
                                        styleMenuExpanded = false
                                        onSetChanged(set.copy(style = style))
                                    }
                                ) {
                                    Text(style)
                                }
                            }
                        }
                    }
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