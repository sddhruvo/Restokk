# Paper & Ink Theme — Complete Implementation Plan

> **Created**: Session 108 (2026-03-02)
> **Source**: `ink-theme-audit.md` (23-item audit)
> **Status**: APPROVED — ready for implementation
> **Rule**: All recommendations accepted. Work wave-by-wave, build → test → next.
> **Theme rule**: ZERO hardcoded visual values in composables. Everything reads from theme tokens.

---

## Decisions Summary

| Decision | Choice |
|----------|--------|
| Architecture | Universal `isInk` check via `ThemeVisuals.isInk` extension property |
| Reduce motion | Keep wobble shapes (static), skip all animated transitions |
| Performance | Cache wobble Paths with `remember`, profile after each wave |
| Snackbar | InkBorderCard container + BouncySpring entrance |
| TopAppBar | ThemedTopAppBar wrapper — transparent in P&I, SemiBold title |
| FAB | ThemedFab — InkBorderCard circle, paper fill, no shadow |
| AlertDialog | Custom Dialog() + InkBorderCard + warm sepia scrim + BouncySpring enter |
| BottomSheet | ThemedBottomSheet wrapper + ink dash drag handle + warm scrim |
| DropdownMenu | Deferred to Wave E (BOM limitation) |
| TextField | "Ink Border Box" — wobbly border, alpha fade focus, shake on error |
| Switch | "Ink Checkbox" — wobbly square + animated checkmark write-in |
| Buttons | ThemedButton + ThemedTextButton, ~30-40 high-visibility instances |
| Chips | ThemedFilterChip for all FilterChip (~20), ink bleed fill on selection |
| Wave E | InkSpinner + Checkbox/Radio + Status bar + DropdownMenu (try) |

---

## Pre-Wave: Token Infrastructure (Do First)

> Before building any composable, add the shared design tokens that ALL waves reference.
> This ensures every composable reads from a single source of truth.

### 1. Add `InkTokens` to `VisualStyle.kt`

All ink-specific visual design parameters live here. No composable file should contain raw alpha, stroke, wobble, or scrim values.

```kotlin
/**
 * Shared design tokens for all Paper & Ink themed components.
 * Every Themed* composable reads from here — zero hardcoded visual values.
 *
 * Organized by category:
 * - Stroke: pen thickness at different scales
 * - Fill: ink wash intensity (how much color bleeds through paper)
 * - Border: ink border visibility at different states
 * - Scrim: overlay dimming for dialogs/sheets
 * - Wobble: hand-drawn irregularity at different component scales
 * - Size: component-specific dimensions
 */
object InkTokens {
    // ── Stroke widths (pen thickness) ──
    val strokeBold = 2.5.dp       // Cards, FAB, dialog borders, checkmark, spinner
    val strokeMedium = 1.5.dp     // TextField, chips, checkbox border, dividers
    val strokeFine = 1.0.dp       // Text button underline, subtle accents

    // ── Fill alphas (ink wash intensity — how much color bleeds through paper) ──
    const val fillBleed = 0.08f       // Bleed layers underneath strokes (spread effect)
    const val fillLight = 0.12f       // Chip selected, button fill (subtle wash)
    const val fillMedium = 0.15f      // FAB fill (slightly more prominent)
    const val fillOpaque = 0.92f      // Snackbar container (needs to be readable above content)

    // ── Border alphas (ink density at different interaction states) ──
    const val borderSketch = 0.30f    // Sketch/secondary stroke overlay in InkBorderCard
    const val borderSubtle = 0.40f    // Unfocused text fields, drag handle, inactive borders
    const val borderMedium = 0.50f    // Unselected chips, text button underline
    const val borderBold = 0.90f      // Focused text fields, selected chips, card borders

    // ── Disabled state alphas (Material3 convention) ──
    const val disabledBorder = 0.25f  // Disabled button/chip border
    const val disabledContent = 0.38f // Disabled text, icons

    // ── Scrim alphas (overlay dimming) ──
    const val scrimSheet = 0.30f      // Bottom sheet dim (lighter — sheet is partial)
    const val scrimDialog = 0.35f     // Dialog dim (slightly heavier — full focus steal)

    // ── Wobble amplitudes (hand-drawn irregularity per component scale) ──
    val wobbleLarge = 3.dp            // Cards (InkBorderCard default)
    val wobbleMedium = 2.dp           // FABs, dialogs, large components
    val wobbleSmall = 1.5.dp          // Text fields, chips, checkboxes, small elements

    // ── Component sizes ──
    val checkboxSize = 22.dp          // Ink checkbox/switch visual (shared C2 + E2)
    val radioSize = 20.dp             // Ink radio button visual (E2)
    val dragHandleWidth = 32.dp       // Bottom sheet drag handle
    val dragHandleHeight = 4.dp       // Bottom sheet drag handle
    val fabSize = 56.dp               // Standard FAB (layout constant)

    // ── Error shake ──
    val shakeOffset = 3.dp            // Horizontal shake distance on error (TextField)
}
```

### 2. Extend `PaperInkMotion` in `Animation.kt`

Add missing duration tokens that the plan's animations need. The existing tokens cover some but not all:

```kotlin
object PaperInkMotion {
    // ... existing springs (BouncySpring, WobblySpring, GentleSpring, SettleSpring) ...

    // ── Duration tokens (existing) ──
    const val DurationShort = 150     // Quick fades, dialog entrance fade
    const val DurationMedium = 300    // Standard transitions, TextField focus fade
    const val DurationLong = 500      // Slow entrances, shimmer
    const val DurationChart = 800     // Chart draw-on, InkSpinner cycle

    // ── Duration tokens (NEW — needed by Waves B-D) ──
    const val DurationQuick = 100     // Very fast: dialog exit fade, micro-transitions
    const val DurationEntry = 200     // Chip bleed-in, ink underline pulse
    const val DurationSettle = 250    // Checkmark write-in, toggle transitions

    // ── Spring specs (NEW — needed by Wave C) ──
    val ShakeSpring: SpringSpec<Float> = spring(dampingRatio = 0.3f, stiffness = 800f)
    // Used for error shake on TextField — fast, aggressive, 2-cycle damped oscillation

    // ── Animation scales (NEW — needed by Wave B) ──
    const val DialogEnterScale = 0.92f   // Dialog scales from this to 1.0 on enter
    const val DialogExitScale = 0.95f    // Dialog scales from 1.0 to this on exit

    // ... existing constants (STAGGER_MS, MAX_STAGGER_ITEMS, etc.) ...
}
```

