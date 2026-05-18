package com.queuerx.tv.domain.auth

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

class AuthUseCasesTest {

    private lateinit var repository: AuthRepository

    private fun freshToken(expiresFromNow: Long = 3_600_000L) = AuthToken(
        accessToken = "access-abc",
        refreshToken = "refresh-xyz",
        expiresAtEpochMillis = System.currentTimeMillis() + expiresFromNow
    )

    private fun expiredToken() = AuthToken(
        accessToken = "old-access",
        refreshToken = "refresh-xyz",
        expiresAtEpochMillis = System.currentTimeMillis() - 1_000L
    )

    private fun almostExpiredToken() = AuthToken(
        accessToken = "almost-expired",
        refreshToken = "refresh-xyz",
        // expires 30 seconds from now — within the 60-second buffer
        expiresAtEpochMillis = System.currentTimeMillis() + 30_000L
    )

    @BeforeEach
    fun setUp() {
        repository = mockk(relaxed = true)
    }

    // ── GetValidAuthTokenUseCase ───────────────────────────────────────────────

    @Nested
    inner class GetValidToken {
        @Test
        fun `valid token is returned successfully`() = runTest {
            val token = freshToken()
            coEvery { repository.getToken() } returns token

            val result = GetValidAuthTokenUseCase(repository)()

            assertThat(result.isSuccess).isTrue()
            assertThat((result as ApiResult.Success).data).isEqualTo(token)
        }

        @Test
        fun `no token returns TokenInvalid`() = runTest {
            coEvery { repository.getToken() } returns null

            val result = GetValidAuthTokenUseCase(repository)()

            assertThat(result.isError).isTrue()
            assertThat((result as ApiResult.Error).error).isEqualTo(QueueRxError.TokenInvalid)
        }

        @Test
        fun `expired token returns TokenExpired`() = runTest {
            coEvery { repository.getToken() } returns expiredToken()

            val result = GetValidAuthTokenUseCase(repository)()

            assertThat(result.isError).isTrue()
            assertThat((result as ApiResult.Error).error).isEqualTo(QueueRxError.TokenExpired)
        }
    }

    // ── EnsureFreshTokenUseCase ───────────────────────────────────────────────

    @Nested
    inner class EnsureFreshToken {
        @Test
        fun `fresh token is returned without refreshing`() = runTest {
            val token = freshToken()
            coEvery { repository.getToken() } returns token

            val result = EnsureFreshTokenUseCase(repository)()

            assertThat(result.isSuccess).isTrue()
            coVerify(exactly = 0) { repository.refreshToken(any()) }
        }

        @Test
        fun `expiring-soon token triggers refresh`() = runTest {
            val oldToken = almostExpiredToken()
            val newToken = freshToken()
            coEvery { repository.getToken() } returns oldToken
            coEvery { repository.refreshToken("refresh-xyz") } returns ApiResult.Success(newToken)
            coEvery { repository.saveToken(newToken) } returns ApiResult.Success(Unit)

            val result = EnsureFreshTokenUseCase(repository)()

            assertThat(result.isSuccess).isTrue()
            assertThat((result as ApiResult.Success).data.accessToken).isEqualTo("access-abc")
            coVerify(exactly = 1) { repository.refreshToken("refresh-xyz") }
        }

        @Test
        fun `refresh failure propagates RefreshFailed error`() = runTest {
            val oldToken = almostExpiredToken()
            coEvery { repository.getToken() } returns oldToken
            coEvery { repository.refreshToken(any()) } returns ApiResult.Error(QueueRxError.RefreshFailed)

            val result = EnsureFreshTokenUseCase(repository)()

            assertThat(result.isError).isTrue()
            assertThat((result as ApiResult.Error).error).isEqualTo(QueueRxError.RefreshFailed)
        }

        @Test
        fun `no stored token returns TokenInvalid`() = runTest {
            coEvery { repository.getToken() } returns null

            val result = EnsureFreshTokenUseCase(repository)()

            assertThat(result.isError).isTrue()
            assertThat((result as ApiResult.Error).error).isEqualTo(QueueRxError.TokenInvalid)
        }
    }

    // ── ClearAuthTokenUseCase ─────────────────────────────────────────────────

    @Nested
    inner class ClearToken {
        @Test
        fun `delegates to repository clearToken`() = runTest {
            coEvery { repository.clearToken() } returns ApiResult.Success(Unit)

            val result = ClearAuthTokenUseCase(repository)()

            assertThat(result.isSuccess).isTrue()
            coVerify(exactly = 1) { repository.clearToken() }
        }
    }

    // ── AuthToken model ───────────────────────────────────────────────────────

    @Nested
    inner class AuthTokenModel {
        @Test
        fun `isExpired returns false for fresh token`() {
            assertThat(freshToken().isExpired()).isFalse()
        }

        @Test
        fun `isExpired returns true for expired token`() {
            assertThat(expiredToken().isExpired()).isTrue()
        }

        @Test
        fun `isExpiringSoon returns true within 60 second buffer`() {
            assertThat(almostExpiredToken().isExpiringSoon()).isTrue()
        }

        @Test
        fun `isExpiringSoon returns false for token with ample time left`() {
            assertThat(freshToken().isExpiringSoon()).isFalse()
        }
    }
}
