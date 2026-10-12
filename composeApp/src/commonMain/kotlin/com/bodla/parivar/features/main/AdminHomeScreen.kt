package com.bodla.parivar.features.main

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bodla.parivar.core.localization.AppLanguage
import com.bodla.parivar.core.localization.LocalAppLanguage
import com.bodla.parivar.core.navigation.Screen
import com.bodla.parivar.core.theme.*
import com.bodla.parivar.core.ui.BodlaButton
import com.bodla.parivar.core.ui.BodlaOutlinedButton
import com.bodla.parivar.domain.model.Notice
import com.bodla.parivar.domain.model.Offering
import com.bodla.parivar.domain.model.Profile
import com.bodla.parivar.domain.repository.AuthRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminHomeScreen(
    profile: Profile?,
    authRepository: AuthRepository,
    notices: List<Notice>,
    offerings: List<Offering>,
    onNavigate: (Screen) -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lang = LocalAppLanguage.current
    var memberCount by remember(profile?.id) { mutableStateOf<Int?>(null) }
    var memberLoadFailed by remember(profile?.id) { mutableStateOf(false) }
    val publishedNoticeCount = notices.count { it.status == "PUBLISHED" }
    val activeOfferingCount = offerings.count { it.status == "ACTIVE" }
    val pendingOfferings = offerings.filter { it.status == "PENDING_APPROVAL" || it.status == "PENDING" }
    val isGujarati = lang == AppLanguage.GUJARATI

    LaunchedEffect(profile?.id, profile?.isAdmin) {
        if (profile?.isAdmin == true) {
            authRepository.getRegisteredUsers()
                .onSuccess { memberCount = it.size }
                .onFailure { memberLoadFailed = true }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isGujarati) "ડેશબોર્ડ" else "Dashboard", style = BodlaPageTitle) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = WarmWhiteSurface)
            )
        },
        containerColor = CreamBackground,
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = if (isGujarati) "બોદલા ગ્રામ પંચાયત અને સમુદાય" else "BODLA VILLAGE ADMINISTRATION",
                        style = BodlaCaption.copy(fontWeight = FontWeight.Bold),
                        color = SaffronPrimary
                    )
                    Text(
                        text = if (isGujarati) "નમસ્કાર, ${profile?.fullName.orEmpty()}" else "Welcome, ${profile?.fullName.orEmpty()}",
                        style = BodlaPageTitle,
                        color = TextPrimary
                    )
                    Text(
                        text = if (isGujarati) "આજનું સમુદાય સંચાલન" else "Village community overview",
                        style = BodlaBody,
                        color = TextSecondary
                    )
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (profile?.isAdmin == true) {
                            DashboardAction(
                                title = if (isGujarati) "સભ્યો" else "Members",
                                onClick = { onNavigate(Screen.AdminUsers) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        DashboardAction(
                            title = if (isGujarati) "સૂચનાઓ" else "Notices",
                            onClick = { onNavigate(Screen.Notices) },
                            modifier = Modifier.weight(1f)
                        )
                        DashboardAction(
                            title = if (isGujarati) "ચઢાવો" else "Offerings",
                            onClick = { onNavigate(Screen.Offerings) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (profile?.isAdmin == true) {
                        BodlaButton(
                            text = if (isGujarati) "નવો ચઢાવો ઉમેરો" else "Add an offering",
                            onClick = { onNavigate(Screen.AddOffering) }
                        )
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = if (isGujarati) "ઝાંખી" else "OVERVIEW",
                        style = BodlaCaption.copy(fontWeight = FontWeight.Bold),
                        color = TextSecondary
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        DashboardMetric(
                            title = if (isGujarati) "નોંધાયેલા સભ્યો" else "Registered members",
                            value = if (memberLoadFailed) "—" else memberCount?.toString() ?: "…",
                            modifier = Modifier.weight(1f)
                        )
                        DashboardMetric(
                            title = if (isGujarati) "પ્રકાશિત સૂચનાઓ" else "Published notices",
                            value = publishedNoticeCount.toString(),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        DashboardMetric(
                            title = if (isGujarati) "સક્રિય ચઢાવો" else "Active offerings",
                            value = activeOfferingCount.toString(),
                            modifier = Modifier.weight(1f)
                        )
                        DashboardMetric(
                            title = if (isGujarati) "મંજૂરી બાકી" else "Awaiting review",
                            value = pendingOfferings.size.toString(),
                            modifier = Modifier.weight(1f),
                            accent = pendingOfferings.isNotEmpty()
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isGujarati) "મંજૂરી માટે બાકી" else "Pending approvals",
                            style = BodlaCardTitle,
                            color = TextPrimary
                        )
                        Text(
                            text = if (isGujarati) "ચઢાવાની વિનંતીઓ તપાસો" else "Review offering requests",
                            style = BodlaCaption,
                            color = TextSecondary
                        )
                    }
                    BodlaOutlinedButton(
                        text = if (isGujarati) "બધા જુઓ" else "View all",
                        onClick = { onNavigate(Screen.Offerings) }
                    )
                }
            }

            if (pendingOfferings.isEmpty()) {
                item {
                    Text(
                        text = if (isGujarati) "હાલમાં કોઈ ચઢાવાની મંજૂરી બાકી નથી." else "There are no offering requests awaiting review.",
                        style = BodlaBody,
                        color = TextSecondary,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                    )
                }
            } else {
                items(pendingOfferings.take(5), key = { it.id }) { offering ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onNavigate(Screen.Offerings) },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = WarmWhiteSurface),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = androidx.compose.ui.graphics.SolidColor(BorderStroke)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "🙏", fontSize = 22.sp)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isGujarati) offering.titleGu else offering.titleEn,
                                    style = BodlaCardTitle,
                                    color = TextPrimary,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (isGujarati) "ચઢાવાની વિનંતી" else "Offering request",
                                    style = BodlaCaption,
                                    color = TextSecondary
                                )
                            }
                            Text(text = "→", fontSize = 20.sp, color = SaffronPrimary)
                        }
                    }
                }
            }

            item {
                BodlaOutlinedButton(
                    text = if (isGujarati) "સાઇન આઉટ" else "Sign out",
                    onClick = onLogout,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun DashboardAction(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.height(52.dp),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = WarmWhiteSurface),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(BorderStroke)
        )
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp)) {
            Text(text = title, style = BodlaCaption.copy(fontWeight = FontWeight.SemiBold), color = TextPrimary, maxLines = 1)
        }
    }
}

@Composable
private fun DashboardMetric(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    accent: Boolean = false
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = WarmWhiteSurface),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(if (accent) GoldenAccent else BorderStroke)
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(text = title, style = BodlaCaption, color = TextSecondary, maxLines = 2)
            Text(text = value, style = BodlaPageTitle.copy(fontSize = 26.sp), color = if (accent) SaffronPrimary else TextPrimary)
        }
    }
}