package com.gps.warehouse.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat

object DataWedgeProfileManager {

    private const val TAG = "DataWedgeProfile"

    private const val PROFILE_NAME = "GPS_Warehouse"
    private const val SCAN_INTENT_ACTION = "com.gps.warehouse.SCAN_RESULT"
    private const val SCAN_INTENT_CATEGORY = "android.intent.category.DEFAULT"

    private const val ACTION_DATAWEDGE = "com.symbol.datawedge.api.ACTION"
    private const val ACTION_DATAWEDGE_RESULTS = "com.symbol.datawedge.api.RESULT_ACTION"
    private const val EXTRA_RESULT_INFO = "com.symbol.datawedge.api.RESULT_INFO"

    private const val DATAWEDGE_PACKAGE = "com.symbol.datawedge"

    private const val CMD_GET_PROFILE_LIST = "com.symbol.datawedge.api.GET_PROFILE_LIST"
    private const val CMD_SET_CONFIG = "com.symbol.datawedge.api.SET_CONFIG"

    private var resultReceiver: BroadcastReceiver? = null
    private var isRegistered = false
    private var appContext: Context? = null
    private val handler = Handler(Looper.getMainLooper())
    private var profileCreated = false

    private val fallbackRunnable = Runnable {
        if (!profileCreated) {
            Log.w(TAG, "⚠ No response from DataWedge in 5 seconds — forcing profile creation")
            appContext?.let { createProfile(it, false) }
        }
    }

    fun ensureProfileExists(context: Context) {
        appContext = context.applicationContext
        Log.d(TAG, "=== Starting DataWedge profile setup ===")
        Log.d(TAG, "App package: ${context.packageName}")
        Log.d(TAG, "Target profile: $PROFILE_NAME")

//        launchDataWedgeIfPossible(context)
        registerResultReceiver(context)

        handler.postDelayed({
            requestProfileList(context)
            handler.postDelayed(fallbackRunnable, 5000)
        }, 1500)
    }

