package com.inventory.app.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
import com.inventory.app.data.local.dao.ItemDao
import com.inventory.app.data.local.dao.SmartDefaultCacheDao
import com.inventory.app.data.local.entity.SmartDefaultCacheEntity
import com.inventory.app.domain.model.DefaultHints
import com.inventory.app.domain.model.ItemDefaults
import com.inventory.app.domain.model.PersonalDefaults
import com.inventory.app.domain.model.ResolvedDefaults
import com.inventory.app.domain.model.ResolveResult
import com.inventory.app.domain.model.SmartDefaults
import com.inventory.app.util.ItemNameNormalizer
import android.util.Log
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmartDefaultRepository @Inject constructor(
    private val smartDefaultCacheDao: SmartDefaultCacheDao,
    private val itemDao: ItemDao,
    private val categoryRepository: CategoryRepository,
    private val unitRepository: UnitRepository,
    private val storageLocationRepository: StorageLocationRepository,
    private val functions: FirebaseFunctions,
    private val auth: FirebaseAuth
) {

    /**
     * Unified entry point for the 5-layer Smart Defaults cascade.
     *
     * Returns a [ResolveResult] with:
     * - `local`: best result from layers 1-3 (instant, offline)
     * - `remoteDeferred`: optional async result from layers 4+5 (null if local was sufficient)
     *
     * Callers can use `local` immediately and optionally await `remoteDeferred` later.
     *
     * @param itemName  The item name typed/scanned by the user
     * @param regionCode  ISO country code for unit conversion (e.g., "US", "IN")
     * @param hints  External hints from AI vision, barcode API, or kitchen area selection
     */
    suspend fun resolve(
        itemName: String,
        regionCode: String,
        hints: DefaultHints = DefaultHints(),
        includeRemote: Boolean = true
    ): ResolveResult {
        if (itemName.isBlank()) return ResolveResult(local = ResolvedDefaults())

        // --- LAYER 1: Personal History (instant, returns IDs directly) ---
        val personal = lookupPersonalHistory(itemName)
        if (personal != null) {
            val resolved = buildFromPersonalHistory(personal, hints)
            return ResolveResult(local = resolved)
        }

        // --- Build working strings: hints → Layer 2 → Layer 3 (first non-null wins per field) ---
        var category = hints.categoryName
        var subcategory = hints.subcategoryName
        var unit = hints.unitAbbreviation
        var location = hints.locationName
        var shelfLife = hints.shelfLifeDays
        var source = if (category != null || unit != null || location != null) "hints" else "none"

        // Layer 2: Static Dictionary
        val staticDefaults = SmartDefaults.lookup(itemName, regionCode)
        if (staticDefaults != null) {
            if (category == null) category = staticDefaults.category
            if (subcategory == null) subcategory = staticDefaults.subcategory
            if (unit == null) unit = staticDefaults.unit
            if (location == null) location = staticDefaults.location
            if (shelfLife == null) shelfLife = staticDefaults.shelfLifeDays
            if (source == "none") source = "static"
        }

        // Layer 3: Local AI Cache
        val normalizedName = ItemNameNormalizer.normalize(itemName)
        val cached = lookupLocalCache(normalizedName)
        if (cached != null) {
            if (category == null) category = cached.category
            if (subcategory == null) subcategory = cached.subcategory
            if (unit == null) unit = cached.unit
            if (location == null) location = cached.location
            if (shelfLife == null) shelfLife = cached.shelfLifeDays
            if (source == "none") source = "cache"
        }

        // Resolve strings → IDs
        val resolved = resolveStringsToIds(
            categoryName = category,
            subcategoryName = subcategory,
            unitAbbr = unit,
            locationName = location,
            locationIdOverride = hints.locationId,
            shelfLifeDays = shelfLife,
            quantity = hints.quantity,
            price = hints.price,
            brand = hints.brand,
            source = source
        )

        // --- LAYERS 4+5: Remote (async, non-blocking) ---
        // Only fire if layers 1-3 all missed (no category resolved), no hints provided category,
        // and caller opted in (skip during typing/debounce to avoid wasted AI calls)
        val needsRemote = includeRemote && resolved.categoryId == null && hints.categoryName == null
        val remoteDeferred = if (needsRemote) {
            val deferred = CompletableDeferred<ResolvedDefaults?>()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val remote = fetchRemoteDefault(itemName, regionCode)
                    if (remote != null) {
                        val remoteResolved = resolveStringsToIds(
                            categoryName = remote.category,
                            subcategoryName = remote.subcategory,
                            unitAbbr = remote.unit,
                            locationName = remote.location,
                            locationIdOverride = hints.locationId,
                            shelfLifeDays = remote.shelfLifeDays,
                            quantity = null,
                            price = null,
                            brand = null,
                            source = "remote"
                        )
                        deferred.complete(remoteResolved)
                    } else {
                        deferred.complete(null)
                    }
                } catch (_: Exception) {
                    deferred.complete(null)
                }
            }
            deferred
        } else null

        return ResolveResult(local = resolved, remoteDeferred = remoteDeferred)
    }

    // ========== Internal helpers ==========

    /**
     * Build ResolvedDefaults from personal history (Layer 1).
     * Personal history returns IDs directly — validate they still exist,
     * then merge with any hints (hints fill gaps personal history doesn't cover).
     */
    private suspend fun buildFromPersonalHistory(
        personal: PersonalDefaults,
        hints: DefaultHints
    ): ResolvedDefaults {
        // Validate IDs still exist in DB (user may have deleted categories/locations)
        val catEntity = personal.categoryId?.let { categoryRepository.getById(it) }
        val subEntity = personal.subcategoryId?.let { categoryRepository.getSubcategoryById(it) }
        val unitEntity = personal.unitId?.let { unitRepository.getById(it) }
        val locEntity = personal.locationId?.let { storageLocationRepository.getById(it) }

        // For location, prefer hints.locationId (kitchen area) over personal history
        val finalLocId = hints.locationId ?: locEntity?.id
        val finalLocName = if (hints.locationId != null) {
            hints.locationId.let { storageLocationRepository.getById(it)?.name }
        } else {
            locEntity?.name
        }

        return ResolvedDefaults(
            categoryId = catEntity?.id,
            subcategoryId = subEntity?.id,
            locationId = finalLocId,
            unitId = unitEntity?.id,
            categoryName = catEntity?.name,
            subcategoryName = subEntity?.name,
            locationName = finalLocName,
            unitAbbreviation = unitEntity?.abbreviation,
            shelfLifeDays = personal.shelfLifeDays,
            quantity = personal.quantity,
            price = personal.price,
            brand = personal.brand,
            source = "personal"
        )
    }

    /**
     * Convert string names (from static dict / cache / remote) to DB IDs.
     */
    private suspend fun resolveStringsToIds(
        categoryName: String?,
        subcategoryName: String?,
        unitAbbr: String?,
        locationName: String?,
        locationIdOverride: Long?,
        shelfLifeDays: Int?,
        quantity: Double?,
        price: Double?,
        brand: String?,
        source: String
    ): ResolvedDefaults {
        val catEntity = categoryName?.let { categoryRepository.findCategoryByNameIgnoreCase(it) }

        val subEntity = if (catEntity != null && subcategoryName != null) {
            categoryRepository.findSubcategoryByNameAndCategory(subcategoryName, catEntity.id)
        } else null

        val unitEntity = unitAbbr?.let {
            unitRepository.findByAbbreviation(it) ?: unitRepository.findByName(it)
        }

        // Location: prefer override ID (kitchen area), then string name lookup
        val locEntity = if (locationIdOverride != null) {
            storageLocationRepository.getById(locationIdOverride)
        } else {
            locationName?.let { storageLocationRepository.findByName(it) }
        }

        return ResolvedDefaults(
            categoryId = catEntity?.id,
            subcategoryId = subEntity?.id,
            locationId = locEntity?.id,
            unitId = unitEntity?.id,
            categoryName = catEntity?.name ?: categoryName,
            subcategoryName = subEntity?.name ?: subcategoryName,
            locationName = locEntity?.name ?: locationName,
            unitAbbreviation = unitEntity?.abbreviation ?: unitAbbr,
            shelfLifeDays = shelfLifeDays,
            quantity = quantity,
            price = price,
            brand = brand,
            source = source
        )
    }

    // ========== Existing layer methods (unchanged, called internally by resolve()) ==========

    /**
     * Layer 1: Personal History — user's own past items.
     * Returns IDs directly (no string matching needed).
     */
    suspend fun lookupPersonalHistory(itemName: String): PersonalDefaults? {
        val pastItem = itemDao.findMostRecentByName(itemName) ?: return null
        val shelfLife = if (pastItem.expiryDate != null && pastItem.purchaseDate != null) {
            val days = ChronoUnit.DAYS.between(pastItem.purchaseDate, pastItem.expiryDate).toInt()
            if (days > 0) days else null
        } else null
        return PersonalDefaults(
            categoryId = pastItem.categoryId,
            subcategoryId = pastItem.subcategoryId,
            locationId = pastItem.storageLocationId,
            unitId = pastItem.unitId,
            shelfLifeDays = shelfLife,
            quantity = pastItem.quantity,
            price = pastItem.purchasePrice,
            brand = pastItem.brand
        )
    }

    /**
     * Layer 3: Local AI Cache — shipped seed + previously server-fetched items.
     * Returns stale entries over nothing (stale > no data).
     */
    suspend fun lookupLocalCache(normalizedName: String): SmartDefaultCacheEntity? {
        return smartDefaultCacheDao.lookup(normalizedName)
    }

    /**
     * Layers 4+5: Remote fetch — Firestore shared cache backed by Groq AI.
     * Calls `lookupSmartDefault` Cloud Function, caches result locally.
     * Returns null on any failure (network, auth, rate limit) — caller should degrade gracefully.
     */
    suspend fun fetchRemoteDefault(
        itemName: String,
        regionCode: String?
    ): SmartDefaultCacheEntity? {
        // Require authentication for server calls
        if (auth.currentUser == null) return null

        return try {
            val data = hashMapOf(
                "name" to itemName,
                "regionCode" to (regionCode ?: "")
            )
            val result = functions
                .getHttpsCallable("lookupSmartDefault")
                .call(data)
                .await()

            @Suppress("UNCHECKED_CAST")
            val responseMap = result.data as? Map<String, Any> ?: return null

            // Server is still generating — caller can retry later
            if (responseMap["pending"] == true) return null

            val category = responseMap["category"] as? String ?: return null

            val cache = SmartDefaultCacheEntity(
                normalizedName = ItemNameNormalizer.normalize(itemName),
                category = category,
                subcategory = responseMap["subcategory"] as? String,
                unit = responseMap["unit"] as? String,
                location = responseMap["location"] as? String,
                shelfLifeDays = (responseMap["shelfLifeDays"] as? Number)?.toInt(),
                version = (responseMap["version"] as? Number)?.toInt() ?: 1,
                fetchedAt = System.currentTimeMillis(),
                source = "server"
            )
            // Cache locally so next lookup is instant (Layer 3)
            smartDefaultCacheDao.insert(cache)
            cache
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Phase 4: Submit anonymous correction when user overrides a smart-defaulted field.
     * Fire-and-forget — never blocks the save flow, silently catches all errors.
     */
    suspend fun submitCorrection(
        itemName: String,
        field: String,
        oldValue: String,
        newValue: String,
        regionCode: String
    ) {
        if (auth.currentUser == null) return
        try {
            val data = hashMapOf(
                "name" to itemName,
                "field" to field,
                "oldValue" to oldValue,
                "newValue" to newValue,
                "regionCode" to regionCode
            )
            functions
                .getHttpsCallable("submitCorrection")
                .call(data)
                .await()
        } catch (e: Exception) {
            if (Log.isLoggable("SmartDefaults", Log.DEBUG)) {
                Log.d("SmartDefaults", "Correction submit failed: ${e.message}")
            }
        }
    }
}
