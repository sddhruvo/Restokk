#!/bin/bash
# =============================================================================
# UI MAP — Home Inventory App (emulator-5554, 1080x2400)
# =============================================================================
# This file is designed for AUTOMATED TESTING by Claude Code.
# All coordinates are absolute pixel values from uiautomator dumps.
# Source this file to get access to all coords and nav functions.
#
# Usage:   source scripts/ui-map.sh
# Then:    nav_to settings && screenshot settings_test
#
# MAINTAINER NOTE: If app layout changes (new cards, reordered items),
# re-dump the UI and update the coordinates below. The grid pattern is
# consistent: left column centers at x≈283, right column at x≈797.
#
# Last calibrated: 2026-02-19 (Session 62) — full uiautomator dump pass
# =============================================================================

# --- Tool paths ---
ADB="/c/Users/dhruv/AppData/Local/Android/Sdk/platform-tools/adb.exe"
SD="/e/UNIVERSITY/python/Inventory/android/testing/screenshots"
APK="/e/UNIVERSITY/python/Inventory/android/app/build/outputs/apk/debug/app-debug.apk"
PKG="com.inventory.app/.MainActivity"
DV="emulator-5554"

# --- Screen geometry ---
SCREEN_W=1080
SCREEN_H=2400
CONTENT_TOP=294      # Below top app bar
CONTENT_BOTTOM=2064  # Above bottom nav (scrollable area)
NAV_TOP=2127         # Bottom nav bar top edge
NAV_BOTTOM=2337      # Bottom nav bar bottom edge

# =============================================================================
# BOTTOM NAV — identical on every screen, always visible
# Bounds from UI dump: [left,top][right,bottom] → center
# =============================================================================
NAV_HOME_X=100;      NAV_HOME_Y=2232       # [0,2127][200,2337]
NAV_ITEMS_X=320;     NAV_ITEMS_Y=2232      # [221,2127][420,2337]
NAV_SCAN_X=540;      NAV_SCAN_Y=2232       # [441,2127][640,2337]
NAV_SHOPPING_X=760;  NAV_SHOPPING_Y=2232   # [661,2127][860,2337]
NAV_MORE_X=980;      NAV_MORE_Y=2232       # [881,2127][1080,2337]

# =============================================================================
# TOP BAR BUTTONS — varies per screen (3 slots: left-action, mid, right)
# Position slots are consistent: slot1=[692,148][818,274] slot2=[818,148][944,274] slot3=[944,148][1070,274]
# =============================================================================
TOPBAR_SLOT1_X=755;  TOPBAR_SLOT1_Y=211    # Leftmost action button
TOPBAR_SLOT2_X=881;  TOPBAR_SLOT2_Y=211    # Middle action button
TOPBAR_SLOT3_X=1007; TOPBAR_SLOT3_Y=211    # Rightmost action button
TOPBAR_BACK_X=75;    TOPBAR_BACK_Y=211     # Back arrow [12,148][138,274] (sub-screens)

# Per-screen top bar mapping:
# HOME:     slot2=Refresh  slot3=Search
# ITEMS:    slot1=Search   slot2=Sort       slot3=ToggleView
# SHOPPING: slot1=Search   slot2=GroupByCat  slot3=MoreOptions
# MORE:     (no top bar actions)
# SETTINGS: back button only

# =============================================================================
# HOME SCREEN — elements at default scroll (top, empty inventory)
# NOTE: Layout changes when inventory has items (hero card may appear/disappear)
# =============================================================================
HOME_REFRESH_X=881;     HOME_REFRESH_Y=211    # [818,148][944,274]
HOME_SEARCH_X=1007;     HOME_SEARCH_Y=211     # [944,148][1070,274]
# Stat cards (top row):
HOME_EXPIRING_X=283;    HOME_EXPIRING_Y=521   # Expiring Soon [42,367][524,676]
HOME_LOW_STOCK_X=797;   HOME_LOW_STOCK_Y=521  # Low Stock [556,367][1038,676]
# Stat card (second row, left only):
HOME_TOTAL_VALUE_X=283; HOME_TOTAL_VALUE_Y=862 # Total Value [42,708][524,1017]
# Quick Actions grid (visible without scroll):
HOME_QA_COOK_X=283;     HOME_QA_COOK_Y=1284   # Cook [42,1169][524,1400]
HOME_QA_KITCHEN_X=797;  HOME_QA_KITCHEN_Y=1284 # Kitchen [556,1169][1038,1400]
HOME_QA_REPORTS_X=283;  HOME_QA_REPORTS_Y=1547 # Reports [42,1432][524,1663]
HOME_QA_SCAN_X=797;     HOME_QA_SCAN_Y=1547   # Scan [556,1432][1038,1663]
HOME_QA_RECEIPT_X=283;  HOME_QA_RECEIPT_Y=1810 # Receipt [42,1695][524,1926]
HOME_QA_SHOPPING_X=797; HOME_QA_SHOPPING_Y=1810 # Shopping [556,1695][1038,1926]
# FAB:
HOME_FAB_X=976;         HOME_FAB_Y=2032       # Add item [913,1969][1039,2095]

