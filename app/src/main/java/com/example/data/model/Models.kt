package com.example.data.model

import com.squareup.moshi.JsonClass
import java.util.UUID

enum class StorageLocation(val displayName: String) {
  FRIDGE("Fridge"),
  FREEZER("Freezer"),
  PANTRY("Pantry")
}

enum class IngredientCategory(val displayName: String, val iconName: String) {
  PRODUCE("Produce", "eco"),
  DAIRY_EGGS("Dairy & Eggs", "egg"),
  MEAT_POULTRY("Meat & Poultry", "restaurant"),
  SEAFOOD("Seafood", "set_meal"),
  PANTRY_GRAINS("Grains & Pasta", "grain"),
  BAKERY("Bakery", "bakery_dining"),
  CONDIMENTS_SAUCES("Condiments", "liquor"),
  SPICES_HERBS("Spices & Herbs", "spa"),
  BEVERAGES("Beverages", "local_cafe"),
  SNACKS("Snacks", "cookie"),
  OTHER("Other", "kitchen")
}

@JsonClass(generateAdapter = true)
data class Ingredient(
  val id: String = UUID.randomUUID().toString(),
  val name: String,
  val quantity: Double,
  val unit: String = "pcs",
  val category: String = IngredientCategory.PRODUCE.name,
  val storageLocation: String = StorageLocation.FRIDGE.name,
  val expiryDateMillis: Long? = null,
  val notes: String = "",
  val dietaryTags: List<String> = emptyList(),
  val addedDateMillis: Long = System.currentTimeMillis(),
  val isUsed: Boolean = false
) {
  fun daysUntilExpiry(): Int? {
    if (expiryDateMillis == null) return null
    val diff = expiryDateMillis - System.currentTimeMillis()
    return (diff / (1000 * 60 * 60 * 24)).toInt()
  }

  fun isExpiringSoon(): Boolean {
    val days = daysUntilExpiry() ?: return false
    return days in 0..3
  }

  fun isExpired(): Boolean {
    val days = daysUntilExpiry() ?: return false
    return days < 0
  }
}

@JsonClass(generateAdapter = true)
data class RecipeIngredient(
  val name: String,
  val quantity: Double,
  val unit: String,
  val isAvailableInFridge: Boolean = false,
  val notes: String = ""
)

@JsonClass(generateAdapter = true)
data class NutritionInfo(
  val calories: Int = 0,
  val proteinGrams: Int = 0,
  val carbsGrams: Int = 0,
  val fatGrams: Int = 0,
  val fiberGrams: Int = 0,
  val sugarGrams: Int = 0
)

@JsonClass(generateAdapter = true)
data class Recipe(
  val id: String = UUID.randomUUID().toString(),
  val title: String,
  val description: String,
  val prepTimeMinutes: Int,
  val cookTimeMinutes: Int,
  val totalTimeMinutes: Int = prepTimeMinutes + cookTimeMinutes,
  val difficulty: String = "Easy", // Easy, Medium, Advanced
  val mealType: String = "Dinner", // Breakfast, Lunch, Dinner, Snack, Dessert
  val cuisine: String = "Modern",
  val servings: Int = 2,
  val imageUrl: String = "",
  val isFavorite: Boolean = false,
  val isCooked: Boolean = false,
  val timesCooked: Int = 0,
  val ingredients: List<RecipeIngredient> = emptyList(),
  val instructions: List<String> = emptyList(),
  val stepTimersMinutes: List<Int?> = emptyList(),
  val nutrition: NutritionInfo = NutritionInfo(),
  val substitutions: Map<String, String> = emptyMap(),
  val storageTips: String = "Keep refrigerated in an airtight container for up to 3 days.",
  val allergenWarnings: List<String> = emptyList(),
  val matchPercentage: Int = 100,
  val missingIngredients: List<String> = emptyList()
)

@JsonClass(generateAdapter = true)
data class MealPlanItem(
  val id: String = UUID.randomUUID().toString(),
  val dayOfWeek: String, // Monday, Tuesday, Wednesday, Thursday, Friday, Saturday, Sunday
  val mealType: String,  // Breakfast, Lunch, Dinner, Snack
  val recipeId: String,
  val recipeTitle: String,
  val recipeImageUrl: String = "",
  val servings: Int = 2,
  val isCompleted: Boolean = false
)

@JsonClass(generateAdapter = true)
data class ShoppingItem(
  val id: String = UUID.randomUUID().toString(),
  val name: String,
  val quantity: Double,
  val unit: String = "pcs",
  val category: String = IngredientCategory.PRODUCE.name,
  val isChecked: Boolean = false,
  val sourceRecipeTitle: String? = null
)

enum class SubscriptionTier(
  val tierName: String,
  val priceLabel: String,
  val dailyGenerationsLimit: Int,
  val description: String,
  val perks: List<String>
) {
  FREE(
    tierName = "Free Starter",
    priceLabel = "Free forever",
    dailyGenerationsLimit = 5,
    description = "Essential fridge tracking and daily AI recipes.",
    perks = listOf(
      "5 AI Recipe Generations / day",
      "Camera & Photo Ingredient Scanner",
      "Smart Expiry Warnings",
      "Basic Shopping List"
    )
  ),
  PLUS(
    tierName = "Culinary Plus",
    priceLabel = "$4.99 / month",
    dailyGenerationsLimit = 25,
    description = "For home chefs looking to minimize food waste.",
    perks = listOf(
      "25 AI Recipe Generations / day",
      "Unlimited AI Ingredient Photo Scanning",
      "7-Day Smart Meal Planner",
      "Nutritional Breakdown & Macro Goals",
      "Advanced Dietary & Allergy Filters"
    )
  ),
  PRO(
    tierName = "Chef Master Pro",
    priceLabel = "$9.99 / month",
    dailyGenerationsLimit = 100,
    description = "Unlimited culinary power and multi-household sync.",
    perks = listOf(
      "100 AI Recipe Generations / day",
      "Instant Custom Substitutions Generator",
      "Pantry & Freezer Long-term Tracking",
      "Export Printable Weekly Meal Plans",
      "Priority AI Generation Latency"
    )
  )
}

@JsonClass(generateAdapter = true)
data class UserPreferences(
  val householdSize: Int = 2,
  val dietaryPreference: String = "Omnivore", // Omnivore, Vegetarian, Vegan, Pescatarian, Keto, Low Carb
  val allergiesToAvoid: List<String> = emptyList(), // Gluten, Dairy, Peanuts, Tree Nuts, Shellfish, Soy, Eggs
  val cookingSkill: String = "Intermediate", // Beginner, Intermediate, Advanced
  val maxCookingTimeMinutes: Int = 30,
  val measurementUnit: String = "Metric", // Metric, Imperial
  val hasCompletedOnboarding: Boolean = false,
  val tier: String = SubscriptionTier.FREE.name,
  val remainingAiGenerations: Int = 5,
  val customApiKey: String = ""
)
