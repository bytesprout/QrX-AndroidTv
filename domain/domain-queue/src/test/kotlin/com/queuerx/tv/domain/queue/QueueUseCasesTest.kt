package com.queuerx.tv.domain.queue

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

class QueueUseCasesTest {

    private lateinit var repository: QueueRepository

    private fun token(num: String, state: TokenState = TokenState.WAITING) = QueueToken(
        tokenNumber = num,
        state = state,
        roomNumber = "Room 1"
    )

    private fun doctorQueue(id: String, now: QueueToken? = null, upcoming: List<QueueToken> = emptyList()) =
        DoctorQueue(
            departmentId = id,
            departmentName = "Dept $id",
            doctorName = "Dr. Test",
            roomNumber = "Room 1",
            nowServing = now,
            upcoming = upcoming,
            recentlyCalled = emptyList()
        )

    @BeforeEach
    fun setUp() {
        repository = mockk(relaxed = true)
    }

    // ── GetCurrentTokenUseCase ────────────────────────────────────────────────

    @Nested
    inner class GetCurrentToken {
        @Test
        fun `returns nowServing token for a department`() = runTest {
            coEvery { repository.getLastUpdateTimestamp() } returns System.currentTimeMillis()
            coEvery { repository.getDoctorQueues() } returns listOf(
                doctorQueue("dept-1", token("A-100"))
            )

            val result = GetCurrentTokenUseCase(repository)("dept-1")
            assertThat(result.isSuccess).isTrue()
            assertThat((result as ApiResult.Success).data?.tokenNumber).isEqualTo("A-100")
        }

        @Test
        fun `returns null when no token is being served`() = runTest {
            coEvery { repository.getLastUpdateTimestamp() } returns System.currentTimeMillis()
            coEvery { repository.getDoctorQueues() } returns listOf(doctorQueue("dept-1"))

            val result = GetCurrentTokenUseCase(repository)("dept-1")
            assertThat(result.isSuccess).isTrue()
            assertThat((result as ApiResult.Success).data).isNull()
        }

        @Test
        fun `returns QueueStale when data is too old`() = runTest {
            val staleTime = System.currentTimeMillis() - 10 * 60 * 1_000L
            coEvery { repository.getLastUpdateTimestamp() } returns staleTime

            val result = GetCurrentTokenUseCase(repository)("dept-1")
            assertThat(result.isError).isTrue()
            assertThat((result as ApiResult.Error).error).isEqualTo(QueueRxError.QueueStale)
        }

        @Test
        fun `returns null when department not found`() = runTest {
            coEvery { repository.getLastUpdateTimestamp() } returns System.currentTimeMillis()
            coEvery { repository.getDoctorQueues() } returns emptyList()

            val result = GetCurrentTokenUseCase(repository)("unknown")
            assertThat(result.isSuccess).isTrue()
            assertThat((result as ApiResult.Success).data).isNull()
        }
    }

    // ── TransitionTokenStateUseCase ───────────────────────────────────────────

    @Nested
    inner class TransitionState {
        @Test
        fun `valid transition succeeds and calls repository`() = runTest {
            coEvery { repository.getToken("A-001") } returns token("A-001", TokenState.WAITING)

            val result = TransitionTokenStateUseCase(repository)("A-001", TokenState.CALLED)
            assertThat(result.isSuccess).isTrue()
            coVerify { repository.updateTokenState("A-001", TokenState.CALLED) }
        }

        @Test
        fun `invalid transition returns InvalidStateTransition error`() = runTest {
            coEvery { repository.getToken("A-001") } returns token("A-001", TokenState.WAITING)

            val result = TransitionTokenStateUseCase(repository)("A-001", TokenState.COLLECTED)
            assertThat(result.isError).isTrue()
            assertThat((result as ApiResult.Error).error)
                .isInstanceOf(QueueRxError.InvalidStateTransition::class.java)
        }

        @Test
        fun `unknown token returns UnexpectedError`() = runTest {
            coEvery { repository.getToken("UNKNOWN") } returns null

            val result = TransitionTokenStateUseCase(repository)("UNKNOWN", TokenState.CALLED)
            assertThat(result.isError).isTrue()
            coVerify(exactly = 0) { repository.updateTokenState(any(), any()) }
        }
    }

    // ── GetUpcomingTokensUseCase ──────────────────────────────────────────────

    @Nested
    inner class GetUpcomingTokens {
        @Test
        fun `returns sorted tokens by displayPriority`() = runTest {
            val waiting = token("A-001", TokenState.WAITING)
            val called = token("A-002", TokenState.CALLED)
            coEvery { repository.getDoctorQueues() } returns listOf(
                doctorQueue("dept-1", upcoming = listOf(waiting, called))
            )

            val result = GetUpcomingTokensUseCase(repository)("dept-1")
            assertThat(result.isSuccess).isTrue()
            val tokens = (result as ApiResult.Success).data
            // CALLED (priority 1) should appear before WAITING (priority 4)
            assertThat(tokens.first().state).isEqualTo(TokenState.CALLED)
        }
    }
}
