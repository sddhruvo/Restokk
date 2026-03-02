package com.inventory.app.ui.screens.more

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.ImportExport
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import com.inventory.app.ui.components.ThemedScaffold
import androidx.compose.material3.Text
import com.inventory.app.ui.components.ThemedTopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.inventory.app.BuildConfig
import com.inventory.app.R
import com.inventory.app.ui.components.AppCard
import com.inventory.app.ui.components.rememberAiSignInGate
import com.inventory.app.ui.components.StaggeredAnimatedItem
import com.inventory.app.ui.components.ThemedIcon
import com.inventory.app.ui.navigation.Screen
import com.inventory.app.ui.theme.Dimens
import com.inventory.app.ui.theme.appColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreScreen(navController: NavController) {
    val aiGate = rememberAiSignInGate()
    ThemedScaffold(
        topBar = {
            ThemedTopAppBar(title = { Text("More") })
        }
    ) { padding ->
        val context = LocalContext.current
        // Only animate on first entry; skip on back-navigation to preserve scroll
        var hasAnimated by rememberSaveable { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            if (!hasAnimated) {
                kotlinx.coroutines.delay(5 * 50L + 300L)
                hasAnimated = true
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Dimens.spacingLg),
            verticalArrangement = Arrangement.spacedBy(Dimens.spacingLg)
        ) {
            Spacer(modifier = Modifier.height(Dimens.spacingXs))

            // Search — standalone tappable row
            AnimateOnce(index = 0, hasAnimated = hasAnimated) {
                ListItem(
                    headlineContent = { Text("Search") },
                    supportingContent = { Text("Search items, categories, and locations") },
                    leadingContent = {
                        ThemedIcon(
                            materialIcon = Icons.Filled.Search,
                            inkIconRes = R.drawable.ic_ink_search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    modifier = Modifier.clickable {
                        navController.navigate(Screen.GlobalSearch.route)
                    }
                )
            }

            // AI & Kitchen section
            AnimateOnce(index = 1, hasAnimated = hasAnimated) {
                Column(verticalArrangement = Arrangement.spacedBy(Dimens.spacingMd)) {
                    MoreSectionHeader(
                        title = "AI & Kitchen",
                        leadingIcon = Icons.Filled.AutoAwesome,
                        color = MaterialTheme.appColors.accentOrange
                    )
                    AdaptiveGrid(
                        columns = 2,
                        spacing = 12.dp,
                        items = listOf<@Composable (Modifier) -> Unit>(
                            { mod ->
                                MoreActionCard(mod, "Cook", Icons.Filled.Restaurant, MaterialTheme.appColors.accentOrange, "What Can I Cook?") {
                                    navController.navigate(Screen.Cook.route)
                                }
                            },
                            { mod ->
                                MoreActionCard(mod, "Kitchen Scan", Icons.Filled.PhotoCamera, MaterialTheme.appColors.accentGreen) {
                                    aiGate.requireSignIn("identify kitchen items") {
                                        navController.navigate(Screen.FridgeScan.route)
                                    }
                                }
                            },
                            { mod ->
                                MoreActionCard(mod, "Kitchen Map", Icons.Filled.Kitchen, MaterialTheme.appColors.accentGold, "My Kitchen") {
                                    navController.navigate(Screen.KitchenMap.route)
                                }
                            },
                            { mod ->
                                MoreActionCard(mod, "My Recipes", Icons.Filled.MenuBook, MaterialTheme.appColors.accentPurple) {
                                    navController.navigate(Screen.SavedRecipes.route)
                                }
                            },
                            { mod ->
                                MoreActionCard(mod, "Scan Receipt", Icons.Filled.Receipt, MaterialTheme.appColors.accentGreen) {
                                    aiGate.requireSignIn("parse receipts") {
                                        navController.navigate(Screen.ReceiptScan.route)
                                    }
                                }
                            }
                        )
                    )
                }
            }

            // Analytics section
            AnimateOnce(index = 2, hasAnimated = hasAnimated) {
                Column(verticalArrangement = Arrangement.spacedBy(Dimens.spacingMd)) {
                    MoreSectionHeader(title = "Analytics")
                    AdaptiveGrid(
                        columns = 2,
                        spacing = 12.dp,
                        items = listOf<@Composable (Modifier) -> Unit>(
                            { mod ->
                                MoreActionCard(mod, "Reports", Icons.Filled.Assessment, MaterialTheme.appColors.accentBlue) {
                                    navController.navigate(Screen.Reports.route)
                                }
                            },
                            { mod ->
                                MoreActionCard(mod, "Purchases", Icons.Filled.ShoppingBag, MaterialTheme.appColors.accentOrange, "Purchase History") {
                                    navController.navigate(Screen.PurchaseHistory.createRoute())
                                }
                            }
                        )
                    )
                }
            }

            // Organize section
            AnimateOnce(index = 3, hasAnimated = hasAnimated) {
                Column(verticalArrangement = Arrangement.spacedBy(Dimens.spacingMd)) {
                    MoreSectionHeader(title = "Organize")
                    AdaptiveGrid(
                        columns = 2,
                        spacing = 12.dp,
                        items = listOf<@Composable (Modifier) -> Unit>(
                            { mod ->
                                MoreActionCard(mod, "Categories", Icons.Filled.Category, MaterialTheme.appColors.accentBlue) {
                                    navController.navigate(Screen.CategoryList.route)
                                }
                            },
                            { mod ->
                                MoreActionCard(mod, "Locations", Icons.Filled.Place, MaterialTheme.appColors.accentGold, "Storage Locations") {
                                    navController.navigate(Screen.LocationList.route)
                                }
                            }
                        )
                    )
                }
            }

            // App section
            AnimateOnce(index = 4, hasAnimated = hasAnimated) {
                Column(verticalArrangement = Arrangement.spacedBy(Dimens.spacingMd)) {
                    MoreSectionHeader(title = "App")
                    AdaptiveGrid(
                        columns = 2,
                        spacing = 12.dp,
                        items = listOf<@Composable (Modifier) -> Unit>(
                            { mod ->
                                MoreActionCard(mod, "Settings", Icons.Filled.Settings, MaterialTheme.colorScheme.primary) {
                                    navController.navigate(Screen.Settings.route)
                                }
                            },
                            { mod ->
                                MoreActionCard(mod, "Export / Import", Icons.Filled.ImportExport, MaterialTheme.colorScheme.primary) {
                                    navController.navigate(Screen.ExportImport.route)
                                }
                            }
                        )
                    )
                }
            }

            // Feedback section
            AnimateOnce(index = 5, hasAnimated = hasAnimated) {
                Column(verticalArrangement = Arrangement.spacedBy(Dimens.spacingMd)) {
                    MoreSectionHeader(title = "Feedback")
                    AdaptiveGrid(
                        columns = 2,
                        spacing = 12.dp,
                        items = listOf<@Composable (Modifier) -> Unit>(
                            { mod ->
                                MoreActionCard(mod, "Send Feedback", Icons.Filled.Feedback, MaterialTheme.appColors.accentPurple, "Report bugs & ideas") {
                                    launchFeedbackEmail(context)
                                }
                            }
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(Dimens.spacingSm))
        }
    }
}

@Composable
private fun MoreSectionHeader(
    title: String,
    leadingIcon: ImageVector? = null,
    color: Color = MaterialTheme.colorScheme.primary
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (leadingIcon != null) {
            ThemedIcon(
                materialIcon = leadingIcon,
                inkIconRes = when (leadingIcon) {
                    Icons.Filled.AutoAwesome -> R.drawable.ic_ink_sparkle
                    else -> 0
                },
                contentDescription = null,
                modifier = Modifier.size(Dimens.iconSizeSm),
                tint = color
            )
            Spacer(modifier = Modifier.width(6.dp))
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = color
        )
    }
}

@Composable
private fun MoreActionCard(
    modifier: Modifier = Modifier,
    title: String,
    icon: ImageVector,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    AppCard(onClick = onClick, modifier = modifier.height(88.dp)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(Dimens.spacingMd),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            ThemedIcon(
                materialIcon = icon,
                inkIconRes = when (icon) {
                    Icons.Filled.Restaurant -> R.drawable.ic_ink_cook
                    Icons.Filled.PhotoCamera -> R.drawable.ic_ink_camera
                    Icons.Filled.Kitchen -> R.drawable.ic_ink_kitchen
                    Icons.Filled.MenuBook -> R.drawable.ic_ink_book
                    Icons.Filled.Receipt -> R.drawable.ic_ink_receipt
                    Icons.Filled.Assessment -> R.drawable.ic_ink_reports
                    Icons.Filled.ShoppingBag -> R.drawable.ic_ink_shopping
                    Icons.Filled.Category -> R.drawable.ic_ink_category
                    Icons.Filled.Place -> R.drawable.ic_ink_location
                    Icons.Filled.Settings -> R.drawable.ic_ink_settings
                    Icons.Filled.ImportExport -> R.drawable.ic_ink_download
                    Icons.Filled.Feedback -> R.drawable.ic_ink_feedback
                    Icons.Filled.Search -> R.drawable.ic_ink_search
                    else -> 0
                },
                contentDescription = title,
                modifier = Modifier.size(Dimens.iconSizeLg),
                tint = iconTint
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(title, style = MaterialTheme.typography.labelMedium)
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun AnimateOnce(
    index: Int,
    hasAnimated: Boolean,
    content: @Composable () -> Unit
) {
    if (hasAnimated) {
        content()
    } else {
        StaggeredAnimatedItem(index = index, slideOffsetDivisor = 6) {
            content()
        }
    }
}

internal fun launchFeedbackEmail(context: android.content.Context) {
    val deviceInfo = """
        |---
        |Device: ${Build.MANUFACTURER} ${Build.MODEL}
        |Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})
        |App version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})
        |---
        |
        |What happened:
        |
        |
        |What I expected:
        |
        |
        |Steps to reproduce:
        |
    """.trimMargin()

    val intent = Intent(Intent.ACTION_SENDTO).apply {
        data = Uri.parse("mailto:")
        putExtra(Intent.EXTRA_EMAIL, arrayOf("dhruvo012@gmail.com"))
        putExtra(Intent.EXTRA_SUBJECT, "Restokk Beta Feedback — v${BuildConfig.VERSION_NAME}")
        putExtra(Intent.EXTRA_TEXT, deviceInfo)
    }
    try {
        context.startActivity(intent)
    } catch (_: android.content.ActivityNotFoundException) {
        Toast.makeText(context, "No email app found", Toast.LENGTH_SHORT).show()
    }
}

@Composable
private fun AdaptiveGrid(
    columns: Int,
    spacing: Dp,
    items: List<@Composable (Modifier) -> Unit>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(spacing)
    ) {
        items.chunked(columns).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing)
            ) {
                rowItems.forEach { child ->
                    child(Modifier.weight(1f))
                }
                repeat(columns - rowItems.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}
