package com.joaograca.chirp.api.mappers

import com.joaograca.chirp.api.dto.PictureUploadResponse
import com.joaograca.chirp.domain.models.ProfilePictureCredentials

fun ProfilePictureCredentials.toResponse(): PictureUploadResponse {
    return PictureUploadResponse(
        uploadUrl = uploadUrl,
        publicUrl = publicUrl,
        headers = headers,
        expiresAt = expiresAt
    )
}