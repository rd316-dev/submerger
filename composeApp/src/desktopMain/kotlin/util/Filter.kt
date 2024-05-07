package util

import androidx.compose.ui.text.coerceIn
import androidx.compose.ui.text.input.TextFieldValue

fun intFilter(
    oldValue: TextFieldValue,
    newValue: TextFieldValue,
    min: Int = Int.MIN_VALUE,
    max: Int = Int.MAX_VALUE,
    prefix: String = "",
    suffix: String = ""
) : Pair<TextFieldValue, Int> {
    val actualValue = newValue.text.removePrefix(prefix).removeSuffix(suffix)
    if (actualValue.isBlank()) {
        return Pair(TextFieldValue("${prefix}0${suffix}"), 0)
    }

    val oldIntValue = oldValue.text.removePrefix(prefix).removeSuffix(suffix).toInt()

    val intValue = actualValue.toIntOrNull() ?: return Pair(oldValue, oldIntValue);
    if (intValue < min || intValue > max) {
        return Pair(oldValue, oldIntValue)
    }

    val numericText = intValue.toString()
    val resultText = prefix + numericText + suffix
    return if (newValue.text != oldValue.text && resultText == oldValue.text) {
        Pair(oldValue, oldIntValue)
    } else if (newValue.selection.max < numericText.length) {
        Pair(newValue.copy(text = resultText), intValue)
    } else {
        Pair(newValue.copy(
            text = resultText,
            selection = newValue.selection.coerceIn(prefix.length, numericText.length + prefix.length)
        ), intValue)
    }
}