package com.inventory.app.data.repository

import android.util.Log
import com.google.firebase.functions.FirebaseFunctions
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonArray
import com.google.gson.reflect.TypeToken
import com.inventory.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

data class FridgeItem(
    val name: String = "",
    val quantity: Double = 1.0,
    val unit: String? = null,
    val category: String? = null,
    val confidence: String = "medium",  // "high", "medium", "low"
    val estimatedExpiryDays: Int? = null
)

data class ReceiptItem(
    val name: String = "",
    val quantity: Double = 1.0,
    val price: Double? = null,
    val unit: String? = null,
    val category: String? = null,
    val matchedShoppingId: Long? = null,
    val matchedInventoryId: Long? = null,
    val estimatedExpiryDays: Int? = null
)

/**
 * Image compression settings — returned by [GrokRepository.getImageConfig]
 * so ViewModels don't need to know which provider is active.
 */
data class ImageConfig(
    val maxDimension: Int,
    val startQuality: Int,
    val maxBytes: Int,
    val minQuality: Int,
    val fallbackScale: Float
)

@Singleton
class GrokRepository @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val gson: Gson,
    private val okHttpClient: OkHttpClient,
    private val analyticsRepository: AnalyticsRepository,
    private val functions: FirebaseFunctions,
    private val authRepository: AuthRepository
) {
    // Extended timeout for vision operations — image analysis can exceed the default 60s
    private val visionClient: OkHttpClient by lazy {
        okHttpClient.newBuilder()
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    // ── Provider resolution (SINGLE SOURCE OF TRUTH) ──────────────────

    private data class VisionProvider(
        val model: String,
        val url: String,
        val apiKey: String,
        val detail: String?,
        val name: String
    )

    /**
     * Resolves which AI provider to use for vision tasks.
     * OpenAI takes priority if key is available; falls back to Groq.
     * Change provider logic HERE — nowhere else.
     */
    private fun resolveVisionProvider(): VisionProvider {
        val openAiKey = getOpenAiApiKey()
        return if (openAiKey != null) {
            VisionProvider(MODEL_VISION_OPENAI, OPENAI_URL, openAiKey, "high", "OpenAI")
        } else {
            VisionProvider(MODEL_VISION_GROQ, GROQ_URL, getGroqApiKey(), null, "Groq")
        }
    }

    private fun getGroqApiKey(): String {
        val settingsKey = settingsRepository.getSecureString(SettingsRepository.KEY_GROK_API_KEY, "")
        val apiKey = settingsKey.ifBlank { BuildConfig.GROK_API_KEY }
        if (apiKey.isBlank()) throw IllegalStateException("AI service unavailable. Please try again later.")
        return apiKey
    }

    private fun getOpenAiApiKey(): String? {
        val settingsKey = settingsRepository.getSecureString(SettingsRepository.KEY_OPENAI_API_KEY, "")
        val key = settingsKey.ifBlank { BuildConfig.OPENAI_API_KEY }
        return key.ifBlank { null }
    }

    /**
     * Returns image compression settings tuned for the active vision provider.
     * OpenAI detail=high benefits from larger images; Groq needs smaller payloads.
     * Call this from ViewModels before compressing — no provider logic needed there.
     */
    fun getImageConfig(): ImageConfig {
        return if (getOpenAiApiKey() != null) {
            ImageConfig(maxDimension = 3200, startQuality = 90, maxBytes = 1_500_000, minQuality = 50, fallbackScale = 0.7f)
        } else {
            ImageConfig(maxDimension = 2000, startQuality = 80, maxBytes = 750_000, minQuality = 40, fallbackScale = 0.6f)
        }
    }

    // ── Cloud Function proxy ─────────────────────────────────────────

    /**
     * Calls the AI proxy Cloud Function. The function adds API keys server-side,
     * checks quota and rate limits, then forwards to Groq/OpenAI.
     *
     * Falls back to direct API call if:
     * - User is not authenticated
     * - Cloud Function call fails (e.g., not deployed yet)
     * - Local API keys are configured (development mode)
     */
    private suspend fun executeViaProxy(
        provider: String,
        bodyJson: String,
        directClient: OkHttpClient,
        directUrl: String,
        directApiKey: String
    ): String {
        // Try Cloud Function first (if authenticated)
        val user = authRepository.currentUser
        if (user != null) {
            try {
                val data = hashMapOf(
                    "provider" to provider,
                    "body" to gson.fromJson(bodyJson, Map::class.java)
                )
                val result = functions
                    .getHttpsCallable("aiProxy")
                    .call(data)
                    .await()

                @Suppress("UNCHECKED_CAST")
                val responseMap = result.data as? Map<String, Any>
                    ?: throw Exception("Empty response from Cloud Function")

                // Extract content from the AI response (same format as direct API)
                val choices = responseMap["choices"] as? List<*>
                val firstChoice = (choices?.firstOrNull() as? Map<*, *>)
                val message = firstChoice?.get("message") as? Map<*, *>
                val content = message?.get("content") as? String
                    ?: throw Exception("No content in Cloud Function response")

                if (BuildConfig.DEBUG) Log.d(TAG, "Cloud Function response OK (${content.length} chars)")
                return content
            } catch (e: Exception) {
                val errorMsg = e.message ?: ""
                // If quota exceeded or rate limited, don't fall back — surface the error
                if (errorMsg.contains("Daily AI limit") || errorMsg.contains("Too many requests")) {
                    throw e
                }
                Log.w(TAG, "Cloud Function failed, trying direct: ${e.message}")
            }
        }

        // Fallback: direct API call (dev mode or Cloud Function not deployed)
        return executeRequest(bodyJson, directClient, directUrl, directApiKey)
    }

    // ── Public API ────────────────────────────────────────────────────

    /**
     * General-purpose text completion (always Groq — text stays cheap).
     */
    suspend fun chatCompletion(
        systemPrompt: String,
        userPrompt: String,
        temperature: Double = 0.3,
        maxTokens: Int = 2048
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val body = buildTextRequestJson(MODEL_TEXT, systemPrompt, userPrompt, temperature, maxTokens)
            val text = executeViaProxy("groq", body, okHttpClient, GROQ_URL, getGroqApiKey())
            analyticsRepository.logAiRequest("text")
            Result.success(text)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Generic vision analysis — sends image + prompt to the active vision provider.
     */
    suspend fun visionAnalysis(
        systemPrompt: String,
        userPrompt: String,
        imageBase64: String,
        mimeType: String = "image/jpeg",
        temperature: Double = 0.3,
        maxTokens: Int = 4096
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val provider = resolveVisionProvider()
            val providerName = if (provider.url == OPENAI_URL) "openai" else "groq"
            val body = buildVisionRequestJson(
                provider.model, systemPrompt, userPrompt,
                imageBase64, mimeType, temperature, maxTokens, provider.detail
            )
            val text = executeViaProxy(providerName, body, visionClient, provider.url, provider.apiKey)
            analyticsRepository.logAiRequest("vision_${provider.name}")
            Result.success(text)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Receipt scanning ──────────────────────────────────────────────

    data class ShoppingListContext(val id: Long, val name: String)
    data class InventoryContext(val id: Long, val name: String)

    suspend fun parseReceiptImage(
        imageBase64: String,
        shoppingList: List<ShoppingListContext> = emptyList(),
        inventoryItems: List<InventoryContext> = emptyList(),
        categoryNames: List<String> = emptyList()
    ): Result<List<ReceiptItem>> {
        return withContext(Dispatchers.IO) {
            try {
                val categorySection = if (categoryNames.isNotEmpty()) {
                    val catList = categoryNames.joinToString(", ") { "\"$it\"" }
                    "\n\nCATEGORIES: [$catList]\n" +
                    "For each product, pick the BEST matching category from the list above and set \"category\" to that exact string. " +
                    "If no category fits well, omit the field or set it to null."
                } else ""

                val shoppingListSection = if (shoppingList.isNotEmpty()) {
                    val listJson = shoppingList.joinToString(", ") {
                        """{"id":${it.id},"name":${gson.toJson(it.name)}}"""
                    }
                    "\n\nSHOPPING LIST: [$listJson]\n" +
                    "Match receipt items to shopping list by CATEGORY. Add \"matchedShoppingId\":<id> to matched items.\n" +
                    "Shopping lists use generic names — match broadly: Tea→Tetley/PG Tips, Snacks→Doritos/KitKat/Crisps, Bread→Hovis/Warburtons, Milk→Semi-Skimmed/Oat Milk.\n" +
                    "One list item can match multiple receipt items. Only skip if totally different category."
                } else ""

                val inventorySection = if (inventoryItems.isNotEmpty()) {
                    val invJson = inventoryItems.joinToString(", ") {
                        """{"id":${it.id},"name":${gson.toJson(it.name)}}"""
                    }
                    "\n\nINVENTORY: [$invJson]\n" +
                    "If a receipt item is the same product as an inventory item, add \"matchedInventoryId\":<id>. Brand/size variants OK. When unsure, don't match."
                } else ""

                val prompt = RECEIPT_VISION_PROMPT + categorySection + shoppingListSection + inventorySection
                val provider = resolveVisionProvider()
                if (BuildConfig.DEBUG) Log.d(TAG, "Receipt scan using ${provider.name} (${provider.model})")

                retryVision(provider, "You are an expert receipt parser. You only respond with valid JSON arrays. No markdown, no explanation.", prompt, imageBase64, temperature = 0.1) { text ->
                    parseResponse(text)
                } ?: Result.failure(Exception("Failed to parse receipt after $MAX_RETRIES attempts. Please try again."))

            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    // ── Kitchen / fridge scanning ─────────────────────────────────────

    suspend fun parseFridgeImage(
        imageBase64: String,
        area: String = "refrigerator",
        existingInventory: List<String> = emptyList(),
        shoppingList: List<String> = emptyList(),
        categoryNames: List<String> = emptyList(),
        previouslyFoundItems: List<String> = emptyList(),
        areaHints: String = "",
        minExpectedItems: Int = 12
    ): Result<List<FridgeItem>> {
        return withContext(Dispatchers.IO) {
            try {
                val categorySection = if (categoryNames.isNotEmpty()) {
                    val catList = categoryNames.joinToString(", ") { "\"$it\"" }
                    "\n\nCATEGORIES: [$catList]\n" +
                    "For each item, pick the BEST matching category from the list above and set \"category\" to that exact string. " +
                    "If no category fits well, omit the field or set it to null."
                } else ""

                val previousItemsSection = if (previouslyFoundItems.isNotEmpty()) {
                    val sanitized = previouslyFoundItems.map { item ->
                        item.replace(Regex("[\"\\n\\r\\\\\\x00-\\x1F]"), "").take(100).trim()
                    }
                    val prevList = sanitized.joinToString(", ") { "\"$it\"" }
                    "\n\nPREVIOUSLY FOUND IN OTHER AREAS (for dedup only): [$prevList]\n" +
                    "These were found in other kitchen areas. Do NOT include them unless you have CLEAR visual evidence in THIS photo. " +
                    "This list is ONLY for deduplication — NEVER use it as a hint of what to look for."
                } else ""

                val basePrompt = if (areaHints.isNotBlank()) {
                    FRIDGE_VISION_PROMPT_AREA
                        .replace("{AREA}", area)
                        .replace("{AREA_SCAN_INSTRUCTIONS}", areaHints)
                        .replace("{MIN_ITEMS}", minExpectedItems.toString())
                } else {
                    FRIDGE_VISION_PROMPT_QUICK.replace("{AREA}", area)
                }
                val prompt = basePrompt + categorySection + previousItemsSection

                val provider = resolveVisionProvider()
                if (BuildConfig.DEBUG) Log.d(TAG, "Kitchen scan using ${provider.name} (${provider.model})")

                retryVision(provider, FRIDGE_SYSTEM_PROMPT, prompt, imageBase64, temperature = 0.2, maxTokens = 8192) { text ->
                    val items = parseFridgeResponse(text)
                    if (BuildConfig.DEBUG && items.isNotEmpty()) {
                        Log.d(TAG, "Successfully parsed ${items.size} fridge items")
                        items.forEachIndexed { i, item ->
                            Log.d(TAG, "  Item ${i+1}: ${item.name} | qty=${item.quantity} ${item.unit ?: ""} | ${item.confidence} | cat=${item.category ?: "?"}")
                        }
                    }
                    items
                } ?: Result.failure(Exception(
                    "Failed to identify items after $MAX_RETRIES attempts.\n\n" +
                    "Tips: Make sure items are clearly visible, the photo is well-lit, and not too blurry."
                ))

            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    // ── Shared retry logic for vision tasks ───────────────────────────

    private suspend fun <T> retryVision(
        provider: VisionProvider,
        systemPrompt: String,
        userPrompt: String,
        imageBase64: String,
        temperature: Double = 0.2,
        maxTokens: Int = 4096,
        parser: (String) -> List<T>
    ): Result<List<T>>? {
        var lastText = ""
        for (attempt in 1..MAX_RETRIES) {
            if (attempt > 1) {
                kotlinx.coroutines.delay(1000L * (1 shl (attempt - 2)))
            }
            try {
                val body = buildVisionRequestJson(
                    provider.model, systemPrompt, userPrompt,
                    imageBase64, "image/jpeg", temperature, maxTokens, provider.detail
                )
                val providerName = if (provider.url == OPENAI_URL) "openai" else "groq"
                lastText = executeViaProxy(providerName, body, visionClient, provider.url, provider.apiKey)
                if (BuildConfig.DEBUG) Log.d(TAG, "Vision attempt $attempt (${lastText.length} chars): ${lastText.take(200)}")

                val hasJson = lastText.contains("[") && lastText.contains("]")
                if (!hasJson) {
                    Log.w(TAG, "Attempt $attempt: no JSON array found, retrying...")
                    continue
                }

                val items = parser(lastText)
                if (items.isNotEmpty()) return Result.success(items)

                Log.w(TAG, "Attempt $attempt: parsed 0 items, retrying...")
            } catch (e: Exception) {
                Log.w(TAG, "Attempt $attempt failed: ${e.message}")
                if (attempt >= MAX_RETRIES) throw e
            }
        }
        Log.w(TAG, "All $MAX_RETRIES attempts failed. Last: $lastText")
        return null
    }

    // ── Response parsers ──────────────────────────────────────────────

    private fun parseFridgeResponse(responseText: String): List<FridgeItem> {
        val jsonStr = responseText.replace("```json", "").replace("```", "").trim()
        val arrayStr = extractJsonArray(jsonStr) ?: jsonStr
        return try {
            val type = object : TypeToken<List<FridgeItem>>() {}.type
            gson.fromJson<List<FridgeItem>>(arrayStr, type).filter { it.name.isNotBlank() }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse fridge items: ${e.message}")
            try {
                val item = gson.fromJson(arrayStr, FridgeItem::class.java)
                if (item.name.isNotBlank()) listOf(item) else emptyList()
            } catch (_: Exception) { emptyList() }
        }
    }

    private fun parseResponse(responseText: String): List<ReceiptItem> {
        val jsonStr = responseText.replace("```json", "").replace("```", "").trim()
        if (BuildConfig.DEBUG) Log.d(TAG, "Parsing receipt response (${jsonStr.length} chars): ${jsonStr.take(300)}")
        val arrayStr = extractJsonArray(jsonStr) ?: jsonStr
        return try {
            val type = object : TypeToken<List<ReceiptItem>>() {}.type
            gson.fromJson<List<ReceiptItem>>(arrayStr, type).filter { it.name.isNotBlank() }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse as list: ${e.message}")
            try {
                val item = gson.fromJson(arrayStr, ReceiptItem::class.java)
                if (item.name.isNotBlank()) listOf(item) else emptyList()
            } catch (_: Exception) { emptyList() }
        }
    }

    // ── HTTP + JSON helpers ───────────────────────────────────────────

    private fun executeRequest(
        jsonBody: String,
        client: OkHttpClient,
        url: String,
        apiKey: String
    ): String {
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(jsonBody.toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            val responseBody = response.body?.string()
            if (BuildConfig.DEBUG) Log.d(TAG, "Response code: ${response.code}")
            if (!response.isSuccessful) {
                val errorMsg = parseApiError(responseBody)
                throw Exception(errorMsg ?: "API error: ${response.code}")
            }
            val provider = if (url.contains("openai.com")) "OpenAI" else "Groq"
            return extractResponseContent(responseBody)
                ?: throw Exception("Empty response from $provider")
        }
    }

    private fun buildTextRequestJson(
        model: String, systemPrompt: String, userPrompt: String,
        temperature: Double, maxTokens: Int
    ): String {
        val body = JsonObject().apply {
            addProperty("model", model)
            add("messages", JsonArray().apply {
                add(JsonObject().apply { addProperty("role", "system"); addProperty("content", systemPrompt) })
                add(JsonObject().apply { addProperty("role", "user"); addProperty("content", userPrompt) })
            })
            addProperty("temperature", temperature)
            addProperty("max_tokens", maxTokens)
        }
        return gson.toJson(body)
    }

    private fun buildVisionRequestJson(
        model: String, systemPrompt: String, userPrompt: String,
        imageBase64: String, mimeType: String,
        temperature: Double, maxTokens: Int,
        detail: String? = null
    ): String {
        val imageUrl = JsonObject().apply {
            addProperty("url", "data:$mimeType;base64,$imageBase64")
            if (detail != null) addProperty("detail", detail)
        }
        val body = JsonObject().apply {
            addProperty("model", model)
            add("messages", JsonArray().apply {
                add(JsonObject().apply { addProperty("role", "system"); addProperty("content", systemPrompt) })
                add(JsonObject().apply {
                    addProperty("role", "user")
                    add("content", JsonArray().apply {
                        add(JsonObject().apply { addProperty("type", "text"); addProperty("text", userPrompt) })
                        add(JsonObject().apply { addProperty("type", "image_url"); add("image_url", imageUrl) })
                    })
                })
            })
            addProperty("temperature", temperature)
            addProperty("max_tokens", maxTokens)
        }
        return gson.toJson(body)
    }

    private fun extractResponseContent(responseBody: String?): String? {
        if (responseBody == null) return null
        return try {
            gson.fromJson(responseBody, JsonObject::class.java)
                .getAsJsonArray("choices")?.get(0)?.asJsonObject
                ?.getAsJsonObject("message")?.get("content")?.asString
        } catch (_: Exception) { null }
    }

    private fun parseApiError(responseBody: String?): String? {
        if (responseBody == null) return null
        return try {
            gson.fromJson(responseBody, JsonObject::class.java)
                .getAsJsonObject("error")?.get("message")?.asString
        } catch (_: Exception) { null }
    }

    private fun extractJsonArray(text: String): String? {
        val start = text.indexOf('[')
        if (start == -1) return null
        val end = text.lastIndexOf(']')
        if (end == -1 || end <= start) return null
        return text.substring(start, end + 1)
    }

    // ── Constants ─────────────────────────────────────────────────────
    // To switch AI providers, ONLY change these constants.

    companion object {
        private const val TAG = "GroqAPI"
        private const val MAX_RETRIES = 3

        // Provider endpoints
        private const val GROQ_URL = "https://api.groq.com/openai/v1/chat/completions"
        private const val OPENAI_URL = "https://api.openai.com/v1/chat/completions"

        // Models — change HERE to swap providers
        private const val MODEL_TEXT = "llama-3.3-70b-versatile"          // Text: always Groq
        private const val MODEL_VISION_GROQ = "meta-llama/llama-4-scout-17b-16e-instruct"  // Vision fallback
        private const val MODEL_VISION_OPENAI = "gpt-4o"                  // Vision primary (if key set)

        // ── Kitchen scan prompts ──────────────────────────────────────

        private val FRIDGE_SYSTEM_PROMPT = """
You are an expert inventory recognition AI. Analyze images and return all visible items as JSON.

═══ INTEGRITY RULES ═══
• VISUAL EVIDENCE ONLY: Every item MUST be physically visible in the image. If you cannot point to where it is, do NOT include it.
• ZERO HALLUCINATION: Do NOT add items because they "should" be there, are common in kitchens, or appear in any provided context.
• SEPARATE ENTRIES: Each distinct food type gets its own entry — apples ≠ pears, lemons ≠ limes.
• COUNT INDIVIDUALLY: Report individual pieces (7 apples = quantity 7), not "1 bag."

═══ IDENTIFICATION METHODOLOGY ═══
When an item's label is unclear or unreadable, identify it using ALL available signals simultaneously:
1. TEXT FIRST — Read any partial text, letters, numbers, or symbols. Even 1-2 characters narrow the answer.
2. CONTENTS — What is inside? Color, texture, consistency, form (powder, liquid, granules, solid) are strong identifiers.
3. CONTAINER — Shape, size, material, closure type (spray top, shaker, squeeze tip, pull tab) tell you the product category.
4. COLOR & DESIGN — Label colors, patterns, and graphic style are brand/product signals even when blurry.
5. CONTEXT — What is around it? Items are usually stored near related items.
6. PROBABILITY — Given all signals, what is the single most likely specific item?

═══ NAMING RULES ═══
• NEVER return a container type as a name ("bottle", "jar", "container", "bag" are NOT item names)
• NEVER return a category as a name ("spice", "condiment", "food item", "product" are NOT item names)
• NEVER group items into vague entries ("various spices" is forbidden — list each separately)
• Always return the MOST SPECIFIC name your reasoning supports
• A specific guess at low confidence is better than a vague category

═══ DEPTH SCANNING ═══
• Images contain items at MULTIPLE depth layers — not just the front row.
• You MUST scan foreground, middle layer, AND background items.
• Partially visible items (tops peeking over, label edges visible beside other items, items seen through gaps) are VALID entries — report them with appropriate confidence.
• A scan that only reports the front row is incomplete and FAILED.

═══ CONFIDENCE ═══
• "high" = label clearly readable or unmistakable item
• "medium" = identified through strong visual inference (shape, color, context)
• "low" = best specific guess with limited signals — still a specific name, never generic

Think carefully before responding. You may include brief reasoning before the JSON array. Your response must contain a valid JSON array.
""".trimIndent()

        private val FRIDGE_VISION_PROMPT_AREA = """
Photo of a {AREA}. Find EVERY food item visible. Users rely on this to track their kitchen — missing items means wasted food.

{AREA_SCAN_INSTRUCTIONS}

═══ SCANNING METHOD ═══

PASS 1 — OBVIOUS ITEMS: Scan left to right, top to bottom. List every clearly visible item.

PASS 2 — LOOK INSIDE BAGS & CONTAINERS: Every plastic bag, mesh bag, or paper bag — look THROUGH it. What color? What shape inside? Identify contents and count individual items.

PASS 3 — DEPTH & OCCLUSION (scan ALL layers, not just front row):
• FOREGROUND: Items closest to camera, fully visible
• MIDDLE LAYER: Items partially visible behind foreground — look for tops peeking over, label edges beside front items
• BACKGROUND: Items only slightly visible at edges, tops, or through gaps between foreground items
• BOTTOM/BASE: Items sitting at the bottom, often hidden under shelf lip or behind door rack
• Look THROUGH gaps between items — anything visible in those windows counts
• Partially visible items are valid entries — set confidence to "medium" or "low"

PASS 4 — DIFFERENTIATE SIMILAR ITEMS (do NOT merge):
• Each distinct fruit/vegetable type gets its OWN entry
• Apples ≠ pears, lemons ≠ limes, white cabbage ≠ red cabbage
• Different varieties of lettuce/greens — SEPARATE entries

PASS 5 — COUNT INDIVIDUAL ITEMS:
• Count individual pieces, not bags (6 apples in a bag = quantity 6, unit "pcs")
• If exact count is hard, estimate on the HIGH side

═══ ITEM STATE ═══
• WHOLE → no suffix, qty 1.0
• HALVED → "(half)", qty 0.5
• SLICED/CUT → "(sliced)", qty 1.0
• OPENED → "(opened)", qty 1.0

═══ FINAL CHECK ═══
Count your items. Fewer than {MIN_ITEMS} for a {AREA}? You likely missed things — scan every zone again.

═══ ACCURACY VERIFICATION (CRITICAL) ═══
Before finalizing, review EVERY item:
• WHERE in the image is this item? (which zone/shelf/section)
• WHAT visual evidence confirms it? (shape, color, label text, packaging)
• If you CANNOT answer both → REMOVE the item
• Do NOT include items based on assumptions about what this area typically contains

═══ JSON FORMAT ═══
• name: Most specific name possible (use identification methodology from system prompt). Include brand + size if label readable.
• quantity: INDIVIDUAL count (not "1 bag"). 0.5 for halved items.
• unit: "pcs"|"kg"|"g"|"lb"|"oz"|"L"|"ml"|"bottle"|"can"|"box"|"bag"|"jar"|"pack"|"bunch"|"doz"|"carton"|"tub"
• category: from CATEGORIES list if provided, EXACT string, null if no match
• confidence: "high"|"medium"|"low" (as defined in system prompt)
• estimatedExpiryDays: fresh produce ~7, meat ~3, dairy ~7, bread ~5, eggs ~28, cheese ~60, canned ~730, frozen ~180, condiments ~180, opened/cut ~2-3. null if unsure.

Respond with a JSON array.
Example: [{"name":"Whole Milk 1L","quantity":1.0,"unit":"carton","category":"Dairy & Eggs","confidence":"high","estimatedExpiryDays":7},{"name":"Avocado (half)","quantity":0.5,"unit":"pcs","category":"Fruits","confidence":"high","estimatedExpiryDays":2},{"name":"Red Bell Pepper","quantity":3.0,"unit":"pcs","category":"Vegetables","confidence":"medium","estimatedExpiryDays":7}]
Empty: []
""".trimIndent()

        private val FRIDGE_VISION_PROMPT_QUICK = """
Analyze this photo. First, determine what kitchen area you're looking at (refrigerator, freezer, pantry, countertop, cabinet, etc.) based on visual cues. Then find EVERY food item visible.

═══ SCANNING METHOD ═══

PASS 1 — ZONE-BY-ZONE SCAN:
• Divide the visible area into zones (shelves, sections, drawers, surfaces)
• For EACH zone: scan left to right, top to bottom, front to back
• What's clearly visible? What's partially hidden behind other items?

PASS 2 — LOOK INSIDE BAGS & CONTAINERS:
• Clear bags → look THROUGH them. What color/shape inside? Count individual items.
• Mesh/net bags → identify contents visible through the mesh
• Paper bags or opaque containers → describe what's visible, confidence "low"
• NEVER report "1 bag" — identify and count the CONTENTS

PASS 3 — DEPTH & OCCLUSION (scan ALL layers, not just front row):
• FOREGROUND: Items closest to camera, fully visible
• MIDDLE LAYER: Items partially visible behind foreground — tops peeking over, label edges beside front items
• BACKGROUND: Items only slightly visible at edges, tops, or through gaps between foreground items
• BOTTOM/BASE: Items at the bottom, often hidden under shelf lip or behind other items
• Look THROUGH gaps between items — anything visible in those windows counts
• Partially visible items are valid entries — set confidence to "medium" or "low"

PASS 4 — DIFFERENTIATE SIMILAR ITEMS (do NOT merge):
• Each distinct food type gets its OWN entry
• Apples ≠ pears, lemons ≠ limes, white cabbage ≠ red cabbage
• Different varieties of similar items — SEPARATE entries

PASS 5 — COUNT INDIVIDUAL ITEMS:
• Count individual pieces, not bags (6 apples in a bag = quantity 6, unit "pcs")
• If exact count is difficult, estimate on the HIGH side

═══ ITEM STATE ═══
• WHOLE → no suffix, qty 1.0
• HALVED → "(half)", qty 0.5
• SLICED/CUT → "(sliced)", qty 1.0
• OPENED → "(opened)", qty 1.0

═══ ACCURACY VERIFICATION (CRITICAL) ═══
Before finalizing, review EVERY item:
• WHERE in the image is this item? (which zone/shelf/section)
• WHAT visual evidence confirms it? (shape, color, label text, packaging)
• If you CANNOT answer both → REMOVE the item
• Do NOT include items based on assumptions about what this area typically contains

═══ JSON FORMAT ═══
• name: Most specific name possible (use identification methodology from system prompt). Include brand + size if label readable.
• quantity: INDIVIDUAL count (not "1 bag"). 0.5 for halved items.
• unit: "pcs"|"kg"|"g"|"lb"|"oz"|"L"|"ml"|"bottle"|"can"|"box"|"bag"|"jar"|"pack"|"bunch"|"doz"|"carton"|"tub"
• category: from CATEGORIES list if provided, EXACT string, null if no match
• confidence: "high"|"medium"|"low" (as defined in system prompt)
• estimatedExpiryDays: fresh produce ~7, meat ~3, dairy ~7, bread ~5, eggs ~28, cheese ~60, canned ~730, frozen ~180, condiments ~180, opened/cut ~2-3. null if unsure.

Respond with a JSON array.
Example: [{"name":"Whole Milk 1L","quantity":1.0,"unit":"carton","category":"Dairy & Eggs","confidence":"high","estimatedExpiryDays":7},{"name":"Banana","quantity":5.0,"unit":"pcs","category":"Fruits","confidence":"high","estimatedExpiryDays":5},{"name":"Red Bell Pepper","quantity":2.0,"unit":"pcs","category":"Vegetables","confidence":"medium","estimatedExpiryDays":7}]
Empty: []
""".trimIndent()

        // ── Receipt scan prompt ───────────────────────────────────────

        private val RECEIPT_VISION_PROMPT = """
Look at this receipt image carefully. Extract EVERY purchased product with its correct price.

CRITICAL — HOW TO READ PRICES CORRECTLY:
1. FOLLOW THE LINE: For each product name on the left, trace your eye horizontally to the RIGHT to find its price. The price is the number aligned on the right side of the SAME visual line.
2. MULTI-LINE ITEMS: Some items span 2 lines. The product name is on line 1, and the weight/price details are on line 2 directly below. Treat them as ONE item. Examples:
   - Line 1: "White Cabbage 0082955"
   - Line 2: "1.346 kg @ £0.79/kg     1.06"
   → This is ONE item: name="White Cabbage", qty=1.346, unit=kg, price=1.06
3. WEIGHT-BASED ITEMS: When you see "X.XXX kg @ £Y.YY/kg" followed by a total, use:
   - quantity = the weight (X.XXX)
   - unit = "kg" (or "lb")
   - price = the FINAL TOTAL on the right (NOT the per-kg price)
4. QUANTITY MULTIPLES: "2x £1.19" or "x2" means quantity=2. The price shown is the LINE TOTAL (for all units), not per-unit.
5. DISCOUNTS & SAVINGS: If a line says "SAVING", "DISCOUNT", "OFF", or shows a negative number (e.g., "-0.50"), SUBTRACT it from the item directly above. Return the net price the customer actually paid.
6. MULTI-BUY DEALS: "2 FOR £3.00" or "3 FOR 2" — if one product, qty=2, price=3.00. If the deal applies to the item above, adjust that item's price.
7. TAX INDICATORS: Letters like "A", "B", "F", "T" next to prices are tax codes. IGNORE them — they are not part of the price or product name.

PRODUCT NAME RULES:
- Receipts use abbreviations. ALWAYS expand to a full, readable name:
  "TS BRIT SM FR RNG" → "Tesco British Small Free Range Eggs"
  "HOVIS WHL 800G" → "Hovis Wholemeal Bread 800g"
  "GV 2% MLK GAL" → "Great Value 2% Milk 1 Gallon"
  "BRI HOT DOG ROL" → "Brioche Hot Dog Rolls"
  "ORG BNNA" → "Organic Banana"
- Include size/weight in the name if visible (e.g., "800g", "2L", "6 pack")
- Remove product codes/barcodes (long numbers like "0082955") from the name

WHAT TO IGNORE (not products):
- Store name, address, phone number
- Date, time, receipt number, transaction number
- Barcodes, product codes (long digit strings)
- Payment method (CARD, CASH, Visa, Mastercard)
- Subtotal, TOTAL, VAT/tax lines, change/balance
- Loyalty points, rewards, survey URLs
- "CUSTOMER COPY", "PLEASE RETAIN", footer text

FOR EACH PRODUCT, RETURN THESE JSON FIELDS:
- name: Full readable product name (expanded from abbreviations)
- quantity: Number (default 1.0). Use actual weight for weighed items. Use count for "2x" multiples.
- price: The TOTAL price paid for this line item after any discounts. Use dot decimal (1.45 not 1,45). null ONLY if the price is genuinely not visible on the receipt.
- unit: One of: "pcs", "kg", "g", "lb", "oz", "L", "ml", "bag", "bottle", "can", "box", "loaf", "bunch", "pack", "doz", "gal". Infer from product type if not shown.
- category: If a CATEGORIES list is provided below, pick the best matching category name from that list. Use the EXACT string from the list. Omit or null if no good match.
- estimatedExpiryDays: Estimated days until expiry. Fresh milk ~7, bread ~5, fresh meat ~3, eggs ~28, yogurt ~14, cheese ~60, fresh fruit ~7, vegetables ~7, canned ~730, pasta/rice ~365, frozen ~180, butter ~60, cereal ~180, biscuits ~90, chocolate ~180, sauces ~180. null for non-food.
- matchedShoppingId: If a SHOPPING LIST is provided below, check each receipt item against it. Use the shopping list item's "id" value. Match by category (Tea→any tea, Snacks→any snack). Omit if no list or no match.
- matchedInventoryId: If an INVENTORY is provided below, match same products. Use the inventory item's "id" value. Omit if no inventory or no match.

Respond ONLY with a JSON array. No explanation, no markdown, no code blocks.
Example: [{"name":"Tetley Teabags 80","quantity":1.0,"price":2.75,"unit":"box","category":"Beverages","estimatedExpiryDays":365,"matchedShoppingId":5},{"name":"Doritos 150g","quantity":1.0,"price":1.50,"unit":"bag","category":"Snacks","estimatedExpiryDays":90,"matchedShoppingId":8},{"name":"Semi-Skimmed Milk 2L","quantity":1.0,"price":1.65,"unit":"bottle","category":"Dairy & Eggs","estimatedExpiryDays":7,"matchedInventoryId":12}]
If no products found: []
""".trimIndent()
    }
}
