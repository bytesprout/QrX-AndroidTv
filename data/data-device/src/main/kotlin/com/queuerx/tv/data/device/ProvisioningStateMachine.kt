package com.queuerx.tv.data.device

import com.queuerx.tv.core.common.ApiResult
import com.queuerx.tv.domain.device.ActivateDeviceUseCase
import com.queuerx.tv.domain.device.ActivationMethod
import com.queuerx.tv.domain.device.DeviceInfo
import com.queuerx.tv.domain.device.DeviceRegistrationResponse
import com.queuerx.tv.domain.device.DeviceRepository
import com.queuerx.tv.domain.device.GetProvisionStateUseCase
import com.queuerx.tv.domain.device.ProvisionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Drives the provisioning lifecycle from first launch to fully ready display.
 *
 * Spec Part 6 flow:
 *   Splash → Provision Check → Activation → Configuration Sync → Ready
 *
 * Each transition is persisted to [DeviceRepository] so that a crash or power
 * loss during provisioning resumes at the correct step on next boot.
 *
 * This class is intentionally side-effect free in tests — inject a fake
 * [DeviceRepository] and [ConfigurationSyncManager] to drive any scenario.
 */
class ProvisioningStateMachine(
    private val repository: DeviceRepository,
    private val configSyncManager: ConfigurationSyncManager,
    private val activateDeviceUseCase: ActivateDeviceUseCase = ActivateDeviceUseCase(repository),
    private val getProvisionStateUseCase: GetProvisionStateUseCase = GetProvisionStateUseCase(repository)
) {

    private val _state = MutableStateFlow<ProvisioningFlowState>(ProvisioningFlowState.CheckingProvision)
    val state: StateFlow<ProvisioningFlowState> = _state

    /**
     * Entry point called on app start.
     *
     * Checks the persisted [ProvisionState]:
     * - If [ProvisionState.PROVISIONED] → emits [ProvisioningFlowState.Provisioned]
     * - If any other state → emits [ProvisioningFlowState.NeedsActivation]
     */
    suspend fun checkProvisionStatus() {
        _state.value = ProvisioningFlowState.CheckingProvision
        val state = getProvisionStateUseCase()
        _state.value = if (state == ProvisionState.PROVISIONED) {
            ProvisioningFlowState.Provisioned
        } else {
            ProvisioningFlowState.NeedsActivation
        }
    }

    /**
     * Executes full activation → registration → config-sync pipeline.
     *
     * The [ProvisioningFlowState] is updated at each step so the UI can show progress.
     * On any failure the error is emitted and the persistent state remains at the
     * failed step so the operator can retry.
     */
    suspend fun activate(
        method: ActivationMethod,
        deviceInfo: DeviceInfo
    ) {
        _state.value = ProvisioningFlowState.Activating(method)

        val activationResult = activateDeviceUseCase(method, deviceInfo)
        if (activationResult is ApiResult.Error) {
            _state.value = ProvisioningFlowState.Error(
                error = activationResult.error.userMessage(),
                step = "ACTIVATION"
            )
            return
        }

        val registration = (activationResult as ApiResult.Success).data
        _state.value = ProvisioningFlowState.SyncingConfig(registration)

        val syncResult = configSyncManager.sync(
            displayId = registration.displayId,
            currentVersion = null
        )

        _state.value = if (syncResult is ApiResult.Success) {
            repository.saveProvisionState(ProvisionState.PROVISIONED)
            ProvisioningFlowState.Provisioned
        } else {
            val error = (syncResult as ApiResult.Error).error
            ProvisioningFlowState.Error(
                error = error.userMessage(),
                step = "CONFIG_SYNC"
            )
        }
    }

    /** Resets provisioning to allow re-activation (e.g. after factory reset). */
    suspend fun reset() {
        repository.saveProvisionState(ProvisionState.NOT_PROVISIONED)
        _state.value = ProvisioningFlowState.NeedsActivation
    }
}

/**
 * Observable provisioning UI state emitted by [ProvisioningStateMachine].
 */
sealed class ProvisioningFlowState {
    /** Initial state — checking persisted [ProvisionState]. */
    data object CheckingProvision : ProvisioningFlowState()

    /** Device has no prior activation; show provisioning screen. */
    data object NeedsActivation : ProvisioningFlowState()

    /** Activation request in progress. */
    data class Activating(val method: ActivationMethod) : ProvisioningFlowState()

    /** Activation complete; downloading and applying configuration. */
    data class SyncingConfig(val registration: DeviceRegistrationResponse) : ProvisioningFlowState()

    /** Fully provisioned — launch display. */
    data object Provisioned : ProvisioningFlowState()

    /** An error occurred at the named [step]. The operator can retry. */
    data class Error(val error: String, val step: String) : ProvisioningFlowState()
}
