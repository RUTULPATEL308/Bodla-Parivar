package com.bodla.parivar

import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.bodla.parivar.core.PendingNotices

/**
 * NoticeAlertActivity:
 * Matches exactly Image 2:
 * - Warm ivory/cream card surface (#FFFCF5 / #FDFBF7)
 * - Top soft blue pill: "📢 નવી ગ્રામ સૂચના"
 * - Clean bold notice title
 * - Full readable description text
 * - Clean rounded action buttons: "બંધ કરો (Close)" and deep navy "જુઓ (Open)"
 */
class NoticeAlertActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Wake screen and show over lock screen
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }

        val alertNotificationId = intent.getIntExtra(
            BodlaFirebaseMessagingService.ALERT_NOTIFICATION_ID_KEY, -1
        )
        if (alertNotificationId >= 0) {
            val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            nm.cancel(alertNotificationId)
        }

        val noticeId = intent.getStringExtra("reference_id")
            ?: intent.getStringExtra("notice_id")
            ?: intent.getStringExtra("id")
            .orEmpty()
        val title = intent.getStringExtra("title")
            ?: intent.getStringExtra("title_gu")
            ?: "બોદલા ગામના તમામ સ્નેહીજનોને સપ્રેમ નમસ્તે."
        val body = intent.getStringExtra("body")
            ?: intent.getStringExtra("body_gu")
            ?: ""
        val imageUrl = intent.getStringExtra("image_url")

        setContent {
            ExactImage2NoticeAlert(
                title = title,
                body = body,
                imageUrl = imageUrl,
                onOpen = {
                    val mainIntent = Intent(this, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        if (noticeId.isNotBlank()) {
                            putExtra("reference_id", noticeId)
                        }
                    }
                    if (noticeId.isNotBlank()) {
                        PendingNotices.post(noticeId)
                    }
                    startActivity(mainIntent)
                    finish()
                },
                onClose = {
                    finish()
                }
            )
        }
    }
}

@Composable
fun ExactImage2NoticeAlert(
    title: String,
    body: String,
    imageUrl: String?,
    onOpen: () -> Unit,
    onClose: () -> Unit
) {
    // Dimmed transparent backdrop
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            .clickable { onClose() }
            .padding(horizontal = 14.dp, vertical = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .clickable(enabled = false) {}
                .shadow(
                    elevation = 20.dp,
                    shape = RoundedCornerShape(26.dp)
                ),
            shape = RoundedCornerShape(26.dp),
            color = Color(0xFFFAF7F0) // Warm Cream/Ivory background matching Image 2
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 1. Soft Light Blue Pill Tag: 📢 નવી ગ્રામ સૂચના
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFE8F0FE) // Light soft pastel blue from Image 2
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "📢 નવી ગ્રામ સૂચના",
                            color = Color(0xFF1E3A8A), // Deep subtle blue text
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // 2. Bold Title
                Text(
                    text = title,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B),
                    lineHeight = 24.sp,
                    modifier = Modifier.fillMaxWidth()
                )

                // Optional Image if present
                if (!imageUrl.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = "Notice image",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .clip(RoundedCornerShape(14.dp))
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 3. Message Body Text
                if (body.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = body,
                            fontSize = 13.5.sp,
                            lineHeight = 21.sp,
                            color = Color(0xFF334155),
                            fontWeight = FontWeight.Normal
                        )
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))

                // 4. Action Buttons Matching Image 2 - full width and no text truncation
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // "બંધ કરો (Close)" - Clean White Button
                    OutlinedButton(
                        onClick = onClose,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.White,
                            contentColor = Color(0xFF1E293B)
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1))
                    ) {
                        Text(
                            text = "બંધ કરો (Close)",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            maxLines = 1,
                            color = Color(0xFF334155)
                        )
                    }

                    // "જુઓ (Open)" - Deep Navy Brand Button
                    Button(
                        onClick = onOpen,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF173B78), // Deep Navy from Image 2
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            text = "જુઓ (Open)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            maxLines = 1,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
