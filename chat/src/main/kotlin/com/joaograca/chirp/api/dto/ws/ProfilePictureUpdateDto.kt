package com.joaograca.chirp.api.dto.ws

import com.joaograca.chirp.domain.type.UserId

data class ProfilePictureUpdateDto(
    val userId: UserId,
    val newUrl: String?,
)
