package com.joaograca.chirp.domain.exception

class SamePasswordException : RuntimeException(
    "The new password cannot be the same as the current password"
)