---
name: image-pipeline-watermark
description: Implement the local Android image pipeline (mandatory untouched original backup to MediaStore, EXIF-safe orientation rotation, 2048px resize, corner PNG watermark compositing, 82% JPEG encoding, and <1s optimistic UUID generation). Use for any code under android/**/pipeline.
---

# Local Image Pipeline & Watermarking (Snapshea)

**Source of Truth:** `docs/SSOT.md` (D-01, D-06, D-08, D-11), `docs/RFC/RFC-001 - Technical Architecture Specification.md` §4.1, `docs/PRD.md` (FR-IMG-01, FR-IMG-02), `contracts/constants.json`.

## Non-Negotiable Ordering (Architectural Invariants)
Every JPEG byte array arriving from `usb-ptp-tethering` **must** execute in this exact sequence:

1. **Generate `photoUuid = UUID.randomUUID().toString()`** at millisecond 0 (D-11).
2. **Save Untouched Original FIRST (D-01, D-08, FR-IMG-01, OQ-02):**
   - Stream the raw `jpegBytes` directly into Android `MediaStore.Images.Media` under `RELATIVE_PATH = "Pictures/${LOCAL_ORIGINALS_DIR_NAME}/"` (from generated constant `LOCAL_ORIGINALS_DIR_NAME = "Pyouyuu_Originals"`) with filename `{eventSlug}_{timestamp}_{shortUuid}.jpg` on `Dispatchers.IO`.
   - Use `IS_PENDING = 1` while writing bytes, then flip `IS_PENDING = 0`.
   - **Never** decode, rotate, or re-encode the original before writing it to `MediaStore`. If bitmap decoding fails or OOMs, the photographer's original photo is already safe on disk.
3. **Decode & EXIF-Safe Rotate on `Dispatchers.Default` (FR-IMG-02):**
   - Read `ExifInterface(ByteArrayInputStream(jpegBytes))` to get `TAG_ORIENTATION` (`ORIENTATION_ROTATE_90`, `180`, `270`, flip variants).
   - Use `BitmapFactory.Options` with `inSampleSize` pre-scaling so a 24MP (`6000x4000`) Sony a6000 JPEG is downsampled near `IMAGE_MAX_LONG_EDGE_PX` (`2048`) during decode rather than allocating a 96MB full-res `ARGB_8888` bitmap.
   - Apply a `Matrix` transformation that simultaneously:
     - Rotates upright according to EXIF orientation.
     - Scales so the longest edge equals `IMAGE_MAX_LONG_EDGE_PX` (`2048px`, unless the source image is smaller, in which case do not upscale).
4. **Composite `.png` Watermark (D-08, FR-IMG-02):**
   - If the photographer configured a `.png` watermark:
     - Compute `shortestSide = min(bitmap.width, bitmap.height)`.
     - Target watermark width = `shortestSide * WATERMARK_REL_WIDTH` (`0.18`), preserving the logo's aspect ratio.
     - Margin padding = `shortestSide * WATERMARK_PADDING_REL` (`0.03`).
     - Position according to `WatermarkPosition` (`TOP_LEFT`, `TOP_RIGHT`, `BOTTOM_LEFT`, `BOTTOM_RIGHT`, `BOTTOM_CENTER`).
     - Draw onto the mutable upright `Bitmap` via `Canvas` with `Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)`.
5. **Compress & Write to Local Cache:**
   - Compress to `Bitmap.CompressFormat.JPEG` at `IMAGE_JPEG_QUALITY` (`82`) into `context.filesDir/watermarked/{photoUuid}.jpg`.
   - Recycle intermediate bitmaps immediately to keep heap usage flat during rapid burst shooting.
6. **Emit Optimistic Kiosk Event & Enqueue (< 1 second total budget, D-11):**
   - Emit the processed item (`photoUuid`, `originalUri`, `watermarkedCachePath`, `widthPx`, `heightPx`) so the Kiosk UI immediately displays the photo + QR code (`https://{domain}/p/{photoUuid}`) and the Room upload queue (`room-workmanager-queue`) starts background sync.
   - Log elapsed pipeline duration (`shutterToQrMs`) in debug builds.
