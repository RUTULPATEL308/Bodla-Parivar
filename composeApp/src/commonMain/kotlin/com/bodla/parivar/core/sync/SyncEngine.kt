package com.bodla.parivar.core.sync

import com.bodla.parivar.core.network.Endpoints
import com.bodla.parivar.core.security.TokenStorage
import com.bodla.parivar.data.remote.dto.AuthResponseDto
import com.bodla.parivar.database.AppDatabase
import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.headers
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.put
import io.ktor.client.statement.bodyAsText

enum class SyncStatus {
    IDLE,
    SYNCING,
    SUCCESS,
    ERROR
}

class SyncEngine(
    private val database: AppDatabase,
    private val httpClient: HttpClient,
    private val tokenStorage: TokenStorage
) {
    private val syncMutex = Mutex()
    private val _syncState = MutableStateFlow(SyncStatus.IDLE)
    val syncState: StateFlow<SyncStatus> = _syncState.asStateFlow()
    private val _lastSyncError = MutableStateFlow<String?>(null)
    val lastSyncError: StateFlow<String?> = _lastSyncError.asStateFlow()

    suspend fun syncAll(): Boolean = withContext(Dispatchers.IO) {
            syncMutex.withLock {

        _syncState.value = SyncStatus.SYNCING
        _lastSyncError.value = null
        var hasFailures = false

        try {
            val pendingItems = database.appDatabaseQueries.selectPendingSyncItems().executeAsList()
            if (pendingItems.isEmpty()) {
                _syncState.value = SyncStatus.IDLE
                return@withLock true
            }

            Napier.d("SyncEngine: Processing ${pendingItems.size} pending mutations")

            for (item in pendingItems) {
                try {
                    val url = when (item.entityType.uppercase()) {
                        "COMPLAINT" -> Endpoints.COMPLAINTS
                        "OFFERING" -> "${Endpoints.REST_BASE_URL}/offerings"
                        "OFFERING_BID" -> Endpoints.OFFERING_BIDS_BASE
                        "PROFILE" -> Endpoints.PROFILES
                        else -> null
                    }

                    if (url == null) {
                        Napier.w("SyncEngine: Unknown entity type ${item.entityType}, removing")
                        database.appDatabaseQueries.deleteSyncItem(item.id)
                        continue
                    }

                    var response = postMutation(url, item.payloadJson, tokenStorage.getToken())
                    if (response.status == HttpStatusCode.Unauthorized) {
                        val refreshedToken = refreshAccessToken()
                        if (refreshedToken != null) {
                            response = postMutation(url, item.payloadJson, refreshedToken)
                        }
                    }

                    if (response.status.isSuccess()) {
                        Napier.i("SyncEngine: Successfully synced item ${item.id}")
                        database.appDatabaseQueries.deleteSyncItem(item.id)
                    } else {
                        val errorBody = response.bodyAsText()
                        val errorMessage = if (response.status == HttpStatusCode.Unauthorized && _lastSyncError.value != null) {
                            _lastSyncError.value!!
                        } else {
                            "HTTP ${response.status.value}: ${summarizeServerError(errorBody)}"
                        }
                        _lastSyncError.value = errorMessage
                        Napier.w("SyncEngine: Failed sync item ${item.id} with status ${response.status}: $errorBody")
                        hasFailures = true
                        val now = Clock.System.now().toString()
                        database.appDatabaseQueries.updateSyncStatus(
                            status = "PENDING_UPLOAD",
                            errorMessage = errorMessage,
                            updatedAt = now,
                            id = item.id
                        )
                    }
                } catch (e: Exception) {
                    Napier.e("SyncEngine: Network error syncing item ${item.id}", e)
                    _lastSyncError.value = e.message ?: "Network error"
                    hasFailures = true
                    val now = Clock.System.now().toString()
                    database.appDatabaseQueries.updateSyncStatus(
                        status = "PENDING_UPLOAD",
                        errorMessage = e.message ?: "Network error",
                        updatedAt = now,
                        id = item.id
                    )
                }
            }

            _syncState.value = if (hasFailures) SyncStatus.ERROR else SyncStatus.SUCCESS
            !hasFailures
        } catch (e: Exception) {
            Napier.e("SyncEngine: Fatal error during syncAll", e)
            _lastSyncError.value = e.message ?: "Sync failed"
            _syncState.value = SyncStatus.ERROR
            false
        } finally {
            if (_syncState.value != SyncStatus.ERROR) {
                _syncState.value = SyncStatus.IDLE
            }
        }
      }
    }

    private suspend fun postMutation(url: String, payload: String, token: String?): HttpResponse =
        httpClient.post(url) {
            contentType(ContentType.Application.Json)
            if (!token.isNullOrBlank()) {
                headers.remove(HttpHeaders.Authorization)
                header(HttpHeaders.Authorization, "Bearer $token")
            }
            header("Prefer", "resolution=merge-duplicates")
            setBody(payload)
        }

    suspend fun refreshAccessToken(): String? {
        val refreshToken = tokenStorage.getRefreshToken()
        if (refreshToken.isNullOrBlank()) {
            _lastSyncError.value = "Your session expired. Sign out and sign in again to restore cloud sync."
            return null
        }
        return try {
            val response = httpClient.post(Endpoints.AUTH_REFRESH) {
                contentType(ContentType.Application.Json)
                headers.remove(HttpHeaders.Authorization)
                header(HttpHeaders.Authorization, "Bearer ${Endpoints.SUPABASE_ANON_KEY}")
                setBody(buildJsonObject { put("refresh_token", refreshToken) }.toString())
            }
            if (!response.status.isSuccess()) {
                val errorBody = response.bodyAsText()
                _lastSyncError.value = "Session refresh failed (HTTP ${response.status.value}). Sign out and sign in again. ${summarizeServerError(errorBody)}"
                Napier.w("SyncEngine: Could not refresh auth token: HTTP ${response.status} $errorBody")
                return null
            }

            val session = response.body<AuthResponseDto>()
            val accessToken = session.accessToken ?: return null
            tokenStorage.saveToken(accessToken)
            session.refreshToken?.let(tokenStorage::saveRefreshToken)
            accessToken
        } catch (e: Exception) {
            _lastSyncError.value = "Session refresh failed. Sign out and sign in again. ${e.message.orEmpty()}"
            Napier.e("SyncEngine: Auth token refresh failed", e)
            null
        }
    }

    private fun summarizeServerError(errorBody: String): String = runCatching {
        val error = Json.parseToJsonElement(errorBody).jsonObject
        val code = error["code"]?.jsonPrimitive?.contentOrNull
        val message = error["message"]?.jsonPrimitive?.contentOrNull
        listOfNotNull(code, message).joinToString(": ").ifBlank { errorBody }
    }.getOrDefault(errorBody)
}
