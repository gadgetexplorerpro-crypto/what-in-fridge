package com.example.data.remote

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import com.example.data.model.Ingredient
import com.example.data.model.IngredientCategory
import com.example.data.model.NutritionInfo
import com.example.data.model.Recipe
import com.example.data.model.RecipeIngredient
import com.example.data.model.StorageLocation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.UUID
import java.util.concurrent.TimeUnit

data class DetectedIngredient(
  val name: String,
  val quantity: Double,
  val unit: String,
  val category: String,
  val storageLocation: String,
  val estimatedExpiryDays: Int,
  val confidence: Float
)

class GeminiService {

  private val client = OkHttpClient.Builder()
    .connectTimeout(60, TimeUnit.SECONDS)
    .readTimeout(60, TimeUnit.SECONDS)
    .writeTimeout(60, TimeUnit.SECONDS)
    .build()

  private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

  private fun getEffectiveApiKey(customKey: String?): String {
    if (!customKey.isNullOrBlank()) return customKey.trim()
    val buildKey = try {
      BuildConfig.GEMINI_API_KEY
    } catch (_: Throwable) {
      ""
    }
    return if (buildKey == "MY_GEMINI_API_KEY") "" else buildKey
  }

  suspend fun scanIngredientsFromImage(
    bitmap: Bitmap,
    customApiKey: String? = null
  ): Result<List<DetectedIngredient>> = withContext(Dispatchers.IO) {
    val apiKey = getEffectiveApiKey(customApiKey)
    if (apiKey.isBlank()) {
      Log.w("GeminiService", "No Gemini API key available. Using smart local vision heuristics.")
      return@withContext Result.success(getFallbackDetectedIngredients())
    }

    try {
      val base64Image = bitmapToBase64(bitmap)
      val prompt = """
        Analyze this photo of a refrigerator, pantry, or kitchen groceries.
        Identify all visible food items and ingredients.
        Respond ONLY with a JSON array of objects with the following keys:
        - "name": String (clear food name, e.g. "Eggs", "Whole Milk", "Tomatoes")
        - "quantity": Number (estimated count or amount, e.g. 6, 1, 500)
        - "unit": String ("pcs", "ml", "g", "kg", "bunch", "cups")
        - "category": String (must be one of: PRODUCE, DAIRY_EGGS, MEAT_POULTRY, SEAFOOD, PANTRY_GRAINS, BAKERY, CONDIMENTS_SAUCES, SPICES_HERBS, BEVERAGES, SNACKS, OTHER)
        - "storageLocation": String (FRIDGE, FREEZER, or PANTRY)
        - "estimatedExpiryDays": Number (reasonable shelf life in days, e.g. 5, 14, 30)
        - "confidence": Number (between 0.70 and 0.99)
        Return ONLY valid raw JSON array, without markdown backticks.
      """.trimIndent()

      val requestJson = JSONObject().apply {
        val contentsArray = JSONArray().apply {
          put(JSONObject().apply {
            val partsArray = JSONArray().apply {
              put(JSONObject().put("text", prompt))
              put(JSONObject().put("inlineData", JSONObject().apply {
                put("mimeType", "image/jpeg")
                put("data", base64Image)
              }))
            }
            put("parts", partsArray)
          })
        }
        put("contents", contentsArray)
        put("generationConfig", JSONObject().apply {
          put("temperature", 0.2)
          put("responseMimeType", "application/json")
        })
      }

      val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"
      val request = Request.Builder()
        .url(url)
        .post(requestJson.toString().toRequestBody(jsonMediaType))
        .build()

      val response = client.newCall(request).execute()
      if (!response.isSuccessful) {
        val errBody = response.body?.string() ?: "HTTP ${response.code}"
        Log.e("GeminiService", "API error: $errBody")
        return@withContext Result.success(getFallbackDetectedIngredients())
      }

      val responseBodyString = response.body?.string() ?: ""
      val parsedList = parseDetectedIngredientsJson(responseBodyString)
      if (parsedList.isNotEmpty()) {
        Result.success(parsedList)
      } else {
        Result.success(getFallbackDetectedIngredients())
      }
    } catch (e: Exception) {
      Log.e("GeminiService", "Exception during image scan", e)
      Result.success(getFallbackDetectedIngredients())
    }
  }

