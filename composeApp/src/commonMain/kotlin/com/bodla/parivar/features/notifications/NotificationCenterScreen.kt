package com.bodla.parivar.features.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bodla.parivar.core.localization.AppLanguage
import com.bodla.parivar.core.localization.LocalAppLanguage
import com.bodla.parivar.core.navigation.Screen
import com.bodla.parivar.core.theme.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AppNotification(
    val id: String,
    @SerialName("title_gu") val titleGu: String? = null,
    @SerialName("title_en") val titleEn: String? = null,
    @SerialName("body_gu") val bodyGu: String? = null,
    @SerialName("body_en") val bodyEn: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("reference_id") val referenceId: String? = null,
    val type: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationCenterScreen(
    notifications: List<AppNotification>,
    onBack: () -> Unit,
    onNotificationClick: (AppNotification) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val lang = LocalAppLanguage.current
    val isGujarati = lang == AppLanguage.GUJARATI

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (isGujarati) "સૂચનાઓ" else "Notifications",
                            style = BodlaPageTitle
                        )
                        Text(
                            text = if (isGujarati) "બોદલા પરિવારના નવા અપડેટ્સ" else "Updates from Bodla Parivar",
                            style = BodlaCaption,
                            color = TextSecondary
                        )
                    }
                },
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
        if (notifications.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(28.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(LightIconBg, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "🔔", fontSize = 28.sp)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (isGujarati) "હાલમાં કોઈ નવી સૂચના નથી" else "You're all caught up",
                        style = BodlaCardTitle,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (isGujarati) "નવી સૂચનાઓ અહીં દેખાશે." else "New community updates will appear here.",
                        style = BodlaBody,
                        color = TextSecondary
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Text(
                        text = if (isGujarati) "તાજેતરના અપડેટ્સ" else "RECENT UPDATES",
                        style = BodlaCaption.copy(fontWeight = FontWeight.Bold),
                        color = SaffronPrimary,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }
                items(notifications, key = { it.id }) { notification ->
                    val title = if (isGujarati) notification.titleGu ?: notification.titleEn.orEmpty()
                    else notification.titleEn ?: notification.titleGu.orEmpty()
                    val body = if (isGujarati) notification.bodyGu ?: notification.bodyEn.orEmpty()
                    else notification.bodyEn ?: notification.bodyGu.orEmpty()

                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { onNotificationClick(notification) },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = WarmWhiteSurface),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = androidx.compose.ui.graphics.SolidColor(BorderStroke)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(LightIconBg, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = "🔔", fontSize = 18.sp)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = title, style = BodlaCardTitle, color = TextPrimary)
                                if (body.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(text = body, style = BodlaBody, color = TextSecondary)
                                }
                                notification.createdAt?.let { createdAt ->
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(text = createdAt, style = BodlaCaption, color = TextTertiary)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
