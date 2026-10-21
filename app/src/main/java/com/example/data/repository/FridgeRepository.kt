package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.model.Ingredient
import com.example.data.model.IngredientCategory
import com.example.data.model.MealPlanItem
import com.example.data.model.NutritionInfo
import com.example.data.model.Recipe
import com.example.data.model.RecipeIngredient
import com.example.data.model.ShoppingItem
import com.example.data.model.StorageLocation
import com.example.data.model.SubscriptionTier
import com.example.data.model.UserPreferences
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

class FridgeRepository(private val context: Context) {

  private val moshi = Moshi.Builder()
    .addLast(KotlinJsonAdapterFactory())
    .build()

  private val scope = CoroutineScope(Dispatchers.IO)

  private val _ingredients = MutableStateFlow<List<Ingredient>>(emptyList())
  val ingredients: StateFlow<List<Ingredient>> = _ingredients.asStateFlow()

  private val _recipes = MutableStateFlow<List<Recipe>>(emptyList())
  val recipes: StateFlow<List<Recipe>> = _recipes.asStateFlow()

  private val _mealPlan = MutableStateFlow<List<MealPlanItem>>(emptyList())
  val mealPlan: StateFlow<List<MealPlanItem>> = _mealPlan.asStateFlow()

  private val _shoppingList = MutableStateFlow<List<ShoppingItem>>(emptyList())
  val shoppingList: StateFlow<List<ShoppingItem>> = _shoppingList.asStateFlow()

  private val _userPreferences = MutableStateFlow(UserPreferences())
  val userPreferences: StateFlow<UserPreferences> = _userPreferences.asStateFlow()

  init {
    loadAllData()
  }

  private fun loadAllData() {
    loadIngredients()
    loadRecipes()
    loadMealPlan()
    loadShoppingList()
    loadPreferences()
  }

  // --- INGREDIENTS ---
  private fun loadIngredients() {
    val file = File(context.filesDir, "ingredients.json")
    if (file.exists()) {
      try {
        val json = file.readText()
        val type = Types.newParameterizedType(List::class.java, Ingredient::class.java)
        val adapter = moshi.adapter<List<Ingredient>>(type)
        val loaded = adapter.fromJson(json)
        if (!loaded.isNullOrEmpty()) {
          _ingredients.value = loaded
          return
        }
      } catch (e: Exception) {
        Log.e("FridgeRepository", "Error loading ingredients", e)
      }
    }
    // Default initial seed
    val defaultList = createInitialIngredients()
    _ingredients.value = defaultList
    saveIngredients(defaultList)
  }

  private fun saveIngredients(list: List<Ingredient>) {
    scope.launch {
      try {
        val file = File(context.filesDir, "ingredients.json")
        val type = Types.newParameterizedType(List::class.java, Ingredient::class.java)
        val adapter = moshi.adapter<List<Ingredient>>(type)
        file.writeText(adapter.toJson(list))
      } catch (e: Exception) {
        Log.e("FridgeRepository", "Error saving ingredients", e)
      }
    }
  }

  fun addIngredient(ingredient: Ingredient) {
    val current = _ingredients.value.toMutableList()
    // check if item with same name exists in same storage
    val index = current.indexOfFirst {
      it.name.equals(ingredient.name.trim(), ignoreCase = true) &&
          it.storageLocation.equals(ingredient.storageLocation, ignoreCase = true)
    }
    if (index >= 0) {
      val existing = current[index]
      current[index] = existing.copy(quantity = existing.quantity + ingredient.quantity)
    } else {
      current.add(0, ingredient)
    }
    _ingredients.value = current
    saveIngredients(current)
    recalculateRecipeMatches()
  }

  fun addMultipleIngredients(items: List<Ingredient>) {
    val current = _ingredients.value.toMutableList()
    items.forEach { ing ->
      val index = current.indexOfFirst {
        it.name.equals(ing.name.trim(), ignoreCase = true) &&
            it.storageLocation.equals(ing.storageLocation, ignoreCase = true)
      }
      if (index >= 0) {
        val existing = current[index]
        current[index] = existing.copy(quantity = existing.quantity + ing.quantity)
      } else {
        current.add(0, ing)
      }
    }
    _ingredients.value = current
    saveIngredients(current)
    recalculateRecipeMatches()
  }

