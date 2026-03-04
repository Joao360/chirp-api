package com.joaograca.chirp.service

import com.joaograca.chirp.domain.models.ChatParticipant
import com.joaograca.chirp.domain.type.UserId
import com.joaograca.chirp.infra.database.mappers.toChatParticipant
import com.joaograca.chirp.infra.database.mappers.toChatParticipantEntity
import com.joaograca.chirp.infra.database.repositories.ChatParticipantRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service

@Service
class ChatParticipantService(
    private val chatParticipantRepository: ChatParticipantRepository
) {

    fun createChatParticipant(
        chatParticipant: ChatParticipant
    ) {
        chatParticipantRepository.save(chatParticipant.toChatParticipantEntity())
    }

    fun findChatParticipantById(userId: UserId): ChatParticipant? {
        return chatParticipantRepository.findByIdOrNull(userId)?.toChatParticipant()
    }

    fun findChatParticipantByEmailOrUsername(
        query: String
    ): ChatParticipant? {
        val normalizedQuery = query.trim().lowercase()
        return chatParticipantRepository.findByEmailOrUsername(normalizedQuery)?.toChatParticipant()
    }
}