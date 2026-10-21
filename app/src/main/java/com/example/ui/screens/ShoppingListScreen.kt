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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ShoppingItem
import com.example.ui.components.EmptyStateView
import com.example.ui.theme.FreshGreen

@Composable
fun ShoppingListScreen(
  shoppingList: List<ShoppingItem>,
  onAddItem: (name: String, quantity: Double, unit: String) -> Unit,
  onToggleItem: (id: String) -> Unit,
  onDeleteItem: (id: String) -> Unit,
  onMoveBoughtToFridge: () -> Unit,
  modifier: Modifier = Modifier
) {
  var newItemName by remember { mutableStateOf("") }
  var newItemQty by remember { mutableDoubleStateOf(1.0) }
  var newItemUnit by remember { mutableStateOf("pcs") }

  val checkedCount = shoppingList.count { it.isChecked }
  val totalCount = shoppingList.size

  Column(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp)
  ) {
    // Quick Add Card
    Card(
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
      modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 8.dp)
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        OutlinedTextField(
          value = newItemName,
          onValueChange = { newItemName = it },
          placeholder = { Text("Add grocery item...") },
          shape = RoundedCornerShape(12.dp),
          colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.background,
            unfocusedContainerColor = MaterialTheme.colorScheme.background
          ),
          singleLine = true,
          modifier = Modifier
            .weight(1f)
            .testTag("shopping_add_input")
        )

        Spacer(modifier = Modifier.width(8.dp))

        Button(
          onClick = {
            if (newItemName.isNotBlank()) {
              onAddItem(newItemName, newItemQty, newItemUnit)
              newItemName = ""
            }
          },
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
          modifier = Modifier
            .height(52.dp)
            .testTag("shopping_add_button")
        ) {
          Icon(Icons.Default.Add, contentDescription = "Add")
        }
      }
    }

    // Move to Fridge Banner if checked items exist
    if (checkedCount > 0) {
      Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = FreshGreen.copy(alpha = 0.12f)),
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 6.dp)
          .testTag("shopping_move_to_fridge_card")
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
          ) {
            Icon(Icons.Default.Kitchen, contentDescription = null, tint = FreshGreen, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Column {
              Text(
                text = "$checkedCount items purchased",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = "Move them directly to your Fridge inventory",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
          Button(
            onClick = onMoveBoughtToFridge,
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = FreshGreen),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            modifier = Modifier.testTag("shopping_move_button")
          ) {
            Text("Move to Fridge", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(4.dp))

    // Shopping List
    if (shoppingList.isEmpty()) {
      EmptyStateView(
        title = "Your shopping list is empty",
        description = "Add grocery items manually above or sync missing ingredients from recipes & meal planner!",
        icon = Icons.Default.ShoppingCart,
        modifier = Modifier.fillMaxSize()
      )
    } else {
      LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 80.dp),
        modifier = Modifier.fillMaxSize()
      ) {
        items(shoppingList, key = { it.id }) { item ->
          Surface(
            shape = RoundedCornerShape(14.dp),
            color = if (item.isChecked) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp,
            modifier = Modifier
              .fillMaxWidth()
              .clickable { onToggleItem(item.id) }
              .testTag("shopping_item_${item.name.lowercase().replace(" ", "_")}")
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
              ) {
                Checkbox(
                  checked = item.isChecked,
                  onCheckedChange = { onToggleItem(item.id) },
                  colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                )

                Spacer(modifier = Modifier.width(6.dp))

                Column {
                  Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodyMedium.copy(
                      fontWeight = if (item.isChecked) FontWeight.Normal else FontWeight.SemiBold,
                      textDecoration = if (item.isChecked) TextDecoration.LineThrough else TextDecoration.None
                    ),
                    color = if (item.isChecked) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    else MaterialTheme.colorScheme.onSurface
                  )
                  if (!item.sourceRecipeTitle.isNullOrBlank()) {
                    Text(
                      text = "For: ${item.sourceRecipeTitle}",
                      style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                  }
                }
              }

              Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                  text = "${if (item.quantity % 1.0 == 0.0) item.quantity.toInt().toString() else item.quantity.toString()} ${item.unit}",
                  style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                IconButton(
                  onClick = { onDeleteItem(item.id) },
                  modifier = Modifier.size(32.dp)
                ) {
                  Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp)
                  )
                }
              }
            }
          }
        }
      }
    }
  }
}
