package com.joaograca.chirp.infra.storage

import com.joaograca.chirp.domain.exception.InvalidProfilePictureException
import com.joaograca.chirp.domain.exception.StorageException
import com.joaograca.chirp.domain.models.ProfilePictureCredentials
import com.joaograca.chirp.domain.type.UserId
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import java.time.Instant
import java.util.*

@Service
class SupabaseStorageService(
    @param:Value("\${chirp.supabase.url}") private val supabaseUrl: String,
    private val supabaseRestClient: RestClient
) {

    fun generateSignedUploadUrl(userId: UserId, mimeType: String): ProfilePictureCredentials {
        val fileExtension = allowedMimeTypesFileExtensions[mimeType]
            ?: throw InvalidProfilePictureException("Invalid file type: $mimeType")

        val fileName = "user_${userId}_${UUID.randomUUID()}.$fileExtension"
        val bucketName = "profile-pictures"
        val path = "$bucketName/$fileName"

        val publicUrl = "$supabaseUrl/storage/v1/object/public/$path"

        return ProfilePictureCredentials(
            uploadUrl = createSignedUploadUrl(path = path, expiresInSeconds = UPLOAD_EXPIRATION_TIME_SECONDS),
            publicUrl = publicUrl,
            headers = mapOf("Content-Type" to mimeType),
            expiresAt = Instant.now().plusSeconds(UPLOAD_EXPIRATION_TIME_SECONDS)
        )
    }

    private fun createSignedUploadUrl(path: String, expiresInSeconds: Long): String {
        val json = """
            { "expiresIn": $expiresInSeconds }
        """.trimIndent()

        val response = supabaseRestClient
            .post()
            .uri("/storage/v1/object/upload/sign/$path")
            .body(json)
            .retrieve()
            .body(SignedUploadResponse::class.java)
            ?: throw StorageException("Failed to create signed URL")

        return response.url
    }

    private data class SignedUploadResponse(
        val url: String
    )

    companion object {
        private val allowedMimeTypesFileExtensions = mapOf(
            "image/jpeg" to "jpg",
            "image/jpg" to "jpg",
            "image/png" to "png",
            "image/webp" to "webp"
        )

        private const val UPLOAD_EXPIRATION_TIME_SECONDS = 300L // 5 minutes
    }
}