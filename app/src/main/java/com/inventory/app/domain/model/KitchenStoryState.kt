package com.inventory.app.domain.model

import com.inventory.app.ui.navigation.Screen

// ─── Kitchen Story Card — domain model ──────────────────────────────────

data class KitchenStoryMission(
    val index: Int,
    val text: String,
    val isCompleted: Boolean,
    val settingsKey: String,
    val navTarget: String?,
    val dynamicSubtitle: String? = null
)

data class KitchenStoryState(
    val missions: List<KitchenStoryMission> = emptyList(),
    val isDismissed: Boolean = true,
    val onboardingCompleted: Boolean = false,
    val cardSubtitle: String = "",
    val showSmartDefaultsEducation: Boolean = false
) {
    val isVisible: Boolean get() = onboardingCompleted && !isDismissed && missions.isNotEmpty()
    val completedCount: Int get() = missions.count { it.isCompleted }
    val totalCount: Int get() = missions.size
    val allComplete: Boolean get() = totalCount > 0 && completedCount == totalCount
    val canDismiss: Boolean get() = completedCount >= 3
    val currentMissionIndex: Int get() = missions.indexOfFirst { !it.isCompleted }.takeIf { it >= 0 } ?: -1
    val chapterNumber: Int get() = (completedCount + 1).coerceAtMost(totalCount)
}

// ─── Mission definitions & ordering ────────────────────────────────────

object KitchenStoryMissions {
    const val KEY_MISSION_STOCK = "ks_mission_stock"
    const val KEY_MISSION_STOCK_5 = "ks_mission_stock_5"
    const val KEY_MISSION_SHOPPING = "ks_mission_shopping"
    const val KEY_MISSION_COOK = "ks_mission_cook"
    const val KEY_MISSION_EXPIRY = "ks_mission_expiry"
    const val KEY_MISSION_REPORTS = "ks_mission_reports"
    const val KEY_DISMISSED = "kitchen_story_dismissed"
    const val KEY_SMART_DEFAULTS_SHOWN = "ks_smart_defaults_shown"

    enum class TriggerType {
        ITEM_COUNT_GT_0,
        ITEM_COUNT_GTE_5,
        SHOPPING_COUNT_GTE_1,
        RECIPE_VIEWED,
        EXPIRY_SET_GTE_1,
        REPORTS_VIEWED
    }

    data class MissionDefinition(
        val text: String,
        val settingsKey: String,
        val triggerType: TriggerType,
        val navTarget: String?
    )

    // Base pool — order depends on user preference
    private val stockShelves = MissionDefinition(
        "Add your first item", KEY_MISSION_STOCK,
        TriggerType.ITEM_COUNT_GT_0, Screen.KitchenMap.route
    )
    private val trackExpiry = MissionDefinition(
        "Track your first expiry date", KEY_MISSION_EXPIRY,
        TriggerType.EXPIRY_SET_GTE_1, Screen.ItemForm.createRoute()
    )
    private val addFive = MissionDefinition(
        "Add 5 items", KEY_MISSION_STOCK_5,
        TriggerType.ITEM_COUNT_GTE_5, Screen.KitchenMap.route
    )
    private val startShopping = MissionDefinition(
        "Start a shopping list", KEY_MISSION_SHOPPING,
        TriggerType.SHOPPING_COUNT_GTE_1, Screen.ShoppingList.route
    )
    private val tryCook = MissionDefinition(
        "See what you can cook", KEY_MISSION_COOK,
        TriggerType.RECIPE_VIEWED, Screen.CookHub.route
    )
    private val viewReports = MissionDefinition(
        "View your kitchen report", KEY_MISSION_REPORTS,
        TriggerType.REPORTS_VIEWED, Screen.Reports.route
    )

    fun getOrderedMissions(preference: String): List<MissionDefinition> = when (preference) {
        "WASTE" -> listOf(stockShelves, trackExpiry, addFive, startShopping, viewReports)
        "COOK" -> listOf(stockShelves, tryCook, addFive, startShopping, viewReports)
        else -> listOf(stockShelves, addFive, startShopping, tryCook, viewReports) // INVENTORY default
    }

    fun evaluateTrigger(
        type: TriggerType,
        itemCount: Int,
        shoppingCount: Int,
        recipeViewed: Boolean,
        reportsViewed: Boolean,
        expiryCount: Int = 0
    ): Boolean = when (type) {
        TriggerType.ITEM_COUNT_GT_0 -> itemCount > 0
        TriggerType.ITEM_COUNT_GTE_5 -> itemCount >= 5
        TriggerType.SHOPPING_COUNT_GTE_1 -> shoppingCount >= 1
        TriggerType.RECIPE_VIEWED -> recipeViewed
        TriggerType.EXPIRY_SET_GTE_1 -> expiryCount >= 1
        TriggerType.REPORTS_VIEWED -> reportsViewed
    }

    fun dynamicSubtitle(
        type: TriggerType,
        itemCount: Int,
        shoppingCount: Int,
        expiryCount: Int = 0
    ): String? = when (type) {
        TriggerType.ITEM_COUNT_GT_0 -> if (itemCount == 0) "scan or add your first item" else null
        TriggerType.ITEM_COUNT_GTE_5 -> when {
            itemCount == 0 -> "start with one item"
            itemCount < 5 -> "you have $itemCount — add ${5 - itemCount} more"
            else -> null
        }
        TriggerType.SHOPPING_COUNT_GTE_1 -> if (shoppingCount == 0) "tap + to add your first item" else null
        TriggerType.RECIPE_VIEWED -> "pick a mood and generate recipes"
        TriggerType.EXPIRY_SET_GTE_1 -> if (expiryCount == 0) "add an expiry date to any item" else null
        TriggerType.REPORTS_VIEWED -> "see insights about your kitchen"
    }

    fun cardSubtitle(completedCount: Int): String = when (completedCount) {
        0 -> "Your kitchen story begins here"
        1 -> "Off to a great start"
        2 -> "Getting the hang of it"
        3 -> "More than halfway there"
        4 -> "One more to go!"
        else -> "Your Kitchen is Alive!"
    }
}
