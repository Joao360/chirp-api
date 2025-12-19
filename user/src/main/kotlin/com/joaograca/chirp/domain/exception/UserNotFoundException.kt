package com.joaograca.chirp.domain.exception

class UserNotFoundException: RuntimeException(
    "The user was not found"
)