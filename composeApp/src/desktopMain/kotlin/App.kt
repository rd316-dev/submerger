import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposePanel
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.rd316.submerger.SubInfo
import com.rd316.submerger.SubMerger
import com.rd316.submerger.ssa.SSAFile
import com.rd316.submerger.ssa.SSAParser
import component.ComboBox
import component.HorizontalListBox
import component.LineField
import data.SubtitleSet
import util.FileDropTargetListener
import util.intFilter
import util.selectFile
import util.selectFolder
import java.awt.Dimension
import java.awt.dnd.*
import java.awt.event.ComponentEvent
import java.awt.event.ComponentListener
import java.io.File
import java.io.InputStreamReader
import java.lang.IllegalArgumentException
import java.nio.file.Paths
import kotlin.io.path.name

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
    val setGap = 10.dp

    var panelDimension by remember { mutableStateOf(parent.size) }

    var formatFilename by remember { mutableStateOf<String?>(null) }
    var formatData by remember { mutableStateOf<SSAFile?>(null) }

    var outputFolder by remember { mutableStateOf<String?>(null) }

    var syncSet by remember { mutableStateOf<Int?>(null) }
    var syncThreshold by remember { mutableIntStateOf(500) }

    val subtitleSets = remember { mutableStateListOf(
        SubtitleSet(
            files = emptyList(),
            offset = 0
        )
    )}

    parent.addComponentListener(ResizeListener { dimension ->
        panelDimension = dimension
    })

    val dndListener by remember { mutableStateOf(FileDropTargetListener()) }

    parent.dropTarget = DropTarget(parent, dndListener)

    Row(modifier = Modifier.padding(outerPadding)) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(0.dp),
            modifier = Modifier.weight(1.0F, true)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("Format file: ")
                TextButton(onClick = { formatFilename = selectFile(parent) ?: formatFilename }) {
                    Text(formatFilename ?: "Open file")
                }
            }
            Divider()
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                var syncThresholdField by remember { mutableStateOf(TextFieldValue("$syncThreshold ms")) }

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
                Spacer(modifier = Modifier.weight(1.0f))
                Text("Sync to: ")
                ComboBox(
                    data = subtitleSets,
                    currentText = syncSet?.let {"Set ${it + 1}" },
                    onItemSelected = { i, _ -> syncSet = i},
                    modifier = Modifier.width(100.dp),
                    nullOption = "None",
                    converter = { i, _ -> "Set ${i+1}" })
                Spacer(modifier = Modifier.width(10.dp))
                Text("Threshold: ")
                LineField(
                    value = syncThresholdField,
                    width = 80.dp,
                    onValueChange = { newValue ->
                        val result = intFilter(syncThresholdField, newValue, suffix = " ms", min = 0, max = 10000)
                        syncThresholdField = result.first
                        syncThreshold = result.second
                    }
                )
            }

            HorizontalListBox(modifier = Modifier.weight(1.0f), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                LaunchedEffect(formatFilename) {
                    formatFilename?.let {
                        InputStreamReader(File(it).inputStream(), Charsets.UTF_8).use { reader ->
                            val data = reader.readText()
                            val formatSSA = SSAParser.parse(data)

                            val styleNames = formatSSA.styles.map{ s -> s.fields["Name"]!! }
                            for (setIndex in 0 ..< subtitleSets.size) {
                                val set = subtitleSets[setIndex]
                                val styleIndex = if (setIndex < styleNames.size) setIndex else (styleNames.size - 1)
                                subtitleSets[setIndex] = set.copy(style = styleNames[styleIndex])
                            }

                            formatData = formatSSA
                        }
                    }

                    if (formatFilename == null) {
                        formatData = null
                    }
                }

                for (setIndex in subtitleSets.indices) {
                    val set = subtitleSets[setIndex]
                    val availableStyles = formatData?.styles?.map { s -> s.fields["Name"]!! } ?: emptyList()

                    SubtitleSetCard(
                        index = setIndex,
                        setsCount = subtitleSets.size,
                        set = set,
                        availableStyles = availableStyles,
                        parent = parent,
                        dndListener = dndListener,
                        modifier = Modifier
                            .width(((panelDimension.width.dp - setGap) / 2) - outerPadding)
                            .padding(bottom = 15.dp),
                        onRemoveSet = { subtitleSets.removeAt(setIndex) },
                        onSetChanged = { newSet -> subtitleSets[setIndex] = newSet }
                    )
                }
            }
            Divider()
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Output folder: ")
                TextButton(onClick = { outputFolder = selectFolder(parent) }) {
                    Text(outputFolder ?: "Choose folder")
                }
                Spacer(modifier = Modifier.weight(1.0f))
                TextButton(onClick = {
                    formatFilename = null
                    syncThreshold = 500
                    syncSet = null
                    formatFilename = null
                    outputFolder = null

                    subtitleSets.clear()
                    subtitleSets.add(SubtitleSet(
                        files = emptyList(),
                        offset = 0
                    ))
                }) {
                    Text("Clear")
                }
                Button(
                    enabled = !formatFilename.isNullOrBlank() && !outputFolder.isNullOrBlank(),
                    onClick = {
                        val info = ArrayList<ArrayList<SubInfo?>>()

                        for (setIndex in subtitleSets.indices) {
                            val set = subtitleSets[setIndex]

                            for (fileIndex in set.files.indices) {
                                val file = set.files[fileIndex]

                                if (info.size <= fileIndex)
                                    info.add(ArrayList())

                                info[fileIndex].add(file?.let { SubInfo(
                                    filename = file,
                                    appliedStyle = set.style ?: formatData?.styles?.get(0)?.fields?.get("Name") ?: throw IllegalArgumentException("no styles"),
                                    offsetMs = set.offset.toLong(),
                                    syncOrigin = setIndex == syncSet
                                )})
                            }
                        }

                        for (fileRow in info) {
                            val files = fileRow.filterNotNull()
                            if (files.isEmpty())
                                continue

                            var filename: String = (files.find { f -> f.syncOrigin }?.filename ?: files[0].filename)

                            if (filename.endsWith(".ssa"))
                                filename = filename.removeSuffix(".ssa")
                            else if (filename.endsWith(".ass"))
                                filename = filename.removeSuffix(".ass")
                            else if (filename.endsWith(".srt"))
                                filename = filename.removeSuffix(".srt")

                            filename = Paths.get("$filename.ass").name

                            val outputFilename = Paths.get(outputFolder!!, filename).toAbsolutePath().toString()
                            SubMerger().merge(
                                formatFilename!!,
                                outputFilename = outputFilename,
                                syncThresholdMs=syncThreshold.toLong(),
                                inputFiles=files
                            )
                        }
                    }
                ) {
                    Text("Convert")
                }
            }
        }
    }
}