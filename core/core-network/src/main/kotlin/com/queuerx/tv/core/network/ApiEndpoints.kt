package com.queuerx.tv.core.network

/** All API URL path constants. Avoids magic strings scattered across the codebase. */
object ApiEndpoints {

    // ── Auth ─────────────────────────────────────────────────────────────────
    const val AUTH_LOGIN = "/auth/login"
    const val AUTH_REFRESH = "/auth/refresh"
    const val AUTH_LOGOUT = "/auth/logout"

    // ── Device provisioning ───────────────────────────────────────────────────
    const val DEVICE_ACTIVATE_QR = "/device/activate/qr"
    const val DEVICE_ACTIVATE_CODE = "/device/activate/code"
    const val DEVICE_ACTIVATE_MAC = "/device/activate/mac"
    const val DEVICE_REGISTER = "/device/register"
    const val DEVICE_DEREGISTER = "/device/deregister"
    const val DEVICE_STATUS = "/device/status"

    // ── Configuration ────────────────────────────────────────────────────────
    const val CONFIG_SYNC = "/config/sync"
    const val CONFIG_VERSION = "/config/version"

    // ── Queue ────────────────────────────────────────────────────────────────
    const val QUEUE_DOCTOR = "/queue/doctor"
    const val QUEUE_PHARMACY = "/queue/pharmacy"
    const val QUEUE_DEPARTMENT = "/queue/department/{departmentId}"

    // ── Media ────────────────────────────────────────────────────────────────
    const val MEDIA_PLAYLIST = "/media/playlist"
    const val MEDIA_PLAYLIST_BY_ID = "/media/playlist/{playlistId}"

    // ── Ticker ───────────────────────────────────────────────────────────────
    const val TICKER_MESSAGES = "/ticker/messages"

    // ── Realtime ─────────────────────────────────────────────────────────────
    const val REALTIME_SSE = "/realtime/events"
    const val REALTIME_WS = "/realtime/ws"

    // ── Telemetry ────────────────────────────────────────────────────────────
    const val TELEMETRY_HEARTBEAT = "/device/telemetry"

    // ── Diagnostics ──────────────────────────────────────────────────────────
    const val DIAGNOSTICS_LOGS_UPLOAD = "/diagnostics/logs"
    const val DIAGNOSTICS_SCREENSHOT_UPLOAD = "/diagnostics/screenshot"

    // ── OTA ──────────────────────────────────────────────────────────────────
    const val OTA_CHECK = "/ota/check"
    const val OTA_DOWNLOAD = "/ota/download/{version}"
}
