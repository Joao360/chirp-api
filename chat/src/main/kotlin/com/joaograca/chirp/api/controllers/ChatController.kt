package com.joaograca.chirp.api.controllers

import com.joaograca.chirp.api.dto.AddParticipantToChatDto
import com.joaograca.chirp.api.dto.ChatDto
import com.joaograca.chirp.api.dto.CreateChatRequest
import com.joaograca.chirp.api.mappers.toChatDto
import com.joaograca.chirp.api.util.requestUserId
import com.joaograca.chirp.domain.type.ChatId
import com.joaograca.chirp.service.ChatService
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/chat")
class ChatController(
    private val chatService: ChatService
) {

    @PostMapping
    fun createChat(
        @Valid @RequestBody body: CreateChatRequest
    ): ChatDto {
        return chatService.createChat(
            creatorId = requestUserId,
            otherUserIds = body.otherUserIds.toSet()
        ).toChatDto()
    }

    @PostMapping("/{chatId}/add")
    fun addChatParticipants(
        @PathVariable chatId: ChatId,
        @Valid @RequestBody body: AddParticipantToChatDto
    ): ChatDto {
        return chatService.addParticipants(
            requestUserId = requestUserId,
            chatId = chatId,
            userIds = body.userIds.toSet()
        ).toChatDto()
    }

    @DeleteMapping("/{chatId}/leave")
    fun leaveChat(
        @PathVariable chatId: ChatId,
    ) {
        chatService.removeParticipantFromChat(
            userId = requestUserId,
            chatId = chatId,
        )
    }
}