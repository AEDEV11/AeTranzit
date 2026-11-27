# AeTranzit — Architecture Decision Records (ADR Log)

> Log every major schema, navigation, or library decision with *Why* it was chosen so future sessions never contradict or overwrite past architecture.

---

## ADR-001: Single-Module Clean Layered Architecture (`ui/` -> `domain/` <- `data/`)
- **Status:** Accepted
- **Decision:** Keep a single `:app` Android module organized cleanly by layers (`ui`, `domain`, `data`, `di`), using MVVM + `StateFlow` and Hilt for dependency injection.
- **Why:** Avoids premature multi-module Gradle overhead while preserving strict separation of concerns (ViewModels depend on `domain/repository` interfaces, never directly on Room DAOs or Firebase SDKs).

## ADR-002: Room Database as Offline-First Cache/Queue + Firestore Sync
- **Status:** Superseded in part by **ADR-010** (Phase 8 promotes Firestore to shared Cloud SSOT per company with `isCloudSyncEnabled = true` by default while preserving Room as the fast local cache and offline write queue)
- **Decision:** Use **Room Database** as the primary local Single Source of Truth (SSOT) for all domain entities (`UserEntity`, `BusEntity`, `RouteEntity`, `TripEntity`, `TicketEntity`), with every entity carrying `isSynced: Boolean = false` and `updatedAt: Long`. Cloud sync to Firebase project `aetranzit` (Auth + Firestore) is **optional**, toggled via Jetpack DataStore (`isCloudSyncEnabled`, default `false`) plus manual "Sync Now".
- **Why:** Buses operate across prolonged cellular dead zones. Room guarantees 100% offline reliability, zero login lockouts without internet, and deterministic local queries, while `isSynced`/`updatedAt` enables clean incremental cloud sync when enabled.

## ADR-003: Role-Separated App (`ADMIN` & `CONDUCTOR` Only)
- **Status:** Accepted
- **Decision:** Gate entry at authentication (`ADMIN` vs `CONDUCTOR`) and enforce separation in Jetpack Navigation Compose (and Firestore Security Rules when cloud sync is active). No passenger booking mode.
- **Why:** Reduces UI complexity for moving-bus operations and prevents unauthorized fare/trip tampering from conductor devices.

## ADR-004: Digital-Only Ticketing (ZXing QR + Bitmap Share Sheet / SMS)
- **Status:** Accepted
- **Decision:** Omit physical thermal printer SDKs/Bluetooth ESC-POS support. Generate tickets digitally using Compose/Canvas -> `Bitmap` with an embedded **ZXing** QR code, shareable via Android `Intent.ACTION_SEND` or SMS.
- **Why:** Eliminates printer pairing failures on buses and reduces hardware dependency.

## ADR-005: Vico for Admin Analytics Charts
- **Status:** Accepted
- **Decision:** Use **Vico** Compose charting library for Admin revenue, occupancy, and route analytics.
- **Why:** Native Jetpack Compose support, Material 3 styling compatibility, and lightweight footprint.

## ADR-006: Package Namespace Confirmed (`com.aedev.aetranzit`)
- **Status:** Accepted
- **Decision:** Use `com.aedev.aetranzit` across Gradle `namespace`/`applicationId`, Kotlin sources, and Firebase Android app registration in project `aetranzit`.
- **Why:** Matches the existing Android project configuration and prevents unnecessary refactoring.

## ADR-007: Brand Design System & Google Fonts (`Plus Jakarta Sans` + `Inter`)
- **Status:** Accepted
- **Decision:** Centralize Material 3 tokens in `ui/theme/` using Deep Navy (`#0A192F`), Electric Blue (`#007BFF`), and Teal (`#00C9A7`) high-contrast light/dark schemes, paired with downloadable Google Fonts (`Plus Jakarta Sans` for headings/UI, `Inter` for body/labels, and `tnum` tabular numerals for fares/amounts) with clean local fallback fonts.
- **Why:** Maximizes sunlight legibility on moving buses and keeps currency/fare columns aligned without jitter.

