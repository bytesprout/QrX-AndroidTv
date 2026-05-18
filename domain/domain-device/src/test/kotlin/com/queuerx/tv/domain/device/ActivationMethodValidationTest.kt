package com.queuerx.tv.domain.device

import com.google.common.truth.Truth.assertThat
import com.queuerx.tv.core.common.ApiResult
import com.queuerx.tv.core.common.QueueRxError
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class ActivationMethodValidationTest {

    // ── QR activation ─────────────────────────────────────────────────────────

    private val validateQr = ValidateQrActivationTokenUseCase()

    @Nested
    inner class QrValidation {
        @Test
        fun `valid queuerx URI returns QrActivation`() {
            val result = validateQr("queuerx://activate?token=ABCDEF12")
            assertThat(result.isSuccess).isTrue()
            assertThat((result as ApiResult.Success).data.activationToken).isEqualTo("ABCDEF12")
        }

        @Test
        fun `valid URI with multiple query params extracts token`() {
            val result = validateQr("queuerx://activate?foo=bar&token=TOKENVALUE")
            assertThat(result.isSuccess).isTrue()
            assertThat((result as ApiResult.Success).data.activationToken).isEqualTo("TOKENVALUE")
        }

        @Test
        fun `wrong scheme returns InvalidActivationToken`() {
            val result = validateQr("https://activate?token=ABCDEF12")
            assertThat(result.isError).isTrue()
            assertThat((result as ApiResult.Error).error).isInstanceOf(QueueRxError.InvalidActivationToken::class.java)
        }

        @Test
        fun `missing token param returns InvalidActivationToken`() {
            val result = validateQr("queuerx://activate?foo=bar")
            assertThat(result.isError).isTrue()
        }

        @Test
        fun `token shorter than 8 chars returns error`() {
            val result = validateQr("queuerx://activate?token=ABC")
            assertThat(result.isError).isTrue()
        }

        @Test
        fun `blank URI returns error`() {
            val result = validateQr("")
            assertThat(result.isError).isTrue()
        }
    }

    // ── Code activation ───────────────────────────────────────────────────────

    private val validateCode = ValidateActivationCodeUseCase()

    @Nested
    inner class CodeValidation {
        @Test
        fun `valid code HSP-9823 is accepted`() {
            val result = validateCode("HSP-9823")
            assertThat(result.isSuccess).isTrue()
            assertThat((result as ApiResult.Success).data.activationCode).isEqualTo("HSP-9823")
        }

        @Test
        fun `lowercase code is uppercased and accepted`() {
            val result = validateCode("hsp-9823")
            assertThat(result.isSuccess).isTrue()
        }

        @Test
        fun `code with spaces is trimmed and accepted`() {
            val result = validateCode("  HSP-9823  ")
            assertThat(result.isSuccess).isTrue()
        }

        @Test
        fun `numeric-only code is rejected`() {
            val result = validateCode("1234567")
            assertThat(result.isError).isTrue()
            assertThat((result as ApiResult.Error).error).isInstanceOf(QueueRxError.InvalidActivationCode::class.java)
        }

        @Test
        fun `too-short code is rejected`() {
            val result = validateCode("HS-123")
            assertThat(result.isError).isTrue()
        }

        @Test
        fun `letters-only code is rejected`() {
            val result = validateCode("HSPABC")
            assertThat(result.isError).isTrue()
        }

        @Test
        fun `blank code is rejected`() {
            val result = validateCode("")
            assertThat(result.isError).isTrue()
        }
    }

    // ── MAC activation ────────────────────────────────────────────────────────

    @Nested
    inner class MacActivation {
        @Test
        fun `valid colon-separated MAC is accepted`() {
            val method = ActivationMethod.MacActivation("AA:BB:CC:DD:EE:FF")
            assertThat(method.macAddress).isEqualTo("AA:BB:CC:DD:EE:FF")
        }

        @Test
        fun `valid dash-separated MAC is accepted`() {
            val method = ActivationMethod.MacActivation("AA-BB-CC-DD-EE-FF")
            assertThat(method.macAddress).isEqualTo("AA-BB-CC-DD-EE-FF")
        }

        @Test
        fun `invalid MAC throws IllegalArgumentException`() {
            val ex = runCatching { ActivationMethod.MacActivation("not-a-mac") }.exceptionOrNull()
            assertThat(ex).isInstanceOf(IllegalArgumentException::class.java)
        }
    }
}
