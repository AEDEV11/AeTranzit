# AeTranzit — Progress State (Anti-Context-Loss Tracker)

> **Mandatory Rule:** Update this file at the end of every completed task or before context compaction so a fresh session can resume with 100% accuracy and zero amnesia.

## Current Phase
- **Phase 9: Admin-Only Registration, Separate Company Setup & Admin-Provisioned Staff with Forced Password Change** — **Completed**
- **Project Status:** **All Phases (0–9) Complete — Multi-Tenant Auto-Sync & Admin-Provisioned Staff Production Ready**

## Completed Features
- [x] **Phase 0:** Context Engineering & Agent Guardrails (`AGENTS.md`, `.ai/*`, `.ai/skills/*`)
- [x] **Phase 1: Auth & Setup**
  - [x] Gradle dependencies configured (`Room 2.8.5` + KSP `2.3.14`, `Hilt 2.60.1`, `Navigation Compose 2.10.2`, `DataStore 1.3.0`, `Firebase BOM 35.2.0` + `google-services.json` for `aetranzit`, Google Fonts `ui-text-google-fonts`)
  - [x] Design system (`ui/theme/`): High-contrast Deep Navy `#0A192F`, Electric Blue `#007BFF`, Teal `#00C9A7`, `Plus Jakarta Sans` + `Inter` + `TabularAmountStyle` (`tnum`), `AeDimens`, `AeTranzitShapes`
  - [x] Room database (`AeTranzitDatabase` v1 + schema export) with `UserEntity`, `BusEntity`, `RouteEntity`, `TripEntity`, `TicketEntity` (all carrying `isSynced` + `updatedAt`) and DAOs
  - [x] `SyncPreferencesRepository` & `AuthRepository` (Room SSOT + salted SHA-256 `PasswordHasher` + Firebase Auth/Firestore sync)
  - [x] `SplashScreen`, `LoginScreen`, `RegisterScreen` (`R.drawable.logo`, validation, `@Preview`s)
  - [x] Role-gated `AeTranzitNavHost` & `SettingsScreen`
- [x] **Phase 2: Core Domain, Bus/Route Management & Active Trip Flow**
  - [x] Transparent `RGBA` brand asset cleanup on `app/src/main/res/drawable/logo.png`
  - [x] Domain models (`Bus`, `RouteStop`, `Route`, `TripStatus`, `Trip`) & Entity <-> Domain mappers (`BusMapper`, `RouteMapper` with zero-dependency RFC-8259 `RouteStopsJsonCodec`, `TripMapper`)
  - [x] `FleetSyncRemoteDataSource` / `FirebaseFleetSyncDataSource` and `BusRepository`, `RouteRepository`, `TripRepository`
  - [x] Admin Fleet & Route Setup (`AdminDashboardScreen`, `ManageBusesScreen`, `ManageRoutesScreen`) & Conductor Shift Flow (`ConductorHomeScreen`)
- [x] **Phase 3: Conductor Ticketing Flow (Seat Map, Fare/20% Discount/Change Calc, Atomic Issuance)**
  - [x] Domain models (`PassengerType`, `TicketStatus`, `Ticket`), `TicketMapper`, and pure `CalculateFareUseCase`
  - [x] Atomic seat issuance & drop-off in `TicketRepositoryImpl` + Firestore transaction sync (`tickets/{ticketId}` + `trips/{tripId}.occupiedSeats`)
  - [x] `ConductorTicketingScreen` & `ConductorTicketingViewModel` (`2x2` & `2x3` interactive seat map, Issue Ticket & Occupied Seat sheets)
- [x] **Phase 4: Digital Ticket Rendering & Sharing (Bitmap, Share Sheet, SMS, ZXing QR)**
  - [x] ZXing Core `3.5.4`, `FileProvider`, `TicketPayloadFormatter`, `QrCodeGenerator`, `DigitalTicketBitmapRenderer`, `TicketShareHelper`
  - [x] `DigitalTicketViewModel` & `DigitalTicketScreen` + `CONDUCTOR_DIGITAL_TICKET` route
- [x] **Phase 5: Admin Analytics (Vico Charts), Reports (CSV/PDF Export) & Staff Management**
  - [x] Vico Compose M3 `3.3.1`, `BuildAdminAnalyticsUseCase`, `ReportExportFormatter`, `ReportFileExporter`
  - [x] `AdminDashboardScreen` analytics hub, `AdminReportsScreen`, and `ManageStaffScreen`
