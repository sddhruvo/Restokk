package com.inventory.app.data.remote.api

import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path

interface OpenFoodFactsApi {
    @GET("api/v2/product/{barcode}")
    suspend fun lookupProduct(
        @Path("barcode") barcode: String,
        @Header("User-Agent") userAgent: String = "RestokkApp/1.0 (Android)"
    ): OpenFoodFactsResponse
}

data class OpenFoodFactsResponse(
    val code: String?,
    val status: Int?,
    val product: ProductData?
)

data class ProductData(
    val product_name: String?,
    val brands: String?,
    val quantity: String?,
    val categories: String?,
    val image_url: String?,
    val image_front_url: String?,
    val ingredients_text: String?,
    val nutrition_grades: String?
)
