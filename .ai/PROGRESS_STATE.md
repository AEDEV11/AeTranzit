# AeTranzit — Progress State (Anti-Context-Loss Tracker)

> **Mandatory Rule:** Update this file at the end of every completed task or before context compaction so a fresh session can resume with 100% accuracy and zero amnesia.

## Current Phase
- **Phase 5: Admin Dashboard (Vico Analytics, Reports CSV/PDF Export & Staff Management)** — **Completed**
- **Next Up:** **Phase 6: Security Rules, Edge-Case QA & Verification**

## Completed Features
- [x] **Phase 0:** Context Engineering & Agent Guardrails (`AGENTS.md`, `.ai/*`, `.ai/skills/*`)
- [x] **Phase 1: Auth & Setup**
  - [x] Gradle dependencies configured (`Room 2.8.5` + KSP `2.3.14`, `Hilt 2.60.1`, `Navigation Compose 2.10.2`, `DataStore 1.3.0`, `Firebase BOM 35.2.0` + `google-services.json` for `aetranzit`, Google Fonts `ui-text-google-fonts`)
  - [x] Design system (`ui/theme/`): High-contrast Deep Navy `#0A192F`, Electric Blue `#007BFF`, Teal `#00C9A7`, `Plus Jakarta Sans` + `Inter` + `TabularAmountStyle` (`tnum`), `AeDimens`, `AeTranzitShapes`
  - [x] Room database (`AeTranzitDatabase` v1 + schema export) with `UserEntity`, `BusEntity`, `RouteEntity`, `TripEntity`, `TicketEntity` (all carrying `isSynced` + `updatedAt`) and DAOs
  - [x] `SyncPreferencesRepository` & `AuthRepository` (Room SSOT + salted SHA-256 `PasswordHasher` + optional Firebase Auth/Firestore sync)
  - [x] `SplashScreen`, `LoginScreen`, `RegisterScreen` (`R.drawable.logo`, role selector, validation, `@Preview`s)
  - [x] Role-gated `AeTranzitNavHost` & `SettingsScreen`
- [x] **Phase 2: Core Domain, Bus/Route Management & Active Trip Flow**
  - [x] Transparent `RGBA` brand asset cleanup on `app/src/main/res/drawable/logo.png` (1024x1024 `1:1` square, exterior flood-fill with de-fringed edges, enclosed white headlights/windshield preserved)
  - [x] Domain models (`Bus`, `RouteStop`, `Route`, `TripStatus`, `Trip`) & Entity <-> Domain mappers (`BusMapper`, `RouteMapper` with zero-dependency RFC-8259 `RouteStopsJsonCodec`, `TripMapper`)
  - [x] DAO enhancements (`BusDao`, `RouteDao`, `TripDao` at Room DB `v1`), `FleetSyncRemoteDataSource` / `FirebaseFleetSyncDataSource`, and `BusRepository`, `RouteRepository`, `TripRepository` (single active trip enforcement per conductor & active-trip deletion protection)
  - [x] `SettingsViewModel` & `SettingsScreen` upgraded to display pending local changes count and sync Users, Buses, Routes, and Trips on manual "Sync Now"
  - [x] Admin Fleet & Route Setup: `AdminDashboardScreen` (+ `AdminDashboardViewModel`), `ManageBusesScreen` (+ `ManageBusesViewModel`), and `ManageRoutesScreen` (+ `ManageRoutesViewModel` with dynamic intermediate stops)
  - [x] Conductor Shift Flow: `ConductorHomeScreen` (+ `ConductorHomeViewModel` with sample fleet/route seeding, Start New Trip selector, Active Trip summary card, End Trip confirmation dialog)
  - [x] Unit tests (`RouteMapperTest`, `TripRepositoryImplTest`, `ManageBusesViewModelTest`, `ManageRoutesViewModelTest`)
- [x] **Phase 3: Conductor Ticketing Flow (Seat Map, Fare/20% Discount/Change Calc, Atomic Issuance)**
  - [x] Domain models (`PassengerType`, `TicketStatus`, `Ticket`), `TicketMapper`, and pure `CalculateFareUseCase` (`HALF_UP` 2-decimal rounding, 20% discount for `STUDENT`/`SENIOR`/`PWD`, manual override, live change & shortfall calculation)
  - [x] `TicketDao` & `TripDao` queries (Room DB kept at `v1`), `DatabaseTransactionRunner` (`database.withTransaction`), atomic Firestore transaction sync (`tickets/{ticketId}` + `trips/{tripId}.occupiedSeats`) in `FirebaseFleetSyncDataSource`, and `TicketRepository` / `TicketRepositoryImpl` (atomic seat double-booking prevention, drop-off freeing seat, offline-first sync)
  - [x] `SettingsViewModel` & `SettingsScreen` updated to count and sync pending `Tickets` alongside Users, Buses, Routes, and Trips
  - [x] `ConductorTicketingScreen` & `ConductorTicketingViewModel` replacing `ConductorTicketingPlaceholderScreen` (deleted): Top Trip Summary Bar (`TabularAmountStyle` + `formatPhp`), dynamic `2x2` & `2x3` interactive seat map (`>= 48.dp` touch targets, accessible `Available`/`Selected`/`Occupied` states), Issue Ticket sheet, confirmation banner, and Occupied Seat Details sheet
  - [x] Unit tests (`CalculateFareUseCaseTest`, `TicketRepositoryImplTest`, `ConductorTicketingViewModelTest`)
