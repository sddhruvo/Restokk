package com.inventory.app.domain.model

import com.inventory.app.util.ItemNameNormalizer
import kotlin.math.ln
import kotlin.math.sqrt

/**
 * Tiered TF-IDF duplicate detection engine.
 * Matches incoming product names against the user's inventory using 4 layers:
 *   1. Barcode match (DEFINITE)
 *   2. Exact normalized match (DEFINITE)
 *   3. TF-IDF cosine similarity (LIKELY / POSSIBLE)
 *   4. Single-token containment (POSSIBLE only)
 *
 * Stateless — all state passed via parameters.
 */
object ProductMatcher {

    data class CandidateItem(
        val id: Long,
        val name: String,
        val barcode: String? = null
    )

    data class MatchCandidate(
        val itemId: Long,
        val itemName: String,
        val score: Double,              // 0.0 to 1.0
        val confidence: MatchConfidence,
        val method: String              // "barcode", "exact", "tfidf", "containment"
    )

    enum class MatchConfidence {
        DEFINITE, LIKELY, POSSIBLE
    }

    /**
     * Find matching inventory items for the given product name.
     *
     * @param incomingName     The new product name to match
     * @param corpus           All active inventory item names (for IDF computation)
     * @param candidates       Items to match against (id + name + optional barcode)
     * @param incomingBarcode  Optional barcode for Layer 1 matching
     * @param barcodeMatch     Optional pre-resolved barcode match (id + name)
     * @return Matches sorted by confidence (DEFINITE first) then score descending
     */
    fun findMatches(
        incomingName: String,
        corpus: List<String>,
        candidates: List<CandidateItem>,
        incomingBarcode: String? = null,
        barcodeMatch: CandidateItem? = null
    ): List<MatchCandidate> {
        if (incomingName.isBlank() || candidates.isEmpty()) return emptyList()

        val results = mutableListOf<MatchCandidate>()
        val matchedIds = mutableSetOf<Long>()

        // --- Layer 1: Barcode Match ---
        if (barcodeMatch != null) {
            results.add(
                MatchCandidate(
                    itemId = barcodeMatch.id,
                    itemName = barcodeMatch.name,
                    score = 1.0,
                    confidence = MatchConfidence.DEFINITE,
                    method = "barcode"
                )
            )
            matchedIds.add(barcodeMatch.id)
        } else if (!incomingBarcode.isNullOrBlank()) {
            // Check candidates for barcode match
            val barcodeHit = candidates.firstOrNull {
                !it.barcode.isNullOrBlank() && it.barcode == incomingBarcode
            }
            if (barcodeHit != null) {
                results.add(
                    MatchCandidate(
                        itemId = barcodeHit.id,
                        itemName = barcodeHit.name,
                        score = 1.0,
                        confidence = MatchConfidence.DEFINITE,
                        method = "barcode"
                    )
                )
                matchedIds.add(barcodeHit.id)
            }
        }

        // --- Layer 2: Exact Normalized Match ---
        val incomingNormalized = ItemNameNormalizer.normalizeForExactMatch(incomingName)
        if (incomingNormalized.isNotBlank()) {
            for (candidate in candidates) {
                if (candidate.id in matchedIds) continue
                val candidateNormalized = ItemNameNormalizer.normalizeForExactMatch(candidate.name)
                if (incomingNormalized == candidateNormalized) {
                    results.add(
                        MatchCandidate(
                            itemId = candidate.id,
                            itemName = candidate.name,
                            score = 1.0,
                            confidence = MatchConfidence.DEFINITE,
                            method = "exact"
                        )
                    )
                    matchedIds.add(candidate.id)
                }
            }
        }

        // --- Layer 3: TF-IDF Cosine Similarity ---
        val incomingTokens = ItemNameNormalizer.tokenize(incomingName)
        if (incomingTokens.isNotEmpty()) {
            val idfMap = buildIdfMap(corpus)
            val incomingIsMultiToken = incomingTokens.size >= 2

            for (candidate in candidates) {
                if (candidate.id in matchedIds) continue
                val candidateTokens = ItemNameNormalizer.tokenize(candidate.name)
                if (candidateTokens.isEmpty()) continue

                val score = cosineSimilarity(incomingTokens, candidateTokens, idfMap)
                val candidateIsMultiToken = candidateTokens.size >= 2

                when {
                    score > 0.7 && incomingIsMultiToken && candidateIsMultiToken -> {
                        results.add(
                            MatchCandidate(
                                itemId = candidate.id,
                                itemName = candidate.name,
                                score = score,
                                confidence = MatchConfidence.LIKELY,
                                method = "tfidf"
                            )
                        )
                        matchedIds.add(candidate.id)
                    }
                    score >= 0.4 -> {
                        results.add(
                            MatchCandidate(
                                itemId = candidate.id,
                                itemName = candidate.name,
                                score = score,
                                confidence = MatchConfidence.POSSIBLE,
                                method = "tfidf"
                            )
                        )
                        matchedIds.add(candidate.id)
                    }
                }
            }
        }

        // --- Layer 4: Single-Token Containment ---
        if (incomingTokens.size == 1) {
            val singleToken = ItemNameNormalizer.depluralize(incomingTokens[0])
            for (candidate in candidates) {
                if (candidate.id in matchedIds) continue
                val candidateTokens = ItemNameNormalizer.tokenize(candidate.name)
                    .map { ItemNameNormalizer.depluralize(it) }
                if (singleToken in candidateTokens) {
                    results.add(
                        MatchCandidate(
                            itemId = candidate.id,
                            itemName = candidate.name,
                            score = 0.5, // mid-range POSSIBLE
                            confidence = MatchConfidence.POSSIBLE,
                            method = "containment"
                        )
                    )
                    matchedIds.add(candidate.id)
                }
            }
        }

        // Sort: DEFINITE first, then LIKELY, then POSSIBLE; within tier by score descending
        return results.sortedWith(
            compareBy<MatchCandidate> { it.confidence.ordinal }
                .thenByDescending { it.score }
        )
    }

