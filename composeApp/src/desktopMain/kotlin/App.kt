import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.rounded.*
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
import com.rd316.submerger.ssa.SSAFile
import com.rd316.submerger.ssa.SSAParser
import component.LineField
import component.HorizontalListBox
import component.VerticalListBox
import data.SubtitleSet
import util.intFilter
import java.awt.Dimension
import java.awt.event.ComponentEvent
import java.awt.event.ComponentListener
import java.io.FileReader
import javax.swing.JFileChooser

class ResizeListener(val onResize: (Dimension) -> Unit) : ComponentListener {
    override fun componentResized(e: ComponentEvent?) {
        onResize(e?.component?.size!!)
    }

    override fun componentMoved(e: ComponentEvent?) {}

    override fun componentShown(e: ComponentEvent?) {}

    override fun componentHidden(e: ComponentEvent?) {}
}

@Composable
fun App(parent: ComposePanel) {
    val outerPadding = 10.dp

    var panelDimension by remember { mutableStateOf(parent.size) }

    var formatFilename by remember { mutableStateOf("") }
    var formatData by remember { mutableStateOf<SSAFile?>(null) }

    var syncThreshold by remember { mutableStateOf(TextFieldValue("500 ms")) }

    val subtitleSets = remember { mutableStateListOf(
        SubtitleSet(
            files = emptyList(),
            offset = 0
        )
    )}

    parent.addComponentListener(ResizeListener { dimension ->
        panelDimension = dimension
    })

    LaunchedEffect(formatFilename) {
        if (formatFilename.isNotBlank()) {
            FileReader(formatFilename).use { reader ->
                val data = reader.readText()
                formatData = SSAParser.parse(data)

                val styleNames = formatData!!.styles.map{ s -> s.fields["Name"]!! }
                for (setIndex in 0 ..< subtitleSets.size) {
                    val set = subtitleSets[setIndex]
                    val styleIndex = if (setIndex < styleNames.size) setIndex else (styleNames.size - 1)
                    subtitleSets[setIndex] = set.copy(style = styleNames[styleIndex])
                }
            }
        }
    }

    Row(modifier = Modifier.padding(outerPadding)) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(0.dp),
            modifier = Modifier.weight(1.0F, true)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("Format file: ")
                TextButton(onClick = {
                    val dialog = JFileChooser()

                    val result = dialog.showOpenDialog(parent)
                    if (result == JFileChooser.APPROVE_OPTION) {
                        formatFilename = dialog.selectedFile.path
                    }
                }) {
                    if (formatFilename.isNotBlank())
                        Text(formatFilename)
                    else
                        Text("Open file")
                }
                Spacer(modifier = Modifier.weight(1.0f))
                Text("Sync threshold: ")
                LineField(
                    value = syncThreshold,
                    width = 80.dp,
                    onValueChange = { newValue ->
                        syncThreshold = intFilter(syncThreshold, newValue, suffix = " ms", min = 0, max = 10000).first
                    }
                )
            }
            Divider()
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("Sets of subtitles: ")
                TextButton(
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.width(60.dp),
                    onClick = {
                        subtitleSets.add(SubtitleSet())
                    }) {
                    Icon(Icons.Rounded.Add, contentDescription = "Add set")
                    Text("New")
                }
            }
            HorizontalListBox(modifier = Modifier.weight(1.0f)) {
                for (setIndex in subtitleSets.indices) {
                    val set = subtitleSets[setIndex]

                    SubtitleSetCard(
                        index = setIndex,
                        set = set,
                        formatData?.styles?.map { s -> s.fields["Name"]!! } ?: emptyList(),
                        modifier = Modifier
                            .width((panelDimension.width / 2).dp - (outerPadding * 2))
                            .padding(bottom = 15.dp),
                        parent = parent,
                        onSetChanged = { newSet ->
                            subtitleSets[setIndex] = newSet
                        }
                    )
                }
            }
            Divider()
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = {}) {
                    Text("Clear")
                }
                Button(onClick = {}) {
                    Text("Convert")
                }
            }
        }
    }
}

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
    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .weight(1.0f)
                .fillMaxWidth()
        ) {
            var selectedIndex by remember { mutableStateOf(-1) }
            var isEmpty by remember { mutableStateOf(set.files.isEmpty()) }

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
                        isEmpty = false
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
                    isEmpty = isEmpty,
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
                    var offset by remember { mutableStateOf(TextFieldValue("${set.offset} ms")) }
                    var styleMenuExpanded by remember { mutableStateOf(false) }
                    var chosenStyle by remember { mutableStateOf(TextFieldValue(set.style ?: "None")) }

                    Text("Offset: ")
                    LineField(
                        value = offset,
                        width = 100.dp,
                        onValueChange = { newValue ->
                            val newOffset = intFilter(offset, newValue, suffix = " ms")

                            offset = newOffset.first
                            if (newOffset.first.text != offset.text)
                                onSetChanged(set.copy(offset = newOffset.second))
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
                            Text(chosenStyle.text)
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
                                        chosenStyle = TextFieldValue(style)
                                        styleMenuExpanded = false
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

                        isEmpty = false
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
                        isEmpty = newFiles.size <= 1

                        onSetChanged(set.copy(files = newFiles))
                    }
                ) {
                    Icon(Icons.Rounded.Delete, contentDescription = "Delete File")
                }
            }
        }
    }
}