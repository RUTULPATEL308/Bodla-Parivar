package com.bodla.parivar.features.directory

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.ktor.http.encodeURLParameter
import com.bodla.parivar.core.localization.AppLanguage
import com.bodla.parivar.core.localization.LocalAppLanguage
import com.bodla.parivar.core.localization.Strings
import com.bodla.parivar.core.navigation.Screen
import com.bodla.parivar.core.theme.*
import com.bodla.parivar.core.ui.*
import com.bodla.parivar.domain.model.Business
import com.bodla.parivar.domain.model.Job
import com.bodla.parivar.domain.model.Place

enum class DirectoryTab {
    BUSINESSES, JOBS, PLACES, OFFERINGS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DirectoryScreen(
    businesses: List<Business> = emptyList(),
    onNavigate: (Screen) -> Unit,
    modifier: Modifier = Modifier
) {
    val lang = LocalAppLanguage.current
    var selectedTab by remember { mutableStateOf(DirectoryTab.BUSINESSES) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = Strings.navDirectory(lang), style = BodlaPageTitle) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = WarmWhiteSurface)
            )
        },
        containerColor = CreamBackground,
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Tab Selector Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    Pair(DirectoryTab.BUSINESSES, Strings.serviceBusinesses(lang)),
                    Pair(DirectoryTab.JOBS, Strings.serviceJobs(lang)),
                    Pair(DirectoryTab.PLACES, Strings.servicePlaces(lang)),
                    Pair(DirectoryTab.OFFERINGS, Strings.serviceOfferings(lang))
                ).forEach { (tab, title) ->
                    val isSelected = selectedTab == tab
                    Surface(
                        color = if (isSelected) SaffronPrimary else WarmWhiteSurface,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                if (tab == DirectoryTab.OFFERINGS) {
                                    onNavigate(Screen.OfferingDetail("list"))
                                } else {
                                    selectedTab = tab
                                }
                            }
                    ) {
                        Text(
                            text = title,
                            style = BodlaCaption.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            ),
                            color = if (isSelected) WarmWhiteSurface else TextPrimary,
                            modifier = Modifier.padding(vertical = 8.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }

            when (selectedTab) {
                DirectoryTab.BUSINESSES -> BusinessListTab(businesses = businesses)
                DirectoryTab.JOBS -> JobListTab()
                DirectoryTab.PLACES -> PlaceListTab()
                DirectoryTab.OFFERINGS -> Unit
            }
        }
    }
}

@Composable
fun BusinessListTab(
    businesses: List<Business> = emptyList()
) {
    val lang = LocalAppLanguage.current
    val uriHandler = LocalUriHandler.current

    val displayBusinesses = businesses

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (displayBusinesses.isEmpty()) {
            item { EmptyStateView(if (lang == AppLanguage.GUJARATI) "હજુ કોઈ વ્યવસાયની માહિતી ઉપલબ્ધ નથી." else "No business listings are available yet.") }
        }
        items(displayBusinesses) { business ->
            BodlaCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (business.isVerified) {
                        Surface(
                            color = StatusSuccess.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "✓ " + Strings.verifiedBadge(lang),
                                color = StatusSuccess,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (lang == AppLanguage.GUJARATI) business.nameGu else business.nameEn,
                    style = BodlaCardTitle,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "📂 " + (if (lang == AppLanguage.GUJARATI) business.categoryNameGu else business.categoryNameEn).orEmpty(),
                    style = BodlaCaption,
                    color = SaffronPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "📍 " + (if (lang == AppLanguage.GUJARATI) business.addressGu else business.addressEn).orEmpty(),
                    style = BodlaCaption,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Action buttons: Call, WhatsApp, Map
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BodlaOutlinedButton(
                        text = "📞 " + Strings.call(lang),
                        onClick = { business.phone?.trim()?.takeIf(String::isNotBlank)?.let { uriHandler.openUri("tel:$it") } },
                        modifier = Modifier.weight(1f)
                    )
                    BodlaOutlinedButton(
                        text = "💬 " + Strings.whatsapp(lang),
                        onClick = {
                            business.whatsapp?.filter(Char::isDigit)?.takeIf(String::isNotBlank)?.let { number ->
                                val internationalNumber = if (number.length == 10) "91$number" else number
                                uriHandler.openUri("https://wa.me/$internationalNumber")
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                    BodlaOutlinedButton(
                        text = "🗺 " + Strings.map(lang),
                        onClick = {
                            val address = (if (lang == AppLanguage.GUJARATI) business.addressGu else business.addressEn).orEmpty()
                            address.takeIf(String::isNotBlank)?.let {
                                uriHandler.openUri("https://www.google.com/maps/search/?api=1&query=${it.encodeURLParameter()}")
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun JobListTab() {
    val lang = LocalAppLanguage.current
    val displayJobs = emptyList<Job>()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (displayJobs.isEmpty()) {
            item { EmptyStateView(if (lang == AppLanguage.GUJARATI) "હાલમાં કોઈ નોકરીની જાહેરાત નથી." else "No job listings are available yet.") }
        }
        items(displayJobs) { job ->
            BodlaCard {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (lang == AppLanguage.GUJARATI) job.titleGu else job.titleEn,
                    style = BodlaCardTitle,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "🏢 " + job.companyName.orEmpty(), style = BodlaCaption, color = TextSecondary)
                Text(text = "📍 " + (if (lang == AppLanguage.GUJARATI) job.locationGu else job.locationEn).orEmpty(), style = BodlaCaption, color = TextSecondary)
                if (job.salaryMin != null && job.salaryMax != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "₹${job.salaryMin.toInt()} - ₹${job.salaryMax.toInt()}",
                        style = BodlaButton.copy(fontSize = 14.sp),
                        color = SaffronPrimary
                    )
                }
            }
        }
    }
}

@Composable
fun PlaceListTab() {
    val lang = LocalAppLanguage.current
    val displayPlaces = emptyList<Place>()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (displayPlaces.isEmpty()) {
            item { EmptyStateView(if (lang == AppLanguage.GUJARATI) "હજુ કોઈ સ્થળની માહિતી ઉપલબ્ધ નથી." else "No places are available yet.") }
        }
        items(displayPlaces) { place ->
            BodlaCard {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (lang == AppLanguage.GUJARATI) place.nameGu else place.nameEn,
                    style = BodlaCardTitle,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "🏛 " + (if (lang == AppLanguage.GUJARATI) place.categoryNameGu else place.categoryNameEn).orEmpty(), style = BodlaCaption, color = SaffronPrimary)
                Text(text = "📍 " + (if (lang == AppLanguage.GUJARATI) place.addressGu else place.addressEn).orEmpty(), style = BodlaCaption, color = TextSecondary)
            }
        }
    }
}
