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
import com.rd316.submerger.ssa.SSAFile
import com.rd316.submerger.ssa.SSAParser
import component.HorizontalListBox
import component.LineField
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
    val outerPadding = 20.dp

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
                    if (result == JFileChooser.APPROVE_OPTION)
                        formatFilename = dialog.selectedFile.path
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

            val setGap = 10.dp
            HorizontalListBox(modifier = Modifier.weight(1.0f), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                LaunchedEffect(formatFilename) {
                    if (formatFilename.isNotBlank()) {
                        FileReader(formatFilename).use { reader ->
                            val data = reader.readText()
                            val formatSSA = SSAParser.parse(data)

                            val styleNames = formatSSA.styles.map{ s -> s.fields["Name"]!! }
                            for (setIndex in 0 ..< subtitleSets.size) {
                                val set = subtitleSets[setIndex]
                                val styleIndex = if (setIndex < styleNames.size) setIndex else (styleNames.size - 1)
                                subtitleSets[setIndex] = set.copy(style = styleNames[styleIndex])
                                println("assigned ${styleNames[styleIndex]} style to the set ${setIndex + 1}")
                            }

                            formatData = formatSSA
                        }
                    }
                }

                for (setIndex in subtitleSets.indices) {
                    val set = subtitleSets[setIndex]

                    val availableStyles = formatData?.styles?.map { s -> s.fields["Name"]!! } ?: emptyList()

                    SubtitleSetCard(
                        index = setIndex,
                        set = set,
                        availableStyles = availableStyles,
                        parent = parent,
                        modifier = Modifier
                            .width(((panelDimension.width.dp - setGap) / 2) - outerPadding)
                            .padding(bottom = 15.dp),
                        onSetChanged = { newSet ->
                            subtitleSets[setIndex] = newSet
                            println(newSet)
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