  fun updateIngredient(ingredient: Ingredient) {
    val current = _ingredients.value.toMutableList()
    val index = current.indexOfFirst { it.id == ingredient.id }
    if (index >= 0) {
      current[index] = ingredient
      _ingredients.value = current
      saveIngredients(current)
      recalculateRecipeMatches()
    }
  }

  fun deleteIngredient(id: String) {
    val current = _ingredients.value.filter { it.id != id }
    _ingredients.value = current
    saveIngredients(current)
    recalculateRecipeMatches()
  }

  fun adjustQuantity(id: String, delta: Double) {
    val current = _ingredients.value.toMutableList()
    val index = current.indexOfFirst { it.id == id }
    if (index >= 0) {
      val existing = current[index]
      val newQty = (existing.quantity + delta).coerceAtLeast(0.0)
      if (newQty == 0.0) {
        current.removeAt(index)
      } else {
        current[index] = existing.copy(quantity = newQty)
      }
      _ingredients.value = current
      saveIngredients(current)
      recalculateRecipeMatches()
    }
  }

  fun markIngredientUsed(id: String, isUsed: Boolean) {
    val current = _ingredients.value.toMutableList()
    val index = current.indexOfFirst { it.id == id }
    if (index >= 0) {
      current[index] = current[index].copy(isUsed = isUsed)
      _ingredients.value = current
      saveIngredients(current)
    }
  }

  // --- RECIPES ---
  private fun loadRecipes() {
    val file = File(context.filesDir, "recipes.json")
    if (file.exists()) {
      try {
        val json = file.readText()
        val type = Types.newParameterizedType(List::class.java, Recipe::class.java)
        val adapter = moshi.adapter<List<Recipe>>(type)
        val loaded = adapter.fromJson(json)
        if (!loaded.isNullOrEmpty()) {
          _recipes.value = loaded
          recalculateRecipeMatches()
          return
        }
      } catch (e: Exception) {
        Log.e("FridgeRepository", "Error loading recipes", e)
      }
    }
    val defaultList = createInitialRecipes()
    _recipes.value = defaultList
    saveRecipes(defaultList)
    recalculateRecipeMatches()
  }

  private fun saveRecipes(list: List<Recipe>) {
    scope.launch {
      try {
        val file = File(context.filesDir, "recipes.json")
        val type = Types.newParameterizedType(List::class.java, Recipe::class.java)
        val adapter = moshi.adapter<List<Recipe>>(type)
        file.writeText(adapter.toJson(list))
      } catch (e: Exception) {
        Log.e("FridgeRepository", "Error saving recipes", e)
      }
    }
  }

  fun toggleFavoriteRecipe(id: String) {
    val current = _recipes.value.toMutableList()
    val index = current.indexOfFirst { it.id == id }
    if (index >= 0) {
      val existing = current[index]
      current[index] = existing.copy(isFavorite = !existing.isFavorite)
      _recipes.value = current
      saveRecipes(current)
    }
  }

  fun markRecipeCooked(id: String) {
    val current = _recipes.value.toMutableList()
    val index = current.indexOfFirst { it.id == id }
    if (index >= 0) {
      val existing = current[index]
      current[index] = existing.copy(
        isCooked = true,
        timesCooked = existing.timesCooked + 1
      )
      _recipes.value = current
      saveRecipes(current)

      // Optionally deduct or mark ingredients
      existing.ingredients.filter { it.isAvailableInFridge }.forEach { recipeIng ->
        val ingMatch = _ingredients.value.firstOrNull {
          it.name.contains(recipeIng.name, ignoreCase = true) ||
              recipeIng.name.contains(it.name, ignoreCase = true)
        }
        if (ingMatch != null) {
          adjustQuantity(ingMatch.id, -recipeIng.quantity)
        }
      }
    }
  }