- [x] **Phase 6: Web Ticket Verification (Firebase Hosting + Cloud Functions), Firestore Security Rules & Final QA**
  - [x] Web verification URLs (`https://aetranzit.aesprt.com/t/<ticketId>`), Node.js 20 `viewTicket` Cloud Function v2, Hosting portal, role-based `firestore.rules`, and `docs/FIREBASE_DEPLOY.md`
- [x] **Phase 7: Multi-Company (Multi-Tenant) Architecture Upgrade**
  - [x] Room v2 (`CompanyEntity` + `MIGRATION_1_2` + `companyId` across all entities/DAOs), strict tenant isolation across repositories & `firestore.rules`, and company-branded Digital Tickets + `viewTicket` web receipts
- [x] **Phase 8: Multi-Tenant Auto-Sync (Firebase Cloud SSOT + Seamless Offline Room Queue)**
  - [x] WorkManager + Hilt `AeTranzitSyncWorker`, `SyncCoordinator` (strict dependency push + safe pull upsert), `NetworkConnectivityObserver`, `SyncStatusBadge`, and session linking on login
- [x] **Phase 9: Admin-Only Registration, Separate Company Setup & Admin-Provisioned Staff with Forced Password Change**
  - [x] **Room v3 Migration:** Added `mustChangePassword: Boolean = false` to `UserEntity` & `User`, bumped `AeTranzitDatabase` to `version = 3` (schema `3.json` exported), and added `MIGRATION_2_3` alongside intact `MIGRATION_1_2` in `DatabaseModule`
  - [x] **Clean Admin-Only Registration:** Stripped role selector, company name, and join code fields from `RegisterScreen` & `AuthViewModel`; `AuthRepositoryImpl.register` creates an `ADMIN` with `companyId = ""` and `mustChangePassword = false` and routes to `COMPANY_SETUP`
  - [x] **Dedicated Business Setup Screen:** Built `CompanySetupScreen` + `CompanySetupViewModel` (`ui/admin/setup/`) and centralized `AeTranzitRoutes.postAuthRouteFor(user)` enforcement across `SplashScreen`, `LoginScreen`, `RegisterScreen`, and `AeTranzitNavHost`
  - [x] **Admin-Provisioned Staff (Secondary `FirebaseApp`):** Added `"Add Staff"` FAB, modal sheet (Full Name, Email, `Conductor`/`Admin` role selector, Temporary Password + `"Generate"` helper), and post-creation `"Copy Credentials"` dialog in `ManageStaffScreen`; implemented `FirebaseAuthSyncDataSource.provisionStaffAccount` using temporary secondary `FirebaseApp` (`"staff_provisioner"`) so the signed-in Admin is never signed out; removed legacy Join Code invite banners from `AdminDashboardScreen` and `ManageStaffScreen`
  - [x] **Forced First-Login Password Change:** Built `ForceChangePasswordScreen` + `ForceChangePasswordViewModel` (`ui/auth/`) blocking back-navigation until `mustChangePassword` is cleared via `AuthRepository.completeForcedPasswordChange` (updating Room, `FirebaseAuth.currentUser.updatePassword`, and Firestore `users/{uid}`)
  - [x] **Firestore Security Rules & QA:** Updated `firestore.rules` for `mustChangePassword: bool`, unconfigured Admin `companyId == ""`, one-time founder `companyId` assignment, and self-service `mustChangePassword` clearing; verified `./gradlew :app:assembleDebug :app:testDebugUnitTest --console=plain` **BUILD SUCCESSFUL** with all unit tests passing

## Phase Roadmap & Checklist
- [x] **Phase 1: Auth & Setup**
- [x] **Phase 2: Core Domain, Bus/Route Management & Active Trip Flow**
- [x] **Phase 3: Conductor Ticketing Flow (Seat Map, Fare/20% Discount/Change Calc, Atomic Issuance)**
- [x] **Phase 4: Digital Ticket Rendering & Sharing (Bitmap, Share Sheet, SMS, ZXing QR)**
- [x] **Phase 5: Admin Dashboard (Vico Analytics, Fleet/User Management)**
- [x] **Phase 6: Security Rules, Edge-Case QA & Verification**
- [x] **Phase 7: Multi-Company (Multi-Tenant) Architecture Upgrade**
- [x] **Phase 8: Multi-Tenant Auto-Sync (Firebase Cloud SSOT + Seamless Offline Room Queue)**
- [x] **Phase 9: Admin-Only Registration, Separate Company Setup & Admin-Provisioned Staff with Forced Password Change**

