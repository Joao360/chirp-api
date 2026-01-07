package com.joaograca.chirp.api.util

import com.joaograca.chirp.domain.exception.UnauthorizedException
import com.joaograca.chirp.domain.model.UserId
import org.springframework.security.core.context.SecurityContextHolder

val requestUserId: UserId
    get() = SecurityContextHolder.getContext().authentication?.principal as? UserId
        ?: throw UnauthorizedException()