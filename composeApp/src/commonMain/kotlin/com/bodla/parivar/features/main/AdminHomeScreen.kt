package com.bodla.parivar.features.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
    val roles = profile?.roles.orEmpty().map { it.uppercase() }.toSet()
    val isSuperAdmin = "SUPER_ADMIN" in roles
    val isAdmin = profile?.isAdmin == true || "ADMIN" in roles || isSuperAdmin
    val isModerator = "MODERATOR" in roles
    val isContentManager = "CONTENT_MANAGER" in roles
    val roleLabel = when {
        isSuperAdmin -> if (isGujarati) "સુપર એડમિન" else "Super Administrator"
        isAdmin -> if (isGujarati) "એડમિન" else "Administrator"
        isModerator -> if (isGujarati) "મોડરેટર" else "Moderator"
        isContentManager -> if (isGujarati) "કન્ટેન્ટ મેનેજર" else "Content Manager"
        else -> if (isGujarati) "સ્ટાફ" else "Staff"
    }

    LaunchedEffect(profile?.id, isAdmin) {
        if (isAdmin) {
            authRepository.getRegisteredUsers()
                .onSuccess { memberCount = it.size }
                .onFailure { memberLoadFailed = true }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isGujarati) "ડેશબોર્ડ" else if (isSuperAdmin) "Super Admin Dashboard" else "$roleLabel Dashboard", style = BodlaPageTitle) },
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
                        text = roleLabel,
                        style = BodlaBody,
                        color = TextSecondary
                    )
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (isAdmin) DashboardAction(
                        title = if (isSuperAdmin) "Users & roles" else if (isGujarati) "સભ્યો" else "Members",
                        icon = "👥",
                        onClick = { onNavigate(Screen.AdminUsers) }
                    )
                    if (isAdmin || isContentManager) DashboardAction(
                        title = if (isGujarati) "સૂચનાઓ" else "Notices",
                        icon = "📢",
                        onClick = { onNavigate(Screen.Notices) }
                    )
                    if (isAdmin || isModerator) DashboardAction(
                        title = if (isGujarati) "ચઢાવો" else "Offerings",
                        icon = "🙏",
                        onClick = { onNavigate(Screen.Offerings) }
                    )
                    if (isAdmin) DashboardAction(
                        title = if (isGujarati) "નવો ચઢાવો ઉમેરો" else "Add an offering",
                        icon = "＋",
                        onClick = { onNavigate(Screen.AddOffering) }
                    )
                    if (isAdmin || isContentManager) DashboardAction(
                        title = if (isGujarati) "નવરાત્રી વ્યવસ્થાપન" else "Navratri management",
                        icon = "✨",
                        onClick = { onNavigate(Screen.Navratri) }
                    )
                    if (isAdmin || isModerator) DashboardAction(
                        title = if (isGujarati) "ફરિયાદો" else "Complaints",
                        icon = "✍️",
                        onClick = { onNavigate(Screen.Complaints) }
                    )
                    BodlaOutlinedButton(
                        text = if (isGujarati) "રહેવાસી હોમ જુઓ" else "Open resident home",
                        onClick = { onNavigate(Screen.Home) },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp)
                    )
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = if (isGujarati) "ઝાંખી" else "Overview",
                        style = BodlaSection,
                        color = TextSecondary
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (isAdmin) DashboardMetric(
                            title = if (isGujarati) "નોંધાયેલા સભ્યો" else "Registered members",
                            value = if (memberLoadFailed) "—" else memberCount?.toString() ?: "…",
                            modifier = Modifier.weight(1f)
                        )
                        if (isAdmin || isContentManager) DashboardMetric(
                            title = if (isGujarati) "પ્રકાશિત સૂચનાઓ" else "Published notices",
                            value = publishedNoticeCount.toString(),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (isAdmin || isModerator) Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
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

            if (isAdmin || isModerator) item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isGujarati) "મંજૂરી માટે બાકી" else "Pending approvals",
                            style = BodlaSection,
                            color = TextPrimary
                        )
                        Text(
                            text = if (isGujarati) "ચઢાવાની વિનંતીઓ તપાસો" else "Review offering requests",
                            style = BodlaBody,
                            color = TextSecondary
                        )
                    }
                    BodlaOutlinedButton(
                        text = if (isGujarati) "બધા જુઓ" else "View all",
                        onClick = { onNavigate(Screen.Offerings) }
                    )
                }
            }

            if (isAdmin || isModerator) {
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
                            modifier = Modifier.fillMaxWidth().heightIn(min = 76.dp).padding(18.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "🙏", fontSize = 22.sp)
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isGujarati) offering.titleGu else offering.titleEn,
                                        style = BodlaBody.copy(fontWeight = FontWeight.SemiBold),
                                        color = TextPrimary,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = if (isGujarati) "ચઢાવાની વિનંતી" else "Offering request",
                                        style = BodlaBody.copy(fontSize = 14.sp),
                                        color = TextSecondary
                                    )
                                }
                                Text(text = "→", fontSize = 20.sp, color = SaffronPrimary)
                            }
                        }
                    }
                }
            }

            item {
                BodlaOutlinedButton(
                    text = if (isGujarati) "સાઇન આઉટ" else "Sign out",
                    onClick = onLogout,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp)
                )
            }
        }
    }
}

@Composable
private fun DashboardAction(
    title: String,
    icon: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth()
) {
    Card(
        onClick = onClick,
        modifier = modifier.heightIn(min = 76.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = WarmWhiteSurface),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(BorderStroke)
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = icon, fontSize = 26.sp)
            Text(
                text = title,
                modifier = Modifier.weight(1f),
                style = BodlaBody.copy(fontWeight = FontWeight.SemiBold),
                color = TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(text = "›", fontSize = 28.sp, color = SaffronPrimary)
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
        modifier = modifier.heightIn(min = 112.dp),
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
            Text(text = title, style = BodlaBody.copy(fontSize = 15.sp), color = TextSecondary, maxLines = 2)
            Text(text = value, style = BodlaPageTitle.copy(fontSize = 30.sp), color = if (accent) SaffronPrimary else TextPrimary)
        }
    }
}