## Active Task
- None — Phases 0–9 complete and verified. Ready for device testing and `firebase deploy --only firestore:rules`.

## Known Bugs / Open Questions
- None blocking.

## Exact File Paths Modified / Created (Phase 9)
### Created
- `app/schemas/com.aedev.aetranzit.data.local.AeTranzitDatabase/3.json`
- `app/src/main/java/com/aedev/aetranzit/ui/admin/setup/CompanySetupViewModel.kt`
- `app/src/main/java/com/aedev/aetranzit/ui/admin/setup/CompanySetupScreen.kt`
- `app/src/main/java/com/aedev/aetranzit/ui/auth/ForceChangePasswordViewModel.kt`
- `app/src/main/java/com/aedev/aetranzit/ui/auth/ForceChangePasswordScreen.kt`
- `app/src/test/java/com/aedev/aetranzit/ui/auth/ForceChangePasswordViewModelTest.kt`

### Updated
- `app/src/main/java/com/aedev/aetranzit/data/local/entity/UserEntity.kt`
- `app/src/main/java/com/aedev/aetranzit/domain/model/User.kt`
- `app/src/main/java/com/aedev/aetranzit/data/mapper/UserMapper.kt`
- `app/src/main/java/com/aedev/aetranzit/data/local/dao/UserDao.kt`
- `app/src/main/java/com/aedev/aetranzit/data/local/AeTranzitDatabase.kt`
- `app/src/main/java/com/aedev/aetranzit/di/DatabaseModule.kt`
- `app/src/main/java/com/aedev/aetranzit/domain/repository/AuthRepository.kt`
- `app/src/main/java/com/aedev/aetranzit/data/remote/FirebaseSyncErrorMapper.kt`
- `app/src/main/java/com/aedev/aetranzit/data/remote/FirebaseAuthSyncDataSource.kt`
- `app/src/main/java/com/aedev/aetranzit/data/remote/FirebaseFleetSyncDataSource.kt`
- `app/src/main/java/com/aedev/aetranzit/data/repository/AuthRepositoryImpl.kt`
- `app/src/main/java/com/aedev/aetranzit/ui/navigation/AeTranzitRoutes.kt`
- `app/src/main/java/com/aedev/aetranzit/ui/navigation/AeTranzitNavHost.kt`
- `app/src/main/java/com/aedev/aetranzit/ui/auth/SplashViewModel.kt`
- `app/src/main/java/com/aedev/aetranzit/ui/auth/SplashScreen.kt`
- `app/src/main/java/com/aedev/aetranzit/ui/auth/LoginScreen.kt`
- `app/src/main/java/com/aedev/aetranzit/ui/auth/RegisterScreen.kt`
- `app/src/main/java/com/aedev/aetranzit/ui/auth/AuthViewModel.kt`
- `app/src/main/java/com/aedev/aetranzit/ui/admin/AdminDashboardScreen.kt`
- `app/src/main/java/com/aedev/aetranzit/ui/admin/staff/ManageStaffViewModel.kt`
- `app/src/main/java/com/aedev/aetranzit/ui/admin/staff/ManageStaffScreen.kt`
- `firestore.rules`
- `app/src/test/java/com/aedev/aetranzit/data/repository/AuthRepositoryImplTest.kt`
- `app/src/test/java/com/aedev/aetranzit/data/sync/SyncCoordinatorTest.kt`
- `app/src/test/java/com/aedev/aetranzit/ui/auth/AuthViewModelTest.kt`
- `app/src/test/java/com/aedev/aetranzit/ui/admin/staff/ManageStaffViewModelTest.kt`
- `app/src/test/java/com/aedev/aetranzit/ui/conductor/ticket/DigitalTicketViewModelTest.kt`
- `.ai/ARCHITECTURE_DECISIONS.md`
- `.ai/PROJECT_CONTEXT.md`
- `.ai/PROGRESS_STATE.md`
