package com.bodla.parivar.core.network

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

actual class NetworkObserver actual constructor() {
    actual fun isConnected(): Boolean = true
    actual fun observe(): Flow<Boolean> = flowOf(true)
}
