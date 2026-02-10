package com.drivetheory.cbt.engine

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class TimerController(
    private val scope: CoroutineScope,
    private val totalSeconds: Int
) {
    private val _remaining = MutableStateFlow(totalSeconds)
    val remaining: StateFlow<Int> = _remaining
    private var job: Job? = null

    fun start(onFinish: () -> Unit = {}) {
        if (job?.isActive == true) return
        job = scope.launch {
            while (_remaining.value > 0 && isActive) {
                delay(1000L)
                _remaining.value = _remaining.value - 1
            }
            if (_remaining.value <= 0) onFinish()
        }
    }

    fun cancel() {
        job?.cancel()
    }
}

