package com.gps.warehouse.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow

class ZebraHelper(private val context: Context) {

    private val _barcodeChannel = Channel<String>(Channel.CONFLATED)
    val barcodeFlow = _barcodeChannel.receiveAsFlow()

    private var zebraReceiver: BroadcastReceiver? = null

    companion object {
        private const val TAG = "ZebraHelper"

        // Стандартный Action для Zebra DataWedge
        const val ZEBRA_ACTION_RESULT = "com.symbol.datawedge.api.RESULT_ACTION"
        // Кастомный Action (на случай, если в DataWedge настроен он)
        const val CUSTOM_ACTION = "com.gps.warehouse.SCAN_RESULT"

        // Ключи для извлечения данных из Intent
        const val ZEBRA_EXTRA_DATA = "com.symbol.datawedge.data_string"
    }

    fun init() {
        Log.d(TAG, "Initializing ZebraHelper...")
        initZebraReceiver()
    }

    private fun initZebraReceiver() {
        val filter = IntentFilter().apply {
            addAction(ZEBRA_ACTION_RESULT)
            addAction(CUSTOM_ACTION)
            addCategory(Intent.CATEGORY_DEFAULT)
        }

        zebraReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val action = intent?.action

                if (action == ZEBRA_ACTION_RESULT || action == CUSTOM_ACTION) {
                    Log.d(TAG, "=== Zebra Intent received ===")
                    Log.d(TAG, "Action: $action")

                    // Пробуем получить данные из разных возможных источников (для надежности)
                    val data = when {
                        intent.hasExtra(ZEBRA_EXTRA_DATA) -> {
                            intent.getStringExtra(ZEBRA_EXTRA_DATA)
                        }
                        intent.hasExtra("data_string") -> {
                            intent.getStringExtra("data_string")
                        }
                        intent.hasExtra("com.symbol.datawedge.DATA_STRING") -> {
                            intent.getStringExtra("com.symbol.datawedge.DATA_STRING")
                        }
                        else -> intent?.dataString
                    }

                    if (!data.isNullOrEmpty()) {
                        Log.d(TAG, "Successfully extracted barcode: $data")
                        _barcodeChannel.trySend(data)
                    } else {
                        Log.w(TAG, "No barcode data found in intent extras")
                        // Для отладки: логируем все extras, если данные не найдены
                        intent?.extras?.keySet()?.forEach { key ->
                            Log.d(TAG, "  Available extra: $key = ${intent.extras?.get(key)}")
                        }
                    }
                }
            }
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(zebraReceiver, filter, Context.RECEIVER_EXPORTED)
            } else {
                // RECEIVER_EXPORTED доступен в androidx.core:core-ktx начиная с версии 1.9.0
                ContextCompat.registerReceiver(
                    context,
                    zebraReceiver,
                    filter,
                    ContextCompat.RECEIVER_EXPORTED
                )
            }
            Log.d(TAG, "Zebra BroadcastReceiver registered successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register Zebra receiver", e)
        }
    }

    fun release() {
        Log.d(TAG, "Releasing ZebraHelper...")
        zebraReceiver?.let {
            try {
                context.unregisterReceiver(it)
            } catch (e: Exception) {
                Log.w(TAG, "Receiver already unregistered or invalid context")
            }
        }
        zebraReceiver = null
        _barcodeChannel.close()
    }
}