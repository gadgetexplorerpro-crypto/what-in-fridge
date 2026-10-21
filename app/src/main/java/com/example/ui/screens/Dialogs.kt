package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Ingredient
import com.example.data.model.IngredientCategory
import com.example.data.model.StorageLocation
import com.example.data.model.SubscriptionTier
import com.example.data.remote.DetectedIngredient
import com.example.ui.theme.FreshGreen
import com.example.ui.theme.TerracottaAccent

@Composable
fun PhotoScannerBottomSheet(
  isScanning: Boolean,
  onDismiss: () -> Unit,
  onImageSelected: (Bitmap) -> Unit
) {
  val context = LocalContext.current

  val photoPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia()
  ) { uri: Uri? ->
    if (uri != null) {
      try {
        val inputStream = context.contentResolver.openInputStream(uri)
        val bitmap = BitmapFactory.decodeStream(inputStream)
        if (bitmap != null) {
          onImageSelected(bitmap)
        }
      } catch (e: Exception) {
        // Fallback demo bitmap
        val demoBitmap = Bitmap.createBitmap(500, 500, Bitmap.Config.ARGB_8888)
        onImageSelected(demoBitmap)
      }
    }
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.CameraAlt, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(8.dp))
        Text("AI Fridge & Grocery Scanner", fontWeight = FontWeight.Bold)
      }
    },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        if (isScanning) {
          CircularProgressIndicator(
            modifier = Modifier.size(48.dp),
            color = MaterialTheme.colorScheme.primary
          )
          Spacer(modifier = Modifier.height(16.dp))
          Text(
            text = "Analyzing image with Gemini Vision...",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
          )
          Text(
            text = "Detecting fresh produce, dairy, perishables, and pantry staples",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
          )
        } else {
          Text(
            text = "Take a photo or pick an image of your refrigerator shelves, countertop, or grocery receipt. Gemini AI will identify the ingredients automatically.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Spacer(modifier = Modifier.height(20.dp))
          Button(
            onClick = {
              photoPickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
              )
            },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
              .fillMaxWidth()
              .height(50.dp)
              .testTag("dialog_pick_photo_button")
          ) {
            Icon(Icons.Default.Image, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Select Kitchen Photo")
          }
          Spacer(modifier = Modifier.height(10.dp))
          OutlinedButton(
            onClick = {
              // Quick demo scan with synthetic sample bitmap
              val sampleBitmap = Bitmap.createBitmap(400, 400, Bitmap.Config.ARGB_8888)
              onImageSelected(sampleBitmap)
            },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
              .fillMaxWidth()
              .height(48.dp)
              .testTag("dialog_demo_scan_button")
          ) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Try Sample Fridge Scan")
          }
        }
      }
    },
    confirmButton = {},
    dismissButton = {
      if (!isScanning) {
        TextButton(onClick = onDismiss) { Text("Cancel") }
      }
    }
  )
}

