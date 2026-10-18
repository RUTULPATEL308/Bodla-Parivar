package com.bodla.parivar.core.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.bodla.parivar.core.theme.*
import com.bodla.parivar.core.util.GujlishTransliteration
import kotlinx.coroutines.launch

/**
 * Reusable Compose Gujlish TextField component.
 * Automatically transliterates phonetic English (e.g. "namaste", "kem cho")
 * into Gujarati script ("નમસ્તે", "કેમ છો") when Space is typed,
 * matching web app / indiatyping.com behavior.
 */
@Composable
fun GujlishTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: (@Composable () -> Unit)? = null,
    placeholder: (@Composable () -> Unit)? = null,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    singleLine: Boolean = false,
    minLines: Int = 1,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    isError: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    colors: TextFieldColors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = WarmWhiteSurface,
        unfocusedContainerColor = WarmWhiteSurface,
        focusedBorderColor = SaffronPrimary,
        unfocusedBorderColor = BorderStroke
    ),
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(12.dp),
    enabled: Boolean = true
) {
    val coroutineScope = rememberCoroutineScope()
    var textFieldValue by remember {
        mutableStateOf(TextFieldValue(text = value, selection = TextRange(value.length)))
    }

    // Keep internal state in sync with external value if changed externally
    LaunchedEffect(value) {
        if (value != textFieldValue.text) {
            textFieldValue = textFieldValue.copy(
                text = value,
                selection = TextRange(value.length.coerceAtMost(value.length))
            )
        }
    }

    OutlinedTextField(
        value = textFieldValue,
        onValueChange = { newTfv ->
            val prevText = textFieldValue.text
            val currText = newTfv.text

            // Check if a space or newline was just typed
            val cursor = newTfv.selection.end
            if (currText.length > prevText.length && cursor > 0 && (currText[cursor - 1] == ' ' || currText[cursor - 1] == '\n')) {
                val textBeforeCursor = currText.substring(0, cursor - 1)
                val lastWordMatch = Regex("([a-zA-Z0-9]+)$").find(textBeforeCursor)
                if (lastWordMatch != null) {
                    val englishWord = lastWordMatch.value
                    val prefix = textBeforeCursor.substring(0, lastWordMatch.range.first)
                    val textAfterCursor = currText.substring(cursor)
                    val separator = currText[cursor - 1]

                    // Show separator immediately
                    textFieldValue = newTfv
                    onValueChange(currText)

                    coroutineScope.launch {
                        val gujaratiWord = GujlishTransliteration.transliterateWord(englishWord)
                        if (gujaratiWord != englishWord) {
                            val convertedText = prefix + gujaratiWord + separator + textAfterCursor
                            val newCursor = prefix.length + gujaratiWord.length + 1
                            textFieldValue = TextFieldValue(
                                text = convertedText,
                                selection = TextRange(newCursor)
                            )
                            onValueChange(convertedText)
                        }
                    }
                    return@OutlinedTextField
                }
            }

            textFieldValue = newTfv
            onValueChange(currText)
        },
        modifier = modifier,
        label = label,
        placeholder = placeholder,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        singleLine = singleLine,
        minLines = minLines,
        maxLines = maxLines,
        isError = isError,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        colors = colors,
        shape = shape,
        enabled = enabled
    )
}
