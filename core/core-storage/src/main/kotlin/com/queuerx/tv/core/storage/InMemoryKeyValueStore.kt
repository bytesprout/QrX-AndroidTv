package com.queuerx.tv.core.storage

import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * In-memory [SecureKeyValueStore] for use in JVM environments and tests.
 *
 * Thread-safe via [ConcurrentHashMap]. Observers are backed by [MutableStateFlow]
 * per key so all existing [observe] collectors receive updates on [set] / [remove].
 */
class InMemoryKeyValueStore : SecureKeyValueStore {

    private val store = ConcurrentHashMap<String, String>()
    private val flows = ConcurrentHashMap<String, MutableStateFlow<String?>>()

    private fun flowFor(key: String): MutableStateFlow<String?> =
        flows.getOrPut(key) { MutableStateFlow(store[key]) }

    override fun observe(key: String): Flow<String?> = flowFor(key)

    override suspend fun get(key: String): String? = store[key]

    override suspend fun set(key: String, value: String) {
        store[key] = value
        flowFor(key).value = value
    }

    override suspend fun remove(key: String) {
        store.remove(key)
        flows[key]?.value = null
    }

    override suspend fun clearAll() {
        val keys = store.keys().toList()
        store.clear()
        keys.forEach { key -> flows[key]?.value = null }
    }

    override suspend fun contains(key: String): Boolean = store.containsKey(key)
}
