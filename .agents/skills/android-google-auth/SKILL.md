---
name: android-google-auth
description: Implement decoupled Google authentication on Android: CredentialManager for primary photographer identity (POST /api/v1/auth/google + 1-device lock handling) and Play Services AuthorizationClient for secondary Google Drive storage account OAuth (drive.file scope). Use for any code under android/**/auth.
---

# Android Decoupled Google Auth & 1-Device Lock (Snapshea)

**Source of Truth:** `docs/SSOT.md` (D-14, D-15, D-19, D-20), `docs/RFC/RFC-001 - Technical Architecture Specification.md` §4.6, `docs/PRD.md` (FR-AUTH-01..04), `contracts/openapi.yaml`, `contracts/constants.json`.

## Core Architecture: Two Separate Google Flows (D-20)
Snapshea strictly decouples **App Identity** (who owns the credits/subscription) from **Google Drive Storage** (which 15GB Gmail bucket stores the event photos):

| Concern | Android API | Scope / Token | Can user switch mid-season? |
|---|---|---|---|
| **1. Primary App Identity** | `androidx.credentials.CredentialManager` (`GetGoogleIdOption`) | Google ID Token (`id_token`) exchanged at `POST /api/v1/auth/google` | Tied to `users` & `billing_transactions` in PostgreSQL |
| **2. Drive Photo Storage** | `com.google.android.gms.auth.api.identity.Identity.getAuthorizationClient` | OAuth2 Access Token with `DRIVE_SCOPE` (`drive.file` only) | Yes — photographer can rotate across secondary Gmail accounts when 15GB fills up (FR-AUTH-03) |

## Procedure

### 1. Lazy Login Boundary (D-14, D-19, FR-AUTH-01, FR-AUTH-02)
- Never show a login wall on app launch. USB camera tethering, local backup, and local watermark preview work 100% offline with zero login.
- Trigger Primary Sign-In **only** when the user taps **"Enable Cloud QR Sharing"** or **"Buy Event Pass / Pro"**.

### 2. Primary Identity Sign-In & 1-Device Lock (D-15, FR-AUTH-04)
1. Request Google ID token via `CredentialManager.getCredential()` using `GetGoogleIdOption.Builder().setServerClientId(serverClientId).build()`.
2. Read hardware ID: `Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)`.
3. Call `POST /api/v1/auth/google`:
   ```json
   {
     "id_token": "<google_id_token>",
     "android_id": "<android_id>",
     "force_device_switch": false
   }
   ```
4. **Handle `409 Conflict` (`ACTIVE_DEVICE_CONFLICT`):**
   - Show an interactive confirmation dialog: *"Your account is currently active on another phone. Switch active session to this device?"*
   - If the user confirms, retry `POST /api/v1/auth/google` with `"force_device_switch": true`.
5. **Persist Session & Headers:**
   - Store `access_token` in encrypted local storage.
   - Attach `Authorization: Bearer <access_token>` and `X-Android-Id: <android_id>` to all authenticated Go API calls (`contracts/openapi.yaml`).
   - If any subsequent API call returns `401` with `error.code == "DEVICE_REVOKED"`, pause cloud uploads and display a prompt allowing the user to re-claim the active device lock.

### 3. Decoupled Google Drive Authorization (D-20, FR-AUTH-03)
1. Build `AuthorizationRequest`:
   ```kotlin
   val request = AuthorizationRequest.builder()
       .setRequestedScopes(listOf(Scope(SnapsheaConstants.DRIVE_SCOPE)))
       .build()
   ```
2. Call `Identity.getAuthorizationClient(context).authorize(request)`.
3. If `authorizationResult.hasResolution()`, launch the resolution `IntentSenderRequest` via `rememberLauncherForActivityResult(StartIntentSenderForResult())` so the user can pick **any** Google account on the device (e.g. `storage02@gmail.com`) and grant `drive.file` access.
4. Extract `authorizationResult.accessToken` for Drive REST v3 calls (`google-drive-upload` skill) and store the chosen `storage_google_email` for `POST /api/v1/events`.
