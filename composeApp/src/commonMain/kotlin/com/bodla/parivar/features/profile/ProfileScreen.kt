package com.bodla.parivar.features.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bodla.parivar.core.localization.AppLanguage
import com.bodla.parivar.core.localization.LocalAppLanguage
import com.bodla.parivar.core.localization.Strings
import com.bodla.parivar.core.navigation.Screen
import com.bodla.parivar.core.theme.*
import com.bodla.parivar.core.ui.*
import com.bodla.parivar.domain.model.Profile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigate: (Screen) -> Unit,
    onLanguageChange: (AppLanguage) -> Unit,
    onLogout: () -> Unit,
    profile: Profile? = null,
    modifier: Modifier = Modifier
) {
    val lang = LocalAppLanguage.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = Strings.navProfile(lang), style = BodlaPageTitle) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = WarmWhiteSurface)
            )
        },
        containerColor = CreamBackground,
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Profile or Login/Register Card
            item {
                if (profile != null) {
                    BodlaCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(LightIconBg),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = "👤", fontSize = 32.sp)
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = profile.fullName,
                                        style = BodlaSection,
                                        color = TextPrimary
                                    )
                                    if (profile.isVerified) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "✓",
                                            color = StatusSuccess,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = profile.phone ?: profile.email.orEmpty(),
                                    style = BodlaCaption,
                                    color = TextSecondary
                                )
                                if (!profile.address.isNullOrBlank()) {
                                    Text(
                                        text = profile.address,
                                        style = BodlaCaption,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }
                    }
                } else {
                    BodlaCard {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(CircleShape)
                                    .background(LightIconBg),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = "🔒", fontSize = 28.sp)
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = if (lang == AppLanguage.GUJARATI) "બોદલા પરિવાર સભ્યપદ" else "Bodla Parivar Membership",
                                style = BodlaSection,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (lang == AppLanguage.GUJARATI) "ફરિયાદ નિવારણ, ચઢાવો અને અંગત સેવાઓ માટે પ્રવેશ કરો" else "Sign in to access grievance tracking, offerings, and personal services",
                                style = BodlaCaption,
                                color = TextSecondary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                BodlaButton(
                                    text = if (lang == AppLanguage.GUJARATI) "પ્રવેશ કરો" else "Log In",
                                    onClick = { onNavigate(Screen.Login) },
                                    modifier = Modifier.weight(1f)
                                )
                                BodlaButton(
                                    text = if (lang == AppLanguage.GUJARATI) "નવી નોંધણી" else "Register",
                                    onClick = { onNavigate(Screen.Register) },
                                    containerColor = LightIconBg,
                                    contentColor = SaffronPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            if (profile?.isStaff == true) {
                item {
                    BodlaCard {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = if (lang == AppLanguage.GUJARATI) "વહીવટકર્તા સાધનો" else "Administrator tools",
                                style = BodlaCardTitle,
                                color = TextPrimary
                            )
                            BodlaButton(
                                text = if (lang == AppLanguage.GUJARATI) "એડમિન વર્કસ્પેસ ખોલો" else "Open admin workspace",
                                onClick = { onNavigate(Screen.AdminHome) }
                            )
                        }
                    }
                }
            }

            // Language Selector Card
            item {
                BodlaCard {
                    Text(
                        text = Strings.preferredLanguage(lang),
                        style = BodlaCardTitle,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            color = if (lang == AppLanguage.GUJARATI) SaffronPrimary else LightIconBg,
                            shape = RoundedCornerShape(ButtonCornerRadius),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(ButtonCornerRadius))
                                .clickable { onLanguageChange(AppLanguage.GUJARATI) }
                        ) {
                            Text(
                                text = "ગુજરાતી",
                                style = BodlaButton,
                                color = if (lang == AppLanguage.GUJARATI) WarmWhiteSurface else TextPrimary,
                                modifier = Modifier.padding(vertical = 12.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }

                        Surface(
                            color = if (lang == AppLanguage.ENGLISH) SaffronPrimary else LightIconBg,
                            shape = RoundedCornerShape(ButtonCornerRadius),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(ButtonCornerRadius))
                                .clickable { onLanguageChange(AppLanguage.ENGLISH) }
                        ) {
                            Text(
                                text = "English",
                                style = BodlaButton,
                                color = if (lang == AppLanguage.ENGLISH) WarmWhiteSurface else TextPrimary,
                                modifier = Modifier.padding(vertical = 12.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            }

            // Menu Items List
            item {
                BodlaCard {
                    val menuItems = listOf(
                        Pair("✍ " + Strings.myComplaints(lang), { onNavigate(Screen.Complaints) }),
                        Pair("🙏 " + Strings.myOfferings(lang), { onNavigate(Screen.Offerings) }),
                        Pair("🏪 " + (if (lang == AppLanguage.GUJARATI) "મારા વ્યવસાયો" else "My Businesses"), { onNavigate(Screen.Directory) }),
                        Pair("📞 " + Strings.serviceEmergency(lang), { onNavigate(Screen.Emergency) })
                    )

                    menuItems.forEachIndexed { index, (label, action) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { action() }
                                .padding(vertical = 12.dp, horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = label, style = BodlaBody, color = TextPrimary)
                            Text(text = "→", style = BodlaBody, color = TextSecondary)
                        }
                        if (index < menuItems.size - 1) {
                            Divider(color = BorderStroke)
                        }
                    }
                }
            }

            // Logout Button (Only if logged in)
            if (profile != null) {
                item {
                    BodlaButton(
                        text = Strings.logout(lang),
                        onClick = onLogout,
                        containerColor = StatusError.copy(alpha = 0.9f),
                        contentColor = WarmWhiteSurface
                    )
                }
            }
        }
    }
}
