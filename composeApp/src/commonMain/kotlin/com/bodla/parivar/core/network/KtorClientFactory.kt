package com.bodla.parivar.core.network

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.header
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

object KtorClientFactory {
    fun create(authTokenProvider: () -> String?): HttpClient {
        return HttpClient {
            install(ContentNegotiation) {
                json(Json {
                    prettyPrint = true
                    isLenient = true
                    ignoreUnknownKeys = true
                    coerceInputValues = true
                })
            }

            install(Logging) {
                level = LogLevel.INFO
            }

            defaultRequest {
                if (Endpoints.SUPABASE_ANON_KEY.isNotBlank() && !headers.contains("apikey")) {
                    header("apikey", Endpoints.SUPABASE_ANON_KEY)
                }
                
                if (!headers.contains("Authorization")) {
                    val token = authTokenProvider()
                    if (!token.isNullOrBlank()) {
                        header("Authorization", "Bearer $token")
                    } else if (Endpoints.SUPABASE_ANON_KEY.isNotBlank()) {
                        header("Authorization", "Bearer ${Endpoints.SUPABASE_ANON_KEY}")
                    }
                }
            }
        }
    }
}
