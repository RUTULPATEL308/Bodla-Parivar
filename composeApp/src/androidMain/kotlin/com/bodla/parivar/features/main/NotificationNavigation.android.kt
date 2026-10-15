package com.bodla.parivar.features.main

import com.bodla.parivar.core.AndroidPlatform

actual fun consumePendingNoticeId(): String? =
    AndroidPlatform.pendingNoticeId.also { AndroidPlatform.pendingNoticeId = null }
