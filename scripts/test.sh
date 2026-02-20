#!/bin/bash
# =============================================================================
# Emulator Test Helper — Home Inventory App
# =============================================================================
# Designed for AUTOMATED TESTING by Claude Code.
# Sources ui-map.sh for coordinates and navigation — no guessing needed.
#
# Usage:
#   bash scripts/test.sh [name]              → install + launch + screenshot
#   bash scripts/test.sh -s name             → screenshot only
#   bash scripts/test.sh -t X Y name         → tap at coords + screenshot
#   bash scripts/test.sh -d name             → scroll down + screenshot
#   bash scripts/test.sh -go dest [name]     → navigate to screen + screenshot
#   bash scripts/test.sh -go dest            → navigate only (no screenshot)
#   bash scripts/test.sh -tap element [name] → tap named element + screenshot
#   bash scripts/test.sh -c                  → crash check
#   bash scripts/test.sh -x                  → cleanup emulator screenshots
#   bash scripts/test.sh -xl                 → cleanup local mapping files
#   bash scripts/test.sh -i                  → install only (no launch)
#   bash scripts/test.sh -l                  → launch only (no install)
#
# Navigation destinations (-go):
#   home, items, scan, shopping, more,
#   cook, kitchen_scan, kitchen_map, recipes, receipt,
#   reports, purchases, categories, locations,
#   settings, export_import
#
# Named elements (-tap):
#   home_refresh, home_search, home_expiring, home_low_stock,
#   home_total_value, home_qa_cook, home_qa_kitchen, home_qa_reports,
#   home_qa_scan, home_qa_receipt, home_qa_shopping, home_fab,
#   items_search, items_sort, items_view, items_fab,
#   shop_search, shop_group, shop_menu, shop_input, shop_full_add,
#   shop_quick_add, shop_restock, shop_paste,
#   more_cook, more_kitchen_scan, more_kitchen_map,
#   more_recipes, more_receipt, more_reports, more_purchases,
#   more_categories, more_locations, more_settings, more_export,
#   theme_green, theme_cream, theme_dark,
#   settings_save, settings_replay,
#   back
# =============================================================================

# Source the UI map for coordinates and nav functions
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/ui-map.sh"

