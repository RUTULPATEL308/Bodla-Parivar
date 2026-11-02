package com.bodla.parivar.core

/**
 * Swift-facing bridge. iOS hands notification taps to Kotlin through this
 * object, which forwards the target to the shared [PendingNotices] bus.
 */
object IOSPlatform {
    fun postPendingNoticeId(noticeId: String?) {
        PendingNotices.post(noticeId)
    }
}
