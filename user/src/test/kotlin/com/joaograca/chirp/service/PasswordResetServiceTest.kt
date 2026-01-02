package com.joaograca.chirp.service

import com.joaograca.chirp.domain.exception.InvalidCredentialsException
import com.joaograca.chirp.domain.exception.InvalidTokenException
import com.joaograca.chirp.domain.exception.SamePasswordException
import com.joaograca.chirp.domain.exception.UserNotFoundException
import com.joaograca.chirp.infra.database.entities.createPasswordResetTokenEntity
import com.joaograca.chirp.infra.database.entities.createUserEntity
import com.joaograca.chirp.infra.database.repositories.PasswordResetTokenRepository
import com.joaograca.chirp.infra.database.repositories.RefreshTokenRepository
import com.joaograca.chirp.infra.database.repositories.UserRepository
import com.joaograca.chirp.infra.security.PasswordEncoder
import io.mockk.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

class PasswordResetServiceTest {

    private val userRepository = mockk<UserRepository>(relaxUnitFun = true) {
        every { save(any()) } answers { firstArg() }
    }
    private val passwordResetTokenRepository = mockk<PasswordResetTokenRepository>(relaxUnitFun = true) {
        every { save(any()) } answers { firstArg() }
    }
    private val passwordEncoder = mockk<PasswordEncoder> {
        every { encode(any()) } returns "hashed_password"
    }
    private val refreshTokenRepository = mockk<RefreshTokenRepository>(relaxUnitFun = true)
    private val expiryMinutes = 30L

    private val passwordResetService = PasswordResetService(
        userRepository = userRepository,
        passwordResetTokenRepository = passwordResetTokenRepository,
        passwordEncoder = passwordEncoder,
        expiryMinutes = expiryMinutes,
        refreshTokenRepository = refreshTokenRepository
    )

    @Nested
    inner class RequestPasswordReset {

        @Test
        fun `should create password reset token when user exists`() {
            // Given
            val email = "test@example.com"
            val userEntity = createUserEntity(email = email)

            every { userRepository.findByEmail(email) } returns userEntity

            // When
            passwordResetService.requestPasswordReset(email)

            // Then
            verify { userRepository.findByEmail(email) }
            verify { passwordResetTokenRepository.invalidateActiveTokensForUser(userEntity) }
            verify { passwordResetTokenRepository.save(match {
                it.user == userEntity && it.expiresAt.isAfter(Instant.now())
            }) }
        }

        @Test
        fun `should silently ignore when user does not exist for security reasons`() {
            // Given
            val email = "nonexistent@example.com"

            every { userRepository.findByEmail(email) } returns null

            // When - should not throw
            passwordResetService.requestPasswordReset(email)

            // Then
            verify { userRepository.findByEmail(email) }
            verify(exactly = 0) { passwordResetTokenRepository.invalidateActiveTokensForUser(any()) }
            verify(exactly = 0) { passwordResetTokenRepository.save(any()) }
        }

        @Test
        fun `should invalidate existing active tokens before creating new one`() {
            // Given
            val email = "test@example.com"
            val userEntity = createUserEntity(email = email)

            every { userRepository.findByEmail(email) } returns userEntity

            // When
            passwordResetService.requestPasswordReset(email)

            // Then
            verifyOrder {
                passwordResetTokenRepository.invalidateActiveTokensForUser(userEntity)
                passwordResetTokenRepository.save(any())
            }
        }
    }

