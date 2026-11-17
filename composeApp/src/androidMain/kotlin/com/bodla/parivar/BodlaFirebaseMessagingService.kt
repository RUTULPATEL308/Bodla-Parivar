package com.bodla.parivar

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.os.PowerManager
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

        // Exclude sender from receiving their own push notification
        val senderProfileId = remoteMessage.data["sender_profile_id"]
        if (!senderProfileId.isNullOrBlank()) {
            val userProfileJson = TokenStorage().getUserProfile()
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
        val title = if (type == "NOTICE") {
            remoteMessage.notification?.title?.takeIf { it.isNotBlank() }
                ?: remoteMessage.data["title_gu"]?.takeIf { it.isNotBlank() }
                ?: "અંતિમ યાત્રા / સ્મશાન યાત્રા"
        } else {
            remoteMessage.notification?.title
                ?: remoteMessage.data["title_gu"]
                ?: remoteMessage.data["title_en"]
                ?: "બોદલા પરિવાર (Bodla Parivar)"
        }

        val body = if (type == "NOTICE") {
            (remoteMessage.data["body_gu"] ?: remoteMessage.notification?.body ?: "")
                .lineSequence()
                .firstOrNull { it.isNotBlank() }
                ?.take(120)
                .orEmpty()
        } else {
            remoteMessage.notification?.body
                ?: remoteMessage.data["body_gu"]
                ?: remoteMessage.data["body_en"]
                ?: ""
        }

        // Wake screen if screen is off so alert pops up immediately
        wakeUpScreen()

        // If app is closed / in background, also launch NoticeAlertActivity directly
        launchAlertDirectly(title, body, remoteMessage.data)

        sendNotification(title, body, remoteMessage.data)
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

    private fun sendNotification(title: String, messageBody: String, data: Map<String, String>) {
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

        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(messageBody)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setDefaults(Notification.DEFAULT_ALL)
            .setContentIntent(fullScreenPendingIntent)
            .build()

        notificationManager.notify(notificationId, notification)
    }
}
