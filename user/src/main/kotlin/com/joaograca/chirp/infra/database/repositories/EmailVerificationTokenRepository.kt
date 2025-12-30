package com.joaograca.chirp.infra.database.repositories

import com.joaograca.chirp.infra.database.entities.EmailVerificationTokenEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.time.Instant

interface EmailVerificationTokenRepository: JpaRepository<EmailVerificationTokenEntity, Long> {
    fun findByToken(token: String): EmailVerificationTokenEntity?
    fun deleteByExpiresAtLessThan(now: Instant): Int
    fun findByUserIdAndUsedAtIsNull(userId: Long): List<EmailVerificationTokenEntity>
}