## ADR-008: Server-Side Web Ticket Receipt (`viewTicket` Cloud Function v2) + Zero Public Firestore Reads
- **Status:** Accepted
- **Decision:** Encode direct web verification links (`/t/${ticket.id}`) in ticket QR codes and SMS receipts, routed via Firebase Hosting rewrites to an HTTP v2 Cloud Function (`viewTicket`, Node.js 20) using `firebase-admin`, while keeping `firestore.rules` strictly closed to unauthenticated reads.
- **Why:** Allows passengers without the app to view and print/save an official digital receipt by scanning the QR code, without exposing the `tickets`, `trips`, `buses`, or `users` collections to public client queries or enumeration attacks.

## ADR-009: Multi-Tenant Company Isolation (`companies` + `companyId`) & Room v2 Migration
- **Status:** Accepted
- **Decision:**
  1. Introduce `CompanyEntity` (`companies`, unique `joinCode` e.g. `VIC-4821`) and an indexed `companyId: String` column across `users`, `buses`, `routes`, `trips`, and `tickets`.
  2. Bump `AeTranzitDatabase` from `version = 1` to `version = 2` with an explicit `MIGRATION_1_2` that creates `companies`, seeds legacy default company `default-company` (`"AeTranzit Fleet"`, `"AET-0001"`), and backfills `companyId = 'default-company'` + `index_<table>_companyId` indices with zero data loss.
  3. Scope all DAO observation/list/mutation/unsynced queries by `WHERE companyId = :companyId`, resolved reactively via `ActiveCompanyProvider` from the signed-in user session.
  4. Onboard `ADMIN` accounts by creating a new `CompanyEntity` + generated Join Code (synced atomically via `WriteBatch` with `ownerUid = request.auth.uid` to prevent cross-tenant Admin privilege escalation), and onboard `CONDUCTOR` accounts by validating a required `Company Join Code` locally or via Firestore (`companies` query) before account creation.
  5. Brand Digital Tickets (in-app UI, PNG Bitmap, SMS receipt, and `viewTicket` Cloud Function HTML receipt) with the resolved Bus Company Name and a subtle `"Powered by AeTranzit"` footer, and enforce `companyId == userCompanyId()` (immutable on update) across all collections in `firestore.rules`.
- **Why:** Enables multiple independent bus operators (e.g., Victory Liner, Genesis Transport) to run AeTranzit concurrently with strict local and cloud tenant isolation, frictionless join-code conductor onboarding, and operator-specific passenger receipts.

## ADR-010: Firebase Cloud SSOT per Bus Company + Room Offline Queue & WorkManager Two-Way Auto-Sync
- **Status:** Accepted (Updates ADR-002)
- **Decision:**
  1. Promote **Firebase Cloud Firestore** (`aetranzit`) to the shared **Cloud Source of Truth** per `companyId` with `isCloudSyncEnabled` defaulting to **`true`** in `SyncPreferencesRepositoryImpl`, while keeping **Room** as the zero-latency local read cache and offline write queue (`isSynced: Boolean`, `updatedAt: Long`).
  2. Integrate **WorkManager** (`androidx.work:work-runtime-ktx:2.12.0`) + **Hilt Work** (`androidx.hilt:hilt-work:1.4.0` via `HiltWorkerFactory` on `AeTranzitApp`, with default `WorkManagerInitializer` disabled in `AndroidManifest.xml`).
  3. Implement **`SyncCoordinator`** (`@Singleton` implementing `SyncManager`) executing two-way sync inside a `Mutex`:
     - **Push Phase (Strict Dependency Order):** `Company -> Users -> Buses -> Routes -> Trips -> Tickets`, marking each Room entity `isSynced = true` only after Firestore confirms write success.
     - **Pull Phase (Role- & Tenant-Scoped):** Queries include `.whereEqualTo("companyId", companyId)` (plus `.whereEqualTo("conductorId", conductorId)` for `CONDUCTOR` on `trips`/`tickets`, and `fetchUsersByCompany` restricted to `ADMIN`) with zero `.orderBy(...)` clauses so single-field index merging handles all queries without composite-index failures. Pulled rows upsert into Room **only if** `local == null || (local.isSynced && remote.updatedAt > local.updatedAt)` — never overwriting unsynced local mutations.
  4. Automatically link `FirebaseAuth` sessions during `AuthRepositoryImpl.login()` (`ensureFirebaseAuthSession`) when `isCloudSyncEnabled` is `true` without blocking offline Room login on network errors, eliminating silent `PERMISSION_DENIED` states after offline registration.
  5. Schedule `OneTimeWorkRequest` (`NetworkType.CONNECTED`, exponential backoff, `Result.retry()` on `TransientSyncException`) via `WorkManagerSyncScheduler` after every local mutation and on `offline -> online` transitions (`NetworkConnectivityObserver`), plus a 15-minute `PeriodicWorkRequest`.
