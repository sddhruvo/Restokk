package com.inventory.app.data.repository

import com.inventory.app.data.local.dao.BarcodeCacheDao
import com.inventory.app.data.local.entity.BarcodeCacheEntity
import com.inventory.app.data.remote.api.OpenFoodFactsApi
import com.inventory.app.data.remote.api.UpcItemDbApi
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

data class BarcodeResult(
    val found: Boolean,
    val productName: String? = null,
    val brand: String? = null,
    val quantityInfo: String? = null,
    val categories: String? = null,
    val imageUrl: String? = null,
    val ingredients: String? = null,
    val nutritionGrade: String? = null,
    val errorMessage: String? = null
)

@Singleton
class BarcodeRepository @Inject constructor(
    private val barcodeCacheDao: BarcodeCacheDao,
    private val openFoodFactsApi: OpenFoodFactsApi,
    private val upcItemDbApi: UpcItemDbApi
) {
    suspend fun lookup(barcode: String): BarcodeResult {
        // Check cache first
        val cached = barcodeCacheDao.findByBarcode(barcode)
        if (cached != null && !isStale(cached)) {
            return BarcodeResult(
                found = cached.found,
                productName = cached.productName,
                brand = cached.brand,
                quantityInfo = cached.quantityInfo,
                categories = cached.categories,
                imageUrl = cached.imageUrl,
                ingredients = cached.ingredients,
                nutritionGrade = cached.nutritionGrade
            )
        }

        // If stale, delete old cache
        if (cached != null) {
            barcodeCacheDao.deleteByBarcode(barcode)
        }

        // Try Open Food Facts first
        val offResult = tryOpenFoodFacts(barcode)
        if (offResult.found) {
            cacheResult(barcode, offResult)
            return offResult
        }

        // Fallback: try UPC Item DB
        val upcResult = tryUpcItemDb(barcode)
        if (upcResult.found) {
            cacheResult(barcode, upcResult)
            return upcResult
        }

        // Nothing found in either API
        val notFound = BarcodeResult(found = false)
        cacheResult(barcode, notFound)
        return notFound
    }

    private suspend fun tryOpenFoodFacts(barcode: String): BarcodeResult {
        return try {
            val response = openFoodFactsApi.lookupProduct(barcode)
            val product = response.product
            val found = response.status == 1 && product?.product_name != null

            BarcodeResult(
                found = found,
                productName = product?.product_name,
                brand = product?.brands,
                quantityInfo = product?.quantity,
                categories = product?.categories,
                imageUrl = product?.image_front_url ?: product?.image_url,
                ingredients = product?.ingredients_text,
                nutritionGrade = product?.nutrition_grades
            )
        } catch (e: Exception) {
            BarcodeResult(found = false)
        }
    }

    private suspend fun tryUpcItemDb(barcode: String): BarcodeResult {
        return try {
            val response = upcItemDbApi.lookupProduct(barcode)
            val item = response.items?.firstOrNull()

            if (response.code == "OK" && item?.title != null) {
                BarcodeResult(
                    found = true,
                    productName = item.title,
                    brand = item.brand,
                    categories = item.category,
                    imageUrl = item.images?.firstOrNull()
                )
            } else {
                BarcodeResult(found = false)
            }
        } catch (e: Exception) {
            BarcodeResult(found = false)
        }
    }

    private suspend fun cacheResult(barcode: String, result: BarcodeResult) {
        barcodeCacheDao.insert(
            BarcodeCacheEntity(
                barcode = barcode,
                found = result.found,
                productName = result.productName,
                brand = result.brand,
                quantityInfo = result.quantityInfo,
                categories = result.categories,
                imageUrl = result.imageUrl,
                ingredients = result.ingredients,
                nutritionGrade = result.nutritionGrade
            )
        )
    }

    private fun isStale(cached: BarcodeCacheEntity): Boolean {
        val hoursSince = ChronoUnit.HOURS.between(cached.createdAt, LocalDateTime.now())
        // Found entries: 30-day TTL; failed lookups: 48-hour TTL
        val ttlHours = if (cached.found) 30L * 24 else 48L
        return hoursSince > ttlHours
    }
}
