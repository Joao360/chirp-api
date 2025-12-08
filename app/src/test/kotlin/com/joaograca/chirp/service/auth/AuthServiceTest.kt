package com.joaograca.chirp.service.auth

import com.joaograca.chirp.domain.exception.UserAlreadyExistsException
import com.joaograca.chirp.domain.model.User
import com.joaograca.chirp.infra.database.entities.UserEntity
import com.joaograca.chirp.infra.database.repositories.UserRepository
import com.joaograca.chirp.infra.security.PasswordEncoder
import io.mockk.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.UUID

class AuthServiceTest {
    private val userRepository = mockk<UserRepository>()
    private val passwordEncoder = mockk<PasswordEncoder>()
    private val authService = AuthService(userRepository, passwordEncoder)

    @Test
    fun `register should successfully create a new user with valid credentials`() {
        // Arrange
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

        every { userRepository.findByEmailOrUsername(email, username) } returns null
        every { passwordEncoder.encode(password) } returns hashedPassword
        every { userRepository.save(any()) } returns userEntity

        // Act
        val result = authService.register(email, username, password)

        // Assert
        assertNotNull(result)
        assertEquals(expectedUser.email, result.email)
        assertEquals(expectedUser.username, result.username)
        assertEquals(expectedUser.hasEmailVerified, result.hasEmailVerified)
        verify { userRepository.findByEmailOrUsername(email, username) }
        verify { passwordEncoder.encode(password) }
        verify { userRepository.save(any()) }
    }

    @Test
    fun `register should throw UserAlreadyExistsException when user with email already exists`() {
        // Arrange
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

        every { userRepository.findByEmailOrUsername(email, username) } returns existingUserEntity

        // Act & Assert
        assertThrows<UserAlreadyExistsException> {
            authService.register(email, username, password)
        }

        verify { userRepository.findByEmailOrUsername(email, username) }
        verify(exactly = 0) { passwordEncoder.encode(any()) }
        verify(exactly = 0) { userRepository.save(any()) }
    }

    @Test
    fun `register should throw UserAlreadyExistsException when user with username already exists`() {
        // Arrange
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

        every { userRepository.findByEmailOrUsername(email, username) } returns existingUserEntity

        // Act & Assert
        assertThrows<UserAlreadyExistsException> {
            authService.register(email, username, password)
        }

        verify { userRepository.findByEmailOrUsername(email, username) }
        verify(exactly = 0) { passwordEncoder.encode(any()) }
        verify(exactly = 0) { userRepository.save(any()) }
    }

    @Test
    fun `register should trim whitespace from email username and password inputs`() {
        // Arrange
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
        val expectedUser = User(id = userId, email = trimmedEmail, username = trimmedUsername, hasEmailVerified = false)

        every { userRepository.findByEmailOrUsername(trimmedEmail, trimmedUsername) } returns null
        every { passwordEncoder.encode(password) } returns hashedPassword
        every { userRepository.save(any()) } returns userEntity

        // Act
        val result = authService.register(emailWithSpaces, usernameWithSpaces, password)

        // Assert
        assertNotNull(result)
        assertEquals(expectedUser.email, result.email)
        assertEquals(expectedUser.username, result.username)
        verify { userRepository.findByEmailOrUsername(trimmedEmail, trimmedUsername) }
        verify { userRepository.save(match { entity ->
            entity.email == trimmedEmail && entity.username == trimmedUsername
        }) }
    }

    @Test
    fun `register should hash the password using PasswordEncoder`() {
        // Arrange
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

        every { userRepository.findByEmailOrUsername(email, username) } returns null
        every { passwordEncoder.encode(password) } returns hashedPassword
        every { userRepository.save(any()) } returns userEntity

        // Act
        authService.register(email, username, password)

        // Assert
        verify { passwordEncoder.encode(password) }
        verify { userRepository.save(match { entity ->
            entity.hashedPassword == hashedPassword
        }) }
    }
}