# =============================================================================
# ITEMS SCREEN
# =============================================================================
ITEMS_SEARCH_X=755;     ITEMS_SEARCH_Y=211    # [692,148][818,274]
ITEMS_SORT_X=881;       ITEMS_SORT_Y=211      # [818,148][944,274]
ITEMS_VIEW_TOGGLE_X=1007; ITEMS_VIEW_TOGGLE_Y=211 # [944,148][1070,274]
ITEMS_FAB_X=964;        ITEMS_FAB_Y=1948      # Add Item FAB [891,1875][1038,2022]
# Category chips (y≈294-420, horizontal scroll):
ITEMS_CHIP_DAIRY_X=187; ITEMS_CHIP_DAIRY_Y=357    # [42,294][333,420]
ITEMS_CHIP_MEAT_X=517;  ITEMS_CHIP_MEAT_Y=357     # [354,294][680,420]
ITEMS_CHIP_SEAFOOD_X=811; ITEMS_CHIP_SEAFOOD_Y=357 # [701,294][921,420]

# =============================================================================
# SHOPPING LIST SCREEN
# =============================================================================
SHOP_SEARCH_X=755;      SHOP_SEARCH_Y=211     # [692,148][818,274]
SHOP_GROUP_CAT_X=881;   SHOP_GROUP_CAT_Y=211  # [818,148][944,274]
SHOP_MORE_OPTS_X=1007;  SHOP_MORE_OPTS_Y=211  # [944,148][1070,274]
# Empty state action buttons:
SHOP_QUICK_ADD_X=257;   SHOP_QUICK_ADD_Y=946  # [125,883][390,1009]
SHOP_RESTOCK_X=541;     SHOP_RESTOCK_Y=946    # [424,883][658,1009]
SHOP_PASTE_LIST_X=824;  SHOP_PASTE_LIST_Y=946 # [692,883][956,1009]
# Quick-add bar (bottom, always visible):
SHOP_INPUT_X=482;       SHOP_INPUT_Y=2037     # Text field [21,1964][943,2111]
SHOP_FULL_ADD_X=1008;   SHOP_FULL_ADD_Y=2039  # Full form button [945,1976][1071,2102]

# =============================================================================
# MORE SCREEN — final settled state (search bar present, cards pushed down)
# NOTE: Search bar is always present but takes ~8s to fully animate in.
# The nav_to back-stack reset flow + 10s wait ensures cards settle here.
# Grid: left col center x≈283, right col center x≈797
# =============================================================================
MORE_SEARCH_X=540;       MORE_SEARCH_Y=441    # [42,347][1038,536]
# AI & Kitchen section:
MORE_COOK_X=283;         MORE_COOK_Y=778      # [42,663][524,894]
MORE_KITCHEN_SCAN_X=797; MORE_KITCHEN_SCAN_Y=778  # [556,663][1038,894]
MORE_KITCHEN_MAP_X=283;  MORE_KITCHEN_MAP_Y=1041  # [42,926][524,1157]
MORE_MY_RECIPES_X=797;   MORE_MY_RECIPES_Y=1041   # [556,926][1038,1157]
MORE_SCAN_RECEIPT_X=283; MORE_SCAN_RECEIPT_Y=1304  # [42,1189][524,1420]
# Analytics section:
MORE_REPORTS_X=283;      MORE_REPORTS_Y=1662  # [42,1547][524,1778]
MORE_PURCHASES_X=797;    MORE_PURCHASES_Y=1662 # [556,1547][1038,1778]
# Organize section (partially visible, at bottom edge):
MORE_CATEGORIES_X=283;   MORE_CATEGORIES_Y=2016 # [42,1905][524,2127]
MORE_LOCATIONS_X=797;    MORE_LOCATIONS_Y=2016   # [556,1905][1038,2127]

