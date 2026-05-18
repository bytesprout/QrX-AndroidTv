package com.queuerx.tv.feature.provisioning

import com.queuerx.tv.data.device.ProvisioningFlowState
import com.queuerx.tv.data.device.ProvisioningStateMachine
import com.queuerx.tv.domain.device.ActivationMethod
import com.queuerx.tv.domain.device.DeviceInfo
import com.queuerx.tv.domain.device.DeviceType
import com.queuerx.tv.domain.device.ValidateActivationCodeUseCase
import com.queuerx.tv.domain.device.ValidateQrActivationTokenUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * Feature-layer coordinator for the provisioning screen.
 *
 * Bridges [ProvisioningStateMachine] (data layer) to [ProvisioningUiState]
 * (feature layer). All UI interaction is initiated through this class; the
 * Compose screen is a pure renderer of [uiState].
 *
 * Lifecycle note: on Android this lives in a ViewModel; in tests it can be
 * driven directly with a [TestCoroutineScope].
 */
class ProvisioningCoordinator(
    private val stateMachine: ProvisioningStateMachine,
    private val deviceInfo: DeviceInfo,
    private val validateQrUseCase: ValidateQrActivationTokenUseCase = ValidateQrActivationTokenUseCase(),
    private val validateCodeUseCase: ValidateActivationCodeUseCase = ValidateActivationCodeUseCase(),
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) {

    private val _uiState = MutableStateFlow<ProvisioningUiState>(ProvisioningUiState.Loading)
    val uiState: StateFlow<ProvisioningUiState> = _uiState

    init {
        observeStateMachine()
    }

    private fun observeStateMachine() {
        stateMachine.state
            .onEach { flowState -> _uiState.value = flowState.toUiState() }
            .launchIn(scope)
    }

    /** Called on screen entry to check if provisioning is needed. */
    fun checkProvisionStatus() {
        scope.launch { stateMachine.checkProvisionStatus() }
    }

    /** User selected QR activation from the menu. */
    fun onQrActivationSelected() {
        _uiState.value = ProvisioningUiState.ScanningQr
    }

    /** User selected manual code entry from the menu. */
    fun onCodeActivationSelected() {
        _uiState.value = ProvisioningUiState.EnteringCode
    }

    /**
     * Called by the QR scan overlay when a URI is decoded.
     *
     * Validates the URI format before handing to the state machine.
     */
    fun onQrScanned(uri: String) {
        val result = validateQrUseCase(uri)
        result.onSuccess { method ->
            scope.launch {
                stateMachine.activate(method, deviceInfo)
            }
        }.onError { error ->
            _uiState.value = ProvisioningUiState.Error(
                message = error.userMessage(),
                step = "QR_VALIDATION",
                canRetry = true
            )
        }
    }

    /**
     * Called when the user submits a manual activation code.
     *
     * Validates format before sending to the backend.
     */
    fun onActivationCodeSubmitted(code: String) {
        val result = validateCodeUseCase(code)
        result.onSuccess { method ->
            scope.launch {
                stateMachine.activate(method, deviceInfo)
            }
        }.onError { error ->
            _uiState.value = ProvisioningUiState.Error(
                message = error.userMessage(),
                step = "CODE_VALIDATION",
                canRetry = true
            )
        }
    }

    /**
     * Called on enterprise deployments that use MAC-based auto-registration.
     *
     * Should be invoked on first boot if MAC activation is configured.
     */
    fun onMacActivation(macAddress: String) {
        scope.launch {
            stateMachine.activate(
                ActivationMethod.MacActivation(macAddress),
                deviceInfo
            )
        }
    }

    /** Operator pressed the Retry button. */
    fun onRetry() {
        scope.launch { stateMachine.checkProvisionStatus() }
    }

    /** Factory reset / reprovision — clears state and shows activation screen. */
    fun onReprovision() {
        scope.launch { stateMachine.reset() }
    }

    // ── Mapping ───────────────────────────────────────────────────────────────

    private fun ProvisioningFlowState.toUiState(): ProvisioningUiState = when (this) {
        ProvisioningFlowState.CheckingProvision -> ProvisioningUiState.Loading
        ProvisioningFlowState.NeedsActivation -> ProvisioningUiState.ShowActivation
        is ProvisioningFlowState.Activating -> ProvisioningUiState.Activating(
            method = when (method) {
                is ActivationMethod.QrActivation -> ActivationMethodUi.QR_CODE
                is ActivationMethod.CodeActivation -> ActivationMethodUi.ACTIVATION_CODE
                is ActivationMethod.MacActivation -> ActivationMethodUi.MAC_ADDRESS
            }
        )
        is ProvisioningFlowState.SyncingConfig -> ProvisioningUiState.SyncingConfig(
            hospitalName = registration.displayId
        )
        ProvisioningFlowState.Provisioned -> ProvisioningUiState.Provisioned
        is ProvisioningFlowState.Error -> ProvisioningUiState.Error(
            message = error,
            step = step,
            canRetry = true
        )
    }
}
