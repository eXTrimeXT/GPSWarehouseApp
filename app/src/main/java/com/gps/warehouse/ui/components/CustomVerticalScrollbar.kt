package com.gps.warehouse.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun CustomVerticalScrollbar(
    scrollState: ScrollState,
    modifier: Modifier = Modifier,
    color: Color = Color.DarkGray.copy(alpha = 0.6f)
) {
    val viewportSize = scrollState.viewportSize.toFloat()
    val maxValue = scrollState.maxValue.toFloat()
    val value = scrollState.value.toFloat()

    // Если прокрутка не требуется (контент помещается), скрываем ползунок
    if (maxValue <= 0f) return

    val total = viewportSize + maxValue
    val thumbHeightFraction = (viewportSize / total).coerceIn(0.1f, 1f)
    val thumbPositionFraction = value / total

    Canvas(
        modifier = modifier
            .width(8.dp)
            .fillMaxHeight()
    ) {
        val thumbHeight = size.height * thumbHeightFraction
        val thumbY = size.height * thumbPositionFraction

        drawRoundRect(
            color = color,
            topLeft = Offset(0f, thumbY),
            size = Size(width = size.width, height = thumbHeight),
            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
        )
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "Dialog Preview", device = "spec:width=400dp,height=1450dp")
@Composable
fun CustomVerticalScrollbarPreview(){
    Box(modifier = Modifier.fillMaxWidth()) {
        CustomVerticalScrollbar(
            scrollState = rememberScrollState(),
            color = Color.Red,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 4.dp)
        )
    }
}