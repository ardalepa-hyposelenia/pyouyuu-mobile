---
name: usb-ptp-tethering
description: Implement or debug USB PTP camera tethering on Android (UsbManager, MtpDevice, Sony a6000 PC Remote mode, 0x4002 ObjectAdded event loop, and 0x3801 EXIF/JPEG filtering). Use for any code under android/**/usb.
---

# USB PTP Tethering (Snapshea)

**Source of Truth:** `docs/SSOT.md` (D-01, D-05, D-06, D-26), `docs/RFC/RFC-001 - Technical Architecture Specification.md` §4.2, `docs/PRD.md` (FR-CAM-01, FR-CAM-02, FR-CAM-03), `contracts/constants.json`.

## Architectural Rules
1. **Never block the main thread:** All `UsbManager` and `MtpDevice` I/O must run on `Dispatchers.IO` inside a foreground service (`CameraTetherService` with `foregroundServiceType="connectedDevice"`).
2. **Testable Abstraction:** Keep `MtpDevice` and USB endpoint calls behind a narrow interface (e.g., `PtpSession` / `PtpDeviceSource`) so the event loop, model detection, and format gate can be unit-tested with recorded event fixtures without a physical camera attached.
3. **Use Generated Constants:** Always reference `PTP_EVENT_OBJECT_ADDED` (`0x4002`), `PTP_FORMAT_EXIF_JPEG` (`0x3801`), `PTP_FORMAT_SONY_ARW` (`0xB101`), and `LEGACY_SONY_MODEL_REGEX` from the generated constants module.

## Step-by-Step Procedure

### 1. USB Device Filter & Permission
- Declare `<uses-feature android:name="android.hardware.usb.host" android:required="true" />` in `AndroidManifest.xml` (D-05).
- In `res/xml/device_filter.xml`, filter for USB Still Image Capture class (`class="6" subclass="1" protocol="1"`).
- When `UsbManager.ACTION_USB_DEVICE_ATTACHED` arrives (or on startup scan of `usbManager.deviceList`), request permission if not already granted, then start `CameraTetherService`.

### 2. Handshake & Camera Model Advisory (FR-CAM-01, FR-CAM-02)
- Open `val connection = usbManager.openDevice(usbDevice)` and `val mtp = MtpDevice(usbDevice)`.
- Call `mtp.open(connection)` and read `val info = mtp.deviceInfo`.
- Inspect `info.manufacturer` and `info.model`:
  - Match against `LEGACY_SONY_MODEL_REGEX` (`(?i).*(ILCE-6000|ILCE-6300|ILCE-6500).*`).
  - If matched → emit `CameraAdvisoryState.LEGACY_SONY_PC_REMOTE_NO_SD_WRITE` (triggers the Amber Hardware Notice explaining that Sony firmware disables SD card writes in PC Remote mode and confirming automatic phone backup to `/Pictures/[AppName]_Originals/`).
  - Otherwise → emit `CameraAdvisoryState.MODERN_DUAL_SAVE_TIP` (triggers the Green Tip Banner advising `PC + Camera` save destination and `JPEG Standard/2M` transfer size).

### 3. Interrupt Event Loop & Format Gate (FR-CAM-03, D-26)
- Run a `while (currentCoroutineContext().isActive)` loop on `Dispatchers.IO` calling `mtp.readEvent(cancellationSignal)`.
- When `event.eventCode == PTP_EVENT_OBJECT_ADDED` (`0x4002`):
  1. Extract `val objectHandle = event.parameter1`.
  2. Query `val objectInfo = mtp.getObjectInfo(objectHandle)` (`GetObjectInfo 0x1008`).
  3. **Strict JPEG Gate (`0x3801`):**
     - If `objectInfo.format != PTP_FORMAT_EXIF_JPEG` (`0x3801`) — e.g. `0xB101` Sony `.ARW`, `.CR3`, `.NEF` — emit `CaptureWarning.RAW_SKIPPED_WARNING` (non-blocking UI toast) and **do not** pull the heavy RAW buffer into the image pipeline.
  4. **Pull JPEG Bytes:**
     - Call `val jpegBytes = mtp.getObject(objectHandle, objectInfo.compressedSize)`.
     - Immediately hand off `jpegBytes` and `objectInfo.name` to the Image Pipeline (`image-pipeline-watermark` skill) without blocking the next `readEvent()` iteration.

## Hardware Gotchas (Sony a6000 / ILCE-6000)
- **No SD Card Write in PC Remote:** The Sony a6000 disables its internal SD card slot when tethered via USB `PC Remote`. Saving the untouched original `jpegBytes` to Android `MediaStore` (`FR-IMG-01`) **before** any bitmap decoding is mandatory data-loss prevention.
- **Disconnect Recovery:** Always handle `IOException` / `OperationCanceledException` from `readEvent()` cleanly, close `MtpDevice` + `UsbDeviceConnection`, and transition UI state to `Disconnected` so replugging the OTG cable cleanly restarts the session.
