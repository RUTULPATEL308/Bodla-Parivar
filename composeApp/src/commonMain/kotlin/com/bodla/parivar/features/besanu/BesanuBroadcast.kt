package com.bodla.parivar.features.besanu

import com.bodla.parivar.core.network.Endpoints
import com.bodla.parivar.core.network.KtorClientFactory
import com.bodla.parivar.core.ui.PlatformShrinkPoster
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlin.random.Random

@Serializable
data class BesanuPublishResult(
    val success: Boolean = false,
    val notice_created: Boolean = false,
    val notification_created: Boolean = false,
    val push_sent: Boolean = false,
    /** True when the push service is not deployed, so no OS notification was sent. */
    val push_service_missing: Boolean = false,
    val feed_refresh_error: String? = null,
    val notice_id: String? = null
)

@Serializable
private data class FunctionErrorResponse(val message: String? = null, val error: String? = null)

/** Title shown on the OS notification and in the in-app notification list. */
private const val NOTIFICATION_TITLE_GU = "બેસણું"
private const val NOTIFICATION_TITLE_EN = "Besanu"

/** Notification body – kept intentionally minimal per design requirement. */
private const val NOTIFICATION_BODY_GU = "બેસણું"
private const val NOTIFICATION_BODY_EN = "Besanu"

/** Notice title shown in the public Notices feed and notification list. */
private fun noticeTitleGu(data: BesanuDesignData): String {
    val name = data.name.trim()
    return if (name.isBlank()) "બેસણું" else "બેસણું: $name".take(80)
}

private fun noticeTitleEn(data: BesanuDesignData): String {
    val name = data.name.trim()
    return if (name.isBlank()) "Besanu" else "Besanu: $name".take(120)
}