  fun saveGeneratedRecipes(newRecipes: List<Recipe>) {
    val current = _recipes.value.toMutableList()
    newRecipes.forEach { newR ->
      if (current.none { it.title.equals(newR.title, ignoreCase = true) }) {
        current.add(0, newR)
      }
    }
    _recipes.value = current
    saveRecipes(current)
    recalculateRecipeMatches()
  }

  private fun recalculateRecipeMatches() {
    val availableNames = _ingredients.value
      .filter { !it.isUsed && it.quantity > 0 }
      .map { it.name.lowercase().trim() }

    val updatedRecipes = _recipes.value.map { recipe ->
      var availableCount = 0
      val missing = mutableListOf<String>()
      val updatedIngList = recipe.ingredients.map { ing ->
        val hasMatch = availableNames.any { avail ->
          avail.contains(ing.name.lowercase()) || ing.name.lowercase().contains(avail)
        }
        if (hasMatch) {
          availableCount++
          ing.copy(isAvailableInFridge = true)
        } else {
          missing.add(ing.name)
          ing.copy(isAvailableInFridge = false)
        }
      }

      val match = if (recipe.ingredients.isNotEmpty()) {
        ((availableCount.toFloat() / recipe.ingredients.size) * 100).toInt()
      } else 100

      recipe.copy(
        ingredients = updatedIngList,
        matchPercentage = match,
        missingIngredients = missing
      )
    }
    _recipes.value = updatedRecipes
  }

  // --- MEAL PLAN ---
  private fun loadMealPlan() {
    val file = File(context.filesDir, "meal_plan.json")
    if (file.exists()) {
      try {
        val json = file.readText()
        val type = Types.newParameterizedType(List::class.java, MealPlanItem::class.java)
        val adapter = moshi.adapter<List<MealPlanItem>>(type)
        val loaded = adapter.fromJson(json)
        if (!loaded.isNullOrEmpty()) {
          _mealPlan.value = loaded
          return
        }
      } catch (e: Exception) {
        Log.e("FridgeRepository", "Error loading meal plan", e)
      }
    }
    val defaultPlan = createInitialMealPlan()
    _mealPlan.value = defaultPlan
    saveMealPlan(defaultPlan)
  }

  private fun saveMealPlan(list: List<MealPlanItem>) {
    scope.launch {
      try {
        val file = File(context.filesDir, "meal_plan.json")
        val type = Types.newParameterizedType(List::class.java, MealPlanItem::class.java)
        val adapter = moshi.adapter<List<MealPlanItem>>(type)
        file.writeText(adapter.toJson(list))
      } catch (e: Exception) {
        Log.e("FridgeRepository", "Error saving meal plan", e)
      }
    }
  }

  fun setMealPlanItem(dayOfWeek: String, mealType: String, recipe: Recipe) {
    val current = _mealPlan.value.toMutableList()
    val index = current.indexOfFirst { it.dayOfWeek.equals(dayOfWeek, ignoreCase = true) && it.mealType.equals(mealType, ignoreCase = true) }
    val newItem = MealPlanItem(
      id = UUID.randomUUID().toString(),
      dayOfWeek = dayOfWeek,
      mealType = mealType,
      recipeId = recipe.id,
      recipeTitle = recipe.title,
      recipeImageUrl = recipe.imageUrl,
      servings = recipe.servings,
      isCompleted = false
    )
    if (index >= 0) {
      current[index] = newItem
    } else {
      current.add(newItem)
    }
    _mealPlan.value = current
    saveMealPlan(current)
  }

  fun removeMealPlanItem(id: String) {
    val current = _mealPlan.value.filter { it.id != id }
    _mealPlan.value = current
    saveMealPlan(current)
  }

  fun toggleMealPlanCompleted(id: String) {
    val current = _mealPlan.value.toMutableList()
    val index = current.indexOfFirst { it.id == id }
    if (index >= 0) {
      current[index] = current[index].copy(isCompleted = !current[index].isCompleted)
      _mealPlan.value = current
      saveMealPlan(current)
    }
  }