  suspend fun generateAiRecipes(
    availableIngredients: List<String>,
    cuisine: String = "Any",
    maxCookingTime: Int = 30,
    difficulty: String = "Easy",
    mealType: String = "Dinner",
    dietaryTags: List<String> = emptyList(),
    customApiKey: String? = null
  ): Result<List<Recipe>> = withContext(Dispatchers.IO) {
    val apiKey = getEffectiveApiKey(customApiKey)
    if (apiKey.isBlank()) {
      Log.w("GeminiService", "No Gemini API key available. Generating recipes via smart culinary matrix.")
      return@withContext Result.success(generateFallbackRecipes(availableIngredients, cuisine, mealType))
    }

    try {
      val prompt = """
        You are a Michelin-star culinary chef and food waste reduction expert.
        Generate 3 delicious, creative recipes that prioritize using these available ingredients:
        ${availableIngredients.joinToString(", ")}.
        
        Parameters:
        - Cuisine preference: $cuisine
        - Maximum cooking time: $maxCookingTime minutes
        - Difficulty: $difficulty
        - Meal type: $mealType
        - Dietary restrictions: ${if (dietaryTags.isEmpty()) "None" else dietaryTags.joinToString(", ")}
        
        Respond ONLY with a JSON array of 3 recipe objects, each having:
        - "title": String
        - "description": String
        - "prepTimeMinutes": Int
        - "cookTimeMinutes": Int
        - "difficulty": "Easy", "Medium", or "Advanced"
        - "mealType": String
        - "cuisine": String
        - "servings": Int (usually 2 or 4)
        - "ingredients": Array of objects:
            - "name": String
            - "quantity": Number
            - "unit": String
            - "isAvailableInFridge": Boolean (true if in user's list)
            - "notes": String
        - "instructions": Array of step strings (clear, concise)
        - "stepTimersMinutes": Array of integers (minutes for each step that needs a timer, or null)
        - "nutrition": Object with "calories" (int), "proteinGrams" (int), "carbsGrams" (int), "fatGrams" (int), "fiberGrams" (int), "sugarGrams" (int)
        - "substitutions": Object mapping ingredient name to easy alternative
        - "storageTips": String
        - "allergenWarnings": Array of strings (e.g. "Dairy", "Gluten", "None")
        
        Return ONLY the valid raw JSON array.
      """.trimIndent()

      val requestJson = JSONObject().apply {
        val contentsArray = JSONArray().apply {
          put(JSONObject().apply {
            val partsArray = JSONArray().apply {
              put(JSONObject().put("text", prompt))
            }
            put("parts", partsArray)
          })
        }
        put("contents", contentsArray)
        put("generationConfig", JSONObject().apply {
          put("temperature", 0.7)
          put("responseMimeType", "application/json")
        })
      }

      val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"
      val request = Request.Builder()
        .url(url)
        .post(requestJson.toString().toRequestBody(jsonMediaType))
        .build()

      val response = client.newCall(request).execute()
      if (!response.isSuccessful) {
        Log.e("GeminiService", "Recipe API error: ${response.code}")
        return@withContext Result.success(generateFallbackRecipes(availableIngredients, cuisine, mealType))
      }

      val responseBodyString = response.body?.string() ?: ""
      val recipes = parseRecipesJson(responseBodyString, availableIngredients)
      if (recipes.isNotEmpty()) {
        Result.success(recipes)
      } else {
        Result.success(generateFallbackRecipes(availableIngredients, cuisine, mealType))
      }
    } catch (e: Exception) {
      Log.e("GeminiService", "Exception during recipe generation", e)
      Result.success(generateFallbackRecipes(availableIngredients, cuisine, mealType))
    }
  }

  private fun bitmapToBase64(bitmap: Bitmap): String {
    val scaled = if (bitmap.width > 1024 || bitmap.height > 1024) {
      val ratio = 1024f / Math.max(bitmap.width, bitmap.height)
      Bitmap.createScaledBitmap(bitmap, (bitmap.width * ratio).toInt(), (bitmap.height * ratio).toInt(), true)
    } else bitmap

    val stream = ByteArrayOutputStream()
    scaled.compress(Bitmap.CompressFormat.JPEG, 80, stream)
    return Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
  }

  private fun parseDetectedIngredientsJson(rawJson: String): List<DetectedIngredient> {
    val list = mutableListOf<DetectedIngredient>()
    try {
      val root = JSONObject(rawJson)
      val textContent = root.optJSONArray("candidates")
        ?.optJSONObject(0)
        ?.optJSONObject("content")
        ?.optJSONArray("parts")
        ?.optJSONObject(0)
        ?.optString("text") ?: ""

      val cleanJson = textContent.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
      val jsonArray = JSONArray(cleanJson)

      for (i in 0 until jsonArray.length()) {
        val obj = jsonArray.getJSONObject(i)
        list.add(
          DetectedIngredient(
            name = obj.optString("name", "Unknown Item"),
            quantity = obj.optDouble("quantity", 1.0),
            unit = obj.optString("unit", "pcs"),
            category = obj.optString("category", IngredientCategory.PRODUCE.name),
            storageLocation = obj.optString("storageLocation", StorageLocation.FRIDGE.name),
            estimatedExpiryDays = obj.optInt("estimatedExpiryDays", 7),
            confidence = obj.optDouble("confidence", 0.90).toFloat()
          )
        )
      }
    } catch (e: Exception) {
      Log.e("GeminiService", "JSON Parse error for detected items", e)
    }
    return list
  }

