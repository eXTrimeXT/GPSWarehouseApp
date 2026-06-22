package com.gps.warehouse.ui.assets_screens.map

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.gps.warehouse.data.remote.assets_dto.AssetPositionDto
import com.gps.warehouse.data.remote.assets_dto.WorkshopDto
import com.gps.warehouse.ui.AssetViewModel
import com.gps.warehouse.ui.components.MyCustomActionBar
import kotlin.collections.forEach

@Composable
fun AssetMapScreen(
    navController: NavHostController,
    assetMapViewModel: AssetMapViewModel = hiltViewModel(),
    assetViewModel: AssetViewModel = hiltViewModel() // Добавляем AssetViewModel
) {
    val mapUiState by assetMapViewModel.uiState.collectAsState()
    val assetUiState by assetViewModel.uiState.collectAsState()

    // Мапа assetId -> name для быстрого доступа
    val assetNames = remember(assetUiState) {
        when (assetUiState) {
            is AssetViewModel.AssetUiState.AssetsLoaded -> {
                (assetUiState as AssetViewModel.AssetUiState.AssetsLoaded)
                    .assets.associate { it.assetId to (it.name.takeIf { n -> n.isNotBlank() } ?: "Актив #${it.assetId}") }
            }
            else -> emptyMap()
        }
    }

    // Загружаем данные при входе
    LaunchedEffect(Unit) {
        assetMapViewModel.loadMapData()
        assetViewModel.loadAssets() // Загружаем список активов для имен
    }

    Scaffold(
        topBar = {
            MyCustomActionBar(
                onBackClick = { navController.popBackStack() },
                text = "Карта активов"
            )
        }
    ) { paddingValues ->
        when (val state = mapUiState) {
            is AssetMapUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is AssetMapUiState.Success -> {
                MapCanvas(
                    workshops = state.workshops,
                    positions = state.positions,
                    assetNames = assetNames, // Передаем имена активов
                    onAssetClick = { assetId -> // Колбэк для навигации
                        navController.navigate("asset_details/$assetId")
                    },
                    modifier = Modifier.padding(paddingValues)
                )
            }
            is AssetMapUiState.Error -> {
                Column(modifier = Modifier.fillMaxSize().padding(paddingValues), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Text(state.message, color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { assetMapViewModel.loadMapData() }) { Text("Повторить") }
                }
            }
        }
    }
}

