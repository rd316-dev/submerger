package component

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun <T> ComboBox(data: List<T>, currentText: String, onItemSelected: (T) -> Unit, modifier: Modifier = Modifier.width(150.dp), header: String? = null) {
    var menuExpanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = menuExpanded,
        onExpandedChange = { expanded ->
            menuExpanded = expanded && data.isNotEmpty()
        }
    ) {
        OutlinedButton(onClick = {}, modifier) { Text(currentText) }
        ExposedDropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
            if (header != null) {
                Text(header, Modifier.padding(5.dp), fontWeight = FontWeight.Bold)
                Divider(modifier = Modifier.padding(top = 5.dp))
            }

            for (item in data) {
                DropdownMenuItem(
                    enabled = true,
                    modifier = Modifier,
                    contentPadding = PaddingValues(5.dp),
                    interactionSource = MutableInteractionSource(),
                    onClick = {
                        menuExpanded = false
                        onItemSelected(item)
                    }
                ) {
                    Text(item.toString())
                }
            }
        }
    }
}