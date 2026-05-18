package com.queuerx.tv.domain.device

import com.queuerx.tv.core.common.ApiResult

/**
 * Repository interface for device provisioning operations.
 *
 * Implemented in [data-device]. The domain layer only depends on this interface.
 */
interface DeviceRepository {
    /** Returns the current [ProvisionState] from persistent storage. */
    suspend fun getProvisionState(): ProvisionState

    /** Persists a [ProvisionState] transition. */
    suspend fun saveProvisionState(state: ProvisionState): ApiResult<Unit>

    /**
     * Activates the device using the given [method].
     * Returns an [ActivationResponse] on success.
     */
    suspend fun activate(method: ActivationMethod): ApiResult<ActivationResponse>

    /**
     * Registers the device with the backend using the activation token
     * obtained from [activate].
     */
    suspend fun register(
        device: DeviceInfo,
        activationResponse: ActivationResponse
    ): ApiResult<DeviceRegistrationResponse>

    /** Saves the registered device context to encrypted storage. */
    suspend fun saveDeviceContext(response: DeviceRegistrationResponse): ApiResult<Unit>

    /** Returns the persisted registration context, or null if not yet provisioned. */
    suspend fun getDeviceContext(): DeviceRegistrationResponse?

    /** Returns the device's MAC address. */
    suspend fun getMacAddress(): String

    /** Returns the device's stable unique identifier. */
    suspend fun getDeviceId(): String
}