### 3. Add `isInk` extension to `VisualStyle.kt`

```kotlin
val ThemeVisuals.isInk: Boolean
    get() = cardStyle is CardStyle.InkBorder
```

### Files changed in Pre-Wave:
- `VisualStyle.kt` — add `InkTokens` object + `isInk` extension
- `Animation.kt` — add 5 new constants + 1 spring to `PaperInkMotion`

---

## Token Reference — What Each Composable Reads

Every value below comes from either `InkTokens.*`, `PaperInkMotion.*`, `MaterialTheme.colorScheme.*`, `MaterialTheme.appColors.*`, or `MaterialTheme.shapes.*`. No raw literals.

| Composable | Token References |
|------------|-----------------|
| **ThemedSnackbarHost** | fill: `colorScheme.surfaceVariant.copy(InkTokens.fillOpaque)`, entrance: `PaperInkMotion.BouncySpring` |
| **ThemedTopAppBar** | container: `Color.Transparent`, title style: `MaterialTheme.typography.titleLarge` (SemiBold already in HandwrittenTypography) |
| **ThemedFab** | fill: `colorScheme.primary.copy(InkTokens.fillMedium)`, size: `InkTokens.fabSize`, wobble: `InkTokens.wobbleMedium`, stroke: `InkTokens.strokeBold` |
| **ThemedAlertDialog** | scrim: `colorScheme.onSurface.copy(InkTokens.scrimDialog)`, enter: `PaperInkMotion.BouncySpring` at `PaperInkMotion.DialogEnterScale`, enter fade: `tween(PaperInkMotion.DurationShort)`, exit: `PaperInkMotion.GentleSpring` at `PaperInkMotion.DialogExitScale`, exit fade: `tween(PaperInkMotion.DurationQuick)` |
| **ThemedBottomSheet** | container: `colorScheme.surfaceVariant`, scrim: `colorScheme.onSurface.copy(InkTokens.scrimSheet)`, handle color: `colorScheme.onSurfaceVariant.copy(InkTokens.borderSubtle)`, handle size: `InkTokens.dragHandleWidth` × `InkTokens.dragHandleHeight` |
| **ThemedTextField** | stroke: `InkTokens.strokeMedium`, wobble: `InkTokens.wobbleSmall`, bleed alpha: `InkTokens.fillBleed`, unfocused alpha: `InkTokens.borderSubtle`, focused alpha: `InkTokens.borderBold`, focus anim: `tween(PaperInkMotion.DurationMedium)`, border color: `colorScheme.onSurface` / `colorScheme.error`, shake: `InkTokens.shakeOffset` + `PaperInkMotion.ShakeSpring`, container: `Color.Transparent` |
| **ThemedSwitch** | size: `InkTokens.checkboxSize`, border stroke: `InkTokens.strokeMedium`, border alpha: `colorScheme.onSurface.copy(InkTokens.borderMedium)`, checkmark stroke: `InkTokens.strokeBold`, checkmark color: `colorScheme.primary`, anim: `tween(PaperInkMotion.DurationSettle)`, wobble: `InkTokens.wobbleSmall` |
| **ThemedButton** | fill: `colorScheme.primary.copy(InkTokens.fillLight)`, border color: `colorScheme.primary`, stroke: `InkTokens.strokeMedium`, bleed: `InkTokens.fillBleed`, wobble: `InkTokens.wobbleSmall`, disabled border: `InkTokens.disabledBorder`, disabled text: `InkTokens.disabledContent`, press scale: `PaperInkMotion.PressScale` |
| **ThemedTextButton** | text color: `colorScheme.primary`, underline alpha: `InkTokens.borderMedium`, underline stroke: `InkTokens.strokeFine`, pulse alpha: `InkTokens.borderMedium * 2` (capped at 0.6), press scale: `PaperInkMotion.PressScale` |
| **ThemedFilterChip** | unselected border: `colorScheme.onSurface.copy(InkTokens.borderMedium)`, selected border: `colorScheme.onSurface.copy(InkTokens.borderBold)`, selected fill alpha: `InkTokens.fillLight`, stroke: `InkTokens.strokeMedium`, wobble: `InkTokens.wobbleSmall`, bleed-in: `tween(PaperInkMotion.DurationEntry)` |
| **ThemedCircularProgress** | stroke: `InkTokens.strokeBold`, color: `colorScheme.onSurface.copy(InkTokens.borderMedium)`, cycle: `PaperInkMotion.DurationChart` |
| **ThemedCheckbox** | Same tokens as ThemedSwitch (shared drawing code) |
| **ThemedRadioButton** | size: `InkTokens.radioSize`, border: `InkTokens.strokeMedium`, wobble: `InkTokens.wobbleSmall`, dot enter: `PaperInkMotion.BouncySpring` |

---

## Architecture: The `isInk` Convention

Every new Themed* composable uses this pattern:

```kotlin
@Composable
fun ThemedFoo(...) {
    val isInk = MaterialTheme.visuals.isInk
    if (isInk) { InkFoo(...) } else { StandardFoo(...) }
}
```

No new sealed interfaces. No new fields in ThemeVisuals. The two visual modes move together — there's no scenario where you want ink cards but modern dialogs.

---

## Wave A: Free Wins

> ~1 session. Creates 3 new composables, modifies ~35 files.
> Every screen improves via centralized changes.

---

### A1. Snackbar (#6) — "Ink Note Toast"

**What**: Custom SnackbarHost that uses InkBorderCard in Paper & Ink mode.
**Impact**: 1 composable change → 23 screens improved. Best ROI in the entire audit.

