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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    onNavigate: (Screen) -> Unit,
    businesses: List<Business> = emptyList(),
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
                DirectoryTab.BUSINESSES -> BusinessListTab(onNavigate = onNavigate, businesses = businesses)
                DirectoryTab.JOBS -> JobListTab()
                DirectoryTab.PLACES -> PlaceListTab()
                DirectoryTab.OFFERINGS -> Unit
            }
        }
    }
}

@Composable
fun BusinessListTab(
    onNavigate: (Screen) -> Unit,
    businesses: List<Business> = emptyList()
) {
    val lang = LocalAppLanguage.current

    val demoBusinesses = listOf(
        Business(
            id = "demo_biz_1",
            nameGu = "[DEMO DATA] શ્રી અંબિકા કરિયાણા સ્ટોર",
            nameEn = "[DEMO DATA] Shri Ambika Grocery Store",
            descriptionGu = "શુદ્ધ તેલ, અનાજ અને કરિયાણાની તમામ વસ્તુઓ ઉપલબ્ધ છે.",
            descriptionEn = "Pure edible oil, grains, and all grocery items.",
            categoryNameGu = "કરિયાણું અને જનરલ સ્ટોર",
            categoryNameEn = "Grocery & General Store",
            phone = "9800000000",
            whatsapp = "9800000000",
            addressGu = "મુખ્ય બજાર, બોદલા",
            addressEn = "Main Bazaar, Bodla",
            isVerified = true
        ),
        Business(
            id = "demo_biz_2",
            nameGu = "[DEMO DATA] પટેલ ટ્રેક્ટર ગેરેજ & સ્પેરપાર્ટ્સ",
            nameEn = "[DEMO DATA] Patel Tractor Garage & Spare Parts",
            descriptionGu = "તમામ પ્રકારના ટ્રેક્ટર અને કૃષિ સાધનોનું સમારકામ.",
            descriptionEn = "Repair of all types of tractors and farm equipment.",
            categoryNameGu = "વાહન સેવા અને ગેરેજ",
            categoryNameEn = "Automobile & Garage",
            phone = "9800000001",
            whatsapp = "9800000001",
            addressGu = "હાઇવે રોડ, બોદલા",
            addressEn = "Highway Road, Bodla",
            isVerified = true
        )
    )

    val displayBusinesses = if (businesses.isNotEmpty()) businesses else demoBusinesses

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
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
                    DemoDataBadge()
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
                        onClick = { /* Launch Call Intent */ },
                        modifier = Modifier.weight(1f)
                    )
                    BodlaOutlinedButton(
                        text = "💬 " + Strings.whatsapp(lang),
                        onClick = { /* Launch WhatsApp Intent */ },
                        modifier = Modifier.weight(1f)
                    )
                    BodlaOutlinedButton(
                        text = "🗺 " + Strings.map(lang),
                        onClick = { /* Launch Map Intent */ },
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
    val demoJobs = listOf(
        Job(
            id = "demo_job_1",
            titleGu = "[DEMO DATA] ટ્રેક્ટર ડ્રાઈવર જરૂર છે",
            titleEn = "[DEMO DATA] Tractor Driver Required",
            companyName = "પટેલ એગ્રો ફાર્મ",
            locationGu = "બોદલા સીમ વિસ્તાર",
            locationEn = "Bodla Sim Area",
            salaryMin = 15000.0,
            salaryMax = 20000.0,
            employmentType = "FULL_TIME"
        )
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(demoJobs) { job ->
            BodlaCard {
                DemoDataBadge()
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
    val demoPlaces = listOf(
        Place(
            id = "demo_place_1",
            nameGu = "[DEMO DATA] શ્રી મહાદેવ મંદિર",
            nameEn = "[DEMO DATA] Shri Mahadev Temple",
            descriptionGu = "ગામનું પ્રાચીન અને પવિત્ર શિવ મંદિર.",
            descriptionEn = "Ancient and holy Shiva temple of the village.",
            categoryNameGu = "મંદિર / ધાર્મિક સ્થળ",
            categoryNameEn = "Temple / Religious Site",
            addressGu = "મંદિર ચોક, બોદલા",
            addressEn = "Mandir Chowk, Bodla"
        ),
        Place(
            id = "demo_place_2",
            nameGu = "[DEMO DATA] બોદલા ગ્રામ પંચાયત ભવન",
            nameEn = "[DEMO DATA] Bodla Gram Panchayat Bhavan",
            descriptionGu = "ગામના વહીવટી અને નાગરિક સેવાઓનું મુખ્ય કેન્દ્ર.",
            descriptionEn = "Central administrative and civic services building.",
            categoryNameGu = "ગ્રામ પંચાયત ભવન",
            categoryNameEn = "Gram Panchayat Office",
            addressGu = "પંચાયત માર્ગ, બોદલા",
            addressEn = "Panchayat Road, Bodla"
        )
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(demoPlaces) { place ->
            BodlaCard {
                DemoDataBadge()
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
