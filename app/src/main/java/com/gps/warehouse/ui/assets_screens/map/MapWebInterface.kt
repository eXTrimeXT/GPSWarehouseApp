package com.gps.warehouse.ui.assets_screens.map

import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface

class MapWebInterface(
    private val onAssetClickAction: (String) -> Unit
) {
    // Создаем Handler, привязанный к главному потоку (Main Thread)
    private val mainHandler = Handler(Looper.getMainLooper())

    @JavascriptInterface
    fun onAssetClicked(assetId: String) {
        // Передаем выполнение лямбды в главный поток
        mainHandler.post {
            onAssetClickAction(assetId)
        }
    }
}