package com.inventory.app.ui

/**
 * Centralized test tag constants for UI Automator / Compose Testing.
 * Naming convention: {screen}.{section}.{element}
 */
object TestTags {

    object BottomNav {
        const val TAB_HOME = "bottomNav.tab.home"
        const val TAB_COOK = "bottomNav.tab.cook"
        const val TAB_SHOPPING = "bottomNav.tab.shopping"
        const val TAB_MORE = "bottomNav.tab.more"
        const val FAB_QUICK_ADD = "bottomNav.fab.quickAdd"
    }

    object QuickAdd {
        const val MENU = "quickAdd.menu"
        const val MENU_ADD_ITEM = "quickAdd.menu.addItem"
        const val MENU_SCAN_BARCODE = "quickAdd.menu.scanBarcode"
        const val MENU_KITCHEN_SCAN = "quickAdd.menu.kitchenScan"
        const val MENU_SCAN_RECEIPT = "quickAdd.menu.scanReceipt"
        const val SCRIM = "quickAdd.scrim"
    }

    object Dashboard {
        const val ROOT = "dashboard.root"
        const val SCORE = "dashboard.score"
        const val GREETING = "dashboard.greeting"
        const val STAT_TOTAL_ITEMS = "dashboard.stats.totalItems"
        const val STAT_EXPIRING = "dashboard.stats.expiring"
        const val STAT_LOW_STOCK = "dashboard.stats.lowStock"
        const val STAT_TOTAL_VALUE = "dashboard.stats.totalValue"
        const val QUICK_ACTION_COOK = "dashboard.quickAction.cook"
        const val QUICK_ACTION_KITCHEN = "dashboard.quickAction.kitchen"
        const val QUICK_ACTION_REPORTS = "dashboard.quickAction.reports"
        const val QUICK_ACTION_BARCODE = "dashboard.quickAction.barcode"
        const val QUICK_ACTION_RECEIPT = "dashboard.quickAction.receipt"
        const val QUICK_ACTION_SHOPPING = "dashboard.quickAction.shopping"
        const val NAV_SEARCH = "dashboard.nav.search"
        const val NAV_REFRESH = "dashboard.nav.refresh"
        const val EXPIRING_SECTION = "dashboard.expiring"
        const val LOW_STOCK_SECTION = "dashboard.lowStock"
        const val EMPTY_STATE = "dashboard.emptyState"
    }

    object ItemList {
        const val ROOT = "itemList.root"
        const val SEARCH_FIELD = "itemList.search.field"
        const val SORT_BUTTON = "itemList.sort"
        const val GRID_TOGGLE = "itemList.gridToggle"
        const val EMPTY_STATE = "itemList.emptyState"
        const val ITEM_PREFIX = "itemList.item."
        const val FILTER_CHIP_PREFIX = "itemList.filter."
        const val SELECT_ALL = "itemList.selectAll"
        const val DELETE_SELECTED = "itemList.deleteSelected"
    }

    object ItemForm {
        const val ROOT = "itemForm.root"
        const val FIELD_NAME = "itemForm.field.name"
        const val FIELD_QUANTITY = "itemForm.field.quantity"
        const val FIELD_CATEGORY = "itemForm.field.category"
        const val FIELD_LOCATION = "itemForm.field.location"
        const val FIELD_UNIT = "itemForm.field.unit"
        const val FIELD_EXPIRY = "itemForm.field.expiry"
        const val FIELD_PRICE = "itemForm.field.price"
        const val FIELD_NOTES = "itemForm.field.notes"
        const val BUTTON_SAVE = "itemForm.button.save"
        const val BUTTON_DELETE = "itemForm.button.delete"
        const val MORE_DETAILS = "itemForm.moreDetails"
    }

    object ItemDetail {
        const val ROOT = "itemDetail.root"
        const val NAME = "itemDetail.name"
        const val QUANTITY = "itemDetail.quantity"
        const val BUTTON_USE = "itemDetail.button.use"
        const val BUTTON_RESTOCK = "itemDetail.button.restock"
        const val BUTTON_EDIT = "itemDetail.button.edit"
        const val BUTTON_DELETE = "itemDetail.button.delete"
        const val CHIP_USAGE = "itemDetail.chip.usage"
        const val CHIP_PURCHASE = "itemDetail.chip.purchase"
        const val CHIP_SHOPPING = "itemDetail.chip.shopping"
    }

