package com.example

import android.graphics.Bitmap
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.Ingredient
import com.example.data.model.Recipe
import com.example.ui.components.BottomNavBar
import com.example.ui.components.FridgeTopBar
import com.example.ui.screens.AddEditIngredientDialog
import com.example.ui.screens.ApiKeyDialog
import com.example.ui.screens.CookingModeScreen
import com.example.ui.screens.FridgeScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.MealPlannerScreen
import com.example.ui.screens.PhotoScannerBottomSheet
import com.example.ui.screens.PrivacyPolicyDialog
import com.example.ui.screens.ProfileSettingsScreen
import com.example.ui.screens.RecipeDetailScreen
import com.example.ui.screens.RecipeGeneratorDialog
import com.example.ui.screens.RecipesScreen
import com.example.ui.screens.ScanConfirmationDialog
import com.example.ui.screens.ShoppingListScreen
import com.example.ui.screens.UpgradeSubscriptionDialog
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.FridgeViewModel

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        MainAppScreen()
      }
    }
  }
}

@Composable
fun MainAppScreen(viewModel: FridgeViewModel = viewModel()) {
  val ingredients by viewModel.ingredients.collectAsStateWithLifecycle()
  val recipes by viewModel.recipes.collectAsStateWithLifecycle()
  val mealPlan by viewModel.mealPlan.collectAsStateWithLifecycle()
  val shoppingList by viewModel.shoppingList.collectAsStateWithLifecycle()
  val userPreferences by viewModel.userPreferences.collectAsStateWithLifecycle()

  val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
  val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()
  val scannedCandidates by viewModel.scannedCandidates.collectAsStateWithLifecycle()
  val showScanConfirmationDialog by viewModel.showScanConfirmationDialog.collectAsStateWithLifecycle()
  val isGeneratingRecipe by viewModel.isGeneratingRecipe.collectAsStateWithLifecycle()
  val selectedRecipeForDetail by viewModel.selectedRecipeForDetail.collectAsStateWithLifecycle()
  val selectedRecipeForCooking by viewModel.selectedRecipeForCooking.collectAsStateWithLifecycle()
  val showAddIngredientDialog by viewModel.showAddIngredientDialog.collectAsStateWithLifecycle()
  val ingredientToEdit by viewModel.ingredientToEdit.collectAsStateWithLifecycle()
  val showGenerateRecipeDialog by viewModel.showGenerateRecipeDialog.collectAsStateWithLifecycle()
  val showUpgradeDialog by viewModel.showUpgradeDialog.collectAsStateWithLifecycle()
  val showApiKeyDialog by viewModel.showApiKeyDialog.collectAsStateWithLifecycle()
  val statusSnackbar by viewModel.statusSnackbar.collectAsStateWithLifecycle()

  val snackbarHostState = remember { SnackbarHostState() }
  var showPhotoScannerSheet by remember { mutableStateOf(false) }
  var showPrivacyPolicyDialog by remember { mutableStateOf(false) }

  LaunchedEffect(statusSnackbar) {
    statusSnackbar?.let { msg ->
      snackbarHostState.showSnackbar(message = msg, duration = SnackbarDuration.Short)
      viewModel.clearSnackbar()
    }
  }

  // Handle system back navigation
  BackHandler(enabled = selectedRecipeForCooking != null || selectedRecipeForDetail != null || selectedTab != 0) {
    when {
      selectedRecipeForCooking != null -> viewModel.exitCookingMode()
      selectedRecipeForDetail != null -> viewModel.selectRecipeDetail(null)
      selectedTab != 0 -> viewModel.selectTab(0)
    }
  }

  when {
    selectedRecipeForCooking != null -> {
      CookingModeScreen(
        recipe = selectedRecipeForCooking!!,
        onExit = { viewModel.exitCookingMode() },
        onMarkCooked = {
          viewModel.markRecipeCooked(selectedRecipeForCooking!!.id)
        }
      )
    }

    selectedRecipeForDetail != null -> {
      RecipeDetailScreen(
        recipe = selectedRecipeForDetail!!,
        onBack = { viewModel.selectRecipeDetail(null) },
        onStartCooking = {
          val recipe = selectedRecipeForDetail!!
          viewModel.selectRecipeDetail(null)
          viewModel.startCookingMode(recipe)
        },
        onToggleFavorite = { viewModel.toggleFavorite(selectedRecipeForDetail!!.id) },
        onMarkCooked = { viewModel.markRecipeCooked(selectedRecipeForDetail!!.id) },
        onAddMissingToShopping = { viewModel.addMissingToShopping(selectedRecipeForDetail!!) },
        onAddToMealPlan = {
          viewModel.assignMealPlan("Monday", "Dinner", selectedRecipeForDetail!!)
        }
      )
    }

    else -> {
      Scaffold(
        topBar = {
          FridgeTopBar(
            title = when (selectedTab) {
              0 -> "What's in My Fridge?"
              1 -> "Kitchen Inventory"
              2 -> "Smart Recipes"
              3 -> "Weekly Meal Planner"
              4 -> "Smart Shopping List"
              5 -> "Chef Profile & Settings"
              else -> "What's in My Fridge?"
            },
            remainingQuota = userPreferences.remainingAiGenerations,
            onQuotaClick = { viewModel.openUpgradeDialog() },
            onCameraClick = { showPhotoScannerSheet = true },
            onProfileClick = { viewModel.selectTab(5) },
            showBackButton = selectedTab == 5,
            onBackClick = { viewModel.selectTab(0) }
          )
        },
        bottomBar = {
          BottomNavBar(
            selectedTab = if (selectedTab == 5) 0 else selectedTab,
            onTabSelected = { tabIndex -> viewModel.selectTab(tabIndex) }
          )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = Modifier.fillMaxSize()
      ) { innerPadding ->
        Box(
          modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
        ) {
          when (selectedTab) {
            0 -> HomeScreen(
              ingredients = ingredients,
              recipes = recipes,
              userPreferences = userPreferences,
              onScanClick = { showPhotoScannerSheet = true },
              onAddIngredientClick = { viewModel.openAddIngredient(null) },
              onFindRecipesClick = { viewModel.openGenerateRecipeDialog() },
              onRecipeClick = { recipe -> viewModel.selectRecipeDetail(recipe) },
              onSeeAllIngredients = { viewModel.selectTab(1) },
              onSeeAllRecipes = { viewModel.selectTab(2) },
              onUpgradeClick = { viewModel.openUpgradeDialog() },
              onCookExpiringClick = {
                viewModel.generateRecipes(prioritizeExpiring = true)
              }
            )

            1 -> FridgeScreen(
              ingredients = ingredients,
              onAddIngredient = { viewModel.openAddIngredient(null) },
              onScanPhoto = { showPhotoScannerSheet = true },
              onEditIngredient = { ing -> viewModel.openAddIngredient(ing) },
              onDeleteIngredient = { id -> viewModel.deleteIngredient(id) },
              onAdjustQuantity = { id, delta -> viewModel.adjustQuantity(id, delta) },
              onMarkUsed = { id, isUsed -> viewModel.markIngredientUsed(id, isUsed) }
            )

            2 -> RecipesScreen(
              recipes = recipes,
              isGenerating = isGeneratingRecipe,
              onRecipeClick = { recipe -> viewModel.selectRecipeDetail(recipe) },
              onCookClick = { recipe -> viewModel.startCookingMode(recipe) },
              onToggleFavorite = { id -> viewModel.toggleFavorite(id) },
              onOpenGenerateWizard = { viewModel.openGenerateRecipeDialog() }
            )

            3 -> MealPlannerScreen(
              mealPlan = mealPlan,
              recipes = recipes,
              onAssignMeal = { day, mealType, recipe ->
                viewModel.assignMealPlan(day, mealType, recipe)
              },
              onRemoveMeal = { id -> viewModel.removeMealPlanItem(id) },
              onToggleCompleted = { id -> viewModel.toggleMealPlanCompleted(id) },
              onGenerateShoppingList = { viewModel.generateShoppingFromMealPlan() },
              onRecipeClick = { recipe -> viewModel.selectRecipeDetail(recipe) }
            )

            4 -> ShoppingListScreen(
              shoppingList = shoppingList,
              onAddItem = { name, qty, unit ->
                viewModel.addShoppingItem(name, qty, unit)
              },
              onToggleItem = { id -> viewModel.toggleShoppingItem(id) },
              onDeleteItem = { id -> viewModel.deleteShoppingItem(id) },
              onMoveBoughtToFridge = { viewModel.moveBoughtItemsToFridge() }
            )

            5 -> ProfileSettingsScreen(
              userPreferences = userPreferences,
              onUpdatePreferences = { diet, allergies, household, skill, maxTime ->
                viewModel.updatePreferences(diet, allergies, household, skill, maxTime)
              },
              onOpenUpgradeDialog = { viewModel.openUpgradeDialog() },
              onOpenApiKeyDialog = { viewModel.openApiKeyDialog() },
              onOpenPrivacyPolicy = { showPrivacyPolicyDialog = true },
              onResetQuota = { viewModel.resetQuota() },
              onClearAllData = { viewModel.clearAllData() }
            )
          }
        }
      }
    }
  }

  // Dialogs
  if (showPhotoScannerSheet) {
    PhotoScannerBottomSheet(
      isScanning = isScanning,
      onDismiss = { showPhotoScannerSheet = false },
      onImageSelected = { bitmap ->
        showPhotoScannerSheet = false
        viewModel.scanImage(bitmap)
      }
    )
  }

  if (showScanConfirmationDialog) {
    ScanConfirmationDialog(
      candidates = scannedCandidates,
      onConfirm = { confirmed ->
        viewModel.confirmScannedIngredients(confirmed)
      },
      onDismiss = { viewModel.dismissScanConfirmation() }
    )
  }

  if (showAddIngredientDialog) {
    AddEditIngredientDialog(
      existingIngredient = ingredientToEdit,
      onDismiss = { viewModel.dismissAddIngredient() },
      onSave = { id, name, qty, unit, cat, loc, expDays, notes ->
        viewModel.saveIngredient(id, name, qty, unit, cat, loc, expDays, notes)
      }
    )
  }

  if (showGenerateRecipeDialog) {
    RecipeGeneratorDialog(
      availableIngredients = ingredients,
      onDismiss = { viewModel.dismissGenerateRecipeDialog() },
      onGenerate = { prioritizeExpiring, cuisine, maxTime, difficulty, mealType ->
        viewModel.generateRecipes(prioritizeExpiring, cuisine, maxTime, difficulty, mealType)
      }
    )
  }

  if (showUpgradeDialog) {
    UpgradeSubscriptionDialog(
      currentTier = userPreferences.tier,
      onSelectTier = { tier -> viewModel.upgradeTier(tier) },
      onDismiss = { viewModel.dismissUpgradeDialog() }
    )
  }

  if (showApiKeyDialog) {
    ApiKeyDialog(
      currentKey = userPreferences.customApiKey,
      onSaveKey = { key -> viewModel.setCustomApiKey(key) },
      onDismiss = { viewModel.dismissApiKeyDialog() }
    )
  }

  if (showPrivacyPolicyDialog) {
    PrivacyPolicyDialog(
      onDismiss = { showPrivacyPolicyDialog = false }
    )
  }
}
