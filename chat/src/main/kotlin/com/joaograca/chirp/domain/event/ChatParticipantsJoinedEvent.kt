package com.joaograca.chirp.domain.event

import com.joaograca.chirp.domain.type.ChatId
import com.joaograca.chirp.domain.type.UserId

data class ChatParticipantsJoinedEvent(
    val chatId: ChatId,
    val userIds: Set<UserId>,
)