  // --- SHOPPING LIST ---
  private fun loadShoppingList() {
    val file = File(context.filesDir, "shopping_list.json")
    if (file.exists()) {
      try {
        val json = file.readText()
        val type = Types.newParameterizedType(List::class.java, ShoppingItem::class.java)
        val adapter = moshi.adapter<List<ShoppingItem>>(type)
        val loaded = adapter.fromJson(json)
        if (!loaded.isNullOrEmpty()) {
          _shoppingList.value = loaded
          return
        }
      } catch (e: Exception) {
        Log.e("FridgeRepository", "Error loading shopping list", e)
      }
    }
    val defaultShopping = listOf(
      ShoppingItem(name = "Fresh Basil", quantity = 1.0, unit = "bunch", category = IngredientCategory.PRODUCE.name),
      ShoppingItem(name = "Spaghetti", quantity = 500.0, unit = "g", category = IngredientCategory.PANTRY_GRAINS.name),
      ShoppingItem(name = "Butter", quantity = 250.0, unit = "g", category = IngredientCategory.DAIRY_EGGS.name)
    )
    _shoppingList.value = defaultShopping
    saveShoppingList(defaultShopping)
  }

  private fun saveShoppingList(list: List<ShoppingItem>) {
    scope.launch {
      try {
        val file = File(context.filesDir, "shopping_list.json")
        val type = Types.newParameterizedType(List::class.java, ShoppingItem::class.java)
        val adapter = moshi.adapter<List<ShoppingItem>>(type)
        file.writeText(adapter.toJson(list))
      } catch (e: Exception) {
        Log.e("FridgeRepository", "Error saving shopping list", e)
      }
    }
  }

  fun addShoppingItem(item: ShoppingItem) {
    val current = _shoppingList.value.toMutableList()
    val index = current.indexOfFirst { it.name.equals(item.name.trim(), ignoreCase = true) }
    if (index >= 0) {
      val existing = current[index]
      current[index] = existing.copy(quantity = existing.quantity + item.quantity)
    } else {
      current.add(0, item)
    }
    _shoppingList.value = current
    saveShoppingList(current)
  }

  fun toggleShoppingItem(id: String) {
    val current = _shoppingList.value.toMutableList()
    val index = current.indexOfFirst { it.id == id }
    if (index >= 0) {
      current[index] = current[index].copy(isChecked = !current[index].isChecked)
      _shoppingList.value = current
      saveShoppingList(current)
    }
  }

  fun deleteShoppingItem(id: String) {
    val current = _shoppingList.value.filter { it.id != id }
    _shoppingList.value = current
    saveShoppingList(current)
  }

  fun clearCheckedShoppingItems() {
    val current = _shoppingList.value.filter { !it.isChecked }
    _shoppingList.value = current
    saveShoppingList(current)
  }

  fun moveBoughtItemsToFridge(): Int {
    val checked = _shoppingList.value.filter { it.isChecked }
    if (checked.isEmpty()) return 0

    val newIngredients = checked.map { item ->
      Ingredient(
        name = item.name,
        quantity = item.quantity,
        unit = item.unit,
        category = item.category,
        storageLocation = StorageLocation.FRIDGE.name,
        expiryDateMillis = System.currentTimeMillis() + (7L * 24 * 60 * 60 * 1000)
      )
    }
    addMultipleIngredients(newIngredients)
    clearCheckedShoppingItems()
    return checked.size
  }

  fun addMissingRecipeIngredientsToShoppingList(recipe: Recipe): Int {
    val missing = recipe.ingredients.filter { !it.isAvailableInFridge }
    if (missing.isEmpty()) return 0

    missing.forEach { ing ->
      addShoppingItem(
        ShoppingItem(
          name = ing.name,
          quantity = ing.quantity,
          unit = ing.unit,
          category = IngredientCategory.PRODUCE.name,
          sourceRecipeTitle = recipe.title
        )
      )
    }
    return missing.size
  }

  fun generateShoppingListFromMealPlan(): Int {
    var addedCount = 0
    val activeRecipes = _mealPlan.value
      .filter { !it.isCompleted }
      .mapNotNull { planItem -> _recipes.value.firstOrNull { it.id == planItem.recipeId } }

    activeRecipes.forEach { recipe ->
      recipe.ingredients.filter { !it.isAvailableInFridge }.forEach { ing ->
        addShoppingItem(
          ShoppingItem(
            name = ing.name,
            quantity = ing.quantity,
            unit = ing.unit,
            sourceRecipeTitle = recipe.title
          )
        )
        addedCount++
      }
    }
    return addedCount
  }

