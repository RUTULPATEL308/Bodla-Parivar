package com.bodla.parivar

/**
 * Helper for requesting the POST_NOTIFICATIONS runtime permission on Android 13+.
 * Uses a simple callback pattern; the calling Activity should forward the
 * `onRequestPermissionsResult` to invoke the stored callback.
 */
object PermissionHelper {
    private const val PERMISSION_REQUEST_CODE = 1001

    /** Request permission; `activity` must be a ComponentActivity (or subclass). */
    fun requestPostNotifications(activity: androidx.activity.ComponentActivity, onGranted: () -> Unit) {
        // No runtime permission required on Android versions prior to 13 (TIRAMISU).
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU) {
            onGranted()
            return
        }
        // If permission already granted, invoke callback immediately.
        if (androidx.core.content.ContextCompat.checkSelfPermission(
                activity,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            onGranted()
            return
        }
        // Request permission – result will be received in Activity's onRequestPermissionsResult.
        androidx.core.app.ActivityCompat.requestPermissions(
            activity,
            arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
            PERMISSION_REQUEST_CODE
        )
        // Store callback for later invocation.
        PermissionCallbackHolder.callback = onGranted
    }
}
