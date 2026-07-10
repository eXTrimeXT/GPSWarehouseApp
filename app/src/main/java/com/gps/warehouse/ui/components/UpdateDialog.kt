package com.gps.warehouse.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.window.DialogProperties
import com.gps.warehouse.utils.UpdateManager

@Composable
fun UpdateDialog(
    currentVersionName: String,
    remoteVersion: UpdateManager.VersionInfo,
    onDownload: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        modifier = Modifier,
        onDismissRequest = onDismiss,
        title = { Text("Доступно обновление!") },
        text = {
            Text(
                "Доступна новая версия:\n" +
                        "• Текущая: $currentVersionName\n" +
                        "• Новая: ${remoteVersion.versionName}\n\n" +
                        "Скачать обновление?\nУстановка начнётся автоматически после загрузки."
            )
        },
        confirmButton = {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            )
            {
                Button(
                    onClick = {
                        onDismiss()
                        onDownload()
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Скачать")
                }
            }
        },
        dismissButton = {
//            TextButton(onClick = onDismiss) {
//                Text("Позже")
//            }
        },
        properties = DialogProperties(dismissOnClickOutside = true, dismissOnBackPress = true)
    )
}

@Preview
@Composable
fun PreviewUpdateDialog() {
    UpdateDialog(
        currentVersionName = "1.0.0",
        remoteVersion = UpdateManager.VersionInfo(
            jobId = 1,
            version = 1,
            versionCode = 1,
            versionName = "1.2.3"
        ),
        onDownload = {},
        onDismiss = {}
    )
}