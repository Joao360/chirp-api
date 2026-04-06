package com.joaograca.chirp.service

import com.joaograca.chirp.domain.exception.ChatNotFoundException
import com.joaograca.chirp.domain.exception.ChatParticipantNotFoundException
import com.joaograca.chirp.domain.exception.MessageNotFoundException
import com.joaograca.chirp.domain.exceptions.ForbiddenException
import com.joaograca.chirp.domain.models.ChatMessage
import com.joaograca.chirp.domain.type.ChatId
import com.joaograca.chirp.domain.type.ChatMessageId
import com.joaograca.chirp.domain.type.UserId
import com.joaograca.chirp.infra.database.entities.ChatMessageEntity
import com.joaograca.chirp.infra.database.mappers.toChatMessage
import com.joaograca.chirp.infra.database.repositories.ChatMessageRepository
import com.joaograca.chirp.infra.database.repositories.ChatParticipantRepository
import com.joaograca.chirp.infra.database.repositories.ChatRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ChatMessageService(
    private val chatRepository: ChatRepository,
    private val chatMessageRepository: ChatMessageRepository,
    private val chatParticipantRepository: ChatParticipantRepository
) {

    @Transactional
    fun sendMessage(
        chatId: ChatId,
        senderId: UserId,
        content: String,
        messageId: ChatMessageId? = null
    ): ChatMessage {
        val chat = chatRepository.findChatById(chatId, senderId)
            ?: throw ChatNotFoundException()
        val sender = chatParticipantRepository.findByIdOrNull(senderId)
            ?: throw ChatParticipantNotFoundException(senderId)

        val savedMessage = chatMessageRepository.save(
            ChatMessageEntity(
                id = messageId,
                content = content.trim(),
                chatId = chatId,
                chat = chat,
                sender = sender,
            )
        )

        return savedMessage.toChatMessage()
    }

    @Transactional
    fun deleteMessage(
        messageId: ChatMessageId,
        requestUserId: UserId
    ) {
        val message = chatMessageRepository.findByIdOrNull(messageId)
            ?: throw MessageNotFoundException(messageId)

        if (message.sender.userId != requestUserId) {
            throw ForbiddenException()
        }

        chatMessageRepository.delete(message)
    }
}