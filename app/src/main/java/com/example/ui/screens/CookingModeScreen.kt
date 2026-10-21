package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.automirrored.filled.NavigateBefore
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Recipe
import com.example.ui.theme.FreshGreen
import com.example.ui.theme.TerracottaAccent
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CookingModeScreen(
  recipe: Recipe,
  onExit: () -> Unit,
  onMarkCooked: () -> Unit,
  modifier: Modifier = Modifier
) {
  BackHandler { onExit() }

  val totalSteps = recipe.instructions.size
  var currentStepIndex by remember { mutableIntStateOf(0) }
  var isCompleted by remember { mutableStateOf(false) }
  var showIngredientsDrawer by remember { mutableStateOf(false) }

  // Timer logic for steps
  val stepTimerMinutes = recipe.stepTimersMinutes.getOrNull(currentStepIndex) ?: 0
  var timerSecondsLeft by remember(currentStepIndex) { mutableIntStateOf(stepTimerMinutes * 60) }
  var isTimerRunning by remember(currentStepIndex) { mutableStateOf(false) }

  LaunchedEffect(isTimerRunning, timerSecondsLeft) {
    if (isTimerRunning && timerSecondsLeft > 0) {
      delay(1000L)
      timerSecondsLeft--
    } else if (timerSecondsLeft == 0) {
      isTimerRunning = false
    }
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text(
              text = recipe.title,
              style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
              maxLines = 1
            )
            Text(
              text = if (isCompleted) "Recipe Completed" else "Step ${currentStepIndex + 1} of $totalSteps",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        },
        actions = {
          IconButton(onClick = onExit, modifier = Modifier.testTag("cooking_mode_close_button")) {
            Icon(Icons.Default.Close, contentDescription = "Exit Cooking Mode")
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
      )
    },
    modifier = modifier.fillMaxSize()
  ) { paddingValues ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
        .padding(horizontal = 20.dp, vertical = 12.dp),
      verticalArrangement = Arrangement.SpaceBetween
    ) {
      // Progress Indicator
      LinearProgressIndicator(
        progress = {
          if (isCompleted) 1f else (currentStepIndex + 1).toFloat() / totalSteps.coerceAtLeast(1)
        },
        modifier = Modifier
          .fillMaxWidth()
          .height(6.dp)
          .clip(RoundedCornerShape(3.dp)),
        color = MaterialTheme.colorScheme.primary,
        trackColor = MaterialTheme.colorScheme.primaryContainer
      )

      if (isCompleted) {
        // Celebration Completion Screen
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center
        ) {
          Box(
            modifier = Modifier
              .size(90.dp)
              .clip(CircleShape)
              .background(FreshGreen.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.Celebration,
              contentDescription = null,
              tint = FreshGreen,
              modifier = Modifier.size(50.dp)
            )
          }

          Spacer(modifier = Modifier.height(20.dp))

          Text(
            text = "Bon Appétit! 🎉",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
          )

          Spacer(modifier = Modifier.height(8.dp))

          Text(
            text = "You successfully cooked ${recipe.title}!",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
          )

          Spacer(modifier = Modifier.height(6.dp))

          Text(
            text = "Food waste saved. Enjoy your fresh home-cooked meal!",
            style = MaterialTheme.typography.bodySmall,
            color = FreshGreen,
            textAlign = TextAlign.Center
          )

          Spacer(modifier = Modifier.height(30.dp))

          Button(
            onClick = {
              onMarkCooked()
              onExit()
            },
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            modifier = Modifier
              .fillMaxWidth()
              .height(52.dp)
              .testTag("cooking_celebration_done_button")
          ) {
            Icon(Icons.Default.Check, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Mark as Cooked & Deduct Ingredients", fontWeight = FontWeight.Bold)
          }

          Spacer(modifier = Modifier.height(12.dp))

          OutlinedButton(
            onClick = onExit,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
              .fillMaxWidth()
              .height(50.dp)
          ) {
            Text("Return to Recipes")
          }
        }
      } else {
        // Active Step Content
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .padding(vertical = 16.dp),
          verticalArrangement = Arrangement.Center,
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          // Big Step Number
          Box(
            modifier = Modifier
              .size(48.dp)
              .clip(CircleShape)
              .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = "${currentStepIndex + 1}",
              style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.primary
            )
          }

          Spacer(modifier = Modifier.height(20.dp))

          // Big readable instruction text
          Text(
            text = recipe.instructions.getOrElse(currentStepIndex) { "" },
            style = MaterialTheme.typography.headlineSmall.copy(
              lineHeight = 36.sp,
              fontWeight = FontWeight.Medium
            ),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 8.dp)
          )

          // Step Timer if available
          if (stepTimerMinutes > 0) {
            Spacer(modifier = Modifier.height(28.dp))
            Card(
              shape = RoundedCornerShape(20.dp),
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
              elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
              Column(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(Icons.Default.Timer, contentDescription = null, tint = TerracottaAccent)
                  Spacer(modifier = Modifier.width(8.dp))
                  val minutes = timerSecondsLeft / 60
                  val seconds = timerSecondsLeft % 60
                  Text(
                    text = "%02d:%02d".format(minutes, seconds),
                    style = MaterialTheme.typography.headlineMedium.copy(
                      fontWeight = FontWeight.Bold,
                      letterSpacing = 2.sp
                    ),
                    color = if (timerSecondsLeft == 0) FreshGreen else MaterialTheme.colorScheme.onSurface
                  )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                  Button(
                    onClick = { isTimerRunning = !isTimerRunning },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                      containerColor = if (isTimerRunning) MaterialTheme.colorScheme.error else TerracottaAccent
                    )
                  ) {
                    Icon(
                      imageVector = if (isTimerRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                      contentDescription = null,
                      modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isTimerRunning) "Pause" else "Start Timer")
                  }

                  OutlinedButton(
                    onClick = {
                      isTimerRunning = false
                      timerSecondsLeft = stepTimerMinutes * 60
                    },
                    shape = RoundedCornerShape(10.dp)
                  ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Reset", modifier = Modifier.size(16.dp))
                  }
                }
              }
            }
          }

          Spacer(modifier = Modifier.height(20.dp))

          // Toggle Ingredients Reference Drawer
          Surface(
            modifier = Modifier
              .clip(RoundedCornerShape(12.dp))
              .clickable { showIngredientsDrawer = !showIngredientsDrawer },
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(12.dp)
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(Icons.Default.Kitchen, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = if (showIngredientsDrawer) "Hide Ingredients List" else "Peek Ingredients List",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
              )
            }
          }

          AnimatedVisibility(visible = showIngredientsDrawer) {
            Card(
              shape = RoundedCornerShape(14.dp),
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
              modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp)
                .height(140.dp)
            ) {
              LazyColumn(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
              ) {
                items(recipe.ingredients) { ing ->
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                  ) {
                    Text(ing.name, style = MaterialTheme.typography.bodySmall)
                    Text("${ing.quantity} ${ing.unit}", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                  }
                }
              }
            }
          }
        }

        // Bottom Navigation Buttons
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          OutlinedButton(
            onClick = {
              if (currentStepIndex > 0) currentStepIndex--
            },
            enabled = currentStepIndex > 0,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
              .weight(1f)
              .height(52.dp)
              .testTag("cooking_prev_step_button")
          ) {
            Icon(Icons.AutoMirrored.Filled.NavigateBefore, contentDescription = null)
            Spacer(modifier = Modifier.width(4.dp))
            Text("Previous")
          }

          Spacer(modifier = Modifier.width(16.dp))

          Button(
            onClick = {
              if (currentStepIndex < totalSteps - 1) {
                currentStepIndex++
              } else {
                isCompleted = true
              }
            },
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            modifier = Modifier
              .weight(1.2f)
              .height(52.dp)
              .testTag("cooking_next_step_button")
          ) {
            Text(if (currentStepIndex < totalSteps - 1) "Next Step" else "Complete Recipe", fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(4.dp))
            Icon(Icons.AutoMirrored.Filled.NavigateNext, contentDescription = null)
          }
        }
      }
    }
  }
}
