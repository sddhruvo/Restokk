package com.inventory.app.domain.model

/**
 * Smart auto-fill defaults for common grocery/household items.
 * When a user types an item name, we look it up here to auto-select
 * category, subcategory, unit, storage location, and typical shelf life.
 */

data class ItemDefaults(
    val category: String,
    val subcategory: String? = null,
    val unit: String? = null,
    val location: String? = null,
    val shelfLifeDays: Int? = null
)

data class CategoryDefaults(
    val location: String? = null,
    val unit: String? = null
)

object SmartDefaults {

    /**
     * Look up an item name and return smart defaults.
     * Uses case-insensitive matching. Tries exact match first,
     * then longest keyword match.
     */
    fun lookup(itemName: String): ItemDefaults? {
        val name = itemName.trim().lowercase()
        if (name.isBlank()) return null

        // Exact match first
        itemMap[name]?.let { return it }

        // Try longest matching keyword (prefer more specific matches)
        var bestMatch: ItemDefaults? = null
        var bestLength = 0
        for ((keyword, defaults) in itemMap) {
            if (name.contains(keyword) && keyword.length > bestLength) {
                bestMatch = defaults
                bestLength = keyword.length
            }
        }
        if (bestMatch != null) return bestMatch

        // Try if any keyword contains the input (for short names like "egg")
        for ((keyword, defaults) in itemMap) {
            if (keyword.contains(name) && name.length >= 3) {
                return defaults
            }
        }

        // Word-level matching: check each word in the name against keywords
        // Handles barcode-scanned names like "Maggi Instant Noodles" → matches "noodles"
        val words = name.split(" ", "-", "_").filter { it.length >= 3 }
        for (word in words) {
            itemMap[word]?.let { return it }
            // Also try without trailing 's' (plural → singular)
            if (word.endsWith("s") && word.length >= 4) {
                itemMap[word.dropLast(1)]?.let { return it }
            }
        }

        return null
    }

    /**
     * Get default location and unit for a category.
     * Used when user manually selects a category.
     */
    fun getCategoryDefaults(categoryName: String): CategoryDefaults? {
        return categoryDefaultsMap[categoryName]
    }

    /**
     * Get item name suggestions matching a query.
     * Returns known item keywords that contain the query string.
     */
    fun suggestNames(query: String, limit: Int = 5): List<String> {
        val q = query.trim().lowercase()
        if (q.length < 2) return emptyList()
        return itemMap.keys
            .filter { it.contains(q) }
            .sortedBy { it.length }
            .take(limit)
            .map { it.replaceFirstChar { c -> c.uppercase() } }
    }

    // ----- Category-level defaults -----

    private val categoryDefaultsMap = mapOf(
        "Dairy & Eggs" to CategoryDefaults(location = "Refrigerator", unit = "pcs"),
        "Meat & Poultry" to CategoryDefaults(location = "Refrigerator", unit = "lb"),
        "Seafood" to CategoryDefaults(location = "Refrigerator", unit = "lb"),
        "Fruits" to CategoryDefaults(location = "Refrigerator", unit = "pcs"),
        "Vegetables" to CategoryDefaults(location = "Refrigerator", unit = "pcs"),
        "Bread & Bakery" to CategoryDefaults(location = "Pantry", unit = "pcs"),
        "Grains & Pasta" to CategoryDefaults(location = "Pantry", unit = "lb"),
        "Canned Goods" to CategoryDefaults(location = "Pantry", unit = "can"),
        "Condiments & Sauces" to CategoryDefaults(location = "Refrigerator", unit = "bottle"),
        "Spices & Seasonings" to CategoryDefaults(location = "Spice Rack", unit = "cont"),
        "Snacks" to CategoryDefaults(location = "Pantry", unit = "bag"),
        "Beverages" to CategoryDefaults(location = "Pantry", unit = "bottle"),
        "Frozen Foods" to CategoryDefaults(location = "Freezer", unit = "box"),
        "Baking Supplies" to CategoryDefaults(location = "Pantry", unit = "bag"),
        "Oils & Vinegars" to CategoryDefaults(location = "Pantry", unit = "bottle"),
        "International Foods" to CategoryDefaults(location = "Pantry", unit = "pcs"),
        "Baby Food" to CategoryDefaults(location = "Pantry", unit = "jar"),
        "Pet Food" to CategoryDefaults(location = "Pantry", unit = "bag"),
        "Other" to CategoryDefaults(location = "Pantry", unit = "pcs")
    )

    // ----- Item keyword → defaults mapping -----