**New composable**: `InkSnackbar` + `ThemedSnackbarHost` in `CommonComponents.kt`

**Approach**:
- Create `ThemedSnackbarHost(hostState: SnackbarHostState)` composable
- In Paper & Ink mode: renders each snackbar inside InkBorderCard with:
  - Container: InkBorderCard, `colorScheme.surfaceVariant.copy(alpha = InkTokens.fillOpaque)` fill
  - Text: inherits Mali font from theme typography
  - Action button: `TextButton` with `colorScheme.primary` color (theming comes later in Wave D)
  - Entrance: `AnimatedVisibility` with `scaleIn(PaperInkMotion.BouncySpring)` + `fadeIn`
  - Exit: `fadeOut` + `slideOutVertically` (downward)
- In Modern mode: delegates to standard `SnackbarHost`

**Migration pattern**:
```
// BEFORE (in every screen):
snackbarHost = { SnackbarHost(snackbarHostState) }

// AFTER:
snackbarHost = { ThemedSnackbarHost(snackbarHostState) }
```

Search-and-replace `SnackbarHost(` → `ThemedSnackbarHost(` in all screen files.

**Files to modify**: ~23 screen files that pass snackbarHost to ThemedScaffold
**Files to create/edit**: `CommonComponents.kt` (add ThemedSnackbarHost + InkSnackbar)

**Edge cases**:
- Snackbar with action button (e.g., "UNDO" on delete) — action slot must work inside InkBorderCard
- Snackbar duration — unchanged, SnackbarHostState handles timing
- Multiple rapid snackbars — unchanged, SnackbarHostState handles queueing
- Reduce motion: skip BouncySpring entrance, show immediately

**Tokens used**: `InkTokens.fillOpaque`, `PaperInkMotion.BouncySpring`, `colorScheme.surfaceVariant`, `colorScheme.primary`

---

### A2. TopAppBar (#2) — "Invisible Header"

**What**: ThemedTopAppBar that forces transparency in Paper & Ink mode.
**Impact**: 1 composable → 31 screens improved. Paper texture flows seamlessly under the header.

**New composable**: `ThemedTopAppBar` in `CommonComponents.kt`

**Approach**:
- Create `ThemedTopAppBar` with the **same parameter signature** as Material3 `TopAppBar`:
  - `title`, `modifier`, `navigationIcon`, `actions`, `windowInsets`, `colors`, `scrollBehavior`
- In Paper & Ink mode:
  - Force `containerColor = Color.Transparent`
  - Force `scrolledContainerColor = Color.Transparent`
  - Title: wrap in `ProvideTextStyle(MaterialTheme.typography.titleLarge)` — HandwrittenTypography already defines titleLarge with `FontWeight.SemiBold`, so this inherits automatically. Zero changes in screen files.
  - No bottom divider/shadow — paper is continuous
- In Modern mode: delegate to standard `TopAppBar` with caller-provided colors
- If caller explicitly passes `colors` parameter, respect it in BOTH modes (allows ItemListScreen selection-mode override)

**Migration pattern**:
```
// BEFORE:
TopAppBar(title = { Text("Items") }, navigationIcon = { InkBackButton(...) }, actions = { ... })

// AFTER:
ThemedTopAppBar(title = { Text("Items") }, navigationIcon = { InkBackButton(...) }, actions = { ... })
```

Mechanical search-and-replace `TopAppBar(` → `ThemedTopAppBar(` across 31 screens.

**Files to modify**: ~31 screen files
**Files to create/edit**: `CommonComponents.kt` (add ThemedTopAppBar)

**Edge cases**:
- `ItemListScreen` selection mode: passes `TopAppBarDefaults.topAppBarColors(containerColor = primaryContainer)`. ThemedTopAppBar must accept `colors` param and use it when provided, overriding the transparent default.
- `scrollBehavior`: Some screens may use `TopAppBarDefaults.pinnedScrollBehavior()`. ThemedTopAppBar passes it through unchanged.
- Reduce motion: no animation involved — just color/transparency. No RM impact.

**Tokens used**: `Color.Transparent` (intentional literal — transparency is not a theme parameter), `MaterialTheme.typography.titleLarge` (HandwrittenTypography provides SemiBold)

---

### A3. FAB (#12) — Ink Circle

**What**: ThemedFab that renders as InkBorderCard circle in Paper & Ink mode.
**Impact**: 4 instances. Small scope, strong visual personality on list screens.

**New composable**: `ThemedFab` in `AnimatedComponents.kt` (where AnimatedFab lives)

**Approach**:
- Create `ThemedFab(onClick, modifier, containerColor, content)` with same core API as FloatingActionButton
- In Paper & Ink mode:
  - Render as `InkBorderCard` with `cornerRadius = InkTokens.fabSize / 2` (56dp/2 = 28dp = circle)
  - Fill: `colorScheme.primary.copy(alpha = InkTokens.fillMedium)`
  - InkBorder params: `wobbleAmplitude = InkTokens.wobbleMedium`, `strokeWidth = InkTokens.strokeBold`
  - Size: `InkTokens.fabSize` (56.dp), centered content
  - No shadow (`visuals.useElevation` is already false)
  - Icon: caller wraps with `ThemedIcon` (already done at all 4 sites)
- In Modern mode: delegate to standard `FloatingActionButton`
- Update `AnimatedFab` to wrap `ThemedFab` instead of `FloatingActionButton`

**Migration pattern**:
```
// BEFORE:
FloatingActionButton(onClick = { ... }) { ThemedIcon(...) }

// AFTER:
ThemedFab(onClick = { ... }) { ThemedIcon(...) }
```

**Files to modify**:
- `CategoryListScreen.kt` — 1 FAB
- `SubcategoryListScreen.kt` — 1 FAB
- `LocationListScreen.kt` — 1 FAB
- `AnimatedComponents.kt` — AnimatedFab wrapper (internal swap)

