package com.gps.warehouse.ui.assets_screens.assets_classes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.gps.warehouse.data.remote.assets_dto.AssetClassDto
import com.gps.warehouse.ui.AssetViewModel
import com.gps.warehouse.ui.assets_screens.assets.DetailRow
import com.gps.warehouse.ui.components.MyCustomActionBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssetClassDetailsScreen(
    classId: Int,
    navController: NavHostController,
    viewModel: AssetViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(classId) {
        viewModel.loadAssetClassDetails(classId)
    }

    Scaffold(
        topBar = {
            MyCustomActionBar(
                text = "Детали класса актива",
                onBackClick = { navController.popBackStack() }
            )
        }
    ) { paddingValues ->
        when (val state = uiState) {
            is AssetViewModel.AssetUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is AssetViewModel.AssetUiState.AssetClassDetailsLoaded -> {
                AssetClassDetailsContent(
                    assetClass = state.assetClass,
                    modifier = Modifier.padding(paddingValues)
                )
            }
            is AssetViewModel.AssetUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = state.message,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.loadAssetClassDetails(classId) }) {
                            Text("Повторить")
                        }
                    }
                }
            }
            else -> {}
        }
    }
}

@Composable
fun AssetClassDetailsContent(
    assetClass: AssetClassDto,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ==================== 1. Основная информация ====================
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Основная информация",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(12.dp))
                DetailRow("Название класса", assetClass.className)
                DetailRow("Описание", assetClass.description)
            }
        }

        // ==================== 2. Тип актива ====================
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Тип актива",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(12.dp))

                val type = assetClass.assetType
                DetailRow("Название (RU)", type?.name)
                DetailRow("Название (EN)", type?.enName)
                DetailRow("ID типа (из класса)", assetClass.classTypeId.toString())
                DetailRow("ID типа (из объекта)", type?.assetTypeId?.toString())
            }
        }

        // ==================== 3. Аудит (Создатель и обновивший) ====================
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Аудит",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(12.dp))

                // === Создатель ===
                Text(
                    text = "Создатель",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))

                val creator = assetClass.creator
                DetailRow("ФИО", creator?.owner)
                DetailRow("Табельный номер", creator?.userTabId)
                DetailRow("Должность", creator?.userPosition)
                DetailRow("Email", creator?.email)
                DetailRow("Телефон", creator?.phone)
                DetailRow("ID создателя (FK)", assetClass.createdBy?.toString())

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // === Обновивший ===
                Text(
                    text = "Обновивший",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))

                val updater = assetClass.updater
                DetailRow("ФИО", updater?.owner)
                DetailRow("Табельный номер", updater?.userTabId)
                DetailRow("Должность", updater?.userPosition)
                DetailRow("Email", updater?.email)
                DetailRow("Телефон", updater?.phone)
                DetailRow("ID обновившего (FK)", assetClass.updatedBy?.toString())
            }
        }

        // ==================== 4. Даты ====================
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Даты",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(12.dp))
                DetailRow("Создано", assetClass.createdAt)
                DetailRow("Обновлено", assetClass.updatedAt)
            }
        }

        // ==================== 5. Техническая информация ====================
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Техническая информация",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(12.dp))
                DetailRow("ID класса", assetClass.classId.toString())
                DetailRow("ID типа (FK)", assetClass.classTypeId.toString())
            }
        }
    }
}