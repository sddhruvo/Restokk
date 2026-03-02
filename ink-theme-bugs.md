# Ink Theme Testing — Bug Report

> **Date**: 2026-03-02 | **Tester**: Claude (Session 111)
> **Scope**: Visual testing of Waves A-E across all major screens
> **Themes tested**: Classic Green + P&I, Modern (regression), AMOLED Dark + Modern, AMOLED Dark + P&I
> **Reference docs**: `ink-theme-implementation-plan.md`, `ThemeArchitecture.md`, `themereview.md`

---

## Bug Summary

| # | Severity | Screen | Title | Status |
|---|----------|--------|-------|--------|
| INK-1 | **HIGH** | ThemedAlertDialog | Dialog background semi-transparent — content bleeds through | **Fixed** (Session 112) |
| INK-2 | **HIGH** | ThemedAlertDialog | Scrim uses `onSurface` — inverts to white overlay in dark mode | **Fixed** (Session 112) |
| INK-3 | MEDIUM | ThemedAlertDialog | Cramped internal padding — title/buttons too close to borders | **Fixed** (Session 112) |
| INK-4 | MEDIUM | Item Form / Bottom Sheet | Dropdown fields (ExposedDropdownMenuBox) have smooth Material borders while adjacent text fields have wobbly ink borders | **Fixed** (Session 112) |
| INK-5 | MEDIUM | Item Form / Settings | "Update Item" and "Save Settings" buttons are solid opaque Material green — not ThemedButton (while Cook! button IS correctly themed) | **Fixed** (Session 112) |
| INK-6 | MEDIUM | Settings | "What Matters Most" radio buttons all show identical green dots — no visible selected/unselected distinction | **Fixed** (Session 112) |
| INK-7 | MEDIUM | Pantry Health | Score gauge is clean geometric arc — not ink-styled, clashes with wobbly ink borders on same screen | **Fixed** (Session 112) |
| INK-8 | LOW | Cook Screen | "Elaborate" chip text wraps to 2 lines ("Elabor / ate") — possible padding regression from ThemedFilterChip | **Fixed** (Session 112) |
| INK-9 | LOW | Settings | "Replay Onboarding" button has standard smooth rounded border, not ThemedButton | **Fixed** (Session 112) |
| INK-10 | LOW | Bottom Sheet | Missing InkDashDragHandle — no drag handle visible at top of ThemedBottomSheet | Verified wired — visual check needed |
| INK-11 | MEDIUM | Item Detail | "Shopping" floating button text fragments ("Sh/op/pin/g") bleeding in from right edge | **Fixed** (Session 112) |

---

## Detailed Bug Descriptions

### INK-1: ThemedAlertDialog — Semi-Transparent Background (HIGH)

**What**: The dialog card's InkBorderCard fill is not fully opaque. Background list items (category names, icons, text) are clearly visible THROUGH the dialog card body, overlapping with dialog content text.

**Where**: `ui/components/ThemedAlertDialog.kt`

**Observed in**: Classic Green + P&I (light), AMOLED Dark + P&I (worse)

**Evidence**:
- Light mode: "Oils & Vinegars", "Baby Food" rows visible through dialog while reading "Are you sure you want to delete..."
- Dark mode: Even worse — dark card fill on dark background provides almost no opacity separation

**Root cause**: InkBorderCard likely uses `colorScheme.surface` or a semi-transparent fill. For overlays, the dialog needs a fully opaque background.

**Fix approach**: The dialog's InkBorderCard must use a **fully opaque** fill color — `colorScheme.surface` at `alpha = 1.0f`. This should be a dedicated token (e.g., `InkTokens.dialogFillAlpha = 1.0f`) so it's centralized. The InkBorderCard already accepts a `fillColor` parameter — pass an opaque surface color.

**Architecture note**: Any new token must go into `InkTokens` in `VisualStyle.kt`. No hardcoded alpha values in the composable.

---

### INK-2: ThemedAlertDialog — Scrim Inverts in Dark Mode (HIGH)

