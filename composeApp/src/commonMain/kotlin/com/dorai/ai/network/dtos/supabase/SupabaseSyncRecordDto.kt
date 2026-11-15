package com.dorai.ai.network.dtos.supabase

import kotlinx.serialization.Serializable

@Serializable
data class SupabaseSyncRecordDto(
    val id: String? = null,
    val device_id: String,
    val payload_version: Int = 1,
    val iv: String,
    val ciphertext: String,
    val checksum: String,
    val updated_at: Long,
)
