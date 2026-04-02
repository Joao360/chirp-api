package com.joaograca.chirp.domain.exception

import com.joaograca.chirp.domain.type.ChatMessageId

class MessageNotFoundException(id: ChatMessageId): RuntimeException("Message with id $id not found")