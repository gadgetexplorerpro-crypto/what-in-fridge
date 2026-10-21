package com.example.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Ingredient
import com.example.data.model.MealPlanItem
import com.example.data.model.Recipe
import com.example.data.model.ShoppingItem
import com.example.data.model.StorageLocation
import com.example.data.model.SubscriptionTier
import com.example.data.model.UserPreferences
import com.example.data.remote.DetectedIngredient
import com.example.data.remote.GeminiService
import com.example.data.repository.FridgeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FridgeViewModel(application: Application) : AndroidViewModel(application) {

  private val repository = FridgeRepository(application.applicationContext)
  private val geminiService = GeminiService()

  val ingredients: StateFlow<List<Ingredient>> = repository.ingredients
  val recipes: StateFlow<List<Recipe>> = repository.recipes
  val mealPlan: StateFlow<List<MealPlanItem>> = repository.mealPlan
  val shoppingList: StateFlow<List<ShoppingItem>> = repository.shoppingList
  val userPreferences: StateFlow<UserPreferences> = repository.userPreferences

  // UI States
  private val _selectedTab = MutableStateFlow(0)
  val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

  private val _isScanning = MutableStateFlow(false)
  val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

  private val _scannedCandidates = MutableStateFlow<List<DetectedIngredient>>(emptyList())
  val scannedCandidates: StateFlow<List<DetectedIngredient>> = _scannedCandidates.asStateFlow()

  private val _showScanConfirmationDialog = MutableStateFlow(false)
  val showScanConfirmationDialog: StateFlow<Boolean> = _showScanConfirmationDialog.asStateFlow()

  private val _isGeneratingRecipe = MutableStateFlow(false)
  val isGeneratingRecipe: StateFlow<Boolean> = _isGeneratingRecipe.asStateFlow()

  private val _selectedRecipeForDetail = MutableStateFlow<Recipe?>(null)
  val selectedRecipeForDetail: StateFlow<Recipe?> = _selectedRecipeForDetail.asStateFlow()

  private val _selectedRecipeForCooking = MutableStateFlow<Recipe?>(null)
  val selectedRecipeForCooking: StateFlow<Recipe?> = _selectedRecipeForCooking.asStateFlow()

  private val _showAddIngredientDialog = MutableStateFlow(false)
  val showAddIngredientDialog: StateFlow<Boolean> = _showAddIngredientDialog.asStateFlow()

  private val _ingredientToEdit = MutableStateFlow<Ingredient?>(null)
  val ingredientToEdit: StateFlow<Ingredient?> = _ingredientToEdit.asStateFlow()

  private val _showGenerateRecipeDialog = MutableStateFlow(false)
  val showGenerateRecipeDialog: StateFlow<Boolean> = _showGenerateRecipeDialog.asStateFlow()

  private val _showUpgradeDialog = MutableStateFlow(false)
  val showUpgradeDialog: StateFlow<Boolean> = _showUpgradeDialog.asStateFlow()

  private val _showApiKeyDialog = MutableStateFlow(false)
  val showApiKeyDialog: StateFlow<Boolean> = _showApiKeyDialog.asStateFlow()

  private val _statusSnackbar = MutableStateFlow<String?>(null)
  val statusSnackbar: StateFlow<String?> = _statusSnackbar.asStateFlow()

  fun selectTab(tabIndex: Int) {
    _selectedTab.value = tabIndex
  }

  fun clearSnackbar() {
    _statusSnackbar.value = null
  }

  fun openScanConfirmation(candidates: List<DetectedIngredient>) {
    _scannedCandidates.value = candidates
    _showScanConfirmationDialog.value = true
  }

  fun dismissScanConfirmation() {
    _showScanConfirmationDialog.value = false
    _scannedCandidates.value = emptyList()
  }

  fun openAddIngredient(ingredient: Ingredient? = null) {
    _ingredientToEdit.value = ingredient
    _showAddIngredientDialog.value = true
  }

  fun dismissAddIngredient() {
    _showAddIngredientDialog.value = false
    _ingredientToEdit.value = null
  }

  fun openGenerateRecipeDialog() {
    _showGenerateRecipeDialog.value = true
  }

  fun dismissGenerateRecipeDialog() {
    _showGenerateRecipeDialog.value = false
  }

  fun openUpgradeDialog() {
    _showUpgradeDialog.value = true
  }

  fun dismissUpgradeDialog() {
    _showUpgradeDialog.value = false
  }

  fun openApiKeyDialog() {
    _showApiKeyDialog.value = true
  }

  fun dismissApiKeyDialog() {
    _showApiKeyDialog.value = false
  }

  fun selectRecipeDetail(recipe: Recipe?) {
    _selectedRecipeForDetail.value = recipe
  }

  fun startCookingMode(recipe: Recipe) {
    _selectedRecipeForCooking.value = recipe
  }

  fun exitCookingMode() {
    _selectedRecipeForCooking.value = null
  }

  // --- ACTIONS ---

  fun scanImage(bitmap: Bitmap) {
    viewModelScope.launch {
      _isScanning.value = true
      _statusSnackbar.value = "Analyzing fridge photo with Gemini Vision..."
      val result = geminiService.scanIngredientsFromImage(
        bitmap = bitmap,
        customApiKey = userPreferences.value.customApiKey
      )
      _isScanning.value = false
      result.onSuccess { list ->
        if (list.isNotEmpty()) {
          _scannedCandidates.value = list
          _showScanConfirmationDialog.value = true
          _statusSnackbar.value = "Detected ${list.size} ingredients! Review and confirm."
        } else {
          _statusSnackbar.value = "No food ingredients recognized. Try a brighter photo or add manually."
        }
      }.onFailure {
        _statusSnackbar.value = "Photo analysis failed. Check network or API settings."
      }
    }
  }

  fun confirmScannedIngredients(selected: List<DetectedIngredient>) {
    val now = System.currentTimeMillis()
    val day = 24L * 60 * 60 * 1000
    val newIngredients = selected.map {
      Ingredient(
        name = it.name,
        quantity = it.quantity,
        unit = it.unit,
        category = it.category,
        storageLocation = it.storageLocation,
        expiryDateMillis = now + (it.estimatedExpiryDays * day),
        notes = "Scanned with AI"
      )
    }
    repository.addMultipleIngredients(newIngredients)
    dismissScanConfirmation()
    _statusSnackbar.value = "Added ${newIngredients.size} items to your inventory!"
  }

  fun saveIngredient(
    id: String?,
    name: String,
    quantity: Double,
    unit: String,
    category: String,
    storageLocation: String,
    expiryDays: Int?,
    notes: String
  ) {
    val expiryMillis = if (expiryDays != null && expiryDays > 0) {
      System.currentTimeMillis() + (expiryDays.toLong() * 24 * 60 * 60 * 1000)
    } else null

    if (id != null) {
      val existing = ingredients.value.firstOrNull { it.id == id }
      if (existing != null) {
        val updated = existing.copy(
          name = name.trim(),
          quantity = quantity,
          unit = unit,
          category = category,
          storageLocation = storageLocation,
          expiryDateMillis = expiryMillis ?: existing.expiryDateMillis,
          notes = notes.trim()
        )
        repository.updateIngredient(updated)
        _statusSnackbar.value = "Updated $name"
      }
    } else {
      val newIng = Ingredient(
        name = name.trim(),
        quantity = quantity,
        unit = unit,
        category = category,
        storageLocation = storageLocation,
        expiryDateMillis = expiryMillis,
        notes = notes.trim()
      )
      repository.addIngredient(newIng)
      _statusSnackbar.value = "Added $name to $storageLocation"
    }
    dismissAddIngredient()
  }

  fun deleteIngredient(id: String) {
    repository.deleteIngredient(id)
    _statusSnackbar.value = "Ingredient removed"
  }

  fun adjustQuantity(id: String, delta: Double) {
    repository.adjustQuantity(id, delta)
  }

  fun markIngredientUsed(id: String, isUsed: Boolean) {
    repository.markIngredientUsed(id, isUsed)
  }

  fun generateRecipes(
    prioritizeExpiring: Boolean = true,
    cuisine: String = "Any",
    maxCookingTime: Int = 30,
    difficulty: String = "Easy",
    mealType: String = "Dinner"
  ) {
    if (userPreferences.value.remainingAiGenerations <= 0) {
      _showUpgradeDialog.value = true
      _statusSnackbar.value = "Daily AI generations limit reached! Upgrade or reset quota."
      return
    }

    val available = if (prioritizeExpiring) {
      val expiring = ingredients.value.filter { !it.isUsed && (it.isExpiringSoon() || it.isExpired()) }
      if (expiring.isNotEmpty()) expiring.map { it.name }
      else ingredients.value.filter { !it.isUsed }.map { it.name }
    } else {
      ingredients.value.filter { !it.isUsed }.map { it.name }
    }

    if (available.isEmpty()) {
      _statusSnackbar.value = "Add ingredients to your fridge first!"
      return
    }

    viewModelScope.launch {
      _isGeneratingRecipe.value = true
      _showGenerateRecipeDialog.value = false
      _statusSnackbar.value = "Crafting recipes with Gemini AI..."

      repository.decrementAiQuota()

      val result = geminiService.generateAiRecipes(
        availableIngredients = available.take(8),
        cuisine = cuisine,
        maxCookingTime = maxCookingTime,
        difficulty = difficulty,
        mealType = mealType,
        dietaryTags = userPreferences.value.allergiesToAvoid,
        customApiKey = userPreferences.value.customApiKey
      )

      _isGeneratingRecipe.value = false
      result.onSuccess { list ->
        repository.saveGeneratedRecipes(list)
        _statusSnackbar.value = "Generated ${list.size} new tailored recipes!"
        _selectedTab.value = 2 // Switch to Recipes tab
      }.onFailure {
        _statusSnackbar.value = "Failed to generate recipes. Using offline culinary suggestions."
      }
    }
  }

  fun toggleFavorite(recipeId: String) {
    repository.toggleFavoriteRecipe(recipeId)
  }

  fun markRecipeCooked(recipeId: String) {
    repository.markRecipeCooked(recipeId)
    _statusSnackbar.value = "Marked as cooked! Ingredients updated."
  }

  fun addMissingToShopping(recipe: Recipe) {
    val count = repository.addMissingRecipeIngredientsToShoppingList(recipe)
    _statusSnackbar.value = if (count > 0) "Added $count missing ingredients to shopping list" else "All ingredients already available!"
  }

  fun addShoppingItem(name: String, quantity: Double, unit: String) {
    if (name.isBlank()) return
    repository.addShoppingItem(
      ShoppingItem(
        name = name.trim(),
        quantity = quantity,
        unit = unit
      )
    )
    _statusSnackbar.value = "Added $name to shopping list"
  }

  fun toggleShoppingItem(id: String) {
    repository.toggleShoppingItem(id)
  }

  fun deleteShoppingItem(id: String) {
    repository.deleteShoppingItem(id)
  }

  fun moveBoughtItemsToFridge() {
    val moved = repository.moveBoughtItemsToFridge()
    if (moved > 0) {
      _statusSnackbar.value = "Moved $moved purchased items to your Fridge inventory!"
    } else {
      _statusSnackbar.value = "Check off items first before moving to fridge."
    }
  }

  fun assignMealPlan(day: String, mealType: String, recipe: Recipe) {
    repository.setMealPlanItem(day, mealType, recipe)
    _statusSnackbar.value = "Scheduled ${recipe.title} for $day $mealType"
  }

  fun removeMealPlanItem(id: String) {
    repository.removeMealPlanItem(id)
  }

  fun toggleMealPlanCompleted(id: String) {
    repository.toggleMealPlanCompleted(id)
  }

  fun generateShoppingFromMealPlan() {
    val added = repository.generateShoppingListFromMealPlan()
    _statusSnackbar.value = "Added $added required ingredients from meal plan to shopping list"
  }

  fun updatePreferences(
    diet: String,
    allergies: List<String>,
    householdSize: Int,
    cookingSkill: String,
    maxTime: Int
  ) {
    val updated = userPreferences.value.copy(
      dietaryPreference = diet,
      allergiesToAvoid = allergies,
      householdSize = householdSize,
      cookingSkill = cookingSkill,
      maxCookingTimeMinutes = maxTime
    )
    repository.updateUserPreferences(updated)
    _statusSnackbar.value = "Preferences saved successfully!"
  }

  fun setCustomApiKey(key: String) {
    val updated = userPreferences.value.copy(customApiKey = key.trim())
    repository.updateUserPreferences(updated)
    _statusSnackbar.value = "API key configuration updated"
    dismissApiKeyDialog()
  }

  fun upgradeTier(tier: SubscriptionTier) {
    repository.upgradeTier(tier)
    _statusSnackbar.value = "Switched to ${tier.tierName} plan!"
    dismissUpgradeDialog()
  }

  fun resetQuota() {
    repository.resetAiQuota()
    _statusSnackbar.value = "AI generation quota reset for today!"
  }

  fun clearAllData() {
    repository.clearAllData()
    _statusSnackbar.value = "All data reset to fresh defaults"
  }
}
