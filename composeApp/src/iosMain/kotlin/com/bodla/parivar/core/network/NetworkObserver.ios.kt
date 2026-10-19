package com.bodla.parivar.core.network

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import platform.Network.nw_path_get_status
import platform.Network.nw_path_monitor_cancel
import platform.Network.nw_path_monitor_create
import platform.Network.nw_path_monitor_set_queue
import platform.Network.nw_path_monitor_set_update_handler
import platform.Network.nw_path_monitor_start
import platform.Network.nw_path_status_satisfied
import platform.darwin.dispatch_get_main_queue

actual class NetworkObserver actual constructor() {
    private var lastKnownStatus: Boolean = true

    actual fun isConnected(): Boolean = lastKnownStatus

    actual fun observe(): Flow<Boolean> = callbackFlow {
        val monitor = nw_path_monitor_create()
        val queue = dispatch_get_main_queue()

        nw_path_monitor_set_update_handler(monitor) { path ->
            val status = nw_path_get_status(path)
            val isSatisfied = status == nw_path_status_satisfied
            lastKnownStatus = isSatisfied
            trySend(isSatisfied)
        }

        if (monitor != null && queue != null) {
            nw_path_monitor_set_queue(monitor, queue)
            nw_path_monitor_start(monitor)
        } else {
            trySend(true)
        }

        awaitClose {
            if (monitor != null) {
                nw_path_monitor_cancel(monitor)
            }
        }
    }.distinctUntilChanged()
}
