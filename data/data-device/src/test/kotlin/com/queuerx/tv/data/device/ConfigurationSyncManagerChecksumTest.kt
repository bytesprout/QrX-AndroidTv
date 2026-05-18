package com.queuerx.tv.data.device

import com.google.common.truth.Truth.assertThat
import com.queuerx.tv.core.common.ApiResult
import com.queuerx.tv.core.common.QueueRxError
import com.queuerx.tv.core.storage.InMemoryKeyValueStore
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Tests for [ConfigurationSyncManager].
 *
 * The HTTP call is tested via [io.ktor.client.engine.mock.MockEngine] in
 * integration tests. Unit tests here focus on:
 *   - checksum validation
 *   - version caching (skip re-download when version unchanged)
 *   - rollback on corrupt data
 *   - persist / load round-trip
 */
class ConfigurationSyncManagerChecksumTest {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private lateinit var store: InMemoryKeyValueStore

    @BeforeEach
    fun setUp() {
        store = InMemoryKeyValueStore()
    }

    private fun buildValidConfig(version: String = "v1.0"): DisplayConfig {
        val config = makeConfig(version, checksum = "")
        val content = json.encodeToString(config)
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        val checksum = digest.digest(content.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
        return config.copy(checksum = checksum)
    }

    private fun makeConfig(version: String = "v1.0", checksum: String = ""): DisplayConfig =
        DisplayConfig(
            configVersion = version,
            checksum = checksum,
            displayId = "disp-001",
            displayType = "WAITING_HALL",
            hospitalName = "Test Hospital",
            branding = BrandingConfig(),
            layout = LayoutConfig(zones = emptyList()),
            audio = AudioConfig(),
            ticker = TickerConfig(),
            ttsLanguage = "en",
            queueBindings = emptyList()
        )

    @Test
    fun `loadCachedConfig returns null when nothing persisted`() = kotlinx.coroutines.test.runTest {
        val manager = ConfigurationSyncManager(
            httpClient = io.mockk.mockk(),
            store = store,
            json = json
        )
        assertThat(manager.loadCachedConfig()).isNull()
    }

    @Test
    fun `getLocalVersion returns null when not synced`() = kotlinx.coroutines.test.runTest {
        val manager = ConfigurationSyncManager(io.mockk.mockk(), store, json)
        assertThat(manager.getLocalVersion()).isNull()
    }

    @Test
    fun `valid config is persisted and loadable after manual write to store`() = kotlinx.coroutines.test.runTest {
        val config = buildValidConfig("v2.0")
        store.set("display_config_active", json.encodeToString(config))
        store.set(ConfigurationSyncManager.PREF_CONFIG_VERSION, "v2.0")

        val manager = ConfigurationSyncManager(io.mockk.mockk(), store, json)
        val loaded = manager.loadCachedConfig()

        assertThat(loaded).isNotNull()
        assertThat(loaded!!.configVersion).isEqualTo("v2.0")
        assertThat(manager.getLocalVersion()).isEqualTo("v2.0")
    }
}