  // --- PREFERENCES & USER ---
  private fun loadPreferences() {
    val file = File(context.filesDir, "preferences.json")
    if (file.exists()) {
      try {
        val json = file.readText()
        val adapter = moshi.adapter(UserPreferences::class.java)
        val loaded = adapter.fromJson(json)
        if (loaded != null) {
          _userPreferences.value = loaded
          return
        }
      } catch (e: Exception) {
        Log.e("FridgeRepository", "Error loading preferences", e)
      }
    }
    val defaultPrefs = UserPreferences(hasCompletedOnboarding = true)
    _userPreferences.value = defaultPrefs
    savePreferences(defaultPrefs)
  }

  private fun savePreferences(prefs: UserPreferences) {
    scope.launch {
      try {
        val file = File(context.filesDir, "preferences.json")
        val adapter = moshi.adapter(UserPreferences::class.java)
        file.writeText(adapter.toJson(prefs))
      } catch (e: Exception) {
        Log.e("FridgeRepository", "Error saving preferences", e)
      }
    }
  }

  fun updateUserPreferences(prefs: UserPreferences) {
    _userPreferences.value = prefs
    savePreferences(prefs)
  }

  fun decrementAiQuota(): Boolean {
    val current = _userPreferences.value
    if (current.remainingAiGenerations <= 0) return false
    val updated = current.copy(remainingAiGenerations = current.remainingAiGenerations - 1)
    updateUserPreferences(updated)
    return true
  }

  fun resetAiQuota() {
    val tierObj = try {
      SubscriptionTier.valueOf(_userPreferences.value.tier)
    } catch (_: Exception) {
      SubscriptionTier.FREE
    }
    val updated = _userPreferences.value.copy(remainingAiGenerations = tierObj.dailyGenerationsLimit)
    updateUserPreferences(updated)
  }

  fun upgradeTier(tier: SubscriptionTier) {
    val updated = _userPreferences.value.copy(
      tier = tier.name,
      remainingAiGenerations = tier.dailyGenerationsLimit
    )
    updateUserPreferences(updated)
  }

  fun clearAllData() {
    _ingredients.value = emptyList()
    _recipes.value = emptyList()
    _mealPlan.value = emptyList()
    _shoppingList.value = emptyList()
    File(context.filesDir, "ingredients.json").delete()
    File(context.filesDir, "recipes.json").delete()
    File(context.filesDir, "meal_plan.json").delete()
    File(context.filesDir, "shopping_list.json").delete()
    loadAllData()
  }

