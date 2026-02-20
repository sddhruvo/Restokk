# Home Inventory App - Changelog

## v1.27.0 - Spending Report Overhaul (Session 27)

### Spending Report — 4 New Sections
- **Period Comparison**: Side-by-side bars comparing this period vs last period for total spending and fair per-active-day averages (only counts days with purchases). Footnote explains "Per day with purchases".
- **By Store Chart**: Horizontal bar chart showing spending breakdown by store. Uses existing stores from purchase history.
- **Most Bought Items**: Ranked list of most frequently purchased items (all-time, ≥2 purchases). Numbered rank badges, clickable item names → Item Detail, purchase count badges ("Nx").
- **Purchases Timeline**: Replaced flat "Top 5 Purchases" with full purchase timeline using shared timeline composables. Grouped by date with relative labels ("Today", "Yesterday", "3 days ago").

### Unit Price Trend on Item Detail
- Line chart showing unit price trend over time for items with ≥2 purchases with price data
- Shows above purchase history section on Item Detail screen
- Uses per-unit price (total ÷ quantity) for fair comparison

### Item-Filtered Purchase History
- "View All" button on Item Detail now navigates to Purchase History filtered to that specific item
- Title shows item name when filtered, filter chips hidden for single-item view
- Route: `purchases?itemId=123`

### Shared Timeline Components
- Extracted `TimelineDateHeader` and `TimelinePurchaseItem` to shared `PurchaseTimelineComponents.kt`
- Reused by both Purchase History screen and Spending Report timeline

## v1.25.0 - Receipt Review UX Overhaul + Shopping List Matching Fix (Session 25)

### Modern AI Processing Screen
- **Replaced** simple spinner with a rich multi-step animated processing experience
- **Blurred receipt preview** in background provides context while waiting
- **Receipt thumbnail** card at top shows what's being processed
- **Pulsing AI icon** (AutoAwesome) with animated glow
- **Step-by-step progress**: 7 animated stages appear one by one with slide-in animation
  - Compressing image → Sending to AI → Reading products → Matching prices → Checking shopping list → Estimating expiry → Almost done
  - Completed steps show checkmark, active step has pulsing dot
- **Unit dropdown**: Unit field in pager card is now a dropdown selector (same as manual item form) instead of free-text
- **Smart unit resolution**: AI unit → SmartDefaults fallback (no more "None" by default)
- **Unit conflict hint**: When receipt says "pcs" but inventory item uses "kg", shows subtle red hint: "Receipt says pcs — Inventory uses kg". Disappears when user changes unit.

### Receipt Review UX Improvements (7 fixes)
1. **Inventory match choice**: Match badge is now tappable — opens action menu to switch between "Create new" / "Update existing" / "Skip" (was only accessible via 3-dot menu)
2. **Editable unit field**: Unit is now a separate editable field (was read-only suffix on quantity). Row 3 is now Qty | Unit | Price
3. **AI expiry label**: Shows "Expected expiry" when date is AI-estimated, "Expiry" when manually set (clears flag on user edit)
4. **Even box heights**: Row 3 (Qty/Unit/Price) and Row 4 (Expiry/Barcode) use `IntrinsicSize.Min` so all fields in a row match height
5. **Quantity in summary**: Summary rows now show quantity + unit between name and price (e.g. "Doritos  2 bag  £1.50")
6. **Compact buttons**: Confirm + Review now side-by-side in a Row, Retake is a small text button below. Saves ~1/3 vertical space on small screens
7. **Back confirmation**: Pressing back or the back arrow when scan results exist shows "Discard scan results?" dialog. Prevents accidental loss of API results