- [x] **Phase 4: Digital Ticket Rendering & Sharing (Bitmap, Share Sheet, SMS, ZXing QR)**
  - [x] Added **ZXing Core `3.5.4`** (`com.google.zxing:core`) in `gradle/libs.versions.toml` & `app/build.gradle.kts` (`compileSdk` updated to `37.1` to satisfy Compose BOM `2026.11.01`)
  - [x] Configured `androidx.core.content.FileProvider` (`${applicationId}.fileprovider`) in `AndroidManifest.xml` + `app/src/main/res/xml/file_paths.xml` (`<cache-path name="shared_tickets" path="tickets/" />`)
  - [x] Added reactive `TicketDao.observeById` & `TicketRepository.observeTicketById(ticketId)` (keeping Room DB at `version = 1`)
  - [x] Implemented `TicketPayloadFormatter` (`buildQrPayload` + `buildSmsReceiptText`), `QrCodeGenerator` (ZXing `QRCodeWriter`, `ErrorCorrectionLevel.M`, off-main-thread `Bitmap` + pure `BitMatrix` encoder), `DigitalTicketBitmapRenderer` (self-contained 1080x1660 Android `Canvas` receipt PNG with `R.drawable.logo`, perforated divider, fare summary, and QR code), and `TicketShareHelper` (`FileProvider` `Intent.ACTION_SEND` `image/png` + `smsto:` SMS receipt intent with `ActivityNotFoundException` handling)
  - [x] Built `DigitalTicketViewModel` & `DigitalTicketScreen` (`ui/conductor/ticket/`): sunlight-legible ticket card, pure-white QR code surface in both Light & Dark themes, `>= 48.dp` one-tap action bar (**Share Ticket Image**, **Send SMS Receipt**, **Back to Seat Map**), error banner/snackbar, and Light + Dark `@Preview`s
  - [x] Wired `CONDUCTOR_DIGITAL_TICKET` (`conductor_digital_ticket/{ticketId}`) in `AeTranzitRoutes` & `AeTranzitNavHost` (strictly role-gated to `CONDUCTOR`) and connected `ConductorTicketingScreen(onOpenDigitalTicket = ...)`
  - [x] Added unit tests (`TicketPayloadFormatterTest`, `QrCodeGeneratorTest` with ZXing `QRCodeReader` round-trip verification, `DigitalTicketViewModelTest`, and `TicketRepositoryImplTest` `observeTicketById` test) — **38/38 total unit tests passing** + `:app:assembleDebug` **BUILD SUCCESSFUL**
- [x] **Phase 5: Admin Analytics (Vico Charts), Reports (CSV/PDF Export) & Staff Management**
  - [x] Added **Vico Compose M3 `3.3.1`** (`com.patrykandpatrick.vico:compose` + `com.patrykandpatrick.vico:compose-m3`) to `gradle/libs.versions.toml` and `app/build.gradle.kts`
  - [x] Added `<cache-path name="shared_reports" path="reports/" />` to `app/src/main/res/xml/file_paths.xml` for `FileProvider` CSV/PDF sharing
  - [x] Extended `UserDao` (`observeAll`, `updateActiveStatus`), `TripDao` (`observeAll`), `TicketDao` (`observeAll`), `AuthRepository` / `AuthRepositoryImpl` (`observeAllUsers`, `setUserActiveStatus` with self-deactivation guard + optional Firestore sync), `TripRepository` / `TripRepositoryImpl` (`observeAllTrips`), and `TicketRepository` / `TicketRepositoryImpl` (`observeAllTickets`) while keeping Room DB at `version = 1`
  - [x] Implemented pure JVM-testable `AnalyticsDateRange`, `BuildAdminAnalyticsUseCase` (date-range filtering, revenue, ticket counts, discounted share, active trips/load factor, chronological daily revenue points, ranked top destinations, conductor summaries), `ReportExportFormatter` (RFC-4180 CSV builder), and `ReportFileExporter` (`exportCsvAndShare` + native `PdfDocument` `exportPdfAndShare`)
  - [x] Upgraded `AdminDashboardViewModel` & `AdminDashboardScreen` into an Admin Operations Hub with date-range filter chips, 2x2 KPI grid (`TabularAmountStyle` + `formatPhp`), Vico `CartesianChartHost` column revenue chart + empty state, Passenger Mix & Top Drop-Offs card, Conductor Shift Performance card, and navigation cards (**Reports & Exports**, **Manage Staff**, **Manage Buses**, **Manage Routes**) + `@Preview`s
  - [x] Built `AdminReportsViewModel` & `AdminReportsScreen` (`ui/admin/reports/`) with date-range/bus/conductor filters, live revenue/ticket summary bar, `>= 48.dp` **Export CSV** & **Export PDF** buttons, filtered ticket `LazyColumn`, and `@Preview`s
  - [x] Built `ManageStaffViewModel` & `ManageStaffScreen` (`ui/admin/staff/`) with role badges (`ADMIN`/`CONDUCTOR`), sync status indicator, Active/Deactivated badges, activation `Switch` blocking self-deactivation of the logged-in Admin, and `@Preview`s
  - [x] Wired `ADMIN_REPORTS` and `ADMIN_STAFF` in `AeTranzitRoutes` & `AeTranzitNavHost` (strictly role-gated to `ADMIN`)
  - [x] Added unit tests (`BuildAdminAnalyticsUseCaseTest`, `ReportExportFormatterTest`, `ManageStaffViewModelTest`) — **46/46 total unit tests passing** + `:app:assembleDebug` **BUILD SUCCESSFUL**

