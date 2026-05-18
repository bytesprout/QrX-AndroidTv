package com.queuerx.tv.domain.device

import kotlinx.serialization.Serializable

/**
 * Canonical device-type taxonomy.
 *
 * The backend uses these values to apply the correct default layout and
 * queue bindings for a newly registered TV.
 */
@Serializable
enum class DeviceType {
    WAITING_HALL,
    RECEPTION,
    PHARMACY,
    DOCTOR_ROOM,
    EMERGENCY,
    CUSTOM
}

/**
 * Immutable snapshot of the registered device identity sent to the backend.
 *
 * The [deviceId] is a stable UUID generated once on first boot and persisted
 * in encrypted storage. It survives app updates but is regenerated on a
 * factory reset / clear-data event.
 */
@Serializable
data class DeviceInfo(
    val deviceId: String,
    val deviceName: String,
    val mac: String,
    val model: String,
    val osVersion: String,
    val appVersion: String,
    val deviceType: DeviceType = DeviceType.WAITING_HALL
)

/**
 * Full registration request as expected by [ApiEndpoints.DEVICE_REGISTER].
 */
@Serializable
data class DeviceRegistrationRequest(
    val deviceId: String,
    val deviceName: String,
    val mac: String,
    val model: String,
    val osVersion: String,
    val appVersion: String,
    val deviceType: String,
    val hospitalId: String? = null,
    val activationToken: String? = null
)

/**
 * Registration response returned by the backend.
 */
@Serializable
data class DeviceRegistrationResponse(
    val deviceId: String,
    val displayId: String,
    val hospitalId: String,
    val tenantId: String,
    val branchId: String,
    val deviceType: String,
    val configVersion: String
)
