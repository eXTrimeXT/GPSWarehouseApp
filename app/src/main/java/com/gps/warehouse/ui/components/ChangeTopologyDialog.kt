package com.gps.warehouse.ui.components

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gps.warehouse.data.remote.gps_dto.TopologyDto

/**
 * Комбинированный диалог изменения топологии.
 * Три способа работают параллельно:
 * 1. Dropdown (ручной выбор)
 * 2. Камера (кнопка → CameraScannerDialog поверх)
 * 3. Honeywell сканер (в фоне, через callback onScanned)
 *
 * При получении значения — автосохранение без подтверждения.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangeTopologyDialog(
    currentPosition: String,
    topologies: List<TopologyDto>,
    currentMaterial: String,
    onDismiss: () -> Unit,
    onTopologySelected: (TopologyDto) -> Unit,
    onCameraScanClick: () -> Unit
) {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Check, contentDescription = null)
                }
            }
        },
        title = {
            Column {
                Text(
                    "Изменить топологию",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    currentMaterial,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Текущая позиция
//                Surface(
//                    shape = RoundedCornerShape(8.dp),
//                    color = MaterialTheme.colorScheme.surfaceVariant,
//                    modifier = Modifier.fillMaxWidth()
//                ) {
//                    Row(
//                        modifier = Modifier.padding(12.dp),
//                        verticalAlignment = Alignment.CenterVertically
//                    ) {
//                        Text(
//                            "Текущая:",
//                            style = MaterialTheme.typography.bodySmall,
//                            color = MaterialTheme.colorScheme.onSurfaceVariant
//                        )
//                        Spacer(Modifier.width(8.dp))
//                        Text(
//                            currentPosition.ifBlank { "—" },
//                            style = MaterialTheme.typography.bodyMedium,
//                            fontWeight = FontWeight.SemiBold
//                        )
//                    }
//                }

                // Dropdown
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = currentPosition,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Выбрать из списка") },
                        placeholder = { Text("Нажмите для выбора...") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        singleLine = true
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        if (topologies.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("Нет доступных позиций") },
                                onClick = { expanded = false },
                                enabled = false
                            )
                        } else {
                            topologies.forEach { topology ->
                                DropdownMenuItem(
                                    text = { Text(topology.position) },
                                    onClick = {
                                        expanded = false
                                        onTopologySelected(topology)
                                    },
                                    leadingIcon = {
                                        if (topology.position == currentPosition) {
                                            Icon(
                                                Icons.Default.Check,
                                                null,
                                                modifier = Modifier.size(16.dp),
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                // Разделитель
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HorizontalDivider(modifier = Modifier.weight(1f))
                    Text(
                        "или",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                    HorizontalDivider(modifier = Modifier.weight(1f))
                }

                // Кнопка камеры
                OutlinedButton(
                    onClick = onCameraScanClick,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        Icons.Default.QrCodeScanner,
                        null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Сканировать камерой")
                }

                // Подсказка про сканер
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.QrCodeScanner,
                            null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Или используйте встроенный сканер!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}

@Preview
@Composable
fun ChangeTopologyDialogPreview(){
    ChangeTopologyDialog(
        currentPosition = "BUFF",
        topologies = emptyList(),
        currentMaterial = "LA2346814",
        onDismiss = {},
        onTopologySelected = {},
        onCameraScanClick = {}
    )
}