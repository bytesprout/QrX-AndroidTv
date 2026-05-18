package com.queuerx.tv.core.common

/**
 * Typed error hierarchy for QueueRx TV.
 *
 * All errors in the system are represented as sealed subclasses so callers
 * can exhaustively handle every failure case without catching raw exceptions.
 */
sealed class QueueRxError {

    // ── Network ──────────────────────────────────────────────────────────────

    data class NetworkError(
        val message: String,
        val cause: Throwable? = null
    ) : QueueRxError()

    data class HttpError(
        val statusCode: Int,
        val message: String
    ) : QueueRxError()

    data object Unauthorized : QueueRxError()
    data object Forbidden : QueueRxError()
    data object NotFound : QueueRxError()
    data object ServiceUnavailable : QueueRxError()
    data object Timeout : QueueRxError()

    // ── Auth ─────────────────────────────────────────────────────────────────

    data object TokenExpired : QueueRxError()
    data object TokenInvalid : QueueRxError()
    data object RefreshFailed : QueueRxError()

    // ── Provisioning ─────────────────────────────────────────────────────────

    data class ActivationFailed(val reason: String) : QueueRxError()
    data class RegistrationFailed(val reason: String) : QueueRxError()
    data object DeviceAlreadyRegistered : QueueRxError()
    data class InvalidActivationToken(val token: String) : QueueRxError()
    data class InvalidActivationCode(val code: String) : QueueRxError()
    data object MacAddressNotRecognized : QueueRxError()

    // ── Configuration ────────────────────────────────────────────────────────

    data class ConfigChecksumMismatch(
        val expected: String,
        val actual: String
    ) : QueueRxError()

    data class ConfigDownloadFailed(val message: String) : QueueRxError()
    data object ConfigRolledBack : QueueRxError()

    // ── Queue ────────────────────────────────────────────────────────────────

    data object QueueStale : QueueRxError()
    data class InvalidStateTransition(
        val from: String,
        val to: String
    ) : QueueRxError()

    // ── Storage ──────────────────────────────────────────────────────────────

    data class StorageReadFailed(val key: String) : QueueRxError()
    data class StorageWriteFailed(val key: String, val cause: Throwable? = null) : QueueRxError()

    // ── Device ───────────────────────────────────────────────────────────────

    data class DeviceCommandFailed(
        val command: String,
        val reason: String
    ) : QueueRxError()

    // ── Generic ──────────────────────────────────────────────────────────────

    data class UnexpectedError(
        val message: String,
        val cause: Throwable? = null
    ) : QueueRxError()

    fun toException(): QueueRxException = QueueRxException(this)

    fun userMessage(): String = when (this) {
        is NetworkError -> "Network error: $message"
        is HttpError -> "Server error ($statusCode): $message"
        Unauthorized -> "Authentication required."
        Forbidden -> "Access denied."
        NotFound -> "Resource not found."
        ServiceUnavailable -> "Service temporarily unavailable."
        Timeout -> "Request timed out. Please check your connection."
        TokenExpired -> "Session expired. Please re-authenticate."
        TokenInvalid -> "Invalid authentication token."
        RefreshFailed -> "Failed to refresh authentication. Please re-activate."
        is ActivationFailed -> "Activation failed: $reason"
        is RegistrationFailed -> "Device registration failed: $reason"
        DeviceAlreadyRegistered -> "This device is already registered."
        is InvalidActivationToken -> "Invalid activation token."
        is InvalidActivationCode -> "Invalid activation code: $code"
        MacAddressNotRecognized -> "MAC address not recognized. Please contact your administrator."
        is ConfigChecksumMismatch -> "Configuration integrity check failed."
        is ConfigDownloadFailed -> "Failed to download configuration: $message"
        ConfigRolledBack -> "Configuration update failed and was rolled back."
        QueueStale -> "Queue data is outdated. Attempting to reconnect."
        is InvalidStateTransition -> "Invalid queue state: $from → $to"
        is StorageReadFailed -> "Failed to read setting: $key"
        is StorageWriteFailed -> "Failed to save setting: $key"
        is DeviceCommandFailed -> "Command $command failed: $reason"
        is UnexpectedError -> "An unexpected error occurred: $message"
    }
}

/** Exception that carries a typed [QueueRxError]. */
class QueueRxException(val error: QueueRxError) : Exception(error.userMessage())
