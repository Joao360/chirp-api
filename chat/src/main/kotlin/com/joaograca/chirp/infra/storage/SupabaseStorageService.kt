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
    @param:Value("\${supabase.url}") private val supabaseUrl: String,
    private val supabaseRestClient: RestClient
) {

    fun generateSignedUploadUrl(userId: UserId, mimeType: String): ProfilePictureCredentials {
        val fileExtension = allowedMimeTypesFileExtensions[mimeType]
            ?: throw InvalidProfilePictureException("Invalid file type: $mimeType")

        val fileName = "user_${userId}_${UUID.randomUUID()}.$fileExtension"
        val objectPath = "$SUPABASE_BUCKET_NAME/$fileName"

        val publicUrl = "$supabaseUrl/${SUPABASE_STORAGE_PATH}/public/$objectPath"

        return ProfilePictureCredentials(
            uploadUrl = createSignedUploadUrl(objectPath = objectPath),
            publicUrl = publicUrl,
            headers = mapOf("Content-Type" to mimeType),
            expiresAt = Instant.now().plusSeconds(UPLOAD_EXPIRATION_TIME_SECONDS)
        )
    }

    private fun createSignedUploadUrl(
        objectPath: String,
    ): String {
        val json = """
            { "expiresIn": $UPLOAD_EXPIRATION_TIME_SECONDS }
        """.trimIndent()

        val response = supabaseRestClient
            .post()
            .uri("/$SUPABASE_STORAGE_PATH/upload/sign/$objectPath")
            .header("Content-Type", "application/json")
            .body(json)
            .retrieve()
            .body(SignedUploadResponse::class.java)
            ?: throw StorageException("Failed to create signed URL")

        return "$supabaseUrl/storage/v1${response.url}"
    }

    fun deleteFile(url: String) {
        val supabasePathPrefix = "object/public/"
        val path = if (url.contains(supabasePathPrefix)) {
            url.substringAfter(supabasePathPrefix)
        } else {
            throw StorageException("Invalid URL")
        }

        val deleteUrl = "/$SUPABASE_STORAGE_PATH/$path"

        val response = supabaseRestClient
            .delete()
            .uri(deleteUrl)
            .retrieve()
            .toBodilessEntity()

        if (response.statusCode.isError) {
            throw StorageException("Failed to delete file: ${response.statusCode.value()}")
        }
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

        private const val SUPABASE_STORAGE_PATH = "storage/v1/object"
        private const val SUPABASE_BUCKET_NAME = "profile-pictures"
    }
}