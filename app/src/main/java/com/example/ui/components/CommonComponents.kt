package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Kitchen
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ExpiredRed
import com.example.ui.theme.FreshGreen
import com.example.ui.theme.WarningOrange

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FridgeTopBar(
  title: String = "What's in My Fridge?",
  remainingQuota: Int,
  onQuotaClick: () -> Unit,
  onCameraClick: () -> Unit,
  onProfileClick: () -> Unit,
  showBackButton: Boolean = false,
  onBackClick: () -> Unit = {}
) {
  TopAppBar(
    title = {
      Row(verticalAlignment = Alignment.CenterVertically) {
        if (!showBackButton) {
          Box(
            modifier = Modifier
              .size(36.dp)
              .clip(RoundedCornerShape(10.dp))
              .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.Kitchen,
              contentDescription = "Logo",
              tint = Color.White,
              modifier = Modifier.size(20.dp)
            )
          }
          Spacer(modifier = Modifier.width(10.dp))
        }
        Text(
          text = title,
          style = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.3).sp
          ),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }
    },
    navigationIcon = {
      if (showBackButton) {
        IconButton(onClick = onBackClick, modifier = Modifier.testTag("top_bar_back_button")) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Go back"
          )
        }
      }
    },
    actions = {
      // AI Quota Pill
      Surface(
        modifier = Modifier
          .clip(RoundedCornerShape(20.dp))
          .clickable { onQuotaClick() }
          .testTag("quota_pill_button"),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
        shape = RoundedCornerShape(20.dp)
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
            imageVector = Icons.Default.AutoAwesome,
            contentDescription = "AI Quota",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(14.dp)
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = "$remainingQuota AI",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onPrimaryContainer
          )
        }
      }

      Spacer(modifier = Modifier.width(4.dp))

      IconButton(onClick = onCameraClick, modifier = Modifier.testTag("top_bar_camera_button")) {
        Icon(
          imageVector = Icons.Default.CameraAlt,
          contentDescription = "Scan Fridge Photo",
          tint = MaterialTheme.colorScheme.primary
        )
      }

      IconButton(onClick = onProfileClick, modifier = Modifier.testTag("top_bar_profile_button")) {
        Icon(
          imageVector = Icons.Default.Person,
          contentDescription = "Profile & Settings",
          tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    },
    colors = TopAppBarDefaults.topAppBarColors(
      containerColor = MaterialTheme.colorScheme.background
    )
  )
}

@Composable
fun BottomNavBar(
  selectedTab: Int,
  onTabSelected: (Int) -> Unit
) {
  val items = listOf(
    Triple("Home", Icons.Filled.Home, Icons.Outlined.Home),
    Triple("My Fridge", Icons.Filled.Kitchen, Icons.Outlined.Kitchen),
    Triple("Recipes", Icons.AutoMirrored.Filled.MenuBook, Icons.AutoMirrored.Outlined.MenuBook),
    Triple("Planner", Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth),
    Triple("Shopping", Icons.Filled.ShoppingCart, Icons.Outlined.ShoppingCart)
  )

  NavigationBar(
    containerColor = MaterialTheme.colorScheme.surface,
    tonalElevation = 8.dp
  ) {
    items.forEachIndexed { index, item ->
      val isSelected = selectedTab == index
      NavigationBarItem(
        icon = {
          Icon(
            imageVector = if (isSelected) item.second else item.third,
            contentDescription = item.first
          )
        },
        label = {
          Text(
            text = item.first,
            style = MaterialTheme.typography.labelSmall.copy(
              fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
          )
        },
        selected = isSelected,
        onClick = { onTabSelected(index) },
        colors = NavigationBarItemDefaults.colors(
          indicatorColor = MaterialTheme.colorScheme.primaryContainer,
          selectedIconColor = MaterialTheme.colorScheme.primary,
          selectedTextColor = MaterialTheme.colorScheme.primary,
          unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
          unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        modifier = Modifier.testTag("nav_item_${item.first.lowercase().replace(" ", "_")}")
      )
    }
  }
}

@Composable
fun StatCard(
  title: String,
  value: String,
  icon: ImageVector,
  iconTint: Color,
  modifier: Modifier = Modifier,
  onClick: (() -> Unit)? = null
) {
  Card(
    modifier = modifier
      .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surface
    ),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
      ) {
        Text(
          text = title,
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Box(
          modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(iconTint.copy(alpha = 0.15f)),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(16.dp)
          )
        }
      }
      Spacer(modifier = Modifier.height(8.dp))
      Text(
        text = value,
        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onSurface
      )
    }
  }
}

@Composable
fun ExpiryBadge(daysUntilExpiry: Int?, modifier: Modifier = Modifier) {
  if (daysUntilExpiry == null) return

  val (bgColor, textColor, label) = when {
    daysUntilExpiry < 0 -> Triple(ExpiredRed.copy(alpha = 0.15f), ExpiredRed, "Expired")
    daysUntilExpiry == 0 -> Triple(ExpiredRed.copy(alpha = 0.15f), ExpiredRed, "Today!")
    daysUntilExpiry == 1 -> Triple(WarningOrange.copy(alpha = 0.18f), WarningOrange, "Tomorrow")
    daysUntilExpiry in 2..3 -> Triple(WarningOrange.copy(alpha = 0.15f), WarningOrange, "$daysUntilExpiry days left")
    else -> Triple(FreshGreen.copy(alpha = 0.12f), FreshGreen, "$daysUntilExpiry days")
  }

  Surface(
    modifier = modifier.clip(RoundedCornerShape(8.dp)),
    color = bgColor,
    shape = RoundedCornerShape(8.dp)
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
      color = textColor,
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
    )
  }
}

@Composable
fun EmptyStateView(
  title: String,
  description: String,
  icon: ImageVector,
  buttonLabel: String? = null,
  onButtonClick: (() -> Unit)? = null,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier
      .fillMaxWidth()
      .padding(32.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center
  ) {
    Box(
      modifier = Modifier
        .size(72.dp)
        .clip(CircleShape)
        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(36.dp)
      )
    }
    Spacer(modifier = Modifier.height(16.dp))
    Text(
      text = title,
      style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
      color = MaterialTheme.colorScheme.onSurface,
      textAlign = TextAlign.Center
    )
    Spacer(modifier = Modifier.height(6.dp))
    Text(
      text = description,
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      textAlign = TextAlign.Center
    )
    if (buttonLabel != null && onButtonClick != null) {
      Spacer(modifier = Modifier.height(20.dp))
      Button(
        onClick = onButtonClick,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.testTag("empty_state_action_button")
      ) {
        Text(buttonLabel)
      }
    }
  }
}
