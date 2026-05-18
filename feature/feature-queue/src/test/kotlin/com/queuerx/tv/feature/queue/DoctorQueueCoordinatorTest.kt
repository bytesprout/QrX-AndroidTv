package com.queuerx.tv.feature.queue

import com.google.common.truth.Truth.assertThat
import com.queuerx.tv.core.common.ApiResult
import com.queuerx.tv.core.realtime.RealtimeEvent
import com.queuerx.tv.core.realtime.RealtimeEventType
import com.queuerx.tv.domain.queue.DoctorQueue
import com.queuerx.tv.domain.queue.GetCurrentTokenUseCase
import com.queuerx.tv.domain.queue.GetUpcomingTokensUseCase
import com.queuerx.tv.domain.queue.QueueRepository
import com.queuerx.tv.domain.queue.QueueToken
import com.queuerx.tv.domain.queue.TokenState
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DoctorQueueCoordinatorTest {

    private lateinit var repository: QueueRepository
    private lateinit var realtimeEvents: MutableSharedFlow<RealtimeEvent>
    private val scope = TestScope()

    private fun token(num: String, state: TokenState = TokenState.WAITING) = QueueToken(
        tokenNumber = num, state = state, roomNumber = "Room 1", doctorName = "Dr. Test"
    )

    private fun doctorQueue(
        id: String = "dept-1",
        now: QueueToken? = null,
        upcoming: List<QueueToken> = emptyList()
    ) = DoctorQueue(
        departmentId = id,
        departmentName = "Dept 1",
        doctorName = "Dr. Test",
        roomNumber = "Room 1",
        nowServing = now,
        upcoming = upcoming,
        recentlyCalled = emptyList(),
        updatedAtEpochMillis = System.currentTimeMillis()
    )

    private fun buildCoordinator(): DoctorQueueCoordinator = DoctorQueueCoordinator(
        repository = repository,
        departmentId = "dept-1",
        realtimeEvents = realtimeEvents,
        getCurrentToken = GetCurrentTokenUseCase(repository),
        getUpcoming = GetUpcomingTokensUseCase(repository),
        scope = scope
    )

    @BeforeEach
    fun setUp() {
        repository = mockk(relaxed = true)
        realtimeEvents = MutableSharedFlow()
    }

    @Nested
    inner class InitialLoad {
        @Test
        fun `loads and emits Showing when data available`() = scope.runTest {
            coEvery { repository.getLastUpdateTimestamp() } returns System.currentTimeMillis()
            coEvery { repository.getDoctorQueues() } returns listOf(
                doctorQueue("dept-1", token("A-101"))
            )

            val coordinator = buildCoordinator()
            coordinator.load()
            advanceUntilIdle()

            val state = coordinator.uiState.value
            assertThat(state).isInstanceOf(DoctorQueueUiState.Showing::class.java)
            assertThat((state as DoctorQueueUiState.Showing).nowServing?.tokenNumber)
                .isEqualTo("A-101")
        }

        @Test
        fun `emits Idle when no queue found for department`() = scope.runTest {
            coEvery { repository.getLastUpdateTimestamp() } returns System.currentTimeMillis()
            coEvery { repository.getDoctorQueues() } returns emptyList()

            val coordinator = buildCoordinator()
            coordinator.load()
            advanceUntilIdle()

            assertThat(coordinator.uiState.value).isEqualTo(DoctorQueueUiState.Idle)
        }
    }

    @Nested
    inner class EmergencyOverride {
        @Test
        fun `emits EmergencyOverride when a token is in EMERGENCY state`() = scope.runTest {
            coEvery { repository.getLastUpdateTimestamp() } returns System.currentTimeMillis()
            coEvery { repository.getDoctorQueues() } returns listOf(
                doctorQueue("dept-1", token("E-001", TokenState.EMERGENCY))
            )

            val coordinator = buildCoordinator()
            coordinator.load()
            advanceUntilIdle()

            assertThat(coordinator.uiState.value)
                .isInstanceOf(DoctorQueueUiState.EmergencyOverride::class.java)
        }
    }

    @Nested
    inner class StaleData {
        @Test
        fun `emits Stale when data is older than staleness threshold`() = scope.runTest {
            val staleTime = System.currentTimeMillis() - 10 * 60 * 1_000L
            coEvery { repository.getLastUpdateTimestamp() } returns staleTime
            coEvery { repository.getDoctorQueues() } returns emptyList()

            val coordinator = buildCoordinator()
            coordinator.load()
            advanceUntilIdle()

            val state = coordinator.uiState.value
            assertThat(state).isInstanceOf(DoctorQueueUiState.Stale::class.java)
            assertThat((state as DoctorQueueUiState.Stale).ageMinutes).isAtLeast(9L)
        }
    }

    @Test
    fun `onQueueUpdated emits Showing state`() = scope.runTest {
        val coordinator = buildCoordinator()
        coordinator.onQueueUpdated(listOf(doctorQueue("dept-1", token("A-200"))))
        advanceUntilIdle()

        assertThat(coordinator.uiState.value)
            .isInstanceOf(DoctorQueueUiState.Showing::class.java)
    }

    @Test
    fun `retry transitions to Loading then Showing`() = scope.runTest {
        coEvery { repository.getLastUpdateTimestamp() } returns System.currentTimeMillis()
        coEvery { repository.getDoctorQueues() } returns listOf(
            doctorQueue("dept-1", token("A-300"))
        )

        val coordinator = buildCoordinator()
        coordinator.retry()
        advanceUntilIdle()

        // After retry, state should be Loading then Showing — check final value
        assertThat(coordinator.uiState.value)
            .isInstanceOf(DoctorQueueUiState.Showing::class.java)
    }
}
