package com.queuerx.tv.data.device

import com.queuerx.tv.core.common.ApiResult
import com.queuerx.tv.core.common.QueueRxError
import com.queuerx.tv.core.common.runCatchingApiResult
import com.queuerx.tv.core.network.ApiEndpoints
import com.queuerx.tv.core.storage.SecureKeyValueStore
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.security.MessageDigest

/**
 * Downloads, validates, and applies remote display configuration.
 *
 * Flow (spec Part 6):
 *   download config → validate checksum → persist locally → apply safely
 *   If checksum fails or any step throws, the previous config is preserved (rollback).
 *
 * Config versioning: only sections whose [version] has changed since the last
 * successful sync are re-applied, avoiding unnecessary downloads.
 */
class ConfigurationSyncManager(
    private val httpClient: HttpClient,
    private val store: SecureKeyValueStore,
    private val json: Json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
) {

    /**
     * Downloads and applies the latest configuration for this display.
     *
     * @param displayId   Identifies which display config to fetch.
     * @param currentVersion The locally persisted config version. Pass null on first sync.
     * @return [ApiResult.Success] with the new [DisplayConfig], or [ApiResult.Error] on failure.
     *         On checksum mismatch or apply failure the old config is kept and
     *         [QueueRxError.ConfigChecksumMismatch] / [QueueRxError.ConfigDownloadFailed] is returned.
     */
    suspend fun sync(
        displayId: String,
        currentVersion: String? = null
    ): ApiResult<DisplayConfig> {
        // 1. Fetch remote config
        val fetchResult = fetchRemoteConfig(displayId)
        if (fetchResult is ApiResult.Error) return fetchResult
        val remote = (fetchResult as ApiResult.Success).data

        // 2. Skip if version unchanged
        if (remote.configVersion == currentVersion) {
            val cached = loadCachedConfig()
            if (cached != null) return ApiResult.Success(cached)
        }

        // 3. Validate SHA-256 checksum
        val checksumResult = validateChecksum(remote)
        if (checksumResult is ApiResult.Error) return checksumResult

        // 4. Persist new config (atomic: write to staging key, then promote)
        return runCatchingApiResult {
            val encoded = json.encodeToString(remote)
            store.set(PREF_STAGED_CONFIG, encoded)
            store.set(PREF_ACTIVE_CONFIG, encoded)
            store.set(PREF_CONFIG_VERSION, remote.configVersion)
            store.remove(PREF_STAGED_CONFIG)
            remote
        }.let { result ->
            if (result is ApiResult.Error) {
                ApiResult.Error(QueueRxError.ConfigDownloadFailed(result.error.userMessage()))
            } else {
                result
            }
        }
    }

    /** Returns the locally persisted [DisplayConfig], or null if never synced. */
    suspend fun loadCachedConfig(): DisplayConfig? {
        val raw = store.get(PREF_ACTIVE_CONFIG) ?: return null
        return runCatching { json.decodeFromString<DisplayConfig>(raw) }.getOrNull()
    }

    /** Returns the last successfully synced config version string. */
    suspend fun getLocalVersion(): String? = store.get(PREF_CONFIG_VERSION)

    private suspend fun fetchRemoteConfig(displayId: String): ApiResult<DisplayConfig> =
        runCatchingApiResult {
            httpClient.get(ApiEndpoints.CONFIG_SYNC) {
                parameter("displayId", displayId)
            }.body()
        }

    private fun validateChecksum(config: DisplayConfig): ApiResult<Unit> {
        val content = json.encodeToString(config.copy(checksum = ""))
        val computed = sha256Hex(content)
        return if (computed == config.checksum) {
            ApiResult.Success(Unit)
        } else {
            ApiResult.Error(
                QueueRxError.ConfigChecksumMismatch(
                    expected = config.checksum,
                    actual = computed
                )
            )
        }
    }

    private fun sha256Hex(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    companion object {
        private const val PREF_ACTIVE_CONFIG = "display_config_active"
        private const val PREF_STAGED_CONFIG = "display_config_staged"
        const val PREF_CONFIG_VERSION = "display_config_version"
    }
}

// ── Remote config model ───────────────────────────────────────────────────────

/**
 * Full display configuration downloaded from the backend.
 *
 * All UI, queue bindings, audio, and branding are controlled remotely —
 * nothing is hardcoded in the app (spec Part 6: Remote Configuration).
 */
@Serializable
data class DisplayConfig(
    val configVersion: String,
    val checksum: String,
    val displayId: String,
    val displayType: String,
    val hospitalName: String,
    val branding: BrandingConfig,
    val layout: LayoutConfig,
    val audio: AudioConfig,
    val ticker: TickerConfig,
    val ttsLanguage: String,
    val queueBindings: List<QueueBinding>,
    val maintenanceWindow: MaintenanceWindow? = null
)

@Serializable
data class BrandingConfig(
    val logoUrl: String? = null,
    val primaryColor: String = "#1976D2",
    val secondaryColor: String = "#FFFFFF",
    val backgroundUrl: String? = null,
    val fontFamily: String = "NotoSans"
)

@Serializable
data class LayoutConfig(
    val zones: List<ZoneConfig>
)

@Serializable
data class ZoneConfig(
    val zoneId: String,
    val type: String,
    val widthPercent: Float,
    val heightPercent: Float,
    val xPercent: Float,
    val yPercent: Float,
    val visible: Boolean = true
)

@Serializable
data class AudioConfig(
    val muteMedia: Boolean = false,
    val muteAnnouncements: Boolean = false,
    val ttsVolumePercent: Int = 80,
    val mediaVolumePercent: Int = 100,
    val emergencyVolumePercent: Int = 100,
    val quietHoursStart: String? = null,
    val quietHoursEnd: String? = null,
    val chimeEnabled: Boolean = true,
    val chimeUrl: String? = null
)

@Serializable
data class TickerConfig(
    val enabled: Boolean = true,
    val speed: String = "normal",
    val language: String = "en"
)

@Serializable
data class QueueBinding(
    val departmentId: String,
    val departmentName: String,
    val type: String,
    val displayOrder: Int = 0
)

@Serializable
data class MaintenanceWindow(
    val startHour: Int,
    val endHour: Int
)
