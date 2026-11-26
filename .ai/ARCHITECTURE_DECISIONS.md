# AeTranzit — Architecture Decision Records (ADR Log)

> Log every major schema, navigation, or library decision with *Why* it was chosen so future sessions never contradict or overwrite past architecture.

---

## ADR-001: Single-Module Clean Layered Architecture (`ui/` -> `domain/` <- `data/`)
- **Status:** Accepted
- **Decision:** Keep a single `:app` Android module organized cleanly by layers (`ui`, `domain`, `data`, `di`), using MVVM + `StateFlow` and Hilt for dependency injection.
- **Why:** Avoids premature multi-module Gradle overhead while preserving strict separation of concerns (ViewModels depend on `domain/repository` interfaces, never directly on Room DAOs or Firebase SDKs).

## ADR-002: Room Database as Offline-First SSOT + Optional Firestore Sync
- **Status:** Accepted (Supersedes Firestore-primary assumption)
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