    private val itemMap: Map<String, ItemDefaults> = buildMap {

        // ========== DAIRY & EGGS ==========
        val dairy = "Dairy & Eggs"
        put("whole milk", ItemDefaults(dairy, "Milk", "gal", "Refrigerator", 7))
        put("2% milk", ItemDefaults(dairy, "Milk", "gal", "Refrigerator", 7))
        put("skim milk", ItemDefaults(dairy, "Milk", "gal", "Refrigerator", 7))
        put("milk", ItemDefaults(dairy, "Milk", "gal", "Refrigerator", 7))
        put("half and half", ItemDefaults(dairy, "Cream", "pt", "Refrigerator", 10))
        put("heavy cream", ItemDefaults(dairy, "Cream", "pt", "Refrigerator", 7))
        put("whipping cream", ItemDefaults(dairy, "Cream", "pt", "Refrigerator", 7))
        put("cream", ItemDefaults(dairy, "Cream", "pt", "Refrigerator", 7))
        put("sour cream", ItemDefaults(dairy, "Other Dairy", "cont", "Refrigerator", 14))
        put("cream cheese", ItemDefaults(dairy, "Cheese", "oz", "Refrigerator", 14))
        put("cheddar", ItemDefaults(dairy, "Cheese", "oz", "Refrigerator", 30))
        put("mozzarella", ItemDefaults(dairy, "Cheese", "oz", "Refrigerator", 21))
        put("parmesan", ItemDefaults(dairy, "Cheese", "oz", "Refrigerator", 60))
        put("swiss cheese", ItemDefaults(dairy, "Cheese", "oz", "Refrigerator", 30))
        put("american cheese", ItemDefaults(dairy, "Cheese", "pcs", "Refrigerator", 30))
        put("cottage cheese", ItemDefaults(dairy, "Cheese", "cont", "Refrigerator", 10))
        put("ricotta", ItemDefaults(dairy, "Cheese", "cont", "Refrigerator", 7))
        put("feta", ItemDefaults(dairy, "Cheese", "oz", "Refrigerator", 30))
        put("cheese", ItemDefaults(dairy, "Cheese", "oz", "Refrigerator", 30))
        put("yogurt", ItemDefaults(dairy, "Yogurt", "cont", "Refrigerator", 14))
        put("greek yogurt", ItemDefaults(dairy, "Yogurt", "cont", "Refrigerator", 14))
        put("eggs", ItemDefaults(dairy, "Eggs", "doz", "Refrigerator", 21))
        put("egg", ItemDefaults(dairy, "Eggs", "doz", "Refrigerator", 21))
        put("butter", ItemDefaults(dairy, "Butter", "lb", "Refrigerator", 30))
        put("margarine", ItemDefaults(dairy, "Butter", "cont", "Refrigerator", 60))
        put("ghee", ItemDefaults(dairy, "Butter", "jar", "Pantry", 90))
        put("buttermilk", ItemDefaults(dairy, "Other Dairy", "qt", "Refrigerator", 14))
        put("condensed milk", ItemDefaults(dairy, "Other Dairy", "can", "Pantry", 365))
        put("evaporated milk", ItemDefaults(dairy, "Other Dairy", "can", "Pantry", 365))
        put("whey protein", ItemDefaults(dairy, "Other Dairy", "cont", "Pantry", 365))
        put("paneer", ItemDefaults(dairy, "Cheese", "oz", "Refrigerator", 7))

        // ========== MEAT & POULTRY ==========
        val meat = "Meat & Poultry"
        put("chicken breast", ItemDefaults(meat, "Chicken", "lb", "Refrigerator", 3))
        put("chicken thigh", ItemDefaults(meat, "Chicken", "lb", "Refrigerator", 3))
        put("chicken wing", ItemDefaults(meat, "Chicken", "lb", "Refrigerator", 3))
        put("chicken drumstick", ItemDefaults(meat, "Chicken", "lb", "Refrigerator", 3))
        put("whole chicken", ItemDefaults(meat, "Chicken", "lb", "Refrigerator", 3))
        put("chicken", ItemDefaults(meat, "Chicken", "lb", "Refrigerator", 3))
        put("turkey", ItemDefaults(meat, "Turkey", "lb", "Refrigerator", 3))
        put("turkey breast", ItemDefaults(meat, "Turkey", "lb", "Refrigerator", 3))
        put("ground turkey", ItemDefaults(meat, "Turkey", "lb", "Refrigerator", 2))
        put("ground beef", ItemDefaults(meat, "Ground Meat", "lb", "Refrigerator", 2))
        put("ground meat", ItemDefaults(meat, "Ground Meat", "lb", "Refrigerator", 2))
        put("minced meat", ItemDefaults(meat, "Ground Meat", "lb", "Refrigerator", 2))
        put("beef steak", ItemDefaults(meat, "Beef", "lb", "Refrigerator", 5))
        put("steak", ItemDefaults(meat, "Beef", "lb", "Refrigerator", 5))
        put("beef", ItemDefaults(meat, "Beef", "lb", "Refrigerator", 5))
        put("pork chop", ItemDefaults(meat, "Pork", "lb", "Refrigerator", 5))
        put("pork tenderloin", ItemDefaults(meat, "Pork", "lb", "Refrigerator", 5))
        put("pork belly", ItemDefaults(meat, "Pork", "lb", "Refrigerator", 5))
        put("pork", ItemDefaults(meat, "Pork", "lb", "Refrigerator", 5))
        put("bacon", ItemDefaults(meat, "Pork", "pack", "Refrigerator", 7))
        put("ham", ItemDefaults(meat, "Deli Meat", "lb", "Refrigerator", 7))
        put("lamb", ItemDefaults(meat, "Lamb", "lb", "Refrigerator", 5))
        put("lamb chop", ItemDefaults(meat, "Lamb", "lb", "Refrigerator", 5))
        put("sausage", ItemDefaults(meat, "Sausages", "pack", "Refrigerator", 5))
        put("hot dog", ItemDefaults(meat, "Sausages", "pack", "Refrigerator", 14))
        put("salami", ItemDefaults(meat, "Deli Meat", "oz", "Refrigerator", 14))
        put("pepperoni", ItemDefaults(meat, "Deli Meat", "oz", "Refrigerator", 21))
        put("deli meat", ItemDefaults(meat, "Deli Meat", "oz", "Refrigerator", 7))

        // ========== SEAFOOD ==========
        val seafood = "Seafood"
        put("salmon", ItemDefaults(seafood, "Fresh Fish", "lb", "Refrigerator", 2))
        put("tuna", ItemDefaults(seafood, "Fresh Fish", "lb", "Refrigerator", 2))
        put("tilapia", ItemDefaults(seafood, "Fresh Fish", "lb", "Refrigerator", 2))
        put("cod", ItemDefaults(seafood, "Fresh Fish", "lb", "Refrigerator", 2))
        put("fish", ItemDefaults(seafood, "Fresh Fish", "lb", "Refrigerator", 2))
        put("shrimp", ItemDefaults(seafood, "Shrimp", "lb", "Freezer", 180))
        put("prawns", ItemDefaults(seafood, "Shrimp", "lb", "Freezer", 180))
        put("crab", ItemDefaults(seafood, "Shellfish", "lb", "Refrigerator", 2))
        put("lobster", ItemDefaults(seafood, "Shellfish", "lb", "Refrigerator", 2))
        put("clams", ItemDefaults(seafood, "Shellfish", "lb", "Refrigerator", 2))
        put("mussels", ItemDefaults(seafood, "Shellfish", "lb", "Refrigerator", 2))
        put("oysters", ItemDefaults(seafood, "Shellfish", "doz", "Refrigerator", 7))
        put("frozen fish", ItemDefaults(seafood, "Frozen Fish", "lb", "Freezer", 180))
        put("fish sticks", ItemDefaults(seafood, "Frozen Fish", "box", "Freezer", 180))
        put("canned tuna", ItemDefaults(seafood, "Canned Seafood", "can", "Pantry", 730))
        put("canned salmon", ItemDefaults(seafood, "Canned Seafood", "can", "Pantry", 730))
        put("sardines", ItemDefaults(seafood, "Canned Seafood", "can", "Pantry", 730))
        put("anchovies", ItemDefaults(seafood, "Canned Seafood", "can", "Pantry", 730))

        // ========== FRUITS ==========
        val fruits = "Fruits"
        put("apple", ItemDefaults(fruits, "Apples & Pears", "pcs", "Refrigerator", 30))
        put("apples", ItemDefaults(fruits, "Apples & Pears", "pcs", "Refrigerator", 30))
        put("pear", ItemDefaults(fruits, "Apples & Pears", "pcs", "Refrigerator", 14))
        put("orange", ItemDefaults(fruits, "Citrus", "pcs", "Counter", 14))
        put("oranges", ItemDefaults(fruits, "Citrus", "pcs", "Counter", 14))
        put("lemon", ItemDefaults(fruits, "Citrus", "pcs", "Refrigerator", 21))
        put("lemons", ItemDefaults(fruits, "Citrus", "pcs", "Refrigerator", 21))
        put("lime", ItemDefaults(fruits, "Citrus", "pcs", "Refrigerator", 21))
        put("grapefruit", ItemDefaults(fruits, "Citrus", "pcs", "Refrigerator", 21))
        put("banana", ItemDefaults(fruits, "Tropical", "bunch", "Counter", 5))
        put("bananas", ItemDefaults(fruits, "Tropical", "bunch", "Counter", 5))
        put("mango", ItemDefaults(fruits, "Tropical", "pcs", "Counter", 7))
        put("pineapple", ItemDefaults(fruits, "Tropical", "pcs", "Counter", 5))
        put("papaya", ItemDefaults(fruits, "Tropical", "pcs", "Refrigerator", 7))
        put("coconut", ItemDefaults(fruits, "Tropical", "pcs", "Pantry", 14))
        put("strawberry", ItemDefaults(fruits, "Berries", "pcs", "Refrigerator", 5))
        put("strawberries", ItemDefaults(fruits, "Berries", "cont", "Refrigerator", 5))
        put("blueberries", ItemDefaults(fruits, "Berries", "cont", "Refrigerator", 7))
        put("raspberries", ItemDefaults(fruits, "Berries", "cont", "Refrigerator", 3))
        put("blackberries", ItemDefaults(fruits, "Berries", "cont", "Refrigerator", 3))
        put("grapes", ItemDefaults(fruits, "Berries", "lb", "Refrigerator", 10))
        put("peach", ItemDefaults(fruits, "Stone Fruit", "pcs", "Counter", 5))
        put("peaches", ItemDefaults(fruits, "Stone Fruit", "pcs", "Counter", 5))
        put("plum", ItemDefaults(fruits, "Stone Fruit", "pcs", "Counter", 5))
        put("cherry", ItemDefaults(fruits, "Stone Fruit", "lb", "Refrigerator", 7))
        put("cherries", ItemDefaults(fruits, "Stone Fruit", "lb", "Refrigerator", 7))
        put("avocado", ItemDefaults(fruits, "Tropical", "pcs", "Counter", 5))
        put("watermelon", ItemDefaults(fruits, "Melons", "pcs", "Refrigerator", 7))
        put("cantaloupe", ItemDefaults(fruits, "Melons", "pcs", "Refrigerator", 7))
        put("honeydew", ItemDefaults(fruits, "Melons", "pcs", "Refrigerator", 7))
        put("melon", ItemDefaults(fruits, "Melons", "pcs", "Refrigerator", 7))
        put("raisins", ItemDefaults(fruits, "Dried Fruit", "bag", "Pantry", 180))
        put("dried cranberries", ItemDefaults(fruits, "Dried Fruit", "bag", "Pantry", 180))
        put("dates", ItemDefaults(fruits, "Dried Fruit", "bag", "Pantry", 180))
        put("dried fruit", ItemDefaults(fruits, "Dried Fruit", "bag", "Pantry", 180))
        put("frozen berries", ItemDefaults(fruits, "Frozen Fruit", "bag", "Freezer", 365))
        put("frozen fruit", ItemDefaults(fruits, "Frozen Fruit", "bag", "Freezer", 365))
        put("kiwi", ItemDefaults(fruits, "Tropical", "pcs", "Refrigerator", 14))
        put("pomegranate", ItemDefaults(fruits, "Tropical", "pcs", "Refrigerator", 14))

        // ========== VEGETABLES ==========
        val vegs = "Vegetables"
        put("onion", ItemDefaults(vegs, "Onions & Garlic", "pcs", "Pantry", 30))
        put("onions", ItemDefaults(vegs, "Onions & Garlic", "pcs", "Pantry", 30))
        put("red onion", ItemDefaults(vegs, "Onions & Garlic", "pcs", "Pantry", 30))
        put("green onion", ItemDefaults(vegs, "Onions & Garlic", "bunch", "Refrigerator", 7))
        put("spring onion", ItemDefaults(vegs, "Onions & Garlic", "bunch", "Refrigerator", 7))
        put("shallot", ItemDefaults(vegs, "Onions & Garlic", "pcs", "Pantry", 30))
        put("garlic", ItemDefaults(vegs, "Onions & Garlic", "head", "Pantry", 60))
        put("ginger", ItemDefaults(vegs, "Onions & Garlic", "pcs", "Refrigerator", 21))
        put("potato", ItemDefaults(vegs, "Root Vegetables", "lb", "Pantry", 30))
        put("potatoes", ItemDefaults(vegs, "Root Vegetables", "lb", "Pantry", 30))
        put("sweet potato", ItemDefaults(vegs, "Root Vegetables", "pcs", "Pantry", 21))
        put("carrot", ItemDefaults(vegs, "Root Vegetables", "lb", "Refrigerator", 21))
        put("carrots", ItemDefaults(vegs, "Root Vegetables", "lb", "Refrigerator", 21))
        put("beet", ItemDefaults(vegs, "Root Vegetables", "lb", "Refrigerator", 14))
        put("turnip", ItemDefaults(vegs, "Root Vegetables", "pcs", "Refrigerator", 14))
        put("radish", ItemDefaults(vegs, "Root Vegetables", "bunch", "Refrigerator", 10))
        put("tomato", ItemDefaults(vegs, "Tomatoes", "pcs", "Counter", 7))
        put("tomatoes", ItemDefaults(vegs, "Tomatoes", "pcs", "Counter", 7))
        put("cherry tomatoes", ItemDefaults(vegs, "Tomatoes", "cont", "Counter", 7))
        put("lettuce", ItemDefaults(vegs, "Leafy Greens", "head", "Refrigerator", 7))
        put("spinach", ItemDefaults(vegs, "Leafy Greens", "bag", "Refrigerator", 5))
        put("kale", ItemDefaults(vegs, "Leafy Greens", "bunch", "Refrigerator", 7))
        put("arugula", ItemDefaults(vegs, "Leafy Greens", "bag", "Refrigerator", 5))
        put("cabbage", ItemDefaults(vegs, "Cruciferous", "head", "Refrigerator", 14))
        put("broccoli", ItemDefaults(vegs, "Cruciferous", "head", "Refrigerator", 7))
        put("cauliflower", ItemDefaults(vegs, "Cruciferous", "head", "Refrigerator", 7))
        put("brussels sprouts", ItemDefaults(vegs, "Cruciferous", "lb", "Refrigerator", 7))
        put("bell pepper", ItemDefaults(vegs, "Peppers", "pcs", "Refrigerator", 10))
        put("green pepper", ItemDefaults(vegs, "Peppers", "pcs", "Refrigerator", 10))
        put("red pepper", ItemDefaults(vegs, "Peppers", "pcs", "Refrigerator", 10))
        put("jalapeno", ItemDefaults(vegs, "Peppers", "pcs", "Refrigerator", 10))
        put("chili pepper", ItemDefaults(vegs, "Peppers", "pcs", "Refrigerator", 10))
        put("pepper", ItemDefaults(vegs, "Peppers", "pcs", "Refrigerator", 10))
        put("cucumber", ItemDefaults(vegs, "Other Vegetables", "pcs", "Refrigerator", 7))
        put("zucchini", ItemDefaults(vegs, "Squash", "pcs", "Refrigerator", 7))
        put("squash", ItemDefaults(vegs, "Squash", "pcs", "Refrigerator", 14))
        put("butternut squash", ItemDefaults(vegs, "Squash", "pcs", "Pantry", 30))
        put("pumpkin", ItemDefaults(vegs, "Squash", "pcs", "Pantry", 30))
        put("corn", ItemDefaults(vegs, "Other Vegetables", "pcs", "Refrigerator", 5))
        put("celery", ItemDefaults(vegs, "Other Vegetables", "bunch", "Refrigerator", 14))
        put("asparagus", ItemDefaults(vegs, "Other Vegetables", "bunch", "Refrigerator", 5))
        put("green beans", ItemDefaults(vegs, "Other Vegetables", "lb", "Refrigerator", 7))
        put("peas", ItemDefaults(vegs, "Other Vegetables", "lb", "Refrigerator", 5))
        put("mushroom", ItemDefaults(vegs, "Other Vegetables", "cont", "Refrigerator", 7))
        put("mushrooms", ItemDefaults(vegs, "Other Vegetables", "cont", "Refrigerator", 7))
        put("eggplant", ItemDefaults(vegs, "Other Vegetables", "pcs", "Refrigerator", 7))
        put("okra", ItemDefaults(vegs, "Other Vegetables", "lb", "Refrigerator", 5))
        put("artichoke", ItemDefaults(vegs, "Other Vegetables", "pcs", "Refrigerator", 7))
        put("frozen vegetables", ItemDefaults(vegs, "Frozen Vegetables", "bag", "Freezer", 365))
        put("frozen peas", ItemDefaults(vegs, "Frozen Vegetables", "bag", "Freezer", 365))
        put("frozen corn", ItemDefaults(vegs, "Frozen Vegetables", "bag", "Freezer", 365))
        put("frozen spinach", ItemDefaults(vegs, "Frozen Vegetables", "bag", "Freezer", 365))
        put("mixed vegetables", ItemDefaults(vegs, "Frozen Vegetables", "bag", "Freezer", 365))

        // ========== BREAD & BAKERY ==========
        val bread = "Bread & Bakery"
        put("bread", ItemDefaults(bread, "Sliced Bread", "loaf", "Pantry", 7))
        put("white bread", ItemDefaults(bread, "Sliced Bread", "loaf", "Pantry", 7))
        put("wheat bread", ItemDefaults(bread, "Sliced Bread", "loaf", "Pantry", 7))
        put("sourdough", ItemDefaults(bread, "Sliced Bread", "loaf", "Pantry", 5))
        put("baguette", ItemDefaults(bread, "Rolls & Buns", "pcs", "Pantry", 3))
        put("rolls", ItemDefaults(bread, "Rolls & Buns", "pack", "Pantry", 5))
        put("buns", ItemDefaults(bread, "Rolls & Buns", "pack", "Pantry", 5))
        put("hamburger buns", ItemDefaults(bread, "Rolls & Buns", "pack", "Pantry", 7))
        put("hot dog buns", ItemDefaults(bread, "Rolls & Buns", "pack", "Pantry", 7))
        put("bagel", ItemDefaults(bread, "Bagels", "pack", "Pantry", 5))
        put("bagels", ItemDefaults(bread, "Bagels", "pack", "Pantry", 5))
        put("tortilla", ItemDefaults(bread, "Tortillas", "pack", "Pantry", 14))
        put("tortillas", ItemDefaults(bread, "Tortillas", "pack", "Pantry", 14))
        put("pita", ItemDefaults(bread, "Tortillas", "pack", "Pantry", 7))
        put("naan", ItemDefaults(bread, "Tortillas", "pack", "Pantry", 5))
        put("croissant", ItemDefaults(bread, "Pastries", "pcs", "Pantry", 3))
        put("muffin", ItemDefaults(bread, "Pastries", "pcs", "Pantry", 5))
        put("donut", ItemDefaults(bread, "Pastries", "pcs", "Pantry", 3))
        put("cake", ItemDefaults(bread, "Cakes", "pcs", "Refrigerator", 5))
        put("pie", ItemDefaults(bread, "Other Bakery", "pcs", "Refrigerator", 5))

        // ========== GRAINS & PASTA ==========
        val grains = "Grains & Pasta"
        put("rice", ItemDefaults(grains, "Rice", "lb", "Pantry", 730))
        put("white rice", ItemDefaults(grains, "Rice", "lb", "Pantry", 730))
        put("brown rice", ItemDefaults(grains, "Rice", "lb", "Pantry", 365))
        put("basmati rice", ItemDefaults(grains, "Rice", "lb", "Pantry", 730))
        put("jasmine rice", ItemDefaults(grains, "Rice", "lb", "Pantry", 730))
        put("pasta", ItemDefaults(grains, "Pasta", "lb", "Pantry", 730))
        put("spaghetti", ItemDefaults(grains, "Pasta", "lb", "Pantry", 730))
        put("penne", ItemDefaults(grains, "Pasta", "lb", "Pantry", 730))
        put("macaroni", ItemDefaults(grains, "Pasta", "lb", "Pantry", 730))
        put("noodles", ItemDefaults(grains, "Pasta", "pack", "Pantry", 365))
        put("ramen", ItemDefaults(grains, "Pasta", "pack", "Pantry", 365))
        put("lasagna", ItemDefaults(grains, "Pasta", "box", "Pantry", 730))
        put("cereal", ItemDefaults(grains, "Cereal", "box", "Pantry", 180))
        put("oatmeal", ItemDefaults(grains, "Oatmeal", "cont", "Pantry", 365))
        put("oats", ItemDefaults(grains, "Oatmeal", "cont", "Pantry", 365))
        put("granola", ItemDefaults(grains, "Cereal", "bag", "Pantry", 180))
        put("flour", ItemDefaults(grains, "Flour", "lb", "Pantry", 365))
        put("all purpose flour", ItemDefaults(grains, "Flour", "lb", "Pantry", 365))
        put("whole wheat flour", ItemDefaults(grains, "Flour", "lb", "Pantry", 180))
        put("quinoa", ItemDefaults(grains, "Quinoa", "lb", "Pantry", 730))
        put("couscous", ItemDefaults(grains, "Other Grains", "lb", "Pantry", 365))
        put("barley", ItemDefaults(grains, "Other Grains", "lb", "Pantry", 365))
        put("breadcrumbs", ItemDefaults(grains, "Other Grains", "cont", "Pantry", 180))
        put("cornmeal", ItemDefaults(grains, "Other Grains", "lb", "Pantry", 365))

        // ========== CANNED GOODS ==========
        val canned = "Canned Goods"
        put("canned beans", ItemDefaults(canned, "Canned Beans", "can", "Pantry", 1095))
        put("black beans", ItemDefaults(canned, "Canned Beans", "can", "Pantry", 1095))
        put("kidney beans", ItemDefaults(canned, "Canned Beans", "can", "Pantry", 1095))
        put("chickpeas", ItemDefaults(canned, "Canned Beans", "can", "Pantry", 1095))
        put("baked beans", ItemDefaults(canned, "Canned Beans", "can", "Pantry", 1095))
        put("lentils", ItemDefaults(canned, "Canned Beans", "can", "Pantry", 1095))
        put("canned tomatoes", ItemDefaults(canned, "Canned Tomatoes", "can", "Pantry", 730))
        put("diced tomatoes", ItemDefaults(canned, "Canned Tomatoes", "can", "Pantry", 730))
        put("tomato paste", ItemDefaults(canned, "Canned Tomatoes", "can", "Pantry", 730))
        put("tomato sauce", ItemDefaults(canned, "Canned Tomatoes", "can", "Pantry", 730))
        put("crushed tomatoes", ItemDefaults(canned, "Canned Tomatoes", "can", "Pantry", 730))
        put("canned corn", ItemDefaults(canned, "Canned Vegetables", "can", "Pantry", 1095))
        put("canned peas", ItemDefaults(canned, "Canned Vegetables", "can", "Pantry", 1095))
        put("canned green beans", ItemDefaults(canned, "Canned Vegetables", "can", "Pantry", 1095))
        put("canned fruit", ItemDefaults(canned, "Canned Fruit", "can", "Pantry", 730))
        put("soup", ItemDefaults(canned, "Soups", "can", "Pantry", 730))
        put("chicken soup", ItemDefaults(canned, "Soups", "can", "Pantry", 730))
        put("tomato soup", ItemDefaults(canned, "Soups", "can", "Pantry", 730))
        put("broth", ItemDefaults(canned, "Soups", "carton", "Pantry", 365))
        put("chicken broth", ItemDefaults(canned, "Soups", "carton", "Pantry", 365))
        put("beef broth", ItemDefaults(canned, "Soups", "carton", "Pantry", 365))
        put("vegetable broth", ItemDefaults(canned, "Soups", "carton", "Pantry", 365))
        put("spam", ItemDefaults(canned, "Canned Meat", "can", "Pantry", 1095))
        put("canned chicken", ItemDefaults(canned, "Canned Meat", "can", "Pantry", 1095))
        put("corned beef", ItemDefaults(canned, "Canned Meat", "can", "Pantry", 1095))

        // ========== CONDIMENTS & SAUCES ==========
        val condiments = "Condiments & Sauces"
        put("ketchup", ItemDefaults(condiments, "Ketchup & Mustard", "bottle", "Refrigerator", 180))
        put("mustard", ItemDefaults(condiments, "Ketchup & Mustard", "bottle", "Refrigerator", 365))
        put("mayonnaise", ItemDefaults(condiments, "Mayonnaise", "jar", "Refrigerator", 90))
        put("mayo", ItemDefaults(condiments, "Mayonnaise", "jar", "Refrigerator", 90))
        put("hot sauce", ItemDefaults(condiments, "Hot Sauce", "bottle", "Pantry", 365))
        put("sriracha", ItemDefaults(condiments, "Hot Sauce", "bottle", "Pantry", 365))
        put("tabasco", ItemDefaults(condiments, "Hot Sauce", "bottle", "Pantry", 365))
        put("soy sauce", ItemDefaults(condiments, "Soy Sauce", "bottle", "Pantry", 730))
        put("worcestershire", ItemDefaults(condiments, "Other Sauces", "bottle", "Pantry", 365))
        put("ranch dressing", ItemDefaults(condiments, "Salad Dressing", "bottle", "Refrigerator", 60))
        put("salad dressing", ItemDefaults(condiments, "Salad Dressing", "bottle", "Refrigerator", 60))
        put("vinaigrette", ItemDefaults(condiments, "Salad Dressing", "bottle", "Refrigerator", 90))
        put("pasta sauce", ItemDefaults(condiments, "Pasta Sauce", "jar", "Pantry", 365))
        put("marinara", ItemDefaults(condiments, "Pasta Sauce", "jar", "Pantry", 365))
        put("alfredo sauce", ItemDefaults(condiments, "Pasta Sauce", "jar", "Pantry", 365))
        put("bbq sauce", ItemDefaults(condiments, "BBQ Sauce", "bottle", "Pantry", 365))
        put("barbecue sauce", ItemDefaults(condiments, "BBQ Sauce", "bottle", "Pantry", 365))
        put("teriyaki sauce", ItemDefaults(condiments, "Other Sauces", "bottle", "Pantry", 365))
        put("steak sauce", ItemDefaults(condiments, "Other Sauces", "bottle", "Pantry", 365))
        put("fish sauce", ItemDefaults(condiments, "Other Sauces", "bottle", "Pantry", 730))
        put("oyster sauce", ItemDefaults(condiments, "Other Sauces", "bottle", "Pantry", 365))
        put("hoisin sauce", ItemDefaults(condiments, "Other Sauces", "bottle", "Pantry", 365))
        put("relish", ItemDefaults(condiments, "Other Sauces", "jar", "Refrigerator", 180))
        put("pickle", ItemDefaults(condiments, "Other Sauces", "jar", "Refrigerator", 365))
        put("pickles", ItemDefaults(condiments, "Other Sauces", "jar", "Refrigerator", 365))
        put("salsa", ItemDefaults(condiments, "Other Sauces", "jar", "Refrigerator", 14))
        put("hummus", ItemDefaults(condiments, "Other Sauces", "cont", "Refrigerator", 7))
        put("guacamole", ItemDefaults(condiments, "Other Sauces", "cont", "Refrigerator", 3))
        put("peanut butter", ItemDefaults(condiments, "Other Sauces", "jar", "Pantry", 180))
        put("jam", ItemDefaults(condiments, "Other Sauces", "jar", "Refrigerator", 180))
        put("jelly", ItemDefaults(condiments, "Other Sauces", "jar", "Refrigerator", 180))
        put("honey", ItemDefaults(condiments, "Other Sauces", "bottle", "Pantry", 730))
        put("maple syrup", ItemDefaults(condiments, "Other Sauces", "bottle", "Refrigerator", 365))

        // ========== SPICES & SEASONINGS ==========
        val spices = "Spices & Seasonings"
        put("salt", ItemDefaults(spices, "Salt & Pepper", "cont", "Spice Rack", 1825))
        put("pepper", ItemDefaults(spices, "Salt & Pepper", "cont", "Spice Rack", 730))
        put("black pepper", ItemDefaults(spices, "Salt & Pepper", "cont", "Spice Rack", 730))
        put("sea salt", ItemDefaults(spices, "Salt & Pepper", "cont", "Spice Rack", 1825))
        put("basil", ItemDefaults(spices, "Dried Herbs", "cont", "Spice Rack", 730))
        put("oregano", ItemDefaults(spices, "Dried Herbs", "cont", "Spice Rack", 730))
        put("thyme", ItemDefaults(spices, "Dried Herbs", "cont", "Spice Rack", 730))
        put("rosemary", ItemDefaults(spices, "Dried Herbs", "cont", "Spice Rack", 730))
        put("parsley", ItemDefaults(spices, "Dried Herbs", "cont", "Spice Rack", 730))
        put("bay leaves", ItemDefaults(spices, "Dried Herbs", "cont", "Spice Rack", 730))
        put("dill", ItemDefaults(spices, "Dried Herbs", "cont", "Spice Rack", 730))
        put("cumin", ItemDefaults(spices, "Ground Spices", "cont", "Spice Rack", 730))
        put("paprika", ItemDefaults(spices, "Ground Spices", "cont", "Spice Rack", 730))
        put("chili powder", ItemDefaults(spices, "Ground Spices", "cont", "Spice Rack", 730))
        put("turmeric", ItemDefaults(spices, "Ground Spices", "cont", "Spice Rack", 730))
        put("cinnamon", ItemDefaults(spices, "Ground Spices", "cont", "Spice Rack", 730))
        put("nutmeg", ItemDefaults(spices, "Ground Spices", "cont", "Spice Rack", 730))
        put("garam masala", ItemDefaults(spices, "Spice Blends", "cont", "Spice Rack", 365))
        put("curry powder", ItemDefaults(spices, "Spice Blends", "cont", "Spice Rack", 365))
        put("italian seasoning", ItemDefaults(spices, "Spice Blends", "cont", "Spice Rack", 365))
        put("taco seasoning", ItemDefaults(spices, "Spice Blends", "pack", "Spice Rack", 365))
        put("vanilla extract", ItemDefaults(spices, "Extracts", "bottle", "Spice Rack", 1825))
        put("vanilla", ItemDefaults(spices, "Extracts", "bottle", "Spice Rack", 1825))
        put("garlic powder", ItemDefaults(spices, "Ground Spices", "cont", "Spice Rack", 730))
        put("onion powder", ItemDefaults(spices, "Ground Spices", "cont", "Spice Rack", 730))
        put("cayenne", ItemDefaults(spices, "Ground Spices", "cont", "Spice Rack", 730))
        put("coriander", ItemDefaults(spices, "Ground Spices", "cont", "Spice Rack", 730))
        put("cloves", ItemDefaults(spices, "Ground Spices", "cont", "Spice Rack", 730))
        put("cardamom", ItemDefaults(spices, "Ground Spices", "cont", "Spice Rack", 730))
        put("fennel seeds", ItemDefaults(spices, "Ground Spices", "cont", "Spice Rack", 730))
        put("mustard seeds", ItemDefaults(spices, "Ground Spices", "cont", "Spice Rack", 730))

        // ========== SNACKS ==========
        val snacks = "Snacks"
        put("chips", ItemDefaults(snacks, "Chips", "bag", "Pantry", 60))
        put("potato chips", ItemDefaults(snacks, "Chips", "bag", "Pantry", 60))
        put("tortilla chips", ItemDefaults(snacks, "Chips", "bag", "Pantry", 60))
        put("crackers", ItemDefaults(snacks, "Crackers", "box", "Pantry", 180))
        put("pretzels", ItemDefaults(snacks, "Pretzels", "bag", "Pantry", 180))
        put("popcorn", ItemDefaults(snacks, "Popcorn", "bag", "Pantry", 180))
        put("nuts", ItemDefaults(snacks, "Nuts", "bag", "Pantry", 180))
        put("almonds", ItemDefaults(snacks, "Nuts", "bag", "Pantry", 180))
        put("cashews", ItemDefaults(snacks, "Nuts", "bag", "Pantry", 180))
        put("peanuts", ItemDefaults(snacks, "Nuts", "bag", "Pantry", 180))
        put("walnuts", ItemDefaults(snacks, "Nuts", "bag", "Pantry", 180))
        put("trail mix", ItemDefaults(snacks, "Nuts", "bag", "Pantry", 180))
        put("cookies", ItemDefaults(snacks, "Cookies", "pack", "Pantry", 60))
        put("oreos", ItemDefaults(snacks, "Cookies", "pack", "Pantry", 60))
        put("candy", ItemDefaults(snacks, "Candy", "bag", "Pantry", 365))
        put("chocolate", ItemDefaults(snacks, "Candy", "bag", "Pantry", 180))
        put("granola bar", ItemDefaults(snacks, "Granola Bars", "box", "Pantry", 180))
        put("granola bars", ItemDefaults(snacks, "Granola Bars", "box", "Pantry", 180))
        put("protein bar", ItemDefaults(snacks, "Granola Bars", "box", "Pantry", 180))
        put("jerky", ItemDefaults(snacks, "Other Snacks", "bag", "Pantry", 180))
        put("fruit snacks", ItemDefaults(snacks, "Other Snacks", "box", "Pantry", 180))

        // ========== BEVERAGES ==========
        val beverages = "Beverages"
        put("water", ItemDefaults(beverages, "Water", "bottle", "Pantry", 730))
        put("bottled water", ItemDefaults(beverages, "Water", "pack", "Pantry", 730))
        put("sparkling water", ItemDefaults(beverages, "Water", "can", "Pantry", 365))
        put("orange juice", ItemDefaults(beverages, "Juice", "carton", "Refrigerator", 10))
        put("apple juice", ItemDefaults(beverages, "Juice", "carton", "Refrigerator", 14))
        put("juice", ItemDefaults(beverages, "Juice", "carton", "Refrigerator", 10))
        put("soda", ItemDefaults(beverages, "Soda", "can", "Pantry", 180))
        put("coke", ItemDefaults(beverages, "Soda", "can", "Pantry", 180))
        put("pepsi", ItemDefaults(beverages, "Soda", "can", "Pantry", 180))
        put("sprite", ItemDefaults(beverages, "Soda", "can", "Pantry", 180))
        put("coffee", ItemDefaults(beverages, "Coffee", "bag", "Pantry", 180))
        put("coffee beans", ItemDefaults(beverages, "Coffee", "bag", "Pantry", 60))
        put("ground coffee", ItemDefaults(beverages, "Coffee", "bag", "Pantry", 90))
        put("instant coffee", ItemDefaults(beverages, "Coffee", "jar", "Pantry", 365))
        put("tea", ItemDefaults(beverages, "Tea", "box", "Pantry", 365))
        put("green tea", ItemDefaults(beverages, "Tea", "box", "Pantry", 365))
        put("black tea", ItemDefaults(beverages, "Tea", "box", "Pantry", 365))
        put("herbal tea", ItemDefaults(beverages, "Tea", "box", "Pantry", 365))
        put("sports drink", ItemDefaults(beverages, "Sports Drinks", "bottle", "Pantry", 180))
        put("gatorade", ItemDefaults(beverages, "Sports Drinks", "bottle", "Pantry", 180))
        put("energy drink", ItemDefaults(beverages, "Sports Drinks", "can", "Pantry", 365))
        put("almond milk", ItemDefaults(beverages, "Milk Alternatives", "carton", "Refrigerator", 10))
        put("oat milk", ItemDefaults(beverages, "Milk Alternatives", "carton", "Refrigerator", 10))
        put("soy milk", ItemDefaults(beverages, "Milk Alternatives", "carton", "Refrigerator", 10))
        put("coconut milk", ItemDefaults(beverages, "Milk Alternatives", "can", "Pantry", 365))
        put("beer", ItemDefaults(beverages, "Other Beverages", "can", "Refrigerator", 180))
        put("wine", ItemDefaults(beverages, "Other Beverages", "bottle", "Wine Cooler", 730))
        put("kombucha", ItemDefaults(beverages, "Other Beverages", "bottle", "Refrigerator", 30))
        put("lemonade", ItemDefaults(beverages, "Other Beverages", "carton", "Refrigerator", 10))

        // ========== FROZEN FOODS ==========
        val frozen = "Frozen Foods"
        put("frozen pizza", ItemDefaults(frozen, "Pizza", "box", "Freezer", 180))
        put("pizza", ItemDefaults(frozen, "Pizza", "box", "Freezer", 180))
        put("ice cream", ItemDefaults(frozen, "Ice Cream", "cont", "Freezer", 60))
        put("frozen meal", ItemDefaults(frozen, "Frozen Meals", "box", "Freezer", 180))
        put("tv dinner", ItemDefaults(frozen, "Frozen Meals", "box", "Freezer", 180))
        put("frozen breakfast", ItemDefaults(frozen, "Frozen Breakfast", "box", "Freezer", 180))
        put("frozen waffles", ItemDefaults(frozen, "Frozen Breakfast", "box", "Freezer", 180))
        put("frozen burrito", ItemDefaults(frozen, "Frozen Meals", "pack", "Freezer", 180))
        put("frozen fries", ItemDefaults(frozen, "Other Frozen", "bag", "Freezer", 180))
        put("french fries", ItemDefaults(frozen, "Other Frozen", "bag", "Freezer", 180))
        put("tater tots", ItemDefaults(frozen, "Other Frozen", "bag", "Freezer", 180))
        put("frozen nuggets", ItemDefaults(frozen, "Frozen Appetizers", "bag", "Freezer", 180))
        put("chicken nuggets", ItemDefaults(frozen, "Frozen Appetizers", "bag", "Freezer", 180))
        put("egg rolls", ItemDefaults(frozen, "Frozen Appetizers", "box", "Freezer", 180))
        put("frozen dumplings", ItemDefaults(frozen, "Frozen Appetizers", "bag", "Freezer", 180))
        put("popsicle", ItemDefaults(frozen, "Ice Cream", "box", "Freezer", 180))
        put("frozen yogurt", ItemDefaults(frozen, "Ice Cream", "cont", "Freezer", 60))

        // ========== BAKING SUPPLIES ==========
        val baking = "Baking Supplies"
        put("sugar", ItemDefaults(baking, "Sugar", "lb", "Pantry", 730))
        put("brown sugar", ItemDefaults(baking, "Sugar", "lb", "Pantry", 365))
        put("powdered sugar", ItemDefaults(baking, "Sugar", "lb", "Pantry", 730))
        put("baking powder", ItemDefaults(baking, "Baking Powder", "cont", "Pantry", 365))
        put("baking soda", ItemDefaults(baking, "Baking Powder", "box", "Pantry", 730))
        put("chocolate chips", ItemDefaults(baking, "Chocolate Chips", "bag", "Pantry", 365))
        put("cocoa powder", ItemDefaults(baking, "Chocolate Chips", "cont", "Pantry", 730))
        put("yeast", ItemDefaults(baking, "Yeast", "pack", "Pantry", 365))
        put("cornstarch", ItemDefaults(baking, "Other Baking", "box", "Pantry", 730))
        put("sprinkles", ItemDefaults(baking, "Other Baking", "cont", "Pantry", 365))
        put("food coloring", ItemDefaults(baking, "Other Baking", "bottle", "Pantry", 730))
        put("gelatin", ItemDefaults(baking, "Other Baking", "box", "Pantry", 730))

        // ========== OILS & VINEGARS ==========
        val oils = "Oils & Vinegars"
        put("olive oil", ItemDefaults(oils, "Olive Oil", "bottle", "Pantry", 365))
        put("vegetable oil", ItemDefaults(oils, "Vegetable Oil", "bottle", "Pantry", 365))
        put("canola oil", ItemDefaults(oils, "Vegetable Oil", "bottle", "Pantry", 365))
        put("coconut oil", ItemDefaults(oils, "Coconut Oil", "jar", "Pantry", 730))
        put("sesame oil", ItemDefaults(oils, "Other Oils", "bottle", "Pantry", 365))
        put("avocado oil", ItemDefaults(oils, "Other Oils", "bottle", "Pantry", 365))
        put("vinegar", ItemDefaults(oils, "Vinegars", "bottle", "Pantry", 730))
        put("apple cider vinegar", ItemDefaults(oils, "Vinegars", "bottle", "Pantry", 730))
        put("balsamic vinegar", ItemDefaults(oils, "Vinegars", "bottle", "Pantry", 730))
        put("white vinegar", ItemDefaults(oils, "Vinegars", "bottle", "Pantry", 730))
        put("rice vinegar", ItemDefaults(oils, "Vinegars", "bottle", "Pantry", 730))
        put("cooking spray", ItemDefaults(oils, "Cooking Spray", "can", "Pantry", 730))

        // ========== INTERNATIONAL FOODS ==========
        val international = "International Foods"
        put("tofu", ItemDefaults(international, "Asian", "pack", "Refrigerator", 7))
        put("sushi", ItemDefaults(international, "Asian", "pack", "Refrigerator", 1))
        put("kimchi", ItemDefaults(international, "Asian", "jar", "Refrigerator", 90))
        put("miso", ItemDefaults(international, "Asian", "cont", "Refrigerator", 365))
        put("curry paste", ItemDefaults(international, "Indian", "jar", "Pantry", 365))
        put("coconut cream", ItemDefaults(international, "Asian", "can", "Pantry", 365))
        put("tahini", ItemDefaults(international, "Mediterranean", "jar", "Pantry", 180))
        put("olives", ItemDefaults(international, "Mediterranean", "jar", "Pantry", 365))
        put("capers", ItemDefaults(international, "Mediterranean", "jar", "Refrigerator", 365))
        put("taco shells", ItemDefaults(international, "Mexican", "box", "Pantry", 180))
        put("refried beans", ItemDefaults(international, "Mexican", "can", "Pantry", 730))
        put("enchilada sauce", ItemDefaults(international, "Mexican", "can", "Pantry", 365))
        put("pesto", ItemDefaults(international, "Italian", "jar", "Refrigerator", 14))
        put("sun dried tomatoes", ItemDefaults(international, "Italian", "jar", "Pantry", 365))
        put("artichoke hearts", ItemDefaults(international, "Italian", "jar", "Pantry", 365))

        // ========== BABY FOOD ==========
        val baby = "Baby Food"
        put("baby formula", ItemDefaults(baby, "Baby Formula", "cont", "Pantry", 30))
        put("baby food", ItemDefaults(baby, "Baby Food Jars", "jar", "Pantry", 365))
        put("baby cereal", ItemDefaults(baby, "Baby Cereal", "box", "Pantry", 365))
        put("baby snacks", ItemDefaults(baby, "Baby Snacks", "cont", "Pantry", 180))

        // ========== PET FOOD ==========
        val pet = "Pet Food"
        put("dog food", ItemDefaults(pet, "Dog Food", "bag", "Pantry", 365))
        put("cat food", ItemDefaults(pet, "Cat Food", "can", "Pantry", 730))
        put("dog treats", ItemDefaults(pet, "Pet Treats", "bag", "Pantry", 365))
        put("cat treats", ItemDefaults(pet, "Pet Treats", "bag", "Pantry", 365))
        put("pet food", ItemDefaults(pet, "Dog Food", "bag", "Pantry", 365))
    }
}
