# Skill: Android Architecture & Firebase (`software-engineering-android`)

Load this skill whenever implementing `data/`, `domain/`, `di/`, Hilt modules, Firebase Auth, Cloud Firestore, or Navigation.

## 1. Clean Layered Architecture
- **Dependency Direction:** `ui/` -> `domain/` <- `data/`.
  - `ui/`: Compose screens, ViewModels (`@HiltViewModel`), UI state/events.
  - `domain/`: Pure Kotlin models, repository interfaces, focused use cases (no Android/Firebase framework imports where avoidable).
  - `data/`: Firebase Auth & Firestore data sources, DTOs, mappers, repository implementations.

## 2. Offline-First Cloud Firestore
- Enable Firestore local persistence explicitly at initialization.
- Reads must use snapshot listeners (`callbackFlow`) or cache-aware queries so conductors keep working in cellular dead zones.
- **Critical Concurrency Rule (Seat Assignment):**
  - Updating `trips/{tripId}.occupiedSeats` and creating `tickets/{ticketId}` MUST run inside a **Firestore transaction** (or atomic batch when offline-queued) so seats cannot be double-assigned.
  - Never perform a read-then-separate-write across `trips` and `tickets`.

## 3. Explicit Async States & Error Handling
- Every async operation exposed to UI must use explicit `Loading`, `Success`, and `Error` states via `StateFlow` (or `Result` mapped into `UiState`).
- **Forbidden:** Empty `catch` blocks or swallowing `FirebaseException` / `CancellationException` (always rethrow `CancellationException` in coroutines).
- Surface user-actionable error messages without exposing raw stack traces.

## 4. Security & Role Enforcement
- **Zero Hardcoded Secrets:** Never place API keys, admin credentials, or tokens in Kotlin source or `BuildConfig`.
- **Dual Enforcement (`admin` vs `conductor`):**
  1. **UI / Navigation:** Route graphs and action visibility check the authenticated user's verified role.
  2. **Firestore Security Rules:** Every collection (`users`, `routes`, `trips`, `tickets`) must enforce `request.auth != null` and role checks via custom claims or `users/{uid}` role lookup.
