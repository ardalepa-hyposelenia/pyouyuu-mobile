package com.pyouyuu.app.core.model

import com.pyouyuu.app.core.constants.PyouyuuConstants

/**
 * FR-CAM-02, D-01, D-26: Camera advisory state based on USB PTP GetDeviceInfo model string.
 */
enum class CameraAdvisoryState {
    DISCONNECTED,
    LEGACY_SONY_PC_REMOTE_NO_SD_WRITE,
    MODERN_DUAL_SAVE_TIP,
}

/**
 * FR-CAM-03: Non-blocking warnings emitted by the USB PTP listener.
 */
enum class CaptureWarning {
    RAW_SKIPPED_WARNING,
}

/**
 * RFC-001 §4.7: 4-stage offline-resilient upload state machine.
 */
enum class SyncStage {
    LOCAL_ONLY,
    INIT_SENT_TO_GO,
    UPLOADED_TO_DRIVE,
    COMPLETED_ON_GO,
    FAILED_PAYWALL,
}

/**
 * Represents a captured and locally processed photo ready for instant Kiosk QR display (< 1s, D-11).
 */
data class CapturedPhoto(
    val photoUuid: String,
    val eventId: String?,
    val fileName: String,
    val originalUri: String,
    val watermarkedCachePath: String,
    val widthPx: Int,
    val heightPx: Int,
    val takenAtEpochMs: Long,
    val syncStage: SyncStage = SyncStage.LOCAL_ONLY,
    val driveFileId: String? = null,
)

/**
 * Helpers for camera detection and QR URL construction using generated constants.
 */
object PyouyuuDomainRules {
    private val legacySonyRegex = Regex(PyouyuuConstants.LEGACY_SONY_MODEL_REGEX)

    /**
     * FR-CAM-02: Classifies camera model string from PTP GetDeviceInfo (0x1001).
     */
    fun classifyCameraModel(model: String): CameraAdvisoryState {
        if (model.isBlank()) return CameraAdvisoryState.DISCONNECTED
        return if (legacySonyRegex.matches(model)) {
            CameraAdvisoryState.LEGACY_SONY_PC_REMOTE_NO_SD_WRITE
        } else {
            CameraAdvisoryState.MODERN_DUAL_SAVE_TIP
        }
    }

    /**
     * D-07, OQ-12: Builds guest QR landing URL `https://{domain}/p/{photo_uuid}`.
     */
    fun buildGuestQrUrl(
        photoUuid: String,
        domain: String = PyouyuuConstants.DEFAULT_PUBLIC_WEB_DOMAIN,
    ): String = "https://${domain.trim().trimEnd('/')}/p/$photoUuid"
}