  // --- SEED DATA ---
  private fun createInitialIngredients(): List<Ingredient> {
    val now = System.currentTimeMillis()
    val day = 24L * 60 * 60 * 1000

    return listOf(
      Ingredient(
        name = "Farm Fresh Eggs",
        quantity = 6.0,
        unit = "pcs",
        category = IngredientCategory.DAIRY_EGGS.name,
        storageLocation = StorageLocation.FRIDGE.name,
        expiryDateMillis = now + (12 * day),
        notes = "Pasture-raised"
      ),
      Ingredient(
        name = "Baby Spinach",
        quantity = 150.0,
        unit = "g",
        category = IngredientCategory.PRODUCE.name,
        storageLocation = StorageLocation.FRIDGE.name,
        expiryDateMillis = now + (2 * day), // Expiring soon!
        notes = "Washed and ready"
      ),
      Ingredient(
        name = "Cherry Tomatoes",
        quantity = 200.0,
        unit = "g",
        category = IngredientCategory.PRODUCE.name,
        storageLocation = StorageLocation.FRIDGE.name,
        expiryDateMillis = now + (3 * day), // Expiring soon!
        notes = "Sweet on the vine"
      ),
      Ingredient(
        name = "Aged Parmesan Cheese",
        quantity = 120.0,
        unit = "g",
        category = IngredientCategory.DAIRY_EGGS.name,
        storageLocation = StorageLocation.FRIDGE.name,
        expiryDateMillis = now + (25 * day)
      ),
      Ingredient(
        name = "Yellow Onion",
        quantity = 3.0,
        unit = "pcs",
        category = IngredientCategory.PRODUCE.name,
        storageLocation = StorageLocation.PANTRY.name,
        expiryDateMillis = now + (14 * day)
      ),
      Ingredient(
        name = "Garlic",
        quantity = 1.0,
        unit = "bulb",
        category = IngredientCategory.PRODUCE.name,
        storageLocation = StorageLocation.PANTRY.name,
        expiryDateMillis = now + (30 * day)
      ),
      Ingredient(
        name = "Greek Whole Milk Yogurt",
        quantity = 500.0,
        unit = "g",
        category = IngredientCategory.DAIRY_EGGS.name,
        storageLocation = StorageLocation.FRIDGE.name,
        expiryDateMillis = now + (1 * day), // Expiring tomorrow!
        notes = "Plain unflavored"
      ),
      Ingredient(
        name = "Extra Virgin Olive Oil",
        quantity = 750.0,
        unit = "ml",
        category = IngredientCategory.CONDIMENTS_SAUCES.name,
        storageLocation = StorageLocation.PANTRY.name,
        expiryDateMillis = now + (120 * day)
      ),
      Ingredient(
        name = "Artisan Sourdough Loaf",
        quantity = 0.5,
        unit = "loaf",
        category = IngredientCategory.BAKERY.name,
        storageLocation = StorageLocation.PANTRY.name,
        expiryDateMillis = now + (2 * day)
      ),
      Ingredient(
        name = "Chicken Breast Fillets",
        quantity = 400.0,
        unit = "g",
        category = IngredientCategory.MEAT_POULTRY.name,
        storageLocation = StorageLocation.FREEZER.name,
        expiryDateMillis = now + (45 * day)
      )
    )
  }

