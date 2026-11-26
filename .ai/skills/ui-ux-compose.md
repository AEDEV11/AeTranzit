# Skill: UI/UX & Jetpack Compose (`ui-ux-compose`)

Load this skill whenever designing or modifying Compose screens, reusable UI components, seat maps, ticket cards, or `ui/theme/`.

## 1. Conductor-First Ergonomics
- Design for **one-handed use on a moving bus**:
  - Minimum touch target size: `48.dp` for all clickable controls (seats, discount toggles, quick-cash chips, action buttons).
  - High-contrast colors and clear typography for outdoor/sunlight readability.
  - Minimize taps per ticket: keep seat selection -> passenger type (Regular / 20% Discount) -> cash tendered/change -> issue & share in a fast, linear flow.

## 2. Material 3 & Centralized Design System
- Centralize all colors, typography, shapes, and spacing/dimensions inside `ui/theme/` (`Color.kt`, `Type.kt`, `Theme.kt`, `Dimens.kt`).
- **Forbidden:** Hardcoded hex `Color(0x...)` or scattered magic `.dp` literals in feature screens. Reference `MaterialTheme.colorScheme`, `MaterialTheme.typography`, and theme/spacing tokens.

## 3. Compose Architecture & Performance
- **Strict State Hoisting:**
  - `*Screen` (stateful wrapper): collects `StateFlow` via `collectAsStateWithLifecycle()`, forwards `UiEvent`s to ViewModel, handles one-off effects (navigation, share sheet, snackbar).
  - `*Content` & components (stateless): accept immutable `UiState` + lambda callbacks only.
- **Unidirectional Data Flow:** `UiState` (single immutable data class or sealed state) + `UiEvent` (sealed interface).
- **Performance:** Use `remember` and `derivedStateOf` for computed UI values (e.g., filtered seats, change preview); supply stable `key`s in `LazyColumn` / `LazyVerticalGrid`.
- **Previews:** Every reusable component and stateless screen content MUST have `@Preview(showBackground = true)` functions covering key states (e.g., default, discounted, error).

## 4. Seat Map & Digital Ticket Clarity
- **Accessible Seat States (Never Color-Only):**
  - **Available:** Distinct outline/surface + seat number label.
  - **Selected:** Filled primary/accent container + check/selection indicator + seat number.
  - **Occupied:** Muted/disabled container + occupied icon/badge + seat number (non-selectable for boarding; distinct action if viewing passenger for drop-off).
- **Digital Ticket:** Render clean high-contrast summary (route, seat, fare, 20% discount badge if applied, timestamp, ZXing QR code) suitable for both screen display and `Bitmap` export.
