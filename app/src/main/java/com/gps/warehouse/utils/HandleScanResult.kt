package com.gps.warehouse.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavController

@Composable
fun HandleScanResult(
    navController: NavController,
    onScanResult: (String) -> Unit
) {
    val backStackEntry = navController.currentBackStackEntry

    DisposableEffect(backStackEntry) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                backStackEntry?.savedStateHandle
                    ?.get<String>("SCAN_RESULT_KEY")
                    ?.let { code ->
                        backStackEntry.savedStateHandle.remove<String>("SCAN_RESULT_KEY")
                        onScanResult(code)
                    }
            }
        }
        backStackEntry?.lifecycle?.addObserver(observer)
        onDispose { backStackEntry?.lifecycle?.removeObserver(observer) }
    }
}