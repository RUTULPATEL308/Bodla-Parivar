package com.bodla.parivar.features.events

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bodla.parivar.core.localization.AppLanguage
import com.bodla.parivar.core.localization.LocalAppLanguage
import com.bodla.parivar.core.localization.Strings
import com.bodla.parivar.core.navigation.Screen
import com.bodla.parivar.core.theme.*
import com.bodla.parivar.core.ui.*
import com.bodla.parivar.domain.model.Event

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventListScreen(
    onNavigate: (Screen) -> Unit,
    events: List<Event> = emptyList(),
    modifier: Modifier = Modifier
) {
    val lang = LocalAppLanguage.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = Strings.navEvents(lang), style = BodlaPageTitle) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = WarmWhiteSurface)
            )
        },
        containerColor = CreamBackground,
        modifier = modifier
    ) { innerPadding ->
        val displayEvents = events

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (displayEvents.isEmpty()) {
                item { EmptyStateView(if (lang == AppLanguage.GUJARATI) "હાલમાં કોઈ કાર્યક્રમ નથી." else "There are no upcoming events.") }
            }
            items(displayEvents) { event ->
                BodlaCard(
                    onClick = { onNavigate(Screen.EventDetail(event.id)) }
                ) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (lang == AppLanguage.GUJARATI) event.titleGu else event.titleEn,
                        style = BodlaCardTitle,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "🕒 " + event.startAt,
                        style = BodlaCaption,
                        color = SaffronPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "📍 " + (if (lang == AppLanguage.GUJARATI) event.locationGu.orEmpty() else event.locationEn.orEmpty()),
                        style = BodlaCaption,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDetailScreen(
    onBack: () -> Unit,
    event: Event? = null,
    modifier: Modifier = Modifier
) {
    val lang = LocalAppLanguage.current
    if (event == null) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(text = Strings.navEvents(lang), style = BodlaPageTitle) },
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
        ) { padding ->
            EmptyStateView(
                message = if (lang == AppLanguage.GUJARATI) "આ કાર્યક્રમ હવે ઉપલબ્ધ નથી." else "This event is no longer available.",
                modifier = Modifier.fillMaxSize().padding(padding)
            )
        }
        return
    }
    val currentEvent = event

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = Strings.navEvents(lang), style = BodlaPageTitle) },
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
                .padding(16.dp)
        ) {
            BodlaCard {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = if (lang == AppLanguage.GUJARATI) currentEvent.titleGu else currentEvent.titleEn,
                    style = BodlaPageTitle,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "🕒 ", fontSize = 16.sp)
                    Text(
                        text = currentEvent.startAt,
                        style = BodlaBody,
                        color = SaffronPrimary
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "📍 ", fontSize = 16.sp)
                    Text(
                        text = (if (lang == AppLanguage.GUJARATI) currentEvent.locationGu else currentEvent.locationEn).orEmpty(),
                        style = BodlaBody,
                        color = TextSecondary
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = BorderStroke)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = (if (lang == AppLanguage.GUJARATI) currentEvent.descriptionGu else currentEvent.descriptionEn).orEmpty(),
                    style = BodlaBody,
                    color = TextPrimary,
                    lineHeight = 26.sp
                )
            }
        }
    }
}
