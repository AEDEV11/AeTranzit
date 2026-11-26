# Skill: QA, Edge Cases & Verification (`qa-verification`)

Load this skill before finalizing any feature, writing tests, or running the pre-completion review gate.

## 1. Mandatory Edge-Case Checklist
Verify all relevant cases for the feature being built:
- **Inputs & Auth:** Empty email/password, malformed inputs, unauthenticated/unknown role, disabled account.
- **Fare & Change Calculator:**
  - Base fare `<= 0` or invalid origin/destination selection.
  - 20% discount toggle applied/removed dynamically before confirmation.
  - Cash tendered empty, non-numeric, `0`, or `< finalFare` (must block ticket issuance and display shortfall).
  - Exact cash (`change == 0.0`) vs over-payment rounding accuracy.
- **Seat Map & Concurrency:**
  - Rapid double-taps on seat selection or "Issue Ticket" button (must debounce / lock UI during `Loading` state).
  - Attempting to select an already occupied seat.
  - Releasing a seat on drop-off without affecting other occupied seats.
- **Offline-to-Online Sync:**
  - Creating/viewing tickets while airplane mode / offline.
  - Handling pending writes and avoiding duplicate ticket creation on retry.
- **Digital Ticket Sharing:**
  - QR code generation with valid non-empty payload.
  - Share sheet / SMS fallback when no SMS or target share app is available (`ActivityNotFoundException` safety).

## 2. Self-Review Gate (Run Before Every Task Sign-Off)
1. **Imports & Compilation:** Check for missing/unused imports, unresolved references, and run Gradle build/tests or IDE file analysis when code is modified.
2. **Compose Safety:** Verify no infinite recomposition loops, no side effects outside `LaunchedEffect`/`rememberCoroutineScope`, and proper state hoisting.
3. **Schema Parity:** Cross-check every Firestore collection and field name (e.g., `trips`, `occupiedSeats`, `tickets`, `users`) against `.ai/PROJECT_CONTEXT.md`.
4. **Memory Sync:** Update `.ai/PROGRESS_STATE.md` (and `.ai/ARCHITECTURE_DECISIONS.md` if applicable) with exact modified file paths and next steps.
