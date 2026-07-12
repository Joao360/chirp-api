package com.joaograca.chirp.service

import com.joaograca.chirp.domain.event.ProfilePictureUpdatedEvent
import com.joaograca.chirp.domain.exception.ChatParticipantNotFoundException
import com.joaograca.chirp.domain.models.ProfilePictureCredentials
import com.joaograca.chirp.domain.type.UserId
import com.joaograca.chirp.infra.database.repositories.ChatParticipantRepository
import com.joaograca.chirp.infra.storage.SupabaseStorageService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ProfilePictureService(
    private val supabaseStorageService: SupabaseStorageService,
    private val chatParticipantRepository: ChatParticipantRepository,
    private val applicationEventPublisher: ApplicationEventPublisher,
    @param:Value("\${supabase.url}") private val supabaseUrl: String,
) {
    private val logger = LoggerFactory.getLogger(ProfilePictureService::class.java)

    fun generateSignedUploadUrl(
        userId: UserId,
        mimeType: String,
    ): ProfilePictureCredentials {
        return supabaseStorageService.generateSignedUploadUrl(userId, mimeType)
    }

    @Transactional
    fun deleteProfilePicture(userId: UserId) {
        val chatParticipant = chatParticipantRepository.findByIdOrNull(userId)
            ?: throw ChatParticipantNotFoundException(userId)

        chatParticipant.profilePictureUrl?.let { url ->
            chatParticipantRepository.save(
                chatParticipant.apply { profilePictureUrl = null }
            )

            supabaseStorageService.deleteFile(url)

            applicationEventPublisher.publishEvent(
                ProfilePictureUpdatedEvent(
                    userId = userId,
                    newUrl = null
                )
            )
        }
    }

    @Transactional
    fun confirmProfilePictureUpload(userId: UserId, newUrl: String) {
        if (!newUrl.startsWith("https://$supabaseUrl")) {
            throw IllegalArgumentException("Invalid profile picture url")
        }

        val chatParticipant = chatParticipantRepository.findByIdOrNull(userId)
            ?: throw ChatParticipantNotFoundException(userId)

        val oldProfilePictureUrl = chatParticipant.profilePictureUrl

        chatParticipantRepository.save(
            chatParticipant.apply { profilePictureUrl = newUrl }
        )

        try {
            oldProfilePictureUrl?.let { url ->
                supabaseStorageService.deleteFile(url)
            }
        } catch (e: Exception) {
            logger.warn("Failed to delete old profile picture for user $userId", e)
        }

        applicationEventPublisher.publishEvent(
            ProfilePictureUpdatedEvent(
                userId = userId,
                newUrl = newUrl
            )
        )
    }
}