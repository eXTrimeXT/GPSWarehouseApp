package com.gps.warehouse.ui.assets_screens.map

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.gps.warehouse.ui.AssetViewModel
import com.gps.warehouse.utils.Constants

@SuppressLint("SetJavaScriptEnabled", "JavascriptInterface")
@Composable
fun AssetMapWebViewScreen(
    navController: NavController,
    viewModel: AssetViewModel = hiltViewModel()
) {
    // URL бэкенда (map-fetch / map-crud)
    val mapUrl = "${Constants.ASSET_URL}map-crud"

    var isLoading by remember { mutableStateOf(true) }
    var webView: WebView? by remember { mutableStateOf(null) }

    // Создаем интерфейс для связи с JS
    val webInterface = remember {
        MapWebInterface { assetId ->
            // При получении клика из JS, переходим на экран деталей
            navController.navigate("asset_details/$assetId")
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }

        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                WebView(context).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true

                    // Критически важно для корректного масштабирования и плавности
                    settings.useWideViewPort = true
                    settings.loadWithOverviewMode = true

                    settings.setSupportZoom(true)
                    settings.builtInZoomControls = true
                    settings.displayZoomControls = false

                    // Запрещаем WebView пытаться сохранять фокус, что может вызывать лаги при тачах
                    settings.setNeedInitialFocus(false)

                    // Явное указание использовать аппаратный слой для этого View
                    setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)

                    // Добавляем наш интерфейс с именем "Android" (будет вызываться как Android.onAssetClicked)
                    addJavascriptInterface(webInterface, "Android")

                    webViewClient = object : WebViewClient() {
                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                            super.onPageStarted(view, url, favicon)
                            isLoading = true
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            isLoading = false

                            // === МАГИЯ: Внедряем JS для перехвата кликов ===
                            // Мы ищем все элементы с data-asset-id и вешаем на них обработчик,
                            // который вызывает наш Kotlin-метод
                            val injectJs = """
                                javascript:(function() {
                                    // Скрываем элементы
                                    var header = document.querySelector('h1');
                                    var legend = document.querySelector('.legend');
                                    var mapInfo = document.querySelector('.map-info');
                                    var statsInfo = document.querySelector('.stats-info');
                                    var helpHint = document.querySelector('.help-hint');

                                    if (header) header.style.display = 'none';
                                    if (legend) legend.style.display = 'none';
                                    if (mapInfo) mapInfo.style.display = 'none';
                                    if (statsInfo) statsInfo.style.display = 'none';
                                    if (helpHint) helpHint.style.display = 'none';

                                    // Растягиваем map-wrapper на весь экран
                                    var mapWrapper = document.querySelector('.map-wrapper');
                                    if (mapWrapper) {
                                        mapWrapper.style.padding = '0';
                                        mapWrapper.style.margin = '0';
                                        mapWrapper.style.borderRadius = '0';
                                        mapWrapper.style.boxShadow = 'none';
                                        mapWrapper.style.position = 'fixed';
                                        mapWrapper.style.top = '0';
                                        mapWrapper.style.left = '0';
                                        mapWrapper.style.width = '100%';
                                        mapWrapper.style.height = '100%';
                                        mapWrapper.style.zIndex = '9999';
                                        mapWrapper.style.overflow = 'hidden';
                                        
                                        // GPU-ускорение и запрет стандартного скролла
                                        mapWrapper.style.touchAction = 'none'; 
                                        mapWrapper.style.willChange = 'transform';
                                        mapWrapper.style.transform = 'translateZ(0)';
                                    }

                                    // Убираем отступы у body
                                    document.body.style.margin = '0';
                                    document.body.style.padding = '0';
                                    document.body.style.overflow = 'hidden';

                                    // Настраиваем SVG на полный экран
                                    var svg = document.querySelector('.map-svg');
                                    if (svg) {
                                        svg.style.width = '100%';
                                        svg.style.height = '100%';
                                        // Важно для плавности SVG
                                        svg.style.touchAction = 'none'; // Отдает управление касаниями вашему JS-коду, а не браузеру
                                        svg.style.willChange = 'transform'; // Подсказка браузеру использовать GPU
                                        svg.style.transform = 'translateZ(0)'; // Принудительное создание отдельного слоя композитинга
                                    }

                                    // Клик по точке актива
                                    var elements = document.querySelectorAll('[data-asset-id]');
                                    elements.forEach(function(el) {
                                        el.addEventListener('click', function(e) {
                                            e.preventDefault();
                                            e.stopPropagation();
                                            var id = this.getAttribute('data-asset-id');
                                            // Вызываем метод Kotlin-интерфейса
                                            Android.onAssetClicked(id);
                                        });
                                        // Делаем курсор "рукой" для удобства на ПК/отладке
                                        el.style.cursor = 'pointer';
                                    });
                                    
                                    // Изменяем первоначальный масштаб
                                    state.scale = 1.4;
                                    state.panX = -20;
                                    updateTransform();
                                    document.getElementById('zoomReset').addEventListener('click', () => {{
                                        state.scale = 1.4;
                                        state.panX = -20;
                                        state.panY = 0;
                                        updateTransform();
                                    }});
                                    
                                    // Разрешаем прокрутку страницы
                                    document.documentElement.style.overflow = 'auto';
                                    document.documentElement.style.height = 'auto';
                                    document.body.style.overflow = 'auto';
                                    document.body.style.height = 'auto';
                                    document.body.style.touchAction = 'auto';
                                })();
                            """.trimIndent()

                            view?.evaluateJavascript(injectJs, null)
                        }
                    }

                    // Загружаем страницу
                    loadUrl(mapUrl)
                }
            },
            update = { view ->
                webView = view
            }
        )
    }

    // Очистка при уничтожении Composable
    DisposableEffect(Unit) {
        onDispose {
            webView?.stopLoading()
            webView?.clearHistory()
            webView?.removeJavascriptInterface("Android")
            webView?.destroy()
        }
    }
}
