package com.joaograca.chirp.service

import com.joaograca.chirp.domain.events.user.UserEvent
import com.joaograca.chirp.domain.exception.InvalidTokenException
import com.joaograca.chirp.domain.exception.UserNotFoundException
import com.joaograca.chirp.domain.model.emailVerificationToken
import com.joaograca.chirp.infra.database.entities.EmailVerificationTokenEntity
import com.joaograca.chirp.infra.database.entities.createTokenEntity
import com.joaograca.chirp.infra.database.entities.userEntity
import com.joaograca.chirp.infra.database.mappers.toUser
import com.joaograca.chirp.infra.database.repositories.EmailVerificationTokenRepository
import com.joaograca.chirp.infra.database.repositories.UserRepository
import com.joaograca.chirp.infra.message_queue.EventPublisher
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Instant
import java.time.temporal.ChronoUnit

class EmailVerificationServiceTest {

    private val userRepository = mockk<UserRepository>(relaxUnitFun = true) {
        every { save(any()) } answers { firstArg() }
    }
    private val emailVerificationTokenRepository = mockk<EmailVerificationTokenRepository>(relaxUnitFun = true) {
        every { save(any()) } answers { firstArg() }
        every { findByUserAndUsedAtIsNull(any()) } returns emptyList()
    }
    private val expiryHours = 24L
    private val eventPublisher = mockk<EventPublisher>(relaxUnitFun = true)

    private val emailVerificationService = EmailVerificationService(
        emailVerificationTokenRepository = emailVerificationTokenRepository,
        userRepository = userRepository,
        expiryHours = expiryHours,
        eventPublisher = eventPublisher
    )

    @Nested
    inner class CreateVerificationToken {

        @Test
        fun `should create verification token successfully when user exists`() {
            // Given
            val email = "test@example.com"
            val userEntity = userEntity(email = email)

            every { userRepository.findByEmail(email) } returns userEntity
            every { emailVerificationTokenRepository.save(any()) } answers {
                val savedToken = firstArg<EmailVerificationTokenEntity>()
                savedToken.apply { id = 1L }
            }

            // When
            val result = emailVerificationService.createVerificationToken(email)

            // Then
            assertNotNull(result)
            assertEquals(1L, result.id)
            assertEquals(email, result.user.email)
            verify { userRepository.findByEmail(email) }
            verify { emailVerificationTokenRepository.save(any()) }
        }

        @Test
        fun `should throw UserNotFoundException when user does not exist`() {
            // Given
            val email = "nonexistent@example.com"

            every { userRepository.findByEmail(email) } returns null

            // When & Then
            assertThrows<UserNotFoundException> {
                emailVerificationService.createVerificationToken(email)
            }

            verify { userRepository.findByEmail(email) }
            verify(exactly = 0) { emailVerificationTokenRepository.findByUserAndUsedAtIsNull(any()) }
            verify(exactly = 0) { emailVerificationTokenRepository.save(any()) }
        }

        @Test
        fun `should invalidate existing unused tokens when creating new token`() {
            // Given
            val email = "test@example.com"
            val userEntity = userEntity(email = email)
            val existingToken1 = createTokenEntity(id = 1L, token = "old-token-1", user = userEntity)
            val existingToken2 = createTokenEntity(id = 2L, token = "old-token-2", user = userEntity)

            every { userRepository.findByEmail(email) } returns userEntity
            every { emailVerificationTokenRepository.findByUserAndUsedAtIsNull(userEntity) } returns listOf(existingToken1, existingToken2)
            every { emailVerificationTokenRepository.save(any()) } answers {
                val savedToken = firstArg<EmailVerificationTokenEntity>()
                savedToken.apply { id = 3L }
            }

            // When
            val result = emailVerificationService.createVerificationToken(email)

            // Then
            assertNotNull(result)
            verify { emailVerificationTokenRepository.invalidateActiveTokensForUser(userEntity) }
            verify { emailVerificationTokenRepository.save(any()) }
        }
    }

