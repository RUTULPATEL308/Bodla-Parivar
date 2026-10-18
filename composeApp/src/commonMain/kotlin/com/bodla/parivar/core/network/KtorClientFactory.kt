package com.bodla.parivar.core.network

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

object KtorClientFactory {
    fun create(_authTokenProvider: () -> String?): HttpClient {
        return HttpClient {
            install(ContentNegotiation) {
                json(Json {
                    prettyPrint = true
                    isLenient = true
                    ignoreUnknownKeys = true
                    coerceInputValues = true
                })
            }

            defaultRequest {
                if (Endpoints.SUPABASE_ANON_KEY.isNotBlank() && !headers.contains("apikey")) {
                    header("apikey", Endpoints.SUPABASE_ANON_KEY)
                }
                if (!headers.contains(HttpHeaders.Authorization)) {
                    _authTokenProvider()
                        ?.takeIf { it.isNotBlank() }
                        ?.let { header(HttpHeaders.Authorization, "Bearer $it") }
                }
            }
        }
    }
}
