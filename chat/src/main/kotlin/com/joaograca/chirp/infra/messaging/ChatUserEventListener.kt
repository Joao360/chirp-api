package com.joaograca.chirp.infra.messaging

import com.joaograca.chirp.domain.events.user.UserEvent
import com.joaograca.chirp.domain.models.ChatParticipant
import com.joaograca.chirp.infra.message_queue.MessageQueues
import com.joaograca.chirp.service.ChatParticipantService
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.stereotype.Component

@Component
class ChatUserEventListener(
    private val chatParticipantService: ChatParticipantService
) {

    @RabbitListener(queues = [MessageQueues.CHAT_USER_EVENTS])
    fun handleUserEvent(event: UserEvent) {
        when (event) {
            is UserEvent.Verified -> {
                chatParticipantService.createChatParticipant(
                    ChatParticipant(
                        userId = event.userId,
                        email = event.email,
                        username = event.username,
                        profilePictureUrl = null
                    )
                )
            }
            else -> Unit
        }
    }
}