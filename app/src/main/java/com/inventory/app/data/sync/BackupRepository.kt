package com.inventory.app.data.sync

import android.content.Context
import android.os.Build
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.inventory.app.data.local.dao.CategoryDao
import com.inventory.app.data.local.dao.ItemDao
import com.inventory.app.data.local.dao.SavedRecipeDao
import com.inventory.app.data.local.dao.SettingsDao
import com.inventory.app.data.local.dao.ShoppingListDao
import com.inventory.app.data.local.dao.StorageLocationDao
import com.inventory.app.data.local.dao.SubcategoryDao
import com.inventory.app.data.local.dao.UnitDao
import com.inventory.app.data.local.entity.SettingsEntity
import com.inventory.app.data.repository.AnalyticsRepository
import com.inventory.app.data.repository.SettingsRepository
import com.inventory.app.data.sync.mapper.BackupChecksumUtil
import com.inventory.app.data.sync.mapper.BackupPrioritizer
import com.inventory.app.data.sync.mapper.toCategoryEntity
import com.inventory.app.data.sync.mapper.toFirestoreMap
import com.inventory.app.data.sync.mapper.toItemEntity
import com.inventory.app.data.sync.mapper.toSavedRecipeEntity
import com.inventory.app.data.sync.mapper.toSettingsEntity
import com.inventory.app.data.sync.mapper.toShoppingListItemEntity
import com.inventory.app.data.sync.mapper.toStorageLocationEntity
import com.inventory.app.data.sync.mapper.toSubcategoryEntity
import com.inventory.app.data.sync.mapper.toUnitEntity
import com.inventory.app.data.sync.model.BackupEligibility
import com.inventory.app.data.sync.model.BackupMetadata
import com.inventory.app.data.sync.model.RestoreResult
import com.inventory.app.data.sync.model.SyncConstants
import com.inventory.app.data.sync.model.SyncState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val itemDao: ItemDao,
    private val shoppingListDao: ShoppingListDao,
    private val savedRecipeDao: SavedRecipeDao,
    private val categoryDao: CategoryDao,
    private val subcategoryDao: SubcategoryDao,
    private val storageLocationDao: StorageLocationDao,
    private val unitDao: UnitDao,
    private val settingsDao: SettingsDao,
    private val settingsRepository: SettingsRepository,
    private val analyticsRepository: AnalyticsRepository,
    @ApplicationContext private val context: Context
) {
    private fun userDocRef() = firestore.collection(SyncConstants.COLLECTION_USERS)
        .document(auth.currentUser?.uid ?: throw IllegalStateException("No user signed in"))

    // ─── Eligibility ────────────────────────────────────────────

    suspend fun canBackup(): BackupEligibility {
        val user = auth.currentUser ?: return BackupEligibility.NotSignedIn
        if (user.isAnonymous) return BackupEligibility.AnonymousOnly

        val lastBackup = settingsRepository.getString(
            SettingsRepository.KEY_LAST_BACKUP_TIMESTAMP, "0"
        ).toLongOrNull() ?: 0L
        val elapsed = System.currentTimeMillis() - lastBackup
        if (elapsed < SyncConstants.BACKUP_COOLDOWN_MS) {
            val remainingMs = SyncConstants.BACKUP_COOLDOWN_MS - elapsed
            return BackupEligibility.RateLimited((remainingMs / 60_000).toInt() + 1)
        }

        return BackupEligibility.Eligible
    }

    // ─── Backup ─────────────────────────────────────────────────

    suspend fun performBackup(): Result<BackupMetadata> { return try {
        val user = auth.currentUser ?: throw IllegalStateException("No user signed in")
        if (user.isAnonymous) throw IllegalStateException("Google Sign-In required for backup")

        val userRef = userDocRef()

        // 1. Snapshot all entities from Room
        val allItems = itemDao.getAllActiveSnapshot()
        val allShopping = shoppingListDao.getAllActiveSnapshot()
        val allRecipes = savedRecipeDao.getAllActiveSnapshot()
        val categories = categoryDao.getAllActiveSnapshot()
        val subcategories = subcategoryDao.getAllActiveSnapshot()
        val locations = storageLocationDao.getAllActiveSnapshot()
        val units = unitDao.getAllActiveSnapshot()
        val settings = settingsDao.getAllSnapshot()

        // 2. Apply free-tier limits
        val items = BackupPrioritizer.prioritizeItems(allItems, SyncConstants.FREE_TIER_ITEM_LIMIT)
        val recipes = BackupPrioritizer.prioritizeRecipes(allRecipes, SyncConstants.FREE_TIER_RECIPE_LIMIT)

        // 3. Map to Firestore format
        val itemMaps = items.map { it.toFirestoreMap() }
        val shoppingMaps = allShopping.map { it.toFirestoreMap() }
        val recipeMaps = recipes.map { it.toFirestoreMap() }
        val categoryMaps = categories.map { it.toFirestoreMap() }
        val subcategoryMaps = subcategories.map { it.toFirestoreMap() }
        val locationMaps = locations.map { it.toFirestoreMap() }
        val unitMaps = units.map { it.toFirestoreMap() }
        val settingMaps = settings
            .filter { it.key in SyncConstants.SETTINGS_KEYS_TO_SYNC }
            .map { it.toFirestoreMap() }

        // 4. Compute checksum
        val checksum = BackupChecksumUtil.computeChecksum(itemMaps, shoppingMaps, recipeMaps)

        // 5. Delete existing backup data (clean slate)
        deleteCollectionDocs(userRef.collection(SyncConstants.COLLECTION_INVENTORY))
        deleteCollectionDocs(userRef.collection(SyncConstants.COLLECTION_SHOPPING))
        deleteCollectionDocs(userRef.collection(SyncConstants.COLLECTION_RECIPES))
        deleteCollectionDocs(userRef.collection(SyncConstants.COLLECTION_CATEGORIES))
        deleteCollectionDocs(userRef.collection(SyncConstants.COLLECTION_SUBCATEGORIES))
        deleteCollectionDocs(userRef.collection(SyncConstants.COLLECTION_STORAGE_LOCATIONS))
        deleteCollectionDocs(userRef.collection(SyncConstants.COLLECTION_UNITS))
        deleteCollectionDocs(userRef.collection(SyncConstants.COLLECTION_SETTINGS))

        // 6. Write new data in batches (Firestore limit: 500 ops per batch)
        writeBatched(userRef.collection(SyncConstants.COLLECTION_CATEGORIES), categoryMaps)
        writeBatched(userRef.collection(SyncConstants.COLLECTION_SUBCATEGORIES), subcategoryMaps)
        writeBatched(userRef.collection(SyncConstants.COLLECTION_STORAGE_LOCATIONS), locationMaps)
        writeBatched(userRef.collection(SyncConstants.COLLECTION_UNITS), unitMaps)
        writeBatched(userRef.collection(SyncConstants.COLLECTION_INVENTORY), itemMaps)
        writeBatched(userRef.collection(SyncConstants.COLLECTION_SHOPPING), shoppingMaps)
        writeBatched(userRef.collection(SyncConstants.COLLECTION_RECIPES), recipeMaps)
        writeBatched(userRef.collection(SyncConstants.COLLECTION_SETTINGS), settingMaps)

        // 7. Write sync_state metadata
        val syncState = SyncState(
            lastBackupTimestamp = System.currentTimeMillis(),
            deviceId = Build.MODEL,
            schemaVersion = SyncState.CURRENT_SCHEMA_VERSION,
            itemCount = items.size,
            shoppingCount = allShopping.size,
            recipeCount = recipes.size,
            checksum = checksum,
            appVersion = getAppVersion()
        )
        userRef.collection(SyncConstants.COLLECTION_METADATA)
            .document(SyncConstants.DOC_SYNC_STATE)
            .set(syncState.toFirestoreMap())
            .await()

        // 8. Update local timestamp
        val now = System.currentTimeMillis()
        settingsRepository.set(SettingsRepository.KEY_LAST_BACKUP_TIMESTAMP, now.toString())
        settingsRepository.setInt(SettingsRepository.KEY_LAST_BACKUP_ITEM_COUNT, items.size)

        // 9. Log analytics
        analyticsRepository.logCloudBackup(
            itemCount = items.size,
            recipeCount = recipes.size,
            shoppingCount = allShopping.size,
            totalItems = allItems.size
        )

        val metadata = BackupMetadata(
            lastBackupAt = now,
            itemCount = items.size,
            shoppingCount = allShopping.size,
            recipeCount = recipes.size,
            schemaVersion = SyncState.CURRENT_SCHEMA_VERSION
        )
        Result.success(metadata)
    } catch (e: Exception) {
        Result.failure(e)
    } }

    // ─── Check Existing Backup ──────────────────────────────────

    suspend fun checkExistingBackup(): BackupMetadata? {
        return try {
            val user = auth.currentUser ?: return null
            if (user.isAnonymous) return null

            val syncDoc = userDocRef()
                .collection(SyncConstants.COLLECTION_METADATA)
                .document(SyncConstants.DOC_SYNC_STATE)
                .get()
                .await()

            if (!syncDoc.exists()) return null

            val data = syncDoc.data ?: return null
            val syncState = SyncState.fromFirestoreMap(data)

            BackupMetadata(
                lastBackupAt = syncState.lastBackupTimestamp,
                itemCount = syncState.itemCount,
                shoppingCount = syncState.shoppingCount,
                recipeCount = syncState.recipeCount,
                schemaVersion = syncState.schemaVersion
            )
        } catch (_: Exception) {
            null
        }
    }

    // ─── Restore ────────────────────────────────────────────────

    suspend fun performRestore(mergeWithLocal: Boolean = false): Result<RestoreResult> { return try {
        val user = auth.currentUser ?: throw IllegalStateException("No user signed in")
        val userRef = userDocRef()

        // 1. Check schema version
        val syncDoc = userRef.collection(SyncConstants.COLLECTION_METADATA)
            .document(SyncConstants.DOC_SYNC_STATE)
            .get().await()
        val syncState = if (syncDoc.exists()) {
            SyncState.fromFirestoreMap(syncDoc.data ?: emptyMap())
        } else {
            throw IllegalStateException("No backup found")
        }

        if (syncState.schemaVersion > SyncState.CURRENT_SCHEMA_VERSION) {
            throw IllegalStateException("Please update Restokk to restore this backup (cloud schema v${syncState.schemaVersion} > app schema v${SyncState.CURRENT_SCHEMA_VERSION})")
        }

        val warnings = mutableListOf<String>()

        // 2. Download all collections
        val cloudCategories = userRef.collection(SyncConstants.COLLECTION_CATEGORIES).get().await()
        val cloudSubcategories = userRef.collection(SyncConstants.COLLECTION_SUBCATEGORIES).get().await()
        val cloudLocations = userRef.collection(SyncConstants.COLLECTION_STORAGE_LOCATIONS).get().await()
        val cloudUnits = userRef.collection(SyncConstants.COLLECTION_UNITS).get().await()
        val cloudItems = userRef.collection(SyncConstants.COLLECTION_INVENTORY).get().await()
        val cloudShopping = userRef.collection(SyncConstants.COLLECTION_SHOPPING).get().await()
        val cloudRecipes = userRef.collection(SyncConstants.COLLECTION_RECIPES).get().await()
        val cloudSettings = userRef.collection(SyncConstants.COLLECTION_SETTINGS).get().await()

        // 3. Restore lookup tables first (for FK remapping)
        // Categories: match by name to avoid duplicating seeded defaults
        val categoryIdRemap = mutableMapOf<Long, Long>()
        var categoriesRestored = 0
        for (doc in cloudCategories.documents) {
            val data = doc.data ?: continue
            val originalId = (data[SyncConstants.FIELD_ORIGINAL_ID] as? Long) ?: continue
            val name = (data["name"] as? String) ?: continue

            val existing = categoryDao.findByNameIgnoreCase(name)
            if (existing != null) {
                categoryIdRemap[originalId] = existing.id
            } else {
                val entity = data.toCategoryEntity(idOverride = 0)
                val newId = categoryDao.insert(entity)
                if (newId > 0) {
                    categoryIdRemap[originalId] = newId
                    categoriesRestored++
                }
            }
        }

        // Subcategories: match by name + category
        val subcategoryIdRemap = mutableMapOf<Long, Long>()
        for (doc in cloudSubcategories.documents) {
            val data = doc.data ?: continue
            val originalId = (data[SyncConstants.FIELD_ORIGINAL_ID] as? Long) ?: continue
            val name = (data["name"] as? String) ?: continue
            val origCatId = (data["categoryId"] as? Long) ?: continue
            val remappedCatId = categoryIdRemap[origCatId] ?: origCatId

            val existing = subcategoryDao.findByNameAndCategory(name, remappedCatId)
            if (existing != null) {
                subcategoryIdRemap[originalId] = existing.id
            } else {
                val entity = data.toSubcategoryEntity(idOverride = 0, categoryIdRemap = categoryIdRemap)
                val newId = subcategoryDao.insert(entity)
                if (newId > 0) {
                    subcategoryIdRemap[originalId] = newId
                }
            }
        }

        // Storage locations: match by name
        val locationIdRemap = mutableMapOf<Long, Long>()
        var locationsRestored = 0
        for (doc in cloudLocations.documents) {
            val data = doc.data ?: continue
            val originalId = (data[SyncConstants.FIELD_ORIGINAL_ID] as? Long) ?: continue
            val name = (data["name"] as? String) ?: continue

            val existing = storageLocationDao.findByName(name)
            if (existing != null) {
                locationIdRemap[originalId] = existing.id
            } else {
                val entity = data.toStorageLocationEntity(idOverride = 0)
                val newId = storageLocationDao.insert(entity)
                if (newId > 0) {
                    locationIdRemap[originalId] = newId
                    locationsRestored++
                }
            }
        }

        // Units: match by name
        val unitIdRemap = mutableMapOf<Long, Long>()
        var unitsRestored = 0
        for (doc in cloudUnits.documents) {
            val data = doc.data ?: continue
            val originalId = (data[SyncConstants.FIELD_ORIGINAL_ID] as? Long) ?: continue
            val name = (data["name"] as? String) ?: continue

            val existing = unitDao.findByName(name)
            if (existing != null) {
                unitIdRemap[originalId] = existing.id
            } else {
                val entity = data.toUnitEntity(idOverride = 0)
                val newId = unitDao.insert(entity)
                if (newId > 0) {
                    unitIdRemap[originalId] = newId
                    unitsRestored++
                }
            }
        }

        // 4. Restore items (with FK remapping)
        val itemIdRemap = mutableMapOf<Long, Long>()
        var itemsRestored = 0
        for (doc in cloudItems.documents) {
            val data = doc.data ?: continue
            val originalId = (data[SyncConstants.FIELD_ORIGINAL_ID] as? Long) ?: continue
            val name = (data["name"] as? String) ?: continue

            if (mergeWithLocal) {
                val existing = itemDao.search(name).firstOrNull { it.name.equals(name, ignoreCase = true) }
                if (existing != null) {
                    // Keep the newer version
                    val cloudUpdatedAt = (data["updatedAt"] as? Long) ?: 0L
                    val localUpdatedAt = existing.updatedAt.atZone(java.time.ZoneOffset.UTC).toInstant().toEpochMilli()
                    if (cloudUpdatedAt > localUpdatedAt) {
                        val entity = data.toItemEntity(
                            idOverride = existing.id,
                            categoryIdRemap = categoryIdRemap,
                            subcategoryIdRemap = subcategoryIdRemap,
                            storageLocationIdRemap = locationIdRemap,
                            unitIdRemap = unitIdRemap
                        )
                        itemDao.update(entity)
                        itemIdRemap[originalId] = existing.id
                        itemsRestored++
                    } else {
                        itemIdRemap[originalId] = existing.id
                    }
                    continue
                }
            }

            val entity = data.toItemEntity(
                idOverride = 0,
                categoryIdRemap = categoryIdRemap,
                subcategoryIdRemap = subcategoryIdRemap,
                storageLocationIdRemap = locationIdRemap,
                unitIdRemap = unitIdRemap
            )
            val newId = itemDao.insert(entity)
            itemIdRemap[originalId] = newId
            itemsRestored++
        }

        // 5. Restore shopping list (with FK remapping)
        var shoppingRestored = 0
        for (doc in cloudShopping.documents) {
            val data = doc.data ?: continue

            if (mergeWithLocal) {
                val customName = data["customName"] as? String
                val origItemId = (data["itemId"] as? Long)
                val remappedItemId = origItemId?.let { itemIdRemap[it] ?: it }

                // Skip if already exists locally
                if (remappedItemId != null) {
                    val existing = shoppingListDao.findActiveByItemId(remappedItemId)
                    if (existing != null) continue
                } else if (customName != null) {
                    val existing = shoppingListDao.findActiveByCustomName(customName)
                    if (existing != null) continue
                }
            }

            val entity = data.toShoppingListItemEntity(
                idOverride = 0,
                itemIdRemap = itemIdRemap,
                unitIdRemap = unitIdRemap
            )
            shoppingListDao.insert(entity)
            shoppingRestored++
        }

        // 6. Restore recipes
        var recipesRestored = 0
        for (doc in cloudRecipes.documents) {
            val data = doc.data ?: continue
            val name = (data["name"] as? String) ?: continue

            if (mergeWithLocal) {
                val existing = savedRecipeDao.findByName(name)
                if (existing != null) {
                    val cloudUpdatedAt = (data["updatedAt"] as? Long) ?: 0L
                    if (cloudUpdatedAt > existing.updatedAt) {
                        val entity = data.toSavedRecipeEntity(idOverride = existing.id)
                        savedRecipeDao.update(entity)
                        recipesRestored++
                    }
                    continue
                }
            }

            val entity = data.toSavedRecipeEntity(idOverride = 0)
            savedRecipeDao.insert(entity)
            recipesRestored++
        }

        // 7. Restore settings (cloud wins on merge)
        var settingsRestored = 0
        for (doc in cloudSettings.documents) {
            val data = doc.data ?: continue
            val entity = data.toSettingsEntity(idOverride = 0)
            if (entity.key in SyncConstants.SETTINGS_KEYS_TO_SYNC) {
                settingsRepository.set(entity.key, entity.value ?: "", entity.valueType, entity.description)
                settingsRestored++
            }
        }

        // 8. Update backup status to reflect cloud state (use cloud metadata, not restored count)
        settingsRepository.setInt(SettingsRepository.KEY_LAST_BACKUP_ITEM_COUNT, syncState.itemCount)
        settingsRepository.set(SettingsRepository.KEY_LAST_BACKUP_TIMESTAMP, syncState.lastBackupTimestamp.toString())

        // 9. Log analytics
        analyticsRepository.logCloudRestore(
            itemsRestored = itemsRestored,
            recipesRestored = recipesRestored,
            mergeMode = mergeWithLocal
        )

        Result.success(RestoreResult(
            itemsRestored = itemsRestored,
            shoppingRestored = shoppingRestored,
            recipesRestored = recipesRestored,
            categoriesRestored = categoriesRestored,
            locationsRestored = locationsRestored,
            unitsRestored = unitsRestored,
            settingsRestored = settingsRestored,
            warnings = warnings
        ))
    } catch (e: Exception) {
        Result.failure(e)
    } }

    // ─── Delete All Cloud Data ──────────────────────────────────

    suspend fun deleteAllCloudData(): Result<Unit> { return try {
        val user = auth.currentUser
        if (user == null || user.isAnonymous) {
            Result.success(Unit) // Nothing to delete
        } else {
            val userRef = userDocRef()
            val collections = listOf(
                SyncConstants.COLLECTION_INVENTORY,
                SyncConstants.COLLECTION_SHOPPING,
                SyncConstants.COLLECTION_RECIPES,
                SyncConstants.COLLECTION_CATEGORIES,
                SyncConstants.COLLECTION_SUBCATEGORIES,
                SyncConstants.COLLECTION_STORAGE_LOCATIONS,
                SyncConstants.COLLECTION_UNITS,
                SyncConstants.COLLECTION_SETTINGS,
                SyncConstants.COLLECTION_METADATA
            )
            for (collection in collections) {
                deleteCollectionDocs(userRef.collection(collection))
            }
            Result.success(Unit)
        }
    } catch (e: Exception) {
        Result.failure(e)
    } }

    // ─── Helpers ────────────────────────────────────────────────

    private suspend fun deleteCollectionDocs(
        collection: com.google.firebase.firestore.CollectionReference
    ) {
        val snapshot = collection.get().await()
        if (snapshot.isEmpty) return

        // Firestore WriteBatch limit is 500 operations
        val batches = snapshot.documents.chunked(500)
        for (chunk in batches) {
            val batch = firestore.batch()
            for (doc in chunk) {
                batch.delete(doc.reference)
            }
            batch.commit().await()
        }
    }

    private suspend fun writeBatched(
        collection: com.google.firebase.firestore.CollectionReference,
        documents: List<Map<String, Any?>>
    ) {
        if (documents.isEmpty()) return

        val batches = documents.chunked(450) // Leave room for other ops in same batch
        for (chunk in batches) {
            val batch = firestore.batch()
            for (doc in chunk) {
                batch.set(collection.document(), doc) // Auto-generated Firestore ID
            }
            batch.commit().await()
        }
    }

    private fun getAppVersion(): String = try {
        val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        pInfo.versionName ?: "unknown"
    } catch (_: Exception) {
        "unknown"
    }
}
