package com.example.speedlab.engine

import kotlinx.coroutines.flow.Flow

interface SpeedTestEngine {
    fun run(request: SpeedTestRequest): Flow<SpeedTestEvent>
    fun cancel()
}
