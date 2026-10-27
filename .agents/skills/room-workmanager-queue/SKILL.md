---
name: room-workmanager-queue
description: Implement the offline-resilient Room UploadQueueEntity and WorkManager 4-stage sync state machine (LOCAL_ONLY -> INIT_SENT_TO_GO -> UPLOADED_TO_DRIVE -> COMPLETED_ON_GO). Use for any code under android/**/sync.
---

# Room + WorkManager Offline Upload Queue (Snapshea)

**Source of Truth:** `docs/SSOT.md` (D-06, D-10, D-11, D-14, D-17, D-22), `docs/RFC/RFC-001 - Technical Architecture Specification.md` §4.6 & §4.7.

## Purpose
Ballrooms and event venues frequently have dead or congested cellular signal. The Kiosk displays the QR code (`https://{domain}/p/{photo_uuid}`) in `< 1 second` while this queue guarantees every photo eventually reaches Google Drive and the Go backend with **zero duplicate binary uploads** and **zero double-counted free trial quota**.

## Room Schema (`UploadQueueEntity` — RFC §4.7)

| Column | Type | Notes |
|---|---|---|
| `photoUuid` | `String` (`@PrimaryKey`) | Client-generated UUIDv4 at capture time (D-11) |
| `eventId` | `String?` | `null` when shooting in No-Login Local Mode (D-14) |
| `originalUri` | `String` | MediaStore URI of untouched original |
| `watermarkedCachePath` | `String` | Local file path of compressed + watermarked JPEG |
| `widthPx` | `Int` | Final upright width (e.g. `2048`) |
| `heightPx` | `Int` | Final upright height (e.g. `1365`) |
| `fileName` | `String` | Original camera file name (e.g. `DSC0042.JPG`) |
| `takenAtEpochMs` | `Long` | Device capture timestamp (`OQ-16`) |
| `syncStage` | `SyncStage` | `LOCAL_ONLY`, `INIT_SENT_TO_GO`, `UPLOADED_TO_DRIVE`, `COMPLETED_ON_GO`, `FAILED_PAYWALL` |
| `driveFileId` | `String?` | Persisted immediately after Drive upload succeeds |
| `retryCount` | `Int` | Incremented on transient network failure |

## 4-Stage Resumable State Machine (Worker Execution)

When `PhotoSyncWorker` runs (with `NetworkType.CONNECTED` constraint and `BackoffPolicy.EXPONENTIAL`):

### Pre-Flight Gate (Local Mode & Entitlement)
- If Cloud QR Sharing is disabled or user is not logged in (D-14) → keep at `LOCAL_ONLY` and do not enqueue network work.
- If an Event Session Lease JWT exists locally (D-17): verify `now < exp` (`contracts/jwt-lease.md`).

### Stage 1: `LOCAL_ONLY` → `INIT_SENT_TO_GO`
- Call `POST /api/v1/photos/init` with `{ photo_uuid, event_id, file_name, width_px, height_px, taken_at }` and optional `X-Event-Session` header.
- **201 Created** or **200 OK** (idempotent replay): update Room `syncStage = INIT_SENT_TO_GO`.
- **402 Payment Required (`FREE_QUOTA_EXHAUSTED`):** mark `syncStage = FAILED_PAYWALL`, emit UI state to prompt Event Pass purchase/activation, and return `Result.failure()` (do not spin-retry 402).
- **401 (`DEVICE_REVOKED`):** halt sync and notify Auth manager to show Device Switched dialog (D-15).
- **5xx / IOException:** increment `retryCount` and return `Result.retry()`.

### Stage 2: `INIT_SENT_TO_GO` → `UPLOADED_TO_DRIVE`
- If `driveFileId == null`:
  - Upload `watermarkedCachePath` directly to Google Drive API v3 (`google-drive-upload` skill) into the event's `drive_folder_id` and set reader permission.
  - **Crucial:** The instant Drive returns `fileId` and permission is set, persist `driveFileId = fileId` and `syncStage = UPLOADED_TO_DRIVE` in Room **before** calling Go `/complete`. That way, if Stage 3 times out, retries skip the 600KB binary upload.

### Stage 3: `UPLOADED_TO_DRIVE` → `COMPLETED_ON_GO`
- Call `PATCH /api/v1/photos/{photo_uuid}/complete` with `{ drive_file_id: driveFileId }`.
- On **200 OK**: update `syncStage = COMPLETED_ON_GO` and return `Result.success()`.
- On transient error: return `Result.retry()` (resumes directly at Stage 3 because `driveFileId` is already saved).
