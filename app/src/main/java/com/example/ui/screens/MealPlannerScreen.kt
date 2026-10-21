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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.data.model.MealPlanItem
import com.example.data.model.Recipe
import com.example.ui.components.EmptyStateView
import com.example.ui.theme.FreshGreen
import com.example.ui.theme.TerracottaAccent

@Composable
fun MealPlannerScreen(
  mealPlan: List<MealPlanItem>,
  recipes: List<Recipe>,
  onAssignMeal: (day: String, mealType: String, recipe: Recipe) -> Unit,
  onRemoveMeal: (id: String) -> Unit,
  onToggleCompleted: (id: String) -> Unit,
  onGenerateShoppingList: () -> Unit,
  onRecipeClick: (Recipe) -> Unit,
  modifier: Modifier = Modifier
) {
  val daysOfWeek = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
  val mealTypes = listOf("Breakfast", "Lunch", "Dinner", "Snack")

  var selectedDay by remember { mutableStateOf("Monday") }
  var showSelectRecipeDialog by remember { mutableStateOf<Pair<String, String>?>(null) } // (Day, MealType)

  val currentDayMeals = mealPlan.filter { it.dayOfWeek.equals(selectedDay, ignoreCase = true) }

  Column(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp)
  ) {
    // Top banner for shopping list generation
    Card(
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
      modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 8.dp)
        .testTag("meal_planner_shopping_card")
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "Smart Weekly Grocery Sync",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onPrimaryContainer
          )
          Text(
            text = "Auto-calculate missing ingredients for planned meals",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
          )
        }
        Button(
          onClick = onGenerateShoppingList,
          shape = RoundedCornerShape(10.dp),
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
          contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
          modifier = Modifier.testTag("meal_planner_sync_button")
        ) {
          Icon(Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Sync List", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
        }
      }
    }

    Spacer(modifier = Modifier.height(4.dp))

    // Days selector row
    LazyRow(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      modifier = Modifier.fillMaxWidth()
    ) {
      items(daysOfWeek) { day ->
        val isSelected = selectedDay == day
        val countForDay = mealPlan.count { it.dayOfWeek.equals(day, ignoreCase = true) }

        FilterChip(
          selected = isSelected,
          onClick = { selectedDay = day },
          label = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(vertical = 4.dp)) {
              Text(
                text = day.take(3),
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
              )
              if (countForDay > 0) {
                Text(
                  text = "$countForDay meals",
                  style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                  color = if (isSelected) Color.White else MaterialTheme.colorScheme.primary
                )
              }
            }
          },
          shape = RoundedCornerShape(16.dp),
          colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = Color.White
          ),
          modifier = Modifier.testTag("planner_day_chip_${day.lowercase()}")
        )
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // Meal Slots
    LazyColumn(
      verticalArrangement = Arrangement.spacedBy(12.dp),
      contentPadding = PaddingValues(bottom = 80.dp),
      modifier = Modifier.fillMaxSize()
    ) {
      items(mealTypes) { mealType ->
        val assigned = currentDayMeals.firstOrNull { it.mealType.equals(mealType, ignoreCase = true) }
        val recipeObj = assigned?.let { item -> recipes.firstOrNull { it.id == item.recipeId } }

        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(
            containerColor = if (assigned?.isCompleted == true) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.surface
          ),
          elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("planner_slot_${mealType.lowercase()}")
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.secondaryContainer
              ) {
                Text(
                  text = mealType.uppercase(),
                  style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                  color = MaterialTheme.colorScheme.onSecondaryContainer,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
              }

              if (assigned != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Checkbox(
                    checked = assigned.isCompleted,
                    onCheckedChange = { onToggleCompleted(assigned.id) },
                    colors = CheckboxDefaults.colors(checkedColor = FreshGreen)
                  )
                  IconButton(
                    onClick = { onRemoveMeal(assigned.id) },
                    modifier = Modifier.size(28.dp)
                  ) {
                    Icon(Icons.Default.Close, contentDescription = "Remove meal", modifier = Modifier.size(16.dp))
                  }
                }
              }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (assigned != null) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .clickable { if (recipeObj != null) onRecipeClick(recipeObj) },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = assigned.recipeTitle,
                    style = MaterialTheme.typography.titleMedium.copy(
                      fontWeight = FontWeight.Bold,
                      color = if (assigned.isCompleted) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                      else MaterialTheme.colorScheme.onSurface
                    )
                  )
                  if (recipeObj != null) {
                    Text(
                      text = "${recipeObj.totalTimeMinutes}m • ${recipeObj.difficulty} • ${recipeObj.nutrition.calories} kcal",
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                  }
                }

                Button(
                  onClick = { showSelectRecipeDialog = selectedDay to mealType },
                  colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                  shape = RoundedCornerShape(8.dp),
                  contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                  Text(
                    text = "Swap",
                    style = MaterialTheme.typography.labelSmall.copy(
                      fontWeight = FontWeight.Bold,
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                  )
                }
              }
            } else {
              // Empty slot CTA
              OutlinedButton(
                onClick = { showSelectRecipeDialog = selectedDay to mealType },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("planner_assign_${mealType.lowercase()}")
              ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Assign $mealType Recipe")
              }
            }
          }
        }
      }
    }
  }

  // Dialog to Pick Recipe
  if (showSelectRecipeDialog != null) {
    val (day, mealType) = showSelectRecipeDialog!!
    AlertDialog(
      onDismissRequest = { showSelectRecipeDialog = null },
      title = { Text("Choose Recipe for $day $mealType") },
      text = {
        LazyColumn(
          modifier = Modifier
            .fillMaxWidth()
            .height(300.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          items(recipes) { recipe ->
            Card(
              shape = RoundedCornerShape(10.dp),
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
              modifier = Modifier
                .fillMaxWidth()
                .clickable {
                  onAssignMeal(day, mealType, recipe)
                  showSelectRecipeDialog = null
                }
            ) {
              Column(modifier = Modifier.padding(10.dp)) {
                Text(recipe.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                Text(
                  "${recipe.totalTimeMinutes}m • ${recipe.matchPercentage}% fridge match",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }
          }
        }
      },
      confirmButton = {
        TextButton(onClick = { showSelectRecipeDialog = null }) {
          Text("Cancel")
        }
      }
    )
  }
}
