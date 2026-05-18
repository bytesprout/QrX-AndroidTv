package com.queuerx.tv.feature.provisioning

/**
 * UI state emitted by [ProvisioningCoordinator].
 *
 * The provisioning screen observes this sealed class to decide which view to render.
 * Using `sealed interface` with `data class` / `data object` members ensures
 * exhaustive when-expressions in Compose.
 */
sealed interface ProvisioningUiState {

    /** Checking whether the device is already provisioned (first emission). */
    data object Loading : ProvisioningUiState

    /** Device is not yet activated. Show the activation method selector. */
    data object ShowActivation : ProvisioningUiState

    /** QR scanner overlay is displayed, waiting for scan. */
    data object ScanningQr : ProvisioningUiState

    /** Manual activation code entry screen. */
    data object EnteringCode : ProvisioningUiState

    /** Activation request sent to backend; spinner visible. */
    data class Activating(
        val method: ActivationMethodUi
    ) : ProvisioningUiState

    /** Downloading and applying remote configuration. */
    data class SyncingConfig(
        val hospitalName: String = "",
        val progressPercent: Int = 0
    ) : ProvisioningUiState

    /** Provisioning succeeded. Navigate to main display. */
    data object Provisioned : ProvisioningUiState

    /** Non-recoverable error. Show error message + retry button. */
    data class Error(
        val message: String,
        val step: String,
        val canRetry: Boolean = true
    ) : ProvisioningUiState
}

/** UI representation of the selected activation method. */
enum class ActivationMethodUi {
    QR_CODE,
    ACTIVATION_CODE,
    MAC_ADDRESS
}
