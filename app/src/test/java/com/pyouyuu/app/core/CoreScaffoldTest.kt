package com.pyouyuu.app.core

import com.pyouyuu.app.core.constants.PyouyuuConstants
import com.pyouyuu.app.core.fakes.FakePhotoRepository
import com.pyouyuu.app.core.model.CameraAdvisoryState
import com.pyouyuu.app.core.model.CapturedPhoto
import com.pyouyuu.app.core.model.PyouyuuDomainRules
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class CoreScaffoldTest {
    @Test
    fun `generated PyouyuuConstants match frozen contract v1_0_0`() {
        assertEquals("Pyouyuu", PyouyuuConstants.APP_NAME)
        assertEquals("pyouyuu.com", PyouyuuConstants.DEFAULT_PUBLIC_WEB_DOMAIN)
        assertEquals("Pyouyuu Events", PyouyuuConstants.DRIVE_ROOT_FOLDER_NAME)
        assertEquals("Pyouyuu_Originals", PyouyuuConstants.LOCAL_ORIGINALS_DIR_NAME)
        assertEquals(10, PyouyuuConstants.FREE_CLOUD_UPLOAD_LIMIT)
        assertEquals(2048, PyouyuuConstants.IMAGE_MAX_LONG_EDGE_PX)
        assertEquals(82, PyouyuuConstants.IMAGE_JPEG_QUALITY)
        assertEquals(0x3801, PyouyuuConstants.PTP_FORMAT_EXIF_JPEG)
    }

    @Test
    fun `classifyCameraModel detects legacy Sony a6000 series vs modern cameras`() {
        assertEquals(
            CameraAdvisoryState.LEGACY_SONY_PC_REMOTE_NO_SD_WRITE,
            PyouyuuDomainRules.classifyCameraModel("Sony ILCE-6000"),
        )
        assertEquals(
            CameraAdvisoryState.LEGACY_SONY_PC_REMOTE_NO_SD_WRITE,
            PyouyuuDomainRules.classifyCameraModel("ILCE-6300"),
        )
        assertEquals(
            CameraAdvisoryState.MODERN_DUAL_SAVE_TIP,
            PyouyuuDomainRules.classifyCameraModel("ILCE-6700"),
        )
        assertEquals(
            CameraAdvisoryState.MODERN_DUAL_SAVE_TIP,
            PyouyuuDomainRules.classifyCameraModel("Canon EOS R6"),
        )
    }

    @Test
    fun `buildGuestQrUrl formats exact pyouyuu guest URL`() {
        val uuid = "123e4567-e89b-12d3-a456-426614174000"
        assertEquals(
            "https://pyouyuu.com/p/123e4567-e89b-12d3-a456-426614174000",
            PyouyuuDomainRules.buildGuestQrUrl(uuid),
        )
    }

    @Test
    fun `FakePhotoRepository prepends newest photo for Kiosk preview`() = runTest {
        val repo = FakePhotoRepository()
        val p1 = CapturedPhoto(
            photoUuid = "uuid-1",
            eventId = null,
            fileName = "DSC0001.JPG",
            originalUri = "content://media/external/images/media/1",
            watermarkedCachePath = "/cache/uuid-1.jpg",
            widthPx = 2048,
            heightPx = 1365,
            takenAtEpochMs = 1000L,
        )
        val p2 = p1.copy(photoUuid = "uuid-2", fileName = "DSC0002.JPG", takenAtEpochMs = 2000L)

        repo.enqueueCapturedPhoto(p1)
        repo.enqueueCapturedPhoto(p2)

        assertEquals(listOf("uuid-2", "uuid-1"), repo.capturedPhotos.value.map { it.photoUuid })
    }
}
