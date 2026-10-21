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
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import com.example.data.model.Ingredient
import com.example.data.model.IngredientCategory
import com.example.data.model.StorageLocation
import com.example.ui.components.EmptyStateView
import com.example.ui.components.ExpiryBadge
import com.example.ui.theme.FreshGreen
import com.example.ui.theme.TerracottaAccent

@Composable
fun FridgeScreen(
  ingredients: List<Ingredient>,
  onAddIngredient: () -> Unit,
  onScanPhoto: () -> Unit,
  onEditIngredient: (Ingredient) -> Unit,
  onDeleteIngredient: (String) -> Unit,
  onAdjustQuantity: (String, Double) -> Unit,
  onMarkUsed: (String, Boolean) -> Unit,
  modifier: Modifier = Modifier
) {
  var searchQuery by remember { mutableStateOf("") }
  var selectedLocation by remember { mutableStateOf("ALL") }
  var selectedCategory by remember { mutableStateOf("ALL") }
  var sortByExpiry by remember { mutableStateOf(true) }

  val filteredIngredients = ingredients.filter { ing ->
    val matchesSearch = ing.name.contains(searchQuery, ignoreCase = true)
    val matchesLocation = selectedLocation == "ALL" || ing.storageLocation.equals(selectedLocation, ignoreCase = true)
    val matchesCategory = selectedCategory == "ALL" || ing.category.equals(selectedCategory, ignoreCase = true)
    matchesSearch && matchesLocation && matchesCategory
  }.sortedWith(
    if (sortByExpiry) {
      compareBy<Ingredient> { it.expiryDateMillis ?: Long.MAX_VALUE }.thenBy { it.name }
    } else {
      compareBy { it.name }
    }
  )

  Scaffold(
    floatingActionButton = {
      Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        FloatingActionButton(
          onClick = onScanPhoto,
          containerColor = MaterialTheme.colorScheme.primaryContainer,
          contentColor = MaterialTheme.colorScheme.primary,
          modifier = Modifier.testTag("fridge_fab_scan")
        ) {
          Icon(Icons.Default.CameraAlt, contentDescription = "Scan with Camera")
        }

        FloatingActionButton(
          onClick = onAddIngredient,
          containerColor = MaterialTheme.colorScheme.primary,
          contentColor = Color.White,
          modifier = Modifier.testTag("fridge_fab_add")
        ) {
          Icon(Icons.Default.Add, contentDescription = "Add Ingredient")
        }
      }
    },
    modifier = modifier.fillMaxSize()
  ) { paddingValues ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
        .padding(horizontal = 16.dp)
    ) {
      // Search Bar
      OutlinedTextField(
        value = searchQuery,
        onValueChange = { searchQuery = it },
        placeholder = { Text("Search your ingredients...") },
        leadingIcon = {
          Icon(Icons.Default.Search, contentDescription = "Search")
        },
        trailingIcon = {
          IconButton(onClick = { sortByExpiry = !sortByExpiry }) {
            Icon(
              imageVector = Icons.Default.FilterList,
              contentDescription = "Toggle Sort",
              tint = if (sortByExpiry) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        },
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
          focusedContainerColor = MaterialTheme.colorScheme.surface,
          unfocusedContainerColor = MaterialTheme.colorScheme.surface
        ),
        singleLine = true,
        modifier = Modifier
          .fillMaxWidth()
          .testTag("fridge_search_input")
      )

      Spacer(modifier = Modifier.height(10.dp))

      // Storage Location Chips: All, Fridge, Freezer, Pantry
      val storageLocations = listOf("ALL" to "All (${ingredients.size})") +
          StorageLocation.values().map { it.name to "${it.displayName} (${ingredients.count { ing -> ing.storageLocation == it.name }})" }

      LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        items(storageLocations) { (locKey, label) ->
          FilterChip(
            selected = selectedLocation == locKey,
            onClick = { selectedLocation = locKey },
            label = { Text(label) },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = MaterialTheme.colorScheme.primary,
              selectedLabelColor = Color.White
            ),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.testTag("chip_storage_${locKey.lowercase()}")
          )
        }
      }

      Spacer(modifier = Modifier.height(6.dp))

      // Category Chips
      val categories = listOf("ALL" to "All Categories") +
          IngredientCategory.values().map { it.name to it.displayName }

      LazyRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        items(categories) { (catKey, label) ->
          FilterChip(
            selected = selectedCategory == catKey,
            onClick = { selectedCategory = catKey },
            label = { Text(label, style = MaterialTheme.typography.labelSmall) },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
              selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
            ),
            shape = RoundedCornerShape(14.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Ingredients List
      if (filteredIngredients.isEmpty()) {
        EmptyStateView(
          title = "No ingredients found",
          description = if (ingredients.isEmpty()) "Your kitchen inventory is currently empty. Scan a photo or add items manually to start finding recipes!" else "No items match your filter criteria.",
          icon = Icons.Default.Kitchen,
          buttonLabel = "Add Ingredient",
          onButtonClick = onAddIngredient,
          modifier = Modifier.fillMaxSize()
        )
      } else {
        LazyColumn(
          verticalArrangement = Arrangement.spacedBy(10.dp),
          contentPadding = PaddingValues(bottom = 80.dp),
          modifier = Modifier.fillMaxSize()
        ) {
          items(filteredIngredients, key = { it.id }) { item ->
            IngredientCard(
              ingredient = item,
              onAdjustQty = { delta -> onAdjustQuantity(item.id, delta) },
              onEdit = { onEditIngredient(item) },
              onDelete = { onDeleteIngredient(item.id) },
              onToggleUsed = { onMarkUsed(item.id, !item.isUsed) }
            )
          }
        }
      }
    }
  }
}

