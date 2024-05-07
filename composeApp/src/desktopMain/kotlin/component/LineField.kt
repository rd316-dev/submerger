package component

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun LineField(
    value: TextFieldValue,
    width: Dp = 300.dp,
    padding: Dp = 8.dp,
    onValueChange: (TextFieldValue) -> Unit
) {
    val inactiveColor = Color.LightGray
    val activeColor = MaterialTheme.colors.primary

    var isFocused by remember { mutableStateOf(false) }

    Box (modifier = Modifier.width(width)) {
        BasicTextField(
            value = value,
            singleLine = true,
            modifier = Modifier
                .padding(padding)
                .onFocusChanged { state -> isFocused = state.isFocused },
            onValueChange = onValueChange
        )
        Divider(color = inactiveColor, thickness = 2.dp, modifier = Modifier.width(width).align(Alignment.BottomCenter))

        AnimatedVisibility(
            visible = isFocused,
            modifier = Modifier.width(width).align(Alignment.BottomCenter),
            enter = scaleIn(animationSpec = tween(durationMillis = 100)),
            exit = scaleOut(animationSpec = tween(durationMillis = 100))
        ) {
            Divider(
                color = activeColor,
                thickness = 2.dp
            )
        }
    }
}