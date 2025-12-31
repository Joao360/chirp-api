package com.joaograca.chirp.service.auth

import com.joaograca.chirp.domain.exception.*
import com.joaograca.chirp.domain.model.AuthenticatedUser
import com.joaograca.chirp.domain.model.User
import com.joaograca.chirp.domain.model.UserId
import com.joaograca.chirp.infra.database.entities.RefreshTokenEntity
import com.joaograca.chirp.infra.database.entities.UserEntity
import com.joaograca.chirp.infra.database.mappers.toUser
import com.joaograca.chirp.infra.database.repositories.RefreshTokenRepository
import com.joaograca.chirp.infra.database.repositories.UserRepository
import com.joaograca.chirp.infra.security.PasswordEncoder
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.MessageDigest
import java.time.Instant
import java.util.*

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtService: JwtService,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val emailVerificationService: EmailVerificationService
) {
    @Transactional
    fun register(email: String, username: String, password: String): User {
        val trimmedEmail = email.trim()
        val users = userRepository.findByEmailOrUsername(trimmedEmail, username.trim())
        if (users.isNotEmpty()) {
            throw UserAlreadyExistsException()
        }


        val savedUser = userRepository.saveAndFlush(
            UserEntity(
                email = trimmedEmail,
                username = username.trim(),
                hashedPassword = passwordEncoder.encode(password)
            )
        ).toUser()

        val token = emailVerificationService.createVerificationToken(trimmedEmail)

        return savedUser
    }

    fun login(
        email: String,
        password: String
    ): AuthenticatedUser {
        val user = userRepository.findByEmail(email.trim())
            ?: throw InvalidCredentialsException()

        if (!passwordEncoder.matches(password, user.hashedPassword)) {
            throw InvalidCredentialsException()
        }

        if (!user.hasVerifiedEmail) {
            throw EmailNotVerifiedException()
        }

        return user.id?.let { userId ->
            val accessToken = jwtService.generateAccessToken(userId)
            val refreshToken = jwtService.generateRefreshToken(userId)

            storeRefreshToken(userId, refreshToken)

            AuthenticatedUser(
                user = user.toUser(),
                accessToken = accessToken,
                refreshToken = refreshToken
            )
        } ?: throw UserNotFoundException()
    }

    @Transactional
    fun refresh(refreshToken: String): AuthenticatedUser {
        if (!jwtService.validateRefreshToken(refreshToken)) {
            throw InvalidTokenException("Invalid refresh token")
        }

        val userId = jwtService.getUserIdFromToken(refreshToken)
        val user = userRepository.findByIdOrNull(userId)
            ?: throw UserNotFoundException()

        val hashedToken = hashToken(refreshToken)
        refreshTokenRepository.findByUserIdAndHashedToken(userId, hashedToken)
            ?: throw InvalidTokenException("Invalid refresh token")

        refreshTokenRepository.deleteByUserIdAndHashedToken(userId, hashedToken)

        val newAccessToken = jwtService.generateAccessToken(userId)
        val newRefreshToken = jwtService.generateRefreshToken(userId)

        storeRefreshToken(userId, newRefreshToken)

        return AuthenticatedUser(
            user = user.toUser(),
            accessToken = newAccessToken,
            refreshToken = newRefreshToken
        )
    }

    @Transactional
    fun logout(refreshToken: String) {
        val userId = jwtService.getUserIdFromToken(refreshToken)
        val hashedToken = hashToken(refreshToken)
        refreshTokenRepository.deleteByUserIdAndHashedToken(userId, hashedToken)
    }

    private fun storeRefreshToken(userId: UserId, refreshToken: String) {
        val hashed = hashToken(refreshToken)
        val expiryMs = jwtService.refreshTokenValidityMs
        val expiresAt = Instant.now().plusMillis(expiryMs)

        refreshTokenRepository.save(
            RefreshTokenEntity(
                userId = userId,
                hashedToken = hashed,
                expiresAt = expiresAt
            )
        )
    }

    private fun hashToken(token: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(token.toByteArray())
        return Base64.getEncoder().encodeToString(hashBytes)
    }
}