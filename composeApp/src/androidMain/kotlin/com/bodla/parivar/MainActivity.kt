package com.bodla.parivar

import android.Manifest
import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.os.Build
import android.media.RingtoneManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.bodla.parivar.core.AndroidPlatform

class MainActivity : ComponentActivity() {
    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Install the Android 12+ Splash Screen — must be called BEFORE super.onCreate()
        installSplashScreen()
        super.onCreate(savedInstanceState)
        AndroidPlatform.appContext = applicationContext
        AndroidPlatform.pendingNoticeId = intent?.getStringExtra("reference_id")
            ?.takeIf { it.isNotBlank() }
        createVillageNotificationChannel()
        requestNotificationPermissionIfNeeded()

        // Subscribe to global topic for push broadcast notifications
        try {
            com.google.firebase.messaging.FirebaseMessaging.getInstance()
                .subscribeToTopic("all_users")
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        io.github.aakira.napier.Napier.d("Successfully subscribed to FCM topic: all_users", tag = "FCM")
                    } else {
                        io.github.aakira.napier.Napier.w("Failed to subscribe to FCM topic: all_users", task.exception, tag = "FCM")
                    }
                }
        } catch (e: Exception) {
            io.github.aakira.napier.Napier.e("FirebaseMessaging init error", e, tag = "FCM")
        }

        setContent {
            App()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        AndroidPlatform.pendingNoticeId = intent.getStringExtra("reference_id")
            ?.takeIf { it.isNotBlank() }
        if (AndroidPlatform.pendingNoticeId != null) recreate()
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) return

        AlertDialog.Builder(this)
            .setTitle("ગામની સૂચનાઓ (Community notifications)")
            .setMessage("નવી સૂચનાઓનો અવાજ અને અપડેટ મેળવવા મંજૂરી આપો. Allow notifications to hear and see new village updates.")
            .setNegativeButton("પછી (Later)", null)
            .setPositiveButton("મંજૂરી આપો (Allow)") { _, _ ->
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            .show()
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
