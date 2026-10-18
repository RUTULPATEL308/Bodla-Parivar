package com.bodla.parivar.features.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bodla.parivar.core.di.AppContainer
import com.bodla.parivar.core.localization.AppLanguage
import com.bodla.parivar.core.localization.LocalAppLanguage
import com.bodla.parivar.core.localization.ProvideAppLanguage
import com.bodla.parivar.core.localization.Strings
import com.bodla.parivar.core.navigation.Screen
import com.bodla.parivar.core.network.Endpoints
import io.ktor.http.HttpHeaders
import com.bodla.parivar.core.network.KtorClientFactory
import com.bodla.parivar.core.sync.SyncStatus
import com.bodla.parivar.core.theme.*
import com.bodla.parivar.domain.model.Offering
import com.bodla.parivar.domain.model.OfferingBid
import com.bodla.parivar.domain.model.Profile
import com.bodla.parivar.features.auth.LoginScreen
import com.bodla.parivar.features.auth.RegisterScreen
import com.bodla.parivar.features.complaints.ComplaintListScreen
import com.bodla.parivar.features.complaints.CreateComplaintScreen
import com.bodla.parivar.features.directory.DirectoryScreen
import com.bodla.parivar.features.emergency.EmergencyScreen
import com.bodla.parivar.features.events.EventDetailScreen
import com.bodla.parivar.features.events.EventListScreen
import com.bodla.parivar.features.history.VillageHistoryScreen
import com.bodla.parivar.features.home.HomeScreen
import com.bodla.parivar.features.main.AdminHomeScreen
import com.bodla.parivar.features.main.RegisteredUsersScreen
import com.bodla.parivar.features.notifications.AppNotification
import com.bodla.parivar.features.notifications.NotificationCenterScreen
import com.bodla.parivar.features.notices.NoticeDetailScreen
import com.bodla.parivar.features.notices.NoticeListScreen
import com.bodla.parivar.features.offerings.AddOfferingScreen
import com.bodla.parivar.features.offerings.OfferingDetailScreen
import com.bodla.parivar.features.offerings.OfferingListScreen
import com.bodla.parivar.features.offerings.replaceBidHistory
import com.bodla.parivar.features.profile.ProfileScreen
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.http.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

