package com.inventory.app.data.repository

import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.logEvent
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Centralized analytics logging. All event names and params go through here
 * so we have a single source of truth for what we track.
 */
@Singleton
class AnalyticsRepository @Inject constructor(
    private val analytics: FirebaseAnalytics
) {
    // ---- Screen views ----
    fun logScreenView(screenName: String) {
        analytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW) {
            param(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
        }
    }

    // ---- Inventory ----
    fun logItemAdded(category: String? = null) {
        analytics.logEvent("item_added") {
            category?.let { param("category", it) }
        }
    }

    fun logItemDeleted() {
        analytics.logEvent("item_deleted") {}
    }

    // ---- Scanning ----
    fun logBarcodeScan(found: Boolean) {
        analytics.logEvent("barcode_scanned") {
            param("product_found", if (found) "yes" else "no")
        }
    }

    fun logReceiptScan(itemCount: Int) {
        analytics.logEvent("receipt_scanned") {
            param("item_count", itemCount.toLong())
        }
    }

    fun logKitchenScan(itemCount: Int) {
        analytics.logEvent("kitchen_scanned") {
            param("item_count", itemCount.toLong())
        }
    }

    // ---- Shopping ----
    fun logShoppingItemAdded() {
        analytics.logEvent("shopping_item_added") {}
    }

    fun logShoppingItemPurchased() {
        analytics.logEvent("shopping_item_purchased") {}
    }

    // ---- AI ----
    fun logAiRequest(feature: String) {
        analytics.logEvent("ai_request") {
            param("feature", feature)
        }
    }

    // ---- Recipes ----
    fun logRecipeSuggested(cuisine: String? = null) {
        analytics.logEvent("recipe_suggested") {
            cuisine?.let { param("cuisine", it) }
        }
    }

    fun logRecipeSaved() {
        analytics.logEvent("recipe_saved") {}
    }

    // ---- Onboarding ----
    fun logOnboardingStep(step: Int) {
        analytics.logEvent("onboarding_step") {
            param("step", step.toLong())
        }
    }

    fun logOnboardingCompleted() {
        analytics.logEvent("onboarding_completed") {}
    }

    // ---- Auth ----
    fun logSignIn(method: String) {
        analytics.logEvent(FirebaseAnalytics.Event.LOGIN) {
            param(FirebaseAnalytics.Param.METHOD, method)
        }
    }

    fun logSignOut() {
        analytics.logEvent("sign_out") {}
    }

    // ---- Set user properties ----
    fun setUserProperty(name: String, value: String?) {
        analytics.setUserProperty(name, value)
    }

    fun setUserId(uid: String?) {
        analytics.setUserId(uid)
    }

    // ---- Cloud Backup ----
    fun logCloudBackup(itemCount: Int, recipeCount: Int, shoppingCount: Int, totalItems: Int) {
        analytics.logEvent("cloud_backup") {
            param("item_count", itemCount.toLong())
            param("recipe_count", recipeCount.toLong())
            param("shopping_count", shoppingCount.toLong())
            param("total_items", totalItems.toLong())
        }
    }

    fun logCloudRestore(itemsRestored: Int, recipesRestored: Int, mergeMode: Boolean) {
        analytics.logEvent("cloud_restore") {
            param("items_restored", itemsRestored.toLong())
            param("recipes_restored", recipesRestored.toLong())
            param("merge_mode", if (mergeMode) "yes" else "no")
        }
    }

    // ---- Notification Center ----
    fun logNotificationDrawerOpened() {
        analytics.logEvent("notification_drawer_opened") {}
    }

    fun logNotificationTapped(type: String) {
        analytics.logEvent("notification_tapped") {
            param("type", type)
        }
    }

    fun logNotificationDismissed(type: String) {
        analytics.logEvent("notification_dismissed") {
            param("type", type)
        }
    }

    fun logNotificationCtaClicked(type: String) {
        analytics.logEvent("notification_cta_clicked") {
            param("type", type)
        }
    }
}

