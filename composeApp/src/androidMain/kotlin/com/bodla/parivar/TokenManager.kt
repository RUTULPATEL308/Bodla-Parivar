package com.bodla.parivar

import android.content.Context
import com.bodla.parivar.core.network.Endpoints
import com.bodla.parivar.core.security.TokenStorage
import com.bodla.parivar.domain.model.Profile
import com.google.firebase.messaging.FirebaseMessaging
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Manages registration, refresh and removal of FCM device tokens in Supabase.
 */
object TokenManager {

    // Deprecated direct Supabase endpoint – now using Vercel proxy for token registration
    private const val VERCEL_REGISTER_TOKEN_ENDPOINT = "https://bodla-parivar.vercel.app/api/notifications/register-token"
    private const val DEVICE_TOKENS_ENDPOINT = "/rest/v1/device_tokens"

    /** Call after user logs in or when the app starts. */
    fun registerIfNeeded(context: Context) {
        val tokenStorage = TokenStorage()
        val isLoggedIn = !tokenStorage.getToken().isNullOrBlank() || !tokenStorage.getUserProfile().isNullOrBlank()
        if (!isLoggedIn) {
            Napier.d("TokenManager: Skipping register, user not logged in", tag = "FCM")
            return
        }

        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Napier.e("FCM token fetch failed", tag = "FCM")
                return@addOnCompleteListener
            }
            val token = task.result ?: return@addOnCompleteListener
            uploadToken(token)
        }
    }

    /** Call when the user logs out – marks token inactive in Supabase. */
    fun unregister(token: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                supabaseDelete(token)
                Napier.d("FCM token removed: $token", tag = "FCM")
            } catch (e: Exception) {
                Napier.e("Failed to delete token: ${e.message}", tag = "FCM")
            }
        }
    }

    /** Called from FirebaseMessagingService when a new token is generated. */
    fun refresh(token: String) {
        val tokenStorage = TokenStorage()
        val isLoggedIn = !tokenStorage.getToken().isNullOrBlank() || !tokenStorage.getUserProfile().isNullOrBlank()
        if (isLoggedIn) {
            uploadToken(token)
        } else {
            Napier.d("TokenManager: Skipping refresh token upload, user not logged in", tag = "FCM")
        }
    }

    // ---------------------------------------------------------------------
    private fun uploadToken(token: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val userJson = TokenStorage().getUserProfile()
                val profile = userJson?.takeIf { it.isNotBlank() }?.let {
                    runCatching { Json.decodeFromString<Profile>(it) }.getOrNull()
                }
                val userId = profile?.id

                val body = Json.encodeToString(
                    mapOf(
                        "user_id" to userId,
                        "token" to token,
                        "platform" to "ANDROID",
                        "active" to true,
                        "updated_at" to kotlinx.datetime.Clock.System.now().toString()
                    )
                )
                val accessToken = TokenStorage().getToken()
                    ?: throw IllegalStateException("No signed-in session for push token registration.")
                vercelPost(body, accessToken)
                Napier.d("FCM token uploaded for user ${userId ?: "guest"} -> $token", tag = "FCM")
            } catch (e: Exception) {
                Napier.e("Error uploading token: ${e.message}", tag = "FCM")
            }
        }
    }

    // ---------------------------------------------------------------------
    private suspend fun vercelPost(jsonBody: String, accessToken: String) {
        val url = java.net.URL(VERCEL_REGISTER_TOKEN_ENDPOINT)
        with(url.openConnection() as java.net.HttpURLConnection) {
            requestMethod = "POST"
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Authorization", "Bearer $accessToken")
            doOutput = true
            outputStream.bufferedWriter().use { it.write(jsonBody) }
            val responseCode = responseCode
            if (responseCode !in 200..299) {
                val errorMsg = runCatching { errorStream?.bufferedReader()?.readText() }.getOrDefault("")
                throw Exception("Vercel token register POST failed with code $responseCode: $errorMsg")
            }
        }
    }

    private suspend fun supabaseDelete(token: String) {
        val url = java.net.URL("${Endpoints.SUPABASE_URL.trimEnd('/')}$DEVICE_TOKENS_ENDPOINT?token=eq.$token")
        with(url.openConnection() as java.net.HttpURLConnection) {
            requestMethod = "DELETE"
            setRequestProperty("apikey", Endpoints.SUPABASE_ANON_KEY)
            setRequestProperty("Authorization", "Bearer ${Endpoints.SUPABASE_ANON_KEY}")
            val responseCode = responseCode
            if (responseCode !in 200..299) {
                throw Exception("Supabase DELETE failed with code $responseCode")
            }
        }
    }
}
