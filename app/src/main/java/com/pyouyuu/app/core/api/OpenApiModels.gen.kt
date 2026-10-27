// Code generated from contracts/openapi.yaml (v1.0.0). DO NOT EDIT.
package com.pyouyuu.app.core.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class ErrorCode {
    ACTIVE_DEVICE_CONFLICT,
    DEVICE_REVOKED,
    UNAUTHORIZED,
    VALIDATION_FAILED,
    NOT_FOUND,
    PURCHASE_INVALID,
    NO_EVENT_CREDITS,
    PASS_ALREADY_ACTIVE,
    FREE_QUOTA_EXHAUSTED,
    GALLERY_DISABLED,
    INTERNAL,
}

@Serializable
data class ApiErrorDetail(
    @SerialName("code") val code: ErrorCode,
    @SerialName("message") val message: String,
)

@Serializable
data class ApiErrorResponse(
    @SerialName("error") val error: ApiErrorDetail,
)

@Serializable
enum class PhotoStatus {
    UPLOADING,
    READY,
    FAILED,
}

@Serializable
enum class WatermarkPosition {
    TOP_LEFT,
    TOP_RIGHT,
    BOTTOM_LEFT,
    BOTTOM_RIGHT,
    BOTTOM_CENTER,
}

@Serializable
data class AuthGoogleRequest(
    @SerialName("id_token") val idToken: String,
    @SerialName("android_id") val androidId: String,
    @SerialName("force_device_switch") val forceDeviceSwitch: Boolean = false,
)

@Serializable
data class UserDto(
    @SerialName("id") val id: String,
    @SerialName("email") val email: String,
    @SerialName("studio_name") val studioName: String,
    @SerialName("instagram_handle") val instagramHandle: String,
    @SerialName("whatsapp_number") val whatsappNumber: String,
)

@Serializable
data class EntitlementDto(
    @SerialName("free_uploads_remaining") val freeUploadsRemaining: Int,
    @SerialName("event_credits") val eventCredits: Int,
    @SerialName("pro_expires_at") val proExpiresAt: String? = null,
)

@Serializable
data class AuthResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("user") val user: UserDto,
    @SerialName("free_uploads_remaining") val freeUploadsRemaining: Int,
    @SerialName("event_credits") val eventCredits: Int,
    @SerialName("pro_expires_at") val proExpiresAt: String? = null,
)

@Serializable
data class BrandingRequest(
    @SerialName("studio_name") val studioName: String? = null,
    @SerialName("instagram_handle") val instagramHandle: String? = null,
    @SerialName("whatsapp_number") val whatsappNumber: String? = null,
)

@Serializable
data class VerifyPurchaseRequest(
    @SerialName("product_id") val productId: String,
    @SerialName("purchase_token") val purchaseToken: String,
)

@Serializable
data class CreateEventRequest(
    @SerialName("name") val name: String,
    @SerialName("drive_folder_id") val driveFolderId: String,
    @SerialName("storage_google_email") val storageGoogleEmail: String,
    @SerialName("is_gallery_public") val isGalleryPublic: Boolean = true,
    @SerialName("watermark_position") val watermarkPosition: WatermarkPosition = WatermarkPosition.BOTTOM_RIGHT,
)

@Serializable
data class EventDto(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String,
    @SerialName("slug") val slug: String,
    @SerialName("drive_folder_id") val driveFolderId: String,
    @SerialName("storage_google_email") val storageGoogleEmail: String,
    @SerialName("is_gallery_public") val isGalleryPublic: Boolean,
    @SerialName("watermark_position") val watermarkPosition: WatermarkPosition,
    @SerialName("pass_display_expires_at") val passDisplayExpiresAt: String? = null,
    @SerialName("pass_hard_expires_at") val passHardExpiresAt: String? = null,
)

@Serializable
data class ActivatePassResponse(
    @SerialName("event_session_jwt") val eventSessionJwt: String,
    @SerialName("display_expires_at") val displayExpiresAt: String,
    @SerialName("hard_expires_at") val hardExpiresAt: String,
    @SerialName("event_credits") val eventCredits: Int,
)

@Serializable
data class GalleryVisibilityRequest(
    @SerialName("is_gallery_public") val isGalleryPublic: Boolean,
)

@Serializable
data class GalleryVisibilityResponse(
    @SerialName("event_id") val eventId: String,
    @SerialName("is_gallery_public") val isGalleryPublic: Boolean,
)

@Serializable
data class InitPhotoRequest(
    @SerialName("photo_uuid") val photoUuid: String,
    @SerialName("event_id") val eventId: String,
    @SerialName("file_name") val fileName: String,
    @SerialName("width_px") val widthPx: Int,
    @SerialName("height_px") val heightPx: Int,
    @SerialName("taken_at") val takenAt: String? = null,
)

@Serializable
data class InitPhotoResponse(
    @SerialName("photo_uuid") val photoUuid: String,
    @SerialName("status") val status: PhotoStatus,
    @SerialName("free_uploads_remaining") val freeUploadsRemaining: Int,
)

@Serializable
data class CompletePhotoRequest(
    @SerialName("drive_file_id") val driveFileId: String,
)

@Serializable
data class CompletePhotoResponse(
    @SerialName("photo_uuid") val photoUuid: String,
    @SerialName("status") val status: PhotoStatus,
)