**Edge cases**:
- `cornerRadius = 28.dp` with `InkTokens.wobbleMedium` (2dp) should produce a near-circle. Verify on emulator — if the wobble breaks the circle feel, increase `segments` from 6 to 8 for smoother curve.
- inkBreathe on the icon inside: already works because ThemedIcon applies inkBreathe internally.
- Reduce motion: no FAB-specific animation.

**Tokens used**: `InkTokens.fabSize`, `InkTokens.fillMedium`, `InkTokens.wobbleMedium`, `InkTokens.strokeBold`, `colorScheme.primary`

---

## Wave B: Overlay Layer

> ~1 session. Creates 3 new composables, modifies ~18 files.
> Every popup/overlay feels papery.

---

### B1. AlertDialog (#4) — "Torn Note"

**What**: Custom dialog with InkBorderCard container, warm scrim, spring entrance.
**Impact**: 19 calls across 15 files. Every confirmation dialog transforms.

**New composable**: `ThemedAlertDialog` in `ui/components/ThemedAlertDialog.kt` (new file)

**Approach**:
- Use low-level `Dialog()` composable (from `androidx.compose.ui.window.Dialog`)
- Match the AlertDialog API: `onDismissRequest`, `title`, `text`, `confirmButton`, `dismissButton`, `icon` (optional)
- In Paper & Ink mode:
  - **Scrim**: Custom full-screen Box with `colorScheme.onSurface.copy(alpha = InkTokens.scrimDialog)` (warm sepia). Use `DialogProperties(usePlatformDefaultWidth = false)` and draw our own scrim + card.
  - **Container**: InkBorderCard with `inkBorder = CardStyle.InkBorder(wobbleAmplitude = InkTokens.wobbleMedium)`, default corner radius from theme shapes, paper fill
  - **Layout inside**: Column with title (`MaterialTheme.typography.titleLarge`), text (`MaterialTheme.typography.bodyMedium`), button Row (end-aligned, confirm + dismiss)
  - **Enter**: `AnimatedVisibility` with `scaleIn(initialScale = PaperInkMotion.DialogEnterScale, animationSpec = PaperInkMotion.BouncySpring)` + `fadeIn(tween(PaperInkMotion.DurationShort))`
  - **Exit**: `scaleOut(targetScale = PaperInkMotion.DialogExitScale, animationSpec = PaperInkMotion.GentleSpring)` + `fadeOut(tween(PaperInkMotion.DurationQuick))`
  - **Buttons**: Standard `TextButton` for now (Wave D upgrades them to ThemedTextButton). Destructive actions already use `color = colorScheme.error`.
- In Modern mode: delegate to standard `AlertDialog` with same params
- Also update `ConfirmDialog` in `CommonComponents.kt` — it uses AlertDialog internally, swap to ThemedAlertDialog

**Migration pattern**:
```
// BEFORE:
AlertDialog(
    onDismissRequest = { ... },
    title = { Text("Discard Changes?") },
    text = { Text("You have unsaved changes...") },
    confirmButton = { TextButton(onClick = { ... }) { Text("Discard") } },
    dismissButton = { TextButton(onClick = { ... }) { Text("Keep Editing") } }
)

// AFTER (identical API):
ThemedAlertDialog(
    onDismissRequest = { ... },
    title = { Text("Discard Changes?") },
    text = { Text("You have unsaved changes...") },
    confirmButton = { TextButton(onClick = { ... }) { Text("Discard") } },
    dismissButton = { TextButton(onClick = { ... }) { Text("Keep Editing") } }
)
```

**Files to modify**: 15 files with 19 AlertDialog calls + CommonComponents.kt (ConfirmDialog)
**Files to create**: `ui/components/ThemedAlertDialog.kt`

**Edge cases**:
- **BatchAddDialog** (ShoppingListScreen): Has an OutlinedTextField inside the `text` slot. ThemedAlertDialog's `text` slot must accept arbitrary composable content, not just Text.
- **Dialog with icon** (AiSignInGate): Uses `icon` param. Include it in ThemedAlertDialog API — render above title when provided.
- **Scrim tap dismiss**: Our custom scrim Box needs `clickable(onClick = onDismissRequest)` with `indication = null`.
- **IME interaction**: Dialogs with text fields need the keyboard to push the dialog up. `Dialog()` handles this via the window's soft input mode.
- **Dark mode**: InkBorderCard already adapts. Scrim uses `colorScheme.onSurface` which is theme-aware.
- **Reduce motion**: Skip scale enter/exit animations. Show dialog immediately at final state. Still render InkBorderCard container (static shape).

**Tokens used**: `InkTokens.scrimDialog`, `InkTokens.wobbleMedium`, `PaperInkMotion.BouncySpring`, `PaperInkMotion.GentleSpring`, `PaperInkMotion.DialogEnterScale`, `PaperInkMotion.DialogExitScale`, `PaperInkMotion.DurationShort`, `PaperInkMotion.DurationQuick`, `MaterialTheme.typography.titleLarge`, `MaterialTheme.typography.bodyMedium`, `colorScheme.onSurface`, `colorScheme.error`

---

### B2. BottomSheet (#9) — "Pulled Notebook Page"

**What**: ThemedBottomSheet with paper tone, ink dash drag handle, warm scrim.
**Impact**: 4 instances in 3 files.

**New composable**: `ThemedBottomSheet` + `InkDashDragHandle` in `CommonComponents.kt`

**Approach**:
- Wrap `ModalBottomSheet` with themed parameters:
  - In Paper & Ink mode:
    - `containerColor = colorScheme.surfaceVariant` (paper-toned, reads from theme)
    - `scrimColor = colorScheme.onSurface.copy(alpha = InkTokens.scrimSheet)` (warm sepia dim)
    - `dragHandle = { InkDashDragHandle() }` — custom composable
  - In Modern mode: delegate to `ModalBottomSheet` with default params
