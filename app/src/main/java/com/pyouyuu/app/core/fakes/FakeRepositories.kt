package com.pyouyuu.app.core.fakes

import com.pyouyuu.app.core.model.CameraAdvisoryState
import com.pyouyuu.app.core.model.CaptureWarning
import com.pyouyuu.app.core.model.CapturedPhoto
import com.pyouyuu.app.core.repository.CameraTetherSource
import com.pyouyuu.app.core.repository.PhotoRepository
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Fake CameraTetherSource for Sub-lane A2 UI development and unit tests (android/AGENTS.md).
 */
class FakeCameraTetherSource : CameraTetherSource {
    private val _advisoryState = MutableStateFlow(CameraAdvisoryState.DISCONNECTED)
    override val advisoryState: StateFlow<CameraAdvisoryState> = _advisoryState.asStateFlow()

    private val _warnings = MutableSharedFlow<CaptureWarning>(
        extraBufferCapacity = 8,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    override val warnings: Flow<CaptureWarning> = _warnings.asSharedFlow()

    fun emitAdvisory(state: CameraAdvisoryState) {
        _advisoryState.value = state
    }

    fun emitWarning(warning: CaptureWarning) {
        _warnings.tryEmit(warning)
    }
}

/**
 * Fake PhotoRepository for Sub-lane A2 Kiosk UI development and unit tests.
 */
class FakePhotoRepository : PhotoRepository {
    private val _capturedPhotos = MutableStateFlow<List<CapturedPhoto>>(emptyList())
    override val capturedPhotos: StateFlow<List<CapturedPhoto>> = _capturedPhotos.asStateFlow()

    override suspend fun enqueueCapturedPhoto(photo: CapturedPhoto) {
        _capturedPhotos.update { current -> listOf(photo) + current }
    }
}
