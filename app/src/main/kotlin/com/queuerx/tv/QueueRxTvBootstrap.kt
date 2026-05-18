package com.queuerx.tv

import com.queuerx.tv.core.player.MediaEngineCoordinator
import com.queuerx.tv.core.player.PlayerConfig
import com.queuerx.tv.core.realtime.RealtimeSseManager
import com.queuerx.tv.core.storage.QueueRxEnvironment

fun main() {
    val player = MediaEngineCoordinator(
        PlayerConfig(
            autoplay = true,
            mute = false,
            loop = true,
            preferredQuality = "auto",
            subtitlesEnabled = false,
            retryCount = 5,
            maxBufferMs = 30_000
        )
    )
    val realtime = RealtimeSseManager()

    println("QueueRx TV foundation bootstrapped for ${QueueRxEnvironment.PROD.baseUrl}")
    println("Player config: ${player.currentConfig()}")
    println("Initial SSE event id: ${realtime.lastEventId.value}")
}
