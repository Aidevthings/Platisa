package com.platisa.app.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.platisa.app.ui.theme.NeonCyan
import com.platisa.app.ui.theme.NeonPurple

@Composable
fun PlatisaInput(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    isError: Boolean = false,
    readOnly: Boolean = false,
    enabled: Boolean = true,
    suffix: @Composable (() -> Unit)? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = NeonCyan,
            unfocusedBorderColor = NeonPurple,
            focusedLabelColor = NeonCyan,
            unfocusedLabelColor = NeonPurple,
            cursorColor = NeonCyan,
            errorBorderColor = androidx.compose.ui.graphics.Color.Red,
            disabledBorderColor = NeonPurple.copy(alpha = 0.5f),
            disabledLabelColor = NeonPurple.copy(alpha = 0.5f),
            disabledTextColor = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.6f)
        ),
        shape = RoundedCornerShape(12.dp),
        keyboardOptions = keyboardOptions,
        isError = isError,
        readOnly = readOnly,
        enabled = enabled,
        suffix = suffix
    )
}

