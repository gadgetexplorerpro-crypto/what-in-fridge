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
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.data.model.SubscriptionTier
import com.example.data.model.UserPreferences
import com.example.ui.theme.FreshGreen
import com.example.ui.theme.TerracottaAccent

@Composable
fun ProfileSettingsScreen(
  userPreferences: UserPreferences,
  onUpdatePreferences: (diet: String, allergies: List<String>, householdSize: Int, skill: String, maxTime: Int) -> Unit,
  onOpenUpgradeDialog: () -> Unit,
  onOpenApiKeyDialog: () -> Unit,
  onOpenPrivacyPolicy: () -> Unit = {},
  onResetQuota: () -> Unit,
  onClearAllData: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  var selectedDiet by remember { mutableStateOf(userPreferences.dietaryPreference) }
  var selectedAllergies by remember { mutableStateOf(userPreferences.allergiesToAvoid.toSet()) }
  var householdSize by remember { mutableIntStateOf(userPreferences.householdSize) }
  var cookingSkill by remember { mutableStateOf(userPreferences.cookingSkill) }
  var showResetConfirmDialog by remember { mutableStateOf(false) }

  val dietOptions = listOf("Omnivore", "Vegetarian", "Vegan", "Pescatarian", "Keto", "Low Carb")
  val allergenOptions = listOf("Gluten", "Dairy", "Peanuts", "Tree Nuts", "Shellfish", "Soy", "Eggs")
  val skillOptions = listOf("Beginner", "Intermediate", "Advanced")

  val currentTier = try {
    SubscriptionTier.valueOf(userPreferences.tier)
  } catch (_: Exception) {
    SubscriptionTier.FREE
  }

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
    contentPadding = PaddingValues(top = 8.dp, bottom = 40.dp)
  ) {
    // 1. Profile Header
    item {
      Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier.padding(18.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Box(
            modifier = Modifier
              .size(60.dp)
              .clip(CircleShape)
              .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.Person,
              contentDescription = null,
              tint = Color.White,
              modifier = Modifier.size(32.dp)
            )
          }

          Spacer(modifier = Modifier.width(16.dp))

          Column {
            Text(
              text = "Chef & Kitchen Household",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onSurface
            )
            Text(
              text = "Plan: ${currentTier.tierName}",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.primary
            )
            Text(
              text = "Household size: $householdSize person${if (householdSize > 1) "s" else ""}",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      }
    }

    // 2. Subscription & AI Quota Tier Card
    item {
      Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("profile_tier_card")
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = currentTier.tierName,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onPrimaryContainer
              )
            }
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = MaterialTheme.colorScheme.primary
            ) {
              Text(
                text = "${userPreferences.remainingAiGenerations} left today",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
              )
            }
          }

          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = currentTier.description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer
          )

          Spacer(modifier = Modifier.height(12.dp))
          Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
              onClick = onOpenUpgradeDialog,
              shape = RoundedCornerShape(10.dp),
              colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
              modifier = Modifier.testTag("profile_upgrade_button")
            ) {
              Text("Manage Subscription")
            }

            OutlinedButton(
              onClick = onResetQuota,
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.testTag("profile_reset_quota_button")
            ) {
              Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Reset Quota")
            }
          }
        }
      }
    }

    // 3. Dietary Preferences
    item {
      Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Text(
            text = "Dietary Preference",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
          )
          Spacer(modifier = Modifier.height(8.dp))
          LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(dietOptions) { diet ->
              FilterChip(
                selected = selectedDiet == diet,
                onClick = {
                  selectedDiet = diet
                  onUpdatePreferences(selectedDiet, selectedAllergies.toList(), householdSize, cookingSkill, userPreferences.maxCookingTimeMinutes)
                },
                label = { Text(diet) },
                shape = RoundedCornerShape(14.dp),
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = MaterialTheme.colorScheme.primary,
                  selectedLabelColor = Color.White
                )
              )
            }
          }
        }
      }
    }

    // 4. Allergens & Exclusions
    item {
      Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Text(
            text = "Allergies to Avoid",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "AI recipes will strictly avoid and substitute these allergens.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Spacer(modifier = Modifier.height(8.dp))
          LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(allergenOptions) { allergen ->
              val isChecked = selectedAllergies.contains(allergen)
              FilterChip(
                selected = isChecked,
                onClick = {
                  selectedAllergies = if (isChecked) selectedAllergies - allergen else selectedAllergies + allergen
                  onUpdatePreferences(selectedDiet, selectedAllergies.toList(), householdSize, cookingSkill, userPreferences.maxCookingTimeMinutes)
                },
                label = { Text(allergen) },
                shape = RoundedCornerShape(14.dp),
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = TerracottaAccent,
                  selectedLabelColor = Color.White
                )
              )
            }
          }
        }
      }
    }

    // 5. Cooking Skill & Household
    item {
      Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Text(
            text = "Cooking Skill Level",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
          )
          Spacer(modifier = Modifier.height(8.dp))
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            skillOptions.forEach { skill ->
              FilterChip(
                selected = cookingSkill == skill,
                onClick = {
                  cookingSkill = skill
                  onUpdatePreferences(selectedDiet, selectedAllergies.toList(), householdSize, cookingSkill, userPreferences.maxCookingTimeMinutes)
                },
                label = { Text(skill) },
                shape = RoundedCornerShape(14.dp),
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                  selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
              )
            }
          }

          Spacer(modifier = Modifier.height(14.dp))

          Text(
            text = "Household Size",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
          )
          Spacer(modifier = Modifier.height(6.dp))
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(1, 2, 3, 4, 6).forEach { size ->
              FilterChip(
                selected = householdSize == size,
                onClick = {
                  householdSize = size
                  onUpdatePreferences(selectedDiet, selectedAllergies.toList(), householdSize, cookingSkill, userPreferences.maxCookingTimeMinutes)
                },
                label = { Text("$size person${if (size > 1) "s" else ""}") },
                shape = RoundedCornerShape(14.dp)
              )
            }
          }
        }
      }
    }

    // 6. Gemini API Key Configuration
    item {
      Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.Key, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "Gemini AI Engine",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
              )
            }
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = FreshGreen.copy(alpha = 0.15f)
            ) {
              Text(
                text = "Active",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = FreshGreen,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
              )
            }
          }
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = "Powered by Google Gemini 3.5 Flash for multimodal fridge photo analysis and culinary intelligence.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Spacer(modifier = Modifier.height(10.dp))
          Button(
            onClick = onOpenApiKeyDialog,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.testTag("profile_api_key_button")
          ) {
            Text("Configure API Key")
          }
        }
      }
    }

    // 7. Food Safety Guide
    item {
      Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Security, contentDescription = null, tint = FreshGreen)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Food Safety & Storage Guidelines",
              style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
            )
          }
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = "• Refrigerator: Keep below 4°C (40°F) to prevent bacterial growth.\n" +
                "• Top Shelves: Ready-to-eat foods, dairy, leftovers.\n" +
                "• Crisper Drawer: High humidity for leafy greens, low humidity for fruit.\n" +
                "• Bottom Shelf: Raw meats sealed to prevent cross-contamination.\n" +
                "• When in doubt: Check smell, texture, and never consume questionable perishables.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 20.sp
          )
        }
      }
    }

    // 8. Privacy & Legal Policy
    item {
      Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("profile_privacy_card")
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.Policy, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "Privacy Policy & Security",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
              )
            }
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = FreshGreen.copy(alpha = 0.15f)
            ) {
              Text(
                text = "Play Store Verified",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = FreshGreen,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
              )
            }
          }
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = "Your kitchen inventory, meal plans, and preferences stay on your device. Zero advertising trackers, no personal data selling, and camera access is strictly user-controlled.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Spacer(modifier = Modifier.height(10.dp))
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
              onClick = onOpenPrivacyPolicy,
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.testTag("profile_view_privacy_button")
            ) {
              Icon(Icons.Default.Policy, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("View & Setup Policy")
            }

            OutlinedButton(
              onClick = {
                val fullPolicy = """
                  Privacy Policy for "What's in My Fridge?"
                  Application ID: com.aistudio.whatsinmyfridge.qknz
                  Developer Contact: gadgetexplorerpro@gmail.com
                  Effective Date: October 21, 2026

                  1. Introduction
                  What's in My Fridge? is committed to protecting your privacy.

                  2. Information We Collect
                  • Kitchen Inventory: Stored locally on your device.
                  • Camera & Photos: Used solely when you scan fridge items. Photos are processed ephemerally by Google Gemini API and are not retained or used for biometric identification.
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
                val clip = ClipData.newPlainText("Privacy Policy", fullPolicy)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(context, "Full Privacy Policy copied! Paste into Google Sites or Gist.", Toast.LENGTH_LONG).show()
              },
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.testTag("profile_copy_privacy_link_button")
            ) {
              Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Copy Policy Text")
            }
          }
        }
      }
    }

    // 9. Danger Zone / Reset
    item {
      Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Text(
            text = "Data Management",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
          )
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = "Clear all inventory, custom recipes, meal plans, and shopping list to restart fresh.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Spacer(modifier = Modifier.height(10.dp))
          Button(
            onClick = { showResetConfirmDialog = true },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.testTag("profile_clear_data_button")
          ) {
            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Reset All Data to Defaults")
          }
        }
      }
    }
  }

  if (showResetConfirmDialog) {
    AlertDialog(
      onDismissRequest = { showResetConfirmDialog = false },
      title = { Text("Reset All Data?") },
      text = { Text("This will reset your fridge inventory, saved recipes, and meal plans back to sample defaults.") },
      confirmButton = {
        Button(
          onClick = {
            onClearAllData()
            showResetConfirmDialog = false
          },
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
          Text("Reset")
        }
      },
      dismissButton = {
        TextButton(onClick = { showResetConfirmDialog = false }) {
          Text("Cancel")
        }
      }
    )
  }
}
