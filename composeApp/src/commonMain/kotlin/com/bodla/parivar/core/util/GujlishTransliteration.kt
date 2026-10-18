package com.bodla.parivar.core.util

import com.bodla.parivar.core.di.AppContainer
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.encodeURLParameter
import io.ktor.http.isSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive

object GujlishTransliteration {
    private val cache = mutableMapOf<String, String>()

    suspend fun transliterateWord(word: String): String = withContext(Dispatchers.IO) {
        val trimmed = word.trim()
        if (trimmed.isEmpty()) return@withContext word
        
        // Return cached translation if present
        cache[trimmed]?.let { return@withContext it }

        // If word contains no English alphabet characters, return as is
        if (!trimmed.any { it in 'a'..'z' || it in 'A'..'Z' }) {
            return@withContext word
        }

        try {
            val encoded = trimmed.encodeURLParameter()
            val url = "https://inputtools.google.com/request?text=$encoded&itc=gu-t-i0-und&num=1&cp=0&cs=1"
            val response = AppContainer.httpClient.get(url) {
                header("Accept", "application/json")
            }

            if (response.status.isSuccess()) {
                val responseText = response.body<String>()
                val jsonElement = Json.parseToJsonElement(responseText)
                val jsonArr = jsonElement.jsonArray
                if (jsonArr.size > 1 && jsonArr[0].jsonPrimitive.content == "SUCCESS") {
                    val candidateGroups = jsonArr[1].jsonArray
                    if (candidateGroups.isNotEmpty()) {
                        val firstGroup = candidateGroups[0].jsonArray
                        if (firstGroup.size > 1) {
                            val matches = firstGroup[1].jsonArray
                            if (matches.isNotEmpty()) {
                                val match = matches[0].jsonPrimitive.content
                                cache[trimmed] = match
                                return@withContext match
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) {
            // Silently fall back to original word on network failure/offline
        }

        return@withContext word
    }
}
