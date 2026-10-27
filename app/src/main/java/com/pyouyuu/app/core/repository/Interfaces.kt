package com.pyouyuu.app.core.repository

import com.pyouyuu.app.core.api.WatermarkPosition
import com.pyouyuu.app.core.model.CameraAdvisoryState
import com.pyouyuu.app.core.model.CaptureWarning
import com.pyouyuu.app.core.model.CapturedPhoto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Sub-lane A1 seam: Emits camera connection state, RAW warnings, and raw JPEG byte arrivals.
 */
interface CameraTetherSource {
    val advisoryState: StateFlow<CameraAdvisoryState>
    val warnings: Flow<CaptureWarning>
}

/**
 * Sub-lane A1 seam: Saves untouched original to MediaStore first (FR-IMG-01), then rotates,
 * resizes to 2048px, composites corner watermark, and emits CapturedPhoto in < 1s (D-08, D-11).
 */
interface ImagePipelineProcessor {
    suspend fun processCapturedJpeg(
        rawJpegBytes: ByteArray,
        fileName: String,
        eventId: String?,
        eventSlug: String,
        watermarkPngBytes: ByteArray?,
        watermarkPosition: WatermarkPosition = WatermarkPosition.BOTTOM_RIGHT,
    ): Result<CapturedPhoto>
}

/**
 * Shared repository observed by Sub-lane A2 (Kiosk UI) and driven by A1 + A3.
 */
interface PhotoRepository {
    val capturedPhotos: StateFlow<List<CapturedPhoto>>

    suspend fun enqueueCapturedPhoto(photo: CapturedPhoto)
}
