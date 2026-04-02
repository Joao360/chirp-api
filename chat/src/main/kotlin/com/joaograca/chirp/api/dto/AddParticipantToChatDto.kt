package com.joaograca.chirp.api.dto

import com.joaograca.chirp.domain.type.UserId
import jakarta.validation.constraints.Size

data class AddParticipantToChatDto(
    @field:Size(min=1)
    val userIds: List<UserId>,
)
