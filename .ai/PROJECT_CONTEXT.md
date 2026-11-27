# AeTranzit — Project Context (Source of Truth)

## 1. Product Scope
- **App:** AeTranzit — Android bus ticketing & fleet operations app.
- **Users:** **Admin** and **Conductor** roles only.
- **Explicit Exclusions:**
  - No passenger self-booking flow.
  - No physical Bluetooth/USB thermal printer integration. Digital tickets are generated in-app and shared directly.

## 2. Tech Stack & Build Baseline
### Verified Project Setup
- **Gradle / AGP:** `9.5.3` (`app/build.gradle.kts`, `gradle/libs.versions.toml`)
- **Kotlin:** `2.2.10` (`kotlin-compose` plugin), KSP `2.3.14`
- **SDK:** `compileSdk = 37`, `targetSdk = 37`, `minSdk = 24`
- **Root Package / Application ID:** `com.aedev.aetranzit`
- **Firebase Project ID:** `aetranzit`
- **Branding Asset:** `res/drawable/logo.png` (`R.drawable.logo`)

### Core Stack
- **Architecture:** Clean Layered (`ui/` -> `domain/` <- `data/`), MVVM + `StateFlow`
- **UI:** Jetpack Compose (BOM `2026.02.01`), Material 3, Material Icons Extended, Google Fonts (`Plus Jakarta Sans` + `Inter` with tabular numerals `tnum`)
- **DI:** Hilt (`2.60.1`), `hilt-navigation-compose`, `hilt-work` (`1.4.0` + `hilt-compiler` via KSP)
- **Background Sync:** WorkManager (`androidx.work:work-runtime-ktx:2.12.0`) + `ConnectivityManager` network observer
- **Navigation:** Jetpack Navigation Compose
- **Local Cache & Offline Queue:** Room (`2.8.5`: `room-runtime`, `room-ktx`, `room-compiler` via KSP, schema `version = 3` with `MIGRATION_1_2` + `MIGRATION_2_3`)
- **Preferences:** Jetpack DataStore Preferences (`isCloudSyncEnabled` default **`true`**, `lastSyncedAt`, active `sessionUserId`)
- **Cloud SSOT:** Firebase BOM (`35.2.0`: `firebase-auth`, `firebase-firestore`) — shared multi-tenant Cloud Source of Truth per `companyId` with automatic two-way sync
- **Charts (Admin, Phase 5):** Vico (`3.3.1`)
- **QR Codes (Tickets, Phase 4):** ZXing (`3.5.4`)

## 3. Package Layout (`com.aedev.aetranzit`)
```text
com.aedev.aetranzit/
├── AeTranzitApp.kt      # @HiltAndroidApp + Configuration.Provider (HiltWorkerFactory) + auto-sync startup
├── MainActivity.kt
├── di/                  # Hilt modules (Database, DataStore, Firebase, Coroutines, SyncModule, RepositoryModule)
├── domain/
│   ├── model/           # Domain entities (Company, User, UserRole, Bus, Route, Trip, Ticket)
│   ├── repository/      # Interfaces (CompanyRepository, AuthRepository, SyncPreferencesRepository, ...)
│   ├── sync/            # SyncStatus, SyncSummary, TransientSyncException, ConnectivityObserver, SyncScheduler, SyncManager
│   └── usecase/         # Focused business rules (CalculateFareUseCase, BuildAdminAnalyticsUseCase, CompanyJoinCodeGenerator, ...)
├── data/
│   ├── local/           # Room v3 entities, DAOs, AeTranzitDatabase + MIGRATION_1_2 + MIGRATION_2_3
│   ├── preferences/     # DataStore preferences data source
│   ├── remote/          # Firebase Auth (incl. secondary staff_provisioner FirebaseApp) & Firestore two-way sync data sources + FirebaseSyncErrorMapper
│   ├── sync/            # SyncCoordinator, AeTranzitSyncWorker (@HiltWorker), WorkManagerSyncScheduler, NetworkConnectivityObserver
│   ├── mapper/          # Entity <-> Domain mappers
│   └── repository/      # Repository implementations (write Room -> immediate push + WorkManager sync trigger)
└── ui/
    ├── theme/           # Color.kt, Type.kt, Theme.kt, Shape.kt, Dimens.kt
    ├── navigation/      # AeTranzitRoutes (incl. postAuthRouteFor), SessionViewModel & role/setup/password-gated AeTranzitNavHost
    ├── components/      # Reusable stateless Compose components (SyncStatusBadge, AePrimaryButton, ...) + @Preview
    ├── auth/            # Splash, Login, Register (Admin-only), ForceChangePasswordScreen & ViewModels
    ├── admin/           # AdminDashboardScreen, setup/CompanySetupScreen, ManageBusesScreen, ManageRoutesScreen, staff/ManageStaffScreen, reports/AdminReportsScreen
    ├── conductor/       # ConductorHomeScreen, ConductorTicketingScreen, ticket/DigitalTicketScreen
    └── settings/        # SettingsScreen & SettingsViewModel (Auto Cloud Sync toggle, live status & two-way Sync Now)
```

