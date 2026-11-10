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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.bodla.parivar.features.notices.ZoomableImage
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bodla.parivar.core.localization.AppLanguage
import com.bodla.parivar.core.localization.LocalAppLanguage
import com.bodla.parivar.core.navigation.Screen
import com.bodla.parivar.core.theme.*
import coil3.compose.AsyncImage
import com.bodla.parivar.features.notices.NoticeAttachmentPreview
import com.bodla.parivar.features.notices.formatNoticeText
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import androidx.compose.material3.Button
import androidx.compose.material3.TextButton

@Serializable
data class AppNotification(
    val id: String,
    @SerialName("title_gu") val titleGu: String? = null,
    @SerialName("title_en") val titleEn: String? = null,
    @SerialName("body_gu") val bodyGu: String? = null,
    @SerialName("body_en") val bodyEn: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("reference_id") val referenceId: String? = null,
    val type: String? = null,
    val payload: JsonObject? = null,
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("attachment_url") val attachmentUrl: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationCenterScreen(
    notifications: List<AppNotification>,
    onBack: () -> Unit,
    onNotificationClick: (AppNotification) -> Unit = {},
    onDelete: (AppNotification) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val lang = LocalAppLanguage.current
    val isGujarati = lang == AppLanguage.GUJARATI

    var previewImageUrl by remember { mutableStateOf<String?>(null) }
    var previewImageTitle by remember { mutableStateOf("") }
    var previewAttachmentUrl by remember { mutableStateOf<String?>(null) }
    var previewAttachmentTitle by remember { mutableStateOf("") }

    var deleteConfirmId by remember { mutableStateOf<String?>(null) }
    var deleteConfirmTitle by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (isGujarati) "સૂચનાઓ" else "Notices",
                            style = BodlaPageTitle,
                            color = TextPrimary
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
                colors = bodlaTopAppBarColors()
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
                                    Text(text = formatNoticeText(body), style = BodlaBody, color = TextSecondary)
                                }
                                val imageUrl = notification.imageUrl ?: notification.payload?.get("image_url")?.jsonPrimitive?.contentOrNull
                                val attachmentUrl = notification.attachmentUrl ?: notification.payload?.get("attachment_url")?.jsonPrimitive?.contentOrNull
                                if (!imageUrl.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    AsyncImage(
                                        model = imageUrl,
                                        contentDescription = title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(160.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable {
                                                previewImageTitle = title
                                                previewImageUrl = imageUrl
                                            }
                                    )
                                }
                                if (!imageUrl.isNullOrBlank() || !attachmentUrl.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        if (!imageUrl.isNullOrBlank()) {
                                            OutlinedButton(
                                                onClick = {
                                                    previewImageTitle = title
                                                    previewImageUrl = imageUrl
                                                },
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                                modifier = Modifier.height(34.dp)
                                            ) {
                                                Text(
                                                    text = if (isGujarati) "🖼️ ફોટો જુઓ" else "🖼️ View Image",
                                                    style = BodlaCaption.copy(fontWeight = FontWeight.SemiBold),
                                                    color = SaffronPrimary
                                                )
                                            }
                                        }
                                        if (!attachmentUrl.isNullOrBlank()) {
                                            OutlinedButton(
                                                onClick = {
                                                    previewAttachmentTitle = title
                                                    previewAttachmentUrl = attachmentUrl
                                                },
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                                modifier = Modifier.height(34.dp)
                                            ) {
                                                Text(
                                                    text = if (isGujarati) "📄 દસ્તાવેજ જુઓ" else "📄 View Document",
                                                    style = BodlaCaption.copy(fontWeight = FontWeight.SemiBold),
                                                    color = SaffronPrimary
                                                )
                                            }
                                        }
                                    }
                                }
                                notification.createdAt?.let { createdAt ->
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(text = createdAt, style = BodlaCaption, color = TextTertiary)
                                }
                            }
                            IconButton(
                                onClick = {
                                    deleteConfirmId = notification.id
                                    deleteConfirmTitle = title
                                }
                            ) {
                                Text(text = "🗑️", fontSize = 18.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    previewImageUrl?.let { url ->
        AlertDialog(
            onDismissRequest = { previewImageUrl = null },
            confirmButton = {
                TextButton(onClick = { previewImageUrl = null }) {
                    Text(if (isGujarati) "બંધ કરો" else "Close")
                }
            },
            title = {
                if (previewImageTitle.isNotBlank()) {
                    Text(text = previewImageTitle, style = BodlaCardTitle)
                }
            },
            text = {
                ZoomableImage(
                    model = url,
                    contentDescription = previewImageTitle.ifBlank { "Notice image" },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 280.dp, max = 520.dp),
                    zoomInLabel = if (isGujarati) "મોટું કરો" else "Zoom in",
                    zoomOutLabel = if (isGujarati) "નાનું કરો" else "Zoom out",
                    resetLabel = if (isGujarati) "રીસેટ" else "Reset"
                )
            }
        )
    }

    previewAttachmentUrl?.let { url ->
        val uriHandler = LocalUriHandler.current
        AlertDialog(
            onDismissRequest = { previewAttachmentUrl = null },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = {
                        uriHandler.openUri(url + if (url.contains('?')) "&download" else "?download")
                    }) {
                        Text(if (isGujarati) "ડાઉનલોડ કરો" else "Download")
                    }
                    TextButton(onClick = { previewAttachmentUrl = null }) {
                        Text(if (isGujarati) "બંધ કરો" else "Close")
                    }
                }
            },
            title = {
                if (previewAttachmentTitle.isNotBlank()) {
                    Text(text = previewAttachmentTitle, style = BodlaCardTitle)
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth().heightIn(min = 300.dp, max = 520.dp)) {
                    NoticeAttachmentPreview(
                        url = url,
                        modifier = Modifier.fillMaxWidth().weight(1f)
                    )
                }
            }
        )
    }

    deleteConfirmId?.let { id ->
        AlertDialog(
            onDismissRequest = { deleteConfirmId = null },
            confirmButton = {
                TextButton(onClick = {
                    deleteConfirmId = null
                    val notificationToDelete = notifications.find { it.id == id }
                    if (notificationToDelete != null) {
                        onDelete(notificationToDelete)
                    }
                }) {
                    Text(if (isGujarati) "હા, ડિલીટ કરો" else "Yes, Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteConfirmId = null }) {
                    Text(if (isGujarati) "નહીં" else "Cancel")
                }
            },
            title = {
                Text(text = if (isGujarati) "આ સૂચના ડિલીટ કરશો?" else "Delete this notice?")
            },
            text = {
                Text(text = deleteConfirmTitle)
            }
        )
    }
}
