package com.queuerx.tv.core.player

data class PlayerConfig(
    val autoplay: Boolean,
    val mute: Boolean,
    val loop: Boolean,
    val preferredQuality: String,
    val subtitlesEnabled: Boolean,
    val retryCount: Int,
    val maxBufferMs: Int
)
