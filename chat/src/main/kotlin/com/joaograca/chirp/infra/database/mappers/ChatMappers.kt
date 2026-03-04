package com.joaograca.chirp.infra.database.mappers

import com.joaograca.chirp.domain.models.Chat
import com.joaograca.chirp.domain.models.ChatMessage
import com.joaograca.chirp.domain.models.ChatParticipant
import com.joaograca.chirp.infra.database.entities.ChatEntity
import com.joaograca.chirp.infra.database.entities.ChatParticipantEntity

fun ChatEntity.toChat(lastMessage: ChatMessage? = null): Chat {
    return Chat(
        id = this.id!!,
        participants = participants.map {
            it.toChatParticipant()
        }.toSet(),
        lastMessage = lastMessage,
        creator = creator.toChatParticipant(),
        lastActivityAt = lastMessage?.createdAt ?: this.createdAt,
        createdAt = this.createdAt
    )
}

fun ChatParticipantEntity.toChatParticipant(): ChatParticipant {
    return ChatParticipant(
        userId = this.userId,
        username = this.username,
        email = this.email,
        profilePictureUrl = this.profilePictureUrl
    )
}

fun ChatParticipant.toChatParticipantEntity(): ChatParticipantEntity {
    return ChatParticipantEntity(
        userId = this.userId,
        username = this.username,
        email = this.email,
        profilePictureUrl = this.profilePictureUrl
    )
}