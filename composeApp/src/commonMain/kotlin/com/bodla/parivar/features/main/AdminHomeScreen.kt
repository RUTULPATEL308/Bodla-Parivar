package com.bodla.parivar.features.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import com.bodla.parivar.core.di.AppContainer
import com.bodla.parivar.core.localization.AppLanguage
import com.bodla.parivar.core.localization.LocalAppLanguage
import com.bodla.parivar.core.navigation.Screen
import com.bodla.parivar.core.theme.*
import com.bodla.parivar.core.ui.AppIconContainer
import com.bodla.parivar.core.ui.BodlaButton
import com.bodla.parivar.core.ui.BodlaOutlinedButton
import com.bodla.parivar.core.ui.formatShortDateTime
import com.bodla.parivar.domain.model.Notice
import com.bodla.parivar.domain.model.Offering
import com.bodla.parivar.domain.model.Profile
import com.bodla.parivar.domain.repository.AuthRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminHomeScreen(
    profile: Profile?,
    authRepository: AuthRepository,
    notices: List<Notice>,
    offerings: List<Offering>,
    onNavigate: (Screen) -> Unit,
    onLogout: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val lang = LocalAppLanguage.current
    var dashboardSearch by remember { mutableStateOf("") }
    var previewAttachmentUrl by remember { mutableStateOf<String?>(null) }
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
    val grantedFeatures = profile?.featurePermissions.orEmpty().toSet()
    val canAccess: (String) -> Boolean = { feature -> feature in grantedFeatures }
    val roleLabel = when {
        isSuperAdmin -> if (isGujarati) "સુપર એડમિન" else "Super Administrator"
        isAdmin -> if (isGujarati) "એડમિન" else "Administrator"
        isModerator -> if (isGujarati) "મોડરેટર" else "Moderator"
        isContentManager -> if (isGujarati) "કન્ટેન્ટ મેનેજર" else "Content Manager"
        profile?.featureContext == true -> if (isGujarati) "આપેલ સુવિધા ઍક્સેસ" else "Assigned feature access"
        else -> if (isGujarati) "સ્ટાફ" else "Staff"
    }
    val dashboardActions: List<Triple<String, String, Screen>> = buildList {
        if (isAdmin || canAccess("members")) add(Triple(if (isGujarati) "સભ્યો" else "My member access", "👤", Screen.AdminUsers))
        if (profile?.isStaff == true || canAccess("notices")) add(Triple(if (isGujarati) "સૂચનાઓ" else "Notices", "📢", Screen.Notices))
        if (profile?.isStaff == true || canAccess("offerings")) add(Triple(if (isGujarati) "ચઢાવો" else "Offerings", "🙏", Screen.Offerings))
        if (profile?.isStaff == true || canAccess("navratri")) {
            add(Triple(if (isGujarati) "નવરાત્રી" else "Navratri", "✨", Screen.Navratri))
        }
        if (profile?.isStaff == true || canAccess("village_history")) add(Triple(if (isGujarati) "ગામનો ઇતિહાસ" else "Village history", "📖", Screen.VillageHistory))
        add(Triple(if (isGujarati) "પરિણામ" else "Results", "🎓", Screen.ResultUpload))
        add(Triple(if (isGujarati) "અંતિમ યાત્રા પોસ્ટ" else "Antim Yatra Post", "🕯️", Screen.NoticeDesign))
        add(Triple(if (isGujarati) "બેસણું પોસ્ટ" else "Besanu Post", "🕊️", Screen.BesanuDesign))
        add(Triple(if (isGujarati) "રહેવાસી હોમ" else "Resident home", "🏠", Screen.Home))
    }
    var pendingNotices by remember { mutableStateOf<List<Notice>>(emptyList()) }
    val coroutineScope = rememberCoroutineScope()
    val filteredActions = dashboardActions.filter { (title, _, _) ->
        title.contains(dashboardSearch.trim(), ignoreCase = true)
    }

    LaunchedEffect(profile?.id, isAdmin, grantedFeatures) {
        if (isAdmin || canAccess("members")) {
            authRepository.getRegisteredUsers()
                .onSuccess { memberCount = it.size }
                .onFailure { memberLoadFailed = true }
        }
        runCatching { AppContainer.noticeRepository.getPendingNotices() }
            .onSuccess { pendingNotices = it.getOrDefault(emptyList()) }
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
                com.bodla.parivar.core.ui.GujlishTextField(
                    value = dashboardSearch,
                    onValueChange = { dashboardSearch = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text(if (isGujarati) "સુવિધા શોધો (ગુજલિશ / ગુજરાતી)" else "Search dashboard features") },
                    shape = RoundedCornerShape(12.dp)
                )
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (isGujarati) "ઝડપી સુવિધાઓ" else "Quick services",
                        style = BodlaSection,
                        color = TextPrimary
                    )
                    for (row in 0 until (filteredActions.size + 2) / 3) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            for (column in 0 until 3) {
                                val index = row * 3 + column
                                if (index < filteredActions.size) {
                                    val (title, icon, destination) = filteredActions[index]
                                    DashboardAction(
                                        title = title,
                                        icon = icon,
                                        onClick = { onNavigate(destination) },
                                        modifier = Modifier.weight(1f)
                                    )
                                } else {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                    if (filteredActions.isEmpty()) {
                        Text(
                            text = if (isGujarati) "કોઈ સુવિધા મળી નથી." else "No matching dashboard features.",
                            style = BodlaBody,
                            color = TextSecondary
                        )
                    }
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

            if (isAdmin || isModerator || isContentManager || profile?.isStaff == true) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = if (isGujarati) "મંજૂરી માટે બાકી નોટિસો (Pending Notices)" else "Pending Notices for Approval",
                            style = BodlaSection,
                            color = TextPrimary
                        )
                        if (pendingNotices.isEmpty()) {
                            Text(
                                text = if (isGujarati) "હાલમાં કોઈ નોટિસ મંજૂરી માટે બાકી નથી." else "No notices awaiting approval.",
                                style = BodlaBody,
                                color = TextSecondary,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        } else {
                            pendingNotices.forEach { notice ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = WarmWhiteSurface),
                                    border = CardDefaults.outlinedCardBorder().copy(
                                        brush = androidx.compose.ui.graphics.SolidColor(GoldenAccent)
                                    )
                                ) {
                                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(
                                            text = if (isGujarati) notice.titleGu else notice.titleEn,
                                            style = BodlaCardTitle,
                                            color = TextPrimary
                                        )
                                        val desc = if (isGujarati) notice.descriptionGu else notice.descriptionEn
                                        if (!desc.isNullOrBlank()) {
                                            Text(
                                                text = desc,
                                                style = BodlaBody.copy(fontSize = 13.sp),
                                                color = TextSecondary,
                                                maxLines = 3,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        val publisher = notice.publisherName?.takeIf { it.isNotBlank() }
                                        if (publisher != null) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = formatShortDateTime(notice.createdAt),
                                                    style = BodlaCaption,
                                                    color = TextTertiary
                                                )
                                                Text(
                                                    text = if (isGujarati) "પ્રકાશક: $publisher" else "By $publisher",
                                                    style = BodlaCaption.copy(fontSize = 11.sp),
                                                    color = TextSecondary.copy(alpha = 0.65f)
                                                )
                                            }
                                        } else {
                                            Text(
                                                text = formatShortDateTime(notice.createdAt),
                                                style = BodlaCaption,
                                                color = TextTertiary
                                            )
                                        }

                                        val imageUrl = notice.imageUrl
                                        if (!imageUrl.isNullOrBlank()) {
                                            AsyncImage(
                                                model = imageUrl,
                                                contentDescription = "Notice image",
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .heightIn(max = 140.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                            )
                                        }

                                        val attachmentUrl = notice.attachmentUrl
                                        if (!attachmentUrl.isNullOrBlank()) {
                                            val uriHandler = LocalUriHandler.current
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                modifier = Modifier.fillMaxWidth().padding(top = 2.dp)
                                            ) {
                                                androidx.compose.material3.OutlinedButton(
                                                    onClick = { previewAttachmentUrl = attachmentUrl },
                                                    shape = RoundedCornerShape(8.dp),
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Text(
                                                        text = if (isGujarati) "📄 દસ્તાવેજ જુઓ" else "📄 View Document",
                                                        style = BodlaCaption.copy(fontWeight = FontWeight.SemiBold),
                                                        color = SaffronPrimary
                                                    )
                                                }
                                                androidx.compose.material3.OutlinedButton(
                                                    onClick = {
                                                        uriHandler.openUri(attachmentUrl + if (attachmentUrl.contains('?')) "&download" else "?download")
                                                    },
                                                    shape = RoundedCornerShape(8.dp),
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Text(
                                                        text = if (isGujarati) "⬇️ ડાઉનલોડ" else "⬇️ Download",
                                                        style = BodlaCaption.copy(fontWeight = FontWeight.SemiBold),
                                                        color = TextPrimary
                                                    )
                                                }
                                            }
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            BodlaButton(
                                                text = if (isGujarati) "મંજૂર કરો (Approve)" else "Approve",
                                                onClick = {
                                                    coroutineScope.launch {
                                                        AppContainer.noticeRepository.approveNotice(notice.id)
                                                            .onSuccess {
                                                                pendingNotices = pendingNotices.filter { it.id != notice.id }
                                                                AppContainer.noticeRepository.refreshNotices()
                                                            }
                                                    }
                                                },
                                                modifier = Modifier.weight(1f)
                                            )
                                            BodlaOutlinedButton(
                                                text = if (isGujarati) "ડિલીટ (Delete)" else "Delete",
                                                onClick = {
                                                    coroutineScope.launch {
                                                        AppContainer.noticeRepository.deleteNotice(notice.id)
                                                            .onSuccess {
                                                                pendingNotices = pendingNotices.filter { it.id != notice.id }
                                                            }
                                                    }
                                                },
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (!previewAttachmentUrl.isNullOrBlank()) {
        val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { previewAttachmentUrl = null },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            androidx.compose.material3.Surface(
                shape = RoundedCornerShape(20.dp),
                color = WarmWhiteSurface,
                modifier = Modifier
                    .fillMaxWidth(0.94f)
                    .fillMaxHeight(0.85f)
                    .padding(12.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    com.bodla.parivar.features.notices.NoticeAttachmentPreview(
                        url = previewAttachmentUrl!!,
                        modifier = Modifier.weight(1f).fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        androidx.compose.material3.OutlinedButton(
                            onClick = { uriHandler.openUri(previewAttachmentUrl + if (previewAttachmentUrl!!.contains('?')) "&download" else "?download") },
                            modifier = Modifier.weight(1f).height(46.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) { Text(if (isGujarati) "ડાઉનલોડ કરો" else "Download", fontWeight = FontWeight.Bold) }
                        androidx.compose.material3.TextButton(
                            onClick = { previewAttachmentUrl = null },
                            modifier = Modifier.weight(1f).height(46.dp)
                        ) { Text(if (isGujarati) "બંધ કરો" else "Close", fontWeight = FontWeight.SemiBold, color = TextSecondary) }
                    }
                }
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
        modifier = modifier.heightIn(min = 128.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = WarmWhiteSurface),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(BorderStroke)
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically)
        ) {
            AppIconContainer {
                Text(text = icon, fontSize = 22.sp)
            }
            Text(
                text = title,
                style = BodlaNavigation.copy(fontWeight = FontWeight.SemiBold),
                color = TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
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
