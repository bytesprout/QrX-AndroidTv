package com.queuerx.tv.data.device

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.queuerx.tv.core.common.ApiResult
import com.queuerx.tv.core.common.QueueRxError
import com.queuerx.tv.domain.device.ActivateDeviceUseCase
import com.queuerx.tv.domain.device.ActivationMethod
import com.queuerx.tv.domain.device.ActivationResponse
import com.queuerx.tv.domain.device.DeviceInfo
import com.queuerx.tv.domain.device.DeviceRegistrationResponse
import com.queuerx.tv.domain.device.DeviceRepository
import com.queuerx.tv.domain.device.GetProvisionStateUseCase
import com.queuerx.tv.domain.device.ProvisionState
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Tests for [ProvisioningStateMachine].
 *
 * Uses Turbine to assert the StateFlow emissions in order.
 */
class ProvisioningStateMachineTest {

    private lateinit var repository: DeviceRepository
    private lateinit var configSyncManager: ConfigurationSyncManager

    private val deviceInfo = DeviceInfo(
        deviceId = "dev-001",
        deviceName = "Hall TV 1",
        mac = "AA:BB:CC:DD:EE:FF",
        model = "Samsung QN85",
        osVersion = "Android 12",
        appVersion = "1.0.0"
    )
    private val activationResponse = ActivationResponse(
        activationToken = "tok-abc",
        hospitalId = "hosp-01",
        tenantId = "ten-01",
        branchId = "br-01",
        deviceType = "WAITING_HALL",
        displayName = "Hall Display"
    )
    private val registrationResponse = DeviceRegistrationResponse(
        deviceId = "dev-001",
        displayId = "disp-001",
        hospitalId = "hosp-01",
        tenantId = "ten-01",
        branchId = "br-01",
        deviceType = "WAITING_HALL",
        configVersion = "v1.0"
    )
    private val displayConfig = DisplayConfig(
        configVersion = "v1.0",
        checksum = "abc",
        displayId = "disp-001",
        displayType = "WAITING_HALL",
        hospitalName = "Test Hospital",
        branding = BrandingConfig(),
        layout = LayoutConfig(emptyList()),
        audio = AudioConfig(),
        ticker = TickerConfig(),
        ttsLanguage = "en",
        queueBindings = emptyList()
    )

    @BeforeEach
    fun setUp() {
        repository = mockk(relaxed = true)
        configSyncManager = mockk(relaxed = true)
    }

    private fun buildMachine() = ProvisioningStateMachine(
        repository = repository,
        configSyncManager = configSyncManager,
        activateDeviceUseCase = ActivateDeviceUseCase(repository),
        getProvisionStateUseCase = GetProvisionStateUseCase(repository)
    )

    // ── checkProvisionStatus ──────────────────────────────────────────────────

    @Nested
    inner class CheckProvisionStatus {
        @Test
        fun `emits Provisioned when state is PROVISIONED`() = runTest {
            coEvery { repository.getProvisionState() } returns ProvisionState.PROVISIONED

            val machine = buildMachine()
            machine.state.test {
                // Initial emission: CheckingProvision
                val initial = awaitItem()
                assertThat(initial).isEqualTo(ProvisioningFlowState.CheckingProvision)

                machine.checkProvisionStatus()

                // StateFlow dedups: CheckingProvision→CheckingProvision is skipped.
                // We go straight to Provisioned.
                val provisioned = awaitItem()
                assertThat(provisioned).isEqualTo(ProvisioningFlowState.Provisioned)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        fun `emits NeedsActivation when state is NOT_PROVISIONED`() = runTest {
            coEvery { repository.getProvisionState() } returns ProvisionState.NOT_PROVISIONED

            val machine = buildMachine()
            machine.state.test {
                // Initial emission
                awaitItem() // CheckingProvision

                machine.checkProvisionStatus()

                // StateFlow dedups the repeated CheckingProvision assignment
                val state = awaitItem()
                assertThat(state).isEqualTo(ProvisioningFlowState.NeedsActivation)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    // ── activate ─────────────────────────────────────────────────────────────

    @Nested
    inner class Activate {
        @Test
        fun `successful QR activation emits Activating, SyncingConfig, Provisioned`() = runTest {
            coEvery { repository.saveProvisionState(any()) } returns ApiResult.Success(Unit)
            coEvery { repository.activate(any()) } returns ApiResult.Success(activationResponse)
            coEvery { repository.register(any(), any()) } returns ApiResult.Success(registrationResponse)
            coEvery { repository.saveDeviceContext(any()) } returns ApiResult.Success(Unit)
            coEvery { configSyncManager.sync(any(), any()) } returns ApiResult.Success(displayConfig)

            val machine = buildMachine()
            machine.state.test {
                awaitItem() // initial CheckingProvision
                machine.activate(ActivationMethod.QrActivation("TOKEN12345"), deviceInfo)

                val activating = awaitItem()
                assertThat(activating).isInstanceOf(ProvisioningFlowState.Activating::class.java)

                val syncing = awaitItem()
                assertThat(syncing).isInstanceOf(ProvisioningFlowState.SyncingConfig::class.java)

                val provisioned = awaitItem()
                assertThat(provisioned).isEqualTo(ProvisioningFlowState.Provisioned)

                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        fun `activation backend error emits Error state with step ACTIVATION`() = runTest {
            coEvery { repository.saveProvisionState(any()) } returns ApiResult.Success(Unit)
            coEvery { repository.activate(any()) } returns ApiResult.Error(
                QueueRxError.ActivationFailed("Invalid QR")
            )

            val machine = buildMachine()
            machine.state.test {
                awaitItem()
                machine.activate(ActivationMethod.QrActivation("BADTOKEN1"), deviceInfo)
                awaitItem() // Activating
                val error = awaitItem()
                assertThat(error).isInstanceOf(ProvisioningFlowState.Error::class.java)
                assertThat((error as ProvisioningFlowState.Error).step).isEqualTo("ACTIVATION")
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        fun `config sync failure emits Error with step CONFIG_SYNC`() = runTest {
            coEvery { repository.saveProvisionState(any()) } returns ApiResult.Success(Unit)
            coEvery { repository.activate(any()) } returns ApiResult.Success(activationResponse)
            coEvery { repository.register(any(), any()) } returns ApiResult.Success(registrationResponse)
            coEvery { repository.saveDeviceContext(any()) } returns ApiResult.Success(Unit)
            coEvery { configSyncManager.sync(any(), any()) } returns ApiResult.Error(
                QueueRxError.ConfigDownloadFailed("Server unreachable")
            )

            val machine = buildMachine()
            machine.state.test {
                awaitItem()
                machine.activate(ActivationMethod.CodeActivation("HSP-9823"), deviceInfo)
                awaitItem() // Activating
                awaitItem() // SyncingConfig
                val error = awaitItem()
                assertThat(error).isInstanceOf(ProvisioningFlowState.Error::class.java)
                assertThat((error as ProvisioningFlowState.Error).step).isEqualTo("CONFIG_SYNC")
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    // ── reset ─────────────────────────────────────────────────────────────────

    @Test
    fun `reset emits NeedsActivation`() = runTest {
        val machine = buildMachine()
        machine.state.test {
            awaitItem()
            machine.reset()
            val state = awaitItem()
            assertThat(state).isEqualTo(ProvisioningFlowState.NeedsActivation)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
