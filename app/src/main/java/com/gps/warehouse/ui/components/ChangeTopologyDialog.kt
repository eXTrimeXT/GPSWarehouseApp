package com.gps.warehouse.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gps.warehouse.data.remote.gps_dto.TopologyDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangeTopologyDialog(
    selectedTopologies: List<TopologyDto>,
    allTopologies: List<TopologyDto>,
    currentMaterial: String,
    onDismiss: () -> Unit,
    onToggleTopology: (TopologyDto) -> Unit,
    onConfirm: () -> Unit,
    onCameraScanClick: () -> Unit
) {
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
                Text("Изменить топологии", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(currentMaterial, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.heightIn(max = 400.dp)
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (allTopologies.isEmpty()) {
                        item {
                            Text("Нет доступных позиций", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 8.dp))
                        }
                    } else {
                        items(allTopologies) { topology ->
                            val isSelected = selectedTopologies.any { it.id == topology.id }

                            Row(
                                modifier = Modifier.fillMaxWidth().clickable { onToggleTopology(topology) }.padding(vertical = 8.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = { onToggleTopology(topology) }
                                )
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text(text = topology.position, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                                    if (topology.positionScan != topology.position) {
                                        Text(text = "Штрихкод: ${topology.positionScan}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider()

                OutlinedButton(
                    onClick = onCameraScanClick,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.QrCodeScanner, null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Сканировать для добавления/удаления")
                }
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Сохранить (${selectedTopologies.size})")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}

@Preview
@Composable
fun ChangeTopologyDialogPreview() {
    MaterialTheme {
        Surface {
            ChangeTopologyDialog(
                selectedTopologies = listOf(
                    TopologyDto(id = "1", position = "BUFF", positionScan = "BUFF")
                ),
                allTopologies = listOf(
                    TopologyDto(id = "1", position = "BUFF", positionScan = "BUFF"),
                    TopologyDto(id = "2", position = "A-01", positionScan = "A1"),
                    TopologyDto(id = "3", position = "C-12", positionScan = "C12")
                ),
                currentMaterial = "LA2346814",
                onDismiss = {},
                onToggleTopology = {},
                onConfirm = {},
                onCameraScanClick = {}
            )
        }
    }
}