    @Nested
    inner class ResetPassword {

        @Test
        fun `should reset password successfully with valid token`() {
            // Given
            val token = "valid-token"
            val newPassword = "newPassword123"
            val hashedNewPassword = "hashed_new_password"
            val userId = UUID.randomUUID()
            val userEntity = createUserEntity(id = userId, hashedPassword = "old_hashed_password")
            val tokenEntity = createPasswordResetTokenEntity(token = token, user = userEntity)

            every { passwordResetTokenRepository.findByToken(token) } returns tokenEntity
            every { passwordEncoder.matches(newPassword, userEntity.hashedPassword) } returns false
            every { passwordEncoder.encode(newPassword) } returns hashedNewPassword

            // When
            passwordResetService.resetPassword(token, newPassword)

            // Then
            verify { passwordResetTokenRepository.findByToken(token) }
            verify { passwordEncoder.matches(newPassword, "old_hashed_password") }
            verify { passwordEncoder.encode(newPassword) }
            verify { userRepository.save(match { it.hashedPassword == hashedNewPassword }) }
            verify { passwordResetTokenRepository.save(match { it.usedAt != null }) }
            verify { refreshTokenRepository.deleteByUserId(userId) }
        }

        @Test
        fun `should throw InvalidTokenException when token is not found`() {
            // Given
            val token = "invalid-token"

            every { passwordResetTokenRepository.findByToken(token) } returns null

            // When & Then
            val exception = assertThrows<InvalidTokenException> {
                passwordResetService.resetPassword(token, "newPassword123")
            }
            assertEquals("Invalid password reset token", exception.message)

            verify { passwordResetTokenRepository.findByToken(token) }
            verify(exactly = 0) { passwordEncoder.matches(any(), any()) }
            verify(exactly = 0) { userRepository.save(any()) }
        }

        @Test
        fun `should throw InvalidTokenException when token is already used`() {
            // Given
            val token = "used-token"
            val userEntity = createUserEntity()
            val tokenEntity = createPasswordResetTokenEntity(
                token = token,
                user = userEntity,
                usedAt = Instant.now().minus(1, ChronoUnit.HOURS)
            )

            every { passwordResetTokenRepository.findByToken(token) } returns tokenEntity

            // When & Then
            val exception = assertThrows<InvalidTokenException> {
                passwordResetService.resetPassword(token, "newPassword123")
            }
            assertEquals("Email verification token has already been used", exception.message)

            verify { passwordResetTokenRepository.findByToken(token) }
            verify(exactly = 0) { passwordEncoder.matches(any(), any()) }
            verify(exactly = 0) { userRepository.save(any()) }
        }

        @Test
        fun `should throw InvalidTokenException when token is expired`() {
            // Given
            val token = "expired-token"
            val userEntity = createUserEntity()
            val tokenEntity = createPasswordResetTokenEntity(
                token = token,
                user = userEntity,
                expiresAt = Instant.now().minus(1, ChronoUnit.HOURS)
            )

            every { passwordResetTokenRepository.findByToken(token) } returns tokenEntity

            // When & Then
            val exception = assertThrows<InvalidTokenException> {
                passwordResetService.resetPassword(token, "newPassword123")
            }
            assertEquals("Email verification token has expired", exception.message)

            verify { passwordResetTokenRepository.findByToken(token) }
            verify(exactly = 0) { passwordEncoder.matches(any(), any()) }
            verify(exactly = 0) { userRepository.save(any()) }
        }

        @Test
        fun `should throw SamePasswordException when new password does not match current password`() {
            // Given
            val token = "valid-token"
            val newPassword = "differentPassword123"
            val userEntity = createUserEntity(hashedPassword = "hashed_current_password")
            val tokenEntity = createPasswordResetTokenEntity(token = token, user = userEntity)

            every { passwordResetTokenRepository.findByToken(token) } returns tokenEntity
            every { passwordEncoder.matches(newPassword, userEntity.hashedPassword) } returns true

            // When & Then
            assertThrows<SamePasswordException> {
                passwordResetService.resetPassword(token, newPassword)
            }

            verify { passwordResetTokenRepository.findByToken(token) }
            verify { passwordEncoder.matches(newPassword, userEntity.hashedPassword) }
            verify(exactly = 0) { passwordEncoder.encode(any()) }
            verify(exactly = 0) { userRepository.save(any()) }
        }

        @Test
        fun `should delete all refresh tokens when password is reset`() {
            // Given
            val token = "valid-token"
            val newPassword = "newPassword123"
            val userId = UUID.randomUUID()
            val userEntity = createUserEntity(id = userId)
            val tokenEntity = createPasswordResetTokenEntity(token = token, user = userEntity)

            every { passwordResetTokenRepository.findByToken(token) } returns tokenEntity
            every { passwordEncoder.matches(newPassword, userEntity.hashedPassword) } returns false

            // When
            passwordResetService.resetPassword(token, newPassword)

            // Then
            verify { refreshTokenRepository.deleteByUserId(userId) }
        }
    }

