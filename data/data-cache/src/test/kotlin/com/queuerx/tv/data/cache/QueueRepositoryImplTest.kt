package com.queuerx.tv.data.cache

import com.google.common.truth.Truth.assertThat
import com.queuerx.tv.domain.queue.DoctorQueue
import com.queuerx.tv.domain.queue.PharmacyCounter
import com.queuerx.tv.domain.queue.PharmacyQueue
import com.queuerx.tv.domain.queue.QueueToken
import com.queuerx.tv.domain.queue.TokenState
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class QueueRepositoryImplTest {

    @TempDir
    lateinit var tempDir: File

    private lateinit var repo: QueueRepositoryImpl

    private fun token(num: String, state: TokenState = TokenState.WAITING) = QueueToken(
        tokenNumber = num,
        state = state,
        roomNumber = "Room 1",
        doctorName = "Dr. Smith"
    )

    private fun doctorQueue(id: String, now: QueueToken? = null, upcoming: List<QueueToken> = emptyList()) =
        DoctorQueue(
            departmentId = id,
            departmentName = "Dept $id",
            doctorName = "Dr. Smith",
            roomNumber = "Room 1",
            nowServing = now,
            upcoming = upcoming,
            recentlyCalled = emptyList()
        )

    @BeforeEach
    fun setUp() {
        repo = QueueRepositoryImpl(snapshotDir = tempDir)
    }

    // ── Basic CRUD ────────────────────────────────────────────────────────────

    @Nested
    inner class BasicCrud {
        @Test
        fun `getDoctorQueues returns empty list initially`() = runTest {
            assertThat(repo.getDoctorQueues()).isEmpty()
        }

        @Test
        fun `saveDoctorQueues persists and getDoctorQueues retrieves them`() = runTest {
            val queues = listOf(
                doctorQueue("dept-1", token("A-101")),
                doctorQueue("dept-2", token("B-201"))
            )
            repo.saveDoctorQueues(queues)
            val result = repo.getDoctorQueues()
            assertThat(result).hasSize(2)
            assertThat(result.map { it.departmentId }).containsExactly("dept-1", "dept-2")
        }

        @Test
        fun `getPharmacyQueue returns null initially`() = runTest {
            assertThat(repo.getPharmacyQueue()).isNull()
        }

        @Test
        fun `savePharmacyQueue and retrieve`() = runTest {
            val pq = PharmacyQueue(
                preparing = listOf(token("P-301")),
                ready = listOf(token("P-201", TokenState.READY)),
                counters = listOf(PharmacyCounter("c1", "Counter 1", null))
            )
            repo.savePharmacyQueue(pq)
            val loaded = repo.getPharmacyQueue()
            assertThat(loaded).isNotNull()
            assertThat(loaded!!.preparing).hasSize(1)
        }
    }

    // ── Token lookup ──────────────────────────────────────────────────────────

    @Nested
    inner class TokenLookup {
        @Test
        fun `getToken finds token in doctor queue nowServing`() = runTest {
            repo.saveDoctorQueues(listOf(doctorQueue("d1", token("A-001"))))
            val found = repo.getToken("A-001")
            assertThat(found).isNotNull()
            assertThat(found!!.tokenNumber).isEqualTo("A-001")
        }

        @Test
        fun `getToken finds token in pharmacy preparing list`() = runTest {
            repo.savePharmacyQueue(
                PharmacyQueue(
                    preparing = listOf(token("P-100")),
                    ready = emptyList(),
                    counters = emptyList()
                )
            )
            val found = repo.getToken("P-100")
            assertThat(found).isNotNull()
        }

        @Test
        fun `getToken returns null for unknown token`() = runTest {
            assertThat(repo.getToken("UNKNOWN")).isNull()
        }
    }

    // ── State updates ─────────────────────────────────────────────────────────

    @Nested
    inner class StateUpdates {
        @Test
        fun `updateTokenState changes token state in nowServing`() = runTest {
            repo.saveDoctorQueues(listOf(doctorQueue("d1", token("A-001", TokenState.WAITING))))
            repo.updateTokenState("A-001", TokenState.CALLED)
            val updated = repo.getToken("A-001")
            assertThat(updated?.state).isEqualTo(TokenState.CALLED)
        }

        @Test
        fun `updateTokenState changes token state in upcoming list`() = runTest {
            val queue = doctorQueue("d1", upcoming = listOf(token("A-002", TokenState.WAITING)))
            repo.saveDoctorQueues(listOf(queue))
            repo.updateTokenState("A-002", TokenState.CALLED)
            val updated = repo.getToken("A-002")
            assertThat(updated?.state).isEqualTo(TokenState.CALLED)
        }

        @Test
        fun `updateTokenState is no-op for unknown token`() = runTest {
            repo.saveDoctorQueues(listOf(doctorQueue("d1", token("A-001"))))
            repo.updateTokenState("UNKNOWN", TokenState.ACTIVE) // should not throw
            assertThat(repo.getToken("A-001")?.state).isEqualTo(TokenState.WAITING)
        }
    }

    // ── Persistence ───────────────────────────────────────────────────────────

    @Nested
    inner class Persistence {
        @Test
        fun `data survives repository reconstruction (disk persistence)`() = runTest {
            val queues = listOf(doctorQueue("dept-persist", token("X-999")))
            repo.saveDoctorQueues(queues)

            // Create a fresh repository from the same directory
            val newRepo = QueueRepositoryImpl(snapshotDir = tempDir)
            val loaded = newRepo.getDoctorQueues()

            assertThat(loaded).hasSize(1)
            assertThat(loaded.first().departmentId).isEqualTo("dept-persist")
        }

        @Test
        fun `clearAll removes all data from memory and disk`() = runTest {
            repo.saveDoctorQueues(listOf(doctorQueue("d1")))
            repo.clearAll()
            assertThat(repo.getDoctorQueues()).isEmpty()
            assertThat(repo.getPharmacyQueue()).isNull()

            // Verify disk is also cleared
            val newRepo = QueueRepositoryImpl(snapshotDir = tempDir)
            assertThat(newRepo.getDoctorQueues()).isEmpty()
        }
    }

    // ── Timestamp ─────────────────────────────────────────────────────────────

    @Test
    fun `getLastUpdateTimestamp is null before any save`() = runTest {
        assertThat(repo.getLastUpdateTimestamp()).isNull()
    }

    @Test
    fun `getLastUpdateTimestamp is set after save`() = runTest {
        val before = System.currentTimeMillis()
        repo.saveDoctorQueues(listOf(doctorQueue("d1")))
        val ts = repo.getLastUpdateTimestamp()
        assertThat(ts).isNotNull()
        assertThat(ts!!).isAtLeast(before)
    }
}
