package com.bodla.parivar.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Shared bridge for notification taps. Platform code posts the target notice id
 * here and the UI observes it, so a tap works both when the app is cold started
 * and when it is already running in the background.
 */
object PendingNotices {
    private val _pendingNoticeId = MutableStateFlow<String?>(null)
    val pendingNoticeId: StateFlow<String?> = _pendingNoticeId.asStateFlow()

    fun post(noticeId: String?) {
        val id = noticeId?.takeIf { it.isNotBlank() } ?: return
        // Clear first so repeating the same notice id still emits a new value.
        _pendingNoticeId.value = null
        _pendingNoticeId.value = id
    }

    fun peek(): String? = _pendingNoticeId.value

    fun consume(): String? = _pendingNoticeId.value.also { _pendingNoticeId.value = null }
}