# --- More screen AFTER scroll (swipe 540 1800→400) ---
# Settings + Export require scroll. Categories/Locations shift up after scroll.
MORE_S_SETTINGS_X=283;      MORE_S_SETTINGS_Y=1885    # [42,1770][524,2001]
MORE_S_EXPORT_IMPORT_X=797; MORE_S_EXPORT_IMPORT_Y=1885 # [556,1770][1038,2001]
MORE_S_CATEGORIES_X=283;    MORE_S_CATEGORIES_Y=1527  # [42,1412][524,1643]
MORE_S_LOCATIONS_X=797;     MORE_S_LOCATIONS_Y=1527    # [556,1412][1038,1643]

# =============================================================================
# SETTINGS SCREEN — elements at default scroll (top)
# All coordinates from uiautomator dump (exact, not approximate)
# =============================================================================
# Back button:
SETTINGS_BACK_X=75;     SETTINGS_BACK_Y=211      # [12,148][138,274]
# Expiry Tracking section:
SETTINGS_WARNING_DAYS_X=540; SETTINGS_WARNING_DAYS_Y=579  # [84,469][996,690]
# Display section:
SETTINGS_CURRENCY_X=540;    SETTINGS_CURRENCY_Y=991  # [84,907][996,1075]
SETTINGS_DEF_QTY_X=540;     SETTINGS_DEF_QTY_Y=1191  # [84,1107][996,1275]
# Theme circles (visible without scroll):
SETTINGS_THEME_GREEN_X=269;  SETTINGS_THEME_GREEN_Y=1451  # Classic Green [206,1388][332,1514]
SETTINGS_THEME_CREAM_X=540;  SETTINGS_THEME_CREAM_Y=1451  # Warm Cream [477,1388][603,1514]
SETTINGS_THEME_DARK_X=812;   SETTINGS_THEME_DARK_Y=1451   # AMOLED Dark [749,1388][875,1514]
# Shopping List section:
SETTINGS_BUDGET_X=540;      SETTINGS_BUDGET_Y=1891   # Shopping Budget [84,1781][996,2002]

# --- Settings AFTER scroll (2 scrolls down from top) ---
# These are position-dependent — only valid at this exact scroll position.
SETTINGS_S_REPLAY_X=540;    SETTINGS_S_REPLAY_Y=1498    # Replay Onboarding [84,1435][996,1561]
SETTINGS_S_SAVE_X=540;      SETTINGS_S_SAVE_Y=1708      # Save Settings [42,1645][1038,1771]

# =============================================================================
# COMMON GESTURES
# =============================================================================
SCROLL_DOWN="input swipe 540 1800 540 400 300"   # Standard scroll down
SCROLL_UP="input swipe 540 400 540 1800 300"     # Standard scroll up
SCROLL_DOWN_HALF="input swipe 540 1400 540 800 300"  # Gentle scroll
FLING_DOWN="input swipe 540 1800 540 200 150"    # Fast fling to bottom
BACK_KEY="input keyevent KEYCODE_BACK"

# =============================================================================
# HELPER FUNCTIONS
# =============================================================================

tap() {
    # tap X Y — tap at coordinates
    MSYS_NO_PATHCONV=1 "$ADB" -s $DV shell input tap "$1" "$2"
}

swipe() {
    # swipe x1 y1 x2 y2 duration_ms
    MSYS_NO_PATHCONV=1 "$ADB" -s $DV shell input swipe "$1" "$2" "$3" "$4" "$5"
}

key() {
    # key KEYCODE_NAME
    MSYS_NO_PATHCONV=1 "$ADB" -s $DV shell input keyevent "$1"
}

screenshot() {
    # screenshot name — saves to screenshots dir
    "$ADB" -s $DV exec-out screencap -p > "$SD/${1:-screen}.png"
}

scroll_down() {
    MSYS_NO_PATHCONV=1 "$ADB" -s $DV shell $SCROLL_DOWN
}

scroll_up() {
    MSYS_NO_PATHCONV=1 "$ADB" -s $DV shell $SCROLL_UP
}

go_back() {
    MSYS_NO_PATHCONV=1 "$ADB" -s $DV shell $BACK_KEY
}

launch_app() {
    "$ADB" -s $DV shell am start -n "$PKG"
}

install_app() {
    "$ADB" -s $DV install -r "$APK"
}

restart_app() {
    # Kill and restart for clean navigation state (clears all back stacks)
    MSYS_NO_PATHCONV=1 "$ADB" -s $DV shell am force-stop com.inventory.app
    sleep 1
    "$ADB" -s $DV shell am start -n "$PKG"
    sleep 3
}

