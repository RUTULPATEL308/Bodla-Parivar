package com.bodla.parivar

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import io.github.aakira.napier.Napier
import java.net.URL

class BodlaFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "bodla_parivar_channel_v2"
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Napier.d("FCM Registration Token refreshed: $token", tag = "FCM")
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Napier.d("FCM Message received from: ${remoteMessage.from}", tag = "FCM")

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

        sendNotification(title, body, remoteMessage.data)
    }

    private fun sendNotification(title: String, messageBody: String, data: Map<String, String>) {
        val notificationId = (System.currentTimeMillis() % 100000).toInt()
        val intent = Intent(this, MainActivity::class.java).apply {
            // CLEAR_TOP + SINGLE_TOP delivers the tap to the running activity
            // through onNewIntent instead of starting a second instance.
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            for ((key, value) in data) {
                putExtra(key, value)
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = NOTIFICATION_CHANNEL_ID
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(messageBody)
            .setStyle(NotificationCompat.BigTextStyle().bigText(messageBody))
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)

        data["image_url"]?.takeIf(String::isNotBlank)?.let { imageUrl ->
            runCatching {
                val connection = URL(imageUrl).openConnection().apply {
                    connectTimeout = 8_000
                    readTimeout = 8_000
                }
                val bytes = connection.getInputStream().use { it.readBytes() }
                decodeNotificationBitmap(bytes)
            }.getOrNull()?.let { poster ->
                notificationBuilder
                    .setLargeIcon(poster)
                    .setStyle(NotificationCompat.BigPictureStyle().bigPicture(poster).bigLargeIcon(null as android.graphics.Bitmap?))
            }
        }

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Notification channel is required on Android Oreo (API 26) and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "બોદલા પરિવાર સૂચનાઓ (Village Notices)",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "General and emergency notifications for Bodla Parivar members"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(notificationId, notificationBuilder.build())
    }

    /**
     * Decodes the notification picture without allocating the full poster,
     * which is far larger than the notification needs.
     */
    private fun decodeNotificationBitmap(bytes: ByteArray): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (bounds.outWidth / sample > 1_200) sample *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
    }
}
