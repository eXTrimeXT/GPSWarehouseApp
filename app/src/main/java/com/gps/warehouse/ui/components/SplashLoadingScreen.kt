package com.gps.warehouse.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.gps.warehouse.utils.Constants

/**
 * Переиспользуемый компонент экрана загрузки с анимацией.
 * Используется на Splash-экране при проверке сессии и в других местах,
 * где требуется показать процесс загрузки.
 *
 * @param message Текст, отображаемый под анимацией (по умолчанию "Проверка сессии...")
 * @param showLogo Показывать ли логотип приложения (по умолчанию true)
 */
@Composable
fun SplashLoadingScreen(
    message: String = "Загрузка ...",
    showLogo: Boolean = true,
    showTextLoading: Boolean = true,
) {
    Box(
        modifier = Modifier
            .fillMaxSize(),
//            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Логотип с анимацией вращения
            if (showLogo) {
                AnimatedLogo()
                Spacer(modifier = Modifier.height(24.dp))
            }

            if (showTextLoading) {
                // Анимированный индикатор загрузки
                AnimatedLoadingIndicator()

                Spacer(modifier = Modifier.height(24.dp))

                // Текст с анимацией пульсации
                AnimatedText(message)
            }
        }
    }
}

/**
 * Анимированный логотип с плавным вращением и масштабированием
 */
@Composable
private fun AnimatedLogo() {
    // Анимация вращения
    val rotation by rememberInfiniteTransition(label = "logo_rotation")
        .animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(12000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "rotation"
        )

    // Анимация масштабирования (пульсация)
    val scale by rememberInfiniteTransition(label = "logo_scale")
        .animateFloat(
            initialValue = 0.95f,
            targetValue = 1.05f,
            animationSpec = infiniteRepeatable(
                animation = tween(2000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "scale"
        )

    Box(
        modifier = Modifier
            .size(120.dp)
            .rotate(rotation)
            .scale(scale),
        contentAlignment = Alignment.Center
    ) {
        // Используем существующие иконки в зависимости от окружения
        if (Constants.BASE_URL_API.contains("gps-rs")) {
            AppIconProd()
        } else {
            AppIconTest()
        }
    }
}

/**
 * Анимированный индикатор загрузки с тремя точками
 */
@Composable
private fun AnimatedLoadingIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "dots")

    // Анимация для каждой точки с разной задержкой
    val delays = listOf(0, 200, 400)

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        delays.forEach { delay ->
            val scale by infiniteTransition.animateFloat(
                initialValue = 0.6f,
                targetValue = 1.2f,
                animationSpec = infiniteRepeatable(
                    animation = tween(600, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse,
                    initialStartOffset = StartOffset(delay)
                ),
                label = "dot_scale_$delay"
            )

            val alpha by infiniteTransition.animateFloat(
                initialValue = 0.4f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(600, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse,
                    initialStartOffset = StartOffset(delay)
                ),
                label = "dot_alpha_$delay"
            )

            Box(
                modifier = Modifier
                    .size(12.dp)
                    .scale(scale)
                    .alpha(alpha)
                    .background(
                        color = MaterialTheme.colorScheme.primary,
                        shape = CircleShape
                    )
            )
        }
    }
}

/**
 * Текст с плавной анимацией появления
 */
@Composable
private fun AnimatedText(text: String) {
    // Анимация пульсации прозрачности
    val alpha by rememberInfiniteTransition(label = "text_alpha")
        .animateFloat(
            initialValue = 0.6f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1500, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "text_alpha"
        )

    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.alpha(alpha)
    )
}

// ====================== PREVIEW ======================
@androidx.compose.ui.tooling.preview.Preview(showBackground = true, name = "Splash Loading Screen")
@Composable
private fun SplashLoadingScreenPreview() {
    MaterialTheme {
        SplashLoadingScreen()
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, name = "Splash Loading Screen - No Logo")
@Composable
private fun SplashLoadingScreenNoLogoPreview() {
    MaterialTheme {
        SplashLoadingScreen(
            message = "Загрузка данных...",
            showLogo = false
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, name = "Splash Loading Screen - No Logo")
@Composable
private fun SplashLoadingScreenNoTextPreview() {
    MaterialTheme {
        SplashLoadingScreen(
            message = "Загрузка данных...",
            showTextLoading = false
        )
    }
}