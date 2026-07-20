package com.gps.warehouse.ui.assets_screens.map

import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.gps.warehouse.data.remote.assets_dto.map.AssetPosition
import com.gps.warehouse.data.remote.assets_dto.map.Workshop
import com.gps.warehouse.data.remote.assets_dto.map.hexToColor
import com.gps.warehouse.ui.AssetViewModel
import kotlin.math.max
import kotlin.math.min

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssetsMapScreen(
    navController: NavController,
    viewModel: AssetViewModel = hiltViewModel()
) {
    val TAG = "LOG MapDebug"
    val uiState by viewModel.mapData.collectAsState()

    var userScale by remember { mutableFloatStateOf(1f) }
    var userOffset by remember { mutableStateOf(Offset.Zero) }

    val transformableState = rememberTransformableState { zoomChange, panChange, _ ->
        userScale = (userScale * zoomChange).coerceIn(0.5f, 5f)
        userOffset += panChange
    }

    LaunchedEffect(Unit) {
        viewModel.loadMapData()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Карта активов") },
                actions = {
                    IconButton(onClick = { viewModel.loadMapData() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Обновить")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is AssetViewModel.AssetUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is AssetViewModel.AssetUiState.Error -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Ошибка: ${state.message}")
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { viewModel.loadMapData() }) { Text("Повторить") }
                    }
                }
                is AssetViewModel.AssetUiState.MapSuccess -> {

                    state.workshops.forEach {
                        Log.d(TAG, "${it.workshopId} ${it.geometry} ${it.workshopWidth} ${it.workshopHeight} ${it.offsetX} ${it.offsetY}")
                    }

                    MapCanvas(
                        workshops = state.workshops,
                        assets = state.assets,
                        userScale = userScale,
                        userOffset = userOffset,
                        transformableState = transformableState,
                        onAssetClick = { assetId ->
                            navController.navigate("asset_details/$assetId")
                        }
                    )

                    MapControls(
                        scale = userScale,
                        onZoomIn = { userScale = min(userScale * 1.25f, 5f) },
                        onZoomOut = { userScale = max(userScale * 0.8f, 0.5f) },
                        onReset = {
                            userScale = 1f
                            userOffset = Offset.Zero
                        },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp)
                    )
                }

                else -> {}
            }
        }
    }
}

@Composable
private fun MapCanvas(
    workshops: List<Workshop>,
    assets: List<AssetPosition>,
    userScale: Float,
    userOffset: Offset,
    transformableState: androidx.compose.foundation.gestures.TransformableState,
    onAssetClick: (Int) -> Unit
) {
    val textMeasurer = rememberTextMeasurer()
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    // 1. ЖЕСТКАЯ ПРИВЯЗКА К 2000x2000 (как в бэкенде)
    val mapWidth = 2000f
    val mapHeight = 2000f

    // 2. МАСШТАБИРОВАНИЕ: Вписываем карту 2000x2000 в размер Canvas
    val fitScale = if (canvasSize.width == 0 || canvasSize.height == 0) {
        1f
    } else {
        min(
            canvasSize.width.toFloat() / mapWidth,
            canvasSize.height.toFloat() / mapHeight
        )
    }

    val totalScale = fitScale * userScale

    // Центрируем карту 2000x2000 на экране + применяем пользовательский сдвиг
    val centerOffset = if (canvasSize.width == 0 || canvasSize.height == 0) {
        Offset.Zero
    } else {
        Offset(
            (canvasSize.width - mapWidth * fitScale) / 2f,
            (canvasSize.height - mapHeight * fitScale) / 2f
        )
    }
    val totalOffset = centerOffset + userOffset

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { canvasSize = it }
            .transformable(state = transformableState)
            .pointerInput(assets, totalScale, totalOffset) {
                detectTapGestures { tapOffset ->
                    // 3. ПРОСТОЕ И ТОЧНОЕ ОБРАТНОЕ ПРЕОБРАЗОВАНИЕ (без minX/minY)
                    val mapX = (tapOffset.x - totalOffset.x) / totalScale
                    val mapY = (tapOffset.y - totalOffset.y) / totalScale

                    // Радиус попадания в координатах карты (30 пикселей на экране)
                    val hitRadiusInMap = 30f / totalScale

                    for (asset in assets) {
                        val dx = mapX - asset.x.toFloat()
                        val dy = mapY - asset.y.toFloat()
                        if (dx * dx + dy * dy < hitRadiusInMap * hitRadiusInMap) {
                            // Защита от asset_id = 0
                            val targetId = if (asset.assetId > 0) asset.assetId else asset.id
                            onAssetClick(targetId)
                            return@detectTapGestures
                        }
                    }
                }
            }
    ) {
        // 4. ТРАНСФОРМАЦИЯ БЕЗ ИСКУССТВЕННЫХ СДВИГОВ
        translate(left = totalOffset.x, top = totalOffset.y) {
            scale(scaleX = totalScale, scaleY = totalScale) {

                // Фон строго 2000x2000
                drawRect(
                    color = Color(0xFFEEEEEE),
                    topLeft = Offset.Zero,
                    size = Size(mapWidth, mapHeight)
                )

                drawGrid(mapWidth, mapHeight)

                workshops.forEach { workshop ->
                    drawWorkshop(workshop, textMeasurer)
                }

                assets.forEach { asset ->
                    drawAsset(asset, textMeasurer)
                }
            }
        }
    }
}

