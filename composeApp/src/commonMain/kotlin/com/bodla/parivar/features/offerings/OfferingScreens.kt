package com.bodla.parivar.features.offerings

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.bodla.parivar.core.localization.AppLanguage
import com.bodla.parivar.core.localization.LocalAppLanguage
import com.bodla.parivar.core.localization.Strings
import com.bodla.parivar.core.navigation.Screen
import com.bodla.parivar.core.theme.*
import com.bodla.parivar.core.ui.*
import com.bodla.parivar.domain.model.Offering
import com.bodla.parivar.domain.model.OfferingBid
import com.bodla.parivar.domain.model.Profile
import kotlinx.coroutines.launch

// ==============================================================================
// OFFERINGS / ચઢાવો MODULE  (Live Bidding Feature)
// ==============================================================================

// ------------------------------------------------------------------------------
// Offering List Screen
// ------------------------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfferingListScreen(
    onNavigate: (Screen) -> Unit,
    offerings: List<Offering> = emptyList(),
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    canManageOfferings: Boolean = false
) {
    val lang = LocalAppLanguage.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = Strings.offeringsTitle(lang), style = BodlaPageTitle) },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Text(text = "←", fontSize = 22.sp, color = TextPrimary)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = WarmWhiteSurface)
            )
        },
        floatingActionButton = {
            if (canManageOfferings) {
                FloatingActionButton(
                    onClick = { onNavigate(Screen.AddOffering) },
                    containerColor = SaffronPrimary,
                    contentColor = WarmWhiteSurface
                ) {
                    Text(text = "+", fontSize = 24.sp)
                }
            }
        },
        containerColor = CreamBackground,
        modifier = modifier
    ) { innerPadding ->
        val displayOfferings = if (offerings.isNotEmpty()) offerings else listOf(
            Offering(
                id = "demo_offering_1",
                titleGu = "[DEMO DATA] શ્રી મહાદેવ મંદિર ધ્વજારોહણ સેવા",
                titleEn = "[DEMO DATA] Shri Mahadev Temple Dhwajarohan Seva",
                descriptionGu = "આ ડેમો ચઢાવો છે. શ્રાવણ માસ નિમિત્તે મંદિર ધ્વજારોહણ ચઢાવા સેવા.",
                descriptionEn = "This is demo data. Temple flag hoisting offering seva for the holy month of Shravan.",
                categoryNameGu = "ધ્વજારોહણ ચઢાવો",
                categoryNameEn = "Temple Flag Hoisting",
                amount = 5100.0,
                locationGu = "બોદલા મંદિર સંકુલ",
                locationEn = "Bodla Temple Complex",
                status = "ACTIVE"
            ),
            Offering(
                id = "demo_offering_2",
                titleGu = "[DEMO DATA] ગૌશાળા ઘાસચારો સહાય સેવા",
                titleEn = "[DEMO DATA] Gaushala Fodder Offering Seva",
                descriptionGu = "આ ડેમો ચઢાવો છે. ગામની ગૌશાળા માટે લીલા ઘાસચારાની દૈનિક સેવા.",
                descriptionEn = "This is demo data. Daily fodder offering for the village gaushala.",
                categoryNameGu = "ગૌશાળા ઘાસચારો",
                categoryNameEn = "Gaushala Fodder",
                amount = 2100.0,
                locationGu = "બોદલા ગૌશાળા",
                locationEn = "Bodla Gaushala",
                status = "ACTIVE"
            )
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(displayOfferings) { offering ->
                BodlaCard(
                    onClick = { onNavigate(Screen.OfferingDetail(offering.id)) }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val category = if (lang == AppLanguage.GUJARATI) offering.categoryNameGu else offering.categoryNameEn
                        if (!category.isNullOrBlank()) {
                            Surface(
                                color = LightIconBg,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = category,
                                    style = BodlaCaption,
                                    color = SaffronPrimary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                        // Live dot for ACTIVE offerings
                        if (offering.status == "ACTIVE") {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val pulsing by rememberInfiniteTransition(label = "pulse")
                                    .animateFloat(
                                        initialValue = 0.4f, targetValue = 1f,
                                        animationSpec = infiniteRepeatable(
                                            tween(700, easing = FastOutSlowInEasing),
                                            RepeatMode.Reverse
                                        ),
                                        label = "alpha"
                                    )
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF22C55E).copy(alpha = pulsing))
                                )
                                Text(
                                    text = if (lang == AppLanguage.GUJARATI) "લાઇવ" else "LIVE",
                                    style = BodlaCaption,
                                    color = Color(0xFF16A34A),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        DemoDataBadge()
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (lang == AppLanguage.GUJARATI) offering.titleGu else offering.titleEn,
                        style = BodlaCardTitle,
                        color = TextPrimary
                    )
                    if (offering.amount != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = Strings.offeringAmount(lang) + ": ₹" + offering.amount.toInt().toString(),
                            style = BodlaBody,
                            color = SaffronPrimary
                        )
                    }
                    val loc = if (lang == AppLanguage.GUJARATI) offering.locationGu else offering.locationEn
                    if (!loc.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "📍 $loc", style = BodlaCaption, color = TextSecondary)
                    }
                    // Tap hint for active offerings
                    if (offering.status == "ACTIVE") {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (lang == AppLanguage.GUJARATI)
                                "👆 વિગત જોવા અને બોલી નોંધવા ટૅપ કરો"
                            else
                                "👆 Tap to view & place a bid",
                            style = BodlaCaption,
                            color = SaffronPrimary.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------------------------
// Offering Detail Screen — with Bid placement
// ------------------------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfferingDetailScreen(
    offeringId: String,
    onBack: () -> Unit,
    offering: Offering? = null,
    existingBids: List<OfferingBid> = emptyList(),
    bidLoadError: String? = null,
    bidderProfile: Profile? = null,
    onPlaceBid: suspend (amount: Double) -> Result<Unit>,
    modifier: Modifier = Modifier
) {
    val lang = LocalAppLanguage.current
    val coroutineScope = rememberCoroutineScope()
    var showBidDialog by remember { mutableStateOf(false) }
    var bidSuccessMessage by remember { mutableStateOf<String?>(null) }
    var bidSubmitErrorMessage by remember { mutableStateOf<String?>(null) }
    var isSubmittingBid by remember { mutableStateOf(false) }

    val currentOffering = offering ?: Offering(
        id = offeringId,
        titleGu = "[DEMO DATA] શ્રી મહાદેવ મંદિર ધ્વજારોહણ સેવા",
        titleEn = "[DEMO DATA] Shri Mahadev Temple Dhwajarohan Seva",
        descriptionGu = "આ ડેમો ચઢાવો છે. શ્રાવણ માસ દરમિયાન ગામના શ્રી મહાદેવ મંદિરે પરંપરાગત ધ્વજારોહણ સેવા ચઢાવો નોંધાવેલ છે. આમાં સહભાગી થવા ઈચ્છુક ગ્રામજનો સંપર્ક કરી શકે છે.",
        descriptionEn = "This is demo data. Traditional flag hoisting offering for Shri Mahadev Temple during Shravan month. Community members wishing to participate can contact.",
        categoryNameGu = "ધ્વજારોહણ ચઢાવો",
        categoryNameEn = "Temple Flag Hoisting",
        amount = 5100.0,
        locationGu = "બોદલા મંદિર સંકુલ",
        locationEn = "Bodla Temple Complex",
        status = "ACTIVE"
    )

    val isActive = currentOffering.status == "ACTIVE"
    val topBid = currentOffering.winningBidAmount ?: existingBids
        .filter { bid -> bid.status != "REJECTED" }
        .maxOfOrNull { bid -> bid.amount }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = Strings.offeringDetails(lang), style = BodlaPageTitle) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text(text = "←", fontSize = 22.sp, color = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = WarmWhiteSurface)
            )
        },
        // Show bid button only for ACTIVE offerings
        bottomBar = {
            if (isActive) {
                Surface(
                    shadowElevation = 8.dp,
                    color = WarmWhiteSurface
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Current highest bid info
                        topBid?.let {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (lang == AppLanguage.GUJARATI) "વર્તમાન ઉચ્ચ બોલી:" else "Current Highest Bid:",
                                    style = BodlaCaption,
                                    color = TextSecondary
                                )
                                val callSuffix = when (currentOffering.bidCallCount) {
                                    1 -> if (lang == AppLanguage.GUJARATI) " · ૧ વાર" else " · Call 1"
                                    2 -> if (lang == AppLanguage.GUJARATI) " · ૨ વાર" else " · Call 2"
                                    3 -> if (lang == AppLanguage.GUJARATI) " · ૩ વાર (બંધ)" else " · Final"
                                    else -> if (currentOffering.bidCallCount > 0) " · ${currentOffering.bidCallCount} વાર" else ""
                                }
                                Text(
                                    text = "₹${it.toInt().toLocaleString()}$callSuffix",
                                    style = BodlaSection,
                                    color = SaffronPrimary,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }

                        Button(
                            onClick = { showBidDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            enabled = bidderProfile != null,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SaffronPrimary,
                                contentColor = WarmWhiteSurface
                            )
                        ) {
                            Text(
                                text = when {
                                    bidderProfile == null && lang == AppLanguage.GUJARATI -> "બોલી માટે સાઇન ઇન કરો"
                                    bidderProfile == null -> "Sign in to place a bid"
                                    lang == AppLanguage.GUJARATI -> "🙏 થઈ બોલી (Thayi Boli) – બોલી નોંધાવો"
                                    else -> "🙏 Place Thai Bid"
                                },
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        },
        containerColor = CreamBackground,
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // ── Main Offering Card ──────────────────────────────────────
            item {
                BodlaCard {
                    // Demo badge + status
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        DemoDataBadge()
                        if (isActive) {
                            val pulsing by rememberInfiniteTransition(label = "pulse")
                                .animateFloat(
                                    initialValue = 0.5f, targetValue = 1f,
                                    animationSpec = infiniteRepeatable(
                                        tween(600, easing = FastOutSlowInEasing),
                                        RepeatMode.Reverse
                                    ),
                                    label = "alpha"
                                )
                            Surface(
                                color = Color(0xFF22C55E).copy(alpha = 0.15f),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF22C55E).copy(alpha = pulsing))
                                    )
                                    Text(
                                        text = if (lang == AppLanguage.GUJARATI) "LIVE બોલી" else "LIVE",
                                        style = BodlaCaption,
                                        color = Color(0xFF15803D),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (lang == AppLanguage.GUJARATI) currentOffering.titleGu else currentOffering.titleEn,
                        style = BodlaPageTitle,
                        color = TextPrimary
                    )

                    if (currentOffering.amount != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(LightIconBg)
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = if (lang == AppLanguage.GUJARATI) "આધાર રકમ" else "Base Amount",
                                    style = BodlaCaption,
                                    color = TextSecondary
                                )
                                Text(
                                    text = "₹${currentOffering.amount.toInt().toLocaleString()}",
                                    style = BodlaSection,
                                    color = SaffronPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            if (topBid != null) {
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = if (lang == AppLanguage.GUJARATI) "ઉચ્ચ બોલી" else "Highest Bid",
                                        style = BodlaCaption,
                                        color = TextSecondary
                                    )
                                    val callCountText = when (currentOffering.bidCallCount) {
                                        1 -> if (lang == AppLanguage.GUJARATI) " · ૧ વાર" else " · Call 1"
                                        2 -> if (lang == AppLanguage.GUJARATI) " · ૨ વાર" else " · Call 2"
                                        3 -> if (lang == AppLanguage.GUJARATI) " · ૩ વાર (બંધ)" else " · Final"
                                        else -> if (currentOffering.bidCallCount > 0) " · ${currentOffering.bidCallCount} વાર" else ""
                                    }
                                    Text(
                                        text = "₹${topBid.toInt().toLocaleString()}$callCountText",
                                        style = BodlaSection,
                                        color = Color(0xFF16A34A),
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }

                        }
                    }

                    // ── Live Call Banner (1, 2, 3 vaar) ──────────────────────────
                    if (isActive && currentOffering.bidCallCount in 1..3) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            color = Color(0xFFFEF3C7),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFF59E0B)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text("📢", fontSize = 22.sp)
                                Column {
                                    val vaarTitle = when (currentOffering.bidCallCount) {
                                        1 -> if (lang == AppLanguage.GUJARATI) "૧ વાર બોલો (Call 1/3)" else "Call 1 of 3"
                                        2 -> if (lang == AppLanguage.GUJARATI) "૨ વાર બોલો (Call 2/3)" else "Call 2 of 3"
                                        3 -> if (lang == AppLanguage.GUJARATI) "૩ વાર · અંતિમ બોલી બંધ (Call 3/3)" else "Call 3 of 3 (Final)"
                                        else -> "${currentOffering.bidCallCount} વાર"
                                    }
                                    Text(
                                        text = "${if (topBid != null) "₹" + topBid.toInt().toLocaleString() + " · " else ""}$vaarTitle",
                                        style = BodlaSection,
                                        color = Color(0xFFB45309),
                                        fontWeight = FontWeight.Black
                                    )
                                    Text(
                                        text = if (currentOffering.bidCallCount < 3) {
                                            if (lang == AppLanguage.GUJARATI) "વધુ બોલી લગાવવા નીચેના બટન પર ટૅપ કરો!" else "Place a higher bid to participate!"
                                        } else {
                                            if (lang == AppLanguage.GUJARATI) "બોલી સંપન્ન થઈ ગઈ છે." else "Bidding completed."
                                        },
                                        style = BodlaCaption,
                                        color = Color(0xFF92400E)
                                    )
                                }
                            }
                        }
                    }

                    val loc = if (lang == AppLanguage.GUJARATI) currentOffering.locationGu else currentOffering.locationEn
                    if (!loc.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "📍 $loc", style = BodlaBody, color = TextSecondary)
                    }

                    // ── Winner Announcement ───────────────────────────────────
                    if (currentOffering.status == "COMPLETED" && !currentOffering.winningBidderName.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            color = Color(0xFFDCFCE7),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = if (lang == AppLanguage.GUJARATI) "વિજેતા ઊંચી બોલી" else "Winning high bid",
                                    style = BodlaCaption,
                                    color = Color(0xFF15803D),
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = currentOffering.winningBidderName,
                                    style = BodlaBody,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                                currentOffering.winningBidAmount?.let { amount ->
                                    Text(
                                        text = "₹${amount.toInt().toLocaleString()}",
                                        style = BodlaSection,
                                        color = Color(0xFF15803D),
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Divider(color = BorderStroke)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = (if (lang == AppLanguage.GUJARATI) currentOffering.descriptionGu else currentOffering.descriptionEn).orEmpty(),
                        style = BodlaBody,
                        color = TextPrimary,
                        lineHeight = 26.sp
                    )
                }
            }

            // ── Bid Success Banner ──────────────────────────────────────
            bidLoadError?.let { error ->
                item {
                    Text(
                        text = if (lang == AppLanguage.GUJARATI)
                            "બોલી અપડેટ થઈ શકી નથી: $error"
                        else
                            "Could not refresh bids: $error",
                        style = BodlaCaption,
                        color = Color(0xFF9F1239),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                    )
                }
            }
            bidSubmitErrorMessage?.let { error ->
                item {
                    Text(
                        text = if (lang == AppLanguage.GUJARATI)
                            "બોલી સિંક થઈ નથી: $error"
                        else
                            "Bid was not synced: $error",
                        style = BodlaCaption,
                        color = Color(0xFF9F1239),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                    )
                }
            }
            bidSuccessMessage?.let { msg ->
                item {
                    Surface(
                        color = Color(0xFFDCFCE7),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color(0xFF86EFAC), RoundedCornerShape(12.dp))
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF16A34A),
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = msg,
                                style = BodlaBody,
                                color = Color(0xFF15803D),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // ── Bid History Section ─────────────────────────────────────
            val visibleBids = if (bidderProfile?.isStaff == true) {
                existingBids
            } else {
                existingBids.filter { bid -> bid.status != "REJECTED" }
            }

            if (visibleBids.isNotEmpty()) {
                item {
                    Text(
                        text = if (lang == AppLanguage.GUJARATI)
                            "📋 ચઢાવાની બોલીઓ (${visibleBids.size})"
                        else
                            "📋 Offering bids (${visibleBids.size})",
                        style = BodlaSection,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }

                items(
                    visibleBids.sortedByDescending { it.createdAt },
                    key = { it.id }
                ) { bid ->
                    BidHistoryItem(
                        bid = bid,
                        lang = lang,
                        showPhone = bidderProfile?.isStaff == true
                    )
                }
            }

            // Bottom spacing for the fixed bottom bar
            if (isActive) item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }

    // ── Bid Placement Dialog ─────────────────────────────────────────────
    if (showBidDialog) {
        BidPlacementDialog(
            offering = currentOffering,
            currentHighestBid = topBid,
            bidderName = bidderProfile?.fullName.orEmpty(),
            bidderPhone = bidderProfile?.phone,
            lang = lang,
            onDismiss = { if (!isSubmittingBid) showBidDialog = false },
            onSubmit = { amount ->
                if (!isSubmittingBid) {
                    coroutineScope.launch {
                        isSubmittingBid = true
                        bidSubmitErrorMessage = null
                        bidSuccessMessage = null
                        val result = onPlaceBid(amount)
                        isSubmittingBid = false
                        showBidDialog = false
                        result.onSuccess {
                            bidSuccessMessage = if (lang == AppLanguage.GUJARATI)
                                "🎉 તમારી ₹${amount.toInt().toLocaleString()} ની બોલી સફળતાપૂર્વક નોંધાઈ! (Pending Approval)"
                            else
                                "🎉 Your bid of ₹${amount.toInt().toLocaleString()} was placed! (Pending Approval)"
                        }.onFailure { error ->
                            bidSubmitErrorMessage = error.message ?: "Please try again."
                        }
                    }
                }
            }
        )
    }
}

