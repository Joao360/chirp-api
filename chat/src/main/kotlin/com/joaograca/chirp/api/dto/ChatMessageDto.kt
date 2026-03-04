package com.joaograca.chirp.api.dto

import com.joaograca.chirp.domain.type.ChatId
import com.joaograca.chirp.domain.type.ChatMessageId
import com.joaograca.chirp.domain.type.UserId
import java.time.Instant

data class ChatMessageDto(
    val id: ChatMessageId,
    val chatId: ChatId,
    val content: String,
    val createdAt: Instant,
    val senderId: UserId,
)