type_text() {
    # type_text "string" — types text into focused field
    MSYS_NO_PATHCONV=1 "$ADB" -s $DV shell input text "$1"
}

wait_for_animations() {
    # More screen: staggered card animations + search bar slide-in (~10s to settle)
    sleep "${1:-10}"
}

# =============================================================================
# NAVIGATION FUNCTIONS
# Each function navigates FROM ANY SCREEN to the target.
# Strategy: always tap the bottom nav first (resets to tab root),
# then do any additional steps (tap sub-item).
# NOTE: More screen NO LONGER needs scroll — all items visible.
# =============================================================================

nav_to() {
    # nav_to <destination>
    # Destinations: home, items, scan, shopping, more,
    #               cook, kitchen_scan, kitchen_map, recipes, receipt,
    #               reports, purchases, categories, locations,
    #               settings, export_import
    local dest="$1"
    case "$dest" in
        # --- Direct tab targets ---
        home)
            tap $NAV_HOME_X $NAV_HOME_Y
            sleep 2
            ;;
        items)
            tap $NAV_ITEMS_X $NAV_ITEMS_Y
            sleep 2
            ;;
        scan)
            tap $NAV_SCAN_X $NAV_SCAN_Y
            sleep 2
            ;;
        shopping)
            tap $NAV_SHOPPING_X $NAV_SHOPPING_Y
            sleep 2
            ;;
        more)
            # More tab retains sub-screen back stack across tab switches.
            # restart_app clears all state for reliable navigation.
            restart_app
            tap $NAV_MORE_X $NAV_MORE_Y
            wait_for_animations
            ;;
        # NOTE: More sub-screen navigation (cook, settings, etc.) is unreliable
        # due to staggered entry animations shifting card positions over ~10s.
        # For More sub-screens, prefer manual screenshot-based testing.

        # --- More screen items (ALL visible without scroll) ---
        # Pattern: go to More root first, then tap the card.
        cook)
            nav_to more
            tap $MORE_COOK_X $MORE_COOK_Y
            sleep 2
            ;;
        kitchen_scan)
            nav_to more
            tap $MORE_KITCHEN_SCAN_X $MORE_KITCHEN_SCAN_Y
            sleep 2
            ;;
        kitchen_map)
            nav_to more
            tap $MORE_KITCHEN_MAP_X $MORE_KITCHEN_MAP_Y
            sleep 2
            ;;
        recipes)
            nav_to more
            tap $MORE_MY_RECIPES_X $MORE_MY_RECIPES_Y
            sleep 2
            ;;
        receipt)
            nav_to more
            tap $MORE_SCAN_RECEIPT_X $MORE_SCAN_RECEIPT_Y
            sleep 2
            ;;
        reports)
            nav_to more
            tap $MORE_REPORTS_X $MORE_REPORTS_Y
            sleep 2
            ;;
        purchases)
            nav_to more
            tap $MORE_PURCHASES_X $MORE_PURCHASES_Y
            sleep 2
            ;;
        # --- More screen items (NEED scroll for these) ---
        categories)
            nav_to more
            scroll_down; sleep 1
            tap $MORE_S_CATEGORIES_X $MORE_S_CATEGORIES_Y
            sleep 2
            ;;
        locations)
            nav_to more
            scroll_down; sleep 1
            tap $MORE_S_LOCATIONS_X $MORE_S_LOCATIONS_Y
            sleep 2
            ;;
        settings)
            nav_to more
            scroll_down; sleep 1
            tap $MORE_S_SETTINGS_X $MORE_S_SETTINGS_Y
            sleep 2
            ;;
        export_import|export|import)
            nav_to more
            scroll_down; sleep 1
            tap $MORE_S_EXPORT_IMPORT_X $MORE_S_EXPORT_IMPORT_Y
            sleep 2
            ;;

        *)
            echo "ERROR: Unknown destination '$dest'"
            echo "Valid: home items scan shopping more cook kitchen_scan"
            echo "       kitchen_map recipes receipt reports purchases"
            echo "       categories locations settings export_import"
            return 1
            ;;
    esac
}

# =============================================================================
# CLEANUP
# =============================================================================
cleanup_emulator() {
    MSYS_NO_PATHCONV=1 "$ADB" -s $DV shell "rm -f /sdcard/Pictures/Screenshots/*.png && rm -f /sdcard/DCIM/Screenshots/*.png"
    echo "Emulator screenshots cleaned"
}

cleanup_local() {
    rm -f "$SD"/map_*.png "$SD"/map_*.txt
    echo "Local mapping files cleaned"
}
