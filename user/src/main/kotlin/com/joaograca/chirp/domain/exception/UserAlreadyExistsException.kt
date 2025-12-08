package com.joaograca.chirp.domain.exception

class UserAlreadyExistsException: RuntimeException(
    "User with this username or email already exists"
)