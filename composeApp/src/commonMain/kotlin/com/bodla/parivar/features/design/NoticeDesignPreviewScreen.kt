package com.bodla.parivar.features.design

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.roundToIntRect
import androidx.compose.ui.unit.sp
import com.bodla.parivar.core.localization.AppLanguage
import com.bodla.parivar.core.localization.LocalAppLanguage
import com.bodla.parivar.core.theme.AnekGujaratiFont
import com.bodla.parivar.core.ui.PlatformCapturePoster
import com.bodla.parivar.composeapp.generated.resources.Res
import com.bodla.parivar.composeapp.generated.resources.bodla_parivar_logo
import com.bodla.parivar.composeapp.generated.resources.poster_bg_golden_mandala
import org.jetbrains.compose.resources.painterResource

// ── Professional Designer Palette (Warm Parchment & Royal Maroon) ──
private val RoyalMaroon = Color(0xFF4A1220)
private val DeepWine    = Color(0xFF6E2332)
private val AntiqueGold = Color(0xFF9C7A38)
private val LightGold   = Color(0xFFD4AF37)
private val ParchmentBg = Color(0xFFFFFDF7)
private val CardBg      = Color(0xFFFBF4E8)
private val InkDark     = Color(0xFF2C1810)
private val MutedTaupe  = Color(0xFF5E453A)

