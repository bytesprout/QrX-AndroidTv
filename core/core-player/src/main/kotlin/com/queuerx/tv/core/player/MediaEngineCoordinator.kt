package com.queuerx.tv.core.player

class MediaEngineCoordinator(
    private var activeConfig: PlayerConfig
) {
    fun updateConfig(config: PlayerConfig) {
        activeConfig = config
    }

    fun currentConfig(): PlayerConfig = activeConfig
}
