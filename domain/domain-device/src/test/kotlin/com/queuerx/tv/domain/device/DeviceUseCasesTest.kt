package com.queuerx.tv.domain.device

import com.google.common.truth.Truth.assertThat
import com.queuerx.tv.core.common.ApiResult
import com.queuerx.tv.core.common.QueueRxError
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class DeviceUseCasesTest {

    private lateinit var repository: DeviceRepository
    private val deviceInfo = DeviceInfo(
        deviceId = "device-001",
        deviceName = "Reception TV 1",
        mac = "AA:BB:CC:DD:EE:FF",
        model = "Sony X85",
        osVersion = "Android 12",
        appVersion = "1.0.0"
    )
    private val activationResponse = ActivationResponse(
        activationToken = "act-token-xyz",
        hospitalId = "hosp-01",
        tenantId = "tenant-01",
        branchId = "branch-01",
        deviceType = "WAITING_HALL",
        displayName = "Reception Display"
    )
    private val registrationResponse = DeviceRegistrationResponse(
        deviceId = "device-001",
        displayId = "disp-001",
        hospitalId = "hosp-01",
        tenantId = "tenant-01",
        branchId = "branch-01",
        deviceType = "WAITING_HALL",
        configVersion = "v1.0"
    )

    @BeforeEach
    fun setUp() {
        repository = mockk(relaxed = true)
    }

    // ── GetProvisionStateUseCase ───────────────────────────────────────────────

    @Nested
    inner class GetProvisionState {
        @Test
        fun `returns NOT_PROVISIONED on fresh device`() = runTest {
            coEvery { repository.getProvisionState() } returns ProvisionState.NOT_PROVISIONED
            val uc = GetProvisionStateUseCase(repository)
            assertThat(uc()).isEqualTo(ProvisionState.NOT_PROVISIONED)
        }

        @Test
        fun `returns PROVISIONED when already activated`() = runTest {
            coEvery { repository.getProvisionState() } returns ProvisionState.PROVISIONED
            val uc = GetProvisionStateUseCase(repository)
            assertThat(uc()).isEqualTo(ProvisionState.PROVISIONED)
        }
    }

    // ── NeedsProvisioningUseCase ───────────────────────────────────────────────

    @Nested
    inner class NeedsProvisioning {
        @Test
        fun `returns true when state is NOT_PROVISIONED`() = runTest {
            coEvery { repository.getProvisionState() } returns ProvisionState.NOT_PROVISIONED
            assertThat(NeedsProvisioningUseCase(repository)()).isTrue()
        }

        @Test
        fun `returns false when state is PROVISIONED`() = runTest {
            coEvery { repository.getProvisionState() } returns ProvisionState.PROVISIONED
            assertThat(NeedsProvisioningUseCase(repository)()).isFalse()
        }
    }

    // ── ActivateDeviceUseCase ─────────────────────────────────────────────────

    @Nested
    inner class ActivateDevice {
        @Test
        fun `successful QR activation follows full flow`() = runTest {
            coEvery { repository.saveProvisionState(any()) } returns ApiResult.Success(Unit)
            coEvery { repository.activate(any()) } returns ApiResult.Success(activationResponse)
            coEvery { repository.register(any(), any()) } returns ApiResult.Success(registrationResponse)
            coEvery { repository.saveDeviceContext(any()) } returns ApiResult.Success(Unit)

            val useCase = ActivateDeviceUseCase(repository)
            val result = useCase(ActivationMethod.QrActivation("ABCDEF12345"), deviceInfo)

            assertThat(result.isSuccess).isTrue()
            assertThat((result as ApiResult.Success).data).isEqualTo(registrationResponse)

            // Verify state transitions in order
            coVerify(ordering = io.mockk.Ordering.SEQUENCE) {
                repository.saveProvisionState(ProvisionState.ACTIVATING)
                repository.activate(any())
                repository.saveProvisionState(ProvisionState.REGISTERING)
                repository.register(any(), any())
                repository.saveDeviceContext(registrationResponse)
                repository.saveProvisionState(ProvisionState.SYNCING_CONFIG)
            }
        }

        @Test
        fun `activation backend failure stops flow at ACTIVATING step`() = runTest {
            coEvery { repository.saveProvisionState(any()) } returns ApiResult.Success(Unit)
            coEvery { repository.activate(any()) } returns ApiResult.Error(
                QueueRxError.ActivationFailed("Invalid token")
            )

            val result = ActivateDeviceUseCase(repository)(
                ActivationMethod.QrActivation("BADTOKEN1"),
                deviceInfo
            )

            assertThat(result.isError).isTrue()
            assertThat((result as ApiResult.Error).error)
                .isInstanceOf(QueueRxError.ActivationFailed::class.java)

            // Registration must NOT be called
            coVerify(exactly = 0) { repository.register(any(), any()) }
        }

        @Test
        fun `registration failure returns RegistrationFailed error`() = runTest {
            coEvery { repository.saveProvisionState(any()) } returns ApiResult.Success(Unit)
            coEvery { repository.activate(any()) } returns ApiResult.Success(activationResponse)
            coEvery { repository.register(any(), any()) } returns ApiResult.Error(
                QueueRxError.RegistrationFailed("MAC conflict")
            )

            val result = ActivateDeviceUseCase(repository)(
                ActivationMethod.CodeActivation("HSP-9823"),
                deviceInfo
            )

            assertThat(result.isError).isTrue()
            coVerify(exactly = 0) { repository.saveDeviceContext(any()) }
        }

        @Test
        fun `MAC activation method flows through correctly`() = runTest {
            coEvery { repository.saveProvisionState(any()) } returns ApiResult.Success(Unit)
            coEvery { repository.activate(ofType<ActivationMethod.MacActivation>()) } returns ApiResult.Success(activationResponse)
            coEvery { repository.register(any(), any()) } returns ApiResult.Success(registrationResponse)
            coEvery { repository.saveDeviceContext(any()) } returns ApiResult.Success(Unit)

            val result = ActivateDeviceUseCase(repository)(
                ActivationMethod.MacActivation("AA:BB:CC:DD:EE:FF"),
                deviceInfo
            )

            assertThat(result.isSuccess).isTrue()
        }
    }
}
