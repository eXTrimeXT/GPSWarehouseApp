package com.gps.warehouse.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionManager @Inject constructor() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // replay = 1 гарантирует, что если ViewModel подпишется чуть позже, она всё равно получит событие
    private val _sessionExpiredEvent = MutableSharedFlow<Unit>(replay = 1)
    val sessionExpiredEvent: SharedFlow<Unit> = _sessionExpiredEvent.asSharedFlow()

    fun notifySessionExpired() {
        scope.launch {
            _sessionExpiredEvent.emit(Unit)
        }
    }
}