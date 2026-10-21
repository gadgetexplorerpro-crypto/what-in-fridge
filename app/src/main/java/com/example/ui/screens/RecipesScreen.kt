package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Recipe
import com.example.ui.components.EmptyStateView
import com.example.ui.theme.FreshGreen
import com.example.ui.theme.TerracottaAccent
import com.example.ui.theme.WarningOrange

@Composable
fun RecipesScreen(
  recipes: List<Recipe>,
  isGenerating: Boolean,
  onRecipeClick: (Recipe) -> Unit,
  onCookClick: (Recipe) -> Unit,
  onToggleFavorite: (String) -> Unit,
  onOpenGenerateWizard: () -> Unit,
  modifier: Modifier = Modifier
) {
  var selectedTab by remember { mutableStateOf(0) } // 0: Make Now, 1: Almost Ready, 2: All, 3: Favorites
  var searchQuery by remember { mutableStateOf("") }
  var selectedCuisine by remember { mutableStateOf("ALL") }

  val tabs = listOf("Make Now", "Almost Ready", "All Recipes", "Favorites")

  val filteredRecipes = recipes.filter { r ->
    val matchesSearch = r.title.contains(searchQuery, ignoreCase = true) ||
        r.cuisine.contains(searchQuery, ignoreCase = true) ||
        r.description.contains(searchQuery, ignoreCase = true)

    val matchesTab = when (selectedTab) {
      0 -> r.missingIngredients.isEmpty() // 100% available
      1 -> r.missingIngredients.size in 1..2 // Almost ready
      2 -> true // All
      3 -> r.isFavorite // Favorites
      else -> true
    }

    val matchesCuisine = selectedCuisine == "ALL" || r.cuisine.equals(selectedCuisine, ignoreCase = true)

    matchesSearch && matchesTab && matchesCuisine
  }.sortedByDescending { it.matchPercentage }

  Scaffold(
    floatingActionButton = {
      ExtendedFloatingActionButton(
        onClick = onOpenGenerateWizard,
        containerColor = TerracottaAccent,
        contentColor = Color.White,
        icon = { Icon(Icons.Default.AutoAwesome, contentDescription = null) },
        text = { Text("Generate AI Recipe", fontWeight = FontWeight.Bold) },
        modifier = Modifier.testTag("recipe_fab_generate")
      )
    },
    modifier = modifier.fillMaxSize()
  ) { paddingValues ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
        .padding(horizontal = 16.dp)
    ) {
      // Search
      OutlinedTextField(
        value = searchQuery,
        onValueChange = { searchQuery = it },
        placeholder = { Text("Search recipes or cuisines...") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
          focusedContainerColor = MaterialTheme.colorScheme.surface,
          unfocusedContainerColor = MaterialTheme.colorScheme.surface
        ),
        singleLine = true,
        modifier = Modifier
          .fillMaxWidth()
          .testTag("recipe_search_input")
      )

      Spacer(modifier = Modifier.height(10.dp))

      // Tab Row
      TabRow(
        selectedTabIndex = selectedTab,
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.primary,
        indicator = { tabPositions ->
          TabRowDefaults.SecondaryIndicator(
            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
            color = MaterialTheme.colorScheme.primary
          )
        }
      ) {
        tabs.forEachIndexed { index, tabTitle ->
          Tab(
            selected = selectedTab == index,
            onClick = { selectedTab = index },
            text = {
              Text(
                text = tabTitle,
                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                style = MaterialTheme.typography.labelMedium
              )
            },
            modifier = Modifier.testTag("recipe_tab_$index")
          )
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Cuisine Chips
      val cuisines = listOf("ALL" to "All Cuisines", "Mediterranean" to "Mediterranean", "Italian" to "Italian", "American" to "American", "Asian" to "Asian", "Mexican" to "Mexican")
      LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        items(cuisines) { (cKey, label) ->
          FilterChip(
            selected = selectedCuisine == cKey,
            onClick = { selectedCuisine = cKey },
            label = { Text(label, style = MaterialTheme.typography.labelSmall) },
            shape = RoundedCornerShape(16.dp),
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
              selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
            )
          )
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      if (isGenerating) {
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
        ) {
          Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            CircularProgressIndicator(
              modifier = Modifier.size(32.dp),
              color = TerracottaAccent
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
              Text(
                text = "Gemini AI is crafting recipes...",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
              )
              Text(
                text = "Matching ingredients and computing nutritional facts",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }
      }

      // Recipe List
      if (filteredRecipes.isEmpty() && !isGenerating) {
        EmptyStateView(
          title = if (selectedTab == 0) "No 100% Make-Now Recipes" else if (selectedTab == 3) "No Favorite Recipes" else "No Recipes Found",
          description = if (selectedTab == 0) "Add more ingredients to your fridge or check 'Almost Ready' to see meals needing only 1-2 items!" else "Generate delicious custom meals based on your available fridge ingredients.",
          icon = Icons.AutoMirrored.Filled.MenuBook,
          buttonLabel = "✨ Generate Recipes",
          onButtonClick = onOpenGenerateWizard,
          modifier = Modifier.fillMaxSize()
        )
      } else {
        LazyColumn(
          verticalArrangement = Arrangement.spacedBy(14.dp),
          contentPadding = PaddingValues(bottom = 90.dp),
          modifier = Modifier.fillMaxSize()
        ) {
          items(filteredRecipes, key = { it.id }) { recipe ->
            RecipeCardItem(
              recipe = recipe,
              onCardClick = { onRecipeClick(recipe) },
              onCookClick = { onCookClick(recipe) },
              onToggleFavorite = { onToggleFavorite(recipe.id) }
            )
          }
        }
      }
    }
  }
}

@Composable
fun RecipeCardItem(
  recipe: Recipe,
  onCardClick: () -> Unit,
  onCookClick: () -> Unit,
  onToggleFavorite: () -> Unit,
  modifier: Modifier = Modifier
) {
  Card(
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    modifier = modifier
      .fillMaxWidth()
      .clickable { onCardClick() }
      .testTag("recipe_card_${recipe.title.lowercase().replace(" ", "_")}")
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      // Header row
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Match Badge
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = if (recipe.missingIngredients.isEmpty()) FreshGreen.copy(alpha = 0.15f)
          else WarningOrange.copy(alpha = 0.15f)
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = if (recipe.missingIngredients.isEmpty()) Icons.Default.CheckCircle else Icons.Default.Warning,
              contentDescription = null,
              tint = if (recipe.missingIngredients.isEmpty()) FreshGreen else WarningOrange,
              modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = if (recipe.missingIngredients.isEmpty()) "Make Now (100%)"
              else "Missing ${recipe.missingIngredients.size} items",
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
              color = if (recipe.missingIngredients.isEmpty()) FreshGreen else WarningOrange
            )
          }
        }

        IconButton(onClick = onToggleFavorite, modifier = Modifier.size(32.dp)) {
          Icon(
            imageVector = if (recipe.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
            contentDescription = "Toggle favorite",
            tint = if (recipe.isFavorite) TerracottaAccent else MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      Text(
        text = recipe.title,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onSurface
      )

      Spacer(modifier = Modifier.height(4.dp))

      Text(
        text = recipe.description,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 2
      )

      Spacer(modifier = Modifier.height(10.dp))

      // Info pills: Time, Difficulty, Calories, Cuisine
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.Timer,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(14.dp)
          )
          Spacer(modifier = Modifier.width(3.dp))
          Text(
            text = "${recipe.totalTimeMinutes}m",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        Text(
          text = "•",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
          text = recipe.difficulty,
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
          text = "•",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
          text = "${recipe.nutrition.calories} kcal",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
          text = "•",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
          text = recipe.cuisine,
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Available vs Missing Ingredients preview
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "Ingredients: ${recipe.ingredients.count { it.isAvailableInFridge }} / ${recipe.ingredients.size} in fridge",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface
          )
        }

        Button(
          onClick = onCookClick,
          shape = RoundedCornerShape(10.dp),
          contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
          modifier = Modifier.testTag("recipe_cook_btn_${recipe.title.lowercase().replace(" ", "_")}")
        ) {
          Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = "Cook Mode",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
          )
        }
      }
    }
  }
}
