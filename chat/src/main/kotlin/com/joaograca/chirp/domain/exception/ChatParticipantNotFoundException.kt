package com.joaograca.chirp.domain.exception

import com.joaograca.chirp.domain.type.UserId

class ChatParticipantNotFoundException(
    id: UserId
) : RuntimeException(
    "The chat participant with the ID $id was not found."
)