package com.joaograca.chirp.domain.models

import java.time.Instant

data class ProfilePictureCredentials(
    val uploadUrl: String,
    val publicUrl: String,
    val headers: Map<String, String>,
    val expiresAt: Instant
)
