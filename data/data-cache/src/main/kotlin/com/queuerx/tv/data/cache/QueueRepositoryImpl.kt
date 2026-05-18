package com.queuerx.tv.data.cache

import com.queuerx.tv.core.common.ApiResult
import com.queuerx.tv.core.common.runCatchingApiResult
import com.queuerx.tv.domain.queue.DoctorQueue
import com.queuerx.tv.domain.queue.PharmacyQueue
import com.queuerx.tv.domain.queue.QueueRepository
import com.queuerx.tv.domain.queue.QueueToken
import com.queuerx.tv.domain.queue.TokenState
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * [QueueRepository] implementation backed by an in-memory cache + JSON file persistence.
 *
 * Design (spec Part 4 — Offline survival):
 * - All reads are served from in-memory maps (sub-millisecond latency).
 * - Every mutation atomically updates memory AND a JSON snapshot file.
 * - On startup, the snapshot is loaded into memory so the display is never blank.
 * - A Mutex ensures no torn writes in concurrent coroutine environments.
 *
 * On Android this would be backed by Room DAOs injected via DI.
 * This class provides equivalent semantics in the pure JVM environment.
 */
class QueueRepositoryImpl(
    private val snapshotDir: File,
    private val json: Json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
) : QueueRepository {

    private val mutex = Mutex()

    // In-memory state
    private val doctorQueues = mutableMapOf<String, DoctorQueue>()   // key: departmentId
    private val pharmacyQueue = mutableMapOf<String, PharmacyQueue>() // single entry "pharmacy"
    private var lastUpdateMs: Long? = null

    init {
        loadFromDisk()
    }

    // ── QueueRepository ───────────────────────────────────────────────────────

    override suspend fun getDoctorQueues(): List<DoctorQueue> = mutex.withLock {
        doctorQueues.values.toList()
    }

    override suspend fun getPharmacyQueue(): PharmacyQueue? = mutex.withLock {
        pharmacyQueue["pharmacy"]
    }

    override suspend fun getToken(tokenNumber: String): QueueToken? = mutex.withLock {
        doctorQueues.values.asSequence()
            .flatMap { q -> (listOfNotNull(q.nowServing) + q.upcoming + q.recentlyCalled).asSequence() }
            .firstOrNull { it.tokenNumber == tokenNumber }
            ?: pharmacyQueue["pharmacy"]?.let { q ->
                (q.preparing + q.ready + q.counters.mapNotNull { it.currentToken })
                    .firstOrNull { it.tokenNumber == tokenNumber }
            }
    }

    override suspend fun saveDoctorQueues(queues: List<DoctorQueue>): Unit = mutex.withLock {
        queues.forEach { q -> doctorQueues[q.departmentId] = q }
        lastUpdateMs = System.currentTimeMillis()
        persistDoctorQueues()
    }

    override suspend fun savePharmacyQueue(queue: PharmacyQueue): Unit = mutex.withLock {
        pharmacyQueue["pharmacy"] = queue
        lastUpdateMs = System.currentTimeMillis()
        persistPharmacyQueue()
    }

    override suspend fun updateTokenState(tokenNumber: String, newState: TokenState): Unit =
        mutex.withLock {
            val updatedNow = System.currentTimeMillis()

            // Update in doctor queues
            val updatedQueues = doctorQueues.values.map { queue ->
                queue.copy(
                    nowServing = queue.nowServing?.updateIfMatch(tokenNumber, newState, updatedNow),
                    upcoming = queue.upcoming.map { it.updateIfMatch(tokenNumber, newState, updatedNow) },
                    recentlyCalled = queue.recentlyCalled.map { it.updateIfMatch(tokenNumber, newState, updatedNow) }
                )
            }
            updatedQueues.forEach { q -> doctorQueues[q.departmentId] = q }

            // Update in pharmacy queue
            pharmacyQueue["pharmacy"]?.let { pq ->
                pharmacyQueue["pharmacy"] = pq.copy(
                    preparing = pq.preparing.map { it.updateIfMatch(tokenNumber, newState, updatedNow) },
                    ready = pq.ready.map { it.updateIfMatch(tokenNumber, newState, updatedNow) }
                )
            }

            lastUpdateMs = updatedNow
            persistDoctorQueues()
            persistPharmacyQueue()
        }

    override suspend fun clearAll(): Unit = mutex.withLock {
        doctorQueues.clear()
        pharmacyQueue.clear()
        lastUpdateMs = null
        snapshotDir.listFiles()?.forEach { it.delete() }
    }

    override suspend fun getLastUpdateTimestamp(): Long? = mutex.withLock { lastUpdateMs }

    // ── Persistence ───────────────────────────────────────────────────────────

    private fun persistDoctorQueues() = runCatching {
        val file = File(snapshotDir, DOCTOR_QUEUES_FILE)
        file.writeText(json.encodeToString(doctorQueues.values.toList()))
    }

    private fun persistPharmacyQueue() = runCatching {
        val file = File(snapshotDir, PHARMACY_QUEUE_FILE)
        pharmacyQueue["pharmacy"]?.let { file.writeText(json.encodeToString(it)) }
    }

    private fun loadFromDisk() = runCatching {
        snapshotDir.mkdirs()

        val doctorFile = File(snapshotDir, DOCTOR_QUEUES_FILE)
        if (doctorFile.exists()) {
            val queues = json.decodeFromString<List<DoctorQueue>>(doctorFile.readText())
            queues.forEach { q -> doctorQueues[q.departmentId] = q }
        }

        val pharmacyFile = File(snapshotDir, PHARMACY_QUEUE_FILE)
        if (pharmacyFile.exists()) {
            val pq = json.decodeFromString<PharmacyQueue>(pharmacyFile.readText())
            pharmacyQueue["pharmacy"] = pq
        }
    }

    private fun QueueToken.updateIfMatch(
        tokenNumber: String,
        newState: TokenState,
        updatedAtMs: Long
    ): QueueToken = if (this.tokenNumber == tokenNumber) copy(state = newState, updatedAtEpochMillis = updatedAtMs) else this

    companion object {
        private const val DOCTOR_QUEUES_FILE = "doctor_queues.json"
        private const val PHARMACY_QUEUE_FILE = "pharmacy_queue.json"
    }
}