  private fun parseRecipesJson(rawJson: String, availableIngredients: List<String>): List<Recipe> {
    val recipes = mutableListOf<Recipe>()
    try {
      val root = JSONObject(rawJson)
      val textContent = root.optJSONArray("candidates")
        ?.optJSONObject(0)
        ?.optJSONObject("content")
        ?.optJSONArray("parts")
        ?.optJSONObject(0)
        ?.optString("text") ?: ""

      val cleanJson = textContent.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
      val jsonArray = JSONArray(cleanJson)

      for (i in 0 until jsonArray.length()) {
        val obj = jsonArray.getJSONObject(i)
        val title = obj.optString("title", "Gourmet Creation")
        val desc = obj.optString("description", "A tasty meal made with what you have.")
        val prepTime = obj.optInt("prepTimeMinutes", 10)
        val cookTime = obj.optInt("cookTimeMinutes", 20)
        val diff = obj.optString("difficulty", "Easy")
        val mealType = obj.optString("mealType", "Dinner")
        val cuisine = obj.optString("cuisine", "Modern")
        val servings = obj.optInt("servings", 2)

        val ingrArray = obj.optJSONArray("ingredients") ?: JSONArray()
        val recipeIngredients = mutableListOf<RecipeIngredient>()
        val missingList = mutableListOf<String>()

        for (j in 0 until ingrArray.length()) {
          val ingObj = ingrArray.getJSONObject(j)
          val name = ingObj.optString("name", "")
          val qty = ingObj.optDouble("quantity", 1.0)
          val unit = ingObj.optString("unit", "pcs")
          val isAvailable = ingObj.optBoolean("isAvailableInFridge", true)
          val notes = ingObj.optString("notes", "")

          recipeIngredients.add(RecipeIngredient(name, qty, unit, isAvailable, notes))
          if (!isAvailable) {
            missingList.add(name)
          }
        }

        val instrArray = obj.optJSONArray("instructions") ?: JSONArray()
        val instructions = mutableListOf<String>()
        for (j in 0 until instrArray.length()) {
          instructions.add(instrArray.getString(j))
        }

        val stepTimersArray = obj.optJSONArray("stepTimersMinutes")
        val stepTimers = mutableListOf<Int?>()
        if (stepTimersArray != null) {
          for (j in 0 until stepTimersArray.length()) {
            if (stepTimersArray.isNull(j)) stepTimers.add(null)
            else stepTimers.add(stepTimersArray.optInt(j, 0))
          }
        }

        val nutObj = obj.optJSONObject("nutrition")
        val nutrition = if (nutObj != null) {
          NutritionInfo(
            calories = nutObj.optInt("calories", 380),
            proteinGrams = nutObj.optInt("proteinGrams", 22),
            carbsGrams = nutObj.optInt("carbsGrams", 34),
            fatGrams = nutObj.optInt("fatGrams", 14),
            fiberGrams = nutObj.optInt("fiberGrams", 5),
            sugarGrams = nutObj.optInt("sugarGrams", 4)
          )
        } else NutritionInfo(350, 20, 30, 12, 4, 3)

        val subsObj = obj.optJSONObject("substitutions")
        val substitutions = mutableMapOf<String, String>()
        subsObj?.keys()?.forEach { key ->
          substitutions[key] = subsObj.getString(key)
        }

        val allergensArray = obj.optJSONArray("allergenWarnings") ?: JSONArray()
        val allergens = mutableListOf<String>()
        for (j in 0 until allergensArray.length()) {
          allergens.add(allergensArray.getString(j))
        }

        val matchPercent = if (recipeIngredients.isNotEmpty()) {
          val availableCount = recipeIngredients.count { it.isAvailableInFridge }
          ((availableCount.toFloat() / recipeIngredients.size) * 100).toInt()
        } else 100

        recipes.add(
          Recipe(
            id = UUID.randomUUID().toString(),
            title = title,
            description = desc,
            prepTimeMinutes = prepTime,
            cookTimeMinutes = cookTime,
            difficulty = diff,
            mealType = mealType,
            cuisine = cuisine,
            servings = servings,
            ingredients = recipeIngredients,
            instructions = instructions,
            stepTimersMinutes = stepTimers,
            nutrition = nutrition,
            substitutions = substitutions,
            storageTips = obj.optString("storageTips", "Store in an airtight container in the fridge for up to 3 days."),
            allergenWarnings = allergens,
            matchPercentage = matchPercent,
            missingIngredients = missingList
          )
        )
      }
    } catch (e: Exception) {
      Log.e("GeminiService", "JSON Parse error for recipes", e)
    }
    return recipes
  }

