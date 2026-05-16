package com.joaograca.chirp.api.dto.ws

import com.joaograca.chirp.domain.type.ChatId

data class ChatParticipantsChangedDto(
    val chatId: ChatId,
)
