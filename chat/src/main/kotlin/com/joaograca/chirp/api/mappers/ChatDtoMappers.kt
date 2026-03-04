package com.joaograca.chirp.api.mappers

import com.joaograca.chirp.api.dto.ChatDto
import com.joaograca.chirp.api.dto.ChatMessageDto
import com.joaograca.chirp.api.dto.ChatParticipantDto
import com.joaograca.chirp.domain.models.Chat
import com.joaograca.chirp.domain.models.ChatMessage
import com.joaograca.chirp.domain.models.ChatParticipant

fun Chat.toChatDto(): ChatDto {
    return ChatDto(
        id = this.id,
        participants = this.participants.map { it.toChatParticipantDto() },
        lastActivityAt = this.lastActivityAt,
        lastMessage = this.lastMessage?.toChatMessageDto(),
        creator = this.creator.toChatParticipantDto()
    )
}

fun ChatMessage.toChatMessageDto(): ChatMessageDto {
    return ChatMessageDto(
        id = this.id,
        chatId = this.chatId,
        content = this.content,
        createdAt = this.createdAt,
        senderId = this.sender.userId
    )
}

fun ChatParticipant.toChatParticipantDto(): ChatParticipantDto {
    return ChatParticipantDto(
        userId = this.userId,
        username = this.username,
        email = this.email,
        profilePictureUrl = this.profilePictureUrl
    )
}