@Composable
fun IngredientCard(
  ingredient: Ingredient,
  onAdjustQty: (Double) -> Unit,
  onEdit: () -> Unit,
  onDelete: () -> Unit,
  onToggleUsed: () -> Unit,
  modifier: Modifier = Modifier
) {
  var showMenu by remember { mutableStateOf(false) }

  Card(
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(
      containerColor = if (ingredient.isUsed) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
      else MaterialTheme.colorScheme.surface
    ),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    modifier = modifier
      .fillMaxWidth()
      .testTag("ingredient_card_${ingredient.name.lowercase().replace(" ", "_")}")
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.weight(1f)
      ) {
        // Icon / Avatar
        Box(
          modifier = Modifier
            .size(42.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.primaryContainer),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.Kitchen,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp)
          )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = ingredient.name,
              style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
              color = if (ingredient.isUsed) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
              else MaterialTheme.colorScheme.onSurface
            )
            if (ingredient.isUsed) {
              Spacer(modifier = Modifier.width(6.dp))
              Surface(
                shape = RoundedCornerShape(4.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
              ) {
                Text(
                  text = "Used",
                  style = MaterialTheme.typography.labelSmall,
                  modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(2.dp))

          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = MaterialTheme.colorScheme.surfaceVariant
            ) {
              Text(
                text = ingredient.storageLocation,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
              )
            }

            ExpiryBadge(daysUntilExpiry = ingredient.daysUntilExpiry())
          }
        }
      }

      // Quantity controls & menu
      Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(
          onClick = { onAdjustQty(-1.0) },
          modifier = Modifier
            .size(32.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
        ) {
          Icon(Icons.Default.Remove, contentDescription = "Decrease", modifier = Modifier.size(16.dp))
        }

        Text(
          text = "${if (ingredient.quantity % 1.0 == 0.0) ingredient.quantity.toInt().toString() else ingredient.quantity.toString()} ${ingredient.unit}",
          style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
          modifier = Modifier.padding(horizontal = 8.dp)
        )

        IconButton(
          onClick = { onAdjustQty(1.0) },
          modifier = Modifier
            .size(32.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
        ) {
          Icon(Icons.Default.Add, contentDescription = "Increase", modifier = Modifier.size(16.dp))
        }

        Box {
          IconButton(onClick = { showMenu = true }) {
            Icon(Icons.Default.MoreVert, contentDescription = "More options")
          }
          DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
          ) {
            DropdownMenuItem(
              text = { Text("Edit") },
              leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
              onClick = {
                showMenu = false
                onEdit()
              }
            )
            DropdownMenuItem(
              text = { Text(if (ingredient.isUsed) "Mark as Active" else "Mark as Used") },
              leadingIcon = { Icon(Icons.Default.Check, contentDescription = null) },
              onClick = {
                showMenu = false
                onToggleUsed()
              }
            )
            DropdownMenuItem(
              text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
              leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
              onClick = {
                showMenu = false
                onDelete()
              }
            )
          }
        }
      }
    }
  }
}