- **Why:** Allows Admins and Conductors across multiple devices in the same bus company to share live fleet, route, trip, staff, and ticket state automatically whenever signal is available, while guaranteeing 100% uninterrupted offline ticketing in cellular dead zones.

## ADR-011: Admin-Only Registration, Separate Business Setup & Secondary-`FirebaseApp` Staff Provisioning with Forced Password Change
- **Status:** Accepted (Updates onboarding portions of ADR-009)
- **Decision:**
  1. **Room Schema v3 (`MIGRATION_2_3`):** Add `mustChangePassword: Boolean = false` (`INTEGER NOT NULL DEFAULT 0`) to `UserEntity` and `User`, while keeping `MIGRATION_1_2` intact for seamless `v1 -> v2 -> v3` upgrades.
  2. **Admin-Only Self-Registration + Deferred Business Setup:** `RegisterScreen` creates `ADMIN` accounts exclusively (`companyId = ""`, `mustChangePassword = false`), then routes to a dedicated `CompanySetupScreen` (`COMPANY_SETUP`). `AeTranzitRoutes.postAuthRouteFor(user)` enforces `FORCE_CHANGE_PASSWORD` and `COMPANY_SETUP` gates across `SplashScreen`, `LoginScreen`, `RegisterScreen`, and `AeTranzitNavHost` so unconfigured Admins cannot enter `AdminDashboardScreen`.
  3. **Admin-Provisioned Staff via Secondary `FirebaseApp`:** Remove public Conductor self-registration and join-code invite banners. Admins provision `CONDUCTOR` and co-`ADMIN` accounts directly in `ManageStaffScreen` with a generated/entered temporary password (`mustChangePassword = true`) and a one-tap `"Copy Credentials"` summary. When Cloud Sync is enabled, `FirebaseAuthSyncDataSource.provisionStaffAccount` initializes a temporary secondary `FirebaseApp` (`"staff_provisioner"`) to create the Firebase Auth user without signing out the active Admin, writes `users/{newUid}` via the primary `FirebaseFirestore` instance, and cleans up (`signOut()` + `delete()`) inside a `finally` block.
  4. **Mandatory First-Login Password Change:** Any authenticated account with `mustChangePassword == true` is locked into `ForceChangePasswordScreen` (`BackHandler` blocked, Logout available) until they set a new password (`>= 6` chars, verified distinct from the temporary password), updating Room, `FirebaseAuth.currentUser.updatePassword(...)`, and `users/{uid}` (`mustChangePassword: false`).
  5. **Hardened `firestore.rules`:** Validate `mustChangePassword: bool` on `users/{uid}`, permit `companyId == ""` only for unconfigured `ADMIN` accounts, allow a one-time `"" -> founder companyId` update verified via `isCompanyFounder(...)`, keep `companyId` strictly immutable thereafter, and allow active users to clear `mustChangePassword` (`true -> false`) on their own profile.
- **Why:** Eliminates leaked Join Code risks where unauthorized external users could self-register into a fleet company, simplifies Admin registration into clean two-step onboarding, keeps the Admin signed in while creating staff accounts, and guarantees staff replace temporary credentials on first login.
