package com.bodla.parivar.features.notices

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import com.bodla.parivar.core.localization.AppLanguage
import com.bodla.parivar.core.localization.LocalAppLanguage
import com.bodla.parivar.core.theme.*
import com.bodla.parivar.domain.model.Notice

@Composable
fun ApprovedNoticePopup(
    notice: Notice,
    onOpen: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lang = LocalAppLanguage.current
    val isGujarati = lang == AppLanguage.GUJARATI

    val title = if (isGujarati) {
        notice.titleGu.ifBlank { notice.titleEn }
    } else {
        notice.titleEn.ifBlank { notice.titleGu }
    }

    val description = if (isGujarati) {
        notice.descriptionGu?.ifBlank { notice.descriptionEn } ?: notice.descriptionEn.orEmpty()
    } else {
        notice.descriptionEn?.ifBlank { notice.descriptionGu } ?: notice.descriptionGu.orEmpty()
    }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Card(
            modifier = modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = WarmWhiteSurface),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header badge
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = LightIconBg,
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Text(
                        text = if (isGujarati) "📢 નવી ગ્રામ સૂચના" else "📢 New Community Notice",
                        style = BodlaCaption,
                        color = SaffronPrimary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }

                // Title
                Text(
                    text = title,
                    style = BodlaCardTitle,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    fontSize = 18.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Optional Image
                if (!notice.imageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = notice.imageUrl,
                        contentDescription = "Notice image",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .padding(bottom = 12.dp)
                    )
                }

                // Description (Scrollable if lengthy)
                if (description.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 160.dp)
                            .verticalScroll(rememberScrollState())
                            .padding(bottom = 16.dp)
                    ) {
                        Text(
                            text = description,
                            style = BodlaBody,
                            color = TextSecondary,
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        )
                    }
                }

                // Action buttons: Close & Open
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onClose,
                        modifier = Modifier.weight(1f).height(46.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = TextSecondary
                        )
                    ) {
                        Text(
                            text = if (isGujarati) "બંધ કરો (Close)" else "Close",
                            style = BodlaButton,
                            color = TextSecondary
                        )
                    }

                    Button(
                        onClick = onOpen,
                        modifier = Modifier.weight(1f).height(46.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SaffronPrimary,
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            text = if (isGujarati) "જુઓ (Open)" else "Open",
                            style = BodlaButton,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
