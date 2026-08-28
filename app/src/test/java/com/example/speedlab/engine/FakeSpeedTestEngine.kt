package com.example.speedlab.engine

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class FakeSpeedTestEngine(
    private val events: List<SpeedTestEvent>,
) : SpeedTestEngine {
    var cancelled: Boolean = false
        private set

    override fun run(request: SpeedTestRequest): Flow<SpeedTestEvent> = flow {
        events.forEach { emit(it) }
    }

    override fun cancel() {
        cancelled = true
    }
}
