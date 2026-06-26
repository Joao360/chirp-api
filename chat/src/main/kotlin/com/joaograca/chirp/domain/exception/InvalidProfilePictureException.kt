package com.joaograca.chirp.domain.exception

class InvalidProfilePictureException (
    override val message: String? = null,
): RuntimeException(message ?: "Invalid profile picture")