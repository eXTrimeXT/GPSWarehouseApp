package com.gps.warehouse.ui.assets_screens.assets_models

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
import com.gps.warehouse.data.remote.assets_dto.AssetModelDto
import com.gps.warehouse.ui.AssetViewModel
import com.gps.warehouse.ui.components.MyCustomActionBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssetModelDetailsScreen(
    modelId: Int,
    navController: NavHostController,
    viewModel: AssetViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(modelId) {
        viewModel.loadAssetModelDetails(modelId)
    }

    Scaffold(
        topBar = {
            MyCustomActionBar(
                text = "Детали модели актива",
                onBackClick = { navController.popBackStack() }
            )
        }
    ) { paddingValues ->
        when (val state = uiState) {
            is AssetViewModel.AssetUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }
            }
            is AssetViewModel.AssetUiState.AssetModelDetailsLoaded -> {
                AssetModelDetailsContent(
                    model = state.model,
                    modifier = Modifier.padding(paddingValues)
                )
            }
            is AssetViewModel.AssetUiState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = state.message, color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.loadAssetModelDetails(modelId) }) {
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
fun AssetModelDetailsContent(
    model: AssetModelDto,
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
                Text("Основная информация", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(12.dp))

                DetailRow("Название модели", model.modelName)
                DetailRow("Описание", model.description)
                DetailRow("Статус", if (model.isActive) "Активна" else "Неактивна")
                DetailRow("Требуется серийный номер", if (model.isSerialRequired) "Да" else "Нет")
            }
        }

        // ==================== 2. Класс и тип актива ====================
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Классификация", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(12.dp))

                val assetClass = model.assetClass
                DetailRow("Название класса", assetClass?.className)
                DetailRow("Описание класса", assetClass?.description)

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                val assetType = assetClass?.assetType
                DetailRow("Тип актива (RU)", assetType?.name)
                DetailRow("Тип актива (EN)", assetType?.enName)

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                DetailRow("ID класса", model.classId.toString())
            }
        }

        // ==================== 3. Аудит ====================
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Аудит", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(12.dp))

                // Создатель
                Text("Создатель", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.height(4.dp))
                val creator = model.creator
                DetailRow("ФИО", creator?.owner)
                DetailRow("Табельный номер", creator?.userTabId)
                DetailRow("Должность", creator?.userPosition)
                DetailRow("Email", creator?.email)
                DetailRow("ID создателя (FK)", model.createdBy?.toString())

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // Обновивший
                Text("Обновивший", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.height(4.dp))
                val updater = model.updater
                DetailRow("ФИО", updater?.owner)
                DetailRow("Табельный номер", updater?.userTabId)
                DetailRow("Должность", updater?.userPosition)
                DetailRow("Email", updater?.email)
                DetailRow("ID обновившего (FK)", model.updatedBy?.toString())
            }
        }

        // ==================== 4. Даты и ID ====================
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Техническая информация", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(12.dp))

                DetailRow("ID модели", model.modelId.toString())
                DetailRow("Создано", model.createdAt)
                DetailRow("Обновлено", model.updatedAt)
            }
        }
    }
}

// Убедитесь, что функция DetailRow доступна здесь (можно импортировать из AssetDetailsScreen.kt или продублировать)
@Composable
fun DetailRow(label: String, value: String?) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.4f)
        )
        Text(
            text = value ?: "",
            style = MaterialTheme.typography.bodyMedium,
            color = if (value.isNullOrBlank())
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
            else
                MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(0.6f)
        )
    }
}