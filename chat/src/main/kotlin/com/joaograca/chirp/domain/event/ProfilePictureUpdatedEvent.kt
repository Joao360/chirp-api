package com.joaograca.chirp.domain.event

import com.joaograca.chirp.domain.type.UserId

data class ProfilePictureUpdatedEvent(
    val userId: UserId,
    val newUrl: String?,
)