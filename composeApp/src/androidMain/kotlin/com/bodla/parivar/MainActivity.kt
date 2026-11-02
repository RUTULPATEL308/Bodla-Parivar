package com.bodla.parivar

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.media.AudioAttributes
import android.os.Build
import android.media.RingtoneManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.bodla.parivar.core.AndroidPlatform
import com.bodla.parivar.core.PendingNotices
import com.google.firebase.messaging.FirebaseMessaging
import io.github.aakira.napier.Napier
import java.lang.ref.WeakReference

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        AndroidPlatform.appContext = applicationContext
        AndroidPlatform.currentActivityRef = WeakReference(this)
        PendingNotices.post(intent?.getStringExtra("reference_id"))
        createVillageNotificationChannel()

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
        // Publish the target so the running UI can navigate without restarting.
        PendingNotices.post(intent.getStringExtra("reference_id"))
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
            "bodla_parivar_channel",
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
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
