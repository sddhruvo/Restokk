package com.inventory.app.domain.model

/** UI/domain model for a step being actively constructed in the Recipe Builder.
 *  Separate from [RecipeStep] (the serialized/stored format). */
data class BuilderStep(
    val instruction: String = "",
    val timerSeconds: Int? = null,
    val timerAutoDetected: Boolean = false,   // true = parsed from text, stops overwriting if user edits
    val ingredients: List<StepIngredient> = emptyList(),
    val captureTimestamp: Long? = null        // capture mode only — wall-clock ms when step was started; stripped on toRecipeSteps()
)

/** A single ingredient entry on a builder step card. */
data class StepIngredient(
    val name: String,
    val amount: String = "",
    val unit: String = ""
)

/** An inventory item offered as a suggestion when typing an ingredient name. */
data class InventorySuggestion(
    val itemId: Long,
    val name: String,
    val unit: String
)
