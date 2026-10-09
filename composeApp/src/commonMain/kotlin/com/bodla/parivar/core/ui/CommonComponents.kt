package com.bodla.parivar.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bodla.parivar.core.localization.LocalAppLanguage
import com.bodla.parivar.core.localization.Strings
import com.bodla.parivar.core.theme.*

// ------------------------------------------------------------------------------
// Standard 48dp x 48dp Icon Container (Specification Section 11)
// ------------------------------------------------------------------------------
@Composable
fun AppIconContainer(
    modifier: Modifier = Modifier,
    backgroundColor: Color = LightIconBg,
    contentColor: Color = SaffronPrimary,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(RoundedCornerShape(IconContainerRadius))
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

// ------------------------------------------------------------------------------
// Primary Action Button (48-52dp height, 12dp radius, Specification Section 12)
// ------------------------------------------------------------------------------
@Composable
fun BodlaButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: Color = SaffronPrimary,
    contentColor: Color = WarmWhiteSurface
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp),
        enabled = enabled,
        shape = RoundedCornerShape(ButtonCornerRadius),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = BorderStroke,
            disabledContentColor = TextSecondary
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp)
    ) {
        Text(
            text = text,
            style = BodlaButton,
            color = contentColor
        )
    }
}

// ------------------------------------------------------------------------------
// Secondary Outlined Button
// ------------------------------------------------------------------------------
@Composable
fun BodlaOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .height(48.dp),
        enabled = enabled,
        shape = RoundedCornerShape(ButtonCornerRadius),
        border = ButtonDefaults.outlinedButtonBorder.copy(
            brush = androidx.compose.ui.graphics.SolidColor(SaffronPrimary)
        )
    ) {
        Text(
            text = text,
            style = BodlaButton,
            color = SaffronPrimary
        )
    }
}

// ------------------------------------------------------------------------------
// Standard Card (16dp radius, subtle border & background)
// ------------------------------------------------------------------------------
@Composable
fun BodlaCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        shape = RoundedCornerShape(CardCornerRadius),
        colors = CardDefaults.cardColors(containerColor = WarmWhiteSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(BorderStroke)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}

// ------------------------------------------------------------------------------
// Section Header with optional "View More →" link
// ------------------------------------------------------------------------------
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = BodlaSection,
            color = TextPrimary
        )
        if (actionText != null && onActionClick != null) {
            Text(
                text = actionText,
                style = BodlaButton.copy(fontSize = 14.sp),
                color = SaffronPrimary,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { onActionClick() }
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            )
        }
    }
}

// ------------------------------------------------------------------------------
// DEMO DATA Tag (Specification Section 1)
// ------------------------------------------------------------------------------
@Composable
fun DemoDataBadge(modifier: Modifier = Modifier) {
    val lang = LocalAppLanguage.current
    Surface(
        color = StatusWarning.copy(alpha = 0.15f),
        shape = RoundedCornerShape(4.dp),
        modifier = modifier
    ) {
        Text(
            text = Strings.demoDataBadge(lang),
            color = SaffronPrimary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

// ------------------------------------------------------------------------------
// Status Chip
// ------------------------------------------------------------------------------
@Composable
fun StatusChip(
    status: String,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor) = when (status) {
        "APPROVED", "PUBLISHED", "ACTIVE", "RESOLVED" -> Pair(StatusSuccess.copy(alpha = 0.15f), StatusSuccess)
        "PENDING", "PENDING_APPROVAL", "UNDER_REVIEW" -> Pair(StatusWarning.copy(alpha = 0.15f), StatusWarning)
        "REJECTED", "CANCELLED", "EXPIRED" -> Pair(StatusError.copy(alpha = 0.15f), StatusError)
        else -> Pair(BorderStroke, TextSecondary)
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(ChipCornerRadius),
        modifier = modifier
    ) {
        Text(
            text = status.replace("_", " "),
            color = textColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

// ------------------------------------------------------------------------------
// Offline Notification Banner (Specification Section 57)
// ------------------------------------------------------------------------------
@Composable
fun OfflineBanner(modifier: Modifier = Modifier) {
    val lang = LocalAppLanguage.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(LightIconBg)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = Strings.offlineNotice(lang),
            style = BodlaCaption,
            color = SaffronPrimary,
            textAlign = TextAlign.Center
        )
    }
}

// ------------------------------------------------------------------------------
// Empty State View (Specification Section 57)
// ------------------------------------------------------------------------------
@Composable
fun EmptyStateView(
    message: String? = null,
    modifier: Modifier = Modifier
) {
    val lang = LocalAppLanguage.current
    val displayMessage = message ?: Strings.emptyData(lang)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        AppIconContainer(
            modifier = Modifier.size(56.dp),
            backgroundColor = LightIconBg
        ) {
            Text(
                text = "ℹ",
                color = SaffronPrimary,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = displayMessage,
            style = BodlaBody,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
    }
}

// ------------------------------------------------------------------------------
// Error State View (Specification Section 57)
// ------------------------------------------------------------------------------
@Composable
fun ErrorStateView(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    message: String? = null
) {
    val lang = LocalAppLanguage.current
    val displayMessage = message ?: Strings.errorGeneral(lang)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "⚠",
            color = StatusError,
            fontSize = 32.sp
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = displayMessage,
            style = BodlaBody,
            color = TextPrimary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        BodlaButton(
            text = Strings.tryAgain(lang),
            onClick = onRetry,
            modifier = Modifier.width(180.dp)
        )
    }
}