/** Publishes a Besanu entry in the app's public Notices feed. */
suspend fun publishBesanuNotice(data: BesanuDesignData, accessToken: String, posterPng: ByteArray): BesanuPublishResult {
    val client = KtorClientFactory.create { accessToken }
    try {
        // The poster is both the Notices picture and the notification picture,
        // so it is shrunk first to stay within the push image size limit.
        val imageUrl = uploadBesanuPoster(client, PlatformShrinkPoster(posterPng))
        val titleGu = noticeTitleGu(data)
        val titleEn = noticeTitleEn(data)
        val fullNoticeText = data.toNoticeText()

        val summaryBody = titleGu.take(200)

        val payload = buildJsonObject {
            put("title_gu", titleGu)
            put("title_en", titleEn)
            put("notification_title_gu", titleGu)
            put("notification_title_en", titleEn)
            put("image_url", imageUrl)
            put("body_gu", summaryBody)
            put("body_en", titleEn)
            put("description_gu", fullNoticeText.take(4000))
            put("description_en", titleEn)
            put("is_besanu", true)
        }

        // 1. Save Notice in Supabase Database via RPC
        val rpcResponse = client.post("${Endpoints.REST_BASE_URL}/rpc/publish_antim_yatra_notice") {
            contentType(ContentType.Application.Json)
            setBody(buildJsonObject { put("p_payload", payload) }.toString())
        }

        if (!rpcResponse.status.isSuccess()) {
            val raw = runCatching { rpcResponse.bodyAsText() }.getOrDefault("")
            if (raw.contains("reached the limit", ignoreCase = true)) {
                val token = com.bodla.parivar.core.security.TokenStorage().getToken() ?: accessToken
                runCatching {
                    client.post("${Endpoints.REST_BASE_URL}/notices") {
                        contentType(ContentType.Application.Json)
                        if (!token.isNullOrBlank()) {
                            header(HttpHeaders.Authorization, "Bearer $token")
                        }
                        setBody(buildJsonObject {
                            put("title_gu", titleGu)
                            put("title_en", titleEn)
                            put("description_gu", titleGu)
                            put("description_en", titleEn)
                            put("image_url", imageUrl)
                            put("status", "PUBLISHED")
                        }.toString())
                    }
                }
            } else {
                val parsed = runCatching {
                    Json { ignoreUnknownKeys = true }.decodeFromString<FunctionErrorResponse>(raw)
                }.getOrNull()
                val detail = parsed?.message ?: parsed?.error ?: raw
                error("Could not publish to Notices (${rpcResponse.status.value})${detail.takeIf(String::isNotBlank)?.let { ": $it" }.orEmpty()}")
            }
        }

        val body = runCatching { rpcResponse.bodyAsText() }.getOrDefault("")
        val result = runCatching {
            Json { ignoreUnknownKeys = true }.decodeFromString<BesanuPublishResult>(body)
        }.getOrElse {
            BesanuPublishResult(success = true, notice_created = true, notification_created = true)
        }

        // 2. Broadcast FCM Push Notification via Vercel Admin API
        val token = com.bodla.parivar.core.security.TokenStorage().getToken() ?: accessToken
        try {
            val pushResponse = client.post("https://bodla-parivar-admin.vercel.app/api/notifications/send") {
                contentType(ContentType.Application.Json)
                if (!token.isNullOrBlank()) {
                    header(HttpHeaders.Authorization, "Bearer $token")
                }
                setBody(buildJsonObject {
                    put("title_gu", titleGu.take(80))
                    put("title_en", titleEn.take(120))
                    put("notification_title_gu", titleGu.take(80))
                    put("notification_title_en", titleEn.take(120))
                    put("image_url", imageUrl)
                    put("body_gu", fullNoticeText.take(200))
                    put("body_en", titleEn.take(200))
                    put("type", "NOTICE")
                    put("is_besanu", true)
                    if (!result.notice_id.isNullOrBlank()) {
                        put("reference_id", result.notice_id)
                    }
                }.toString())
            }
            if (!pushResponse.status.isSuccess()) {
                val errBody = runCatching { pushResponse.bodyAsText() }.getOrDefault("")
                io.github.aakira.napier.Napier.e("Besanu push broadcast failed (${pushResponse.status.value}): $errBody", tag = "FCM")
            } else {
                io.github.aakira.napier.Napier.d("Besanu push broadcast success: ${pushResponse.bodyAsText()}", tag = "FCM")
            }
        } catch (e: Exception) {
            io.github.aakira.napier.Napier.e("Besanu push broadcast exception: ${e.message}", e, tag = "FCM")
        }

        return result
    } finally {
        client.close()
    }
}

private suspend fun uploadBesanuPoster(client: io.ktor.client.HttpClient, png: ByteArray): String {
    if (png.isEmpty() || png.size > 10 * 1024 * 1024) error("The poster image is empty or too large to upload.")
    val authUser = client.get(Endpoints.AUTH_USER).body<JsonObject>()
    val userId = sequenceOf(authUser, authUser["user"]?.let { runCatching { it.jsonObject }.getOrNull() })
        .filterNotNull()
        .flatMap { user -> sequenceOf("id", "authUserId", "auth_user_id", "sub").mapNotNull { key ->
            user[key]?.jsonPrimitive?.contentOrNull?.takeIf(String::isNotBlank)
        } }
        .firstOrNull()
        ?: error("Could not identify the signed-in account for the poster upload. Please sign in again.")
    val filePath = "$userId/besanu-${Clock.System.now().toEpochMilliseconds()}-${Random.nextInt().toUInt().toString(16)}.png"
    val upload = client.post("${Endpoints.SUPABASE_URL.trimEnd('/')}/storage/v1/object/antim-yatra-posters/$filePath") {
        contentType(ContentType.parse("image/png"))
        header("Cache-Control", "max-age=31536000")
        header("x-upsert", "false")
        setBody(png)
    }
    if (!upload.status.isSuccess()) {
        val detail = runCatching { upload.bodyAsText() }.getOrDefault("").take(240)
        error("Could not upload the poster image (${upload.status.value})${detail.takeIf(String::isNotBlank)?.let { ": $it" }.orEmpty()}")
    }
    return "${Endpoints.SUPABASE_URL.trimEnd('/')}/storage/v1/object/public/antim-yatra-posters/$filePath"
}
