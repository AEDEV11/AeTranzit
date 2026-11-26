# AeTranzit — Progress State (Anti-Context-Loss Tracker)

> **Mandatory Rule:** Update this file at the end of every completed task or before context compaction so a fresh session can resume with 100% accuracy and zero amnesia.

## Current Phase
- **Phase 1: Foundation, Theme, Room DB & Auth** — **Completed**
- **Next Up:** **Phase 2: Core Domain, Bus/Route Setup & Trip Management**

## Completed Features
- [x] **Phase 0:** Context Engineering & Agent Guardrails (`AGENTS.md`, `.ai/*`, `.ai/skills/*`)
- [x] **Phase 1: Auth & Setup**
  - [x] Gradle dependencies configured (`Room 2.8.5` + KSP `2.3.14`, `Hilt 2.60.1`, `Navigation Compose 2.10.2`, `DataStore 1.3.0`, `Firebase BOM 35.2.0` + `google-services.json` for `aetranzit`, Google Fonts `ui-text-google-fonts`, `Kotlin 2.4.21`)
  - [x] Design system (`ui/theme/`): High-contrast Deep Navy `#0A192F`, Electric Blue `#007BFF`, Teal `#00C9A7`, `Plus Jakarta Sans` + `Inter` + `TabularAmountStyle` (`tnum`), `AeDimens`, `AeTranzitShapes`
  - [x] Room database (`AeTranzitDatabase` v1 + schema export) with `UserEntity`, `BusEntity`, `RouteEntity`, `TripEntity`, `TicketEntity` (all carrying `isSynced` + `updatedAt`) and DAOs
  - [x] `SyncPreferencesRepository` (DataStore `isCloudSyncEnabled` default `false`, `lastSyncedAt`, `sessionUserId`) & `AuthRepository` (Room SSOT + salted SHA-256 `PasswordHasher` + optional Firebase Auth/Firestore sync)
  - [x] `SplashScreen`, `LoginScreen`, `RegisterScreen` (`R.drawable.logo`, role selector, validation, `@Preview`s)
  - [x] Role-gated `AeTranzitNavHost` -> `AdminDashboardScreen` / `ConductorHomeScreen` shells + `SettingsScreen` (Cloud Sync toggle & manual Sync Now)
  - [x] Unit tests (`PasswordHasherTest`, `AuthViewModelTest`) + `:app:assembleDebug :app:testDebugUnitTest` passing

## Phase Roadmap & Checklist
- [x] **Phase 1: Auth & Setup**
- [ ] **Phase 2: Core Domain, Bus/Route Management & Active Trip Flow**
- [ ] **Phase 3: Conductor Ticketing Flow (Seat Map, Fare/20% Discount/Change Calc, Atomic Issuance)**
- [ ] **Phase 4: Digital Ticket Rendering & Sharing (Bitmap, Share Sheet, SMS, ZXing QR)**
- [ ] **Phase 5: Admin Dashboard (Vico Analytics, Fleet/User Management)**
- [ ] **Phase 6: Security Rules, Edge-Case QA & Verification**

## Active Task
- None (Ready for Phase 2 upon user instruction).

## Known Bugs / Open Questions
- None blocking. `app/schemas/` will contain exported Room schema JSONs (ensure `.gitignore` preference if needed).

## Exact File Paths Modified / Created (Phase 1)
- `.ai/PROJECT_CONTEXT.md` (updated)
- `.ai/ARCHITECTURE_DECISIONS.md` (updated)
- `.ai/PROGRESS_STATE.md` (updated)
- `gradle/libs.versions.toml` (updated)
- `build.gradle.kts` (updated)
- `app/build.gradle.kts` (updated)
- `app/src/main/AndroidManifest.xml` (updated)
- `app/src/main/res/values/font_certs.xml` (created)
- `app/src/main/java/com/aedev/aetranzit/AeTranzitApp.kt` (created)
- `app/src/main/java/com/aedev/aetranzit/MainActivity.kt` (updated)
- `app/src/main/java/com/aedev/aetranzit/ui/theme/Color.kt` (updated)
- `app/src/main/java/com/aedev/aetranzit/ui/theme/Type.kt` (updated)
- `app/src/main/java/com/aedev/aetranzit/ui/theme/Dimens.kt` (created)
- `app/src/main/java/com/aedev/aetranzit/ui/theme/Shape.kt` (created)
- `app/src/main/java/com/aedev/aetranzit/ui/theme/Theme.kt` (updated)
- `app/src/main/java/com/aedev/aetranzit/data/local/entity/{UserEntity,BusEntity,RouteEntity,TripEntity,TicketEntity}.kt` (created)
- `app/src/main/java/com/aedev/aetranzit/data/local/dao/{UserDao,BusDao,RouteDao,TripDao,TicketDao}.kt` (created)
- `app/src/main/java/com/aedev/aetranzit/data/local/AeTranzitDatabase.kt` (created)
- `app/src/main/java/com/aedev/aetranzit/data/mapper/UserMapper.kt` (created)
- `app/src/main/java/com/aedev/aetranzit/data/security/PasswordHasher.kt` (created)
- `app/src/main/java/com/aedev/aetranzit/data/remote/FirebaseAuthSyncDataSource.kt` (created)
- `app/src/main/java/com/aedev/aetranzit/data/repository/{AuthRepositoryImpl,SyncPreferencesRepositoryImpl}.kt` (created)
- `app/src/main/java/com/aedev/aetranzit/domain/model/{User,UserRole}.kt` (created)
- `app/src/main/java/com/aedev/aetranzit/domain/repository/{AuthRepository,SyncPreferencesRepository}.kt` (created)
- `app/src/main/java/com/aedev/aetranzit/di/{CoroutinesModule,DatabaseModule,DataStoreModule,FirebaseModule,RepositoryModule}.kt` (created)
- `app/src/main/java/com/aedev/aetranzit/ui/components/{AeTranzitBrandHeader,AePrimaryButton}.kt` (created)
- `app/src/main/java/com/aedev/aetranzit/ui/auth/{AuthViewModel,SplashViewModel,SplashScreen,LoginScreen,RegisterScreen}.kt` (created)
- `app/src/main/java/com/aedev/aetranzit/ui/settings/{SettingsViewModel,SettingsScreen}.kt` (created)
- `app/src/main/java/com/aedev/aetranzit/ui/admin/AdminDashboardScreen.kt` (created)
- `app/src/main/java/com/aedev/aetranzit/ui/conductor/ConductorHomeScreen.kt` (created)
- `app/src/main/java/com/aedev/aetranzit/ui/navigation/{AeTranzitRoutes,AeTranzitNavHost,SessionViewModel}.kt` (created)
- `app/src/test/java/com/aedev/aetranzit/data/security/PasswordHasherTest.kt` (created)
- `app/src/test/java/com/aedev/aetranzit/ui/auth/AuthViewModelTest.kt` (created)