# Resolve named element to X Y coordinates
resolve_element() {
    local el="$1"
    case "$el" in
        # Home screen
        home_refresh)     echo "$HOME_REFRESH_X $HOME_REFRESH_Y" ;;
        home_search)      echo "$HOME_SEARCH_X $HOME_SEARCH_Y" ;;
        home_expiring)    echo "$HOME_EXPIRING_X $HOME_EXPIRING_Y" ;;
        home_low_stock)   echo "$HOME_LOW_STOCK_X $HOME_LOW_STOCK_Y" ;;
        home_total_value) echo "$HOME_TOTAL_VALUE_X $HOME_TOTAL_VALUE_Y" ;;
        home_qa_cook)     echo "$HOME_QA_COOK_X $HOME_QA_COOK_Y" ;;
        home_qa_kitchen)  echo "$HOME_QA_KITCHEN_X $HOME_QA_KITCHEN_Y" ;;
        home_qa_reports)  echo "$HOME_QA_REPORTS_X $HOME_QA_REPORTS_Y" ;;
        home_qa_scan)     echo "$HOME_QA_SCAN_X $HOME_QA_SCAN_Y" ;;
        home_qa_receipt)  echo "$HOME_QA_RECEIPT_X $HOME_QA_RECEIPT_Y" ;;
        home_qa_shopping) echo "$HOME_QA_SHOPPING_X $HOME_QA_SHOPPING_Y" ;;
        home_fab)         echo "$HOME_FAB_X $HOME_FAB_Y" ;;
        # Items screen
        items_search)     echo "$ITEMS_SEARCH_X $ITEMS_SEARCH_Y" ;;
        items_sort)       echo "$ITEMS_SORT_X $ITEMS_SORT_Y" ;;
        items_view)       echo "$ITEMS_VIEW_TOGGLE_X $ITEMS_VIEW_TOGGLE_Y" ;;
        items_fab)        echo "$ITEMS_FAB_X $ITEMS_FAB_Y" ;;
        # Shopping screen
        shop_search)      echo "$SHOP_SEARCH_X $SHOP_SEARCH_Y" ;;
        shop_group)       echo "$SHOP_GROUP_CAT_X $SHOP_GROUP_CAT_Y" ;;
        shop_menu)        echo "$SHOP_MORE_OPTS_X $SHOP_MORE_OPTS_Y" ;;
        shop_input)       echo "$SHOP_INPUT_X $SHOP_INPUT_Y" ;;
        shop_full_add)    echo "$SHOP_FULL_ADD_X $SHOP_FULL_ADD_Y" ;;
        shop_quick_add)   echo "$SHOP_QUICK_ADD_X $SHOP_QUICK_ADD_Y" ;;
        shop_restock)     echo "$SHOP_RESTOCK_X $SHOP_RESTOCK_Y" ;;
        shop_paste)       echo "$SHOP_PASTE_LIST_X $SHOP_PASTE_LIST_Y" ;;
        # More screen
        more_cook)        echo "$MORE_COOK_X $MORE_COOK_Y" ;;
        more_kitchen_scan) echo "$MORE_KITCHEN_SCAN_X $MORE_KITCHEN_SCAN_Y" ;;
        more_kitchen_map) echo "$MORE_KITCHEN_MAP_X $MORE_KITCHEN_MAP_Y" ;;
        more_recipes)     echo "$MORE_MY_RECIPES_X $MORE_MY_RECIPES_Y" ;;
        more_receipt)     echo "$MORE_SCAN_RECEIPT_X $MORE_SCAN_RECEIPT_Y" ;;
        more_reports)     echo "$MORE_REPORTS_X $MORE_REPORTS_Y" ;;
        more_purchases)   echo "$MORE_PURCHASES_X $MORE_PURCHASES_Y" ;;
        more_categories)  echo "$MORE_CATEGORIES_X $MORE_CATEGORIES_Y" ;;
        more_locations)   echo "$MORE_LOCATIONS_X $MORE_LOCATIONS_Y" ;;
        more_settings)    echo "$MORE_SETTINGS_X $MORE_SETTINGS_Y" ;;
        more_export)      echo "$MORE_EXPORT_IMPORT_X $MORE_EXPORT_IMPORT_Y" ;;
        # Settings (must be on settings screen already)
        theme_green)      echo "$SETTINGS_THEME_GREEN_X $SETTINGS_THEME_GREEN_Y" ;;
        theme_cream)      echo "$SETTINGS_THEME_CREAM_X $SETTINGS_THEME_CREAM_Y" ;;
        theme_dark)       echo "$SETTINGS_THEME_DARK_X $SETTINGS_THEME_DARK_Y" ;;
        settings_save)    echo "$SETTINGS_S_SAVE_X $SETTINGS_S_SAVE_Y" ;;
        settings_replay)  echo "$SETTINGS_S_REPLAY_X $SETTINGS_S_REPLAY_Y" ;;
        # Generic
        back)             echo "BACK" ;;
        *)
            echo "ERROR: Unknown element '$el'" >&2
            echo "Valid: home_refresh home_search home_expiring home_low_stock" >&2
            echo "       home_total_value home_qa_cook home_qa_kitchen home_qa_reports" >&2
            echo "       home_qa_scan home_qa_receipt home_qa_shopping home_fab" >&2
            echo "       items_search items_sort items_view items_fab" >&2
            echo "       shop_search shop_group shop_menu shop_input shop_full_add" >&2
            echo "       shop_quick_add shop_restock shop_paste" >&2
            echo "       more_cook more_kitchen_scan more_kitchen_map" >&2
            echo "       more_recipes more_receipt more_reports more_purchases" >&2
            echo "       more_categories more_locations more_settings more_export" >&2
            echo "       theme_green theme_cream theme_dark" >&2
            echo "       settings_save settings_replay back" >&2
            return 1
            ;;
    esac
}

case "${1:-run}" in
    -s)  # screenshot only
        screenshot "${2:-screen}"
        ;;

    -t)  # tap X Y then screenshot
        tap "$2" "$3"
        sleep 1
        screenshot "${4:-tap}"
        ;;

    -d)  # scroll down then screenshot
        scroll_down
        sleep 1
        screenshot "${2:-scroll}"
        ;;

    -go) # navigate to destination, optionally screenshot
        dest="$2"
        name="$3"
        nav_to "$dest"
        if [ -n "$name" ]; then
            screenshot "$name"
        fi
        ;;

    -tap) # tap named element, optionally screenshot
        el="$2"
        name="$3"
        coords=$(resolve_element "$el")
        if [ $? -ne 0 ]; then
            exit 1
        fi
        if [ "$coords" = "BACK" ]; then
            go_back
        else
            tap $coords
        fi
        sleep 1
        if [ -n "$name" ]; then
            screenshot "$name"
        fi
        ;;

    -c)  # crash check
        TMPF="$SD/.crash_check"
        MSYS_NO_PATHCONV=1 "$ADB" -s $DV shell "pidof com.inventory.app" > "$TMPF" 2>&1
        if [ -s "$TMPF" ] && grep -qE '[0-9]' "$TMPF"; then
            echo "APP OK (pid: $(tr -d '\r\n' < "$TMPF"))"
        else
            echo "APP NOT RUNNING"
            echo "--- Recent crashes ---"
            "$ADB" -s $DV logcat -d -s AndroidRuntime:E | tail -30
        fi
        rm -f "$TMPF"
        ;;

    -x)  # cleanup emulator screenshots
        cleanup_emulator
        ;;

    -xl) # cleanup local mapping files
        cleanup_local
        ;;

    -i)  # install only
        install_app
        ;;

    -l)  # launch only
        launch_app
        sleep 3
        ;;

    *)   # full flow: install + launch + screenshot
        NAME="${1:-test}"
        install_app && launch_app && sleep 3 && screenshot "$NAME"
        ;;
esac