// ------------------------------------------------------------------------------
// Bid History Item Card
// ------------------------------------------------------------------------------
@Composable
private fun BidHistoryItem(bid: OfferingBid, lang: AppLanguage, showPhone: Boolean) {
    val isApproved = bid.status == "APPROVED"
    val isPending  = bid.status == "PENDING"
    val isRejected = bid.status == "REJECTED"

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = when {
            isApproved -> Color(0xFFF0FDF4)
            isRejected -> Color(0xFFFFF1F2)
            else       -> WarmWhiteSurface
        },
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = when {
                    isApproved -> Color(0xFF86EFAC)
                    isRejected -> Color(0xFFFCE7F3)
                    else       -> BorderStroke
                },
                shape = RoundedCornerShape(12.dp)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = bid.bidderName,
                    style = BodlaBody,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                if (showPhone && !bid.bidderPhone.isNullOrBlank()) {
                    Text(
                        text = "📞 ${bid.bidderPhone}",
                        style = BodlaCaption,
                        color = TextSecondary
                    )
                }
                // Status chip
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    color = when {
                        isApproved -> Color(0xFF22C55E).copy(alpha = 0.15f)
                        isRejected -> Color(0xFFF43F5E).copy(alpha = 0.12f)
                        else       -> Color(0xFFF59E0B).copy(alpha = 0.15f)
                    },
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        text = when {
                            isApproved -> if (lang == AppLanguage.GUJARATI) "✓ મંજૂર" else "✓ Approved"
                            isRejected -> if (lang == AppLanguage.GUJARATI) "✗ નામંજૂર" else "✗ Rejected"
                            else       -> if (lang == AppLanguage.GUJARATI) "⏳ સમીક્ષા" else "⏳ Pending"
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = BodlaCaption,
                        color = when {
                            isApproved -> Color(0xFF15803D)
                            isRejected -> Color(0xFFBE123C)
                            else       -> Color(0xFFD97706)
                        },
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = "₹${bid.amount.toInt().toLocaleString()}",
                style = BodlaSection,
                color = when {
                    isApproved -> Color(0xFF16A34A)
                    isRejected -> TextSecondary
                    else       -> SaffronPrimary
                },
                fontWeight = FontWeight.Black
            )
        }
    }
}