### Smarter Shopping List ↔ Receipt Matching
- **Fixed:** Generic shopping list items ("snacks", "tea", "bread") now correctly match specific branded receipt products ("Doritos Cool Ranch", "Tetley Teabags Original", "Hovis Wholemeal 800g")
- **3 structural prompt fixes:**
  1. Shopping list/inventory context now comes BEFORE main instructions (was appended after "Respond ONLY with JSON" — model ignored it)
  2. `matchedShoppingId` and `matchedInventoryId` added to the field list (model didn't know these fields existed)
  3. Example JSON now shows realistic matches (Tetley→Tea, Doritos→Snacks) instead of bare items with no matching
- **Category-level matching:** "Would the person who wrote this list item consider this receipt product as fulfilling it?"
- **Multiple matches:** One shopping list item (e.g. "Snacks") can match multiple receipt items (Doritos + KitKat + Pringles)

### Auto-Retry on Gibberish AI Response
- **Fixed:** Groq's Llama 4 Scout model intermittently returns gibberish (random multilingual text instead of JSON)
- **Auto-retry:** Up to 3 attempts with gibberish detection (checks for valid JSON array brackets)
- **Better error message:** "Failed to parse receipt after 3 attempts. This is usually a temporary AI issue."
- User no longer needs to manually retry when the AI glitches

## v1.24.0 - Receipt Review Screen Overhaul (Session 24)

### Two-Stage Review Flow
- **Summary Screen**: Shows item count + total price, summary chips (N new / N update / N from list), compact scrollable item list. Tap any item to jump to its pager card. Three actions: "Confirm All" (skip review), "Review Items" (enter pager), "Retake" (rescan).
- **Pager Review**: HorizontalPager — one item per full-screen card with swipe navigation. Chevron icons (< >) on edges for tap navigation. Animated progress bar at top ("4/12 reviewed"). Page indicator text ("3 of 15").

### AI Expiry Date Estimation
- Vision model now estimates `estimatedExpiryDays` for each item (milk ~7d, bread ~5d, canned ~730d, etc.)
- Pre-fills expiry date on each item: `today + estimatedExpiryDays`
- Fallback chain: AI estimate → SmartDefaults.shelfLifeDays → no expiry
- Expiry date saved to both ItemEntity and PurchaseHistoryEntity

### Per-Item Barcode Scanning
- Extracted `BarcodeCameraPreview` shared component from `BarcodeScannerScreen.kt`
- Pager card has barcode field — tap opens `ModalBottomSheet` with live camera preview
- On barcode detection: auto-dismiss sheet, set barcode on item
- Barcode saved to ItemEntity on confirm

### Pager Item Card (4-row layout)
- Row 1: Match badge (New/Update/Skip) + 3-dot action menu
- Row 2: Product name text field
- Row 3: Quantity + unit + Price fields
- Row 4: Expiry date (tap → DatePickerDialog, X to clear) + Barcode (tap → camera sheet, X to clear)

### Progress Tracking
- `isReviewed` field on each item, marked when pager page is visited
- Animated `LinearProgressIndicator` shows review progress
- "Confirm All" from summary marks all as reviewed

### Other
- New DAO methods: `updateExpiryDateIfNull()`, `updateBarcodeIfEmpty()` — only update if field is empty
- Explicit `androidx.compose.foundation:foundation` dependency added for HorizontalPager

## v1.20.0 - Waste-Avoidance, Category Sort, Buy Again (Session 20)

### Waste-Avoidance Mode
- When adding a new item to shopping list, fuzzy-matches against inventory (qty > 0)
- High-confidence matches (≥0.8) trigger a warning dialog showing item name, quantity, storage location, and expiry
- User can "Add Anyway" or "Cancel" — suggestion, not a blocker
- Reuses existing `ShoppingListMatcher` 3-stage pipeline; skipped in edit mode

### Category-Based Shopping Sort
- Toggle button in TopAppBar switches between flat list and grouped-by-category view
- Category headers show name + item count (e.g., "Dairy (3)")
- Items without inventory link or category → "Uncategorized" group
- Groups sorted alphabetically by category name

### Quick Re-Buy from Purchase History
- Horizontal "Buy Again" chip ribbon on shopping list screen
- Shows frequently purchased items (≥2 purchases in history), sorted by frequency + recency
- Tap chip to instantly add to shopping list; filters out items already on active list
- Chips show item name and purchase count (e.g., "Milk 5×")

## v1.18.0 - Shopping List Overhaul + Visual Low Stock (Session 16)

### Bottom Nav Badge
- Active shopping list item count displayed as badge on the Shopping tab icon
- Updates in real-time as items are added/purchased

### Visual Low Stock Indicator
- Dashboard low stock cards now show a colored progress bar (qty/minQty ratio)
- Red when < 30% stocked, yellow otherwise; formatted quantities via `formatQty()`

### Shopping List — Edit Items
- New `EditShoppingItem` route; tap any active item row to open edit form pre-filled
- Uses INSERT OR REPLACE for seamless add/edit flow

### Shopping List — Inline Quantity +/- Buttons
- Quick +/- buttons in trailing content for active items
- Min quantity coerced to 0.5; new DAO `updateQuantity()` query

### Shopping List — Notes Visibility
- Notes now shown as secondary text below qty/priority in list rows

### Shopping List — Undo on Delete
- Deleting shows a Snackbar with "Undo" action; tapping restores the item

### Shopping List — Swipe Gestures
- Right swipe → mark as purchased (green background)
- Left swipe → delete with undo (red background)

### Shopping List — Share
- Share icon in top app bar builds a text list and opens Android share sheet

### Shopping List — Budget Indicator
- Estimated total card changes color based on budget proximity (green → yellow → red)
- Progress bar shows spending vs. budget ratio
- "Over Budget!" warning label when exceeding budget

### Shopping List — Auto-Clear Purchased
- New setting: "Auto-Clear Purchased After (Days)" in Settings → Shopping List
- On screen load, purchased items older than N days are automatically deleted

### Shopping List — Batch Quick-Add
- PlaylistAdd icon in top app bar opens multi-line input dialog
- One item per line, optional quantity prefix: "2 Milk", "Eggs", "3x Bread"

### Shopping List — Animated Checkbox
- Checkbox bounces with spring animation when toggling purchased state

### Settings
- New "Shopping List" section with Shopping Budget and Auto-Clear Days fields

### Bug Fix
- Fixed `LinearProgressIndicator` to use lambda `progress` parameter for Material3 1.2.0 compatibility

## v1.17.0 - Smart Auto-Strike (Session 17)

### Shopping List Auto-Strike on Inventory Add
- **New**: When adding a new item to inventory, matching active shopping list items are automatically marked as purchased
- **3-stage matching pipeline**: normalized-contains → Jaccard token similarity → FuzzyWuzzy partial ratio
- **Confidence tiers**: High (≥0.8) auto-strikes with snackbar; Medium (0.6–0.79) shows confirmation dialog; Low (<0.6) ignored
- **Safe**: Uses `markAsPurchasedOnly()` — only flips `is_purchased` flag, no duplicate purchase history or quantity adjustment
- **New dependency**: `fuzzywuzzy-kotlin:0.9.0` (~150KB)
- **New file**: `domain/model/ShoppingListMatcher.kt`
- Only triggers for new items, not edits

## v1.14.0 - Receipt Scanning (Session 14)

### Replaced Image Recognition with Receipt Scanning
- **Removed** ML Kit on-device image labeling (deleted `MLKitRecognitionRepository.kt`, removed `image-labeling` dependency)
- **Removed** old Gemini image recognition (RecognizedItem, recognizeItems, RECOGNITION_PROMPT)
- **Added** ML Kit Text Recognition (`text-recognition:16.0.1`) for on-device OCR
- **Two-phase pipeline**: Camera/Gallery → ML Kit OCR (on-device) → raw text → Gemini 2.5 Pro (text-only) → structured items
- **New** `ReceiptItem` data class and `parseReceiptText(ocrText)` method in GeminiRepository (text-only, no image upload)
- **New** `ReceiptScanViewModel` with states: Idle → Capturing → ReadingText → ParsingWithAI → Review → Saving → Success
- **New** `ReceiptScanScreen` with:
  - Take Photo (CameraX) or Pick from Gallery
  - Two-step loading: "Reading text from receipt..." then "Parsing items with AI..."
  - Editable review list (name, quantity, price per item)
  - Batch add all items to inventory with SmartDefaults enrichment + purchase history records
- Updated navigation route from `recognize` → `receipt-scan`
- Updated labels: Dashboard "Receipt", More menu "Scan Receipt", Settings "Receipt Scanning"

## v1.12.0 - Unified AppCard Style (Session 12)

### Shared AppCard Composable
- New `ui/components/AppCard.kt` — single source of truth for card styling across the entire app
- Two overloads: static (non-clickable) and clickable
- Theme-adaptive: light = `surfaceColorAtElevation(2.dp)` with 2dp elevation; dark/AMOLED = `White@10%` with 0dp elevation
- Default 16dp rounded corners; optional `shape` param for custom corner radius
- Optional `containerColor` param for semantic overrides (errorContainer, primaryContainer, accent tints)

### Consistent Cards Across 18 Files
- Dashboard: Removed private `GlassCard` composable — replaced with `AppCard(shape = 20.dp)` for stat/quick-action cards, `AppCard()` for list-wrapper cards
- Settings/Export-Import: All form section cards now use AppCard
- Item Detail: Quantity card, details grid, notes, usage/purchase history — all AppCard with preserved semantic colors
- Item List: Grid card uses AppCard with conditional `containerColor` for selection state
- Reports (5 screens): Summary stat cards, chart wrappers, empty states — all AppCard
- Barcode Scanner: Permission card, result cards, tips card — all AppCard
- Shopping List: Estimated total card → AppCard
- Purchase History: Summary card, timeline purchase cards → AppCard
- Location Detail, Image Recognition, Category Form — all cards → AppCard
- Cleaned up unused `Card`/`CardDefaults` imports

## v1.11.1 - Dashboard Card Visual Fixes (Session 11)

### GlassCard Border Fix
- Light theme: Removed gradient border entirely — fill + elevation provides clean card edges without visible gap/margin
- Dark/AMOLED theme: Replaced 1.5dp gradient border with 0.5dp solid border (White@15%) — eliminates fake "divider" lines between adjacent stat cards

### Bottom Nav Icon Visibility
- Added explicit `unselectedIconColor` and `unselectedTextColor` (`onSurfaceVariant`) to NavigationBarItemDefaults — unselected icons now visible on AMOLED black backgrounds

## v1.11.0 - Dashboard Overhaul (Session 10)

### Glassmorphism (Haze)
- Stat cards and quick action cards now have frosted glass blur effect (API 31+)
- Graceful fallback to solid surface with 85% alpha on older devices
- Added Haze 1.1.1 dependency (`dev.chrisbanes.haze:haze` + `haze-materials`)

### Layout Overhaul
- Removed floating action button (FAB) — replaced with inline + button in "Expiring Soon" header
- Search icon now styled as white circle with gray magnifying glass in TopAppBar
- Stat card aspect ratio: 1.2f (landscape-ish)
- Quick action card aspect ratio: 1.0f (square) with centered content

### Adaptive Grid (WindowSizeClass)
- Added `material3-window-size-class` dependency
- Grid columns adapt: Compact=2, Medium=3, Expanded=4
- Works for both stat cards and quick action cards
- WindowSizeClass calculated in MainActivity and threaded through AppNavigation

### Typography
- Greeting bumped from headlineLarge (32sp) to displaySmall (36sp)

### Bottom Navigation
- Selected item now shows green-tinted circle indicator (primaryContainer color)

### Files Changed
- `app/build.gradle.kts` — 3 new dependencies (haze, haze-materials, window-size-class)
- `MainActivity.kt` — calculateWindowSizeClass, pass to AppNavigation
- `ui/navigation/AppNavigation.kt` — accept windowWidthSizeClass param, pass to DashboardScreen
- `ui/navigation/BottomNavBar.kt` — NavigationBarItemDefaults.colors with indicatorColor
- `ui/components/AnimatedComponents.kt` — greeting displaySmall
- `ui/screens/dashboard/DashboardScreen.kt` — full rewrite: Haze, AdaptiveGrid, search icon, inline add button, aspect ratios

## v1.10.0 - Dashboard Visual Polish (Session 9)

### Per-Card Colored Icons
- Stat cards: Total Items (blue), Expiring Soon (orange), Low Stock (green), Total Value (green)
- Quick actions: Reports (blue), Scan (gold), Recognize (green), Shopping (purple)
- New accent color constants: CardBlue, CardOrange, CardGreen, CardPurple, CardGold in Color.kt

### Frosted-Glass Card Style
- Stat cards: `primaryContainer` → `surface` background with 2dp elevation shadow
- Quick action cards: same surface + elevation treatment for consistency
- Works across all 3 themes (white on light, dark surface on AMOLED)

### Typography Upgrades
- Dashboard greeting: `headlineSmall` (24sp) → `headlineLarge` (32sp)
- Section headers: `titleMedium` (16sp) → `titleLarge` (22sp) for Overview, Quick Actions, Expiring Soon, Low Stock, Items by Category, Items by Location

### Files Changed
- `ui/theme/Color.kt` — 5 new card accent color constants
- `ui/components/AnimatedComponents.kt` — greeting text size bump
- `ui/screens/dashboard/DashboardScreen.kt` — StatCard/QuickActionCard iconTint param, surface+elevation, section header sizes

## v1.9.0 - Multi-Theme System (Session 8)

### Theme Selector
- 3 themes: Classic Green (original), Warm Cream (new), AMOLED Dark (existing)
- Warm Cream palette: warm beige background, cream-white cards, muted blue-gray primary accent
- Theme selector in Settings: 3 color circles with check icon for selected theme
- Instant apply — theme changes the moment user taps a circle, persisted immediately
- Dynamic color (Material You) removed — user explicitly picks their theme

### Migration
- Existing users with dark mode enabled automatically migrated to AMOLED Dark theme
- Old dark_mode setting key preserved for read-only migration; new `app_theme` key used going forward

### Files Changed
- `ui/theme/Color.kt` — AppTheme enum + Warm Cream color palette
- `ui/theme/Theme.kt` — 3 color schemes, simplified theme function (no dynamic color)
- `MainActivity.kt` — reads `app_theme` key instead of `dark_mode`
- `data/repository/SettingsRepository.kt` — added `KEY_APP_THEME` constant
- `ui/screens/settings/SettingsViewModel.kt` — `appTheme` state replaces `darkMode`, migration logic
- `ui/screens/settings/SettingsScreen.kt` — ThemeCircle composable replaces Switch toggle

## v1.8.0 - Purchase History Backfill + Smart Defaults + Shopping Cost (Session 7)

### Purchase History Backfill
- One-time migration on app startup backfills `purchase_history` records for all existing items that have a `purchase_price` but no history entry
- Uses settings flag (`purchase_history_backfilled`) to ensure it only runs once
- New DAO query: `ItemDao.getItemsMissingPurchaseHistory()`

### Smart Defaults from Purchase History
- `applySmartDefaults()` now queries the most recent purchase of the same item name
- Autofills quantity, unit, and price from purchase history (only for fields user hasn't manually touched)
- Purchase history defaults override hardcoded SmartDefaults (reflects actual user behavior)
- New tracking flags: `userSetQuantity`, `userSetPrice`
- New DAO query: `PurchaseHistoryDao.getLatestPurchaseDefaultsByName(name)`

### Shopping List Cost Calculator
- Per-item estimated cost (`~£X.XX`) shown next to quantity for unpurchased items with known price history
- Estimated total card (primaryContainer) shown above "To Buy" header when total > 0
- Currency symbol loaded from SettingsRepository
- New DAO query: `PurchaseHistoryDao.getLatestPricesForItems(itemIds)` — batch lookup of latest unit prices
- ShoppingListViewModel now injects PurchaseHistoryDao and SettingsRepository

### Shopping List Smart Defaults & Item Resolution
- Add Shopping Item form now applies smart defaults from purchase history (unit, quantity) when typing or selecting a suggestion
- Typing a name that matches an existing inventory item auto-resolves `itemId` — ensures shopping list items link to inventory for cost estimates
- Final `findByName` lookup on save as fallback for items not resolved during typing
- New DAO query: `ItemDao.findByName(name)` — case-insensitive exact match

## v1.7.0 - Spending Report Upgrade (Session 6)

### Spending Report — Full Redesign
- **Summary stat cards**: 3-card row showing Total Spent (with colored % change chip vs previous period), Average per Day, and Purchase Count
- **Spending Trend chart**: New `SpendingLineChart` canvas component — area chart with gradient fill (primary → transparent), data point dots, peak value label, and abbreviated date axis labels
- **Top Purchases list**: Top 5 most expensive purchases in period, showing item name, date, store, and amount; clickable rows navigate to item detail
- **By Category**: Existing horizontal bar chart preserved

### Purchase History Integration (Bug Fix)
- **Add Item form**: Creating a new item now also creates a `purchase_history` record (with price, quantity, date)
- **Shopping list**: Marking a shopping list item as purchased now creates a `purchase_history` record
- Previously, both paths only wrote to the `items` / `shopping_list` tables — spending reports saw nothing

### Data Layer
- New DAO query: `getTopPurchases(since, limit)` — joins purchase_history with items and stores, ordered by total_price DESC
- New DAO query: `getPurchaseCount(since)` — count of purchases in period
- New DAO query: `ShoppingListDao.getById(id)` — fetch single shopping list item
- Repository methods added for both new queries
- `ShoppingListRepository.togglePurchased()` now creates purchase_history + adjusts inventory
- Now uses existing `getDailySpending()` query (was previously unused)

### New Component
- `SpendingLineChart` in Charts.kt — reusable canvas-based area chart with `DailyChartEntry` data class

## v1.6.0 - Visual Polish & Engagement (Session 5)

### AMOLED Dark Mode
- Dark theme now uses pure black (#000000) background and surface for OLED screens
- Dynamic color (Android 12+) also applies AMOLED black in dark mode
- surfaceVariant darkened for subtle contrast against black

### Dashboard Improvements
- Added time-based greeting ("Good morning/afternoon/evening") with context subtitle
- Fixed inconsistent spacing — removed redundant 4dp spacers (parent spacedBy handles it)
- Normalized section titles from titleLarge to titleMedium for consistency
- Replaced duplicate "Add Item" quick action with "Reports" shortcut (FAB still available)
- Added staggered entrance animations on greeting, quick action rows

### Item Form
- Added scroll progress indicator pinned above form content
- Shows current section name (Basic Info, Category, Quantity, Expiry, Purchase) and percentage

### Reports Screen
- Each report card now has a distinct accent color (orange, red, blue, purple, green)
- Cards use tinted backgrounds and colored icons instead of generic primary
- Added staggered entrance animations

### Bottom Navigation
- Selected icon now bounces with a subtle spring scale animation (1.0x to 1.1x)

### New Components
- `DashboardGreeting` — time-based greeting with item context
- `FormProgressIndicator` — animated linear progress with section label
- `StaggeredAnimatedItem` — added `slideOffsetDivisor` param for gentler slides

### Item Cards Visual Refresh
- Grid + list cards now show a first-letter avatar circle (10 distinct colors, consistent per item name)
- Status warnings shown as colored pill chips instead of plain text (Expired, Xd left, Low stock)
- Grid cards get a subtle colored border when items have warnings (expiry/stock)
- Grid cards use elevated surface style (2dp elevation)
- List rows show avatar as leading content, status chips inline in supporting text

### New Theme Colors
- 5 report accent colors: ReportExpiring, ReportLowStock, ReportSpending, ReportUsage, ReportInventory

## v1.5.0 - Robustness, Undo & Responsive UI (Session 4)

### Error Handling (#14)
- All ViewModels now have try-catch error handling with Snackbar error display
- Dashboard, ShoppingList, and all 5 report ViewModels wrapped in error states
- All report screens now show errors via SnackbarHost

### Undo for Deletions (#22)
- Category deletion now shows "Undo" snackbar — tap to restore
- Item batch delete now shows "Undo" snackbar — restores all deleted items
- Added `restore()` to ItemDao/Repository and `restoreCategory()` to CategoryDao/Repository

### UI Consistency
- Dashboard empty states now use consistent EmptyState component (#16)
- Item form shows "* Required field" helper text to distinguish optional fields (#24)

### Responsive & Adaptive UI
- Dashboard stat cards adapt to tablet/landscape — 4 columns on wide screens, 2x2 on phones (#33)
- DonutChart now responsive with min/max width + aspect ratio instead of fixed 120dp (#31)

### Spending Report Improvements
- Spending report now shows % change vs previous period (e.g., "+12.3% vs prev.") (#39)
- Added `getTotalSpendingBetween()` to PurchaseHistoryDao/Repository for period comparison

### Shopping List
- Sort logic now applied to filtered active/purchased lists (#28 completion)

## v1.4.0 - Critical & High Priority UX Fixes (22 issues resolved)

### Critical Fixes
- Dashboard now shows shimmer loading state while data loads (was blank)
- Global search keyboard search button now works (was empty callback)
- Item form validation: quantity, min/max qty, and price fields now show errors for invalid/negative input
- Item form save: added try-catch error handling with snackbar error display
- Item form save button disabled during save to prevent duplicate submissions (uses isSaving state)
- Delete confirmation dialog added for batch delete in Item List
- "Clear Purchased" confirmation dialog added in Shopping List
- Spending Report now uses currency symbol from Settings (was hardcoded to £)
- Color picker in Category Form now shows visual preset color grid (18 colors) plus custom hex input
- 30+ icons now have proper contentDescription for WCAG accessibility compliance

### High Priority Fixes
- Unsaved changes warning dialog on back press in Item Form and Add Shopping Item screens
- Dashboard category rows now navigate to Item List filtered by that category
- Dashboard now shows "Items by Location" section (data was loaded but never displayed)
- Expiring/low stock lists on Dashboard limited to 5 items with "View All" link
- Empty states in Category List and Location List now have action buttons ("Add Category"/"Add Location")
- Settings validation: warning days (1-365), currency (required), default quantity (positive number)
- API key field now has show/hide toggle for verification
- Export/Import progress now shows descriptive status text, import explains merge behavior

### Medium Priority Fixes
- Item form: vague labels clarified with helper text ("Warning Days", "Use Within", "Min Qty", "Max Qty")
- Item form: date validation — warns past expiry, blocks save if opened date > expiry
- Item form: smart defaults now show "Smart defaults applied" chip indicator
- Category form: icon picker now shows labels under each icon
- Category list: reorder buttons enlarged from 20dp to 32dp for better touch targets
- Shopping list: search bar added to filter items by name
- Shopping list: sort options added (Priority, Name, Quantity) via dropdown menu
- Shopping "Add" form: unit field added alongside quantity
- Inventory report: search bar added to filter the all-items list
- Charts: donut chart legend now shows all entries (was truncated to 6)
- Barcode scanner: permission messages now explain privacy + feature purpose
- Settings screen: "Developed by Shantanu Dey" credit added

### Bug Fixes
- Fixed PullToRefreshBox crash (API not available in Compose BOM 2024.02.00, replaced with Box)
- Fixed animateItem() crash (replaced with animateItemPlacement() for BOM compatibility)
- Fixed Dashboard "Total Items" navigation using template route instead of createRoute()
- Removed duplicate import in SettingsScreen

## v1.3.0 - UX & Bug Fixes
- Dashboard stat cards now clickable (navigate to relevant screens)
- Dashboard "Items by Category" rows now clickable (filter items by category)
- Settings "Expiry Warning Days" now properly used by dashboard queries (was hardcoded to 7)
- Dark mode toggle now actually works (was saved but ignored)
- Date pickers added for Expiry Date, Purchase Date, Opened Date (was plain text)
- Barcode scanner: added "Scan Again" button to reset after scanning

## v1.2.0 - Smart Features & Barcode Scanner
- Smart auto-categorization: type item name to auto-fill category, subcategory, unit, location, expiry
- 200+ common grocery items mapped across all 19 categories
- Category-level defaults (select Frozen Foods → location auto-fills to Freezer)
- Autocomplete suggestions when typing item names (Item Form + Shopping List)
- Better form defaults: quantity=1, minQuantity=1, purchaseDate=today
- Real barcode scanner with CameraX camera preview + ML Kit barcode detection
- Runtime camera permission request with Accompanist
- Dual barcode API: Open Food Facts + UPC Item DB fallback for better coverage
- Currency changed from $ to GBP (£) across entire app
- Price field clarified as "Total Price" (not per unit)

## v1.1.0 - Reports, Settings & Export
- 5 report screens: Expiring Items, Low Stock, Spending, Usage, Inventory
- Settings screen: expiry warning days, currency, default quantity, dark mode
- Export/Import: CSV export, CSV import, JSON export
- All placeholder screens replaced with real implementations

## v1.0.0 - Initial Release
- Full inventory management with CRUD for items, categories, subcategories, locations
- Room database with 12 entities, seed data for 19 categories, 10 locations, 30 units
- Dashboard with stats, expiring/low stock lists, category summary
- Item list with search, category filter, grid/list toggle, favorites
- Barcode scanning with Open Food Facts API lookup + manual entry
- Shopping list with check/uncheck, generate from low stock, priority levels
- Material 3 design with dynamic colors
- Hilt dependency injection, MVVM architecture
- Navigation Compose with bottom nav (5 tabs) + nested routes
