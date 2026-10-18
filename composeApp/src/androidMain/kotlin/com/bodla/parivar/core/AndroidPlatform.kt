package com.bodla.parivar.core

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import java.lang.ref.WeakReference

@SuppressLint("StaticFieldLeak")
object AndroidPlatform {
    var appContext: Context? = null
    var currentActivityRef: WeakReference<Activity>? = null
    var pendingNoticeId: String? = null
}
