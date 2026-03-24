package com.inventory.app.domain.model

data class ProductMatchResult(
    val matches: List<ProductMatcher.MatchCandidate>,
    val bestMatch: ProductMatcher.MatchCandidate?,
    val suggestedAction: SuggestedAction
)

enum class SuggestedAction {
    UPDATE_EXISTING,   // DEFINITE or LIKELY match found
    CREATE_NEW,        // no matches above POSSIBLE
    ASK_USER           // only POSSIBLE matches — don't auto-decide
}
