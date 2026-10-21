package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.Ingredient
import com.example.data.model.IngredientCategory
import com.example.data.model.StorageLocation
import com.example.data.model.SubscriptionTier
import com.example.data.repository.FridgeRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  private lateinit var context: Context
  private lateinit var repository: FridgeRepository

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    repository = FridgeRepository(context)
  }

  @Test
  fun testAppNameString() {
    val appName = context.getString(R.string.app_name)
    assertEquals("What's in My Fridge?", appName)
  }

  @Test
  fun testInitialIngredientsLoaded() {
    val items = repository.ingredients.value
    assertNotNull(items)
    assertTrue("Initial ingredients should not be empty", items.isNotEmpty())
    assertTrue("Should contain Eggs", items.any { it.name.contains("Eggs", ignoreCase = true) })
  }

  @Test
  fun testAddAndAdjustIngredient() {
    val testItem = Ingredient(
      name = "Organic Avocados",
      quantity = 3.0,
      unit = "pcs",
      category = IngredientCategory.PRODUCE.name,
      storageLocation = StorageLocation.FRIDGE.name
    )
    repository.addIngredient(testItem)
    val item = repository.ingredients.value.firstOrNull { it.name == "Organic Avocados" }
    assertNotNull(item)
    assertEquals(3.0, item!!.quantity, 0.01)

    repository.adjustQuantity(item.id, 2.0)
    val updated = repository.ingredients.value.firstOrNull { it.id == item.id }
    assertEquals(5.0, updated!!.quantity, 0.01)
  }

  @Test
  fun testIngredientExpiryCalculation() {
    val now = System.currentTimeMillis()
    val day = 24L * 60 * 60 * 1000

    val expiringItem = Ingredient(
      name = "Greek Yogurt",
      quantity = 1.0,
      expiryDateMillis = now + (2 * day)
    )
    assertTrue("Should be expiring soon", expiringItem.isExpiringSoon())
    assertFalse("Should not be expired", expiringItem.isExpired())

    val freshItem = Ingredient(
      name = "Dry Lentils",
      quantity = 1.0,
      expiryDateMillis = now + (60 * day)
    )
    assertFalse("Should not be expiring soon", freshItem.isExpiringSoon())
  }

  @Test
  fun testInitialRecipesAndFavorites() {
    val recipes = repository.recipes.value
    assertTrue("Initial recipes should not be empty", recipes.isNotEmpty())

    val first = recipes.first()
    val initialFav = first.isFavorite
    repository.toggleFavoriteRecipe(first.id)
    val updated = repository.recipes.value.first { it.id == first.id }
    assertEquals(!initialFav, updated.isFavorite)
  }

  @Test
  fun testAiQuotaAndTierUpgrade() {
    val initialQuota = repository.userPreferences.value.remainingAiGenerations
    assertTrue(initialQuota > 0)

    val success = repository.decrementAiQuota()
    assertTrue(success)
    assertEquals(initialQuota - 1, repository.userPreferences.value.remainingAiGenerations)

    repository.upgradeTier(SubscriptionTier.PLUS)
    assertEquals(SubscriptionTier.PLUS.name, repository.userPreferences.value.tier)
    assertEquals(SubscriptionTier.PLUS.dailyGenerationsLimit, repository.userPreferences.value.remainingAiGenerations)
  }
}