private fun DrawScope.drawGrid(mapWidth: Float, mapHeight: Float) {
    val gridColor = Color(0xFFD0D0D0)
    val step = 100f

    var x = 0f
    while (x <= mapWidth) {
        drawLine(color = gridColor, start = Offset(x, 0f), end = Offset(x, mapHeight), strokeWidth = 1f)
        x += step
    }
    var y = 0f
    while (y <= mapHeight) {
        drawLine(color = gridColor, start = Offset(0f, y), end = Offset(mapWidth, y), strokeWidth = 1f)
        y += step
    }
}

private fun DrawScope.drawWorkshop(
    workshop: Workshop,
    textMeasurer: androidx.compose.ui.text.TextMeasurer
) {
    val color = hexToColor(workshop.color)

    // Отладочный вывод
    Log.d("MapDebug", "Drawing workshop: ${workshop.code}, geometry: ${workshop.geometry}, width: ${workshop.workshopWidth}, height: ${workshop.workshopHeight}, offset: ${workshop.offsetX},${workshop.offsetY}")

    // Полигон (координаты уже абсолютные, как в API)
    if (workshop.geometry != null && workshop.geometry.type == "polygon" && workshop.geometry.coordinates.isNotEmpty()) {
        val coords = workshop.geometry.coordinates
        val path = Path().apply {
            moveTo(coords[0][0].toFloat(), coords[0][1].toFloat())
            for (i in 1 until coords.size) {
                lineTo(coords[i][0].toFloat(), coords[i][1].toFloat())
            }
            close()
        }
        drawPath(path = path, color = color.copy(alpha = 0.55f))
        drawPath(path = path, color = color, style = Stroke(width = 3f))

        val cx = coords.map { it[0] }.average().toFloat()
        val cy = coords.map { it[1] }.average().toFloat()
        drawWorkshopLabel("${workshop.name}\n${workshop.code}", cx, cy, textMeasurer)
    }
    // Прямоугольник (абсолютные offsetX/Y + ширина/высота)
    else {
        val w = (workshop.workshopWidth ?: 0).toFloat()
        val h = (workshop.workshopHeight ?: 0).toFloat()
        val x = workshop.offsetX.toFloat()
        val y = workshop.offsetY.toFloat()

        Log.d("MapDebug", "  -> Rectangle: x=$x, y=$y, w=$w, h=$h")

        if (w > 0 && h > 0) {
            drawRect(color = color.copy(alpha = 0.55f), topLeft = Offset(x, y), size = Size(w, h))
            drawRect(color = color, topLeft = Offset(x, y), size = Size(w, h), style = Stroke(width = 3f))
            drawWorkshopLabel("${workshop.name}\n${workshop.code}", x + w / 2f, y + h / 2f, textMeasurer)
            Log.d("MapDebug", "  -> Drawn successfully!")
        } else {
            Log.e("MapDebug", "  -> SKIPPED: w=$w, h=$h")
        }
    }
}

private fun DrawScope.drawWorkshopLabel(
    text: String,
    centerX: Float,
    centerY: Float,
    textMeasurer: androidx.compose.ui.text.TextMeasurer
) {
    val textLayoutResult = textMeasurer.measure(
        text = text,
        style = TextStyle(fontSize = 16.sp, color = Color.White)
    )
    drawText(
        textLayoutResult = textLayoutResult,
        topLeft = Offset(
            centerX - textLayoutResult.size.width / 2f,
            centerY - textLayoutResult.size.height / 2f
        )
    )
}

private fun DrawScope.drawAsset(
    asset: AssetPosition,
    textMeasurer: androidx.compose.ui.text.TextMeasurer
) {
    // Координаты актива АБСОЛЮТНЫЕ в пространстве 2000x2000
    val x = asset.x.toFloat()
    val y = asset.y.toFloat()
    val markerRadius = 18f

    // Тень
    drawCircle(color = Color.Black.copy(alpha = 0.3f), radius = markerRadius, center = Offset(x + 2f, y + 3f))
    // Внешний круг (красный)
    drawCircle(color = Color(0xFFE53935), radius = markerRadius, center = Offset(x, y))
    // Внутренний круг (белый)
    drawCircle(color = Color.White, radius = markerRadius * 0.45f, center = Offset(x, y))

    // Защита от id=0: Если asset_id равен 0, используем id позиции
    val displayId = if (asset.assetId > 0) asset.assetId else asset.id

    val textLayoutResult = textMeasurer.measure(
        text = "A$displayId",
        style = TextStyle(fontSize = 14.sp, color = Color.Black)
    )
    drawText(
        textLayoutResult = textLayoutResult,
        topLeft = Offset(
            x - textLayoutResult.size.width / 2f,
            y + markerRadius + 4f
        )
    )
}

@Composable
private fun MapControls(
    scale: Float,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Surface(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
            tonalElevation = 2.dp
        ) {
            Text(
                text = "${(scale * 100).toInt()}%",
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelMedium
            )
        }
        FloatingActionButton(onClick = onZoomIn, modifier = Modifier.size(48.dp), containerColor = MaterialTheme.colorScheme.primaryContainer) { Text("+", fontSize = 20.sp) }
        FloatingActionButton(onClick = onZoomOut, modifier = Modifier.size(48.dp), containerColor = MaterialTheme.colorScheme.primaryContainer) { Text("−", fontSize = 20.sp) }
        FloatingActionButton(onClick = onReset, modifier = Modifier.size(48.dp), containerColor = MaterialTheme.colorScheme.secondaryContainer) { Icon(Icons.Default.Refresh, contentDescription = "Сброс") }
    }
}