package com.gps.warehouse.utils

import android.content.Context
import android.util.Log
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import com.honeywell.aidc.AidcManager
import com.honeywell.aidc.BarcodeFailureEvent
import com.honeywell.aidc.BarcodeReadEvent
import com.honeywell.aidc.BarcodeReader

class HoneywellAidcHelper(private val context: Context) {

    private var aidcManager: AidcManager? = null
    private var barcodeReader: BarcodeReader? = null

    private val _barcodeChannel = Channel<String>(Channel.CONFLATED)
    val barcodeFlow = _barcodeChannel.receiveAsFlow()

    companion object { private const val TAG = "HoneywellSDK" }

    fun init(onInitialized: () -> Unit, onError: (Exception) -> Unit) {
        Log.d(TAG, "Initializing...")

        try {
            AidcManager.create(context, object : AidcManager.CreatedCallback {
                override fun onCreated(manager: AidcManager?) {
                    aidcManager = manager

                    if (manager == null) {
                        onError(Exception("Manager is null"))
                        return
                    }

                    try {
                        barcodeReader = manager.createBarcodeReader()

                        if (barcodeReader == null) {
                            onError(Exception("Reader is null"))
                            return
                        }

                        barcodeReader?.addBarcodeListener(object : BarcodeReader.BarcodeListener {

                            // Вызывается при успешном сканировании
                            override fun onBarcodeEvent(event: BarcodeReadEvent?) {
                                if (event != null) {
                                    val data = event.barcodeData

                                    if (!data.isNullOrEmpty()) {
                                        Log.d(TAG, "Scanned: $data")
                                        _barcodeChannel.trySend(data)
                                    }
                                }
                            }

                            override fun onFailureEvent(event: BarcodeFailureEvent?) {
                                Log.e(TAG, "Scan failure: ${event?.toString()}")
                            }
                        })

                        Log.d(TAG, "Init OK")
                        onInitialized()

                    } catch (e: Exception) {
                        Log.e(TAG, "Error", e)
                        onError(e)
                    }
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Init crash", e)
            onError(e)
        }
    }

    fun enableScanner(enable: Boolean) {
        try {
            if (enable) {
                barcodeReader?.claim()
            } else {
                barcodeReader?.close()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Toggle error", e)
        }
    }

    fun release() {
        Log.d(TAG, "Releasing resources...")
        try {
            barcodeReader?.let { reader ->
                try {
                    reader.release()
                } catch (e: IllegalStateException) {
                    Log.w(TAG, "stopDecode failed (already closed?): ${e.message}")
                } catch (e: Exception) {
                    Log.e(TAG, "Error stopping decode", e)
                }

                try {
                    reader.close()
                } catch (e: IllegalStateException) {
                    Log.w(TAG, "close failed (already closed?): ${e.message}")
                } catch (e: Exception) {
                    Log.e(TAG, "Error closing reader", e)
                }
            }

            barcodeReader = null

            try {
                aidcManager?.close()
            } catch (e: Exception) {
                Log.e(TAG, "Error closing manager", e)
            }
            aidcManager = null

            _barcodeChannel.close()

        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error in release", e)
        }
    }
}