- `InkDashDragHandle`: Small composable (~15 lines):
  - Size: `InkTokens.dragHandleWidth` × `InkTokens.dragHandleHeight` Canvas
  - Draws a wobbly line with round caps using wobble-bezier math
  - Stroke: `InkTokens.strokeMedium`
  - Subtle `inkBreathe` modifier (reads from `PaperInkMotion.BreatheScaleDefault`, `PaperInkMotion.BreathePeriodDefault`)
  - Color: `colorScheme.onSurfaceVariant.copy(alpha = InkTokens.borderSubtle)`
- ThemedBottomSheet keeps the same API as ModalBottomSheet: `onDismissRequest`, `sheetState`, `content`

**Migration pattern**:
```
// BEFORE:
ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) { ... }

// AFTER:
ThemedBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) { ... }
```

**Files to modify**:
- `AddShoppingItemSheet.kt` — 1 sheet
- `ReceiptScanScreen.kt` — 1 sheet
- `CookScreen.kt` — 2 sheets (CuisinePassportSheet, HeroPickerSheet)

**Edge cases**:
- All 4 sheets use `skipPartiallyExpanded = true`. ThemedBottomSheet passes through `sheetState` as-is (caller controls).
- Sheet content already sits inside ThemedScaffold's paper texture world — the `surfaceVariant` gives a slightly different paper tone to differentiate.
- InkDashDragHandle with inkBreathe: inkBreathe checks `LocalReduceMotion` internally.
- `@OptIn(ExperimentalMaterial3Api::class)` required — same as current usage.

**Tokens used**: `InkTokens.scrimSheet`, `InkTokens.borderSubtle`, `InkTokens.strokeMedium`, `InkTokens.dragHandleWidth`, `InkTokens.dragHandleHeight`, `PaperInkMotion.BreatheScaleDefault`, `colorScheme.surfaceVariant`, `colorScheme.onSurface`, `colorScheme.onSurfaceVariant`

---

### B3. DropdownMenu (#16) — DEFERRED TO WAVE E

**Why deferred**: Material3 1.2.0 (our BOM) doesn't expose `containerColor` or `shape` on `DropdownMenu`. The ROI is too low for 5 instances users see for 1-2 seconds.

---

## Wave C: Form Layer

> ~1.5 sessions. Creates 2 new composables, modifies ~24 files.
> Every form screen transforms. Users spend time on these screens — high dwell time.

---

### C1. TextField (#3) — "Ink Border Field"

**What**: ThemedTextField with wobbly ink border box in Paper & Ink mode.
**Impact**: ~50 calls across 19 files. The most-repeated Material3 element in the app.

**New composable**: `ThemedTextField` in `ui/components/ThemedTextField.kt` (new file)

**Design**:
- Keep the familiar box shape (users expect a bounded input area)
- Border: wobbly ink border (same technique as InkBorderCard but smaller scale)
- Background: transparent — paper texture shows through
- Focused: border alpha fades from `InkTokens.borderSubtle` → `InkTokens.borderBold` via `tween(PaperInkMotion.DurationMedium)`
- Unfocused: border at `InkTokens.borderSubtle` alpha
- Error: border switches to `colorScheme.error` color + horizontal shake (`InkTokens.shakeOffset`, `PaperInkMotion.ShakeSpring`)
- Label: standard floating label behavior (Material3 handles this)

**Implementation approach**:

Since our BOM (2024.02.00) does NOT expose `DecorationBox` or `ContainerBox` APIs:

