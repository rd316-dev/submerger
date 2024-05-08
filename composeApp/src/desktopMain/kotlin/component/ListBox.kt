package component

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun HorizontalListBox(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.spacedBy(10.dp),
    content: @Composable RowScope.() -> Unit
) {
    Box(modifier = modifier) {
        val scrollState = rememberScrollState(0)
        Box(modifier = Modifier.horizontalScroll(scrollState)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = horizontalArrangement,
            ) {
                content()
            }
        }
        HorizontalScrollbar(
            adapter = rememberScrollbarAdapter(scrollState),
            modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth()
        )
    }
}

@Composable
fun <T> HorizontalListBox(
    data: Collection<T>,
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.spacedBy(10.dp),
    content: @Composable RowScope.(T) -> Unit
) {
    HorizontalListBox(modifier, horizontalArrangement) {
        for (i in data) {
            content(i)
        }
    }
}

@Composable
fun VerticalListBox(
    modifier: Modifier = Modifier,
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(10.dp),
    isEmpty: Boolean = false,
    emptyContent: @Composable BoxScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit
) {
    Box(modifier = modifier) {
        if (isEmpty) {
            emptyContent()
        } else {
            val scrollState = rememberScrollState(0)
            Column(modifier = Modifier.verticalScroll(scrollState)) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = verticalArrangement,
                ) {
                    content()
                }
            }
            VerticalScrollbar(
                adapter = rememberScrollbarAdapter(scrollState),
                modifier = Modifier.align(Alignment.TopEnd).fillMaxHeight()
            )
        }
    }
}

@Composable
fun <T> VerticalListBox(
    data: Collection<T>,
    modifier: Modifier = Modifier,
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(10.dp),
    emptyContent: @Composable BoxScope.() -> Unit = {},
    content: @Composable ColumnScope.(T) -> Unit
) {
    VerticalListBox(modifier, verticalArrangement, data.isEmpty(), emptyContent) {
        for (i in data) {
            content(i)
        }
    }
}