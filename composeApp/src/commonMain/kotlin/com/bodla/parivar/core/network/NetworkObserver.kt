package com.bodla.parivar.core.network

import kotlinx.coroutines.flow.Flow

expect class NetworkObserver() {
    fun isConnected(): Boolean
    fun observe(): Flow<Boolean>
}