**What**: The dialog scrim uses `colorScheme.onSurface.copy(alpha = InkTokens.scrimDialog)`. In dark mode, `onSurface` is white — this creates a WHITE overlay at 35% opacity, making the background LIGHTER instead of darker. The dialog (dark card) recedes behind the lighter scrim'd area, creating an inverted visual hierarchy.

**Where**: `ui/components/ThemedAlertDialog.kt`

**Observed in**: AMOLED Dark + P&I

**Evidence**: Background categories appear as milky gray through the white scrim, while the dialog card is nearly black. Dialog doesn't pop out — it recedes.

**Fix approach**: Use `colorScheme.scrim` (Material3 provides this — it's always dark/black regardless of theme) or a fixed `Color.Black.copy(alpha = InkTokens.scrimDialog)`. Add a dedicated `scrimColor` property to `InkTokens` or use `colorScheme.scrim` directly. The same fix should apply to `ThemedBottomSheet` scrim for consistency.

**Architecture note**: Scrim color should be theme-aware but NOT `onSurface`-based. `colorScheme.scrim` is the Material3 standard. If we need a custom warm tint for light mode, consider adding a `scrimBase` to `AppColors`.

---

### INK-3: ThemedAlertDialog — Cramped Padding (MEDIUM)

**What**: Dialog title sits too close to the top wobbly border, and buttons sit too close to the bottom border. Content feels squeezed inside the card.

**Where**: `ui/components/ThemedAlertDialog.kt`

**Fix approach**: Increase internal padding. Use `Dimens.spacingLg` (16dp) or `Dimens.spacingXl` (24dp) for dialog content padding — these are centralized spacing tokens from `Dimens.kt`. Material3 AlertDialog uses 24dp horizontal, 24dp top, 24dp bottom by convention. Match or exceed this.

---

### INK-4: Dropdown Fields — Smooth Borders vs Wobbly Text Fields (MEDIUM)

**What**: On forms (ItemFormScreen, AddShoppingItemSheet, SettingsScreen), regular ThemedTextField fields have wobbly ink borders, but dropdown fields (Category, Location, Unit, Subcategory, Priority) have standard smooth Material OutlinedTextField borders. When placed side-by-side on the same row (e.g., Quantity [wobbly] next to Unit [smooth]), the inconsistency is jarring.

**Where**: Any screen with ExposedDropdownMenuBox — `ItemFormScreen.kt`, `AddShoppingItemSheet.kt`, `SettingsScreen.kt`, `LocationFormScreen.kt`, `CategoryFormScreen.kt`

**Root cause**: The implementation plan explicitly excluded dropdown fields: "ExposedDropdownMenuBox — anchor system depends on OutlinedTextField". The `DropdownField` and `AutoCompleteTextField` in `CommonComponents.kt` still use raw `OutlinedTextField`.

**Fix approach**: Apply the same visual treatment to dropdown fields. Two options:
1. **Preferred**: Modify `DropdownField`/`AutoCompleteTextField` in `CommonComponents.kt` to use the same `Modifier.drawBehind` wobble-border approach as ThemedTextField — hide the Material border via transparent colors, draw wobbly border behind. The `ExposedDropdownMenuBox` anchor system only needs the OutlinedTextField composable, not its specific visual border.
2. **Fallback**: If the anchor system breaks, apply the wobble border as an outer wrapper `Box` with `drawBehind` around the entire ExposedDropdownMenuBox, and hide the inner OutlinedTextField border.

**Architecture note**: The wobble-border drawing code should be a shared utility (already extracted to `buildWobbleBorderPath()` in InkBorderCard.kt). Reuse it — don't duplicate.

---

### INK-5: Save/Submit Buttons — Solid Material Green (MEDIUM)

**What**: "Update Item" (ItemFormScreen) and "Save Settings" (SettingsScreen) are solid opaque green buttons with white text — standard Material `Button`. Meanwhile, the "Cook!" button on CookScreen correctly uses ThemedButton with ink wash fill and wobbly border. Same type of primary CTA, completely different visual treatment.

**Where**: `ItemFormScreen.kt` (AnimatedSaveButton), `SettingsScreen.kt` (save button)

**Root cause**: The implementation plan excluded `AnimatedSaveButton` as "too complex to wrap". The SettingsScreen save button was likely missed during Wave D migration.

**Fix approach**:
- **SettingsScreen save button**: Convert to ThemedButton directly.
- **AnimatedSaveButton**: This has custom save/loading/success animation states. The fix is NOT to replace it entirely, but to change its VISUAL appearance in P&I mode — use ink wash fill + wobbly border instead of solid green. Keep the animation logic. Check `isInk` inside AnimatedSaveButton: if ink, use InkBorderCard-style rendering; if modern, keep current Material Button.

**Architecture note**: The button should read fill alpha from `InkTokens.fillLight`, border from `InkTokens.strokeMedium`, etc. No hardcoded values.

---

### INK-6: Settings Radio Buttons — No Selected State (MEDIUM)

**What**: "What Matters Most" section shows 3 priority options (Never waste food, Always know what I have, Cook with what's here). All three display identical small green dots — no way to tell which one is currently selected.

**Where**: `SettingsScreen.kt` — the priority selector section

**Root cause**: Likely using ThemedRadioButton (E2), but the selected/unselected visual distinction isn't prominent enough. Or using a custom dot indicator that doesn't branch on selection state.

**Fix approach**: If using ThemedRadioButton — verify the selected state renders a filled ink dot inside the wobbly circle (as per E2 spec). If using custom green dots — replace with ThemedRadioButton. The selected radio should have `colorScheme.primary` filled dot with `PaperInkMotion.BouncySpring` entrance; unselected should be empty wobbly circle at `InkTokens.borderMedium` alpha.

---

### INK-7: Pantry Health Score Gauge — Geometric Arc (MEDIUM)

**What**: The circular score gauge (64/100) is a perfectly smooth, geometric arc. It sits on a screen surrounded by wobbly ink borders, hand-drawn chips, and paper texture. The clean geometric arc clashes with the organic ink aesthetic.

**Where**: `PantryHealthScreen.kt` — score arc rendering (likely Canvas drawArc)

**Fix approach**: In P&I mode, add subtle wobble to the arc path. Instead of `drawArc()`, use a custom path with slight bezier wobble points (same technique as InkBorderCard but for a circular path). Use `InkTokens.wobbleSmall` for amplitude. Read stroke from `InkTokens.strokeBold`. The unfilled arc portion should use `colorScheme.outlineVariant` instead of `Color.Gray` (themereview Issue #3).

**Architecture note**: The arc color should come from `MaterialTheme.appColors.scoreToColor(score)`. The background arc should use `colorScheme.outlineVariant`. Both are theme-system colors.

---

### INK-8: "Elaborate" Chip Text Wrapping (LOW)

**What**: The "Elaborate" chip in CookScreen's Effort section wraps to 2 lines: "Elabor" / "ate". Standard FilterChip fit this text on one line.

**Where**: `CookScreen.kt` — Effort filter chips; `ThemedChip.kt` — ThemedFilterChip

**Root cause**: ThemedFilterChip likely has slightly different internal padding or size constraints than standard FilterChip. The wobble border drawing adds visual space but may also affect layout.

**Fix approach**: Compare ThemedFilterChip's `minHeight` and horizontal padding with standard FilterChip. Ensure the content area is at least as wide. May need to reduce horizontal padding slightly, or ensure `singleLine = true` on the label text.

---

### INK-9: "Replay Onboarding" Button — Standard Border (LOW)

**What**: The "Replay Onboarding" button in Settings has a smooth rounded border, not a wobbly ink border. It's inside an InkBorderCard section, making the smooth button border stand out.

**Where**: `SettingsScreen.kt`

**Fix approach**: Convert to ThemedButton or ThemedTextButton.

---

### INK-10: Bottom Sheet — Missing InkDashDragHandle (LOW)

**What**: ThemedBottomSheet has no visible drag handle. The B2 spec called for `InkDashDragHandle()` — a wobbly line with round caps, subtle inkBreathe modifier, at the top of the sheet.

**Where**: `CommonComponents.kt` — ThemedBottomSheet implementation

**Fix approach**: Verify InkDashDragHandle composable exists. If it exists, check that ThemedBottomSheet passes it as the `dragHandle` parameter. If it doesn't exist, create it per the B2 spec: `InkTokens.dragHandleWidth × InkTokens.dragHandleHeight` Canvas, wobble bezier, `InkTokens.strokeMedium`, `colorScheme.onSurfaceVariant.copy(alpha = InkTokens.borderSubtle)`.

---

### INK-11: Item Detail — "Shopping" Text Bleeding from Right (MEDIUM)

**What**: On the ItemDetailScreen, text fragments "Sh", "op", "pin", "g" (spelling "Shopping") are visible bleeding in from the right edge of the screen. This is a floating "Add to Shopping List" button or card that's not properly clipped or positioned.

**Where**: `ItemDetailScreen.kt`

**Note**: User confirmed this is a pre-existing layout bug (not from ink theme work). The button to add item to shopping list overflows on some screens. Documenting here for tracking.

**Fix approach**: Find the shopping list action button/card in ItemDetailScreen. Check its positioning constraints — likely needs a max width constraint or proper horizontal clipping. May need `Modifier.clipToBounds()` on a parent container, or the button's x-position needs adjusting.

---

## Pre-Existing Issues (from themereview.md, NOT from ink wave)

These were documented before Waves A-E and remain open:

| # | Issue | File | Fix |
|---|-------|------|-----|
| TR-2 | Score colors hardcoded in DashboardScreen | `DashboardScreen.kt` | Use `MaterialTheme.appColors.scoreTeal` / `.scoreBlue` |
| TR-3 | Duplicate score colors in PantryHealthScreen | `PantryHealthScreen.kt` | Delete local `TealColor`/`BlueInfo`, use AppColors |
| TR-4 | Theme circle colors duplicated in SettingsScreen | `SettingsScreen.kt` | Import from Color.kt |
| TR-6 | CategoryFormScreen fallback `Color.Gray` | `CategoryFormScreen.kt` | Use `surfaceVariant` |
| TR-7 | CategoryFormScreen icon always white | `CategoryFormScreen.kt` | Luminance detection |
| TR-8 | HorizontalBarChart `Color.LightGray` | `Charts.kt` | Use `surfaceVariant` |
| TR-9 | Shopping List budget bar hardcoded green | `ShoppingListScreen.kt` | Use `StockGreen` |
| TR-10 | CategoryListScreen `Color(0xFF6c757d)` | `CategoryListScreen.kt` | Use `onSurfaceVariant` |
| TR-11 | LocationListScreen `Color(0xFF6c757d)` | `LocationListScreen.kt` | Use `onSurfaceVariant` |

Note: `AppColors.scoreToColor()` still has `Color.Gray` fallback at score=0. Should use `colorScheme.outlineVariant`.

---

## Architecture Rules for Fixes

1. **All visual values through token system** — `InkTokens`, `AppColors`, `MaterialTheme.colorScheme`, `Dimens`, `PaperInkMotion`
2. **No hardcoded colors/alphas/sizes in composable files** — everything reads from centralized tokens
3. **New tokens go in `VisualStyle.kt`** (`InkTokens` object) or `AppColors.kt`
4. **New durations/springs go in `Animation.kt`** (`PaperInkMotion` object)
5. **`isInk` check pattern** — every Themed* composable branches on `MaterialTheme.visuals.isInk`
6. **Modern mode must not regress** — every fix must verify standard Material rendering in Modern mode
7. **Dark mode must work** — every fix must verify with AMOLED Dark palette
8. **Shared drawing code** — reuse `buildWobbleBorderPath()` from `InkBorderCard.kt`, don't duplicate