## 4. Core Domain & Sync Rules
1. **Firebase Cloud SSOT + Seamless Offline Room Queue (Phase 8 / ADR-010):**
   - UI observes Room `Flow`s for instant zero-latency rendering and 100% offline resilience.
   - Local mutations write to Room (`isSynced = false`, `updatedAt = now`), attempt immediate cloud push when online, and enqueue `AeTranzitSyncWorker` via `WorkManagerSyncScheduler` (`NetworkType.CONNECTED`, exponential backoff).
   - `SyncCoordinator.syncAll()` pushes pending records in strict dependency order (`Company -> Users -> Buses -> Routes -> Trips -> Tickets`) before pulling company-scoped Firestore documents, upserting into Room **only if** `local == null || (local.isSynced && remote.updatedAt > local.updatedAt)`.
2. **Room Schema (v3 — Multi-Tenant + Forced Password Change):**
   - `companies` (`CompanyEntity`): `id`, `name`, `joinCode` (unique index, e.g. `VIC-4821`), `createdAt`, `isSynced`, `updatedAt`
   - `users` (`UserEntity`): `id`, `name`, `email`, `passwordHash`, `role` (`ADMIN`/`CONDUCTOR`), `companyId` (indexed, `""` allowed for newly registered Admin prior to Business Setup), `isActive`, `mustChangePassword`, `isSynced`, `updatedAt`
   - `buses` (`BusEntity`): `id`, `busNumber`, `plateNumber`, `totalSeats`, `columns`, `companyId` (indexed), `isSynced`, `updatedAt`
   - `routes` (`RouteEntity`): `id`, `origin`, `destination`, `baseFare`, `stopsJson`, `companyId` (indexed), `isSynced`, `updatedAt`
   - `trips` (`TripEntity`): `id`, `busId`, `routeId`, `conductorId`, `companyId` (indexed), `status`, `startedAt`, `endedAt`, `isSynced`, `updatedAt`
   - `tickets` (`TicketEntity`): `id`, `tripId`, `busId`, `conductorId`, `companyId` (indexed), `seatNumber`, `destination`, `passengerType`, `fareAmount`, `cashTendered`, `changeAmount`, `status`, `createdAt`, `isSynced`, `updatedAt`
   - Explicit `MIGRATION_1_2` + `MIGRATION_2_3` preserve all data across `v1 -> v2 -> v3`.
3. **Admin Onboarding, Separate Business Setup & Admin-Provisioned Staff (Phase 9 / ADR-011):**
   - Self-registration (`RegisterScreen`) is strictly **Create Admin Account** (`role = ADMIN`, `companyId = ""`, `mustChangePassword = false`), routing immediately to `CompanySetupScreen` (`COMPANY_SETUP`) where the Admin names their bus company (`CompanyEntity` + atomic founder `WriteBatch` to `companies/{companyId}` and `users/{uid}`).
   - Conductors and co-Admins are provisioned exclusively by an active `ADMIN` in `ManageStaffScreen` (`AuthRepository.createStaffAccount`) with a temporary password (`mustChangePassword = true`), using a temporary secondary `FirebaseApp` (`"staff_provisioner"`) so the Admin's session is never signed out.
   - Any user with `mustChangePassword == true` is routed to `ForceChangePasswordScreen` (`FORCE_CHANGE_PASSWORD`) on login/startup and cannot access home screens until they set a new password distinct from their temporary password.
   - All DAO queries, Firestore pull queries (`.whereEqualTo("companyId", companyId)`), and `firestore.rules` enforce strict `companyId` isolation and immutable `companyId` once assigned.
4. **Auth Session Linking & Role-Aware Pull Scoping:**
   - `AuthRepositoryImpl.login()` automatically links/restores `FirebaseAuth` (`ensureFirebaseAuthSession`) when `isCloudSyncEnabled` is `true` without blocking offline login when signal is unavailable.
   - `CONDUCTOR` pull queries on `trips` and `tickets` include `.whereEqualTo("conductorId", conductorId)` and skip `users` list queries to match `firestore.rules` with zero `PERMISSION_DENIED` errors.
5. **Fare, 20% Discount, Seat Map & Company-Branded Digital Ticket Rules (Phases 2–7):**
   - Live seat occupancy derived from active `tickets` per `tripId` (and synced to Firestore `trips/{tripId}.occupiedSeats` + `tickets/{ticketId}` atomically).
   - 20% discount (`0.80 * baseFare`) for discounted passenger types; change calculator rejects insufficient cash tendered.
   - Digital tickets (in-app UI, PNG Bitmap, SMS receipt, and `viewTicket` web receipt) prominently display the resolved **Bus Company Name** (fallback `"AeTranzit"`) with ZXing QR verification link and a subtle *"Powered by AeTranzit"* footer.
