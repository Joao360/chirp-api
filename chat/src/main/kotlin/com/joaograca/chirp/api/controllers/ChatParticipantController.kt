package com.joaograca.chirp.api.controllers

import com.joaograca.chirp.api.dto.ChatParticipantDto
import com.joaograca.chirp.api.dto.ConfirmProfilePictureRequest
import com.joaograca.chirp.api.dto.PictureUploadResponse
import com.joaograca.chirp.api.mappers.toChatParticipantDto
import com.joaograca.chirp.api.mappers.toResponse
import com.joaograca.chirp.api.util.requestUserId
import com.joaograca.chirp.service.ChatParticipantService
import com.joaograca.chirp.service.ProfilePictureService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException

@RestController
@RequestMapping("/api/participants")
class ChatParticipantController(
    private val chatParticipantService: ChatParticipantService,
    private val profilePictureService: ProfilePictureService
) {

    @GetMapping
    fun getChatParticipantByUsernameOrEmail(
        @RequestParam(required = false) query: String?
    ): ChatParticipantDto {
        val participant = if (query == null) {
            chatParticipantService.findChatParticipantById(requestUserId)
        } else {
            chatParticipantService.findChatParticipantByEmailOrUsername(query)
        }

        return participant?.toChatParticipantDto()
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
    }

    @PostMapping("/profile-picture-upload")
    fun getProfilePictureUpload(
        @RequestParam mimeType: String,
    ): PictureUploadResponse {
        return profilePictureService.generateSignedUploadUrl(
            userId = requestUserId,
            mimeType = mimeType
        ).toResponse()
    }

    @PostMapping("/confirm-profile-picture")
    fun confirmProfilePictureUpload(
        @RequestBody body: ConfirmProfilePictureRequest,
    ) {
        profilePictureService.confirmProfilePictureUpload(
            userId = requestUserId,
            newUrl = body.publicUrl
        )
    }

    @DeleteMapping("/profile-picture")
    fun deleteProfilePicture() {
        profilePictureService.deleteProfilePicture(
            userId = requestUserId,
        )
    }
}
