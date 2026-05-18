package com.queuerx.tv.core.database

import kotlinx.serialization.Serializable

@Serializable data class DeviceEntity(val id: String, val displayType: String, val hospitalId: String)
@Serializable data class DisplayConfigEntity(val displayId: String, val displayType: String, val branchId: String)
@Serializable data class QueueEntity(val id: String, val department: String, val waitingCount: Int)
@Serializable data class TokenEntity(val id: String, val state: String, val roomNumber: String)
@Serializable data class DoctorQueueEntity(val id: String, val doctorName: String, val roomNumber: String)
@Serializable data class PharmacyQueueEntity(val id: String, val preparing: Int, val ready: Int)
@Serializable data class MediaEntity(val id: String, val url: String, val type: String)
@Serializable data class PlaylistEntity(val id: String, val name: String)
@Serializable data class TickerEntity(val id: String, val message: String)
@Serializable data class EmergencyEntity(val id: String, val message: String, val active: Boolean)
@Serializable data class RealtimeEventEntity(val eventId: String, val type: String, val payload: String, val processed: Boolean)
@Serializable data class HeartbeatEntity(val id: String, val status: String, val timestampEpochMillis: Long)
@Serializable data class DiagnosticsEntity(val id: String, val key: String, val value: String)
