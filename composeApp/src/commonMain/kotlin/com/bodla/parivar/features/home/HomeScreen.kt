package com.bodla.parivar.features.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bodla.parivar.core.localization.AppLanguage
import com.bodla.parivar.core.localization.LocalAppLanguage
import com.bodla.parivar.core.localization.Strings
import com.bodla.parivar.core.navigation.Screen
import com.bodla.parivar.core.theme.*
import com.bodla.parivar.core.ui.*
import com.bodla.parivar.composeapp.generated.resources.Res
import com.bodla.parivar.composeapp.generated.resources.bodla_parivar_logo
import org.jetbrains.compose.resources.painterResource
import com.bodla.parivar.domain.model.Event
import com.bodla.parivar.domain.model.Notice

@Composable
fun HomeScreen(
    onNavigate: (Screen) -> Unit,
    modifier: Modifier = Modifier,
    userName: String = "ગ્રામજન",
    isOffline: Boolean = false,
    pinnedNotice: Notice? = null,
    upcomingEvents: List<Event> = emptyList(),
    latestNotices: List<Notice> = emptyList()
) {
    val lang = LocalAppLanguage.current
    var searchQuery by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            HomeTopBar(
                onNotificationClick = { /* Handle notification bell */ },
                onProfileClick = { onNavigate(Screen.Profile) }
            )
        },
        containerColor = CreamBackground,
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // Offline Notification Banner
            if (isOffline) {
                item {
                    OfflineBanner()
                }
            }

            // 1. Welcome Greeting Section
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = Strings.greeting(userName, lang),
                        style = BodlaPageTitle,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = Strings.welcomeMessage(lang),
                        style = BodlaBody,
                        color = TextSecondary
                    )
                }
            }

            // 2. Search Bar
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        placeholder = {
                            Text(
                                text = Strings.searchPlaceholder(lang),
                                style = BodlaBody,
                                color = TextTertiary
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = WarmWhiteSurface,
                            unfocusedContainerColor = WarmWhiteSurface,
                            focusedBorderColor = SaffronPrimary,
                            unfocusedBorderColor = BorderStroke
                        ),
                        singleLine = true
                    )
                }
            }

            // 3. Important / Pinned Notice Card
            item {
                val notice = pinnedNotice ?: Notice(
                    id = "demo_pinned",
                    titleGu = "[DEMO DATA] ગ્રામ પંચાયત સામાન્ય સભાનું આયોજન",
                    titleEn = "[DEMO DATA] Gram Panchayat General Meeting Scheduled",
                    descriptionGu = "આગામી રવિવારે પંચાયત હોલ ખાતે બેઠક મળશે.",
                    descriptionEn = "Upcoming community meeting at Panchayat hall.",
                    isPinned = true,
                    createdAt = "2026-09-28"
                )

                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigate(Screen.NoticeDetail(notice.id)) },
                        shape = RoundedCornerShape(CardCornerRadius),
                        colors = CardDefaults.cardColors(containerColor = LightIconBg),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = androidx.compose.ui.graphics.SolidColor(GoldenAccent)
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = Strings.importantNotice(lang),
                                        style = BodlaButton.copy(fontWeight = FontWeight.Bold),
                                        color = SaffronPrimary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    DemoDataBadge()
                                }
                                Text(
                                    text = Strings.viewMore(lang),
                                    style = BodlaCaption.copy(fontWeight = FontWeight.SemiBold),
                                    color = SaffronPrimary
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (lang == AppLanguage.GUJARATI) notice.titleGu else notice.titleEn,
                                style = BodlaCardTitle,
                                color = TextPrimary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // 4. Quick Services Grid (3x3 = 9 services)
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    SectionHeader(title = Strings.quickServices(lang))
                    Spacer(modifier = Modifier.height(6.dp))

                    val services = listOf(
                        Triple(Strings.serviceNotices(lang), "📢", Screen.Notices),
                        Triple(Strings.serviceEvents(lang), "📅", Screen.Events),
                        Triple(Strings.serviceBusinesses(lang), "🏪", Screen.Directory),
                        Triple(Strings.serviceJobs(lang), "💼", Screen.Directory),
                        Triple(Strings.serviceOfferings(lang), "🙏", Screen.Offerings), // ચઢાવો
                        Triple(Strings.serviceEmergency(lang), "🚨", Screen.Emergency),
                        Triple(Strings.servicePlaces(lang), "🏛", Screen.Places),
                        Triple(Strings.serviceGallery(lang), "🖼", Screen.Gallery),
                        Triple(Strings.serviceComplaints(lang), "✍", Screen.Complaints)
                    )

                    // 3 rows of 3 columns
                    for (row in 0 until 3) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            for (col in 0 until 3) {
                                val itemIndex = row * 3 + col
                                val (title, iconSymbol, destination) = services[itemIndex]

                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { onNavigate(destination) }
                                        .padding(vertical = 4.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    AppIconContainer {
                                        Text(
                                            text = iconSymbol,
                                            fontSize = 22.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = title,
                                        style = BodlaNavigation,
                                        color = TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 5. Upcoming Events Section
            item {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        SectionHeader(
                            title = Strings.upcomingEvents(lang),
                            actionText = Strings.viewMore(lang),
                            onActionClick = { onNavigate(Screen.Events) }
                        )
                    }

                    val events = if (upcomingEvents.isNotEmpty()) upcomingEvents else listOf(
                        Event(
                            id = "demo_event_1",
                            titleGu = "[DEMO DATA] વાર્ષિક સાંસ્કૃતિક ઉત્સવ",
                            titleEn = "[DEMO DATA] Annual Cultural Celebration",
                            startAt = "2026-10-05 18:00",
                            locationGu = "બોદલા પ્રાથમિક શાળા મેદાન",
                            locationEn = "Bodla Primary School Ground"
                        ),
                        Event(
                            id = "demo_event_2",
                            titleGu = "[DEMO DATA] વિનામૂલ્યે આરોગ્ય તપાસ કેમ્પ",
                            titleEn = "[DEMO DATA] Free Medical Health Camp",
                            startAt = "2026-10-12 09:00",
                            locationGu = "પ્રાથમિક આરોગ્ય કેન્દ્ર",
                            locationEn = "Primary Health Center"
                        )
                    )

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(events) { event ->
                            Card(
                                modifier = Modifier
                                    .width(260.dp)
                                    .clickable { onNavigate(Screen.EventDetail(event.id)) },
                                shape = RoundedCornerShape(CardCornerRadius),
                                colors = CardDefaults.cardColors(containerColor = WarmWhiteSurface),
                                border = CardDefaults.outlinedCardBorder().copy(
                                    brush = androidx.compose.ui.graphics.SolidColor(BorderStroke)
                                )
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    DemoDataBadge()
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = if (lang == AppLanguage.GUJARATI) event.titleGu else event.titleEn,
                                        style = BodlaCardTitle,
                                        color = TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "📍 " + (if (lang == AppLanguage.GUJARATI) event.locationGu.orEmpty() else event.locationEn.orEmpty()),
                                        style = BodlaCaption,
                                        color = TextSecondary,
                                        maxLines = 1
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "🕒 " + event.startAt,
                                        style = BodlaCaption,
                                        color = SaffronPrimary,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 6. Latest Notices Section
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    SectionHeader(
                        title = Strings.latestNotices(lang),
                        actionText = Strings.viewMore(lang),
                        onActionClick = { onNavigate(Screen.Notices) }
                    )

                    val notices = if (latestNotices.isNotEmpty()) latestNotices else listOf(
                        Notice(
                            id = "demo_notice_1",
                            titleGu = "[DEMO DATA] પીવાના પાણીના વિતરણ સમયમાં ફેરફાર",
                            titleEn = "[DEMO DATA] Change in Drinking Water Distribution Schedule",
                            createdAt = "2026-09-27"
                        ),
                        Notice(
                            id = "demo_notice_2",
                            titleGu = "[DEMO DATA] પશુપાલન સહાય યોજના અરજી શરૂ",
                            titleEn = "[DEMO DATA] Animal Husbandry Scheme Applications Open",
                            createdAt = "2026-09-25"
                        )
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        notices.forEach { notice ->
                            BodlaCard(
                                onClick = { onNavigate(Screen.NoticeDetail(notice.id)) }
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (lang == AppLanguage.GUJARATI) notice.titleGu else notice.titleEn,
                                            style = BodlaCardTitle,
                                            color = TextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = notice.createdAt,
                                            style = BodlaCaption,
                                            color = TextSecondary
                                        )
                                    }
                                    DemoDataBadge()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeTopBar(
    onNotificationClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    val lang = LocalAppLanguage.current

    Surface(
        color = WarmWhiteSurface,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(Res.drawable.bodla_parivar_logo),
                        contentDescription = Strings.APP_NAME_GU,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.size(width = 132.dp, height = 38.dp)
                    )
                }
                Column {
                    Text(
                        text = if (lang == AppLanguage.GUJARATI) Strings.VILLAGE_SUBTITLE_GU else Strings.VILLAGE_SUBTITLE_EN,
                        style = BodlaCaption,
                        color = TextSecondary
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                // Notification Bell
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(LightIconBg)
                        .clickable { onNotificationClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "🔔", fontSize = 18.sp)
                }

                // Profile Avatar
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(LightIconBg)
                        .clickable { onProfileClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "👤", fontSize = 18.sp)
                }
            }
        }
    }
}
