package com.queuerx.tv.data.device

import com.queuerx.tv.core.common.ApiResult
import com.queuerx.tv.core.common.QueueRxError
import com.queuerx.tv.core.common.runCatchingApiResult
import com.queuerx.tv.core.network.ApiEndpoints
import com.queuerx.tv.core.storage.PreferenceKeys
import com.queuerx.tv.core.storage.SecureKeyValueStore
import com.queuerx.tv.domain.device.ActivationMethod
import com.queuerx.tv.domain.device.ActivationResponse
import com.queuerx.tv.domain.device.CodeActivationRequest
import com.queuerx.tv.domain.device.DeviceInfo
import com.queuerx.tv.domain.device.DeviceRegistrationRequest
import com.queuerx.tv.domain.device.DeviceRegistrationResponse
import com.queuerx.tv.domain.device.DeviceRepository
import com.queuerx.tv.domain.device.MacActivationRequest
import com.queuerx.tv.domain.device.ProvisionState
import com.queuerx.tv.domain.device.QrActivationRequest
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * [DeviceRepository] implementation backed by Ktor HTTP and [SecureKeyValueStore].
 *
 * This class coordinates all three activation methods (QR, code, MAC) and
 * manages the device registration + context persistence lifecycle.
 */
class DeviceRepositoryImpl(
    private val httpClient: HttpClient,
    private val store: SecureKeyValueStore,
    private val json: Json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
) : DeviceRepository {

    override suspend fun getProvisionState(): ProvisionState {
        val raw = store.get(PreferenceKeys.PROVISION_STATE) ?: return ProvisionState.NOT_PROVISIONED
        return runCatching { ProvisionState.valueOf(raw) }.getOrDefault(ProvisionState.NOT_PROVISIONED)
    }

    override suspend fun saveProvisionState(state: ProvisionState): ApiResult<Unit> =
        runCatchingApiResult { store.set(PreferenceKeys.PROVISION_STATE, state.name) }

    override suspend fun activate(method: ActivationMethod): ApiResult<ActivationResponse> =
        runCatchingApiResult {
            when (method) {
                is ActivationMethod.QrActivation -> {
                    httpClient.post(ApiEndpoints.DEVICE_ACTIVATE_QR) {
                        setBody(QrActivationRequest(token = method.activationToken))
                    }.body()
                }

                is ActivationMethod.CodeActivation -> {
                    val deviceId = getDeviceId()
                    httpClient.post(ApiEndpoints.DEVICE_ACTIVATE_CODE) {
                        setBody(CodeActivationRequest(code = method.activationCode, deviceId = deviceId))
                    }.body()
                }

                is ActivationMethod.MacActivation -> {
                    val deviceId = getDeviceId()
                    httpClient.post(ApiEndpoints.DEVICE_ACTIVATE_MAC) {
                        setBody(MacActivationRequest(mac = method.macAddress, deviceId = deviceId))
                    }.body()
                }
            }
        }

    override suspend fun register(
        device: DeviceInfo,
        activationResponse: ActivationResponse
    ): ApiResult<DeviceRegistrationResponse> = runCatchingApiResult {
        val request = DeviceRegistrationRequest(
            deviceId = device.deviceId,
            deviceName = device.deviceName,
            mac = device.mac,
            model = device.model,
            osVersion = device.osVersion,
            appVersion = device.appVersion,
            deviceType = activationResponse.deviceType,
            hospitalId = activationResponse.hospitalId,
            activationToken = activationResponse.activationToken
        )
        httpClient.post(ApiEndpoints.DEVICE_REGISTER) {
            setBody(request)
        }.body()
    }

    override suspend fun saveDeviceContext(response: DeviceRegistrationResponse): ApiResult<Unit> =
        runCatchingApiResult {
            store.set(PreferenceKeys.DEVICE_ID, response.deviceId)
            store.set(PreferenceKeys.DISPLAY_ID, response.displayId)
            store.set(PreferenceKeys.HOSPITAL_ID, response.hospitalId)
            store.set(PreferenceKeys.TENANT_ID, response.tenantId)
            store.set(PREF_DEVICE_CONTEXT, json.encodeToString(response))
        }

    override suspend fun getDeviceContext(): DeviceRegistrationResponse? {
        val raw = store.get(PREF_DEVICE_CONTEXT) ?: return null
        return runCatching { json.decodeFromString<DeviceRegistrationResponse>(raw) }.getOrNull()
    }

    override suspend fun getMacAddress(): String {
        // On Android this would read NetworkInterface / WifiManager.
        // In the JVM environment we return a placeholder; the Android implementation
        // overrides this with the real MAC address.
        return store.get(PREF_MAC_ADDRESS) ?: UNKNOWN_MAC
    }

    override suspend fun getDeviceId(): String {
        return store.get(PreferenceKeys.DEVICE_ID) ?: generateAndPersistDeviceId()
    }

    private suspend fun generateAndPersistDeviceId(): String {
        val id = java.util.UUID.randomUUID().toString()
        store.set(PreferenceKeys.DEVICE_ID, id)
        return id
    }

    companion object {
        private const val PREF_DEVICE_CONTEXT = "device_context_json"
        private const val PREF_MAC_ADDRESS = "device_mac"
        const val UNKNOWN_MAC = "00:00:00:00:00:00"
    }
}
