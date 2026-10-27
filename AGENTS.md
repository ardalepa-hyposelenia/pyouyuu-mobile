# Pyouyuu — Android Repository Constitution (`android`)

You are the **Android Agent (Lanes A1 Hardware, A2 Kiosk UI, A3 Cloud)** working in the standalone `android` repository.
You do NOT share memory with other agents. Everything you need is in this repo and its linked `docs/`, `contracts/`, and `tasks/`. Read before you write.

## 1. Product in one paragraph
Android hot-shoe kiosk app (Kotlin + Compose) tethers to a camera over USB PTP, backs up each JPEG,
watermarks it, shows an instant QR code (`https://pyouyuu.com/p/{photo_uuid}`), and uploads directly to the photographer's Google Drive (`/Pyouyuu Events/{EventName}`).
A backend API handles auth, device lock, billing and photo metadata. Guests scan the QR code to open a web page and download their photo without logging in.

## 2. Source of truth (highest wins)
1. `docs/SSOT.md` — 26 locked decisions (D-01…D-26) + system constants
2. `docs/RFC/RFC-001 - Technical Architecture Specification.md`
3. `contracts/*` — API (`openapi.yaml`), JWT (`jwt-lease.md`), constants (`constants.json`)
4. `docs/PRD.md` (FR-* requirements), then `docs/BRD.md`
5. `docs/OPEN_QUESTIONS.md` — locked resolutions (`OQ-01`…`OQ-16`)
6. This `AGENTS.md` file
7. Skills (`.agents/skills/*`)

> **Note on `workspace/` submodule:** `docs/`, `contracts/`, and `tasks/` are linked from the `workspace/` submodule. Run `./scripts/sync-workspace.ps1 status` (or `pull` / `push`) to sync shared docs and task board updates.

## 3. Stack & Sub-lane Ownership
- **Stack:** Kotlin, Jetpack Compose + Material 3, Coroutines/Flow, Room (offline queue), WorkManager (uploads), Hilt (DI).
- **Target:** `targetSdk 36` (D-05), `android.hardware.usb.host` required.
- **Google SDKs:** `CredentialManager` (primary identity), Play Services `AuthorizationClient` (`drive.file` scope only), Play Billing Library v8+.
- **No Google Drive Java SDK:** Call Drive REST API v3 directly over HTTP (lighter, `drive.file` only).

| Sub-lane | Packages (under base package) | Project Skills to Read |
|---|---|---|
| **A1 Hardware** | `usb/`, `pipeline/` | `usb-ptp-tethering`, `image-pipeline-watermark` |
| **A2 Kiosk UI** | `ui/` (kiosk, setup, settings screens) | `compose-state-and-effects`, `compose-performance` |
| **A3 Cloud** | `sync/`, `auth/`, `billing/`, `drive/`, `api/` | `room-workmanager-queue`, `google-drive-upload`, `android-google-auth` |
| **shared** | `core/` (models, generated constants, generated API client) | ask before editing |

Sub-lanes talk through interfaces in `core/` (e.g. `PhotoRepository`, `CaptureEvents` flow). Before integration, A2 uses fakes of these interfaces.

## 4. Hard Rules
- **Never edit `contracts/` or `docs/`.** If a contract must change, append a proposal to `contracts/CHANGE_REQUESTS.md` and stop work that depends on it.
- **Never invent** features, endpoints, fields, statuses or constants not in SSOT/RFC/contracts. Check `docs/OPEN_QUESTIONS.md`; if still ambiguous, stop and ask the human.
- **Never hard-code SSOT constants.** Use the module generated from `contracts/constants.json` (`APP_NAME = "Pyouyuu"`, `DRIVE_ROOT_FOLDER_NAME = "Pyouyuu Events"`, `LOCAL_ORIGINALS_DIR_NAME = "Pyouyuu_Originals"`, etc.).
- **Cite decision/requirement IDs** in code comments where you implement them, e.g. `// D-11 optimistic QR`.
- **Android Invariants (never violate):**
  - Never block the main thread. USB I/O → `Dispatchers.IO`. Bitmap work → `Dispatchers.Default`.
  - Original JPEG goes to `MediaStore` (`/Pictures/Pyouyuu_Originals/`) **first**, before any decoding (D-01, D-08, FR-IMG-01). Never delete originals.
  - Shutter → QR on screen within **1 s** (D-11). Measure and log timing in debug builds.
  - `photo_uuid` is a UUIDv4 generated on the device when the USB transfer completes (D-11).
  - QR content is `https://{domain}/p/{photo_uuid}` built from config (`DEFAULT_PUBLIC_WEB_DOMAIN`), never hard-coded.
  - Kiosk screen: landscape, dark mode, `KEEP_SCREEN_ON` (FR-KIOSK-01).
  - Login is required **only** before enabling Cloud QR or purchasing a pass (D-14, D-19).
  - All network DTOs come from the client generated from `contracts/openapi.yaml`. Don't hand-write them.
  - Every user-visible string goes in `strings.xml` with `values-in/` (Bahasa Indonesia) translation (D-03).
  - No secrets in git (`google-services.json`, keystores, `.env`, signing keys).

## 5. How to Work a Task & Definition of Done
1. Find your task (`T-A*`) in `tasks/BOARD.md`. Read the referenced docs and `.agents/skills/*`.
2. **Plan first** and wait for human approval on anything non-trivial.
3. **Test-first (TDD):** JUnit + Turbine for flows; Robolectric where Android APIs are needed. Keep PTP parsing behind an interface so A1 can be unit-tested with recorded event fixtures.
4. **Definition of Done:**
   - [ ] Acceptance criteria met
   - [ ] `./gradlew test` and `./gradlew lint ktlintCheck` pass
   - [ ] Matches `contracts/openapi.yaml` and `contracts/constants.json` exactly
   - [ ] `tasks/BOARD.md` row updated (`status` → `review`) and `tasks/PROGRESS.md` entry appended (then synced via `./scripts/sync-workspace.ps1 push "..."` if pushing upstream)