  private fun createInitialRecipes(): List<Recipe> {
    return listOf(
      Recipe(
        id = "recipe_frittata_01",
        title = "Rustic Spinach & Parmesan Frittata",
        description = "A golden, fluffy skillet frittata packed with vibrant baby spinach, melted cheese, and sweet blistered cherry tomatoes.",
        prepTimeMinutes = 8,
        cookTimeMinutes = 15,
        difficulty = "Easy",
        mealType = "Breakfast",
        cuisine = "Mediterranean",
        servings = 2,
        ingredients = listOf(
          RecipeIngredient("Farm Fresh Eggs", 4.0, "pcs", isAvailableInFridge = true),
          RecipeIngredient("Baby Spinach", 100.0, "g", isAvailableInFridge = true),
          RecipeIngredient("Cherry Tomatoes", 100.0, "g", isAvailableInFridge = true),
          RecipeIngredient("Aged Parmesan Cheese", 40.0, "g", isAvailableInFridge = true),
          RecipeIngredient("Extra Virgin Olive Oil", 1.0, "tbsp", isAvailableInFridge = true),
          RecipeIngredient("Black Pepper", 1.0, "pinch", isAvailableInFridge = true)
        ),
        instructions = listOf(
          "Whisk 4 eggs with a pinch of sea salt, black pepper, and half the grated parmesan until light and frothy.",
          "Warm 1 tbsp olive oil in an 8-inch oven-safe skillet over medium heat. Add cherry tomatoes and cook for 2 minutes until blistered.",
          "Add baby spinach in batches; stir gently for 1 minute until wilted.",
          "Pour the beaten eggs evenly over the skillet. Cook on low heat undisturbed for 4-5 minutes until the bottom is set.",
          "Scatter remaining parmesan on top and broil on high for 2 minutes until golden, puffed, and aromatic."
        ),
        stepTimersMinutes = listOf(null, 2, 1, 5, 2),
        nutrition = NutritionInfo(calories = 310, proteinGrams = 22, carbsGrams = 6, fatGrams = 22, fiberGrams = 2, sugarGrams = 3),
        substitutions = mapOf("Aged Parmesan" to "Feta, Cheddar, or Pecorino", "Baby Spinach" to "Kale, Arugula, or Swiss Chard"),
        storageTips = "Wrap slices in parchment and refrigerate for up to 3 days. Reheats crisply in a dry skillet.",
        allergenWarnings = listOf("Eggs", "Dairy"),
        isFavorite = true
      ),
      Recipe(
        id = "recipe_aglio_02",
        title = "Garlic Blistered Tomato Aglio e Olio",
        description = "Fragrant golden garlic slices steeped in olive oil, tossed with sweet blistered tomatoes and tender greens.",
        prepTimeMinutes = 10,
        cookTimeMinutes = 12,
        difficulty = "Easy",
        mealType = "Dinner",
        cuisine = "Italian",
        servings = 2,
        ingredients = listOf(
          RecipeIngredient("Spaghetti or Linguine", 200.0, "g", isAvailableInFridge = false),
          RecipeIngredient("Garlic", 4.0, "cloves", isAvailableInFridge = true),
          RecipeIngredient("Cherry Tomatoes", 150.0, "g", isAvailableInFridge = true),
          RecipeIngredient("Extra Virgin Olive Oil", 3.0, "tbsp", isAvailableInFridge = true),
          RecipeIngredient("Baby Spinach", 50.0, "g", isAvailableInFridge = true),
          RecipeIngredient("Aged Parmesan Cheese", 30.0, "g", isAvailableInFridge = true)
        ),
        instructions = listOf(
          "Cook pasta in salted boiling water until al dente (about 8-9 min). Reserve 1/2 cup of starchy cooking water before draining.",
          "In a wide skillet, heat olive oil and thinly sliced garlic over medium-low heat until pale golden and aromatic. Watch closely so it doesn't brown.",
          "Toss in whole cherry tomatoes. Cover with a lid and let them burst for 3 minutes, then mash lightly with a wooden spatula.",
          "Transfer pasta directly to the skillet along with 3 tbsp pasta water and spinach. Toss vigorously over heat for 1 minute to emulsify into a silky sauce.",
          "Turn off heat, stir in grated parmesan and freshly cracked black pepper. Serve warm."
        ),
        stepTimersMinutes = listOf(9, 3, 3, 1, null),
        nutrition = NutritionInfo(calories = 440, proteinGrams = 14, carbsGrams = 64, fatGrams = 15, fiberGrams = 4, sugarGrams = 5),
        substitutions = mapOf("Spaghetti" to "Penne, Soba, or Rigatoni", "Parmesan" to "Toasted breadcrumbs for vegan option"),
        storageTips = "Store in an airtight container for up to 2 days. Refresh with a tablespoon of olive oil when reheating.",
        allergenWarnings = listOf("Gluten", "Dairy")
      ),
      Recipe(
        id = "recipe_crispy_eggs_03",
        title = "Crispy Olive Oil Eggs with Caramelized Onion Toast",
        description = "Sourdough toast layered with sweet slow-caramelized onions and olive-oil fried eggs with lace-crispy edges and velvety yolks.",
        prepTimeMinutes = 5,
        cookTimeMinutes = 12,
        difficulty = "Easy",
        mealType = "Breakfast",
        cuisine = "Modern American",
        servings = 1,
        ingredients = listOf(
          RecipeIngredient("Artisan Sourdough Loaf", 2.0, "slices", isAvailableInFridge = true),
          RecipeIngredient("Farm Fresh Eggs", 2.0, "pcs", isAvailableInFridge = true),
          RecipeIngredient("Yellow Onion", 1.0, "pcs", isAvailableInFridge = true),
          RecipeIngredient("Extra Virgin Olive Oil", 2.0, "tbsp", isAvailableInFridge = true),
          RecipeIngredient("Aged Parmesan Cheese", 15.0, "g", isAvailableInFridge = true)
        ),
        instructions = listOf(
          "Thinly slice onion. Heat 1 tbsp olive oil in a skillet and cook onions over medium heat with a pinch of salt until deeply golden, about 7 minutes.",
          "Toast sourdough slices until crisp and golden brown.",
          "Spread the warm caramelized onions generously across the toasted sourdough.",
          "In the same skillet, heat remaining 1 tbsp olive oil over high heat. Crack in the eggs; tilt pan to spoon sizzling hot oil over the whites until bubbly and crisp, keeping yolks soft.",
          "Slide crispy eggs onto the prepared toasts. Shower with shaved parmesan and flaky salt."
        ),
        stepTimersMinutes = listOf(7, 3, null, 3, null),
        nutrition = NutritionInfo(calories = 420, proteinGrams = 18, carbsGrams = 38, fatGrams = 22, fiberGrams = 3, sugarGrams = 6),
        substitutions = mapOf("Sourdough" to "Brioche, English Muffin, or Rye Bread"),
        storageTips = "Best consumed fresh off the skillet.",
        allergenWarnings = listOf("Gluten", "Eggs", "Dairy")
      ),
      Recipe(
        id = "recipe_yogurt_bowl_04",
        title = "Savory Turkish-Style Garlic Herb Yogurt",
        description = "Velvety whipped garlic yogurt drizzled with warm chili-infused olive oil and served with toasted artisan sourdough soldiers.",
        prepTimeMinutes = 6,
        cookTimeMinutes = 4,
        difficulty = "Easy",
        mealType = "Snack",
        cuisine = "Mediterranean",
        servings = 1,
        ingredients = listOf(
          RecipeIngredient("Greek Whole Milk Yogurt", 200.0, "g", isAvailableInFridge = true),
          RecipeIngredient("Garlic", 1.0, "clove", isAvailableInFridge = true),
          RecipeIngredient("Extra Virgin Olive Oil", 1.5, "tbsp", isAvailableInFridge = true),
          RecipeIngredient("Artisan Sourdough Loaf", 1.0, "slice", isAvailableInFridge = true),
          RecipeIngredient("Black Pepper", 1.0, "pinch", isAvailableInFridge = true)
        ),
        instructions = listOf(
          "Grate garlic finely into a bowl with the Greek yogurt and a pinch of salt. Whisk until silky smooth and spread across a shallow bowl.",
          "In a small butter pan, warm olive oil over low heat for 2 minutes with crushed red pepper or paprika until fragrant.",
          "Toast sourdough slice and cut into dipping soldiers.",
          "Drizzle the warm infused oil over the cool yogurt base. Dip warm crunchy toast and savor the contrast."
        ),
        stepTimersMinutes = listOf(null, 2, 2, null),
        nutrition = NutritionInfo(calories = 340, proteinGrams = 19, carbsGrams = 24, fatGrams = 19, fiberGrams = 2, sugarGrams = 5),
        substitutions = mapOf("Greek Yogurt" to "Labneh or Sour Cream"),
        storageTips = "Garlic yogurt base can be kept chilled in fridge for up to 2 days.",
        allergenWarnings = listOf("Dairy", "Gluten")
      )
    )
  }

  private fun createInitialMealPlan(): List<MealPlanItem> {
    return listOf(
      MealPlanItem(
        dayOfWeek = "Monday",
        mealType = "Breakfast",
        recipeId = "recipe_frittata_01",
        recipeTitle = "Rustic Spinach & Parmesan Frittata",
        servings = 2,
        isCompleted = true
      ),
      MealPlanItem(
        dayOfWeek = "Monday",
        mealType = "Dinner",
        recipeId = "recipe_aglio_02",
        recipeTitle = "Garlic Blistered Tomato Aglio e Olio",
        servings = 2,
        isCompleted = false
      ),
      MealPlanItem(
        dayOfWeek = "Tuesday",
        mealType = "Breakfast",
        recipeId = "recipe_crispy_eggs_03",
        recipeTitle = "Crispy Olive Oil Eggs with Caramelized Onion Toast",
        servings = 1,
        isCompleted = false
      ),
      MealPlanItem(
        dayOfWeek = "Wednesday",
        mealType = "Snack",
        recipeId = "recipe_yogurt_bowl_04",
        recipeTitle = "Savory Turkish-Style Garlic Herb Yogurt",
        servings = 1,
        isCompleted = false
      )
    )
  }
}
