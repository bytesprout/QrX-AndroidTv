package com.queuerx.tv.domain.device

import com.queuerx.tv.core.common.ApiResult
import com.queuerx.tv.core.common.QueueRxError

/**
 * Validates that an activation token embedded in a QR URI is well-formed.
 *
 * Accepted format: `queuerx://activate?token=<TOKEN>`
 * The token itself must be non-blank and ≥ 8 characters.
 */
class ValidateQrActivationTokenUseCase {
    operator fun invoke(uri: String): ApiResult<ActivationMethod.QrActivation> {
        if (!uri.startsWith("queuerx://activate")) {
            return ApiResult.Error(
                QueueRxError.InvalidActivationToken(uri)
            )
        }
        val token = extractTokenFromUri(uri)
        if (token.isNullOrBlank() || token.length < 8) {
            return ApiResult.Error(
                QueueRxError.InvalidActivationToken(uri)
            )
        }
        return ApiResult.Success(ActivationMethod.QrActivation(token))
    }

    private fun extractTokenFromUri(uri: String): String? {
        val queryStart = uri.indexOf('?')
        if (queryStart == -1) return null
        val query = uri.substring(queryStart + 1)
        return query.split('&')
            .mapNotNull { part ->
                val (key, value) = part.split('=').takeIf { it.size == 2 } ?: return@mapNotNull null
                if (key == "token") value else null
            }
            .firstOrNull()
    }
}

/**
 * Validates a manual activation code entered via remote.
 *
 * Format: `[A-Z]{3}-[0-9]{4}` (e.g. `HSP-9823`).
 */
class ValidateActivationCodeUseCase {
    private val regex = Regex("^[A-Z]{3}-[0-9]{4}$")

    operator fun invoke(code: String): ApiResult<ActivationMethod.CodeActivation> {
        val trimmed = code.trim().uppercase()
        return if (regex.matches(trimmed)) {
            ApiResult.Success(ActivationMethod.CodeActivation(trimmed))
        } else {
            ApiResult.Error(QueueRxError.InvalidActivationCode(code))
        }
    }
}

/**
 * Executes the full activation + registration flow for a given [ActivationMethod].
 *
 * Flow:
 *  1. Transition state to ACTIVATING
 *  2. Call backend to validate activation → get [ActivationResponse]
 *  3. Transition state to REGISTERING
 *  4. Register device with activation token → get [DeviceRegistrationResponse]
 *  5. Persist device context
 *  6. Transition state to SYNCING_CONFIG
 *
 * Callers should follow with [com.queuerx.tv.domain.device.SyncConfigUseCase].
 */
class ActivateDeviceUseCase(private val repository: DeviceRepository) {
    suspend operator fun invoke(
        method: ActivationMethod,
        deviceInfo: DeviceInfo
    ): ApiResult<DeviceRegistrationResponse> {
        repository.saveProvisionState(ProvisionState.ACTIVATING)

        val activationResult = repository.activate(method)
        if (activationResult is ApiResult.Error) return activationResult

        val activationResponse = (activationResult as ApiResult.Success).data
        repository.saveProvisionState(ProvisionState.REGISTERING)

        val registrationResult = repository.register(deviceInfo, activationResponse)
        if (registrationResult is ApiResult.Error) return registrationResult

        val registrationResponse = (registrationResult as ApiResult.Success).data
        repository.saveDeviceContext(registrationResponse)
        repository.saveProvisionState(ProvisionState.SYNCING_CONFIG)

        return ApiResult.Success(registrationResponse)
    }
}

/**
 * Checks the persisted [ProvisionState] and returns whether the device
 * is already fully provisioned.
 */
class GetProvisionStateUseCase(private val repository: DeviceRepository) {
    suspend operator fun invoke(): ProvisionState = repository.getProvisionState()
}

/**
 * Returns true if the device is not yet provisioned and should show
 * the activation/provisioning screen.
 */
class NeedsProvisioningUseCase(private val repository: DeviceRepository) {
    suspend operator fun invoke(): Boolean =
        repository.getProvisionState() != ProvisionState.PROVISIONED
}