@Composable
fun NoticeDesignPreviewScreen(
    data: NoticeDesignData,
    onBack: () -> Unit,
    onBroadcast: (posterPng: ByteArray, onComplete: (Result<AntimYatraPublishResult>) -> Unit) -> Unit,
    modifier: Modifier = Modifier
) {
    val gu = LocalAppLanguage.current == AppLanguage.GUJARATI
    var isSending by remember { mutableStateOf(false) }
    var feedback by remember { mutableStateOf<String?>(null) }
    var sendSucceeded by remember { mutableStateOf(false) }
    var posterBounds by remember { mutableStateOf<IntRect?>(null) }
    var portraitScale by remember(data.photoBytes) { mutableFloatStateOf(1.25f) }
    var portraitOffset by remember(data.photoBytes) { mutableStateOf(Offset(0f, 0f)) }
    val familyCount = data.familyMembers.count(String::isNotBlank).coerceAtMost(5)
    val extraContentRows = (familyCount - 2).coerceAtLeast(0) + if (data.name.length > 28) 1 else 0
    val posterAspectRatio = (0.62f - extraContentRows * 0.015f).coerceAtLeast(0.58f)

    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        TextButton(onClick = onBack) { Text(if (gu) "← પાછા" else "← Back") }
        BoxWithConstraints(
            Modifier.fillMaxWidth().aspectRatio(posterAspectRatio).onGloballyPositioned { coordinates ->
                posterBounds = coordinates.boundsInWindow().roundToIntRect()
            }
        ) {
            val posterWidth = maxWidth
            val posterHeight = maxHeight

            // ── Background & Professional Frame ──
            PosterBackdrop(Modifier.fillMaxSize())

            // ── Main Content Layer (Optimized vertical padding to ensure logo visibility) ──
            Column(
                modifier = Modifier.fillMaxSize().padding(
                    horizontal = posterWidth * 0.10f,
                    vertical = posterHeight * 0.012f
                ),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // ── Sacred Invocation: ૐ શાંતિઃ ──
                Text(
                    "ૐ શાંતિઃ",
                    color = AntiqueGold,
                    fontFamily = AnekGujaratiFont,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.5.sp
                )

                Spacer(Modifier.height(posterHeight * 0.002f))

                // ── Title: અંતિમ યાત્રા ──
                Text(
                    if (gu) "અંતિમ યાત્રા" else "Antim Yatra",
                    color = RoyalMaroon,
                    fontFamily = AnekGujaratiFont,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                    letterSpacing = 1.sp
                )

                Spacer(Modifier.height(posterHeight * 0.004f))
                OrnateDivider(Modifier.fillMaxWidth(0.55f))
                Spacer(Modifier.height(posterHeight * 0.004f))

                // ── Portrait Frame (Circular with Gold Foil Border & Touch Gestures) ──
                PortraitFrame(
                    photoBytes = data.photoBytes,
                    scale = portraitScale,
                    translation = portraitOffset,
                    onTransform = { pan, zoom, frameSize ->
                        val nextScale = (portraitScale * zoom).coerceIn(1f, 4f)
                        val maxPanX = (nextScale * 0.5f) * frameSize.width
                        val maxPanY = (nextScale * 0.5f) * frameSize.height
                        portraitScale = nextScale
                        portraitOffset = Offset(
                            (portraitOffset.x + pan.x).coerceIn(-maxPanX, maxPanX),
                            (portraitOffset.y + pan.y).coerceIn(-maxPanY, maxPanY)
                        )
                    },
                    modifier = Modifier.fillMaxWidth(0.33f).aspectRatio(1f)
                )

                Spacer(Modifier.height(posterHeight * 0.004f))

                // ── Deceased Name: સ્વ. [Name] (No box, Anek Gujarati SemiBold 600) ──
                Text(
                    "સ્વ. ${data.name.ifBlank { "અહીં નામ લખો" }}",
                    modifier = Modifier.fillMaxWidth(),
                    color = RoyalMaroon,
                    fontFamily = AnekGujaratiFont,
                    fontSize = 24.sp,
                    lineHeight = 29.sp,
                    fontWeight = FontWeight.SemiBold, // SemiBold 600
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Clip
                )

                Spacer(Modifier.height(2.dp))

                // ── Date & Day ──
                Text(
                    deathDateLine(data, gu),
                    color = DeepWine,
                    fontFamily = AnekGujaratiFont,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )

                Spacer(Modifier.height(posterHeight * 0.004f))
                OrnateDivider(Modifier.fillMaxWidth(0.8f))
                Spacer(Modifier.height(posterHeight * 0.004f))

                // ── Obituary Message ──
                Text(
                    obituaryText(data),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    color = InkDark,
                    fontFamily = AnekGujaratiFont,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    maxLines = 6,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(posterHeight * 0.004f))

                // ── Funeral Journey / Antim Yatra Details Card ──
                JourneyDetailsCard(data, gu, Modifier.fillMaxWidth())

                Spacer(Modifier.height(posterHeight * 0.004f))

                // ── Family Names (લી.) ──
                FamilyNames(data.familyMembers, Modifier.fillMaxWidth())

                Spacer(Modifier.height(2.dp))

                // ── Mobile Number ──
                Text(
                    "મો. ${data.mobileNumber.ifBlank { "—" }}",
                    color = MutedTaupe,
                    fontFamily = AnekGujaratiFont,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(posterHeight * 0.004f))
                OrnateDivider(Modifier.fillMaxWidth(0.65f))
                Spacer(Modifier.height(posterHeight * 0.003f))

                // ── Organization Logo (Guaranteed visible at bottom) ──
                Box(
                    modifier = Modifier.fillMaxWidth(0.48f)
                        .height(posterHeight * 0.035f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(CardBg)
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(Res.drawable.bodla_parivar_logo),
                        contentDescription = "Bodla Parivar",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }
            }
        }

        // ── Photo Adjustment Control Card ──
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        if (gu) "ફોટો એડજસ્ટ કરો" else "Adjust Photo",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        if (gu) "ખસેડવા ખેંચો • ઝૂમ માટે બટન વાપરો" else "Drag to pan • Use buttons to zoom",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            portraitScale = 1.25f
                            portraitOffset = Offset(0f, 0f)
                        },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("🎯 Face", fontSize = 11.sp)
                    }
                    OutlinedButton(
                        onClick = {
                            portraitScale = (portraitScale / 1.25f).coerceIn(1f, 4f)
                        },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("－ Zoom", fontSize = 11.sp)
                    }
                    OutlinedButton(
                        onClick = {
                            portraitScale = (portraitScale * 1.25f).coerceIn(1f, 4f)
                        },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("＋ Zoom", fontSize = 11.sp)
                    }
                    TextButton(
                        onClick = {
                            portraitScale = 1.25f
                            portraitOffset = Offset(0f, 0f)
                        },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text(if (gu) "રીસેટ" else "Reset", fontSize = 11.sp)
                    }
                }
            }
        }

        Button(
            onClick = {
                if (!isSending) {
                    feedback = null
                    sendSucceeded = false
                    val bounds = posterBounds
                    if (bounds == null || bounds.width <= 0 || bounds.height <= 0) {
                        feedback = if (gu) "પોસ્ટર તૈયાર થઈ રહ્યું છે. કૃપા કરીને એક ક્ષણ રાહ જુઓ અને ફરી પ્રયત્ન કરો." else "Poster is rendering. Please wait a moment and try again."
                    } else {
                        val posterPng = PlatformCapturePoster(bounds)
                            if (posterPng == null || posterPng.size < 100) {
                            feedback = if (gu) "પોસ્ટર ઇમેજ બનાવી શકાઈ નથી. કૃપા કરીને ફરી પ્રયત્ન કરો." else "Could not create the poster image. Please try again."
                        } else {
                            isSending = true
                            onBroadcast(posterPng) { result ->
                                isSending = false
                                sendSucceeded = result.isSuccess
                                feedback = if (result.isSuccess) {
                                    if (result.getOrNull()?.push_sent == true) {
                                        if (gu) "નોટિસ પ્રકાશિત થઈ અને મોબાઇલ પુશ પણ મોકલાઈ." else "Notice published and mobile push sent."
                                    } else if (result.getOrNull()?.push_service_missing == true) {
                                        if (gu) "નોટિસ પ્રકાશિત થઈ, પણ નોટિફિકેશન સેવા ઉપલબ્ધ નથી. મોબાઇલ પુશ મોકલાઈ નથી." else "Notice published, but the notification service is not available. Mobile push was not delivered."
                                    } else {
                                        if (gu) "નોટિસ પ્રકાશિત થઈ. મોબાઇલ પુશ મોકલાઈ નથી." else "Notice published. Mobile push was not delivered."
                                    }.let { base ->
                                        if (result.getOrNull()?.feed_refresh_error.isNullOrBlank()) base
                                        else "$base Notices could not refresh on this device. Open Notices and refresh manually."
                                    }
                                } else {
                                    result.exceptionOrNull()?.message
                                        ?: if (gu) "નોટિસ મોકલી શકાઈ નથી." else "Could not send the notice."
                                }
                            }
                        }
                    }
                }
            },
            enabled = !isSending,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isSending) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
            else Text(if (gu) "નોટિસમાં પ્રકાશિત કરો" else "Publish to Notices")
        }
        feedback?.let { message ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                color = if (sendSucceeded) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.errorContainer
            ) {
                Text(
                    message,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    textAlign = TextAlign.Center,
                    color = if (sendSucceeded) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun PosterBackdrop(modifier: Modifier = Modifier) {
    Box(modifier) {
        Image(
            painter = painterResource(Res.drawable.poster_bg_golden_mandala),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Canvas(Modifier.fillMaxSize()) {
            drawRect(Color(0x1AF3EAD8))

            val outerInset = size.minDimension * 0.022f
            val innerInset = size.minDimension * 0.034f
            
            drawRoundRect(
                color = AntiqueGold,
                topLeft = Offset(outerInset, outerInset),
                size = Size(size.width - outerInset * 2, size.height - outerInset * 2),
                cornerRadius = CornerRadius(size.minDimension * 0.016f),
                style = Stroke(width = size.minDimension * 0.005f)
            )
            drawRoundRect(
                color = LightGold.copy(alpha = 0.8f),
                topLeft = Offset(innerInset, innerInset),
                size = Size(size.width - innerInset * 2, size.height - innerInset * 2),
                cornerRadius = CornerRadius(size.minDimension * 0.012f),
                style = Stroke(width = size.minDimension * 0.002f)
            )

            val ornamentRadius = size.minDimension * 0.014f
            val corners = listOf(
                Offset(innerInset * 1.5f, innerInset * 1.5f),
                Offset(size.width - innerInset * 1.5f, innerInset * 1.5f),
                Offset(innerInset * 1.5f, size.height - innerInset * 1.5f),
                Offset(size.width - innerInset * 1.5f, size.height - innerInset * 1.5f)
            )
            corners.forEach { pt ->
                drawCircle(AntiqueGold, radius = ornamentRadius * 0.8f, center = pt)
                drawCircle(ParchmentBg, radius = ornamentRadius * 0.35f, center = pt)
            }
        }
    }
}

@Composable
private fun PortraitFrame(
    photoBytes: ByteArray?,
    scale: Float,
    translation: Offset,
    onTransform: (pan: Offset, zoom: Float, frameSize: Size) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier
            .shadow(10.dp, CircleShape)
            .clip(CircleShape)
            .background(ParchmentBg)
            .border(3.dp, AntiqueGold, CircleShape)
            .padding(3.dp)
            .border(1.5.dp, LightGold, CircleShape)
            .clip(CircleShape)
            .pointerInput(photoBytes) {
                val transform = onTransform
                detectTransformGestures { _, pan, zoom, _ ->
                    transform(pan, zoom, Size(size.width.toFloat(), size.height.toFloat()))
                }
            },
        contentAlignment = Alignment.Center
    ) {
        if (photoBytes != null) {
            DesignPhotoPreview(
                photoBytes,
                Modifier.fillMaxSize().graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = translation.x
                    translationY = translation.y
                }
            )
        } else {
            Box(
                Modifier.fillMaxSize().background(
                    Brush.radialGradient(listOf(Color(0xFFEFE5D0), Color(0xFFD6C3A1)))
                ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "ફોટો",
                    color = MutedTaupe,
                    fontFamily = AnekGujaratiFont,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun OrnateDivider(modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
        HorizontalDivider(Modifier.weight(1f), color = AntiqueGold.copy(alpha = 0.7f), thickness = 1.dp)
        Text("  ✦  ", color = AntiqueGold, fontSize = 11.sp)
        HorizontalDivider(Modifier.weight(1f), color = AntiqueGold.copy(alpha = 0.7f), thickness = 1.dp)
    }
}

@Composable
private fun JourneyDetailsCard(data: NoticeDesignData, gu: Boolean, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(CardBg)
            .border(1.dp, AntiqueGold.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            if (gu) "સ્મશાન યાત્રા / અંતિમ યાત્રા" else "Antim Yatra Details",
            color = RoyalMaroon,
            fontFamily = AnekGujaratiFont,
            fontSize = 12.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center
        )

        val route = if (gu) {
            buildString {
                append("અંતિમ યાત્રા")
                if (data.antimYatraDate.isNotBlank()) append(" તા. ${data.antimYatraDate.trim()}")
                if (data.antimYatraTime.isNotBlank()) {
                    val parts = data.antimYatraTime.trim().split(":")
                    val hour24 = parts.getOrNull(0)?.toIntOrNull() ?: 16
                    val minute = parts.getOrNull(1) ?: "00"
                    val hour12 = when {
                        hour24 == 0 -> 12
                        hour24 > 12 -> hour24 - 12
                        else -> hour24
                    }
                    val hourStr = hour12.toString().padStart(2, '0')
                    val period = if (hour24 < 12) "સવારે" else "સાંજે"
                    append(" ના $period $hourStr:$minute વાગ્યે")
                }
                if (data.address.isNotBlank()) append(" અમારા નિવાસસ્થાન ${data.address.trim()} થી નીકળીને")
                if (data.smashanGhat.isNotBlank()) append(" ${data.smashanGhat.trim()} જશે.") else append(" જશે.")
            }
        } else {
            buildString {
                append("Antim Yatra")
                if (data.antimYatraDate.isNotBlank()) append(" on ${data.antimYatraDate.trim()}")
                if (data.antimYatraTime.isNotBlank()) {
                    val parts = data.antimYatraTime.trim().split(":")
                    val hour24 = parts.getOrNull(0)?.toIntOrNull() ?: 0
                    val minute = parts.getOrNull(1) ?: "00"
                    val hour12 = when {
                        hour24 == 0 -> 12
                        hour24 > 12 -> hour24 - 12
                        else -> hour24
                    }
                    val hourStr = hour12.toString().padStart(2, '0')
                    val period = if (hour24 < 12) "AM" else "PM"
                    append(" at $hourStr:$minute $period")
                }
                if (data.address.isNotBlank()) append(" from ${data.address.trim()}")
                if (data.smashanGhat.isNotBlank()) append(" to ${data.smashanGhat.trim()}")
                append(".")
            }
        }

        Text(
            route,
            color = InkDark,
            fontFamily = AnekGujaratiFont,
            fontSize = 11.sp,
            lineHeight = 15.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun obituaryText(data: NoticeDesignData): String {
    val announcement = data.toNoticeText().lineSequence().drop(5).firstOrNull().orEmpty()
    return listOf(announcement, "ભગવાન એમના દિવ્ય આત્માને શાંતિ અર્પે ...")
        .filter(String::isNotBlank)
        .joinToString("\n")
}

private fun deathDateLine(data: NoticeDesignData, gu: Boolean): String {
    val date = data.dateOfDeath.trim().let { if (gu) it.toGujaratiDigits() else it }
    val day = data.day.trim()
    if (gu) {
        return buildString {
            if (date.isNotBlank()) append("અવસાન તા. $date")
            if (day.isNotBlank()) {
                if (isNotEmpty()) append(", ")
                append("વાર : $day")
            }
        }
    }
    return listOfNotNull(
        date.takeIf(String::isNotBlank)?.let { "Date of Passing: $it" },
        day.takeIf(String::isNotBlank)?.let { "Day: $it" }
    ).joinToString(", ")
}

private fun String.toGujaratiDigits(): String = map { char ->
    if (char in '0'..'9') ('૦'.code + (char.code - '0'.code)).toChar() else char
}.joinToString("")

@Composable
private fun FamilyNames(members: List<String>, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            color = RoyalMaroon,
            shape = RoundedCornerShape(4.dp)
        ) {
            Text(
                "લી.", modifier = Modifier.padding(horizontal = 10.dp, vertical = 1.dp),
                color = ParchmentBg, fontFamily = AnekGujaratiFont,
                fontSize = 11.sp, fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.height(2.dp))
        val family = members.map(String::trim).filter(String::isNotBlank).take(5)
        if (family.isEmpty()) {
            Text(
                "પરિવારજનો", color = InkDark,
                fontFamily = AnekGujaratiFont, fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        } else {
            family.forEach { member ->
                Text(
                    member, modifier = Modifier.fillMaxWidth(),
                    color = InkDark, fontFamily = AnekGujaratiFont,
                    fontSize = 11.sp, lineHeight = 14.sp, fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center, overflow = TextOverflow.Clip
                )
            }
        }
    }
}

@Composable
expect fun DesignPhotoPreview(bytes: ByteArray, modifier: Modifier = Modifier)
