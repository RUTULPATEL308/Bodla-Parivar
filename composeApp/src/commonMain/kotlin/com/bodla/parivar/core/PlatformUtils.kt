package com.bodla.parivar.core

expect fun openAppSettings()
expect fun openOverlayPermissionSettings()
expect fun hasOverlayPermission(): Boolean
expect fun hasNotificationPermission(): Boolean
expect fun registerPushForUser(isStaff: Boolean)
