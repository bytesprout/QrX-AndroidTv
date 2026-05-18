package com.queuerx.tv.domain.device

import kotlinx.serialization.Serializable

/**
 * Sealed class representing all supported device activation methods.
 *
 * Part 6 requires support for ALL three methods:
 *  1. QR code scan  (`queuerx://activate?token=xxxx`)
 *  2. Manual activation code (`HSP-9823`)
 *  3. MAC address auto-bind (enterprise deployment)
 */
sealed class ActivationMethod {

    /**
     * QR code activation.
     *
     * Admin generates a QR code from the hospital admin panel.
     * The TV camera (or barcode overlay on Fire TV) scans the QR.
     * The embedded URL must match the pattern:
     *   `queuerx://activate?token=<ACTIVATION_TOKEN>`
     */
    data class QrActivation(val activationToken: String) : ActivationMethod() {
        init {
            require(activationToken.isNotBlank()) { "activationToken must not be blank" }
        }
    }

    /**
     * Manual activation code entry.
     *
     * Format: `[A-Z]{3}-[0-9]{4}` (e.g. `HSP-9823`).
     * Displayed in the admin portal and entered via the TV remote D-pad.
     */
    data class CodeActivation(val activationCode: String) : ActivationMethod() {
        init {
            require(ACTIVATION_CODE_REGEX.matches(activationCode)) {
                "activationCode must match pattern XXX-0000, got '$activationCode'"
            }
        }

        companion object {
            private val ACTIVATION_CODE_REGEX = Regex("^[A-Z]{3}-[0-9]{4}$")
        }
    }

    /**
     * MAC-address auto-bind for enterprise deployments.
     *
     * The TV reads its own MAC address on boot and sends it to the backend.
     * The backend matches it against pre-registered devices.
     */
    data class MacActivation(val macAddress: String) : ActivationMethod() {
        init {
            require(MAC_REGEX.matches(macAddress)) {
                "macAddress must be a valid MAC address, got '$macAddress'"
            }
        }

        companion object {
            // Accepts formats: AA:BB:CC:DD:EE:FF  or  AA-BB-CC-DD-EE-FF
            private val MAC_REGEX = Regex(
                "^([0-9A-Fa-f]{2}[:\\-]){5}([0-9A-Fa-f]{2})$"
            )
        }
    }
}

/**
 * Request body sent to the activation endpoints.
 */
@Serializable
data class QrActivationRequest(val token: String)

@Serializable
data class CodeActivationRequest(val code: String, val deviceId: String)

@Serializable
data class MacActivationRequest(val mac: String, val deviceId: String)

/**
 * Unified activation response.
 *
 * On success the backend returns an [activationToken] that the device uses
 * to complete [DeviceRegistrationRequest].
 */
@Serializable
data class ActivationResponse(
    val activationToken: String,
    val hospitalId: String,
    val tenantId: String,
    val branchId: String,
    val deviceType: String,
    val displayName: String
)
