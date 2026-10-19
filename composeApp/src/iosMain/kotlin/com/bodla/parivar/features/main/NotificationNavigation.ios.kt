package com.bodla.parivar.features.main

import com.bodla.parivar.core.IOSPlatform

actual fun consumePendingNoticeId(): String? =
    IOSPlatform.pendingNoticeId.also { IOSPlatform.pendingNoticeId = null }
