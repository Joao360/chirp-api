package com.joaograca.chirp.service

import com.joaograca.chirp.domain.exception.UserAlreadyExistsException
import com.joaograca.chirp.domain.model.User
import com.joaograca.chirp.infra.database.entities.UserEntity
import com.joaograca.chirp.infra.database.repositories.RefreshTokenRepository
import com.joaograca.chirp.infra.database.repositories.UserRepository
import com.joaograca.chirp.infra.security.PasswordEncoder
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.*

class AuthServiceTest {
    private val userRepository = mockk<UserRepository>(relaxUnitFun = true) {
        every { saveAndFlush(any()) } answers { firstArg() }
    }
    private val passwordEncoder = mockk<PasswordEncoder> {
        every { encode(any()) } returns "hashed_password"
    }
    private val jwtService = mockk<JwtService>()
    private val refreshTokenRepository = mockk<RefreshTokenRepository>(relaxUnitFun = true)
    private val emailVerificationService = mockk<EmailVerificationService>(relaxed = true)
    private val authService = AuthService(
        userRepository = userRepository,
        passwordEncoder = passwordEncoder,
        jwtService = jwtService,
        refreshTokenRepository = refreshTokenRepository,
        emailVerificationService = emailVerificationService
    )

    @Test
    fun `register should successfully create a new user with valid credentials`() {
        // Given
        val userId = UUID.randomUUID()
        val email = "test@example.com"
        val username = "testuser"
        val password = "password123"
        val hashedPassword = "hashed_password123"

        val userEntity = UserEntity(
            id = userId,
            email = email,
            username = username,
            hashedPassword = hashedPassword,
            hasVerifiedEmail = false
        )
        val expectedUser = User(id = userId, email = email, username = username, hasEmailVerified = false)

        every { userRepository.findByEmailOrUsername(email, username) } returns emptyList()
        every { passwordEncoder.encode(password) } returns hashedPassword
        every { userRepository.saveAndFlush(any()) } returns userEntity

        // When
        val result = authService.register(email, username, password)

        // Then
        assertNotNull(result)
        assertEquals(expectedUser.email, result.email)
        assertEquals(expectedUser.username, result.username)
        assertEquals(expectedUser.hasEmailVerified, result.hasEmailVerified)
        verify { userRepository.findByEmailOrUsername(email, username) }
        verify { passwordEncoder.encode(password) }
        verify { userRepository.saveAndFlush(any()) }
    }

    @Test
    fun `register should throw UserAlreadyExistsException when user with email already exists`() {
        // Given
        val existingUserId = UUID.randomUUID()
        val email = "existing@example.com"
        val username = "newuser"
        val password = "password123"

        val existingUserEntity = UserEntity(
            id = existingUserId,
            email = email,
            username = "existinguser",
            hashedPassword = "some_hash",
            hasVerifiedEmail = false
        )

        every { userRepository.findByEmailOrUsername(email, username) } returns listOf(existingUserEntity)

        // When & Then
        assertThrows<UserAlreadyExistsException> {
            authService.register(email, username, password)
        }

        verify { userRepository.findByEmailOrUsername(email, username) }
        verify(exactly = 0) { passwordEncoder.encode(any()) }
        verify(exactly = 0) { userRepository.saveAndFlush(any()) }
    }

    @Test
    fun `register should throw UserAlreadyExistsException when user with username already exists`() {
        // Given
        val existingUserId = UUID.randomUUID()
        val email = "new@example.com"
        val username = "existinguser"
        val password = "password123"

        val existingUserEntity = UserEntity(
            id = existingUserId,
            email = "existing@example.com",
            username = username,
            hashedPassword = "some_hash",
            hasVerifiedEmail = false
        )

        every { userRepository.findByEmailOrUsername(email, username) } returns listOf(existingUserEntity)

        // When & Then
        assertThrows<UserAlreadyExistsException> {
            authService.register(email, username, password)
        }

        verify { userRepository.findByEmailOrUsername(email, username) }
        verify(exactly = 0) { passwordEncoder.encode(any()) }
        verify(exactly = 0) { userRepository.saveAndFlush(any()) }
    }

    @Test
    fun `register should trim whitespace from email username and password inputs`() {
        // Given
        val userId = UUID.randomUUID()
        val emailWithSpaces = "  test@example.com  "
        val usernameWithSpaces = "  testuser  "
        val password = "password123"
        val hashedPassword = "hashed_password123"

        val trimmedEmail = emailWithSpaces.trim()
        val trimmedUsername = usernameWithSpaces.trim()

        val userEntity = UserEntity(
            id = userId,
            email = trimmedEmail,
            username = trimmedUsername,
            hashedPassword = hashedPassword,
            hasVerifiedEmail = false
        )

        every { userRepository.findByEmailOrUsername(trimmedEmail, trimmedUsername) } returns emptyList()
        every { passwordEncoder.encode(password) } returns hashedPassword
        every { userRepository.saveAndFlush(any()) } returns userEntity

        // When
        val result = authService.register(emailWithSpaces, usernameWithSpaces, password)

        // Then
        assertNotNull(result)
        assertEquals(trimmedEmail, result.email)
        assertEquals(trimmedUsername, result.username)
        verify { userRepository.findByEmailOrUsername(trimmedEmail, trimmedUsername) }
        verify { userRepository.saveAndFlush(match { entity ->
            entity.email == trimmedEmail && entity.username == trimmedUsername
        }) }
    }

    @Test
    fun `register should hash the password using PasswordEncoder`() {
        // Given
        val userId = UUID.randomUUID()
        val email = "test@example.com"
        val username = "testuser"
        val password = "plainPassword123"
        val hashedPassword = "bcrypt_hashed_password"

        val userEntity = UserEntity(
            id = userId,
            email = email,
            username = username,
            hashedPassword = hashedPassword,
            hasVerifiedEmail = false
        )

        every { userRepository.findByEmailOrUsername(email, username) } returns emptyList()
        every { passwordEncoder.encode(password) } returns hashedPassword
        every { userRepository.saveAndFlush(any()) } returns userEntity

        // When
        authService.register(email, username, password)

        // Then
        verify { passwordEncoder.encode(password) }
        verify { userRepository.saveAndFlush(match { entity ->
            entity.hashedPassword == hashedPassword
        }) }
    }
}

