package com.inventory.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.*
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.inventory.app.data.repository.AnalyticsRepository
import com.inventory.app.data.repository.AuthRepository
import com.inventory.app.data.repository.CategoryRepository
import com.inventory.app.data.repository.SettingsRepository
import com.inventory.app.data.repository.ItemRepository
import com.inventory.app.data.repository.ShoppingListRepository
import androidx.compose.runtime.CompositionLocalProvider
import com.inventory.app.ui.navigation.AppNavigation
import com.inventory.app.ui.navigation.BottomNavBar
import com.inventory.app.ui.navigation.QuickAddMenuOverlay
import com.inventory.app.ui.navigation.Screen
import com.inventory.app.ui.screens.onboarding.OnboardingViewModel
import com.inventory.app.ui.screens.shopping.AddShoppingItemSheet
import com.inventory.app.ui.screens.shopping.LocalShowAddShoppingSheet
import com.inventory.app.ui.screens.shopping.SheetRequest
import com.inventory.app.ui.theme.AppTheme
import com.inventory.app.ui.theme.HomeInventoryTheme
import com.inventory.app.worker.SmartNotificationWorker
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var shoppingListRepository: ShoppingListRepository

    @Inject
    lateinit var itemRepository: ItemRepository

    @Inject
    lateinit var analyticsRepository: AnalyticsRepository

    @Inject
    lateinit var categoryRepository: CategoryRepository

    @Inject
    lateinit var authRepository: AuthRepository

    // Notification deep link route (set from intent extras)
    private var pendingNavRoute: String? = null

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val route = intent.getStringExtra(SmartNotificationWorker.EXTRA_NAV_ROUTE)
        if (route != null) {
            pendingNavRoute = route
            setIntent(intent)
        }
    }

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pendingNavRoute = intent.getStringExtra(SmartNotificationWorker.EXTRA_NAV_ROUTE)
        enableEdgeToEdge()
        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            val themeKey by settingsRepository
                .getStringFlow(SettingsRepository.KEY_APP_THEME, AppTheme.CLASSIC_GREEN.key)
                .collectAsState(initial = AppTheme.CLASSIC_GREEN.key)
            val appTheme = AppTheme.fromKey(themeKey)
            val shoppingBadgeCount by shoppingListRepository.getActiveCount()
                .collectAsState(initial = 0)

            // Expiry warning days + expiring count for badge
            var warningDays by remember { mutableIntStateOf(7) }
            LaunchedEffect(Unit) {
                warningDays = settingsRepository.getExpiryWarningDays()
            }
            val expiringBadgeCount by itemRepository.getExpiringSoonCount(warningDays)
                .collectAsState(initial = 0)

            // Read onboarding flag
            var onboardingCompleted by remember { mutableStateOf<Boolean?>(null) }
            LaunchedEffect(Unit) {
                onboardingCompleted = settingsRepository.getBoolean(
                    OnboardingViewModel.KEY_ONBOARDING_COMPLETED, false
                )
            }

            // Ensure anonymous auth on launch (creates UID for analytics/quota)
            LaunchedEffect(Unit) {
                try { authRepository.ensureAuthenticated() } catch (_: Exception) { }
            }

            // One-time category icon backfill (v2 = fixed mapping from seeded icon keys)
            LaunchedEffect(Unit) {
                val key = "category_icons_backfilled_v2"
                if (!settingsRepository.getBoolean(key, false)) {
                    categoryRepository.backfillIcons()
                    settingsRepository.setBoolean(key, true)
                }
            }

            HomeInventoryTheme(appTheme = appTheme) {
                val navController = rememberNavController()
                val currentRoute = navController.currentBackStackEntryAsState()
                    .value?.destination?.route

                // Handle notification deep link navigation
                LaunchedEffect(Unit) {
                    pendingNavRoute?.let { route ->
                        pendingNavRoute = null
                        navController.navigate(route) {
                            launchSingleTop = true
                        }
                    }
                }

                // Track screen views
                LaunchedEffect(currentRoute) {
                    currentRoute?.let { route ->
                        val screenName = route.substringBefore("/").substringBefore("?")
                        analyticsRepository.logScreenView(screenName)
                    }
                }

                when (onboardingCompleted) {
                    null -> {
                        // Loading — brief splash while reading setting
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    else -> {
                        val startDest = if (onboardingCompleted == true)
                            Screen.Dashboard.route else Screen.Onboarding.route

                        // Quick Add menu state
                        var isQuickAddOpen by remember { mutableStateOf(false) }

                        // Shopping sheet state
                        var sheetRequest by remember { mutableStateOf<SheetRequest?>(null) }

                        CompositionLocalProvider(
                            LocalShowAddShoppingSheet provides { itemId, shoppingItemId ->
                                sheetRequest = SheetRequest(itemId, shoppingItemId)
                            }
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                Scaffold(
                                    modifier = Modifier.fillMaxSize(),
                                    bottomBar = {
                                        if (currentRoute != Screen.Onboarding.route) {
                                            BottomNavBar(
                                                navController,
                                                shoppingBadgeCount = shoppingBadgeCount,
                                                expiringBadgeCount = expiringBadgeCount,
                                                isQuickAddOpen = isQuickAddOpen,
                                                onQuickAddToggle = { isQuickAddOpen = !isQuickAddOpen }
                                            )
                                        }
                                    }
                                ) { innerPadding ->
                                    AppNavigation(
                                        navController = navController,
                                        modifier = Modifier.padding(innerPadding),
                                        startDestination = startDest,
                                        windowWidthSizeClass = windowSizeClass.widthSizeClass
                                    )
                                }

                                // Quick Add overlay (renders above everything)
                                QuickAddMenuOverlay(
                                    isVisible = isQuickAddOpen,
                                    onDismiss = { isQuickAddOpen = false },
                                    onItemClick = { menuItem ->
                                        isQuickAddOpen = false
                                        if (menuItem.route == Screen.AddShoppingItem.createRoute()) {
                                            // Open bottom sheet instead of navigating
                                            sheetRequest = SheetRequest()
                                        } else {
                                            navController.navigate(menuItem.route) {
                                                launchSingleTop = true
                                            }
                                        }
                                    }
                                )
                            }

                            // Shopping bottom sheet
                            sheetRequest?.let { request ->
                                AddShoppingItemSheet(
                                    request = request,
                                    onDismiss = { sheetRequest = null }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
