package com.inventory.app.data.remote.api

import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface UpcItemDbApi {
    @GET("prod/trial/lookup")
    suspend fun lookupProduct(
        @Query("upc") upc: String,
        @Header("User-Agent") userAgent: String = "RestokkApp/1.0 (Android)"
    ): UpcItemDbResponse
}

data class UpcItemDbResponse(
    val code: String?,
    val total: Int?,
    val items: List<UpcItemDbProduct>?
)

data class UpcItemDbProduct(
    val ean: String?,
    val title: String?,
    val brand: String?,
    val category: String?,
    val description: String?,
    val images: List<String>?
)
