package com.bodla.parivar.core

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import android.os.Build

actual fun openAppSettings() {
    val context = AndroidPlatform.currentActivityRef?.get() ?: AndroidPlatform.appContext
    if (context != null) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
    }
}

actual fun openOverlayPermissionSettings() {
    val context = AndroidPlatform.currentActivityRef?.get() ?: AndroidPlatform.appContext
    if (context != null) {
        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
    }
}

actual fun hasOverlayPermission(): Boolean {
    val context = AndroidPlatform.appContext ?: return true
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        Settings.canDrawOverlays(context)
    } else {
        true
    }
}

actual fun hasNotificationPermission(): Boolean {
    val context = AndroidPlatform.appContext ?: return true
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    } else {
        true
    }
}

actual fun registerPushForUser(isStaff: Boolean) {
    val context = com.bodla.parivar.core.AndroidPlatform.appContext ?: return
    com.bodla.parivar.TokenManager.registerIfNeeded(context)
    com.google.firebase.messaging.FirebaseMessaging.getInstance().subscribeToTopic("all_users")
    if (isStaff) {
        com.google.firebase.messaging.FirebaseMessaging.getInstance().subscribeToTopic("admin_staff")
    } else {
        com.google.firebase.messaging.FirebaseMessaging.getInstance().unsubscribeFromTopic("admin_staff")
    }
}
