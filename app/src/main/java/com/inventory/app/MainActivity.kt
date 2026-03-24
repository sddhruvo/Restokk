package com.inventory.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import com.inventory.app.ui.components.ThemedCircularProgress
import androidx.compose.material3.Scaffold
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.*
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
import com.inventory.app.ui.components.BreathingBudget
import com.inventory.app.ui.components.LocalBreathingBudget
import com.inventory.app.ui.components.LocalSurpriseManager
import com.inventory.app.ui.components.SurpriseManager
import com.inventory.app.ui.components.ThemedBackground
import com.inventory.app.ui.components.rememberAiSignInGate
import com.inventory.app.ui.navigation.AppNavigation
import com.inventory.app.ui.navigation.BottomNavBar
import com.inventory.app.ui.navigation.LocalBottomNavHeight
import com.inventory.app.ui.navigation.LocalBottomNavSlide
import com.inventory.app.ui.navigation.LocalNavigationGuard
import com.inventory.app.ui.navigation.NavigationGuardState
import com.inventory.app.ui.navigation.QuickAddMenuOverlay
import com.inventory.app.ui.navigation.Screen
import com.inventory.app.ui.screens.cook.LocalVolumeHandler
import com.inventory.app.ui.screens.cook.VolumeHandlerState
import com.inventory.app.ui.screens.onboarding.OnboardingViewModel
import com.inventory.app.ui.screens.shopping.AddShoppingItemSheet
import com.inventory.app.ui.screens.shopping.LocalShowAddShoppingSheet
import com.inventory.app.ui.screens.shopping.SheetRequest
import com.inventory.app.ui.theme.AppTheme
import com.inventory.app.ui.theme.HomeInventoryTheme
import com.inventory.app.ui.theme.LocalReduceMotion
import com.inventory.app.ui.theme.VisualStyle
import com.inventory.app.ui.theme.rememberReduceMotion
import com.inventory.app.worker.SmartNotificationWorker
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.IntOffset
import androidx.core.view.WindowCompat
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.tasks.await
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

    // Volume button handler for cooking playback
    private val volumeHandlerState = VolumeHandlerState()

    private val appUpdateManager by lazy { AppUpdateManagerFactory.create(this) }

    private val installStateListener = InstallStateUpdatedListener { state ->
        if (state.installStatus() == InstallStatus.DOWNLOADED) {
            appUpdateManager.completeUpdate()
        }
    }

    private suspend fun checkForAppUpdate() {
        try {
            val lastCheck = settingsRepository.getString(SettingsRepository.KEY_LAST_UPDATE_CHECK, "0").toLongOrNull() ?: 0L
            val twelveHoursMs = 12 * 60 * 60 * 1000L
            if (System.currentTimeMillis() - lastCheck < twelveHoursMs) return

            settingsRepository.set(SettingsRepository.KEY_LAST_UPDATE_CHECK, System.currentTimeMillis().toString())

            val info = appUpdateManager.appUpdateInfo.await()
            if (info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)
            ) {
                appUpdateManager.registerListener(installStateListener)
                appUpdateManager.startUpdateFlowForResult(
                    info,
                    this@MainActivity,
                    AppUpdateOptions.defaultOptions(AppUpdateType.FLEXIBLE),
                    REQUEST_CODE_UPDATE
                )
            }
        } catch (_: Exception) { }
    }

    override fun onDestroy() {
        super.onDestroy()
        appUpdateManager.unregisterListener(installStateListener)
    }

    override fun onKeyDown(keyCode: Int, event: android.view.KeyEvent?): Boolean {
        if (keyCode == android.view.KeyEvent.KEYCODE_VOLUME_UP ||
            keyCode == android.view.KeyEvent.KEYCODE_VOLUME_DOWN
        ) {
            val isUp = keyCode == android.view.KeyEvent.KEYCODE_VOLUME_UP
            if (volumeHandlerState.onVolumeKey(isUp)) return true
        }
        return super.onKeyDown(keyCode, event)
    }

    companion object {
        private const val REQUEST_CODE_UPDATE = 1001
    }

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

            val visualStyleKey by settingsRepository
                .getStringFlow(SettingsRepository.KEY_VISUAL_STYLE, VisualStyle.PAPER_INK.key)
                .collectAsState(initial = VisualStyle.PAPER_INK.key)
            val visualStyle = VisualStyle.fromKey(visualStyleKey)
            val shoppingBadgeCount by shoppingListRepository.getActiveCount()
                .collectAsState(initial = 0)

            // Expiry warning days + expiring count for badge (reactive)
            val warningDays by settingsRepository.getIntFlow(SettingsRepository.KEY_EXPIRY_WARNING_DAYS, 3)
                .collectAsState(initial = 3)
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

            // In-app update check (flexible, max once per 12 hours)
            LaunchedEffect(Unit) {
                checkForAppUpdate()
            }

            // One-time category icon backfill (v2 = fixed mapping from seeded icon keys)
            LaunchedEffect(Unit) {
                val key = "category_icons_backfilled_v2"
                if (!settingsRepository.getBoolean(key, false)) {
                    categoryRepository.backfillIcons()
                    settingsRepository.setBoolean(key, true)
                }
            }

            // Request notification permission on Android 13+ after onboarding
            val notifPermissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { _ -> /* no-op — user's choice is respected */ }

            LaunchedEffect(onboardingCompleted) {
                if (onboardingCompleted == true && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val granted = ContextCompat.checkSelfPermission(
                        this@MainActivity, Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                    if (!granted) {
                        notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            }

            val reduceMotion = rememberReduceMotion()
            val breathingBudget = remember { BreathingBudget() }
            val surpriseManager = remember { SurpriseManager() }
            CompositionLocalProvider(
                LocalReduceMotion provides reduceMotion,
                LocalBreathingBudget provides breathingBudget,
                LocalSurpriseManager provides surpriseManager
            ) {
            HomeInventoryTheme(appTheme = appTheme, visualStyle = visualStyle) {
                // Status bar icons: dark icons on light themes, light icons on dark
                val isLightTheme = appTheme != AppTheme.AMOLED_DARK
                SideEffect {
                    WindowCompat.getInsetsController(window, window.decorView)
                        .isAppearanceLightStatusBars = isLightTheme
                }

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
                            ThemedCircularProgress()
                        }
                    }
                    else -> {
                        val startDest = if (onboardingCompleted == true)
                            Screen.Dashboard.route else Screen.Onboarding.route

                        // Quick Add menu state
                        var isQuickAddOpen by remember { mutableStateOf(false) }

                        // Shopping sheet state
                        var sheetRequest by remember { mutableStateOf<SheetRequest?>(null) }

                        // Navigation guard state (discard dialogs for bottom nav)
                        val navigationGuardState = remember { NavigationGuardState() }

                        // Bottom nav bar height — measured dynamically, propagated to all screens
                        var bottomNavHeight by remember { mutableStateOf(80.dp) }
                        val density = LocalDensity.current
                        val showNavBar = currentRoute != Screen.Onboarding.route &&
                            currentRoute?.startsWith("cooking-playback") != true

                        // Overscroll-to-reveal: push nav bar DOWN when child scrollable
                        // hits its bottom and the user keeps scrolling. Content stays put,
                        // nav bar slides out of the way to reveal hidden items.
                        var navBarSlide by remember { mutableFloatStateOf(0f) }
                        val navBarHeightPx = with(density) { bottomNavHeight.toPx() }

                        // Reset slide on navigation
                        LaunchedEffect(currentRoute) { navBarSlide = 0f }

                        val overscrollConnection = remember(navBarHeightPx) {
                            object : NestedScrollConnection {
                                override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                                    // Scrolling up: restore nav bar first, before child scrolls
                                    if (available.y > 0f && navBarSlide > 0f) {
                                        val consumed = minOf(available.y, navBarSlide)
                                        navBarSlide -= consumed
                                        return Offset(0f, consumed)
                                    }
                                    return Offset.Zero
                                }

                                override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                                    // Scrolling down: child hit bottom, slide nav bar down
                                    if (available.y < 0f && navBarHeightPx > 0f) {
                                        val newSlide = (navBarSlide - available.y).coerceAtMost(navBarHeightPx)
                                        val actualConsumed = -(newSlide - navBarSlide)
                                        navBarSlide = newSlide
                                        return Offset(0f, actualConsumed)
                                    }
                                    return Offset.Zero
                                }
                            }
                        }

                        CompositionLocalProvider(
                            LocalShowAddShoppingSheet provides { itemId, shoppingItemId ->
                                sheetRequest = SheetRequest(itemId, shoppingItemId)
                            },
                            LocalNavigationGuard provides navigationGuardState,
                            LocalVolumeHandler provides volumeHandlerState,
                            LocalBottomNavHeight provides if (showNavBar) bottomNavHeight else 0.dp,
                            LocalBottomNavSlide provides if (showNavBar && navBarHeightPx > 0f) (navBarSlide / navBarHeightPx).coerceIn(0f, 1f) else 0f
                        ) {
                            val aiGate = rememberAiSignInGate()

                            @OptIn(ExperimentalComposeUiApi::class)
                            Box(modifier = Modifier
                                .fillMaxSize()
                                .semantics { testTagsAsResourceId = true }
                            ) {
                                Scaffold(
                                    modifier = Modifier.fillMaxSize()
                                ) { innerPadding ->
                                    ThemedBackground(
                                        modifier = Modifier
                                            .padding(top = innerPadding.calculateTopPadding())
                                            .nestedScroll(overscrollConnection)
                                    ) {
                                        AppNavigation(
                                            navController = navController,
                                            modifier = Modifier
                                                .fillMaxSize(),
                                            startDestination = startDest,
                                            windowWidthSizeClass = windowSizeClass.widthSizeClass
                                        )
                                    }
                                }

                                // Quick Add overlay (renders above everything)
                                QuickAddMenuOverlay(
                                    isVisible = isQuickAddOpen,
                                    onDismiss = { isQuickAddOpen = false },
                                    onItemClick = { menuItem ->
                                        isQuickAddOpen = false
                                        val aiRoutes = setOf(Screen.FridgeScan.route, Screen.ReceiptScan.route)
                                        if (menuItem.route == Screen.AddShoppingItem.createRoute()) {
                                            sheetRequest = SheetRequest()
                                        } else if (menuItem.route in aiRoutes) {
                                            val desc = if (menuItem.route == Screen.FridgeScan.route)
                                                "identify kitchen items" else "parse receipts"
                                            aiGate.requireSignIn(desc) {
                                                navController.navigate(menuItem.route) {
                                                    launchSingleTop = true
                                                }
                                            }
                                        } else {
                                            navController.navigate(menuItem.route) {
                                                launchSingleTop = true
                                            }
                                        }
                                    }
                                )

                                // Floating pill bottom nav (renders above content)
                                if (showNavBar) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize(),
                                        contentAlignment = Alignment.BottomCenter
                                    ) {
                                        BottomNavBar(
                                            navController,
                                            modifier = Modifier
                                                .onGloballyPositioned { coordinates ->
                                                    with(density) {
                                                        val h = coordinates.size.height.toDp()
                                                        if (h > 0.dp) bottomNavHeight = h
                                                    }
                                                }
                                                .offset { IntOffset(0, navBarSlide.toInt()) },
                                            shoppingBadgeCount = shoppingBadgeCount,
                                            expiringBadgeCount = expiringBadgeCount,
                                            isQuickAddOpen = isQuickAddOpen,
                                            onQuickAddToggle = { isQuickAddOpen = !isQuickAddOpen }
                                        )
                                    }
                                }
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
            } // CompositionLocalProvider(LocalReduceMotion)
        }
    }
}
