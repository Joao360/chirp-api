package com.joaograca.chirp.domain.models

import com.joaograca.chirp.domain.type.UserId

data class ChatParticipant(
    val userId: UserId,
    val username: String,
    val email: String,
    val profilePictureUrl: String?,
)
