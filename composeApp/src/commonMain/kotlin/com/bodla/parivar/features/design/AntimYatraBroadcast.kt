package com.bodla.parivar.features.design

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
data class AntimYatraPublishResult(
    val success: Boolean = false,
    val notice_created: Boolean = false,
    val notification_created: Boolean = false,
    val push_sent: Boolean = false,
    /** True when the push service is not deployed, so no OS notification was sent. */
    val push_service_missing: Boolean = false,
    val feed_refresh_error: String? = null
)

@Serializable
private data class FunctionErrorResponse(val message: String? = null, val error: String? = null)

/** Title shown on the OS notification and in the in-app notification list. */
private const val NOTIFICATION_TITLE_GU = "અંતિમયાત્રા"
private const val NOTIFICATION_TITLE_EN = "Antimyatra"

/** Notification body – kept intentionally minimal per design requirement. */
private const val NOTIFICATION_BODY_GU = "અંતિમયાત્રા"
private const val NOTIFICATION_BODY_EN = "Antimyatra"

/** Notice title shown in the public Notices feed and notification list. */
private fun noticeTitleGu(data: NoticeDesignData): String {
    val name = data.name.trim()
    return if (name.isBlank()) "અંતિમ યાત્રા" else "અંતિમ યાત્રા: $name".take(80)
}

private fun noticeTitleEn(data: NoticeDesignData): String {
    val name = data.name.trim()
    return if (name.isBlank()) "Antim Yatra" else "Antim Yatra: $name".take(120)
}

/** Publishes an Antim Yatra entry in the app's public Notices feed. */
suspend fun publishAntimYatraNotice(data: NoticeDesignData, accessToken: String, posterPng: ByteArray): AntimYatraPublishResult {
    val client = KtorClientFactory.create { accessToken }
    try {
        // The poster is both the Notices picture and the notification picture,
        // so it is shrunk first to stay within the push image size limit.
        val imageUrl = uploadAntimYatraPoster(client, PlatformShrinkPoster(posterPng))
        val titleGu = noticeTitleGu(data)
        val titleEn = noticeTitleEn(data)
        var pushServiceMissing = false
        var response = client.post("${Endpoints.SUPABASE_URL.trimEnd('/')}/functions/v1/send-notification") {
            contentType(ContentType.Application.Json)
            setBody(buildJsonObject {
                put("title_gu", titleGu)
                put("title_en", titleEn)
                put("notification_title_gu", titleGu)
                put("notification_title_en", titleEn)
                put("image_url", imageUrl)
                put("body_gu", titleGu)
                put("description_gu", titleGu)
                put("body_en", titleEn)
                put("description_en", titleEn)
            }.toString())
        }
        if (response.status.value == 404) {
            pushServiceMissing = true
            val fallbackPayload = buildJsonObject {
                put("title_gu", titleGu)
                put("title_en", titleEn)
                put("notification_title_gu", titleGu)
                put("notification_title_en", titleEn)
                put("image_url", imageUrl)
                put("body_gu", titleGu)
                put("body_en", titleEn)
                put("description_gu", titleGu)
                put("description_en", titleEn)
            }
            response = client.post("${Endpoints.REST_BASE_URL}/rpc/publish_antim_yatra_notice") {
                contentType(ContentType.Application.Json)
                setBody(buildJsonObject { put("p_payload", fallbackPayload) }.toString())
            }
        }
        if (!response.status.isSuccess()) {
            val raw = runCatching { response.bodyAsText() }.getOrDefault("")
            val parsed = runCatching {
                Json { ignoreUnknownKeys = true }.decodeFromString<FunctionErrorResponse>(raw)
            }.getOrNull()
            val detail = parsed?.message ?: parsed?.error ?: raw
            error("Could not publish to Notices (${response.status.value})${detail.takeIf(String::isNotBlank)?.let { ": $it" }.orEmpty()}")
        }
        val body = runCatching { response.bodyAsText() }.getOrDefault("")
        val result = runCatching {
            Json { ignoreUnknownKeys = true }.decodeFromString<AntimYatraPublishResult>(body)
        }.getOrElse {
            error("Notice service returned an invalid response. Please retry.")
        }
        if (!result.success || !result.notice_created || !result.notification_created) {
            error("Notice service did not confirm saving this notice and notification. Please retry.")
        }
        return result.copy(push_service_missing = pushServiceMissing)
    } finally {
        client.close()
    }
}

private suspend fun uploadAntimYatraPoster(client: io.ktor.client.HttpClient, png: ByteArray): String {
    if (png.isEmpty() || png.size > 10 * 1024 * 1024) error("The poster image is empty or too large to upload.")
    val authUser = client.get(Endpoints.AUTH_USER).body<JsonObject>()
    val userId = sequenceOf(authUser, authUser["user"]?.let { runCatching { it.jsonObject }.getOrNull() })
        .filterNotNull()
        .flatMap { user -> sequenceOf("id", "authUserId", "auth_user_id", "sub").mapNotNull { key ->
            user[key]?.jsonPrimitive?.contentOrNull?.takeIf(String::isNotBlank)
        } }
        .firstOrNull()
        ?: error("Could not identify the signed-in account for the poster upload. Please sign in again.")
    val filePath = "$userId/antim-yatra-${Clock.System.now().toEpochMilliseconds()}-${Random.nextInt().toUInt().toString(16)}.png"
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
