package com.bodla.parivar

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.os.Build
import android.media.RingtoneManager
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.bodla.parivar.core.AndroidPlatform
import com.bodla.parivar.core.PendingNotices
import com.google.firebase.messaging.FirebaseMessaging
import io.github.aakira.napier.Napier
import java.lang.ref.WeakReference

class MainActivity : ComponentActivity() {
    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted) {
                Napier.w("Notification permission was denied; push alerts may not be displayed", tag = "FCM")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        AndroidPlatform.appContext = applicationContext
        AndroidPlatform.currentActivityRef = WeakReference(this)
        handleNotificationIntent(intent)
        createVillageNotificationChannel()
        requestNotificationPermission()

        // Subscribe to global topic for push broadcast notifications
        try {
            FirebaseMessaging.getInstance()
                .subscribeToTopic("all_users")
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Napier.d("Successfully subscribed to FCM topic: all_users", tag = "FCM")
                    } else {
                        Napier.w("Failed to subscribe to FCM topic: all_users", task.exception, tag = "FCM")
                    }
                }
        } catch (e: Exception) {
            Napier.e("FirebaseMessaging init error", e, tag = "FCM")
        }

        setContent {
            App()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotificationIntent(intent)
    }

    private fun handleNotificationIntent(intent: Intent?) {
        if (intent == null) return
        val extras = intent.extras
        if (extras != null) {
            for (key in extras.keySet()) {
                Napier.d("Notification Intent extra: $key = ${extras.get(key)?.toString()}", tag = "FCM")
            }
        }
        val noticeId = intent.getStringExtra("reference_id")
            ?: intent.getStringExtra("referenceId")
            ?: intent.getStringExtra("notice_id")
            ?: intent.getStringExtra("noticeId")
            ?: intent.getStringExtra("id")
            ?: intent.getStringExtra("gcm.notification.reference_id")
            ?: intent.getStringExtra("data.reference_id")
        if (!noticeId.isNullOrBlank()) {
            PendingNotices.post(noticeId)
        } else {
            val notificationId = intent.getStringExtra("notification_id")
                ?: intent.getStringExtra("notificationId")
                ?: intent.getStringExtra("gcm.notification.notification_id")
                ?: intent.getStringExtra("data.notification_id")
            if (!notificationId.isNullOrBlank()) {
                PendingNotices.post("NOTIFICATION_CENTER:$notificationId")
            } else if (extras?.containsKey("notification") == true || extras?.containsKey("google.message_id") == true || intent.getStringExtra("type") != null) {
                PendingNotices.post("NOTIFICATION_CENTER:general")
            }
        }
    }

    override fun onResume() {
        super.onResume()
        AndroidPlatform.currentActivityRef = WeakReference(this)
    }

    override fun onDestroy() {
        if (AndroidPlatform.currentActivityRef?.get() === this) {
            AndroidPlatform.currentActivityRef = null
        }
        super.onDestroy()
    }

    private fun createVillageNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            BodlaFirebaseMessagingService.NOTIFICATION_CHANNEL_ID,
            "ગામની સૂચનાઓ (Village notices)",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "New notices and community updates"
            enableVibration(true)
            setSound(
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun requestNotificationPermission() {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
