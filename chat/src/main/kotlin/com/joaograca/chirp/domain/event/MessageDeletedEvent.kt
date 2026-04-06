package com.joaograca.chirp.domain.event

import com.joaograca.chirp.domain.type.ChatId
import com.joaograca.chirp.domain.type.ChatMessageId

data class MessageDeletedEvent(
    val chatId: ChatId,
    val messageId: ChatMessageId,
)
