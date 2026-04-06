package com.joaograca.chirp.domain.event

import com.joaograca.chirp.domain.type.ChatId
import com.joaograca.chirp.domain.type.UserId

data class ChatParticipantLeftEvent(
    val chatId: ChatId,
    val userId: UserId
)