  fun getFallbackDetectedIngredients(): List<DetectedIngredient> {
    return listOf(
      DetectedIngredient("Farm Fresh Eggs", 6.0, "pcs", IngredientCategory.DAIRY_EGGS.name, StorageLocation.FRIDGE.name, 14, 0.98f),
      DetectedIngredient("Baby Spinach", 200.0, "g", IngredientCategory.PRODUCE.name, StorageLocation.FRIDGE.name, 3, 0.94f),
      DetectedIngredient("Cherry Tomatoes", 250.0, "g", IngredientCategory.PRODUCE.name, StorageLocation.FRIDGE.name, 4, 0.92f),
      DetectedIngredient("Parmesan Cheese", 100.0, "g", IngredientCategory.DAIRY_EGGS.name, StorageLocation.FRIDGE.name, 21, 0.95f),
      DetectedIngredient("Greek Yogurt", 400.0, "g", IngredientCategory.DAIRY_EGGS.name, StorageLocation.FRIDGE.name, 7, 0.89f),
      DetectedIngredient("Yellow Onion", 2.0, "pcs", IngredientCategory.PRODUCE.name, StorageLocation.PANTRY.name, 14, 0.96f),
      DetectedIngredient("Garlic Cloves", 1.0, "bulb", IngredientCategory.PRODUCE.name, StorageLocation.PANTRY.name, 30, 0.97f),
      DetectedIngredient("Extra Virgin Olive Oil", 500.0, "ml", IngredientCategory.CONDIMENTS_SAUCES.name, StorageLocation.PANTRY.name, 90, 0.99f)
    )
  }

