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
        val displayEvents = if (events.isNotEmpty()) events else listOf(
            Event(
                id = "demo_event_1",
                titleGu = "[DEMO DATA] વાર્ષિક સાંસ્કૃતિક ઉત્સવ",
                titleEn = "[DEMO DATA] Annual Cultural Celebration",
                descriptionGu = "આ ડેમો કાર્યક્રમ છે. ગામના તમામ પરિવારો માટે સાંસ્કૃતિક મહોત્સવનું આયોજન.",
                descriptionEn = "This is demo data. Annual cultural celebration for all Bodla Parivar families.",
                startAt = "2026-10-05 18:00",
                locationGu = "બોદલા પ્રાથમિક શાળા મેદાન",
                locationEn = "Bodla Primary School Ground"
            ),
            Event(
                id = "demo_event_2",
                titleGu = "[DEMO DATA] વિનામૂલ્યે આરોગ્ય તપાસ કેમ્પ",
                titleEn = "[DEMO DATA] Free Medical Health Camp",
                descriptionGu = "આ ડેમો કાર્યક્રમ છે. તમામ ગ્રામજનો માટે નિઃશુલ્ક બ્લડ પ્રેશર અને ડાયાબિટીસ તપાસ.",
                descriptionEn = "This is demo data. Free health checkup camp for blood pressure and diabetes.",
                startAt = "2026-10-12 09:00",
                locationGu = "પ્રાથમિક આરોગ્ય કેન્દ્ર",
                locationEn = "Primary Health Center"
            )
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(displayEvents) { event ->
                BodlaCard(
                    onClick = { onNavigate(Screen.EventDetail(event.id)) }
                ) {
                    DemoDataBadge()
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
    eventId: String,
    onBack: () -> Unit,
    event: Event? = null,
    modifier: Modifier = Modifier
) {
    val lang = LocalAppLanguage.current
    val currentEvent = event ?: Event(
        id = eventId,
        titleGu = "[DEMO DATA] વાર્ષિક સાંસ્કૃતિક ઉત્સવ",
        titleEn = "[DEMO DATA] Annual Cultural Celebration",
        descriptionGu = "આ ડેમો કાર્યક્રમ છે. બોદલા પરિવારના સર્વે ભાઈઓ અને બહેનોને જણાવવાનું કે આગામી તારીખે પ્રાથમિક શાળાના મેદાન ખાતે ભવ્ય સાંસ્કૃતિક કાર્યક્રમ અને સમૂહ ભોજનનું આયોજન કરવામાં આવ્યું છે.",
        descriptionEn = "This is demo data. Notice to all Bodla Parivar members regarding the annual cultural evening and community dinner at the primary school ground.",
        startAt = "2026-10-05 18:00",
        locationGu = "બોદલા પ્રાથમિક શાળા મેદાન",
        locationEn = "Bodla Primary School Ground"
    )

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
                DemoDataBadge()
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
                Divider(color = BorderStroke)
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
