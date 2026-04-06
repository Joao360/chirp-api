package com.joaograca.chirp.api.controllers

import com.joaograca.chirp.api.dto.AddParticipantToChatDto
import com.joaograca.chirp.api.dto.ChatDto
import com.joaograca.chirp.api.dto.ChatMessageDto
import com.joaograca.chirp.api.dto.CreateChatRequest
import com.joaograca.chirp.api.mappers.toChatDto
import com.joaograca.chirp.api.util.requestUserId
import com.joaograca.chirp.domain.type.ChatId
import com.joaograca.chirp.service.ChatService
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.*
import java.time.Instant

@RestController
@RequestMapping("/api/chat")
class ChatController(
    private val chatService: ChatService
) {

    @GetMapping("/{chatId}/messages")
    fun getMessagesForChat(
        @PathVariable("chatId") chatId: ChatId,
        @RequestParam("before", required = false) before: Instant? = null,
        @RequestParam("pageSize", required = false) pageSize: Int = DEFAULT_PAGE_SIZE,
    ): List<ChatMessageDto> {
        return chatService.getChatMessages(
            chatId = chatId,
            before = before,
            pageSize = pageSize
        )
    }

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

    companion object {
        private const val DEFAULT_PAGE_SIZE = 20
    }
}