## Phase Roadmap & Checklist
- [x] **Phase 1: Auth & Setup**
- [x] **Phase 2: Core Domain, Bus/Route Management & Active Trip Flow**
- [x] **Phase 3: Conductor Ticketing Flow (Seat Map, Fare/20% Discount/Change Calc, Atomic Issuance)**
- [x] **Phase 4: Digital Ticket Rendering & Sharing (Bitmap, Share Sheet, SMS, ZXing QR)**
- [x] **Phase 5: Admin Dashboard (Vico Analytics, Fleet/User Management)**
- [ ] **Phase 6: Security Rules, Edge-Case QA & Verification**

## Active Task
- None (Ready for Phase 6 upon user instruction).

## Known Bugs / Open Questions
- None blocking.

## Exact File Paths Modified / Created (Phase 5)
- `gradle/libs.versions.toml` (updated)
- `app/build.gradle.kts` (updated)
- `app/src/main/res/xml/file_paths.xml` (updated)
- `app/src/main/java/com/aedev/aetranzit/ui/theme/Dimens.kt` (updated)
- `app/src/main/java/com/aedev/aetranzit/data/local/dao/{UserDao,TripDao,TicketDao}.kt` (updated)
- `app/src/main/java/com/aedev/aetranzit/data/remote/FirebaseAuthSyncDataSource.kt` (updated)
- `app/src/main/java/com/aedev/aetranzit/domain/repository/{AuthRepository,TripRepository,TicketRepository}.kt` (updated)
- `app/src/main/java/com/aedev/aetranzit/data/repository/{AuthRepositoryImpl,TripRepositoryImpl,TicketRepositoryImpl}.kt` (updated)
- `app/src/main/java/com/aedev/aetranzit/domain/usecase/{AnalyticsDateRange,BuildAdminAnalyticsUseCase,ReportExportFormatter}.kt` (created)
- `app/src/main/java/com/aedev/aetranzit/ui/admin/{AdminDashboardViewModel,AdminDashboardScreen}.kt` (updated)
- `app/src/main/java/com/aedev/aetranzit/ui/admin/reports/{ReportFileExporter,AdminReportsViewModel,AdminReportsScreen}.kt` (created)
- `app/src/main/java/com/aedev/aetranzit/ui/admin/staff/{ManageStaffViewModel,ManageStaffScreen}.kt` (created)
- `app/src/main/java/com/aedev/aetranzit/ui/navigation/{AeTranzitRoutes,AeTranzitNavHost}.kt` (updated)
- `app/src/test/java/com/aedev/aetranzit/domain/usecase/{BuildAdminAnalyticsUseCaseTest,ReportExportFormatterTest}.kt` (created)
- `app/src/test/java/com/aedev/aetranzit/ui/admin/staff/ManageStaffViewModelTest.kt` (created)
- `app/src/test/java/com/aedev/aetranzit/ui/auth/AuthViewModelTest.kt` (updated)
- `app/src/test/java/com/aedev/aetranzit/ui/conductor/ConductorTicketingViewModelTest.kt` (updated)
- `app/src/test/java/com/aedev/aetranzit/ui/conductor/ticket/DigitalTicketViewModelTest.kt` (updated)
- `app/src/test/java/com/aedev/aetranzit/data/repository/{TicketRepositoryImplTest,TripRepositoryImplTest}.kt` (updated)
- `.ai/PROGRESS_STATE.md` (updated)
