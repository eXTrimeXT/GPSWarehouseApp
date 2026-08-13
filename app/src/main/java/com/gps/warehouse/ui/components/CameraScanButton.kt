package com.gps.warehouse.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun CameraScanButton(
    cameraScanEnabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Если настройка выключена — кнопки просто нет в UI
    if (!cameraScanEnabled) return

    OutlinedButton(
        onClick = onClick,
        modifier = modifier
    ) {
        Icon(Icons.Default.QrCodeScanner, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text("Сканировать")
    }
}

@Preview(showBackground = true)
@Composable
fun CameraScanButtonPreview(){
    CameraScanButton(
        cameraScanEnabled = true,
        onClick = {},
    )
}