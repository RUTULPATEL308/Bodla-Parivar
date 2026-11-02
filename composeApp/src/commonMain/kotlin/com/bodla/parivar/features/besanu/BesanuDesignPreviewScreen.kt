package com.bodla.parivar.features.besanu

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
import com.bodla.parivar.features.design.DesignPhotoPreview
import org.jetbrains.compose.resources.painterResource

// ── Besanu Designer Palette (Deep Teal & Bronze) ──
private val DeepTeal     = Color(0xFF1A3C40)
private val DarkCyan     = Color(0xFF2C6E6A)
private val WarmBronze   = Color(0xFF8B6914)
private val LightBronze  = Color(0xFFB8860B)
private val CreamBg      = Color(0xFFFFF8F0)
private val CardBg       = Color(0xFFF5EDE0)
private val InkDark      = Color(0xFF1C1208)
private val MutedOlive   = Color(0xFF5A5030)

@Composable
fun BesanuDesignPreviewScreen(
    data: BesanuDesignData,
    onBack: () -> Unit,
    onBroadcast: (posterPng: ByteArray, onComplete: (Result<BesanuPublishResult>) -> Unit) -> Unit,
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
    val extraContentRows = (familyCount - 1).coerceAtLeast(0) + if (data.name.length > 25) 1 else 0
    val posterAspectRatio = (0.58f - extraContentRows * 0.02f).coerceAtLeast(0.50f)

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
            BesanuPosterBackdrop(Modifier.fillMaxSize())

            // ── Main Content Layer ──
            Column(
                modifier = Modifier.fillMaxSize().padding(
                    horizontal = posterWidth * 0.09f,
                    vertical = posterHeight * 0.020f
                ),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // ── Sacred Invocation: ૐ શાંતિઃ ──
                Text(
                    "ૐ શાંતિઃ",
                    color = WarmBronze,
                    fontFamily = AnekGujaratiFont,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )

                Spacer(Modifier.height(posterHeight * 0.002f))

                // ── Title: બેસણું ──
                Text(
                    if (gu) "બેસણું" else "Besanu",
                    color = DeepTeal,
                    fontFamily = AnekGujaratiFont,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                    letterSpacing = 1.sp
                )

                Spacer(Modifier.height(posterHeight * 0.003f))
                BesanuOrnateDivider(Modifier.fillMaxWidth(0.55f))
                Spacer(Modifier.height(posterHeight * 0.003f))

                // ── Portrait Frame (Circular with Bronze Border & Touch Gestures) ──
                BesanuPortraitFrame(
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
                    modifier = Modifier.fillMaxWidth(0.28f).aspectRatio(1f)
                )

                Spacer(Modifier.height(posterHeight * 0.003f))

                // ── Deceased Name: સ્વ. [Name] ──
                Text(
                    "સ્વ. ${data.name.ifBlank { "અહીં નામ લખો" }}",
                    modifier = Modifier.fillMaxWidth(),
                    color = DeepTeal,
                    fontFamily = AnekGujaratiFont,
                    fontSize = 20.sp,
                    lineHeight = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Clip
                )

                Spacer(Modifier.height(1.dp))

                // ── Date & Day ──
                Text(
                    besanuDeathDateLine(data, gu),
                    color = DarkCyan,
                    fontFamily = AnekGujaratiFont,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )

                Spacer(Modifier.height(posterHeight * 0.003f))
                BesanuOrnateDivider(Modifier.fillMaxWidth(0.8f))
                Spacer(Modifier.height(posterHeight * 0.003f))

                // ── Obituary Message ──
                Text(
                    besanuObituaryText(data),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    color = InkDark,
                    fontFamily = AnekGujaratiFont,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    maxLines = 6,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(posterHeight * 0.003f))

                // ── Besanu Details Card ──
                BesanuDetailsCard(data, gu, Modifier.fillMaxWidth())

                Spacer(Modifier.height(posterHeight * 0.003f))

                // ── Family Names (લી.) ──
                BesanuFamilyNames(data.familyMembers, Modifier.fillMaxWidth())

                if (data.mobileNumber.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "મો. ${data.mobileNumber.trim()}",
                        color = MutedOlive,
                        fontFamily = AnekGujaratiFont,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(Modifier.height(posterHeight * 0.003f))
                BesanuOrnateDivider(Modifier.fillMaxWidth(0.55f))
                Spacer(Modifier.height(posterHeight * 0.002f))

                // ── Organization Logo ──
                Box(
                    modifier = Modifier.fillMaxWidth(0.44f)
                        .height(posterHeight * 0.032f)
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

                Spacer(Modifier.height(posterHeight * 0.002f))
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
private fun BesanuPosterBackdrop(modifier: Modifier = Modifier) {
    Box(modifier) {
        Image(
            painter = painterResource(Res.drawable.poster_bg_golden_mandala),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Canvas(Modifier.fillMaxSize()) {
            // Teal-tinted overlay instead of warm parchment
            drawRect(Color(0x1AE0EDE8))

            val outerInset = size.minDimension * 0.022f
            val innerInset = size.minDimension * 0.034f

            drawRoundRect(
                color = WarmBronze,
                topLeft = Offset(outerInset, outerInset),
                size = Size(size.width - outerInset * 2, size.height - outerInset * 2),
                cornerRadius = CornerRadius(size.minDimension * 0.016f),
                style = Stroke(width = size.minDimension * 0.005f)
            )
            drawRoundRect(
                color = LightBronze.copy(alpha = 0.8f),
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
                drawCircle(WarmBronze, radius = ornamentRadius * 0.8f, center = pt)
                drawCircle(CreamBg, radius = ornamentRadius * 0.35f, center = pt)
            }
        }
    }
}

@Composable
private fun BesanuPortraitFrame(
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
            .background(CreamBg)
            .border(3.dp, WarmBronze, CircleShape)
            .padding(3.dp)
            .border(1.5.dp, LightBronze, CircleShape)
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
                    Brush.radialGradient(listOf(Color(0xFFE0EDE8), Color(0xFFC4D6CE)))
                ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "ફોટો",
                    color = MutedOlive,
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
private fun BesanuOrnateDivider(modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
        HorizontalDivider(Modifier.weight(1f), color = WarmBronze.copy(alpha = 0.7f), thickness = 1.dp)
        Text("  ✦  ", color = WarmBronze, fontSize = 11.sp)
        HorizontalDivider(Modifier.weight(1f), color = WarmBronze.copy(alpha = 0.7f), thickness = 1.dp)
    }
}

@Composable
private fun BesanuDetailsCard(data: BesanuDesignData, gu: Boolean, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(CardBg)
            .border(1.dp, WarmBronze.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Section Header: બેસણું
        Text(
            if (gu) "બેસણું" else "Besanu",
            color = DeepTeal,
            fontFamily = AnekGujaratiFont,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        BesanuOrnateDivider(Modifier.fillMaxWidth(0.5f))

        // Time and Date text
        val besanuDateTimeText = if (gu) {
            buildString {
                if (data.besanuDate.isNotBlank()) append("${data.besanuDate.trim()} ના રોજ")
                val timeSpan = BesanuDesignData.formatTimeSpan(data.besanuStartTime, data.besanuEndTime)
                if (timeSpan.isNotBlank()) {
                    if (isNotEmpty()) append("\n")
                    append("$timeSpan કલાક સુધી રાખેલ છે.")
                } else if (isNotEmpty()) {
                    append(" રાખેલ છે.")
                }
            }
        } else {
            buildString {
                if (data.besanuDate.isNotBlank()) append("Date: ${data.besanuDate.trim()}")
                val timeSpan = BesanuDesignData.formatTimeSpan(data.besanuStartTime, data.besanuEndTime)
                if (timeSpan.isNotBlank()) {
                    if (isNotEmpty()) append("\n")
                    append("Time: $timeSpan")
                }
            }
        }

        if (besanuDateTimeText.isNotBlank()) {
            Text(
                besanuDateTimeText,
                color = InkDark,
                fontFamily = AnekGujaratiFont,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
        }

        // Section: સ્થળ
        val locationText = data.address.trim()

        if (locationText.isNotBlank()) {
            Spacer(Modifier.height(2.dp))
            Text(
                if (gu) "સ્થળ" else "Location",
                color = DeepTeal,
                fontFamily = AnekGujaratiFont,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                locationText,
                color = InkDark,
                fontFamily = AnekGujaratiFont,
                fontSize = 11.5.sp,
                lineHeight = 15.sp,
                fontWeight = FontWeight.Normal,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun besanuObituaryText(data: BesanuDesignData): String {
    val name = data.name.trim().ifBlank { "..." }
    val deathDateFormatted = data.dateOfDeath.trim().toGujaratiDigits()
    val deathPart = if (deathDateFormatted.isNotBlank()) "નું તા. $deathDateFormatted ના રોજ" else "ના"
    return "દિલગીરી સાથે જણાવવાનું કે અમારા સ્વ. $name $deathPart અક્ષરવાસ થયેલ છે.\nપરમકૃપાળુ પરમાત્મા સદ્દગત દિવ્ય આત્માને શાંતિ પ્રદાન કરે એવી પ્રભુના ચરણોમાં પ્રાર્થના."
}

private fun besanuDeathDateLine(data: BesanuDesignData, gu: Boolean): String {
    val date = data.dateOfDeath.trim().let { if (gu) it.toGujaratiDigits() else it }
    val day = data.day.trim()
    if (gu) {
        return buildString {
            if (date.isNotBlank()) append("તા. $date")
            if (day.isNotBlank()) {
                if (isNotEmpty()) append(", ")
                append(day)
            }
        }
    }
    return listOfNotNull(
        date.takeIf(String::isNotBlank)?.let { "Date: $it" },
        day.takeIf(String::isNotBlank)
    ).joinToString(", ")
}

private fun String.toGujaratiDigits(): String = map { char ->
    if (char in '0'..'9') ('૦'.code + (char.code - '0'.code)).toChar() else char
}.joinToString("")

@Composable
private fun BesanuFamilyNames(members: List<String>, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            "લિ.",
            color = DeepTeal,
            fontFamily = AnekGujaratiFont,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
        BesanuOrnateDivider(Modifier.fillMaxWidth(0.4f))
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