    private fun launchDataWedgeIfPossible(context: Context) {
        try {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(DATAWEDGE_PACKAGE)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                context.startActivity(launchIntent)
                Log.d(TAG, "✓ DataWedge launch intent sent")
            } else {
                Log.w(TAG, "⚠ DataWedge has no launch intent")
            }
        } catch (e: Exception) {
            Log.w(TAG, "⚠ Failed to launch DataWedge: ${e.message}")
        }
    }

    private fun registerResultReceiver(context: Context) {
        if (isRegistered) return

        val filter = IntentFilter().apply {
            addAction(ACTION_DATAWEDGE_RESULTS)
            addCategory(Intent.CATEGORY_DEFAULT)
        }

        resultReceiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                val command = intent?.getStringExtra("COMMAND")
                val result = intent?.getStringExtra("RESULT")
                val resultInfo = intent?.getBundleExtra(EXTRA_RESULT_INFO)

                Log.d(TAG, "=== DW RESULT RECEIVED ===")
                Log.d(TAG, "Command: $command")
                Log.d(TAG, "Result: $result")
                resultInfo?.let {
                    for (key in it.keySet()) {
                        Log.d(TAG, "  ResultInfo [$key]: ${it.get(key)}")
                    }
                }

                handler.removeCallbacks(fallbackRunnable)

                when (command) {
                    CMD_GET_PROFILE_LIST -> handleProfileListResult(ctx ?: return, resultInfo)
                    CMD_SET_CONFIG -> {
                        profileCreated = true
                        handleSetConfigResult(result, resultInfo)
                    }
                }
            }
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(resultReceiver, filter, Context.RECEIVER_EXPORTED)
            } else {
                ContextCompat.registerReceiver(
                    context, resultReceiver, filter, ContextCompat.RECEIVER_EXPORTED
                )
            }
            isRegistered = true
            Log.d(TAG, "✓ Result receiver registered")
        } catch (e: Exception) {
            Log.e(TAG, "✗ Failed to register receiver", e)
        }
    }

    private fun requestProfileList(context: Context) {
        val intent = Intent().apply {
            action = ACTION_DATAWEDGE
            setPackage(DATAWEDGE_PACKAGE)
            putExtra(CMD_GET_PROFILE_LIST, "")
        }
        try {
            context.sendBroadcast(intent)
            Log.d(TAG, "✓ GET_PROFILE_LIST sent to $DATAWEDGE_PACKAGE")
        } catch (e: Exception) {
            Log.e(TAG, "✗ Failed to send GET_PROFILE_LIST", e)
        }
    }

    private fun handleProfileListResult(context: Context, resultInfo: Bundle?) {
        val profileList = resultInfo?.getStringArray("PROFILE_LIST") ?: emptyArray()
        Log.d(TAG, "Existing profiles: ${profileList.joinToString()}")

        if (profileList.contains(PROFILE_NAME)) {
            Log.d(TAG, "Profile '$PROFILE_NAME' already exists — updating...")
            createProfile(context, updateMode = true)
        } else {
            Log.d(TAG, "Profile '$PROFILE_NAME' not found — creating...")
            createProfile(context, updateMode = false)
        }
    }

    private fun createProfile(context: Context, updateMode: Boolean) {
        val packageName = context.packageName
        val configMode = if (updateMode) "UPDATE" else "CREATE_IF_NOT_EXIST"

        Log.d(TAG, "Creating/updating profile with CONFIG_MODE: $configMode")

        // 1. Создаем Bundle для связанного приложения (Associated Apps)
        val appBundle = Bundle().apply {
            putString("PACKAGE_NAME", packageName)
            // "*" означает, что профиль будет активен для всех Activity в этом пакете
            putStringArray("ACTIVITY_LIST", arrayOf("*"))
        }

        val profileConfig = Bundle().apply {
            putString("PROFILE_NAME", PROFILE_NAME)
            putString("PROFILE_ENABLED", "true")
            putString("CONFIG_MODE", configMode)

            // ВАЖНО: APP_LIST должен быть указан ПЕРЕД PLUGIN_CONFIG
            // И использовать putBundle, а не putParcelableArray
//            putBundle("APP_LIST", Bundle().apply {
//                putString("PACKAGE_NAME", packageName)
//                putStringArray("ACTIVITY_LIST", arrayOf("*"))
//            })
            putParcelableArray("APP_LIST", arrayOf(appBundle))

            val pluginConfigList = arrayListOf(
                createBarcodeInputPlugin(),
                createIntentOutputPlugin()
            )
            putParcelableArrayList("PLUGIN_CONFIG", pluginConfigList)
        }

        val intent = Intent().apply {
            action = ACTION_DATAWEDGE
            setPackage(DATAWEDGE_PACKAGE)
            putExtra(CMD_SET_CONFIG, profileConfig)
        }

        try {
            context.sendBroadcast(intent)
            Log.d(TAG, "✓ SET_CONFIG sent for package: $packageName with mode: $configMode")
        } catch (e: Exception) {
            Log.e(TAG, "✗ Failed to send SET_CONFIG", e)
        }
    }

    private fun createBarcodeInputPlugin(): Bundle {
        return Bundle().apply {
            putString("PLUGIN_NAME", "BARCODE")
            putString("RESET_CONFIG", "true")

            val paramList = Bundle().apply {
                putString("scanner_input_enabled", "true")
                putString("scanner_selection", "auto")

                putBundle("decoder_params", Bundle().apply {
                    putString("decoder_ean13", "true")
                    putString("decoder_ean8", "true")
                    putString("decoder_code128", "true")
                    putString("decoder_code39", "true")
                    putString("decoder_code93", "true")
                    putString("decoder_qrcode", "true")
                    putString("decoder_datamatrix", "true")
                    putString("decoder_upca", "true")
                    putString("decoder_upce0", "true")
                })
            }
            putBundle("PARAM_LIST", paramList)
        }
    }

    private fun createIntentOutputPlugin(): Bundle {
        return Bundle().apply {
            putString("PLUGIN_NAME", "INTENT")
            putString("RESET_CONFIG", "true")

            val paramList = Bundle().apply {
                putString("intent_output_enabled", "true")
                putString("intent_action", SCAN_INTENT_ACTION)
                putString("intent_category", SCAN_INTENT_CATEGORY)
                putString("intent_delivery", "2") // 2 = Broadcast intent
                putString("intent_use_content_provider", "false")
            }
            putBundle("PARAM_LIST", paramList)
        }
    }

    private fun handleSetConfigResult(result: String?, resultInfo: Bundle?) {
        if (result == "SUCCESS") {
            Log.d(TAG, "✓✓✓ Profile '$PROFILE_NAME' created/updated SUCCESSFULLY ✓✓✓")
            profileCreated = true
        } else {
            Log.e(TAG, "✗ Failed to configure profile: $result")
            resultInfo?.let {
                for (key in it.keySet()) {
                    Log.e(TAG, "  Info [$key]: ${it.get(key)}")
                }
            }
        }
    }

    fun unregisterReceiver(context: Context) {
        handler.removeCallbacks(fallbackRunnable)
        if (!isRegistered) return
        resultReceiver?.let {
            try {
                context.unregisterReceiver(it)
            } catch (e: Exception) {
                Log.w(TAG, "Receiver already unregistered")
            }
        }
        resultReceiver = null
        isRegistered = false
        appContext = null
    }
}