- **Modern mode**: Delegate entirely to `OutlinedTextField` with all params forwarded
- **Paper & Ink mode**: Use `OutlinedTextField` BUT override its `colors` to:
  - `focusedBorderColor = Color.Transparent` (hide Material3's border)
  - `unfocusedBorderColor = Color.Transparent` (hide Material3's border)
  - `errorBorderColor = Color.Transparent` (we draw our own error border)
  - `focusedContainerColor = Color.Transparent`
  - `unfocusedContainerColor = Color.Transparent`
  - Then add a `Modifier.drawBehind { }` to draw the wobble-border ourselves
  - This preserves ALL of OutlinedTextField's internal layout (labels, slots, animation, padding, IME handling) while replacing only the visual border

**Wobble border drawing** (in `drawBehind`):
- Reuse `buildWobbleBorderPath()` from `InkBorderCard.kt` — extract to a shared utility
- 2 layers (simpler than card — fields are smaller):
  - Layer 1: Bleed — `borderColor.copy(alpha = InkTokens.fillBleed)`, width = `InkTokens.strokeMedium * 2`
  - Layer 2: Primary — `borderColor.copy(alpha = animatedAlpha)`, width = `InkTokens.strokeMedium`
- Wobble: `amplitude = InkTokens.wobbleSmall`
- Border color: `colorScheme.onSurface` for normal, `colorScheme.error` for error state
- Border alpha: animated between `InkTokens.borderSubtle` (unfocused) and `InkTokens.borderBold` (focused) using `animateFloatAsState(tween(PaperInkMotion.DurationMedium))`

**Error shake animation**:
- `Modifier.offset(x = shakeOffset)` where shakeOffset is animated:
  - On error becoming true: `PaperInkMotion.ShakeSpring` animates to `InkTokens.shakeOffset` then damps to 0
- Reduce motion: skip shake, just change border color

**API signature**:
```kotlin
@Composable
fun ThemedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    prefix: @Composable (() -> Unit)? = null,
    suffix: @Composable (() -> Unit)? = null,
    supportingText: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    singleLine: Boolean = false,
    minLines: Int = 1,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    textStyle: TextStyle = LocalTextStyle.current,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    shape: Shape = MaterialTheme.shapes.small,
    colors: TextFieldColors = OutlinedTextFieldDefaults.colors(),
)
```

**Files to modify**: ~19 files with ~50 OutlinedTextField calls
**Files to create**: `ui/components/ThemedTextField.kt`

**What NOT to convert** (keep as OutlinedTextField):
- `ShoppingListScreen` inline quick-add field (pill shape, custom colors — intentionally different UX)
- `CommonComponents.kt` `DropdownField` / `AutoCompleteTextField` (inside ExposedDropdownMenuBox — anchor system depends on OutlinedTextField)
- Any field with custom `shape = RoundedCornerShape(24.dp)` or similar
- Count: ~8 fields excluded → ~50 converted

**Edge cases**:
- **Field with prefix** (ItemFormScreen currency): `prefix` slot passes through to OutlinedTextField. Works unchanged.
- **Read-only field**: `readOnly = true` passes through. Works unchanged.
- **Multi-line field** (CookScreen notes): Wobble border scales with field size (based on `size.width`/`size.height` in `drawBehind`).
- **Dark mode**: Border color `colorScheme.onSurface` is white in dark mode. Alpha tokens are the same in both modes.
- **Performance**: Each field has one `drawBehind` with one cached `buildWobbleBorderPath`. 8 fields on a form = 8 cached paths — negligible.

**Tokens used**: `InkTokens.strokeMedium`, `InkTokens.wobbleSmall`, `InkTokens.fillBleed`, `InkTokens.borderSubtle`, `InkTokens.borderBold`, `InkTokens.shakeOffset`, `PaperInkMotion.DurationMedium`, `PaperInkMotion.ShakeSpring`, `colorScheme.onSurface`, `colorScheme.error`

---

### C2. Switch (#10) — "Ink Checkbox"

**What**: ThemedSwitch that renders as a hand-drawn checkbox with animated checkmark.
**Impact**: 7 instances in 5 files. Small scope, strong micro-interaction.

**New composable**: `ThemedSwitch` in `ui/components/ThemedSwitch.kt` (new file)

**Design**:
- **Off state**: `InkTokens.checkboxSize` wobbly ink square (empty). Border: `colorScheme.onSurface.copy(alpha = InkTokens.borderMedium)`, stroke: `InkTokens.strokeMedium`
- **On state**: Same wobbly square + animated checkmark inside
  - Checkmark path: starts at bottom-left (30%, 55%), dips to (45%, 70%), rises to top-right (75%, 30%)
  - Write-in animation: path fraction animates 0→1 over `tween(PaperInkMotion.DurationSettle)` with spring
  - Checkmark stroke: `InkTokens.strokeBold`, `colorScheme.primary` color, round cap
- **Wobble**: `InkTokens.wobbleSmall` amplitude

**Implementation**:
```kotlin
@Composable
fun ThemedSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
)
```

- Modern mode: delegate to standard `Switch`
- Paper & Ink mode: `Canvas(modifier.size(InkTokens.checkboxSize))` with:
  - Wobble square border (simplified — 4 sides, 2 segments each)
  - Animated checkmark using `animateFloatAsState(if (checked) 1f else 0f, tween(PaperInkMotion.DurationSettle))`
  - `pathMeasure.getSegment(0, length * animatedFraction, dst, true)` to progressively reveal

**Files to modify**:
- `ItemFormScreen.kt` — 2 switches (favorite, paused)
- `LocationFormScreen.kt` — 1 switch (isActive)
- `CategoryFormScreen.kt` — 1 switch (isActive)
- `CookScreen.kt` — 1 switch (flexibleIngredients)
- `SettingsScreen.kt` — 2 switches (notificationsEnabled + SettingToggleRow)

**Edge cases**:
- **`onCheckedChange = null` pattern**: 5 of 7 switches use this. ThemedSwitch supports null — just renders visual state.
- **Accessibility**: Set `Role.Switch` in semantics (NOT `Role.Checkbox`) — visual is checkbox but semantic role remains Switch.
- **Size**: `InkTokens.checkboxSize` (22dp) is smaller than standard Switch (52x32dp). Parent Row's `toggleable` modifier ensures 48dp touch target.
- **Reduce motion**: Skip checkmark animation. Show at full state immediately.
- **Dark mode**: Square border uses `colorScheme.onSurface` (white in dark). Checkmark uses `colorScheme.primary`.

**Tokens used**: `InkTokens.checkboxSize`, `InkTokens.strokeMedium`, `InkTokens.strokeBold`, `InkTokens.borderMedium`, `InkTokens.wobbleSmall`, `PaperInkMotion.DurationSettle`, `colorScheme.onSurface`, `colorScheme.primary`

---

## Wave D: Interaction Layer

> ~1.5 sessions. Creates 3 new composables, modifies ~30 files.
> Key CTAs and the Cook showpiece screen transform.

---

### D1. Buttons (#5) — Selective Ink Stamp

**What**: ThemedButton and ThemedTextButton for high-visibility actions.
**Impact**: ~30-40 instances across ~25 files. NOT all 258 buttons.

**New composables**: `ThemedButton` and `ThemedTextButton` in `ui/components/ThemedButton.kt` (new file)

**ThemedButton** (replaces `Button`, `FilledTonalButton` at key sites):
- Paper & Ink mode:
  - Shape: InkBorderCard-style wobbly border, pill-shaped (`cornerRadius = height/2`)
  - Fill: `colorScheme.primary.copy(alpha = InkTokens.fillLight)` — subtle ink wash tint
  - Border: 2-layer ink stroke — bleed at `InkTokens.fillBleed`, primary at `colorScheme.primary`
  - Stroke: `InkTokens.strokeMedium`, wobble: `InkTokens.wobbleSmall`
  - Text: inherits Mali font from theme, `colorScheme.primary` color
  - Press: scale feedback via InteractionSource (detect PressInteraction, animate to `PaperInkMotion.PressScale`). NOT `inkPress()` — gesture conflict with Button's onClick.
  - Disabled: border alpha `InkTokens.disabledBorder`, text alpha `InkTokens.disabledContent`
- Modern mode: delegate to standard `Button`

**ThemedTextButton** (replaces `TextButton` at key sites):
- Paper & Ink mode:
  - No border, no fill
  - Text: Mali font, `colorScheme.primary` color
  - Subtle ink underline beneath text: wobble line, stroke `InkTokens.strokeFine`, alpha `InkTokens.borderMedium`
  - Press: scale via InteractionSource (`PaperInkMotion.PressScale`)
- Modern mode: delegate to standard `TextButton`

**NOT creating**: ThemedOutlinedButton — `OutlinedButton` is rarely used in the app.

**Which ~30-40 buttons to convert** (high-visibility targets):

| Category | Files | ~Count |
|----------|-------|--------|
| Form save/submit | ItemFormScreen, CategoryFormScreen, SubcategoryFormScreen, LocationFormScreen | ~6 |
| Dialog primary actions | Inside ThemedAlertDialog (automatic via Wave B) | ~19 |
| Hero CTAs | DashboardScreen, EmptyStateIllustration | ~4 |
| Onboarding actions | OnboardingScreen, PathChoicePage, TypePathPage, etc. | ~6 |
| AI screen actions | CookScreen ("Get Suggestions"), AiSignInGate ("Sign In") | ~3 |
| Shopping list | ShoppingListScreen (clear, add batch) | ~2 |

**What to leave as standard**:
- Small TextButtons inside ListItem trailing slots
- IconButton wrapping (not a text button)
- AnimatedSaveButton (custom animation component — too complex to wrap)
- Any button with custom animations already applied

**Edge cases**:
- **Button with icon**: Content slot handles this — it's just a Row.
- **Button enabled/disabled**: Accept `enabled: Boolean`, use `InkTokens.disabledBorder` / `InkTokens.disabledContent`.
- **inkPress inside Button**: DON'T use `inkPress()` modifier. Use InteractionSource instead (see approach above).
- **Reduce motion**: Skip press scale animation. Border wobble remains (static shape).

**Tokens used**: `InkTokens.fillLight`, `InkTokens.fillBleed`, `InkTokens.strokeMedium`, `InkTokens.strokeFine`, `InkTokens.wobbleSmall`, `InkTokens.borderMedium`, `InkTokens.disabledBorder`, `InkTokens.disabledContent`, `PaperInkMotion.PressScale`, `colorScheme.primary`

---

### D2. Chips (#8) — Cook-First, Then All FilterChip

**What**: ThemedFilterChip with wobbly ink border and ink bleed fill.
**Impact**: ~20 FilterChip instances across ~8 files. CookScreen alone has 12.

**New composable**: `ThemedFilterChip` + `ThemedInputChip` in `ui/components/ThemedChip.kt` (new file)

**Design**:
- **Unselected**: Transparent background, wobbly ink border (pill shape, `cornerRadius = height/2`), Mali text
  - Border: `colorScheme.onSurface.copy(alpha = InkTokens.borderMedium)`, stroke: `InkTokens.strokeMedium`, wobble: `InkTokens.wobbleSmall`
- **Selected**: Ink wash fill at `InkTokens.fillLight`, border darkens to `InkTokens.borderBold`, text weight bold
  - Fill transition: alpha animates 0 → `InkTokens.fillLight` over `tween(PaperInkMotion.DurationEntry)` (ink "bleeds in")
  - Border alpha: animates `InkTokens.borderMedium` → `InkTokens.borderBold` over `tween(PaperInkMotion.DurationEntry)`
- **Leading icon**: `ThemedIcon` at 18dp (standard chip icon size)
- **Dismiss icon** (InputChip): Small ink "×" drawn with Canvas

**Also handle InputChip** (CookScreen hero ingredients):
- `ThemedInputChip` alongside ThemedFilterChip — same wobbly border style but with close/remove trailing icon
- Only 2-3 InputChip instances, all on CookScreen

**Defer**:
- `AssistChip` (ItemDetail quick actions) — informational, low interaction
- `SuggestionChip` (ReceiptScan summary) — read-only

**Files to modify**:
- `CookScreen.kt` — 12 FilterChip + 2 InputChip
- `ItemListScreen.kt` — category filter chips
- `PantryHealthScreen.kt` — period selector chips
- `PurchaseHistoryScreen.kt` — purchase filter chips
- `ExpiringReportScreen.kt`, `SpendingReportScreen.kt`, `UsageReportScreen.kt` — filter chips

**Edge cases**:
- **CookScreen InputChip with custom containerColor**: ThemedInputChip must accept a custom selected fill color that overrides the default `InkTokens.fillLight`.
- **Many chips on one screen**: CookScreen has 12+ chips. Paths are small (pill, 4 segments), cached with `remember`. Profile after implementation.
- **FlowRow layout**: ThemedFilterChip must have the same sizing as standard FilterChip.
- **Reduce motion**: Skip fill alpha animation (show final state immediately). Wobble border is static.

**Tokens used**: `InkTokens.borderMedium`, `InkTokens.borderBold`, `InkTokens.fillLight`, `InkTokens.strokeMedium`, `InkTokens.wobbleSmall`, `PaperInkMotion.DurationEntry`, `colorScheme.onSurface`

---

## Wave E: Polish & Deferred

> ~1 session. Opportunistic. Complete the form controls family, add ink loading state.

---

### E1. InkSpinner (#17) — "Ink Scribble Spinner"

**What**: Custom loading spinner that draws like a pen scribbling a circle.
**Impact**: 14 instances across 13 files.

**New composable**: `ThemedCircularProgress` in `CommonComponents.kt`

**Design**:
- Circular path with slight wobble (not a perfect circle)
- Stroke: `InkTokens.strokeBold`, `colorScheme.onSurface.copy(alpha = InkTokens.borderMedium)`, round cap
- Animation: Path trim cycles over `PaperInkMotion.DurationChart` (800ms)
- Alternative if too complex: Three ink dots that breathe in sequence

- Modern mode: standard `CircularProgressIndicator`
- Paper & Ink mode: InkSpinner

**Migration**: `CircularProgressIndicator()` → `ThemedCircularProgress()`

**Tokens used**: `InkTokens.strokeBold`, `InkTokens.borderMedium`, `PaperInkMotion.DurationChart`, `colorScheme.onSurface`

---

### E2. Checkbox / RadioButton (#15) — Ink Form Controls

**ThemedCheckbox**: Same drawing code as ThemedSwitch's ink checkbox. Extract shared helper. Different semantics (`Role.Checkbox`).

**ThemedRadioButton**:
- Wobbly ink circle (`InkTokens.radioSize`), stroke: `InkTokens.strokeMedium`, wobble: `InkTokens.wobbleSmall`
- Selected: filled ink dot scales in from center (`PaperInkMotion.BouncySpring`, 0→1)
- Unselected: empty wobbly circle

**Tokens used**: `InkTokens.checkboxSize`, `InkTokens.radioSize`, `InkTokens.strokeMedium`, `InkTokens.strokeBold`, `InkTokens.wobbleSmall`, `InkTokens.borderMedium`, `PaperInkMotion.BouncySpring`, `PaperInkMotion.DurationSettle`

---

### E3. Status Bar (#23) — Light/Dark Icon Control

**What**: Set status bar icon color based on current theme.
**Impact**: 1 file (`MainActivity.kt`), 5-minute fix.

**Implementation**: Read `isSystemInDarkTheme()` (or selected color palette), set `isAppearanceLightStatusBars`.

---

### E4. DropdownMenu (#16) — Try Simple Override

**What**: Attempt to theme DropdownMenu with `Modifier.background(colorScheme.surfaceVariant, MaterialTheme.shapes.medium)`.
**Impact**: 5 instances in 4 files. Ship if it works, defer if not.

---

### E5. Deferred Permanently (Do When Touching Files)

| Item | Why |
|------|-----|
| #14 Raw Surface/Card audit (22) | Per-file cleanup. Touch when editing those files |
| #21 Animation token drift (~50) | Code quality only, zero visual change |
| #22 Pixel stroke widths (2 files) | Barely visible. Fix when touching Charts.kt |
| #13 ListItem | No wrapper needed — handled by AppCard + ThemedDivider |

---

## Implementation Checklist

### Per-Wave Build/Test Protocol

After completing each wave:
1. `bash scripts/build.sh` — must compile
2. Install on emulator
3. Navigate to affected screens in Paper & Ink mode — verify visual correctness
4. Switch to Modern mode — verify no regression (everything identical to before)
5. Toggle dark mode in both visual styles — verify colors adapt
6. Enable reduce motion (system settings) — verify no animations, correct final states
7. Update tracking files (FUTURE_IMPROVEMENTS.md, COMPLETED_WORK.md, Features.md)

### New Files Created (Total)

| File | Wave | Contains |
|------|------|----------|
| `ui/components/ThemedAlertDialog.kt` | B | ThemedAlertDialog |
| `ui/components/ThemedTextField.kt` | C | ThemedTextField |
| `ui/components/ThemedSwitch.kt` | C | ThemedSwitch |
| `ui/components/ThemedButton.kt` | D | ThemedButton, ThemedTextButton |
| `ui/components/ThemedChip.kt` | D | ThemedFilterChip, ThemedInputChip |

### Existing Files Extended

| File | Wave | Additions |
|------|------|-----------|
| `VisualStyle.kt` | Pre | `InkTokens` object, `ThemeVisuals.isInk` extension |
| `Animation.kt` | Pre | `DurationQuick`, `DurationEntry`, `DurationSettle`, `ShakeSpring`, `DialogEnterScale`, `DialogExitScale` in PaperInkMotion |
| `CommonComponents.kt` | A, B, E | ThemedSnackbarHost, ThemedTopAppBar, InkDashDragHandle, ThemedBottomSheet, ThemedCircularProgress |
| `AnimatedComponents.kt` | A | ThemedFab, AnimatedFab update |
| `InkBorderCard.kt` | C | Extract `buildWobbleBorderPath` to shared utility |
| `MainActivity.kt` | E | Status bar light/dark |

### Total Scope Estimate

| Wave | New Composables | Files Modified | Risk |
|------|----------------|----------------|------|
| Pre | 0 (token objects) | 2 | None — additive only |
| A | 3 | ~35 | Low — mechanical replacements |
| B | 3 | ~18 | Medium — custom Dialog needs testing |
| C | 2 | ~24 | Medium — TextField is complex |
| D | 4 | ~30 | Medium — selective replacement |
| E | 4 | ~15 | Low — small components |
| **Total** | **16** | **~120** | |

---

## Performance Budget

### Screens with highest custom-draw density (after all waves):

| Screen | Estimated custom Canvas draws | Risk |
|--------|------------------------------|------|
| CookScreen | 12 chips + 3 fields + 2 buttons + dividers ≈ 20 | Profile after Wave D |
| ItemFormScreen | 8 fields + 2 switches + 1 button ≈ 12 | Profile after Wave C |
| ShoppingListScreen | 5 fields + chips + dividers ≈ 10 | Profile after Wave C |
| ItemListScreen | Category chips + dividers ≈ 8 | Low risk |

**Mitigation if slow**:
- Reduce chip wobble segments from 4 to 2 (simpler pill paths)
- Use `graphicsLayer` caching on repeated identical shapes
- Worst case: drop wobble on chips entirely, keep just ink wash fill (still papery, zero Canvas cost)

---

## Dependency Graph

```
Pre-Wave (token infrastructure — do FIRST)
  └── VisualStyle.kt (InkTokens + isInk) + Animation.kt (new PaperInkMotion tokens)

Wave A (depends on Pre-Wave)
  ├── A1 Snackbar
  ├── A2 TopAppBar
  └── A3 FAB

Wave B (depends on Pre-Wave)
  ├── B1 AlertDialog
  └── B2 BottomSheet

Wave C (depends on Pre-Wave)
  ├── C1 TextField
  └── C2 Switch

Wave D (depends on Pre-Wave, benefits from B1 for dialog buttons)
  ├── D1 Buttons ──► ThemedAlertDialog auto-upgrades when TextButton → ThemedTextButton
  └── D2 Chips

Wave E (E2 shares code with C2)
  ├── E1 InkSpinner
  ├── E2 Checkbox/Radio (reuses C2's ink checkbox drawing)
  ├── E3 Status bar
  └── E4 DropdownMenu (try)
```

Waves A through D can technically be done in any order after Pre-Wave. The recommended order (A→B→C→D) follows the "outside-in" principle: fix containers first, controls inherit the vibe.
