package com.bodla.parivar.features.main

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.bodla.parivar.core.localization.AppLanguage
import com.bodla.parivar.core.localization.LocalAppLanguage
import com.bodla.parivar.core.navigation.Screen
import com.bodla.parivar.core.theme.BodlaBody
import com.bodla.parivar.core.theme.BodlaCaption
import com.bodla.parivar.core.theme.BodlaCardTitle
import com.bodla.parivar.core.theme.BodlaPageTitle
import com.bodla.parivar.core.theme.CreamBackground
import com.bodla.parivar.core.theme.TextPrimary
import com.bodla.parivar.core.theme.TextSecondary
import com.bodla.parivar.core.theme.WarmWhiteSurface
import com.bodla.parivar.core.ui.BodlaButton
import com.bodla.parivar.core.ui.BodlaCard
import com.bodla.parivar.composeapp.generated.resources.Res
import com.bodla.parivar.composeapp.generated.resources.bodla_parivar_mark
import com.bodla.parivar.domain.model.Profile
import org.jetbrains.compose.resources.painterResource

private data class AdminDestination(
    val titleEn: String,
    val titleGu: String,
    val detailEn: String,
    val detailGu: String,
    val screen: Screen
)

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun AdminHomeScreen(
    profile: Profile?,
    onNavigate: (Screen) -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lang = LocalAppLanguage.current
    val destinations = buildList {
        if (profile?.isAdmin == true) {
            add(AdminDestination("Registered users", "નોંધાયેલા સભ્યો", "Search the member registry", "સભ્યોની નોંધણી જુઓ", Screen.AdminUsers))
        }
        add(AdminDestination("Notices", "સૂચનાઓ", "Community announcements", "સમુદાયની સૂચનાઓ", Screen.Notices))
        add(AdminDestination("Events", "કાર્યક્રમો", "Village events and updates", "ગામના કાર્યક્રમો અને અપડેટ્સ", Screen.Events))
        add(AdminDestination("Live bidding", "લાઇવ બોલી", "Manage admin-created offerings and contributions", "વહીવટકર્તા દ્વારા બનાવેલા ચઢાવો અને બોલીઓ જુઓ", Screen.Offerings))
        add(AdminDestination("Community directory", "સમુદાય ડિરેક્ટરી", "Residents and businesses", "રહેવાસીઓ અને વ્યવસાયો", Screen.Directory))
        add(AdminDestination("Complaints", "ફરિયાદો", "Complaint records", "ફરિયાદોની નોંધ", Screen.Complaints))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (lang == AppLanguage.GUJARATI) "એડમિન વર્કસ્પેસ" else "Admin workspace", style = BodlaPageTitle) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = WarmWhiteSurface)
            )
        },
        containerColor = CreamBackground,
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                BodlaCard {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Image(
                            painter = painterResource(Res.drawable.bodla_parivar_mark),
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = if (lang == AppLanguage.GUJARATI) "વહીવટકર્તા પ્રવેશ સક્રિય" else "Administrator access active",
                            style = BodlaCardTitle,
                            color = TextPrimary
                        )
                        Text(profile?.fullName.orEmpty(), style = BodlaBody, color = TextPrimary)
                        Text(profile?.email.orEmpty(), style = BodlaCaption, color = TextSecondary)
                    }
                }
            }
            items(destinations) { destination ->
                BodlaCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = if (lang == AppLanguage.GUJARATI) destination.titleGu else destination.titleEn,
                            style = BodlaCardTitle,
                            color = TextPrimary
                        )
                        Text(
                            text = if (lang == AppLanguage.GUJARATI) destination.detailGu else destination.detailEn,
                            style = BodlaCaption,
                            color = TextSecondary
                        )
                        BodlaButton(
                            text = if (lang == AppLanguage.GUJARATI) "ખોલો" else "Open",
                            onClick = { onNavigate(destination.screen) }
                        )
                    }
                }
            }
            item {
                BodlaButton(
                    text = if (lang == AppLanguage.GUJARATI) "સાઇન આઉટ" else "Sign out",
                    onClick = onLogout,
                    containerColor = WarmWhiteSurface,
                    contentColor = TextPrimary
                )
            }
        }
    }
}