// ------------------------------------------------------------------------------
// Bid Placement Dialog (Thai Bid / થઈ બોલી)
// ------------------------------------------------------------------------------
@Composable
fun BidPlacementDialog(
    offering: Offering,
    currentHighestBid: Double?,
    bidderName: String,
    bidderPhone: String?,
    lang: AppLanguage,
    onDismiss: () -> Unit,
    onSubmit: (amount: Double) -> Unit
) {
    var bidAmount   by remember { mutableStateOf("") }
    var amountError by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = true
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(20.dp),
            color = WarmWhiteSurface,
            shadowElevation = 16.dp
        ) {
            Column(modifier = Modifier.padding(24.dp)) {

                // ── Header ──────────────────────────────────────────────
                Text(
                    text = if (lang == AppLanguage.GUJARATI)
                        "🙏 બોલી નોંધાવો"
                    else
                        "🙏 Place a Thai Bid",
                    style = BodlaPageTitle,
                    color = TextPrimary
                )
                Text(
                    text = if (lang == AppLanguage.GUJARATI) offering.titleGu else offering.titleEn,
                    style = BodlaCaption,
                    color = TextSecondary,
                    modifier = Modifier.padding(top = 4.dp)
                )

                // Minimum / highest bid hint
                Spacer(modifier = Modifier.height(12.dp))
                val baseAmount = currentHighestBid ?: offering.amount
                if (baseAmount != null) {
                    Surface(
                        color = LightIconBg,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (lang == AppLanguage.GUJARATI) "ઉચ્ચ/આધાર રકમ:" else "Current/Base Amt:",
                                style = BodlaCaption,
                                color = TextSecondary
                            )
                            Text(
                                text = "₹${baseAmount.toInt().toLocaleString()}",
                                style = BodlaBody,
                                color = SaffronPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Divider(color = BorderStroke)
                Spacer(modifier = Modifier.height(16.dp))

                // ── Name ────────────────────────────────────────────────
                OutlinedTextField(
                    value = bidderName,
                    onValueChange = {},
                    label = {
                        Text(
                            if (lang == AppLanguage.GUJARATI)
                                "પ્રોફાઇલનું નામ"
                            else
                                "Profile name"
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    readOnly = true,
                    supportingText = {
                        Text(if (lang == AppLanguage.GUJARATI) "આ નામ તમારી પ્રોફાઇલમાંથી લેવામાં આવ્યું છે" else "Taken from your profile")
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // ── Phone ────────────────────────────────────────────────
                OutlinedTextField(
                    value = bidderPhone.orEmpty(),
                    onValueChange = {},
                    label = {
                        Text(
                            if (lang == AppLanguage.GUJARATI)
                                "ફોન નંબર (વૈકલ્પિક)"
                            else
                                "Phone (Optional)"
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    readOnly = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Phone,
                        imeAction = ImeAction.Next
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // ── Bid Amount ───────────────────────────────────────────
                OutlinedTextField(
                    value = bidAmount,
                    onValueChange = {
                        bidAmount = it
                        amountError = null
                    },
                    label = {
                        Text(
                            if (lang == AppLanguage.GUJARATI)
                                "બોલી રકમ (₹) *"
                            else
                                "Bid Amount (₹) *"
                        )
                    },
                    isError = amountError != null,
                    supportingText = amountError?.let { err -> { Text(err) } },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    prefix = { Text("₹", color = SaffronPrimary, fontWeight = FontWeight.Bold) }
                )

                Spacer(modifier = Modifier.height(20.dp))

                // ── Action Buttons ───────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            if (lang == AppLanguage.GUJARATI) "રદ કરો" else "Cancel",
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Button(
                        onClick = {
                            // Validate
                            val parsedAmount = bidAmount.toDoubleOrNull()
                            amountError = when {
                                bidAmount.isBlank() -> if (lang == AppLanguage.GUJARATI) "રકમ દાખલ કરો" else "Enter amount"
                                parsedAmount == null -> if (lang == AppLanguage.GUJARATI) "માન્ય રકમ દાખલ કરો" else "Enter valid amount"
                                parsedAmount <= 0 -> if (lang == AppLanguage.GUJARATI) "રકમ 0 કરતાં વધારે હોવી જોઈએ" else "Amount must be > 0"
                                else -> null
                            }
                            if (amountError == null && parsedAmount != null) {
                                onSubmit(parsedAmount)
                            }
                        },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SaffronPrimary,
                            contentColor = WarmWhiteSurface
                        )
                    ) {
                        Text(
                            if (lang == AppLanguage.GUJARATI) "🙏 બોલી નોંધો" else "🙏 Confirm Bid",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Disclaimer
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = if (lang == AppLanguage.GUJARATI)
                        "* બોલી નોંધ્યા પછી admin ની મંજૂરી આવ્યે અસ્તિત્વ ધરાવે."
                    else
                        "* Bids require admin approval before being accepted.",
                    style = BodlaCaption,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

// ------------------------------------------------------------------------------
// Add Offering Screen (for community-side submission)
// ------------------------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddOfferingScreen(
    onBack: () -> Unit,
    onSubmit: (titleGu: String, amount: Double, locationGu: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val lang = LocalAppLanguage.current
    var title by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = Strings.addOffering(lang), style = BodlaPageTitle) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text(text = "←", fontSize = 22.sp, color = TextPrimary)
                    }
                },
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text(if (lang == AppLanguage.GUJARATI) "ચઢાવાનું નામ / શીર્ષક" else "Offering Title") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                label = { Text(Strings.offeringAmount(lang)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                prefix = { Text("₹") }
            )

            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                label = { Text(Strings.offeringLocation(lang)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text(if (lang == AppLanguage.GUJARATI) "વર્ણન" else "Description") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            BodlaButton(
                text = Strings.submitOffering(lang),
                onClick = {
                    onSubmit(title, amount.toDoubleOrNull() ?: 0.0, location)
                    onBack()
                },
                enabled = title.isNotBlank()
            )
        }
    }
}

// ------------------------------------------------------------------------------
// Private helpers
// ------------------------------------------------------------------------------
private fun Double.toLocaleString(): String = toInt().toString() // Simple fallback
private fun Int.toLocaleString(): String {
    if (this < 1000) return toString()
    val s = toString()
    val result = StringBuilder()
    var count = 0
    for (i in s.lastIndex downTo 0) {
        if (count > 0 && count % 3 == 0) result.insert(0, ',')
        result.insert(0, s[i])
        count++
    }
    return result.toString()
}