private fun Profile.hasAdminDashboardAccess(): Boolean =
    isAdmin || roles.any { it.trim().uppercase() in setOf("ADMIN", "SUPER_ADMIN") }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var currentLanguage by remember { mutableStateOf(AppLanguage.GUJARATI) }
    var currentUser by remember {
        mutableStateOf(
            AppContainer.tokenStorage.getUserProfile()
                ?.takeIf { it.isNotBlank() }
                ?.let { runCatching { kotlinx.serialization.json.Json.decodeFromString<Profile>(it) }.getOrNull() }
        )
    }
    val cachedAdminAccess = currentUser?.hasAdminDashboardAccess() == true
    var currentScreen by remember {
        mutableStateOf<Screen>(
            when {
                AppContainer.tokenStorage.getToken().isNullOrBlank() -> Screen.Login
                cachedAdminAccess -> Screen.AdminHome
                else -> Screen.Home
            }
        )
    }
    val snackbarHostState = remember { SnackbarHostState() }
    var lastNotifId by remember { mutableStateOf<String?>(null) }
    var pendingNoticeId by remember { mutableStateOf(consumePendingNoticeId()) }
    var topNotificationMessage by remember { mutableStateOf<String?>(null) }
    var latestNotifications by remember { mutableStateOf<List<AppNotification>>(emptyList()) }
    var pullDistance by remember { mutableFloatStateOf(0f) }
    var isPullRefreshing by remember { mutableStateOf(false) }
    val notificationsStartedAt = remember { Clock.System.now() }
    val pullThresholdPx = with(androidx.compose.ui.platform.LocalDensity.current) { 76.dp.toPx() }

    // Auto-restore / refresh current user session on app start
    LaunchedEffect(Unit) {
        suspend fun refreshCommunityData() {
            val refreshResults = listOf(
                AppContainer.noticeRepository.refreshNotices(),
                AppContainer.eventRepository.refreshEvents(),
                AppContainer.businessRepository.refreshBusinesses(),
                AppContainer.offeringRepository.refreshOfferings(),
                AppContainer.emergencyRepository.refreshEmergencyContacts()
            )
            refreshResults.filter { it.isFailure }.forEach { result ->
                result.exceptionOrNull()?.let { error ->
                    io.github.aakira.napier.Napier.w("MainScreen: Could not refresh community content", error)
                }
            }
        }

        val user = AppContainer.authRepository.getCurrentUser()
        if (user != null) {
            currentUser = user
            val isAdminUser = user.hasAdminDashboardAccess()
            // A saved session starts on Home before its roles have loaded.
            // Always route from the refreshed role data so returning admins
            // do not remain on the resident dashboard.
            currentScreen = if (isAdminUser) Screen.AdminHome else Screen.Home
            refreshCommunityData()
        } else if (AppContainer.tokenStorage.getToken().isNullOrBlank()) {
            currentUser = null
            currentScreen = Screen.Login
        }
    }

    val pullRefreshConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y < 0f && pullDistance > 0f) {
                    val newDistance = (pullDistance + available.y).coerceAtLeast(0f)
                    val consumed = pullDistance - newDistance
                    pullDistance = newDistance
                    return Offset(0f, consumed)
                }
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (available.y > 0f && !isPullRefreshing) {
                    pullDistance = (pullDistance + available.y * 0.5f).coerceAtMost(pullThresholdPx * 1.5f)
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (pullDistance >= pullThresholdPx && !isPullRefreshing) {
                    isPullRefreshing = true
                    pullDistance = 0f
                    coroutineScope.launch {
                        try {
                            AppContainer.syncEngine.syncAll()
                            AppContainer.noticeRepository.refreshNotices()
                            AppContainer.eventRepository.refreshEvents()
                            AppContainer.businessRepository.refreshBusinesses()
                            AppContainer.offeringRepository.refreshOfferings()
                            AppContainer.emergencyRepository.refreshEmergencyContacts()
                        } finally {
                            isPullRefreshing = false
                        }
                    }
                } else {
                    pullDistance = 0f
                }
                return super.onPreFling(available)
            }
        }
    }

    // Live Flow Collections from SQLDelight Cache & Repositories
    val notices by AppContainer.noticeRepository.getNotices().collectAsState(initial = emptyList())
    val allEvents by AppContainer.eventRepository.getEvents().collectAsState(initial = emptyList())
    val businesses by AppContainer.businessRepository.getBusinesses().collectAsState(initial = emptyList())
    val offerings by AppContainer.offeringRepository.getOfferings().collectAsState(initial = emptyList())
    val emergencyContacts by AppContainer.emergencyRepository.getEmergencyContacts().collectAsState(initial = emptyList())
    val myComplaints by AppContainer.complaintRepository.getMyComplaints().collectAsState(initial = emptyList())
    val syncStatus by AppContainer.syncEngine.syncState.collectAsState(initial = SyncStatus.IDLE)
    val lastSyncError by AppContainer.syncEngine.lastSyncError.collectAsState(initial = null)
    val isConnectedToInternet by AppContainer.networkObserver.observe().collectAsState(initial = AppContainer.networkObserver.isConnected())
    var offeringBidsById by remember { mutableStateOf<Map<String, List<OfferingBid>>>(emptyMap()) }
    var offeringBidsError by remember { mutableStateOf<String?>(null) }
    val offeringBidRefreshVersions = remember { mutableMapOf<String, Long>() }

    suspend fun refreshOfferingBids(offeringId: String) {
        val refreshVersion = (offeringBidRefreshVersions[offeringId] ?: 0L) + 1L
        offeringBidRefreshVersions[offeringId] = refreshVersion
        val result = AppContainer.offeringRepository.getOfferingBids(offeringId)
        if (offeringBidRefreshVersions[offeringId] != refreshVersion) return
        result.onSuccess { bids ->
            offeringBidsById = offeringBidsById + (offeringId to replaceBidHistory(bids))
            offeringBidsError = null
        }.onFailure { error ->
            offeringBidsError = error.message ?: "Could not refresh bids from the server."
        }
    }

    LaunchedEffect(currentScreen) {
        val screen = currentScreen
        if (screen is Screen.OfferingDetail) {
            while (true) {
                refreshOfferingBids(screen.offeringId)
                delay(5000)
            }
        }
    }

    ProvideAppLanguage(language = currentLanguage) {
        val lang = LocalAppLanguage.current

        val userRoles = currentUser?.roles.orEmpty().map { it.uppercase() }.toSet()
        val isStrictAdmin = currentUser?.isAdmin == true || "ADMIN" in userRoles || "SUPER_ADMIN" in userRoles
        val isStaffOrAdmin = isStrictAdmin || currentUser?.isStaff == true || currentUser?.featurePermissions?.isNotEmpty() == true
        val defaultHome = if (isStrictAdmin) Screen.AdminHome else Screen.Home

        val isBottomBarVisible = when (currentScreen) {
            Screen.Home, Screen.AdminHome, Screen.AdminUsers, Screen.Notices, Screen.Navratri, Screen.Offerings, Screen.AddOffering, Screen.Notifications, Screen.Complaints, Screen.Emergency, Screen.Profile -> true
            else -> false
        } || isStaffOrAdmin

        Scaffold(
            snackbarHost = {
                SnackbarHost(hostState = snackbarHostState) { data ->
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = WarmWhiteSurface,
                        shadowElevation = 12.dp,
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderStroke)
                    ) {
                        Row(
                            modifier = Modifier.padding(start = 14.dp, end = 6.dp, top = 12.dp, bottom = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier.size(40.dp).background(LightIconBg, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = "🔔", fontSize = 18.sp)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (lang == AppLanguage.GUJARATI) "નવી સૂચના" else "COMMUNITY UPDATE",
                                    style = BodlaCaption.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
                                    color = SaffronPrimary
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = data.visuals.message,
                                    style = BodlaBody,
                                    color = TextPrimary,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            TextButton(onClick = { data.dismiss() }, contentPadding = PaddingValues(8.dp)) {
                                Text(text = "×", fontSize = 22.sp, color = TextSecondary)
                            }
                        }
                    }
                }
            },
            bottomBar = {
                if (isBottomBarVisible) {
                    NavigationBar(
                        containerColor = WarmWhiteSurface,
                        tonalElevation = 8.dp
                    ) {
                        val canSeeMembers = isStrictAdmin || currentUser?.featurePermissions?.contains("members") == true
                        val canSeeNotices = true // standard resident menu
                        val canSeeOfferings = currentUser?.isStaff == true || currentUser?.featurePermissions?.contains("offerings") == true
                        val canSeeNavratri = currentUser?.isStaff == true || currentUser?.featurePermissions?.contains("navratri") == true

                        if (isStrictAdmin) {
                            // When user is ADMIN: Only show Admin Dashboard & Admin controls
                            NavigationBarItem(
                                selected = currentScreen == Screen.AdminHome,
                                onClick = { currentScreen = Screen.AdminHome },
                                icon = { Text(text = "⚡", fontSize = 20.sp) },
                                label = { Text(text = if (lang == AppLanguage.GUJARATI) "વહીવટ" else "Admin", style = BodlaNavigation) },
                                colors = NavigationBarItemDefaults.colors(selectedIconColor = SaffronPrimary, selectedTextColor = SaffronPrimary, indicatorColor = LightIconBg)
                            )

                            NavigationBarItem(
                                selected = currentScreen == Screen.AdminUsers,
                                onClick = { currentScreen = Screen.AdminUsers },
                                icon = { Text(text = "👥", fontSize = 20.sp) },
                                label = { Text(text = if (lang == AppLanguage.GUJARATI) "સભ્યો" else "Members", style = BodlaNavigation) },
                                colors = NavigationBarItemDefaults.colors(selectedIconColor = SaffronPrimary, selectedTextColor = SaffronPrimary, indicatorColor = LightIconBg)
                            )

                            NavigationBarItem(
                                selected = currentScreen == Screen.Notices,
                                onClick = { currentScreen = Screen.Notices },
                                icon = { Text(text = "📢", fontSize = 20.sp) },
                                label = { Text(text = Strings.navNotices(lang), style = BodlaNavigation) },
                                colors = NavigationBarItemDefaults.colors(selectedIconColor = SaffronPrimary, selectedTextColor = SaffronPrimary, indicatorColor = LightIconBg)
                            )

                            NavigationBarItem(
                                selected = currentScreen == Screen.Offerings,
                                onClick = { currentScreen = Screen.Offerings },
                                icon = { Text(text = "🙏", fontSize = 20.sp) },
                                label = { Text(text = if (lang == AppLanguage.GUJARATI) "ચઢાવો" else "Offerings", style = BodlaNavigation) },
                                colors = NavigationBarItemDefaults.colors(selectedIconColor = SaffronPrimary, selectedTextColor = SaffronPrimary, indicatorColor = LightIconBg)
                            )

                            NavigationBarItem(
                                selected = currentScreen == Screen.Profile,
                                onClick = { currentScreen = Screen.Profile },
                                icon = { Text(text = "👤", fontSize = 20.sp) },
                                label = { Text(text = Strings.navProfile(lang), style = BodlaNavigation) },
                                colors = NavigationBarItemDefaults.colors(selectedIconColor = SaffronPrimary, selectedTextColor = SaffronPrimary, indicatorColor = LightIconBg)
                            )
                        } else {
                            // Standard Resident Menu
                            NavigationBarItem(
                                selected = currentScreen == Screen.Home,
                                onClick = { currentScreen = Screen.Home },
                                icon = { Text(text = "🏠", fontSize = 20.sp) },
                                label = { Text(text = Strings.navHome(lang), style = BodlaNavigation) },
                                colors = NavigationBarItemDefaults.colors(selectedIconColor = SaffronPrimary, selectedTextColor = SaffronPrimary, indicatorColor = LightIconBg)
                            )

                            if (canSeeMembers) {
                                NavigationBarItem(
                                    selected = currentScreen == Screen.AdminUsers,
                                    onClick = { currentScreen = Screen.AdminUsers },
                                    icon = { Text(text = "👥", fontSize = 20.sp) },
                                    label = { Text(text = if (lang == AppLanguage.GUJARATI) "સભ્યો" else "Members", style = BodlaNavigation) },
                                    colors = NavigationBarItemDefaults.colors(selectedIconColor = SaffronPrimary, selectedTextColor = SaffronPrimary, indicatorColor = LightIconBg)
                                )
                            }

                            if (canSeeNotices) {
                                NavigationBarItem(
                                    selected = currentScreen == Screen.Notices,
                                    onClick = { currentScreen = Screen.Notices },
                                    icon = { Text(text = "📢", fontSize = 20.sp) },
                                    label = { Text(text = Strings.navNotices(lang), style = BodlaNavigation) },
                                    colors = NavigationBarItemDefaults.colors(selectedIconColor = SaffronPrimary, selectedTextColor = SaffronPrimary, indicatorColor = LightIconBg)
                                )
                            }

                            if (canSeeOfferings) {
                                NavigationBarItem(
                                    selected = currentScreen == Screen.Offerings,
                                    onClick = { currentScreen = Screen.Offerings },
                                    icon = { Text(text = "🙏", fontSize = 20.sp) },
                                    label = { Text(text = if (lang == AppLanguage.GUJARATI) "ચઢાવો" else "Offerings", style = BodlaNavigation) },
                                    colors = NavigationBarItemDefaults.colors(selectedIconColor = SaffronPrimary, selectedTextColor = SaffronPrimary, indicatorColor = LightIconBg)
                                )
                            }

                            if (canSeeNavratri) {
                                NavigationBarItem(
                                    selected = currentScreen == Screen.Navratri,
                                    onClick = { currentScreen = Screen.Navratri },
                                    icon = { Text(text = "✨", fontSize = 20.sp) },
                                    label = { Text(text = if (lang == AppLanguage.GUJARATI) "નવરાત્રી" else "Navratri", style = BodlaNavigation) },
                                    colors = NavigationBarItemDefaults.colors(selectedIconColor = SaffronPrimary, selectedTextColor = SaffronPrimary, indicatorColor = LightIconBg)
                                )
                            }

                            NavigationBarItem(
                                selected = currentScreen == Screen.Notifications,
                                onClick = { currentScreen = Screen.Notifications },
                                icon = { Text(text = "🔔", fontSize = 20.sp) },
                                label = { Text(text = if (lang == AppLanguage.GUJARATI) "સૂચનાઓ" else "Updates", style = BodlaNavigation) },
                                colors = NavigationBarItemDefaults.colors(selectedIconColor = SaffronPrimary, selectedTextColor = SaffronPrimary, indicatorColor = LightIconBg)
                            )

                            NavigationBarItem(
                                selected = currentScreen == Screen.Profile,
                                onClick = { currentScreen = Screen.Profile },
                                icon = { Text(text = "👤", fontSize = 20.sp) },
                                label = { Text(text = Strings.navProfile(lang), style = BodlaNavigation) },
                                colors = NavigationBarItemDefaults.colors(selectedIconColor = SaffronPrimary, selectedTextColor = SaffronPrimary, indicatorColor = LightIconBg)
                            )
                        }
                    }
                }
            },
            containerColor = CreamBackground,
            modifier = modifier
        ) { paddingValues ->
            val contentModifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)

            Column(modifier = Modifier.fillMaxSize()) {
                // Sync indicator banner when SyncEngine is active
                if (syncStatus == SyncStatus.SYNCING) {
                    Surface(
                        color = SaffronPrimary.copy(alpha = 0.9f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                color = WarmWhiteSurface,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (lang == AppLanguage.GUJARATI) "સિંક થઈ રહ્યું છે..." else "Syncing with cloud...",
                                style = BodlaCaption,
                                color = WarmWhiteSurface
                            )
                        }
                    }
                }
                if (syncStatus == SyncStatus.ERROR) {
                    Surface(
                        color = Color(0xFFFFE4E6),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = lastSyncError ?: if (lang == AppLanguage.GUJARATI)
                                    "ક્લાઉડ સિંક નિષ્ફળ થયું. ઇન્ટરનેટ અને સાઇન-ઇન તપાસો."
                                else
                                    "Cloud sync failed. Check your connection and sign-in.",
                                style = BodlaCaption,
                                color = Color(0xFF9F1239),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f).padding(vertical = 4.dp)
                            )
                            TextButton(
                                enabled = syncStatus != SyncStatus.SYNCING,
                                onClick = {
                                    coroutineScope.launch {
                                        AppContainer.syncEngine.syncAll()
                                        AppContainer.noticeRepository.refreshNotices()
                                        AppContainer.eventRepository.refreshEvents()
                                        AppContainer.businessRepository.refreshBusinesses()
                                        AppContainer.offeringRepository.refreshOfferings()
                                        AppContainer.emergencyRepository.refreshEmergencyContacts()
                                    }
                                }
                            ) {
                                Text(if (lang == AppLanguage.GUJARATI) "ફરી પ્રયાસ" else "Retry")
                            }
                            if (lastSyncError?.contains("Sign out and sign in again") == true ||
                                lastSyncError?.startsWith("Your session expired") == true
                            ) {
                                TextButton(
                                    onClick = {
                                        coroutineScope.launch {
                                            AppContainer.authRepository.logout()
                                            currentUser = null
                                            currentScreen = Screen.Login
                                        }
                                    }
                                ) {
                                    Text(if (lang == AppLanguage.GUJARATI) "સાઇન-ઇન" else "Sign in")
                                }
                            }
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f).nestedScroll(pullRefreshConnection)) {
                    when (val screen = currentScreen) {
                        is Screen.Home -> HomeScreen(
                            onNavigate = { currentScreen = it },
                            userName = currentUser?.fullName ?: (if (lang == AppLanguage.GUJARATI) "ગ્રામજન" else "Resident"),
                            isOffline = !isConnectedToInternet,
                            pinnedNotice = notices.firstOrNull { it.isPinned },
                            latestNotices = notices.take(5),
                            modifier = contentModifier
                        )

                        is Screen.VillageHistory -> VillageHistoryScreen(
                            onBack = { currentScreen = defaultHome },
                            modifier = contentModifier
                        )

                        is Screen.Notices -> NoticeListScreen(
                            onNavigate = { currentScreen = it },
                            notices = notices,
                            onBack = { currentScreen = defaultHome },
                            modifier = contentModifier
                        )

                        is Screen.Navratri -> com.bodla.parivar.features.navratri.NavratriScreen(
                            onBack = { currentScreen = defaultHome },
                            canManage = currentUser?.isStaff == true || currentUser?.featurePermissions?.contains("navratri") == true,
                            modifier = contentModifier
                        )

                        is Screen.Notifications -> NotificationCenterScreen(
                            notifications = latestNotifications,
                            onBack = { currentScreen = defaultHome },
                            onNotificationClick = { notification ->
                                if (notification.type == "NOTICE" && !notification.referenceId.isNullOrBlank()) {
                                    currentScreen = Screen.NoticeDetail(notification.referenceId)
                                }
                            },
                            modifier = contentModifier
                        )

                        is Screen.Events -> EventListScreen(
                            onNavigate = { currentScreen = it },
                            events = allEvents,
                            modifier = contentModifier
                        )

                        is Screen.Directory -> DirectoryScreen(
                            businesses = businesses,
                            onNavigate = { currentScreen = it },
                            modifier = contentModifier
                        )

                        is Screen.Profile -> ProfileScreen(
                            profile = currentUser,
                            onNavigate = { currentScreen = it },
                            onLanguageChange = { currentLanguage = it },
                            onLogout = {
                                coroutineScope.launch {
                                    AppContainer.authRepository.logout()
                                    currentUser = null
                                    currentScreen = Screen.Login
                                }
                            },
                            modifier = contentModifier
                        )

                        is Screen.AdminHome -> AdminHomeScreen(
                            profile = currentUser,
                            authRepository = AppContainer.authRepository,
                            notices = notices,
                            offerings = offerings,
                            onNavigate = { currentScreen = it },
                            onLogout = {
                                coroutineScope.launch {
                                    AppContainer.authRepository.logout()
                                    currentUser = null
                                    currentScreen = Screen.Login
                                }
                            },
                            modifier = contentModifier
                        )

                        is Screen.AdminUsers -> RegisteredUsersScreen(
                            authRepository = AppContainer.authRepository,
                            onBack = { currentScreen = Screen.AdminHome },
                            modifier = contentModifier
                        )

                        is Screen.Login -> LoginScreen(
                            authRepository = AppContainer.authRepository,
                            onLoginSuccess = { profile ->
                                currentUser = profile
                                val isAdminUser = profile.hasAdminDashboardAccess()
                                currentScreen = if (isAdminUser) Screen.AdminHome else Screen.Home
                                coroutineScope.launch {
                                    AppContainer.syncEngine.syncAll()
                                    AppContainer.noticeRepository.refreshNotices()
                                    AppContainer.eventRepository.refreshEvents()
                                    AppContainer.businessRepository.refreshBusinesses()
                                    AppContainer.offeringRepository.refreshOfferings()
                                    AppContainer.emergencyRepository.refreshEmergencyContacts()
                                }
                            },
                            onNavigateRegister = { currentScreen = Screen.Register },
                            modifier = contentModifier
                        )

                        is Screen.Register -> RegisterScreen(
                            authRepository = AppContainer.authRepository,
                            onRegisterSuccess = { profile ->
                                currentUser = profile
                                currentScreen = Screen.Home
                            },
                            onNavigateLogin = { currentScreen = Screen.Login },
                            modifier = contentModifier
                        )

                        is Screen.NoticeDetail -> NoticeDetailScreen(
                            notice = notices.find { it.id == screen.noticeId },
                            onBack = { currentScreen = Screen.Notices },
                            modifier = contentModifier
                        )

                        is Screen.EventDetail -> EventDetailScreen(
                            event = allEvents.find { it.id == screen.eventId },
                            onBack = { currentScreen = Screen.Events },
                            modifier = contentModifier
                        )

                        is Screen.Offerings -> OfferingListScreen(
                            onNavigate = { currentScreen = it },
                            offerings = offerings,
                            onBack = { currentScreen = defaultHome },
                            canManageOfferings = currentUser?.isStaff == true || currentUser?.featurePermissions?.contains("offerings") == true,
                            modifier = contentModifier
                        )

                        is Screen.OfferingDetail -> OfferingDetailScreen(
                            offeringId = screen.offeringId,
                            offering = offerings.find { it.id == screen.offeringId },
                            existingBids = offeringBidsById[screen.offeringId].orEmpty(),
                            bidLoadError = offeringBidsError,
                            bidderProfile = currentUser,
                            canManageOfferings = currentUser?.isStaff == true || currentUser?.featurePermissions?.contains("offerings") == true,
                            onPlaceBid = { amount ->
                                val profile = currentUser
                                if (profile == null) {
                                    Result.failure(IllegalStateException("Sign in before placing a bid."))
                                } else {
                                    val result = AppContainer.offeringRepository.submitOfferingBid(
                                        offeringId = screen.offeringId,
                                        bidderName = profile.fullName,
                                        amount = amount,
                                        bidderPhone = profile.phone
                                    )
                                    if (result.isSuccess) {
                                        refreshOfferingBids(screen.offeringId)
                                    }
                                    result
                                }
                            },
                            onRefreshBids = { refreshOfferingBids(screen.offeringId) },
                            onBack = { currentScreen = Screen.Offerings },
                            modifier = contentModifier
                        )

                        is Screen.AddOffering -> if (currentUser?.isStaff == true || currentUser?.featurePermissions?.contains("offerings") == true) {
                            AddOfferingScreen(
                                onBack = { currentScreen = Screen.Offerings },
                                onSubmit = { offering ->
                                    coroutineScope.launch {
                                        AppContainer.offeringRepository.submitOffering(offering)
                                            .onSuccess { currentScreen = Screen.Offerings }
                                            .onFailure { error ->
                                                snackbarHostState.showSnackbar(error.message ?: "Could not create offering.")
                                            }
                                    }
                                },
                                modifier = contentModifier
                            )
                        } else {
                            OfferingListScreen(
                                onNavigate = { currentScreen = it },
                                offerings = offerings,
                                onBack = { currentScreen = defaultHome },
                                modifier = contentModifier
                            )
                        }

                        is Screen.Complaints -> ComplaintListScreen(
                            onNavigate = { currentScreen = it },
                            complaints = myComplaints,
                            onBack = { currentScreen = defaultHome },
                            modifier = contentModifier
                        )

                        is Screen.CreateComplaint -> CreateComplaintScreen(
                            onBack = { currentScreen = Screen.Complaints },
                            onSubmit = { title, description ->
                                coroutineScope.launch {
                                    AppContainer.complaintRepository.submitComplaint(
                                        title = title,
                                        description = description,
                                        categoryId = null,
                                        photoUrl = null
                                    )
                                    currentScreen = Screen.Complaints
                                }
                            },
                            modifier = contentModifier
                        )

                        is Screen.Emergency -> EmergencyScreen(
                            onBack = { currentScreen = defaultHome },
                            emergencyContacts = emergencyContacts,
                            modifier = contentModifier
                        )

                        else -> HomeScreen(
                            onNavigate = { currentScreen = it },
                            modifier = contentModifier
                        )
                    }

                    if (isPullRefreshing) {
                        Surface(
                            modifier = Modifier.align(Alignment.TopCenter).padding(top = 10.dp),
                            shape = CircleShape,
                            color = WarmWhiteSurface,
                            shadowElevation = 6.dp,
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderStroke)
                        ) {
                            Box(modifier = Modifier.padding(10.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.5.dp, color = SaffronPrimary)
                            }
                        }
                    }

                    topNotificationMessage?.let { message ->
                        Surface(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            shape = RoundedCornerShape(16.dp),
                            color = WarmWhiteSurface,
                            shadowElevation = 14.dp,
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderStroke)
                        ) {
                            Row(
                                modifier = Modifier.padding(start = 14.dp, end = 6.dp, top = 12.dp, bottom = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier.size(40.dp).background(LightIconBg, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = "🔔", fontSize = 18.sp)
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (lang == AppLanguage.GUJARATI) "નવી સૂચના" else "COMMUNITY UPDATE",
                                        style = BodlaCaption.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
                                        color = SaffronPrimary
                                    )
                                    Text(
                                        text = message,
                                        style = BodlaBody,
                                        color = TextPrimary,
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                TextButton(onClick = { topNotificationMessage = null }, contentPadding = PaddingValues(8.dp)) {
                                    Text(text = "×", fontSize = 22.sp, color = TextSecondary)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
