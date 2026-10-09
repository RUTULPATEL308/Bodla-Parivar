package com.bodla.parivar.features.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bodla.parivar.core.di.AppContainer
import com.bodla.parivar.core.localization.AppLanguage
import com.bodla.parivar.core.localization.LocalAppLanguage
import com.bodla.parivar.core.localization.ProvideAppLanguage
import com.bodla.parivar.core.localization.Strings
import com.bodla.parivar.core.navigation.Screen
import com.bodla.parivar.core.network.Endpoints
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
import com.bodla.parivar.features.home.HomeScreen
import com.bodla.parivar.features.main.AdminHomeScreen
import com.bodla.parivar.features.main.RegisteredUsersScreen
import com.bodla.parivar.features.notices.NoticeDetailScreen
import com.bodla.parivar.features.notices.NoticeListScreen
import com.bodla.parivar.features.offerings.AddOfferingScreen
import com.bodla.parivar.features.offerings.OfferingDetailScreen
import com.bodla.parivar.features.offerings.OfferingListScreen
import com.bodla.parivar.features.offerings.replaceBidHistory
import com.bodla.parivar.features.profile.ProfileScreen
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
private data class NotificationDto(
    val id: String,
    @SerialName("title_gu") val titleGu: String? = null,
    @SerialName("title_en") val titleEn: String? = null,
    @SerialName("body_gu") val bodyGu: String? = null,
    @SerialName("body_en") val bodyEn: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var currentLanguage by remember { mutableStateOf(AppLanguage.GUJARATI) }
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Login) }
    val snackbarHostState = remember { SnackbarHostState() }
    var lastNotifId by remember { mutableStateOf<String?>(null) }
    val notificationsStartedAt = remember { Clock.System.now() }

    // Live Flow Collections from SQLDelight Cache & Repositories
    val notices by AppContainer.noticeRepository.getNotices().collectAsState(initial = emptyList())
    val upcomingEvents by AppContainer.eventRepository.getUpcomingEvents().collectAsState(initial = emptyList())
    val allEvents by AppContainer.eventRepository.getEvents().collectAsState(initial = emptyList())
    val businesses by AppContainer.businessRepository.getBusinesses().collectAsState(initial = emptyList())
    val offerings by AppContainer.offeringRepository.getOfferings().collectAsState(initial = emptyList())
    val emergencyContacts by AppContainer.emergencyRepository.getEmergencyContacts().collectAsState(initial = emptyList())
    val myComplaints by AppContainer.complaintRepository.getMyComplaints().collectAsState(initial = emptyList())
    val syncStatus by AppContainer.syncEngine.syncState.collectAsState(initial = SyncStatus.IDLE)
    val lastSyncError by AppContainer.syncEngine.lastSyncError.collectAsState(initial = null)
    val isConnectedToInternet by AppContainer.networkObserver.observe().collectAsState(initial = AppContainer.networkObserver.isConnected())

    var currentUser by remember { mutableStateOf<Profile?>(null) }
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

    LaunchedEffect(Unit) {
        currentUser = AppContainer.authRepository.getCurrentUser()
        currentScreen = when {
            currentUser?.isStaff == true -> Screen.AdminHome
            currentUser != null -> Screen.Home
            else -> Screen.Login
        }
    }

    // Refresh public community data in background after first UI frame renders
    LaunchedEffect(isConnectedToInternet) {
        if (isConnectedToInternet && com.bodla.parivar.core.network.Endpoints.isConfigured) {
            delay(300) // Yield to UI so app opens without stutter
            coroutineScope.launch { AppContainer.offeringRepository.refreshOfferings() }
            coroutineScope.launch { AppContainer.noticeRepository.refreshNotices() }
            coroutineScope.launch { AppContainer.eventRepository.refreshEvents() }
            coroutineScope.launch { AppContainer.businessRepository.refreshBusinesses() }
            coroutineScope.launch { AppContainer.emergencyRepository.refreshEmergencyContacts() }
            coroutineScope.launch { AppContainer.villageRepository.refreshVillageInfo() }
            coroutineScope.launch { AppContainer.syncEngine.syncAll() }
        }
    }

    LaunchedEffect(isConnectedToInternet) {
        if (!isConnectedToInternet) return@LaunchedEffect

        while (isConnectedToInternet) {
            AppContainer.offeringRepository.refreshOfferings()
            delay(10000)
        }
    }

    LaunchedEffect(currentScreen, isConnectedToInternet) {
        if (!isConnectedToInternet) return@LaunchedEffect

        if (currentScreen !is Screen.OfferingDetail) return@LaunchedEffect

        val offeringDetail = currentScreen as? Screen.OfferingDetail ?: return@LaunchedEffect
        while (true) {
            refreshOfferingBids(offeringDetail.offeringId)
            AppContainer.offeringRepository.refreshOfferings()
            delay(2000) // 2-second fast refresh for live bidding
        }
    }

    // Poll notifications table for in-app toasts for ALL app users (every 3 seconds for instant real-time feel)
    LaunchedEffect(isConnectedToInternet) {
        if (!isConnectedToInternet) return@LaunchedEffect
        if (!Endpoints.isConfigured) return@LaunchedEffect
        while (true) {
            try {
                val notifications = AppContainer.httpClient
                    .get(Endpoints.NOTIFICATIONS_LATEST) {
                        header("apikey", Endpoints.SUPABASE_ANON_KEY)
                        header("Accept", "application/json")
                    }
                    .body<List<NotificationDto>>()
                val latest = notifications.firstOrNull()
                if (latest != null && latest.id != lastNotifId) {
                    val arrivedAfterStartup = latest.createdAt?.let { createdAt ->
                        runCatching { Instant.parse(createdAt) >= notificationsStartedAt }.getOrDefault(false)
                    } == true
                    if (lastNotifId != null || arrivedAfterStartup) {
                        val title = (if (currentLanguage == AppLanguage.GUJARATI) latest.titleGu else latest.titleEn)
                            ?: latest.titleGu ?: "🔔 નવી અપડેટ"
                        val body = (if (currentLanguage == AppLanguage.GUJARATI) latest.bodyGu else latest.bodyEn)
                            ?: latest.bodyGu ?: ""
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(
                                message = if (body.isNotBlank()) "$title: $body" else title,
                                duration = SnackbarDuration.Long
                            )
                        }
                    }
                    lastNotifId = latest.id
                }
            } catch (_: Exception) { /* ignore poll errors */ }
            delay(3000)
        }
    }

    ProvideAppLanguage(language = currentLanguage) {
        val lang = LocalAppLanguage.current

        val isBottomBarVisible = when (currentScreen) {
            Screen.Home, Screen.Notices, Screen.Events, Screen.Directory, Screen.Profile -> true
            else -> false
        }

        Scaffold(
            snackbarHost = {
                SnackbarHost(hostState = snackbarHostState) { data ->
                    Snackbar(
                        snackbarData = data,
                        containerColor = SaffronPrimary,
                        contentColor = WarmWhiteSurface,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            },
            bottomBar = {
                if (isBottomBarVisible) {
                    NavigationBar(
                        containerColor = WarmWhiteSurface,
                        tonalElevation = 8.dp
                    ) {
                        // 1. Home
                        NavigationBarItem(
                            selected = currentScreen == Screen.Home,
                            onClick = { currentScreen = Screen.Home },
                            icon = { Text(text = "🏠", fontSize = 20.sp) },
                            label = { Text(text = Strings.navHome(lang), style = BodlaNavigation) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = SaffronPrimary,
                                selectedTextColor = SaffronPrimary,
                                indicatorColor = LightIconBg
                            )
                        )

                        // 2. Notices
                        NavigationBarItem(
                            selected = currentScreen == Screen.Notices,
                            onClick = { currentScreen = Screen.Notices },
                            icon = { Text(text = "📢", fontSize = 20.sp) },
                            label = { Text(text = Strings.navNotices(lang), style = BodlaNavigation) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = SaffronPrimary,
                                selectedTextColor = SaffronPrimary,
                                indicatorColor = LightIconBg
                            )
                        )

                        // 3. Events
                        NavigationBarItem(
                            selected = currentScreen == Screen.Events,
                            onClick = { currentScreen = Screen.Events },
                            icon = { Text(text = "📅", fontSize = 20.sp) },
                            label = { Text(text = Strings.navEvents(lang), style = BodlaNavigation) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = SaffronPrimary,
                                selectedTextColor = SaffronPrimary,
                                indicatorColor = LightIconBg
                            )
                        )

                        // 4. Directory
                        NavigationBarItem(
                            selected = currentScreen == Screen.Directory,
                            onClick = { currentScreen = Screen.Directory },
                            icon = { Text(text = "🏛", fontSize = 20.sp) },
                            label = { Text(text = Strings.navDirectory(lang), style = BodlaNavigation) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = SaffronPrimary,
                                selectedTextColor = SaffronPrimary,
                                indicatorColor = LightIconBg
                            )
                        )

                        // 5. Profile
                        NavigationBarItem(
                            selected = currentScreen == Screen.Profile,
                            onClick = { currentScreen = Screen.Profile },
                            icon = { Text(text = "👤", fontSize = 20.sp) },
                            label = { Text(text = Strings.navProfile(lang), style = BodlaNavigation) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = SaffronPrimary,
                                selectedTextColor = SaffronPrimary,
                                indicatorColor = LightIconBg
                            )
                        )
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
                                onClick = { coroutineScope.launch { AppContainer.syncEngine.syncAll() } }
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

                Box(modifier = Modifier.weight(1f)) {
                    when (val screen = currentScreen) {
                        is Screen.Home -> HomeScreen(
                            onNavigate = { currentScreen = it },
                            userName = currentUser?.fullName ?: (if (lang == AppLanguage.GUJARATI) "ગ્રામજન" else "Resident"),
                            isOffline = !isConnectedToInternet,
                            pinnedNotice = notices.firstOrNull { it.isPinned },
                            upcomingEvents = upcomingEvents,
                            latestNotices = notices.take(5),
                            modifier = contentModifier
                        )

                        is Screen.Notices -> NoticeListScreen(
                            onNavigate = { currentScreen = it },
                            notices = notices,
                            modifier = contentModifier
                        )

                        is Screen.Events -> EventListScreen(
                            onNavigate = { currentScreen = it },
                            events = allEvents,
                            modifier = contentModifier
                        )

                        is Screen.Directory -> DirectoryScreen(
                            onNavigate = { currentScreen = it },
                            businesses = businesses,
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
                                currentScreen = if (profile.isStaff) Screen.AdminHome else Screen.Home
                                coroutineScope.launch { AppContainer.syncEngine.syncAll() }
                            },
                            onNavigateRegister = { currentScreen = Screen.Register },
                            modifier = contentModifier
                        )

                        is Screen.Register -> RegisterScreen(
                            authRepository = AppContainer.authRepository,
                            onRegisterSuccess = { profile ->
                                currentUser = profile
                                currentScreen = if (profile.isStaff) Screen.AdminHome else Screen.Home
                            },
                            onNavigateLogin = { currentScreen = Screen.Login },
                            modifier = contentModifier
                        )

                        is Screen.NoticeDetail -> NoticeDetailScreen(
                            noticeId = screen.noticeId,
                            notice = notices.find { it.id == screen.noticeId },
                            onBack = { currentScreen = Screen.Notices },
                            modifier = contentModifier
                        )

                        is Screen.EventDetail -> EventDetailScreen(
                            eventId = screen.eventId,
                            event = allEvents.find { it.id == screen.eventId },
                            onBack = { currentScreen = Screen.Events },
                            modifier = contentModifier
                        )

                        is Screen.Offerings -> OfferingListScreen(
                            onNavigate = { currentScreen = it },
                            offerings = offerings,
                            onBack = { currentScreen = Screen.Home },
                            canManageOfferings = currentUser?.isStaff == true,
                            modifier = contentModifier
                        )

                        is Screen.OfferingDetail -> OfferingDetailScreen(
                            offeringId = screen.offeringId,
                            offering = offerings.find { it.id == screen.offeringId },
                            existingBids = offeringBidsById[screen.offeringId].orEmpty(),
                            bidLoadError = offeringBidsError,
                            bidderProfile = currentUser,
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
                            onBack = { currentScreen = Screen.Offerings },
                            modifier = contentModifier
                        )

                        is Screen.AddOffering -> if (currentUser?.isStaff == true) {
                            AddOfferingScreen(
                                onBack = { currentScreen = Screen.Offerings },
                                onSubmit = { titleGu, amount, locationGu ->
                                    coroutineScope.launch {
                                        val newOffering = Offering(
                                            id = "off_${Clock.System.now().toEpochMilliseconds()}",
                                            titleGu = titleGu,
                                            titleEn = titleGu,
                                            amount = amount,
                                            locationGu = locationGu,
                                            status = "ACTIVE"
                                        )
                                        AppContainer.offeringRepository.submitOffering(newOffering)
                                        currentScreen = Screen.Offerings
                                    }
                                },
                                modifier = contentModifier
                            )
                        } else {
                            OfferingListScreen(
                                onNavigate = { currentScreen = it },
                                offerings = offerings,
                                onBack = { currentScreen = Screen.Home },
                                modifier = contentModifier
                            )
                        }

                        is Screen.Complaints -> ComplaintListScreen(
                            onNavigate = { currentScreen = it },
                            complaints = myComplaints,
                            onBack = { currentScreen = Screen.Home },
                            modifier = contentModifier
                        )

                        is Screen.CreateComplaint -> CreateComplaintScreen(
                            onBack = { currentScreen = Screen.Complaints },
                            onSubmit = { title, description, category ->
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
                            onBack = { currentScreen = Screen.Home },
                            emergencyContacts = emergencyContacts,
                            modifier = contentModifier
                        )

                        else -> HomeScreen(
                            onNavigate = { currentScreen = it },
                            modifier = contentModifier
                        )
                    }
                }
            }
        }
    }
}
