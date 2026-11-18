package com.bodla.parivar

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.os.PowerManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.net.URL
import androidx.core.app.NotificationCompat
import com.bodla.parivar.core.AndroidPlatform
import com.bodla.parivar.core.security.TokenStorage
import com.bodla.parivar.domain.model.Profile
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import io.github.aakira.napier.Napier
import kotlinx.serialization.json.Json

class BodlaFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "bodla_parivar_channel_v4"
        const val ALERT_NOTIFICATION_ID_KEY = "alert_notification_id"
        private var lastProcessedNoticeId: String? = null
        private var lastProcessedTime = 0L
    }

    override fun onCreate() {
        super.onCreate()
        if (AndroidPlatform.appContext == null) {
            AndroidPlatform.appContext = applicationContext
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Napier.d("FCM Registration Token refreshed: $token", tag = "FCM")
        TokenManager.refresh(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        if (AndroidPlatform.appContext == null) {
            AndroidPlatform.appContext = applicationContext
        }
        val noticeId = remoteMessage.data["reference_id"] ?: remoteMessage.data["notification_id"] ?: ""
        val now = System.currentTimeMillis()
        if (noticeId.isNotBlank() && noticeId == lastProcessedNoticeId && now - lastProcessedTime < 4000) {
            Napier.d("Skipping duplicate FCM message for notice $noticeId", tag = "FCM")
            return
        }
        if (noticeId.isNotBlank()) {
            lastProcessedNoticeId = noticeId
            lastProcessedTime = now
        }
        Napier.d("FCM Message received from: ${remoteMessage.from}", tag = "FCM")

        // Only deliver notifications to users who are logged in
        val tokenStorage = TokenStorage()
        val authToken = tokenStorage.getToken()
        val userProfileJson = tokenStorage.getUserProfile()
        if (authToken.isNullOrBlank() && userProfileJson.isNullOrBlank()) {
            Napier.d("Skipping push notification: user is not logged in", tag = "FCM")
            return
        }

        // Exclude sender from receiving their own push notification
        val senderProfileId = remoteMessage.data["sender_profile_id"]
        if (!senderProfileId.isNullOrBlank()) {
            if (userProfileJson != null) {
                val profile = runCatching { Json.decodeFromString<Profile>(userProfileJson) }.getOrNull()
                if (profile?.id == senderProfileId) {
                    Napier.d("Skipping push notification sent by self ($senderProfileId)", tag = "FCM")
                    return
                }
            }
        }

        // Extract title and body from notification payload or data payload
        val type = remoteMessage.data["type"]
        val isAntim = remoteMessage.data["is_antim_yatra"] == "true" ||
            remoteMessage.data["title_gu"]?.contains("અંતિમ") == true ||
            remoteMessage.data["body_gu"]?.contains("અંતિમ") == true
        val isBesanu = remoteMessage.data["is_besanu"] == "true" ||
            remoteMessage.data["title_gu"]?.contains("બેસણું") == true ||
            remoteMessage.data["body_gu"]?.contains("બેસણું") == true

        val title = when {
            isAntim -> remoteMessage.data["title_gu"]?.takeIf { it.isNotBlank() } ?: "અંતિમ યાત્રા"
            isBesanu -> remoteMessage.data["title_gu"]?.takeIf { it.isNotBlank() } ?: "બેસણું"
            type == "NOTICE" -> {
                remoteMessage.notification?.title?.takeIf { it.isNotBlank() }
                    ?: remoteMessage.data["title_gu"]?.takeIf { it.isNotBlank() }
                    ?: "બોદલા પરિવાર સૂચના"
            }
            else -> {
                remoteMessage.notification?.title
                    ?: remoteMessage.data["title_gu"]
                    ?: remoteMessage.data["title_en"]
                    ?: "બોદલા પરિવાર (Bodla Parivar)"
            }
        }

        val body = (remoteMessage.data["body_gu"] ?: remoteMessage.notification?.body ?: "")
            .lineSequence()
            .firstOrNull { it.isNotBlank() }
            ?.take(160)
            ?: (remoteMessage.notification?.body ?: remoteMessage.data["body_en"] ?: "")

        val imageUrl = remoteMessage.data["image_url"] ?: remoteMessage.data["imageUrl"]

        // Wake screen if screen is off so alert pops up immediately
        wakeUpScreen()

        // If app is closed / in background, also launch NoticeAlertActivity directly
        launchAlertDirectly(title, remoteMessage.data["body_gu"] ?: body, remoteMessage.data)

        sendNotification(title, body, remoteMessage.data, imageUrl)
    }

    private fun wakeUpScreen() {
        try {
            val powerManager = getSystemService(POWER_SERVICE) as? PowerManager
            if (powerManager != null && !powerManager.isInteractive) {
                val wakeLock = powerManager.newWakeLock(
                    PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                    "BodlaParivar:NoticeWakeLock"
                )
                wakeLock.acquire(10000L) // Release after 10s automatically
            }
        } catch (e: Exception) {
            Napier.e("Failed to acquire wake lock: ${e.message}", tag = "FCM")
        }
    }

    private fun launchAlertDirectly(title: String, body: String, data: Map<String, String>) {
        try {
            val alertIntent = Intent(this, NoticeAlertActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                putExtra("title", title)
                putExtra("body", body)
                for ((key, value) in data) {
                    putExtra(key, value)
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) { // Android 14+ (API 34+)
                val options = android.app.ActivityOptions.makeBasic().apply {
                    setPendingIntentBackgroundActivityStartMode(
                        android.app.ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
                    )
                }
                startActivity(alertIntent, options.toBundle())
            } else {
                startActivity(alertIntent)
            }
        } catch (e: Exception) {
            Napier.e("Direct startActivity failed: ${e.message}", tag = "FCM")
        }
    }

    private fun sendNotification(title: String, messageBody: String, data: Map<String, String>, imageUrl: String? = null) {
        val notificationId = (System.currentTimeMillis() % 100000).toInt()

        val fullScreenIntent = Intent(this, NoticeAlertActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra("title", title)
            putExtra("body", messageBody)
            putExtra(ALERT_NOTIFICATION_ID_KEY, notificationId)
            for ((key, value) in data) {
                putExtra(key, value)
            }
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            this,
            notificationId,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "બોદલા પરિવાર સૂચનાઓ (Village Notices)",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "General and emergency notifications for Bodla Parivar members"
                enableVibration(true)
                enableLights(true)
                setBypassDnd(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION), null)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Download poster image for big picture notification style if available
        val pictureBitmap: Bitmap? = if (!imageUrl.isNullOrBlank()) {
            runCatching {
                val url = URL(imageUrl)
                val conn = url.openConnection()
                conn.connectTimeout = 4000
                conn.readTimeout = 4000
                BitmapFactory.decodeStream(conn.getInputStream())
            }.getOrNull()
        } else null

        val notificationBuilder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(messageBody)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setDefaults(Notification.DEFAULT_ALL)
            .setContentIntent(fullScreenPendingIntent)
            .setFullScreenIntent(fullScreenPendingIntent, true)

        if (pictureBitmap != null) {
            notificationBuilder
                .setLargeIcon(pictureBitmap)
                .setStyle(
                    NotificationCompat.BigPictureStyle()
                        .bigPicture(pictureBitmap)
                        .setSummaryText(messageBody)
                )
        } else {
            notificationBuilder.setStyle(NotificationCompat.BigTextStyle().bigText(messageBody))
        }

        notificationManager.notify(notificationId, notificationBuilder.build())

        // Automatically dismiss the status bar notification after 15 seconds
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            runCatching {
                notificationManager.cancel(notificationId)
            }
        }, 15000L)
    }
}
