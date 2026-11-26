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
- **DI:** Hilt (`2.60.1`) + `hilt-navigation-compose`
- **Navigation:** Jetpack Navigation Compose
- **Local SSOT (Offline-First):** Room (`2.8.5`: `room-runtime`, `room-ktx`, `room-compiler` via KSP)
- **Preferences:** Jetpack DataStore Preferences (`isCloudSyncEnabled` toggle, `lastSyncedAt`, active `sessionUserId`)
- **Optional Cloud Sync:** Firebase BOM (`35.2.0`: `firebase-auth`, `firebase-firestore`) — sync runs only when `cloudSyncEnabled` is enabled or via manual "Sync Now"
- **Charts (Admin, Phase 5):** Vico
- **QR Codes (Tickets, Phase 4):** ZXing

## 3. Package Layout (`com.aedev.aetranzit`)
```text
com.aedev.aetranzit/
├── AeTranzitApp.kt      # @HiltAndroidApp
├── MainActivity.kt
├── di/                  # Hilt modules (Database, DataStore, Firebase, Coroutines, Repository bindings)
├── domain/
│   ├── model/           # Domain entities (User, UserRole, Bus, Route, Trip, Ticket)
│   ├── repository/      # Interfaces (AuthRepository, SyncPreferencesRepository, ...)
│   └── usecase/         # Focused business rules
├── data/
│   ├── local/           # Room entities, DAOs, AeTranzitDatabase
│   ├── preferences/     # DataStore preferences data source
│   ├── remote/          # Optional Firebase Auth & Firestore sync helpers
│   ├── mapper/          # Entity <-> Domain mappers
│   └── repository/      # Repository implementations
└── ui/
    ├── theme/           # Color.kt, Type.kt, Theme.kt, Shape.kt, Dimens.kt
    ├── navigation/      # AeTranzitRoutes & role-gated AeTranzitNavHost
    ├── components/      # Reusable stateless Compose components + @Preview
    ├── auth/            # Splash, Login, Register screens & AuthViewModel
    ├── admin/           # AdminDashboardScreen (shell in Phase 1)
    ├── conductor/       # ConductorHomeScreen (shell in Phase 1)
    └── settings/        # SettingsScreen (Cloud Sync toggle & Sync Now status)
```

## 4. Core Domain & Sync Rules
1. **Room as Offline-First SSOT:**
   - All reads/writes operate against Room first so the app works 100% offline by default.
   - Every Room entity includes `isSynced: Boolean = false` and `updatedAt: Long` (epoch millis) for clean incremental background/manual syncing.
2. **Room Schema (Phase 1 Entities):**
   - `users` (`UserEntity`): `id`, `name`, `email`, `passwordHash`, `role` (`ADMIN`/`CONDUCTOR`), `isActive`, `isSynced`, `updatedAt`
   - `buses` (`BusEntity`): `id`, `busNumber`, `plateNumber`, `totalSeats`, `columns`, `isSynced`, `updatedAt`
   - `routes` (`RouteEntity`): `id`, `origin`, `destination`, `baseFare`, `stopsJson`, `isSynced`, `updatedAt`
   - `trips` (`TripEntity`): `id`, `busId`, `routeId`, `conductorId`, `status`, `startedAt`, `endedAt`, `isSynced`, `updatedAt`
   - `tickets` (`TicketEntity`): `id`, `tripId`, `busId`, `conductorId`, `seatNumber`, `destination`, `passengerType`, `fareAmount`, `cashTendered`, `changeAmount`, `status`, `createdAt`, `isSynced`, `updatedAt`
3. **Optional Cloud Sync (`aetranzit`):**
   - Controlled by DataStore preference `isCloudSyncEnabled` (default `false`).
   - When disabled (or when `google-services.json` is absent / device is offline), auth and ticketing run strictly on Room without failing.
4. **Role-Based Access (`ADMIN` vs `CONDUCTOR`):**
   - Authenticated user resolves role from `UserEntity.role` (and `users/{uid}.role` when cloud sync is active).
   - Navigation strictly separates Admin capabilities from Conductor capabilities.
5. **Fare, 20% Discount, Seat Map & Digital Ticket Rules (Phases 2–4):**
   - Live seat occupancy derived from active `tickets` per `tripId` (and synced to Firestore `trips/{tripId}.occupiedSeats` + `tickets/{ticketId}` atomically when cloud sync is active).
   - 20% discount (`0.80 * baseFare`) for discounted passenger types; change calculator rejects insufficient cash tendered.
   - Digital tickets include ZXing QR code, shareable as Bitmap / Share Sheet / SMS.
