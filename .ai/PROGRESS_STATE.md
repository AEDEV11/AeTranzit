# AeTranzit — Progress State (Anti-Context-Loss Tracker)

> **Mandatory Rule:** Update this file at the end of every completed task or before context compaction so a fresh session can resume with 100% accuracy and zero amnesia.

## Current Phase
- **Phase 13: 3D-Flat Isometric Fintech/Transit SaaS UI/UX Overhaul & Drawable Asset Integration** — **Completed**
- **Project Status:** **All Phases (0–13) Complete — Multi-Tenant Auto-Sync, Admin-Provisioned Staff, 3D-Flat Isometric UI/UX Design System & Reference Assets Production Ready**

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
  - [x] Room v3 (`MIGRATION_2_3` + `mustChangePassword`), Admin-only `RegisterScreen`, `CompanySetupScreen`, secondary-`FirebaseApp` staff provisioning in `ManageStaffScreen`, and blocking `ForceChangePasswordScreen`
- [x] **Phase 13: 3D-Flat Isometric Fintech/Transit SaaS UI/UX Overhaul & Drawable Asset Integration**
  - [x] **Transparent Brand Bus Emblem:** Converted `app/src/main/res/drawable/ic_brand_bus_emblem.webp` in-place to transparent `RGBA` `WEBP` (`2048x2048`, `border_max_alpha = 0`) for clean compositing over Deep Navy banners
  - [x] **Theme & Shared UI Tokens (`ui/theme/`, `ui/components/`):** Enforced centralized palette (`DeepNavy #0A192F`, `ElectricBlue #007BFF`, `TransitTeal #00C9A7`, `PureWhite #FFFFFF`, `CoolMist #F6F8FC`), `Plus Jakarta Sans` + `Inter` + tabular numerals (`TabularAmountStyle` / `TabularLabelStyle`), zero hardcoded hex colors or `.dp` literals outside `ui/theme/`, `>= 48.dp` touch targets (`AeDimens.MinTouchTarget`), `AeIsometricBackground` (`bg_auth_isometric`), `AeEmptyStateCard`, `AePrimaryButton`, `AeTranzitBrandHeader`, and `SyncStatusBadge(onDarkHeader)`
  - [x] **Onboarding, Auth & Setup (`ui/onboarding/`, `ui/auth/`, `ui/admin/setup/`):** Aligned `OnboardingScreen`, `SplashScreen`, `LoginScreen`, `RegisterScreen`, `ForceChangePasswordScreen`, and `CompanySetupScreen` with full-bleed `bg_auth_isometric` (Light + subtle Dark fallback), `1:1` transparent `.webp` studio stage, `extraLarge` white/dark cards, and pill CTAs
  - [x] **Conductor Shift, Seat Map & Digital Ticket (`ui/conductor/`):** Built curved Deep Navy hero headers (`ic_brand_bus_emblem`, `SyncStatusBadge`), wired `ic_seat_available` / `ic_seat_selected` / `ic_seat_occupied` seat tiles with `tnum` seat numbers + text labels + accessibility descriptions in `ConductorTicketingScreen`, wired `empty_fleet_route` in `ConductorHomeScreen`, and aligned photo-ready `DigitalTicketScreen` (`ic_brand_bus_emblem`, Electric Blue `SEAT` banner, `-20%` discount pill, `tnum` `Fare`/`Cash`/`Change`, side-notched perforated divider, pure-white QR card, stacked pill share/SMS CTAs)
  - [x] **Admin Hub, Fleet, Staff, Reports & Settings (`ui/admin/`, `ui/settings/`):** Aligned `AdminDashboardScreen` (`ic_brand_bus_emblem` Deep Navy hero header, pill date filters, circular-badge KPI grid, Vico chart with `empty_analytics_data` fallback, grouped directory rows), `ManageBusesScreen` & `ManageRoutesScreen` (`empty_fleet_route`), `AdminReportsScreen` (`empty_analytics_data`), `ManageStaffScreen`, and `SettingsScreen` with Light + Dark `@Preview` coverage and passing `:app:assembleDebug :app:testDebugUnitTest` (`71 passed, 0 failed`)

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
- [x] **Phase 10: Codebase Cleanup Pass & 4-Slide Onboarding Screen**
- [x] **Phase 11: Animated Logo Video Splash Screen**
- [x] **Phase 12: Transparent `RGBA` Onboarding WebP Asset Cleanup**
- [x] **Phase 13: 3D-Flat Isometric Fintech/Transit SaaS UI/UX Overhaul & Drawable Asset Integration**

## Active Task
- None — Phase 13 UI/UX overhaul and reference drawable asset integration complete and verified (`:app:assembleDebug` and `:app:testDebugUnitTest` passing with 71/71 tests green, Compose `@Preview`s rendered and verified). Ready for device deployment or further instructions.

## Known Bugs / Open Questions
- None blocking.

## Exact File Paths Modified / Created (Phase 13 — 3D-Flat Isometric UI/UX & Reference Assets)
### Created
- `app/src/main/java/com/aedev/aetranzit/ui/components/AeIsometricBackground.kt`

### Updated
- `app/src/main/res/drawable/ic_brand_bus_emblem.webp`
- `app/src/main/java/com/aedev/aetranzit/ui/theme/Color.kt`
- `app/src/main/java/com/aedev/aetranzit/ui/theme/Theme.kt`
- `app/src/main/java/com/aedev/aetranzit/ui/theme/Type.kt`
- `app/src/main/java/com/aedev/aetranzit/ui/theme/Shape.kt`
- `app/src/main/java/com/aedev/aetranzit/ui/theme/Dimens.kt`
- `app/src/main/java/com/aedev/aetranzit/ui/components/AePrimaryButton.kt`
- `app/src/main/java/com/aedev/aetranzit/ui/components/AeTranzitBrandHeader.kt`
- `app/src/main/java/com/aedev/aetranzit/ui/components/SyncStatusBadge.kt`
- `app/src/main/java/com/aedev/aetranzit/ui/onboarding/OnboardingScreen.kt`
- `app/src/main/java/com/aedev/aetranzit/ui/auth/SplashScreen.kt`
- `app/src/main/java/com/aedev/aetranzit/ui/auth/LoginScreen.kt`
- `app/src/main/java/com/aedev/aetranzit/ui/auth/RegisterScreen.kt`
- `app/src/main/java/com/aedev/aetranzit/ui/auth/ForceChangePasswordScreen.kt`
- `app/src/main/java/com/aedev/aetranzit/ui/admin/setup/CompanySetupScreen.kt`
- `app/src/main/java/com/aedev/aetranzit/ui/conductor/ConductorHomeScreen.kt`
- `app/src/main/java/com/aedev/aetranzit/ui/conductor/ConductorTicketingScreen.kt`
- `app/src/main/java/com/aedev/aetranzit/ui/conductor/ticket/DigitalTicketScreen.kt`
- `app/src/main/java/com/aedev/aetranzit/ui/admin/AdminDashboardScreen.kt`
- `app/src/main/java/com/aedev/aetranzit/ui/admin/ManageBusesScreen.kt`
- `app/src/main/java/com/aedev/aetranzit/ui/admin/ManageRoutesScreen.kt`
- `app/src/main/java/com/aedev/aetranzit/ui/admin/reports/AdminReportsScreen.kt`
- `app/src/main/java/com/aedev/aetranzit/ui/admin/staff/ManageStaffScreen.kt`
- `app/src/main/java/com/aedev/aetranzit/ui/settings/SettingsScreen.kt`
- `.ai/PROGRESS_STATE.md`