@Composable
private fun MapCanvas(
    workshops: List<WorkshopDto>,
    positions: List<AssetPositionDto>,
    assetNames: Map<Int, String>,
    onAssetClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var isFirstRender by remember { mutableStateOf(true) }

    // Группируем позиции по workshop_id для быстрого доступа
    val positionsByWorkshop = remember(positions) { positions.groupBy { it.workshopId } }
    // Группируем цеха по ID для быстрого доступа при клике
    val workshopsById = remember(workshops) { workshops.associateBy { it.workshopId } }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                // Pan & Zoom
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(0.3f, 5.0f)
                    offsetX += pan.x
                    offsetY += pan.y
                }
            }
            .pointerInput(Unit) {
                // Обработка кликов по маркерам
                detectTapGestures { tapOffset ->
                    // Преобразуем экранные координаты тапа в координаты Canvas
                    // Формула обратная трансформации: translate(offsetX, offsetY) { scale(scale) { ... } }
                    val canvasX = (tapOffset.x - offsetX) / scale
                    val canvasY = (tapOffset.y - offsetY) / scale

                    // Проверяем попадание в каждый маркер
                    positions.forEach { pos ->
                        val workshop = workshopsById[pos.workshopId] ?: return@forEach

                        val ox = workshop.offset_x.toFloat()
                        val oy = workshop.offset_y.toFloat()
                        val sc = workshop.workshop_scale.toFloat()

                        // Координаты маркера в координатах Canvas (та же формула, что при отрисовке)
                        val markerX = pos.x.toFloat() * sc + ox
                        val markerY = pos.y.toFloat() * sc + oy

                        // Проверка попадания в квадрат 36x36px (маркер центрирован в markerX/Y)
                        if (canvasX in (markerX - 18f)..(markerX + 18f) &&
                            canvasY in (markerY - 18f)..(markerY + 18f)) {
                            onAssetClick(pos.assetId)
                            return@detectTapGestures
                        }
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            // Автоподгонка при первом рендере
            if (isFirstRender) {
                val bounds = calculateBounds(workshops)
                val padding = 40f
                val scaleX = (canvasWidth - padding * 2) / bounds.width
                val scaleY = (canvasHeight - padding * 2) / bounds.height
                scale = minOf(scaleX, scaleY, 1.5f).coerceIn(0.5f, 2.0f)
                offsetX = (canvasWidth - bounds.width * scale) / 2 - bounds.left * scale
                offsetY = (canvasHeight - bounds.height * scale) / 2 - bounds.top * scale
                isFirstRender = false
            }

            // Применяем трансформацию ко всему холсту
            translate(offsetX, offsetY) {
                scale(scale) {
                    drawGrid()
                    workshops.forEach { workshop ->
                        drawWorkshop(
                            workshop = workshop,
                            assetPositions = positionsByWorkshop[workshop.workshopId] ?: emptyList(),
                            assetNames = assetNames
                        )
                    }
                }
            }
        }

        // Кнопка сброса
        IconButton(
            onClick = { scale = 1f; offsetX = 1000f; offsetY = 0f; isFirstRender = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
        ) {
            Icon(Icons.Default.Refresh, "Сброс вида", tint = MaterialTheme.colorScheme.primary)
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawGrid() {
    val gridSize = 100f
    repeat(30) { i ->
        val pos = i * gridSize
        drawLine(Color(0xFFE0E0E0), Offset(pos, 0f), Offset(pos, 3000f), 1f)
        drawLine(Color(0xFFE0E0E0), Offset(0f, pos), Offset(3000f, pos), 1f)
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawWorkshop(
    workshop: WorkshopDto,
    assetPositions: List<AssetPositionDto>,
    assetNames: Map<Int, String> // Новый параметр
) {
    val baseColor = Color(workshop.color.toColorInt())
    val strokeColor = Color.Black
    val strokeWidth = 3f
    val sc = workshop.workshop_scale.toFloat()
    val ox = workshop.offset_x.toFloat()
    val oy = workshop.offset_y.toFloat()

    // 1. Рисуем контур цеха (без изменений)
    val path = Path()
    if (workshop.geometry?.type == "polygon" && workshop.geometry.coordinates.isNotEmpty()) {
        workshop.geometry.coordinates.forEachIndexed { i, point ->
            val x = (point[0].toFloat() * sc + ox)
            val y = (point[1].toFloat() * sc + oy)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        drawPath(path, color = baseColor, style = Fill)
        drawPath(path, color = strokeColor, style = Stroke(width = strokeWidth))
    } else if (workshop.workshop_width != null && workshop.workshop_height != null) {
        val w = (workshop.workshop_width.toFloat() * sc)
        val h = (workshop.workshop_height.toFloat() * sc)
        drawRect(color = baseColor, topLeft = Offset(ox, oy), size = Size(w, h))
        drawRect(color = strokeColor, topLeft = Offset(ox, oy), size = Size(w, h), style = Stroke(width = strokeWidth))
    }

    // 2. Рисуем маркеры активов с именами
    assetPositions.forEach { pos ->
        val ax = (pos.x.toFloat() + ox)
        val ay = (pos.y.toFloat() + oy)

        // Получаем имя актива из мапы или fallback
        val assetName = assetNames[pos.assetId] ?: "Актив #${pos.assetId}"
        val displayName = if (assetName.length > 12) "${assetName.take(10)}..." else assetName

        // Маркер (квадрат 36x36px)
        drawRect(
            color = Color(0xFF37474F),
            topLeft = Offset(ax - 18f, ay - 18f),
            size = Size(36f, 36f)
        )
        drawRect(
            color = strokeColor,
            topLeft = Offset(ax - 18f, ay - 18f),
            size = Size(36f, 36f),
            style = Stroke(width = 1.5f)
        )

        // Текст: сокращенное имя актива
        drawContext.canvas.nativeCanvas.drawText(
            displayName,
            ax,
            ay + 5f,
            android.graphics.Paint().apply {
                color = android.graphics.Color.WHITE
                textSize = 9f // Чуть меньше, чтобы вмещалось
                textAlign = android.graphics.Paint.Align.CENTER
                isAntiAlias = true
            }
        )
    }

    // 3. Текст названия цеха (без изменений)
    val centerX = if (workshop.geometry?.type == "polygon") {
        workshop.geometry.coordinates.map { it[0].toFloat() * sc + ox }.average().toFloat()
    } else {
        ox + (workshop.workshop_width?.toFloat() ?: 0f) / 2f
    }
    val centerY = if (workshop.geometry?.type == "polygon") {
        workshop.geometry.coordinates.map { it[1].toFloat() * sc + oy }.average().toFloat()
    } else {
        oy + (workshop.workshop_height?.toFloat() ?: 0f) / 2f
    }

    drawContext.canvas.nativeCanvas.apply {
        drawText(workshop.code, centerX, centerY, android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE; textSize = 24f; textAlign = android.graphics.Paint.Align.CENTER; isAntiAlias = true
        })
        drawText(workshop.name, centerX, centerY + 30f, android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE; textSize = 18f; textAlign = android.graphics.Paint.Align.CENTER; isAntiAlias = true
        })
    }
}

private fun calculateBounds(workshops: List<WorkshopDto>): Rect {
    var minX = Float.MAX_VALUE; var minY = Float.MAX_VALUE
    var maxX = -Float.MAX_VALUE; var maxY = -Float.MAX_VALUE
    workshops.forEach { w ->
        val ox = w.offset_x.toFloat(); val oy = w.offset_y.toFloat(); val sc = w.workshop_scale.toFloat()
        if (w.geometry?.type == "polygon" && w.geometry.coordinates.isNotEmpty()) {
            w.geometry.coordinates.forEach { c ->
                val x = c[0].toFloat() * sc + ox; val y = c[1].toFloat() * sc + oy
                if (x < minX) minX = x; if (y < minY) minY = y
                if (x > maxX) maxX = x; if (y > maxY) maxY = y
            }
        } else if (w.workshop_width != null) {
            val wW = w.workshop_width.toFloat() * sc + ox; val wH = (w.workshop_height?.toFloat() ?: 0f) * sc + oy
            if (ox < minX) minX = ox; if (oy < minY) minY = oy
            if (wW > maxX) maxX = wW; if (wH > maxY) maxY = wH
        }
    }
    return Rect(minX, minY, maxX, maxY)
}