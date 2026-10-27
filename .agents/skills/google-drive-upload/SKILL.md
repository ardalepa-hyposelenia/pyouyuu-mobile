---
name: google-drive-upload
description: Implement direct Android-to-Google-Drive REST API v3 folder creation (/Pyouyuu Events/{EventName}), multipart/resumable JPEG upload, and anyoneWithLink reader permission using only the drive.file OAuth scope. Use for any code under android/**/drive.
---

# Direct Google Drive API v3 Upload (Pyouyuu)

**Source of Truth:** `docs/SSOT.md` (D-10, D-20, D-23, D-24), `docs/RFC/RFC-001 - Technical Architecture Specification.md` §4.1, `docs/PRD.md` (FR-AUTH-03), `contracts/constants.json`.

## Hard Constraints
1. **No Google Drive Java Client SDK (`android/AGENTS.md`):** Do not add `google-api-services-drive` (bloated, legacy HttpTransport dependencies). Call Google Drive REST API v3 directly using OkHttp / Ktor with the Bearer access token obtained from `AuthorizationClient` (`android-google-auth` skill).
2. **Scope is strictly `DRIVE_SCOPE` (`https://www.googleapis.com/auth/drive.file`):** The app can only see and manage folders and files that the app itself created.
3. **Go Server Never Sees Image Bytes (D-10):** Binary uploads go strictly from Android → `www.googleapis.com/upload/drive/v3/files`.

## Procedure

### 1. Folder Hierarchy Creation (D-23, OQ-02)
Path: `/${DRIVE_ROOT_FOLDER_NAME}/{EventName}` (default `/Pyouyuu Events/Wedding Budi & Siti`). Always use the generated constant `DRIVE_ROOT_FOLDER_NAME` (`"Pyouyuu Events"`).
1. **Find or Create Root Folder (`DRIVE_ROOT_FOLDER_NAME`):**
   - `GET https://www.googleapis.com/drive/v3/files?q=mimeType='application/vnd.google-apps.folder' and name='Pyouyuu Events' and trashed=false&spaces=drive&fields=files(id,name)`
   - If empty, `POST https://www.googleapis.com/drive/v3/files` with JSON body:
     ```json
     {
       "name": "Pyouyuu Events",
       "mimeType": "application/vnd.google-apps.folder"
     }
     ```
2. **Create Event Subfolder (`{EventName}`):**
   - `POST https://www.googleapis.com/drive/v3/files?fields=id` with JSON body:
     ```json
     {
       "name": "<EventName>",
       "mimeType": "application/vnd.google-apps.folder",
       "parents": ["<rootFolderId>"]
     }
     ```
3. **Share Event Subfolder Publicly (`anyone` + `reader`):**
   - Creating a public `reader` permission on the **event subfolder** once (`POST https://www.googleapis.com/drive/v3/files/{folderId}/permissions` with `{ "role": "reader", "type": "anyone" }`) automatically makes files uploaded inside that parent folder readable by link (`lh3.googleusercontent.com/d/{fileId}=w1600` and `drive.google.com/uc?export=download&id={fileId}`), OR you can also set/verify permission per file if needed. Setting it on the parent folder saves 1 HTTP round-trip per photo during live shooting!

### 2. Direct Compressed JPEG Upload (D-10)
Since watermarked JPEGs are `~400KB–800KB` (`IMAGE_MAX_LONG_EDGE_PX = 2048`, `IMAGE_JPEG_QUALITY = 82`), use single-request `uploadType=multipart` (under 5MB threshold):
- Endpoint: `POST https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart&fields=id`
- Header: `Authorization: Bearer <drive_access_token>`
- Header: `Content-Type: multipart/related; boundary=<boundary>`
- Part 1 (`application/json; charset=UTF-8`):
  ```json
  {
    "name": "DSC0042.JPG",
    "parents": ["<drive_folder_id>"],
    "mimeType": "image/jpeg"
  }
  ```
- Part 2 (`image/jpeg`): raw bytes of `watermarkedCachePath`.
- Parse `{ "id": "<drive_file_id>" }` from the `200 OK` response.

### 3. Token Refresh & Account Switching (D-20)
- On `401 Unauthorized` from Drive API, request a fresh token via `AuthorizationClient.authorize()` for the bound `storage_google_email` and retry once.
- If the user switches their storage Gmail account in Settings (FR-AUTH-03), new events will create folders in the newly authorized Drive account while old events remain accessible via their existing Drive CDN links.
