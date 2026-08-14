package com.gps.warehouse.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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

    FloatingActionButton(
        onClick = onClick,
        modifier = modifier.padding(16.dp),
        containerColor = Color.White,
    ) {
        Icon(
            imageVector = Icons.Default.QrCodeScanner,
            contentDescription = "Сканировать",
            tint = Color.Black
        )
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