    object Shopping {
        const val ROOT = "shopping.root"
        const val QUICK_ADD_FIELD = "shopping.quickAdd.field"
        const val QUICK_ADD_SEND = "shopping.quickAdd.send"
        const val EMPTY_STATE = "shopping.emptyState"
        const val ITEM_PREFIX = "shopping.item."
        const val CELEBRATION = "shopping.celebration"
        const val PURCHASED_SECTION = "shopping.purchased"
        const val PROGRESS_RING = "shopping.progressRing"
    }

    object CookHub {
        const val ROOT = "cookHub.root"
        const val CARD_AI_COOK = "cookHub.card.aiCook"
        const val CARD_RECIPE_BUILDER = "cookHub.card.recipeBuilder"
        const val CARD_SAVED_RECIPES = "cookHub.card.savedRecipes"
        const val CARD_DESCRIBE_RECIPE = "cookHub.card.describeRecipe"
    }

    object Cook {
        const val ROOT = "cook.root"
        const val MOOD_PREFIX = "cook.mood."
        const val CUISINE_BUTTON = "cook.cuisine"
        const val HERO_INGREDIENT = "cook.heroIngredient"
        const val BUTTON_COOK = "cook.button.cook"
        const val RECIPE_CARD_PREFIX = "cook.recipe."
    }

    object SavedRecipes {
        const val ROOT = "savedRecipes.root"
        const val SEARCH_FIELD = "savedRecipes.search"
        const val EMPTY_STATE = "savedRecipes.emptyState"
        const val RECIPE_PREFIX = "savedRecipes.recipe."
    }

    object Reports {
        const val ROOT = "reports.root"
        const val CARD_EXPIRING = "reports.card.expiring"
        const val CARD_LOW_STOCK = "reports.card.lowStock"
        const val CARD_SPENDING = "reports.card.spending"
        const val CARD_USAGE = "reports.card.usage"
        const val CARD_INVENTORY = "reports.card.inventory"
    }

    object GlobalSearch {
        const val ROOT = "globalSearch.root"
        const val SEARCH_FIELD = "globalSearch.field"
        const val RESULTS = "globalSearch.results"
        const val EMPTY_STATE = "globalSearch.emptyState"
    }

    object Settings {
        const val ROOT = "settings.root"
        const val ITEM_THEME = "settings.item.theme"
        const val ITEM_VISUAL_STYLE = "settings.item.visualStyle"
        const val ITEM_NOTIFICATIONS = "settings.item.notifications"
        const val ITEM_CURRENCY = "settings.item.currency"
        const val ITEM_EXPORT = "settings.item.export"
    }

    object More {
        const val ROOT = "more.root"
        const val CARD_PREFIX = "more.card."
    }

    object Onboarding {
        const val ROOT = "onboarding.root"
        const val BUTTON_NEXT = "onboarding.button.next"
        const val BUTTON_SKIP = "onboarding.button.skip"
        const val REGION_PICKER = "onboarding.regionPicker"
        const val PREFERENCE_PREFIX = "onboarding.preference."
    }

    object Dialog {
        const val CONFIRM = "dialog.confirm"
        const val CANCEL = "dialog.cancel"
        const val DISCARD = "dialog.discard"
    }

    object BarcodeScan {
        const val ROOT = "barcodeScan.root"
        const val CAMERA_PREVIEW = "barcodeScan.camera"
        const val MANUAL_ENTRY = "barcodeScan.manualEntry"
    }

    object ReceiptScan {
        const val ROOT = "receiptScan.root"
    }

    object FridgeScan {
        const val ROOT = "fridgeScan.root"
    }

    object KitchenMap {
        const val ROOT = "kitchenMap.root"
    }

    object PantryHealth {
        const val ROOT = "pantryHealth.root"
    }
}