@Composable
fun ScanConfirmationDialog(
  candidates: List<DetectedIngredient>,
  onConfirm: (List<DetectedIngredient>) -> Unit,
  onDismiss: () -> Unit
) {
  val selectedMap = remember {
    mutableStateMapOf<Int, Boolean>().apply {
      candidates.indices.forEach { put(it, true) }
    }
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Column {
        Text("Review Detected Ingredients", fontWeight = FontWeight.Bold)
        Text(
          "Uncheck any items you don't have, or tap Confirm to save to inventory.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    },
    text = {
      LazyColumn(
        modifier = Modifier
          .fillMaxWidth()
          .height(320.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        items(candidates.indices.toList()) { index ->
          val item = candidates[index]
          val isChecked = selectedMap[index] ?: true

          Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (isChecked) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            tonalElevation = 1.dp,
            modifier = Modifier
              .fillMaxWidth()
              .clickable { selectedMap[index] = !isChecked }
          ) {
            Row(
              modifier = Modifier.padding(10.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Checkbox(
                checked = isChecked,
                onCheckedChange = { selectedMap[index] = it },
                colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = item.name,
                  style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                  text = "${item.quantity} ${item.unit} • ${item.storageLocation} • expires ~${item.estimatedExpiryDays}d",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
              Surface(
                shape = RoundedCornerShape(6.dp),
                color = FreshGreen.copy(alpha = 0.12f)
              ) {
                Text(
                  text = "${(item.confidence * 100).toInt()}%",
                  style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                  color = FreshGreen,
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
              }
            }
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val confirmedList = candidates.filterIndexed { idx, _ -> selectedMap[idx] == true }
          onConfirm(confirmedList)
        },
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.testTag("scan_dialog_confirm_button")
      ) {
        val count = selectedMap.values.count { it }
        Text("Add $count to Fridge")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Cancel") }
    }
  )
}

@Composable
fun AddEditIngredientDialog(
  existingIngredient: Ingredient?,
  onDismiss: () -> Unit,
  onSave: (
    id: String?,
    name: String,
    quantity: Double,
    unit: String,
    category: String,
    storageLocation: String,
    expiryDays: Int?,
    notes: String
  ) -> Unit
) {
  var name by remember { mutableStateOf(existingIngredient?.name ?: "") }
  var quantityStr by remember { mutableStateOf(existingIngredient?.quantity?.toString() ?: "1.0") }
  var unit by remember { mutableStateOf(existingIngredient?.unit ?: "pcs") }
  var selectedCategory by remember { mutableStateOf(existingIngredient?.category ?: IngredientCategory.PRODUCE.name) }
  var selectedStorage by remember { mutableStateOf(existingIngredient?.storageLocation ?: StorageLocation.FRIDGE.name) }
  var expiryDaysStr by remember {
    mutableStateOf(
      existingIngredient?.daysUntilExpiry()?.let { if (it > 0) it.toString() else "7" } ?: "7"
    )
  }
  var notes by remember { mutableStateOf(existingIngredient?.notes ?: "") }

  val unitOptions = listOf("pcs", "g", "kg", "ml", "l", "cups", "tbsp", "tsp", "bunch", "bulb", "loaf")

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(if (existingIngredient != null) "Edit Ingredient" else "Add New Ingredient", fontWeight = FontWeight.Bold)
    },
    text = {
      LazyColumn(
        modifier = Modifier
          .fillMaxWidth()
          .height(360.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        item {
          OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Ingredient Name") },
            placeholder = { Text("e.g. Greek Yogurt, Eggs, Tomato") },
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("input_ingredient_name")
          )
        }

        item {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            OutlinedTextField(
              value = quantityStr,
              onValueChange = { quantityStr = it },
              label = { Text("Qty") },
              shape = RoundedCornerShape(12.dp),
              singleLine = true,
              modifier = Modifier
                .weight(1f)
                .testTag("input_ingredient_qty")
            )
            OutlinedTextField(
              value = unit,
              onValueChange = { unit = it },
              label = { Text("Unit") },
              shape = RoundedCornerShape(12.dp),
              singleLine = true,
              modifier = Modifier
                .weight(1f)
                .testTag("input_ingredient_unit")
            )
          }
        }

        item {
          Text("Storage Location", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
          Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            StorageLocation.values().forEach { loc ->
              FilterChip(
                selected = selectedStorage == loc.name,
                onClick = { selectedStorage = loc.name },
                label = { Text(loc.displayName) },
                shape = RoundedCornerShape(12.dp)
              )
            }
          }
        }

        item {
          Text("Category", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
          LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(IngredientCategory.values()) { cat ->
              FilterChip(
                selected = selectedCategory == cat.name,
                onClick = { selectedCategory = cat.name },
                label = { Text(cat.displayName, style = MaterialTheme.typography.labelSmall) },
                shape = RoundedCornerShape(12.dp)
              )
            }
          }
        }

        item {
          OutlinedTextField(
            value = expiryDaysStr,
            onValueChange = { expiryDaysStr = it },
            label = { Text("Days until expiry") },
            placeholder = { Text("e.g. 7") },
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("input_ingredient_expiry")
          )
        }

        item {
          OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text("Notes (optional)") },
            placeholder = { Text("e.g. Opened yesterday") },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
          )
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (name.isNotBlank()) {
            val qty = quantityStr.toDoubleOrNull() ?: 1.0
            val expDays = expiryDaysStr.toIntOrNull() ?: 7
            onSave(
              existingIngredient?.id,
              name,
              qty,
              unit,
              selectedCategory,
              selectedStorage,
              expDays,
              notes
            )
          }
        },
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.testTag("dialog_ingredient_save_button")
      ) {
        Text("Save")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Cancel") }
    }
  )
}

@Composable
fun RecipeGeneratorDialog(
  availableIngredients: List<Ingredient>,
  onDismiss: () -> Unit,
  onGenerate: (
    prioritizeExpiring: Boolean,
    cuisine: String,
    maxTime: Int,
    difficulty: String,
    mealType: String
  ) -> Unit
) {
  var prioritizeExpiring by remember { mutableStateOf(true) }
  var selectedCuisine by remember { mutableStateOf("Any") }
  var maxTime by remember { mutableIntStateOf(30) }
  var difficulty by remember { mutableStateOf("Easy") }
  var mealType by remember { mutableStateOf("Dinner") }

  val cuisines = listOf("Any", "Mediterranean", "Italian", "American", "Asian", "Mexican", "French")
  val times = listOf(15, 25, 35, 50)
  val mealTypes = listOf("Dinner", "Lunch", "Breakfast", "Snack")

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = TerracottaAccent)
        Spacer(modifier = Modifier.width(8.dp))
        Text("AI Recipe Generator", fontWeight = FontWeight.Bold)
      }
    },
    text = {
      LazyColumn(
        modifier = Modifier
          .fillMaxWidth()
          .height(340.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        item {
          Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Checkbox(
                checked = prioritizeExpiring,
                onCheckedChange = { prioritizeExpiring = it },
                colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Column {
                Text("Prioritize Expiring Ingredients", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                Text("Reduces waste by cooking near-expiry items first", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
              }
            }
          }
        }

        item {
          Text("Meal Type", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
          Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            mealTypes.forEach { type ->
              FilterChip(
                selected = mealType == type,
                onClick = { mealType = type },
                label = { Text(type) },
                shape = RoundedCornerShape(12.dp)
              )
            }
          }
        }

        item {
          Text("Cuisine", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
          LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(cuisines) { c ->
              FilterChip(
                selected = selectedCuisine == c,
                onClick = { selectedCuisine = c },
                label = { Text(c) },
                shape = RoundedCornerShape(12.dp)
              )
            }
          }
        }

        item {
          Text("Max Cooking Time", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            times.forEach { t ->
              FilterChip(
                selected = maxTime == t,
                onClick = { maxTime = t },
                label = { Text("${t}m") },
                shape = RoundedCornerShape(12.dp)
              )
            }
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          onGenerate(prioritizeExpiring, selectedCuisine, maxTime, difficulty, mealType)
        },
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(containerColor = TerracottaAccent),
        modifier = Modifier.testTag("wizard_generate_button")
      ) {
        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("Generate 3 Recipes")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Cancel") }
    }
  )
}

@Composable
fun UpgradeSubscriptionDialog(
  currentTier: String,
  onSelectTier: (SubscriptionTier) -> Unit,
  onDismiss: () -> Unit
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Star, contentDescription = null, tint = TerracottaAccent)
        Spacer(modifier = Modifier.width(8.dp))
        Text("Subscription & Usage Plans", fontWeight = FontWeight.Bold)
      }
    },
    text = {
      LazyColumn(
        modifier = Modifier
          .fillMaxWidth()
          .height(350.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        items(SubscriptionTier.values()) { tier ->
          val isCurrent = currentTier.equals(tier.name, ignoreCase = true)
          Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(
              containerColor = if (isCurrent) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
              else MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(14.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(tier.tierName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                Surface(
                  shape = RoundedCornerShape(6.dp),
                  color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer
                ) {
                  Text(
                    text = tier.priceLabel,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = if (isCurrent) Color.White else MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                  )
                }
              }

              Spacer(modifier = Modifier.height(6.dp))
              Text(tier.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
              Spacer(modifier = Modifier.height(8.dp))
              tier.perks.forEach { perk ->
                Row(modifier = Modifier.padding(vertical = 1.dp)) {
                  Icon(Icons.Default.Check, contentDescription = null, tint = FreshGreen, modifier = Modifier.size(14.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(perk, style = MaterialTheme.typography.labelSmall)
                }
              }
              Spacer(modifier = Modifier.height(10.dp))
              if (!isCurrent) {
                Button(
                  onClick = { onSelectTier(tier) },
                  shape = RoundedCornerShape(8.dp),
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Text("Switch to ${tier.tierName}")
                }
              } else {
                Surface(
                  shape = RoundedCornerShape(8.dp),
                  color = FreshGreen.copy(alpha = 0.15f),
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Text(
                    text = "Current Active Plan",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = FreshGreen,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 6.dp)
                  )
                }
              }
            }
          }
        }
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss) { Text("Close") }
    }
  )
}

@Composable
fun ApiKeyDialog(
  currentKey: String,
  onSaveKey: (String) -> Unit,
  onDismiss: () -> Unit
) {
  var keyInput by remember { mutableStateOf(currentKey) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Key, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(8.dp))
        Text("Gemini API Key Configuration", fontWeight = FontWeight.Bold)
      }
    },
    text = {
      Column(modifier = Modifier.fillMaxWidth()) {
        Text(
          text = "By default, What's in My Fridge? uses the secure AI Studio backend environment. You may also provide a custom Gemini API key for local development or direct testing.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
          value = keyInput,
          onValueChange = { keyInput = it },
          label = { Text("Custom Gemini API Key") },
          placeholder = { Text("AIzaSy...") },
          shape = RoundedCornerShape(12.dp),
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("dialog_api_key_input")
        )
      }
    },
    confirmButton = {
      Button(
        onClick = { onSaveKey(keyInput) },
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.testTag("dialog_api_key_save_button")
      ) {
        Text("Save Key")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Cancel") }
    }
  )
}

