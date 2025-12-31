package com.joaograca.chirp.service.auth

import com.joaograca.chirp.domain.exception.InvalidTokenException
import com.joaograca.chirp.domain.exception.UserNotFoundException
import com.joaograca.chirp.infra.database.entities.EmailVerificationTokenEntity
import com.joaograca.chirp.infra.database.entities.UserEntity
import com.joaograca.chirp.infra.database.repositories.EmailVerificationTokenRepository
import com.joaograca.chirp.infra.database.repositories.UserRepository
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

class EmailVerificationServiceTest {

    private val emailVerificationTokenRepository = mockk<EmailVerificationTokenRepository>()
    private val userRepository = mockk<UserRepository>()
    private val expiryHours = 24L

    private lateinit var emailVerificationService: EmailVerificationService

    @BeforeEach
    fun setUp() {
        clearAllMocks()
        emailVerificationService = EmailVerificationService(
            emailVerificationTokenRepository,
            userRepository,
            expiryHours
        )
    }

    private fun createUserEntity(
        id: UUID = UUID.randomUUID(),
        email: String = "test@example.com",
        username: String = "testuser",
        hasVerifiedEmail: Boolean = false
    ) = UserEntity(
        id = id,
        email = email,
        username = username,
        hashedPassword = "hashed_password",
        hasVerifiedEmail = hasVerifiedEmail
    )

    private fun createTokenEntity(
        id: Long = 1L,
        token: String = "test-token",
        user: UserEntity,
        expiresAt: Instant = Instant.now().plus(24, ChronoUnit.HOURS),
        usedAt: Instant? = null
    ) = EmailVerificationTokenEntity(
        id = id,
        token = token,
        expiresAt = expiresAt,
        user = user,
        usedAt = usedAt
    )

    @Nested
    inner class CreateVerificationToken {

        @Test
        fun `should create verification token successfully when user exists`() {
            // Arrange
            val email = "test@example.com"
            val userEntity = createUserEntity(email = email)

            every { userRepository.findByEmail(email) } returns userEntity
            every { emailVerificationTokenRepository.findByUserIdAndUsedAtIsNull(userEntity) } returns emptyList()
            every { emailVerificationTokenRepository.saveAll(any<List<EmailVerificationTokenEntity>>()) } returns emptyList()
            every { emailVerificationTokenRepository.save(any()) } answers {
                val savedToken = firstArg<EmailVerificationTokenEntity>()
                savedToken.apply { id = 1L }
            }

            // Act
            val result = emailVerificationService.createVerificationToken(email)

            // Assert
            assertNotNull(result)
            assertEquals(1L, result.id)
            assertEquals(email, result.user.email)
            verify { userRepository.findByEmail(email) }
            verify { emailVerificationTokenRepository.findByUserIdAndUsedAtIsNull(userEntity) }
            verify { emailVerificationTokenRepository.save(any()) }
        }

        @Test
        fun `should throw UserNotFoundException when user does not exist`() {
            // Arrange
            val email = "nonexistent@example.com"

            every { userRepository.findByEmail(email) } returns null

            // Act & Assert
            assertThrows<UserNotFoundException> {
                emailVerificationService.createVerificationToken(email)
            }

            verify { userRepository.findByEmail(email) }
            verify(exactly = 0) { emailVerificationTokenRepository.findByUserIdAndUsedAtIsNull(any()) }
            verify(exactly = 0) { emailVerificationTokenRepository.save(any()) }
        }

        @Test
        fun `should invalidate existing unused tokens when creating new token`() {
            // Arrange
            val email = "test@example.com"
            val userEntity = createUserEntity(email = email)
            val existingToken1 = createTokenEntity(id = 1L, token = "old-token-1", user = userEntity)
            val existingToken2 = createTokenEntity(id = 2L, token = "old-token-2", user = userEntity)

            every { userRepository.findByEmail(email) } returns userEntity
            every { emailVerificationTokenRepository.findByUserIdAndUsedAtIsNull(userEntity) } returns listOf(existingToken1, existingToken2)
            every { emailVerificationTokenRepository.saveAll(any<List<EmailVerificationTokenEntity>>()) } answers {
                firstArg<List<EmailVerificationTokenEntity>>()
            }
            every { emailVerificationTokenRepository.save(any()) } answers {
                val savedToken = firstArg<EmailVerificationTokenEntity>()
                savedToken.apply { id = 3L }
            }

            // Act
            val result = emailVerificationService.createVerificationToken(email)

            // Assert
            assertNotNull(result)
            verify { emailVerificationTokenRepository.saveAll(match<List<EmailVerificationTokenEntity>> { tokens ->
                tokens.all { it.usedAt != null }
            }) }
            verify { emailVerificationTokenRepository.save(any()) }
        }
    }

    @Nested
    inner class VerifyEmail {

        @Test
        fun `should verify email successfully with valid token`() {
            // Arrange
            val token = "valid-token"
            val userEntity = createUserEntity(hasVerifiedEmail = false)
            val tokenEntity = createTokenEntity(token = token, user = userEntity)

            every { emailVerificationTokenRepository.findByToken(token) } returns tokenEntity
            every { emailVerificationTokenRepository.save(any()) } answers { firstArg() }
            every { userRepository.save(any()) } answers { firstArg() }

            // Act
            emailVerificationService.verifyEmail(token)

            // Assert
            verify { emailVerificationTokenRepository.findByToken(token) }
            verify { emailVerificationTokenRepository.save(match { it.usedAt != null }) }
            verify { userRepository.save(match { it.hasVerifiedEmail }) }
        }

        @Test
        fun `should throw InvalidTokenException when token does not exist`() {
            // Arrange
            val token = "nonexistent-token"

            every { emailVerificationTokenRepository.findByToken(token) } returns null

            // Act & Assert
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
            // Arrange
            val token = "used-token"
            val userEntity = createUserEntity()
            val tokenEntity = createTokenEntity(
                token = token,
                user = userEntity,
                usedAt = Instant.now().minus(1, ChronoUnit.HOURS)
            )

            every { emailVerificationTokenRepository.findByToken(token) } returns tokenEntity

            // Act & Assert
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
            // Arrange
            val token = "expired-token"
            val userEntity = createUserEntity()
            val tokenEntity = createTokenEntity(
                token = token,
                user = userEntity,
                expiresAt = Instant.now().minus(1, ChronoUnit.HOURS)
            )

            every { emailVerificationTokenRepository.findByToken(token) } returns tokenEntity

            // Act & Assert
            val exception = assertThrows<InvalidTokenException> {
                emailVerificationService.verifyEmail(token)
            }

            assertEquals("Email verification token has expired", exception.message)
            verify { emailVerificationTokenRepository.findByToken(token) }
            verify(exactly = 0) { emailVerificationTokenRepository.save(any()) }
            verify(exactly = 0) { userRepository.save(any()) }
        }
    }

    @Nested
    inner class CleanupExpiredTokens {

        @Test
        fun `should delete expired tokens`() {
            // Arrange
            every { emailVerificationTokenRepository.deleteByExpiresAtLessThan(any()) } returns 5

            // Act
            emailVerificationService.cleanupExpiredTokens()

            // Assert
            verify { emailVerificationTokenRepository.deleteByExpiresAtLessThan(any()) }
        }

        @Test
        fun `should not fail when no expired tokens exist`() {
            // Arrange
            every { emailVerificationTokenRepository.deleteByExpiresAtLessThan(any()) } returns 0

            // Act
            emailVerificationService.cleanupExpiredTokens()

            // Assert
            verify { emailVerificationTokenRepository.deleteByExpiresAtLessThan(any()) }
        }
    }
}