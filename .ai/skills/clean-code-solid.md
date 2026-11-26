# Skill: Clean Code & SOLID (`clean-code-solid`)

Load this skill for all Kotlin code authoring, refactoring, and ViewModel/UseCase/Repository design.

## 1. Single Responsibility Principle (SRP)
- **One ViewModel per screen or cohesive flow** (e.g., `LoginViewModel`, `TicketingViewModel`, `AdminDashboardViewModel`).
- **One Repository per domain aggregate:** `AuthRepository`, `TripRepository`, `TicketRepository` (and `RouteRepository` if separated).
- Keep functions small and single-purpose (< 30 lines where practical). Extract validation, fare math, and mapping into dedicated helpers/use cases.

## 2. Open/Closed & Dependency Inversion (OCP / DIP)
- ViewModels and UseCases MUST depend on **Repository interfaces** (`domain/repository/`), never directly on `FirebaseFirestore`, `FirebaseAuth`, or concrete `*RepositoryImpl`.
- Inject all dependencies via **Hilt** (`@Inject constructor`, `@Binds` in Hilt modules).
- Inject `CoroutineDispatcher`s (e.g., `@IoDispatcher`, `@DefaultDispatcher`) rather than hardcoding `Dispatchers.IO` inside testable business logic.

## 3. Intention-Revealing Naming
- Use explicit domain verbs and nouns:
  - Good: `calculateFareWithDiscount`, `markPassengerDroppedOff`, `issueDigitalTicket`, `calculateChangeAmount`
  - Avoid: `calc()`, `proc()`, `handleData()`, `tempVal`, `flag`
- Match Firestore field names consistently between `PROJECT_CONTEXT.md`, DTO `@PropertyName` annotations, and Security Rules.

## 4. Zero Dead Code
- No commented-out code blocks left in files.
- No unused imports, unused parameters, or lingering `TODO` placeholders in completed features.
- Keep models immutable (`val`, `data class`) with explicit defaults only where Firestore deserialization requires a no-arg constructor (in `data/` DTOs only, never pollute `domain/` models with Firestore annotations).
