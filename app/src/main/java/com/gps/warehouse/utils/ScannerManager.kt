package com.gps.warehouse.utils

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class ScannerManager(private val context: Context) {

    private val _barcodeChannel = Channel<String>(Channel.CONFLATED)
    val barcodeFlow = _barcodeChannel.receiveAsFlow()

    private var honeywellHelper: HoneywellHelper? = null
    private var zebraHelper: ZebraHelper? = null

    companion object {
        private const val TAG = "ScannerManager"
    }

    fun init() {
        Log.d(TAG, "Initializing ScannerManager (Universal)...")

        // 1. Инициализация Honeywell SDK (сработает только на устройствах Honeywell)
        initHoneywell()

        // 2. Инициализация Zebra DataWedge Receiver (будет слушать Intent-ы на любом устройстве)
        initZebra()
    }

    private fun initHoneywell() {
        honeywellHelper = HoneywellHelper(context)
        honeywellHelper?.init(
            onInitialized = {
                Log.d(TAG, "Honeywell SDK initialized successfully")
                honeywellHelper?.enableScanner(true)

                // Пробрасываем данные из Honeywell в общий канал
                CoroutineScope(Dispatchers.Default).launch {
                    honeywellHelper?.barcodeFlow?.collect { data ->
                        Log.d(TAG, "Honeywell scan received: $data")
                        _barcodeChannel.trySend(data)
                    }
                }
            },
            onError = { e ->
                Log.w(TAG, "Honeywell SDK init failed (expected on non-Honeywell devices): ${e.message}")
            }
        )
    }

    private fun initZebra() {
        zebraHelper = ZebraHelper(context)
        zebraHelper?.init()

        // Пробрасываем данные из Zebra в общий канал
        CoroutineScope(Dispatchers.Default).launch {
            zebraHelper?.barcodeFlow?.collect { data ->
                Log.d(TAG, "Zebra scan received: $data")
                _barcodeChannel.trySend(data)
            }
        }
    }

    fun release() {
        Log.d(TAG, "Releasing ScannerManager...")

        honeywellHelper?.enableScanner(false)
        honeywellHelper?.release()
        honeywellHelper = null

        zebraHelper?.release()
        zebraHelper = null

        _barcodeChannel.close()
    }
}