package com.joaograca.chirp.api.dto.ws

import com.joaograca.chirp.domain.type.ChatId
import com.joaograca.chirp.domain.type.ChatMessageId

data class DeleteMessageDto(
    val chatId: ChatId,
    val messageId: ChatMessageId,
)
