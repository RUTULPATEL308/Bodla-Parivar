package com.bodla.parivar

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.bodla.parivar.core.security.TokenStorage
import io.github.aakira.napier.Napier

/**
 * Short‑lived foreground service that launches the full‑screen NoticeAlertActivity
 * without showing a heads‑up notification banner on unlocked screens.
 */
class NoticeForegroundService : Service() {

    companion object {
        const val CHANNEL_ID = "notice_foreground_channel"
        const val FOREGROUND_ID = 1001
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createChannel()

        // Extract data passed from the messaging service
        val title = intent?.getStringExtra("title") ?: "બોદલા પરિવાર સૂચના"
        val body = intent?.getStringExtra("body") ?: ""
        val notificationId = intent?.getIntExtra(BodlaFirebaseMessagingService.ALERT_NOTIFICATION_ID_KEY, -1) ?: -1
        val data = intent?.extras?.let { bundle ->
            val map = mutableMapOf<String, String>()
            for (key in bundle.keySet()) {
                if (key != "title" && key != "body" && key != BodlaFirebaseMessagingService.ALERT_NOTIFICATION_ID_KEY) {
                    map[key] = bundle.getString(key) ?: ""
                }
            }
            map
        } ?: emptyMap()

        // Minimal foreground notification (no heads‑up banner)
        val pending = PendingIntent.getActivity(
            this,
            notificationId,
            Intent(this, NoticeAlertActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra("title", title)
                putExtra("body", body)
                putExtra(BodlaFirebaseMessagingService.ALERT_NOTIFICATION_ID_KEY, notificationId)
                data.forEach { (k, v) -> putExtra(k, v) }
            },
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        )

        val foregroundNotification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setContentIntent(pending)
            .build()

        startForeground(FOREGROUND_ID, foregroundNotification)

        // Launch the full‑screen activity directly
        val activityIntent = Intent(this, NoticeAlertActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra("title", title)
            putExtra("body", body)
            putExtra(BodlaFirebaseMessagingService.ALERT_NOTIFICATION_ID_KEY, notificationId)
            data.forEach { (k, v) -> putExtra(k, v) }
        }
        startActivity(activityIntent)

        // Remove the foreground notification and stop the service
        stopForeground(true)
        stopSelf()
        return START_NOT_STICKY
    }

    private fun createChannel() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Notice Foreground Service",
                NotificationManager.IMPORTANCE_NONE
            ).apply {
                // No sound, vibration, or visual interruption
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_SECRET
            }
            manager.createNotificationChannel(channel)
        }
    }
}
