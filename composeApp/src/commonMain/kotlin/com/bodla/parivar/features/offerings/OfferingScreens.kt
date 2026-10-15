package com.bodla.parivar.features.offerings

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.text.style.TextOverflow
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
import com.bodla.parivar.core.di.AppContainer
import com.bodla.parivar.domain.repository.WinnerFlowException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

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
    val coroutineScope = rememberCoroutineScope()
    var searchQuery by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf("ALL") }
    var actionError by remember { mutableStateOf<String?>(null) }
    var busyOfferingId by remember { mutableStateOf<String?>(null) }
    var deleteConfirmation by remember { mutableStateOf<Offering?>(null) }
    var allBids by remember { mutableStateOf<List<OfferingBid>>(emptyList()) }
    val statusFilters = listOf("ALL", "ACTIVE", "PENDING_APPROVAL", "APPROVED", "COMPLETED", "REJECTED")

    LaunchedEffect(canManageOfferings, offerings.map { it.id }) {
        if (canManageOfferings) {
            AppContainer.offeringRepository.getAllOfferingBids()
                .onSuccess { allBids = it }
        }
    }

    LaunchedEffect(canManageOfferings) {
        if (!canManageOfferings) return@LaunchedEffect
        while (true) {
            AppContainer.offeringRepository.getAllOfferingBids()
                .onSuccess { allBids = it }
            delay(5000)
        }
    }

    fun updateStatus(offering: Offering, status: String) {
        coroutineScope.launch {
            busyOfferingId = offering.id
            actionError = null
            AppContainer.offeringRepository.updateOfferingStatus(offering.id, status)
                .onFailure { actionError = it.message ?: "Could not update offering status." }
            AppContainer.offeringRepository.getAllOfferingBids().onSuccess { allBids = it }
            busyOfferingId = null
        }
    }

    fun deleteOffering(offering: Offering) {
        coroutineScope.launch {
            busyOfferingId = offering.id
            actionError = null
            AppContainer.offeringRepository.deleteOffering(offering.id)
                .onFailure { actionError = it.message ?: "Could not delete offering." }
            AppContainer.offeringRepository.getAllOfferingBids().onSuccess { allBids = it }
            busyOfferingId = null
            deleteConfirmation = null
        }
    }

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
        val displayOfferings = offerings

        val filteredOfferings = displayOfferings.filter { offering ->
            val matchesSearch = searchQuery.isBlank() ||
                offering.titleGu.contains(searchQuery, ignoreCase = true) ||
                offering.titleEn.contains(searchQuery, ignoreCase = true)
            val matchesStatus = statusFilter == "ALL" || offering.status == statusFilter
            matchesSearch && matchesStatus
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (canManageOfferings) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OfferingListMetric(
                                title = if (lang == AppLanguage.GUJARATI) "સક્રિય" else "Active",
                                value = displayOfferings.count { it.status == "ACTIVE" }.toString(),
                                modifier = Modifier.weight(1f)
                            )
                            OfferingListMetric(
                                title = if (lang == AppLanguage.GUJARATI) "કુલ બોલીઓ" else "Total bids",
                                value = allBids.size.toString(),
                                modifier = Modifier.weight(1f)
                            )
                            OfferingListMetric(
                                title = if (lang == AppLanguage.GUJARATI) "સમીક્ષા બાકી" else "Pending bids",
                                value = allBids.count { it.status == "PENDING" }.toString(),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            label = { Text(if (lang == AppLanguage.GUJARATI) "ચઢાવો શોધો" else "Search offerings") },
                            shape = RoundedCornerShape(12.dp)
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(statusFilters) { filter ->
                                FilterChip(
                                    selected = statusFilter == filter,
                                    onClick = { statusFilter = filter },
                                    label = { Text(if (filter == "ALL") if (lang == AppLanguage.GUJARATI) "બધા" else "All" else filter.replace('_', ' ')) }
                                )
                            }
                        }
                    }
                }
            }

            actionError?.let { message ->
                item { Text(message, color = MaterialTheme.colorScheme.error, style = BodlaCaption) }
            }


            if (filteredOfferings.isEmpty()) {
                item {
                    Text(
                        text = if (searchQuery.isNotBlank() || statusFilter != "ALL") {
                            if (lang == AppLanguage.GUJARATI) "કોઈ ચઢાવો મળ્યો નથી." else "No offerings match this filter."
                        } else {
                            if (lang == AppLanguage.GUJARATI) "હાલમાં કોઈ ચઢાવો ઉપલબ્ધ નથી." else "No offerings are available yet."
                        },
                        style = BodlaBody,
                        color = TextSecondary,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp)
                    )
                }
            }

            items(filteredOfferings, key = { it.id }) { offering ->
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
                        if (offering.isBidOpen) {
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
                    if (offering.isBidOpen) {
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
                    if (canManageOfferings) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            when (offering.status) {
                                "PENDING_APPROVAL" -> {
                                    TextButton(
                                        enabled = busyOfferingId != offering.id,
                                        onClick = { updateStatus(offering, "APPROVED") }
                                    ) { Text(if (lang == AppLanguage.GUJARATI) "મંજૂર" else "Approve") }
                                }
                                "APPROVED" -> Unit
                                "ACTIVE" -> {
                                    TextButton(
                                        enabled = busyOfferingId != offering.id,
                                        onClick = { updateStatus(offering, "COMPLETED") }
                                    ) { Text(if (lang == AppLanguage.GUJARATI) "પૂર્ણ કરો" else "Complete") }
                                }
                            }
                            if (offering.status == "APPROVED" || offering.status == "PENDING_APPROVAL") {
                                TextButton(
                                    enabled = busyOfferingId != offering.id,
                                    onClick = { updateStatus(offering, "ACTIVE") }
                                ) { Text(if (lang == AppLanguage.GUJARATI) "સક્રિય કરો" else "Activate") }
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            TextButton(
                                enabled = busyOfferingId != offering.id,
                                onClick = { deleteConfirmation = offering }
                            ) { Text(if (lang == AppLanguage.GUJARATI) "કાઢી નાખો" else "Delete", color = MaterialTheme.colorScheme.error) }
                        }
                    }
                }
            }
        }
    }

    deleteConfirmation?.let { offering ->
        AlertDialog(
            onDismissRequest = { if (busyOfferingId == null) deleteConfirmation = null },
            title = { Text(if (lang == AppLanguage.GUJARATI) "ચઢાવો કાઢી નાખવો?" else "Delete offering?") },
            text = {
                Text(
                    if (lang == AppLanguage.GUJARATI) "આ ચઢાવો અને તેની બોલીઓ કાયમ માટે કાઢી નાખવામાં આવશે."
                    else "This permanently deletes the offering and its bids."
                )
            },
            confirmButton = {
                TextButton(
                    enabled = busyOfferingId == null,
                    onClick = { deleteOffering(offering) }
                ) { Text(if (lang == AppLanguage.GUJARATI) "કાઢી નાખો" else "Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { deleteConfirmation = null }) { Text(if (lang == AppLanguage.GUJARATI) "રદ કરો" else "Cancel") }
            }
        )
    }
}

@Composable
private fun OfferingListMetric(title: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = WarmWhiteSurface),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderStroke))
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(10.dp)) {
            Text(text = value, style = BodlaCardTitle, color = TextPrimary)
            Text(text = title, style = BodlaCaption, color = TextSecondary, maxLines = 1)
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
    onRefreshBids: suspend () -> Unit,
    modifier: Modifier = Modifier
) {
    val lang = LocalAppLanguage.current
    val coroutineScope = rememberCoroutineScope()
    var showBidDialog by remember { mutableStateOf(false) }
    var bidSuccessMessage by remember { mutableStateOf<String?>(null) }
    var bidSubmitErrorMessage by remember { mutableStateOf<String?>(null) }
    var isSubmittingBid by remember { mutableStateOf(false) }
    var adminActionError by remember(offeringId) { mutableStateOf<String?>(null) }
    var adminBidderName by remember { mutableStateOf("") }
    var adminBidAmount by remember { mutableStateOf("") }
    var adminWinnerName by remember { mutableStateOf("") }
    var isSavingAdminAction by remember { mutableStateOf(false) }
    var bidToDelete by remember { mutableStateOf<OfferingBid?>(null) }
    var showCallConfirmation by remember { mutableStateOf(false) }

    if (offering == null) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(if (lang == AppLanguage.GUJARATI) "ચઢાવો" else "Offering", style = BodlaPageTitle) },
                    navigationIcon = {
                        IconButton(onClick = onBack) { Text("←", fontSize = 22.sp, color = TextPrimary) }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = WarmWhiteSurface)
                )
            },
            containerColor = CreamBackground,
            modifier = modifier
        ) { padding ->
            EmptyStateView(
                message = if (lang == AppLanguage.GUJARATI) "આ ચઢાવો હવે ઉપલબ્ધ નથી." else "This offering is no longer available.",
                modifier = Modifier.fillMaxSize().padding(padding)
            )
        }
        return
    }
    val currentOffering = offering
    val isActive = currentOffering.isBidOpen
    val topBid = resolveCurrentHighestBid(existingBids, currentOffering.winningBidAmount)

    var serverTimeOffsetMillis by remember { mutableStateOf(0L) }
    LaunchedEffect(Unit) {
        val offsetResult = AppContainer.offeringRepository.getServerTimeOffsetMillis()
        if (offsetResult.isSuccess) {
            serverTimeOffsetMillis = offsetResult.getOrDefault(0L)
        }
    }

    val canBeWinnerPromptUser = bidderProfile != null && !bidderProfile.isStaff
    var isVerifiedWinner by remember(currentOffering.id, bidderProfile?.id) { mutableStateOf(false) }
    LaunchedEffect(currentOffering.id, currentOffering.status, currentOffering.winnerNameStatus, bidderProfile?.id) {
        if (!canBeWinnerPromptUser) {
            isVerifiedWinner = false
            return@LaunchedEffect
        }
        if (currentOffering.status != "COMPLETED" || currentOffering.winnerNameStatus != "AWAITING_WINNER") {
            return@LaunchedEffect
        }
        repeat(60) { attempt ->
            val result = AppContainer.offeringRepository.isCurrentUserWinner(currentOffering.id)
            if (result.isSuccess) {
                if (result.getOrDefault(false)) {
                    isVerifiedWinner = true
                    return@LaunchedEffect
                }
            }
            if (attempt < 59) delay(2000)
        }
    }

    val deadlineEpochMs = remember(currentOffering.winnerNameDeadlineAt) {
        currentOffering.winnerNameDeadlineAt?.let {
            runCatching { Instant.parse(it).toEpochMilliseconds() }.getOrNull()
        }
    }

    var remainingSeconds by remember(currentOffering.id, deadlineEpochMs) { mutableStateOf<Long?>(null) }
    LaunchedEffect(currentOffering.id, deadlineEpochMs, serverTimeOffsetMillis) {
        if (deadlineEpochMs == null) {
            remainingSeconds = null
            return@LaunchedEffect
        }
        while (true) {
            val nowServer = Clock.System.now().toEpochMilliseconds() + serverTimeOffsetMillis
            val diff = (deadlineEpochMs - nowServer) / 1000L
            remainingSeconds = if (diff > 0) diff else 0L
            if (diff <= 0) break
            delay(1000)
        }
    }

    val isDeadlinePassed = remainingSeconds == 0L
    val waitingForCountdown = deadlineEpochMs != null && remainingSeconds == null
    val canStaffOverrideWinner = bidderProfile?.isStaff == true && currentOffering.status == "COMPLETED" && when (currentOffering.winnerNameStatus) {
        "AWAITING_ADMIN", "CONFIRMED" -> true
        "AWAITING_WINNER" -> deadlineEpochMs?.let {
            Clock.System.now().toEpochMilliseconds() + serverTimeOffsetMillis > it
        } == true
        else -> false
    }

    var winnerNameInput by remember(currentOffering.id, bidderProfile?.id) {
        mutableStateOf(bidderProfile?.fullName.orEmpty())
    }
    var isSubmittingWinnerName by remember(currentOffering.id) { mutableStateOf(false) }
    var awaitingWinnerConfirmation by remember(currentOffering.id) { mutableStateOf(false) }
    var winnerSubmitErrorMessage by remember(currentOffering.id) { mutableStateOf<String?>(null) }

    LaunchedEffect(awaitingWinnerConfirmation, currentOffering.winnerNameStatus) {
        if (!awaitingWinnerConfirmation) return@LaunchedEffect
        if (currentOffering.winnerNameStatus == "CONFIRMED") {
            awaitingWinnerConfirmation = false
            isSubmittingWinnerName = false
            return@LaunchedEffect
        }
        delay(3000)
        if (currentOffering.winnerNameStatus != "CONFIRMED") {
            awaitingWinnerConfirmation = false
            isSubmittingWinnerName = false
            winnerSubmitErrorMessage = Strings.winnerErrorGeneric(lang)
        }
    }

    val shouldShowWinnerPrompt = currentOffering.status == "COMPLETED" &&
        currentOffering.winnerNameStatus == "AWAITING_WINNER" &&
        canBeWinnerPromptUser &&
        isVerifiedWinner &&
        !waitingForCountdown &&
        !isDeadlinePassed

    val shouldShowWinnerAdminMessage = currentOffering.status == "COMPLETED" &&
        currentOffering.winnerNameStatus != "CONFIRMED" &&
        canBeWinnerPromptUser &&
        isVerifiedWinner &&
        (isDeadlinePassed || currentOffering.winnerNameStatus == "AWAITING_ADMIN")

    fun updateBidStatus(bid: OfferingBid, status: String) {
        coroutineScope.launch {
            isSavingAdminAction = true
            adminActionError = null
            AppContainer.offeringRepository.updateOfferingBidStatus(currentOffering.id, bid, status)
                .onFailure { adminActionError = it.message ?: "Could not update bid." }
            onRefreshBids()
            isSavingAdminAction = false
        }
    }

    fun deleteBid(bid: OfferingBid) {
        coroutineScope.launch {
            isSavingAdminAction = true
            adminActionError = null
            AppContainer.offeringRepository.deleteOfferingBid(currentOffering.id, bid.id)
                .onFailure { adminActionError = it.message ?: "Could not delete bid." }
            onRefreshBids()
            bidToDelete = null
            isSavingAdminAction = false
        }
    }

    fun addStaffBid() {
        val amount = adminBidAmount.toDoubleOrNull()
        if (adminBidderName.isBlank() || amount == null || amount <= 0.0) {
            adminActionError = if (lang == AppLanguage.GUJARATI) "નામ અને માન્ય રકમ દાખલ કરો." else "Enter a bidder name and valid amount."
            return
        }
        coroutineScope.launch {
            isSavingAdminAction = true
            adminActionError = null
            AppContainer.offeringRepository.addStaffOfferingBid(currentOffering.id, adminBidderName.trim(), amount)
                .onSuccess {
                    adminBidderName = ""
                    adminBidAmount = ""
                    onRefreshBids()
                }
                .onFailure { adminActionError = it.message ?: "Could not add bid." }
            isSavingAdminAction = false
        }
    }

    fun recordBidCall() {
        val eligibleBids = existingBids.filter { it.status != "REJECTED" }
        if (eligibleBids.isEmpty()) {
            adminActionError = if (lang == AppLanguage.GUJARATI) "વાર નોંધતા પહેલાં ઓછામાં ઓછી એક માન્ય બોલી જરૂરી છે." else "Add at least one eligible bid before recording a call."
            return
        }
        val winner = if (currentOffering.bidCallCount == 2) {
            eligibleBids.maxByOrNull { it.amount }?.bidderName
        } else null
        coroutineScope.launch {
            isSavingAdminAction = true
            adminActionError = null
            AppContainer.offeringRepository.callOfferingBid(currentOffering.id, winner)
                .onSuccess { onRefreshBids() }
                .onFailure { adminActionError = it.message ?: "Could not record bid call." }
            isSavingAdminAction = false
        }
    }

    fun setWinnerDisplayName() {
        val name = adminWinnerName.trim()
        if (name.length !in 2..60) {
            adminActionError = if (lang == AppLanguage.GUJARATI) "વિજેતાનું નામ ૨ થી ૬૦ અક્ષરનું હોવું જોઈએ." else "Winner name must be 2 to 60 characters."
            return
        }
        coroutineScope.launch {
            isSavingAdminAction = true
            adminActionError = null
            AppContainer.offeringRepository.adminSetWinnerDisplayName(currentOffering.id, name)
                .onSuccess {
                    adminWinnerName = ""
                    onRefreshBids()
                }
                .onFailure { adminActionError = it.message ?: "Could not update winner name." }
            isSavingAdminAction = false
        }
    }

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
                            onClick = {
                                bidSubmitErrorMessage = null
                                showBidDialog = true
                            },
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

                    // ── Winner Announcement & Display Name Flow ───────────────
                    if (currentOffering.status == "COMPLETED") {
                        Spacer(modifier = Modifier.height(12.dp))

                        if (shouldShowWinnerPrompt) {
                            Surface(
                                color = Color(0xFFF0FDF4),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Color(0xFF86EFAC)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = Strings.winnerPromptTitle(lang),
                                            style = BodlaSection,
                                            color = Color(0xFF15803D),
                                            fontWeight = FontWeight.Black
                                        )
                                        remainingSeconds?.takeIf { it > 0 }?.let { secondsLeft ->
                                            Text(
                                                text = Strings.winnerTimeRemaining(secondsLeft, lang),
                                                style = BodlaCaption,
                                                color = Color(0xFFB45309),
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    Text(
                                        text = Strings.winnerPromptSubtitle(lang),
                                        style = BodlaCaption,
                                        color = TextSecondary
                                    )

                                    OutlinedTextField(
                                        value = winnerNameInput,
                                        onValueChange = {
                                            winnerNameInput = it
                                            winnerSubmitErrorMessage = null
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        label = { Text(Strings.winnerNameInputLabel(lang)) },
                                        singleLine = true,
                                        enabled = !isSubmittingWinnerName,
                                        shape = RoundedCornerShape(12.dp)
                                    )

                                    if (!winnerSubmitErrorMessage.isNullOrBlank()) {
                                        Text(
                                            text = winnerSubmitErrorMessage.orEmpty(),
                                            style = BodlaCaption,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }

                                    Button(
                                        onClick = {
                                            val trimmed = winnerNameInput.trim()
                                            winnerSubmitErrorMessage = when {
                                                trimmed.length < 2 -> Strings.winnerErrorNameTooShort(lang)
                                                trimmed.length > 60 -> Strings.winnerErrorNameTooLong(lang)
                                                else -> null
                                            }
                                            if (winnerSubmitErrorMessage != null) return@Button
                                            coroutineScope.launch {
                                                isSubmittingWinnerName = true
                                                awaitingWinnerConfirmation = false
                                                winnerSubmitErrorMessage = null
                                                val result = AppContainer.offeringRepository.submitWinnerDisplayName(
                                                    offeringId = currentOffering.id,
                                                    name = trimmed
                                                )
                                                if (result.isFailure) {
                                                    isSubmittingWinnerName = false
                                                    val ex = result.exceptionOrNull()
                                                    winnerSubmitErrorMessage = when (ex) {
                                                        is WinnerFlowException.NotWinner -> Strings.winnerErrorNotWinner(lang)
                                                        is WinnerFlowException.DeadlineExpired -> Strings.winnerErrorDeadlinePassed(lang)
                                                        is WinnerFlowException.InvalidName -> Strings.winnerErrorInvalidLength(lang)
                                                        is WinnerFlowException.Network -> Strings.winnerErrorNetwork(lang)
                                                        is WinnerFlowException.Unknown -> Strings.winnerErrorGeneric(lang)
                                                        else -> Strings.winnerErrorGeneric(lang)
                                                    }
                                                } else {
                                                    awaitingWinnerConfirmation = true
                                                }
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        enabled = !isSubmittingWinnerName,
                                        colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary)
                                    ) {
                                        if (isSubmittingWinnerName) {
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(20.dp),
                                                    color = Color.White,
                                                    strokeWidth = 2.dp
                                                )
                                                Text(Strings.winnerSubmitting(lang))
                                            }
                                        } else {
                                            Text(Strings.winnerConfirmButton(lang))
                                        }
                                    }
                                }
                            }
                        } else if (currentOffering.winnerNameStatus == "CONFIRMED") {
                            Surface(
                                color = Color(0xFFDCFCE7),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = Strings.winnerCardTitle(lang),
                                        style = BodlaCaption,
                                        color = Color(0xFF15803D),
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = currentOffering.winnerDisplayName.orEmpty(),
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
                        } else if (shouldShowWinnerAdminMessage) {
                            Surface(
                                color = Color(0xFFFFF7ED),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Color(0xFFF59E0B)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = Strings.winnerDeadlineExpired(lang),
                                        style = BodlaCaption,
                                        color = Color(0xFF9A5B00),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        } else {
                            Surface(
                                color = Color(0xFFF3F4F6),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = Strings.winnerPendingConfirmation(lang),
                                    style = BodlaCaption,
                                    color = TextSecondary,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = BorderStroke)
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

            if (bidderProfile?.isStaff == true) {
                item {
                    BodlaCard {
                        Text(
                            text = if (lang == AppLanguage.GUJARATI) "વહીવટ નિયંત્રણો" else "Staff controls",
                            style = BodlaCardTitle,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        if (currentOffering.status == "ACTIVE" && currentOffering.bidCallCount < 3) {
                            Text(
                                text = if (lang == AppLanguage.GUJARATI) "સ્થળ પર મળેલી બોલી ઉમેરો" else "Add an in-person bid",
                                style = BodlaCaption.copy(fontWeight = FontWeight.Bold),
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = adminBidderName,
                                onValueChange = { adminBidderName = it; adminActionError = null },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text(if (lang == AppLanguage.GUJARATI) "બોલી આપનારનું નામ" else "Bidder name") },
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = adminBidAmount,
                                onValueChange = { adminBidAmount = it; adminActionError = null },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text(if (lang == AppLanguage.GUJARATI) "બોલી રકમ (₹)" else "Bid amount (₹)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { addStaffBid() },
                                enabled = !isSavingAdminAction,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = LeafGreenSecondary)
                            ) {
                                Text(if (isSavingAdminAction) "…" else if (lang == AppLanguage.GUJARATI) "બોલી ઉમેરો" else "Add approved bid")
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = LightIconBg,
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = if (lang == AppLanguage.GUJARATI)
                                            "બોલી વાર ${currentOffering.bidCallCount}/3 · ઉચ્ચ બોલી ${topBid?.let { "₹${it.toInt().toLocaleString()}" } ?: "—"}"
                                        else
                                            "Bid call ${currentOffering.bidCallCount}/3 · Highest ${topBid?.let { "₹${it.toInt().toLocaleString()}" } ?: "—"}",
                                        style = BodlaCaption.copy(fontWeight = FontWeight.Bold),
                                        color = TextPrimary
                                    )
                                    Button(
                                        onClick = { showCallConfirmation = true },
                                        enabled = !isSavingAdminAction && existingBids.any { it.status != "REJECTED" },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary)
                                    ) {
                                        Text(
                                            if (currentOffering.bidCallCount == 2) {
                                                if (lang == AppLanguage.GUJARATI) "ત્રીજી વાર · બોલી બંધ કરો" else "Third call · close bidding"
                                            } else {
                                                if (lang == AppLanguage.GUJARATI) "${currentOffering.bidCallCount + 1}મી વાર બોલો" else "Record call ${currentOffering.bidCallCount + 1} of 3"
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        if (currentOffering.status == "COMPLETED") {
                            Text(
                                text = if (lang == AppLanguage.GUJARATI) "વિજેતા" else "Winner",
                                style = BodlaCaption.copy(fontWeight = FontWeight.Bold),
                                color = TextSecondary
                            )
                            Text(
                                text = currentOffering.winnerDisplayName
                                    ?: currentOffering.winningBidderName
                                    ?: if (lang == AppLanguage.GUJARATI) "નામ ઉપલબ્ધ નથી" else "Name unavailable",
                                style = BodlaCardTitle,
                                color = TextPrimary
                            )
                            currentOffering.winningBidAmount?.let { amount ->
                                Text("₹${amount.toInt().toLocaleString()}", style = BodlaBody, color = SaffronPrimary)
                            }
                            if (canStaffOverrideWinner) {
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = adminWinnerName,
                                    onValueChange = { adminWinnerName = it; adminActionError = null },
                                    modifier = Modifier.fillMaxWidth(),
                                    label = { Text(if (lang == AppLanguage.GUJARATI) "પ્રદર્શિત વિજેતા નામ" else "Winner display name") },
                                    supportingText = { Text(if (lang == AppLanguage.GUJARATI) "૨ થી ૬૦ અક્ષર" else "2 to 60 characters") },
                                    singleLine = true
                                )
                                Button(
                                    onClick = { setWinnerDisplayName() },
                                    enabled = !isSavingAdminAction,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(if (isSavingAdminAction) "…" else if (lang == AppLanguage.GUJARATI) "વિજેતાનું નામ સાચવો" else "Save winner name")
                                }
                            }
                        }

                        adminActionError?.let { error ->
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(error, style = BodlaCaption, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            // ── Bid History Section ─────────────────────────────────────
            val visibleBids = when {
                bidderProfile?.isStaff == true -> existingBids
                bidderProfile == null -> emptyList()
                else -> filterVisibleBidsForUser(existingBids, bidderProfile)
            }

            if (visibleBids.isNotEmpty()) {
                item {
                    Text(
                        text = if (bidderProfile?.isStaff == true) {
                            if (lang == AppLanguage.GUJARATI) "બોલી ઇતિહાસ (${visibleBids.size})" else "Bid history (${visibleBids.size})"
                        } else {
                            if (lang == AppLanguage.GUJARATI) "📋 તમારી બોલીઓ (${visibleBids.size})" else "📋 Your bids (${visibleBids.size})"
                        },
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
                        showPhone = bidderProfile?.isStaff == true,
                        onApprove = if (bidderProfile?.isStaff == true) ({ updateBidStatus(bid, "APPROVED") }) else null,
                        onReject = if (bidderProfile?.isStaff == true) ({ updateBidStatus(bid, "REJECTED") }) else null,
                        onDelete = if (bidderProfile?.isStaff == true) ({ bidToDelete = bid }) else null
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
            submitError = bidSubmitErrorMessage,
            onClearSubmitError = { bidSubmitErrorMessage = null },
            onDismiss = {
                if (!isSubmittingBid) {
                    showBidDialog = false
                    bidSubmitErrorMessage = null
                }
            },
            onSubmit = { amount ->
                if (!isSubmittingBid) {
                    coroutineScope.launch {
                        isSubmittingBid = true
                        bidSubmitErrorMessage = null
                        bidSuccessMessage = null
                        val result = onPlaceBid(amount)
                        isSubmittingBid = false
                        result.onSuccess {
                            showBidDialog = false
                            bidSubmitErrorMessage = null
                            bidSuccessMessage = if (lang == AppLanguage.GUJARATI)
                                "🎉 તમારી ₹${amount.toInt().toLocaleString()} ની બોલી સફળતાપૂર્વક નોંધાઈ! (Pending Approval)"
                            else
                                "🎉 Your bid of ₹${amount.toInt().toLocaleString()} was placed! (Pending Approval)"
                        }.onFailure { error ->
                            val rawMessage = error.message.orEmpty()
                            val requiredMinimum = Regex("₹\\s*([0-9,]+)").find(rawMessage)
                                ?.groupValues?.getOrNull(1)?.replace(",", "")
                            val currentAmount = topBid ?: currentOffering.amount
                            bidSubmitErrorMessage = when {
                                rawMessage.contains("at least", ignoreCase = true) -> if (lang == AppLanguage.GUJARATI) {
                                    val minimum = requiredMinimum ?: calculateMinimumBidAmount(currentOffering.amount, topBid).toInt().toString()
                                    "બોલી ઓછામાં ઓછી ₹$minimum હોવી જોઈએ. હાલમાં સૌથી વધુ બોલી ₹${currentAmount?.toInt() ?: 0} છે."
                                } else {
                                    val minimum = requiredMinimum ?: calculateMinimumBidAmount(currentOffering.amount, topBid).toInt().toString()
                                    "Bid must be at least ₹$minimum. The current highest bid is ₹${currentAmount?.toInt() ?: 0}."
                                }
                                lang == AppLanguage.GUJARATI -> "બોલી નોંધાઈ શકી નથી. કૃપા કરીને ફરી પ્રયાસ કરો."
                                else -> rawMessage.ifBlank { "Bid could not be submitted. Please try again." }
                            }
                        }
                    }
                }
            }
        )
    }

    bidToDelete?.let { bid ->
        AlertDialog(
            onDismissRequest = { if (!isSavingAdminAction) bidToDelete = null },
            title = { Text(if (lang == AppLanguage.GUJARATI) "બોલી કાઢી નાખવી?" else "Delete this bid?") },
            text = {
                Text(
                    if (lang == AppLanguage.GUJARATI) "${bid.bidderName} ની ₹${bid.amount.toInt().toLocaleString()} ની બોલી કાયમ માટે કાઢી નાખવામાં આવશે."
                    else "Permanently delete ${bid.bidderName}'s ₹${bid.amount.toInt().toLocaleString()} bid?"
                )
            },
            confirmButton = {
                TextButton(enabled = !isSavingAdminAction, onClick = { deleteBid(bid) }) {
                    Text(if (lang == AppLanguage.GUJARATI) "કાઢી નાખો" else "Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { bidToDelete = null }) { Text(if (lang == AppLanguage.GUJARATI) "રદ કરો" else "Cancel") }
            }
        )
    }

    if (showCallConfirmation) {
        val closesOffering = currentOffering.bidCallCount == 2
        AlertDialog(
            onDismissRequest = { if (!isSavingAdminAction) showCallConfirmation = false },
            title = {
                Text(
                    if (closesOffering) {
                        if (lang == AppLanguage.GUJARATI) "અંતિમ વાર નોંધવી?" else "Record the final call?"
                    } else {
                        if (lang == AppLanguage.GUJARATI) "બોલી વાર નોંધવી?" else "Record bid call ${currentOffering.bidCallCount + 1}/3?"
                    }
                )
            },
            text = {
                Text(
                    if (closesOffering) {
                        if (lang == AppLanguage.GUJARATI) "આ પગલાથી બોલી બંધ થશે અને વિજેતા નામની પ્રક્રિયા શરૂ થશે." else "This closes bidding and starts winner-name confirmation."
                    } else {
                        if (lang == AppLanguage.GUJARATI) "આ બોલી માટે ${currentOffering.bidCallCount + 1}મી વાર જાહેર થશે." else "This records the next public bid call for this offering."
                    }
                )
            },
            confirmButton = {
                TextButton(
                    enabled = !isSavingAdminAction,
                    onClick = {
                        showCallConfirmation = false
                        recordBidCall()
                    }
                ) { Text(if (lang == AppLanguage.GUJARATI) "નોંધો" else "Confirm") }
            },
            dismissButton = {
                TextButton(onClick = { showCallConfirmation = false }) { Text(if (lang == AppLanguage.GUJARATI) "રદ કરો" else "Cancel") }
            }
        )
    }
}

// ------------------------------------------------------------------------------
// Bid History Item Card
// ------------------------------------------------------------------------------
@Composable
private fun BidHistoryItem(
    bid: OfferingBid,
    lang: AppLanguage,
    showPhone: Boolean,
    onApprove: (() -> Unit)? = null,
    onReject: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null
) {
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

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "₹${bid.amount.toInt().toLocaleString()}",
                    style = BodlaSection,
                    color = when {
                        isApproved -> Color(0xFF16A34A)
                        isRejected -> TextSecondary
                        else -> SaffronPrimary
                    },
                    fontWeight = FontWeight.Black
                )
                if (onApprove != null || onReject != null || onDelete != null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        if (isPending && onApprove != null) {
                            TextButton(onClick = onApprove, contentPadding = PaddingValues(horizontal = 6.dp)) {
                                Text(if (lang == AppLanguage.GUJARATI) "મંજૂર" else "Approve", color = Color(0xFF15803D), style = BodlaCaption)
                            }
                        }
                        if (isPending && onReject != null) {
                            TextButton(onClick = onReject, contentPadding = PaddingValues(horizontal = 6.dp)) {
                                Text(if (lang == AppLanguage.GUJARATI) "નામંજૂર" else "Reject", color = Color(0xFFBE123C), style = BodlaCaption)
                            }
                        }
                        if (onDelete != null) {
                            TextButton(onClick = onDelete, contentPadding = PaddingValues(horizontal = 6.dp)) {
                                Text(if (lang == AppLanguage.GUJARATI) "કાઢો" else "Delete", color = Color(0xFFBE123C), style = BodlaCaption)
                            }
                        }
                    }
                }
            }
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
    submitError: String? = null,
    onClearSubmitError: () -> Unit = {},
    onDismiss: () -> Unit,
    onSubmit: (amount: Double) -> Unit
) {
    var bidAmount by remember { mutableStateOf("") }
    var amountError by remember { mutableStateOf<String?>(null) }
    val minimumAllowedBid = calculateMinimumBidAmount(offering.amount, currentHighestBid)

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
                val currentPrice = currentHighestBid ?: offering.amount
                if (currentPrice != null) {
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
                                text = if (lang == AppLanguage.GUJARATI) "ન્યૂનતમ/ઉચ્ચ રકમ:" else "Min/Current Amt:",
                                style = BodlaCaption,
                                color = TextSecondary
                            )
                            Text(
                                text = "₹${minimumAllowedBid.toInt().toLocaleString()} / ₹${currentPrice.toInt().toLocaleString()}",
                                style = BodlaBody,
                                color = SaffronPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = BorderStroke)
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

                val displayedError = amountError ?: submitError
                if (!displayedError.isNullOrBlank()) {
                    Surface(
                        color = Color(0xFFFFF1F2),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFFFECDD3))
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = displayedError,
                                style = BodlaCaption,
                                color = Color(0xFFBE123C),
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = { amountError = null; onClearSubmitError() }) {
                                Text(if (lang == AppLanguage.GUJARATI) "બંધ કરો" else "Close")
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // ── Bid Amount ───────────────────────────────────────────
                OutlinedTextField(
                    value = bidAmount,
                    onValueChange = {
                        bidAmount = it
                        amountError = null
                        onClearSubmitError()
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
                    supportingText = {
                        if (amountError == null) {
                            Text(
                                if (lang == AppLanguage.GUJARATI)
                                    "ન્યૂનતમ રકમ: ₹${minimumAllowedBid.toInt().toLocaleString()}"
                                else
                                    "Minimum allowed: ₹${minimumAllowedBid.toInt().toLocaleString()}"
                            )
                        } else {
                            Text("")
                        }
                    },
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
                            val parsedAmount = bidAmount.toDoubleOrNull()
                            val validation = when {
                                bidAmount.isBlank() -> Pair(false, if (lang == AppLanguage.GUJARATI) "રકમ દાખલ કરો" else "Enter amount")
                                parsedAmount == null -> Pair(false, if (lang == AppLanguage.GUJARATI) "માન્ય રકમ દાખલ કરો" else "Enter valid amount")
                                else -> validateBidAmount(parsedAmount, offering.amount, currentHighestBid, lang)
                            }
                            amountError = validation.second
                            if (validation.first && parsedAmount != null) {
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
    onSubmit: (Offering) -> Unit,
    modifier: Modifier = Modifier
) {
    val lang = LocalAppLanguage.current
    var titleGu by remember { mutableStateOf("") }
    var titleEn by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var locationGu by remember { mutableStateOf("") }
    var locationEn by remember { mutableStateOf("") }
    var descriptionGu by remember { mutableStateOf("") }
    var descriptionEn by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("ACTIVE") }
    var showStatusMenu by remember { mutableStateOf(false) }

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
                value = titleGu,
                onValueChange = { titleGu = it },
                label = { Text(if (lang == AppLanguage.GUJARATI) "ચઢાવાનું નામ (ગુજરાતી) *" else "Gujarati title *") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = titleEn,
                onValueChange = { titleEn = it },
                label = { Text(if (lang == AppLanguage.GUJARATI) "શીર્ષક (અંગ્રેજી)" else "Title (English)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                label = { Text(Strings.offeringAmount(lang)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                prefix = { Text("₹") }
            )

            OutlinedTextField(
                value = locationGu,
                onValueChange = { locationGu = it },
                label = { Text(if (lang == AppLanguage.GUJARATI) "સ્થળ (ગુજરાતી)" else "Location (Gujarati)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = locationEn,
                onValueChange = { locationEn = it },
                label = { Text(if (lang == AppLanguage.GUJARATI) "સ્થળ (અંગ્રેજી)" else "Location (English)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = descriptionGu,
                onValueChange = { descriptionGu = it },
                label = { Text(if (lang == AppLanguage.GUJARATI) "વર્ણન (ગુજરાતી)" else "Description (Gujarati)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = descriptionEn,
                onValueChange = { descriptionEn = it },
                label = { Text(if (lang == AppLanguage.GUJARATI) "વર્ણન (અંગ્રેજી)" else "Description (English)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                shape = RoundedCornerShape(12.dp)
            )

            Box {
                OutlinedButton(onClick = { showStatusMenu = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("${if (lang == AppLanguage.GUJARATI) "પ્રારંભિક સ્થિતિ" else "Initial status"}: $status")
                }
                DropdownMenu(expanded = showStatusMenu, onDismissRequest = { showStatusMenu = false }) {
                    listOf("ACTIVE", "APPROVED", "PENDING_APPROVAL").forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option.replace('_', ' ')) },
                            onClick = { status = option; showStatusMenu = false }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            BodlaButton(
                text = Strings.submitOffering(lang),
                onClick = {
                    val cleanedTitleGu = titleGu.trim()
                    if (cleanedTitleGu.isNotEmpty()) {
                        onSubmit(
                            Offering(
                                id = "off_${Clock.System.now().toEpochMilliseconds()}",
                                titleGu = cleanedTitleGu,
                                titleEn = titleEn.trim().ifBlank { cleanedTitleGu },
                                descriptionGu = descriptionGu.trim().ifBlank { null },
                                descriptionEn = descriptionEn.trim().ifBlank { null },
                                amount = amount.toDoubleOrNull(),
                                locationGu = locationGu.trim().ifBlank { null },
                                locationEn = locationEn.trim().ifBlank { null },
                                status = status
                            )
                        )
                    }
                },
                enabled = titleGu.isNotBlank() && (amount.isBlank() || amount.toDoubleOrNull() != null)
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
