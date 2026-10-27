package com.pyouyuu.app.core.api

/**
 * Contract interface matching `contracts/openapi.yaml` (v1.0.0).
 * Invariant: No endpoint accepts or proxies image bytes (D-10).
 */
interface PyouyuuApi {
    companion object {
        /** Locked header name for Settings.Secure.ANDROID_ID (D-15, OQ-05) */
        const val HEADER_ANDROID_ID = "X-Android-Id"

        /** Locked header name for Event Session Lease JWT (D-17, OQ-05) */
        const val HEADER_EVENT_SESSION = "X-Event-Session"
    }

    suspend fun authGoogle(request: AuthGoogleRequest): Result<AuthResponse>

    suspend fun updateBranding(request: BrandingRequest): Result<UserDto>

    suspend fun verifyPurchase(request: VerifyPurchaseRequest): Result<EntitlementDto>

    suspend fun createEvent(request: CreateEventRequest): Result<EventDto>

    suspend fun activatePass(eventId: String): Result<ActivatePassResponse>

    suspend fun setGalleryVisibility(
        eventId: String,
        request: GalleryVisibilityRequest,
    ): Result<GalleryVisibilityResponse>

    suspend fun initPhoto(
        request: InitPhotoRequest,
        eventSessionJwt: String? = null,
    ): Result<InitPhotoResponse>

    suspend fun completePhoto(
        photoUuid: String,
        request: CompletePhotoRequest,
    ): Result<CompletePhotoResponse>
}
