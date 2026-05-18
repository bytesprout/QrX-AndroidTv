package com.queuerx.tv.core.storage

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class InMemoryKeyValueStoreTest {

    private lateinit var store: InMemoryKeyValueStore

    @BeforeEach
    fun setUp() {
        store = InMemoryKeyValueStore()
    }

    @Test
    fun `get returns null for unknown key`() = runTest {
        assertThat(store.get("unknown")).isNull()
    }

    @Test
    fun `set then get returns stored value`() = runTest {
        store.set("key1", "value1")
        assertThat(store.get("key1")).isEqualTo("value1")
    }

    @Test
    fun `remove clears value`() = runTest {
        store.set("key1", "value1")
        store.remove("key1")
        assertThat(store.get("key1")).isNull()
    }

    @Test
    fun `contains returns false for absent key`() = runTest {
        assertThat(store.contains("absent")).isFalse()
    }

    @Test
    fun `contains returns true after set`() = runTest {
        store.set("k", "v")
        assertThat(store.contains("k")).isTrue()
    }

    @Test
    fun `clearAll removes all keys`() = runTest {
        store.set("a", "1")
        store.set("b", "2")
        store.clearAll()
        assertThat(store.get("a")).isNull()
        assertThat(store.get("b")).isNull()
    }

    @Test
    fun `observe emits null initially for unset key`() = runTest {
        store.observe("unset").test {
            assertThat(awaitItem()).isNull()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `observe emits new value when set`() = runTest {
        store.observe("key").test {
            assertThat(awaitItem()).isNull()
            store.set("key", "hello")
            assertThat(awaitItem()).isEqualTo("hello")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `observe emits null after remove`() = runTest {
        store.set("key", "initial")
        store.observe("key").test {
            assertThat(awaitItem()).isEqualTo("initial")
            store.remove("key")
            assertThat(awaitItem()).isNull()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `overwrite emits updated value`() = runTest {
        store.observe("key").test {
            awaitItem() // null
            store.set("key", "first")
            assertThat(awaitItem()).isEqualTo("first")
            store.set("key", "second")
            assertThat(awaitItem()).isEqualTo("second")
            cancelAndIgnoreRemainingEvents()
        }
    }
}
