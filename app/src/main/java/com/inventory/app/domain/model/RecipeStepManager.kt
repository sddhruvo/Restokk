package com.inventory.app.domain.model

/** Stateless step operations for the RecipeBuilder. Pure functions, no dependencies. */
object RecipeStepManager {

    /** Insert blank step after the given index. Returns new list. */
    fun insertAfter(steps: List<BuilderStep>, afterIndex: Int): List<BuilderStep> {
        val mutable = steps.toMutableList()
        val insertAt = (afterIndex + 1).coerceIn(0, mutable.size)
        mutable.add(insertAt, BuilderStep())
        return mutable
    }

    /** Insert blank step before the given index. Returns new list. */
    fun insertBefore(steps: List<BuilderStep>, beforeIndex: Int): List<BuilderStep> {
        val mutable = steps.toMutableList()
        val insertAt = beforeIndex.coerceIn(0, mutable.size)
        mutable.add(insertAt, BuilderStep())
        return mutable
    }

    /** Delete step at index. Returns new list. Caller must ensure steps.size > 1. */
    fun deleteStep(steps: List<BuilderStep>, atIndex: Int): List<BuilderStep> {
        if (steps.size <= 1) return steps
        val mutable = steps.toMutableList()
        mutable.removeAt(atIndex.coerceIn(0, mutable.lastIndex))
        return mutable
    }

    /** Collect + merge all ingredients across steps. */
    fun collectIngredients(steps: List<BuilderStep>): List<RecipeIngredient> {
        val allIngredients = steps.flatMap { step ->
            step.ingredients.map { si ->
                RecipeIngredient(name = si.name, amount = si.amount, unit = si.unit)
            }
        }
        return mergeIngredients(allIngredients)
    }

    /** Sum all step timers. Returns null if no timers set. */
    fun calculateTotalTime(steps: List<BuilderStep>): Int? {
        val total = steps.mapNotNull { it.timerSeconds }.sum()
        return if (total > 0) total else null
    }

    /** Convert BuilderStep list → RecipeStep list for serialization. */
    fun toRecipeSteps(steps: List<BuilderStep>): List<RecipeStep> {
        return steps.map { bs ->
            RecipeStep(
                instruction = bs.instruction,
                timerSeconds = bs.timerSeconds,
                ingredients = bs.ingredients.map { si ->
                    RecipeIngredient(name = si.name, amount = si.amount, unit = si.unit)
                }
            )
        }
    }

    /** Replace the step at [index] with one or more [replacements]. Returns new list.
     *  Used by structureStep() when AI splits a single step into multiple steps. */
    fun replaceStepWithMany(steps: List<BuilderStep>, index: Int, replacements: List<BuilderStep>): List<BuilderStep> {
        if (index < 0 || index >= steps.size || replacements.isEmpty()) return steps
        val mutable = steps.toMutableList()
        mutable.removeAt(index)
        mutable.addAll(index, replacements)
        return mutable
    }

    /** Convert RecipeStep list → BuilderStep list for edit mode loading. */
    fun fromRecipeSteps(steps: List<RecipeStep>): List<BuilderStep> {
        return steps.map { rs ->
            BuilderStep(
                instruction = rs.instruction,
                timerSeconds = rs.timerSeconds,
                timerAutoDetected = false,
                ingredients = rs.ingredients.map { ri ->
                    StepIngredient(name = ri.name, amount = ri.amount, unit = ri.unit)
                }
            )
        }
    }
}
