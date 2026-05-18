package com.queuerx.tv.core.storage

import kotlinx.coroutines.flow.Flow

/**
 * Platform-agnostic encrypted key-value store interface.
 *
 * On Android this is backed by EncryptedDataStore (AES-256-GCM).
 * In tests / JVM it is backed by [InMemoryKeyValueStore].
 *
 * All keys are defined in [PreferenceKeys]. String values are used
 * so that callers can serialise whatever domain types they need.
 */
interface SecureKeyValueStore {
    /** Observe a string preference value. Emits null when not set. */
    fun observe(key: String): Flow<String?>

    /** Read a string preference value synchronously. Returns null when not set. */
    suspend fun get(key: String): String?

    /** Persist a string value. Throws [StorageException] on write failure. */
    suspend fun set(key: String, value: String)

    /** Remove a key. No-op if absent. */
    suspend fun remove(key: String)

    /** Remove all stored keys (factory reset / clear app data). */
    suspend fun clearAll()

    /** Returns true if the key has a stored value. */
    suspend fun contains(key: String): Boolean
}

class StorageException(message: String, cause: Throwable? = null) : Exception(message, cause)