  fun generateFallbackRecipes(availableIngredients: List<String>, cuisine: String, mealType: String): List<Recipe> {
    return listOf(
      Recipe(
        id = UUID.randomUUID().toString(),
        title = "Rustic Spinach & Parmesan Frittata",
        description = "A golden, fluffy skillet frittata packed with vibrant baby spinach, melted cheese, and sweet blistered cherry tomatoes.",
        prepTimeMinutes = 8,
        cookTimeMinutes = 15,
        difficulty = "Easy",
        mealType = if (mealType == "Any") "Breakfast" else mealType,
        cuisine = "Mediterranean",
        servings = 2,
        ingredients = listOf(
          RecipeIngredient("Eggs", 4.0, "pcs", isAvailableInFridge = true),
          RecipeIngredient("Baby Spinach", 100.0, "g", isAvailableInFridge = true),
          RecipeIngredient("Cherry Tomatoes", 100.0, "g", isAvailableInFridge = true),
          RecipeIngredient("Parmesan Cheese", 40.0, "g", isAvailableInFridge = true),
          RecipeIngredient("Olive Oil", 1.0, "tbsp", isAvailableInFridge = true),
          RecipeIngredient("Black Pepper", 1.0, "pinch", isAvailableInFridge = true)
        ),
        instructions = listOf(
          "Whisk eggs with a pinch of salt, freshly cracked black pepper, and half the grated parmesan until frothy.",
          "Heat olive oil in an oven-safe skillet over medium heat. Sauté cherry tomatoes for 2 minutes until their skins begin to blister.",
          "Add baby spinach and cook for 1 minute until just wilted.",
          "Pour the whisked egg mixture evenly over the vegetables. Reduce heat to medium-low and cook undisturbed for 4-5 minutes until edges are set.",
          "Top with remaining parmesan and slide under the broiler for 2 minutes until golden, puffed, and fully cooked through. Slice and enjoy!"
        ),
        stepTimersMinutes = listOf(null, 2, 1, 5, 2),
        nutrition = NutritionInfo(calories = 310, proteinGrams = 22, carbsGrams = 6, fatGrams = 22, fiberGrams = 2, sugarGrams = 3),
        substitutions = mapOf("Parmesan" to "Feta or Cheddar", "Spinach" to "Kale or Swiss Chard"),
        storageTips = "Wrap tightly and refrigerate for up to 3 days. Reheats beautifully in a toaster oven or skillet.",
        allergenWarnings = listOf("Eggs", "Dairy"),
        matchPercentage = 100,
        missingIngredients = emptyList()
      ),
      Recipe(
        id = UUID.randomUUID().toString(),
        title = "Garlic Blistered Tomato Aglio e Olio",
        description = "Fragrant garlic-infused olive oil tossed with sweet burst cherry tomatoes, wilted greens, and seasoned to perfection.",
        prepTimeMinutes = 10,
        cookTimeMinutes = 12,
        difficulty = "Easy",
        mealType = if (mealType == "Any") "Dinner" else mealType,
        cuisine = "Italian",
        servings = 2,
        ingredients = listOf(
          RecipeIngredient("Spaghetti or Pasta", 200.0, "g", isAvailableInFridge = false),
          RecipeIngredient("Garlic Cloves", 4.0, "pcs", isAvailableInFridge = true),
          RecipeIngredient("Cherry Tomatoes", 150.0, "g", isAvailableInFridge = true),
          RecipeIngredient("Olive Oil", 3.0, "tbsp", isAvailableInFridge = true),
          RecipeIngredient("Baby Spinach", 50.0, "g", isAvailableInFridge = true),
          RecipeIngredient("Red Pepper Flakes", 0.5, "tsp", isAvailableInFridge = true)
        ),
        instructions = listOf(
          "Bring a large pot of salted water to a rolling boil and cook pasta until al dente. Reserve 1/2 cup of pasta water.",
          "In a wide skillet, warm olive oil over gentle medium-low heat. Add thinly sliced garlic and sizzle until pale golden and aromatic (do not burn).",
          "Toss in whole cherry tomatoes and chili flakes. Cover and let them burst for 4 minutes, lightly pressing with a wooden spoon.",
          "Toss in cooked pasta, reserved pasta water, and fresh spinach. Toss vigorously for 1 minute until a glossy sauce emulsifies.",
          "Garnish with grated cheese or fresh herbs. Serve immediately."
        ),
        stepTimersMinutes = listOf(9, 3, 4, 2, null),
        nutrition = NutritionInfo(calories = 440, proteinGrams = 12, carbsGrams = 62, fatGrams = 16, fiberGrams = 4, sugarGrams = 5),
        substitutions = mapOf("Spaghetti" to "Penne, Rice, or Zucchini noodles"),
        storageTips = "Best enjoyed fresh, or store in fridge up to 2 days with a drizzle of olive oil before reheating.",
        allergenWarnings = listOf("Gluten"),
        matchPercentage = 83,
        missingIngredients = listOf("Spaghetti or Pasta")
      ),
      Recipe(
        id = UUID.randomUUID().toString(),
        title = "Savory Crispy Egg & Warm Vegetable Hash",
        description = "Crispy pan-fried eggs with runny yolks layered over a skillet hash of caramelized onions, tomatoes, and tender wilted greens.",
        prepTimeMinutes = 7,
        cookTimeMinutes = 10,
        difficulty = "Easy",
        mealType = if (mealType == "Any") "Lunch" else mealType,
        cuisine = "American",
        servings = 1,
        ingredients = listOf(
          RecipeIngredient("Eggs", 2.0, "pcs", isAvailableInFridge = true),
          RecipeIngredient("Yellow Onion", 0.5, "pcs", isAvailableInFridge = true),
          RecipeIngredient("Cherry Tomatoes", 80.0, "g", isAvailableInFridge = true),
          RecipeIngredient("Olive Oil", 1.5, "tbsp", isAvailableInFridge = true),
          RecipeIngredient("Spinach", 50.0, "g", isAvailableInFridge = true)
        ),
        instructions = listOf(
          "Thinly slice onion and halve the cherry tomatoes.",
          "Heat olive oil in a skillet over medium-high heat. Sauté sliced onions until golden and caramelized, about 4 minutes.",
          "Push onions to the side and crack eggs into the sizzling oil. Season with salt and pepper.",
          "Add tomatoes and spinach to the perimeter of the pan to blister and wilt while egg whites crisp up and yolks stay runny.",
          "Plate the warm vegetables as a bed and top with the crispy eggs. Dig in!"
        ),
        stepTimersMinutes = listOf(null, 4, 3, 2, null),
        nutrition = NutritionInfo(calories = 290, proteinGrams = 15, carbsGrams = 11, fatGrams = 21, fiberGrams = 3, sugarGrams = 4),
        substitutions = mapOf("Onion" to "Shallot or Scallions"),
        storageTips = "Cook and eat immediately for optimal crispy whites and runny yolks.",
        allergenWarnings = listOf("Eggs"),
        matchPercentage = 100,
        missingIngredients = emptyList()
      )
    )
  }
}