    @Nested
    inner class VerifyEmail {

        @Test
        fun `should verify email successfully with valid token`() {
            // Given
            val token = "valid-token"
            val userEntity = userEntity(hasVerifiedEmail = false)
            val tokenEntity = createTokenEntity(token = token, user = userEntity)

            every { emailVerificationTokenRepository.findByToken(token) } returns tokenEntity

            // When
            emailVerificationService.verifyEmail(token)

            // Then
            verify { emailVerificationTokenRepository.findByToken(token) }
            verify { emailVerificationTokenRepository.save(match { it.usedAt != null }) }
            verify { userRepository.save(match { it.hasVerifiedEmail }) }
        }

        @Test
        fun `should throw InvalidTokenException when token does not exist`() {
            // Given
            val token = "nonexistent-token"

            every { emailVerificationTokenRepository.findByToken(token) } returns null

            // When & Then
            val exception = assertThrows<InvalidTokenException> {
                emailVerificationService.verifyEmail(token)
            }

            assertEquals("Email verification token is invalid", exception.message)
            verify { emailVerificationTokenRepository.findByToken(token) }
            verify(exactly = 0) { emailVerificationTokenRepository.save(any()) }
            verify(exactly = 0) { userRepository.save(any()) }
        }

        @Test
        fun `should throw InvalidTokenException when token is already used`() {
            // Given
            val token = "used-token"
            val userEntity = userEntity()
            val tokenEntity = createTokenEntity(
                token = token,
                user = userEntity,
                usedAt = Instant.now().minus(1, ChronoUnit.HOURS)
            )

            every { emailVerificationTokenRepository.findByToken(token) } returns tokenEntity

            // When & Then
            val exception = assertThrows<InvalidTokenException> {
                emailVerificationService.verifyEmail(token)
            }

            assertEquals("Email verification token has already been used", exception.message)
            verify { emailVerificationTokenRepository.findByToken(token) }
            verify(exactly = 0) { emailVerificationTokenRepository.save(any()) }
            verify(exactly = 0) { userRepository.save(any()) }
        }

        @Test
        fun `should throw InvalidTokenException when token is expired`() {
            // Given
            val token = "expired-token"
            val userEntity = userEntity()
            val tokenEntity = createTokenEntity(
                token = token,
                user = userEntity,
                expiresAt = Instant.now().minus(1, ChronoUnit.HOURS)
            )

            every { emailVerificationTokenRepository.findByToken(token) } returns tokenEntity

            // When & Then
            val exception = assertThrows<InvalidTokenException> {
                emailVerificationService.verifyEmail(token)
            }

            assertEquals("Email verification token has expired", exception.message)
            verify { emailVerificationTokenRepository.findByToken(token) }
            verify(exactly = 0) { emailVerificationTokenRepository.save(any()) }
            verify(exactly = 0) { userRepository.save(any()) }
        }

        @Test
        fun `resendVerificationEmail should not send email if user already verified`() {
            // Given
            every { userRepository.findByEmail(any()) } returns userEntity(hasVerifiedEmail = true)

            // When
            emailVerificationService.resendVerificationEmail("email")

            // Then
            verify(exactly = 0) { eventPublisher.publish(any()) }
        }

        @Test
        fun `resendVerificationEmail should send email if user not verified`() {
            // Given
            val userEntity = userEntity(hasVerifiedEmail = false)
            val emailVerificationToken = emailVerificationToken(user = userEntity.toUser())
            every { userRepository.findByEmail(any()) } returns userEntity
            every { emailVerificationTokenRepository.save(any()) } returns EmailVerificationTokenEntity(
                id = emailVerificationToken.id,
                token = emailVerificationToken.token,
                expiresAt = Instant.now().plus(24, ChronoUnit.HOURS),
                user = userEntity
            )

            // When
            emailVerificationService.resendVerificationEmail("email")

            // Then
            val expectedEvent = UserEvent.RequestResendVerification(
                userId = userEntity.id!!,
                email = userEntity.email,
                username = userEntity.username,
                verificationToken = emailVerificationToken.token,
            )
            verify {
                eventPublisher.publish(expectedEvent)
            }
        }
    }

    @Nested
    inner class CleanupExpiredTokens {

        @Test
        fun `should delete expired tokens`() {
            // Given
            every { emailVerificationTokenRepository.deleteByExpiresAtLessThan(any()) } returns 5

            // When
            emailVerificationService.cleanupExpiredTokens()

            // Then
            verify { emailVerificationTokenRepository.deleteByExpiresAtLessThan(any()) }
        }

        @Test
        fun `should not fail when no expired tokens exist`() {
            // Given
            every { emailVerificationTokenRepository.deleteByExpiresAtLessThan(any()) } returns 0

            // When
            emailVerificationService.cleanupExpiredTokens()

            // Then
            verify { emailVerificationTokenRepository.deleteByExpiresAtLessThan(any()) }
        }
    }
}