@Composable
fun PrivacyPolicyDialog(
  onDismiss: () -> Unit
) {
  val context = LocalContext.current
  val privacyUrl = "https://ais-pre-jgqgdcylhg5ntlbgcpar25-861509315491.asia-east1.run.app/privacy"

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Policy, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(8.dp))
        Text("Privacy Policy", fontWeight = FontWeight.Bold)
      }
    },
    text = {
      LazyColumn(
        modifier = Modifier
          .fillMaxWidth()
          .height(380.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        // Google Play Compliant Hosting Card
        item {
          Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f))
          ) {
            Column(modifier = Modifier.padding(14.dp)) {
              Text(
                text = "Google Play Policy Declaration",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onPrimaryContainer
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = "Google Play requires a permanent public web URL returning HTTP 200 (not an emulator stream). You can host the provided policy for free on Google Sites or GitHub Gist in 1 minute.",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = MaterialTheme.colorScheme.onPrimaryContainer
              )
              Spacer(modifier = Modifier.height(10.dp))
              Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                  onClick = {
                    val fullPolicy = """
                      Privacy Policy for "What's in My Fridge?"
                      Application ID: com.aistudio.whatsinmyfridge.qknz
                      Developer Contact: gadgetexplorerpro@gmail.com
                      Effective Date: October 21, 2026

                      1. Introduction
                      What's in My Fridge? is committed to protecting your privacy. This policy describes our data handling practices.

                      2. Information We Collect
                      • Kitchen Inventory: Stored locally on your device.
                      • Camera & Photo Gallery: Accessed strictly when you choose to scan fridge items. Photos are processed ephemerally by Google Gemini API to identify food items and are not retained or used for biometric identification.
                      • No Tracking: We do not collect location data, advertising IDs, or sell user information.

                      3. Third-Party Services
                      • Google Gemini AI: Encrypted API calls solely for recipe generation and ingredient detection.

                      4. Data Control & Deletion
                      • You can delete all data at any time via Settings > Reset All Data.
                      • Uninstalling the app completely removes all stored data.

                      5. Contact
                      Email: gadgetexplorerpro@gmail.com
                    """.trimIndent()
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("What's in My Fridge? Privacy Policy", fullPolicy)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(context, "Full Privacy Policy text copied to clipboard!", Toast.LENGTH_LONG).show()
                  },
                  shape = RoundedCornerShape(8.dp),
                  contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                  modifier = Modifier.testTag("privacy_copy_full_text_button")
                ) {
                  Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("Copy Full Policy Text", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                }

                OutlinedButton(
                  onClick = {
                    try {
                      val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://sites.google.com/new"))
                      context.startActivity(intent)
                    } catch (e: Exception) {
                      Toast.makeText(context, "Could not open browser", Toast.LENGTH_SHORT).show()
                    }
                  },
                  shape = RoundedCornerShape(8.dp),
                  contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                  modifier = Modifier.testTag("privacy_open_google_sites_button")
                ) {
                  Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(14.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("Open Google Sites", style = MaterialTheme.typography.labelSmall)
                }
              }
            }
          }
        }

        item {
          Text(
            text = "Effective Date: October 14, 2026\nApplication: What's in My Fridge? (Version 4.0)",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        item {
          Text("1. Overview", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
          Text(
            text = "What's in My Fridge? (\"we\", \"our\", or \"app\") is committed to protecting your privacy and personal data. This Privacy Policy details how we handle information, device permissions, and AI data processing in compliance with Google Play Developer Policies and global privacy regulations.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        item {
          Text("2. Information We Collect", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
          Text(
            text = "• Kitchen Inventory: Ingredient names, quantities, categories, storage zones (fridge, freezer, pantry), and expiration dates you enter or confirm.\n" +
                "• Meal Plans & Shopping Lists: Recipes you schedule and items you add to your grocery list.\n" +
                "• Dietary Preferences & Allergies: Dietary lifestyle choices (e.g. Vegetarian, Keto) and allergen exclusions (e.g. Peanuts, Gluten) configured in your profile.\n" +
                "• Camera / Photos: Images you choose to upload for AI ingredient scanning. Photos are processed ephemerally solely to recognize visible food items and are not retained or used for biometric identification.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        item {
          Text("3. Device Permissions & Zero-Tracking", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
          Text(
            text = "• Camera & Media Access: Used strictly when you trigger the 'Scan Fridge' feature. We use the modern Android Photo Picker which operates with zero broad media storage permissions.\n" +
                "• Internet Access: Required to communicate with the Gemini AI model for ingredient image analysis and recipe generation.\n" +
                "• No Location Tracking: We never collect, track, or share your GPS or location data.\n" +
                "• No Ad Identifiers: We do not track you across third-party apps or websites, and we do not sell your personal data to data brokers.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        item {
          Text("4. Artificial Intelligence & Gemini API Processing", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
          Text(
            text = "Our AI features are powered by Google Gemini 3.5 Flash. Transmitted text queries (available ingredient names, dietary preferences) and images are processed securely under Google Cloud and Google AI Studio terms. Transmitted data is not used to train public generative models without your consent.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        item {
          Text("5. Data Retention & Your Right to Delete", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
          Text(
            text = "All your fridge items, recipes, preferences, and meal plans are stored locally on your device in secure app-private storage. You have complete control to wipe all data at any moment using the 'Reset All Data to Defaults' action in the Profile Settings screen.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        item {
          Text("6. Contact & Data Protection", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
          Text(
            text = "If you have questions, inquiries, or data requests regarding this Privacy Policy, please contact our privacy officer at:\nEmail: gadgetexplorerpro@gmail.com\nWebsite: https://ais-pre-jgqgdcylhg5ntlbgcpar25-861509315491.asia-east1.run.app",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
    },
    confirmButton = {
      Button(onClick = onDismiss, shape = RoundedCornerShape(10.dp)) {
        Text("Done")
      }
    }
  )
}