    // ── TF-IDF internals ──

    private fun buildIdfMap(corpus: List<String>): Map<String, Double> {
        val n = corpus.size.coerceAtLeast(1)
        val docFrequency = mutableMapOf<String, Int>()

        for (name in corpus) {
            val tokens = ItemNameNormalizer.tokenize(name)
            val allForms = tokens.flatMap {
                listOf(it, ItemNameNormalizer.depluralize(it))
            }.toSet()
            for (token in allForms) {
                docFrequency[token] = (docFrequency[token] ?: 0) + 1
            }
        }

        return docFrequency.mapValues { (_, df) ->
            ln((n + 1.0) / (df + 1.0)) + 1.0 // smoothed IDF
        }
    }

    private fun cosineSimilarity(
        queryTokens: List<String>,
        candidateTokens: List<String>,
        idfMap: Map<String, Double>
    ): Double {
        val defaultIdf = ln((idfMap.size.coerceAtLeast(1) + 1.0) / 1.0) + 1.0

        fun tfidfVector(tokens: List<String>): Map<String, Double> {
            val tf = tokens.groupingBy { it }.eachCount()
            return tf.mapValues { (token, count) ->
                val idf = idfMap[token]
                    ?: idfMap[ItemNameNormalizer.depluralize(token)]
                    ?: defaultIdf
                count.toDouble() * idf
            }
        }

        val vecA = tfidfVector(queryTokens)
        val vecB = tfidfVector(candidateTokens)

        val allTokens = vecA.keys + vecB.keys
        var dot = 0.0
        var normA = 0.0
        var normB = 0.0
        for (token in allTokens) {
            val a = vecA[token] ?: 0.0
            val b = vecB[token] ?: 0.0
            dot += a * b
            normA += a * a
            normB += b * b
        }

        return if (normA == 0.0 || normB == 0.0) 0.0
        else dot / (sqrt(normA) * sqrt(normB))
    }
}