    @Nested
    inner class ChangePassword {

        @Test
        fun `should change password successfully with valid credentials`() {
            // Given
            val oldPassword = "oldPassword123"
            val newPassword = "newPassword123"
            val hashedNewPassword = "hashed_new_password"
            val userId = UUID.randomUUID()
            val userEntity = createUserEntity(id = userId, hashedPassword = "hashed_old_password")

            every { userRepository.findById(userId) } returns Optional.of(userEntity)
            every { passwordEncoder.matches(oldPassword, userEntity.hashedPassword) } returns true
            every { passwordEncoder.encode(newPassword) } returns hashedNewPassword

            // When
            passwordResetService.changePassword(oldPassword, newPassword, userId)

            // Then
            verify { userRepository.findById(userId) }
            verify { passwordEncoder.matches(oldPassword, "hashed_old_password") }
            verify { passwordEncoder.encode(newPassword) }
            verify { refreshTokenRepository.deleteByUserId(userId) }
            verify { userRepository.save(match { it.hashedPassword == hashedNewPassword }) }
        }

        @Test
        fun `should throw UserNotFoundException when user does not exist`() {
            // Given
            val userId = UUID.randomUUID()

            every { userRepository.findById(userId) } returns Optional.empty()

            // When & Then
            assertThrows<UserNotFoundException> {
                passwordResetService.changePassword("oldPass", "newPass", userId)
            }

            verify { userRepository.findById(userId) }
            verify(exactly = 0) { passwordEncoder.matches(any(), any()) }
            verify(exactly = 0) { userRepository.save(any()) }
        }

        @Test
        fun `should throw InvalidCredentialsException when old password is incorrect`() {
            // Given
            val oldPassword = "wrongPassword"
            val newPassword = "newPassword123"
            val userId = UUID.randomUUID()
            val userEntity = createUserEntity(id = userId, hashedPassword = "hashed_correct_password")

            every { userRepository.findById(userId) } returns Optional.of(userEntity)
            every { passwordEncoder.matches(oldPassword, userEntity.hashedPassword) } returns false

            // When & Then
            assertThrows<InvalidCredentialsException> {
                passwordResetService.changePassword(oldPassword, newPassword, userId)
            }

            verify { userRepository.findById(userId) }
            verify { passwordEncoder.matches(oldPassword, userEntity.hashedPassword) }
            verify(exactly = 0) { passwordEncoder.encode(any()) }
            verify(exactly = 0) { userRepository.save(any()) }
        }

        @Test
        fun `should throw SamePasswordException when new password equals old password`() {
            // Given
            val samePassword = "samePassword123"
            val userId = UUID.randomUUID()
            val userEntity = createUserEntity(id = userId)

            every { userRepository.findById(userId) } returns Optional.of(userEntity)
            every { passwordEncoder.matches(samePassword, userEntity.hashedPassword) } returns true

            // When & Then
            assertThrows<SamePasswordException> {
                passwordResetService.changePassword(samePassword, samePassword, userId)
            }

            verify { userRepository.findById(userId) }
            verify { passwordEncoder.matches(samePassword, userEntity.hashedPassword) }
            verify(exactly = 0) { passwordEncoder.encode(any()) }
            verify(exactly = 0) { userRepository.save(any()) }
        }

        @Test
        fun `should delete all refresh tokens when password is changed`() {
            // Given
            val oldPassword = "oldPassword123"
            val newPassword = "newPassword123"
            val userId = UUID.randomUUID()
            val userEntity = createUserEntity(id = userId)

            every { userRepository.findById(userId) } returns Optional.of(userEntity)
            every { passwordEncoder.matches(oldPassword, userEntity.hashedPassword) } returns true

            // When
            passwordResetService.changePassword(oldPassword, newPassword, userId)

            // Then
            verify { refreshTokenRepository.deleteByUserId(userId) }
        }

        @Test
        fun `should delete refresh tokens before saving new password`() {
            // Given
            val oldPassword = "oldPassword123"
            val newPassword = "newPassword123"
            val userId = UUID.randomUUID()
            val userEntity = createUserEntity(id = userId)

            every { userRepository.findById(userId) } returns Optional.of(userEntity)
            every { passwordEncoder.matches(oldPassword, userEntity.hashedPassword) } returns true

            // When
            passwordResetService.changePassword(oldPassword, newPassword, userId)

            // Then
            verifyOrder {
                refreshTokenRepository.deleteByUserId(userId)
                userRepository.save(any())
            }
        }
    }

    @Nested
    inner class CleanupExpiredTokens {

        @Test
        fun `should delete expired tokens`() {
            // Given
            every { passwordResetTokenRepository.deleteByExpiresAtLessThan(any()) } returns 5

            // When
            passwordResetService.cleanupExpiredTokens()

            // Then
            verify { passwordResetTokenRepository.deleteByExpiresAtLessThan(any()) }
        }

        @Test
        fun `should call deleteByExpiresAtLessThan with current time`() {
            // Given
            val capturedInstant = slot<Instant>()
            every { passwordResetTokenRepository.deleteByExpiresAtLessThan(capture(capturedInstant)) } returns 0

            val beforeTest = Instant.now()

            // When
            passwordResetService.cleanupExpiredTokens()

            val afterTest = Instant.now()

            // Then
            assertTrue(capturedInstant.captured.isAfter(beforeTest.minusSeconds(1)))
            assertTrue(capturedInstant.captured.isBefore(afterTest.plusSeconds(1)))
        }
    }
}

