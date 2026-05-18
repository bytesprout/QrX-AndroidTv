package com.queuerx.tv.core.storage

enum class QueueRxEnvironment(val baseUrl: String) {
    DEV("https://dev.api.QueueRx.mightyscape.com"),
    STAGE("https://stage.api.QueueRx.mightyscape.com"),
    PROD("https://api.QueueRx.mightyscape.com")
}

data class DeviceContext(
    val deviceId: String,
    val hospitalId: String,
    val displayId: String,
    val tenantId: String
)

object PreferenceKeys {
    const val AUTH_TOKEN = "auth_token"
    const val REFRESH_TOKEN = "refresh_token"
    const val DEVICE_ID = "device_id"
    const val TENANT_ID = "tenant_id"
    const val HOSPITAL_ID = "hospital_id"
    const val DISPLAY_ID = "display_id"
    const val LAST_EVENT_ID = "last_event_id"
    const val PROVISION_STATE = "provision_state"
    const val SETTINGS = "settings"
    const val THEME = "theme"
}
