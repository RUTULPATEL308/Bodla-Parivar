package com.bodla.parivar.features.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bodla.parivar.core.di.AppContainer
import com.bodla.parivar.core.localization.AppLanguage
import com.bodla.parivar.core.localization.LocalAppLanguage
import com.bodla.parivar.core.network.Endpoints
import com.bodla.parivar.core.network.KtorClientFactory
import com.bodla.parivar.core.theme.*
import com.bodla.parivar.core.ui.BodlaButton
import com.bodla.parivar.core.ui.BodlaOutlinedButton
import com.bodla.parivar.core.ui.GujlishTextField
import com.bodla.parivar.core.ui.PlatformShrinkPoster
import com.bodla.parivar.features.design.ImagePickerButton
import com.bodla.parivar.features.design.ResultDocument
import com.bodla.parivar.features.design.ResultDocumentPicker
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.github.aakira.napier.Napier
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SendNotificationScreen(
    onBack: () -> Unit,
    onSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lang = LocalAppLanguage.current
    val isGujarati = lang == AppLanguage.GUJARATI
    val coroutineScope = rememberCoroutineScope()

    var titleGu by remember { mutableStateOf("") }
    var bodyGu by remember { mutableStateOf("") }
    var titleEn by remember { mutableStateOf("") }
    var bodyEn by remember { mutableStateOf("") }
    var bodySelection by remember { mutableStateOf(TextRange(0)) }

    var selectedImageBytes by remember { mutableStateOf<ByteArray?>(null) }
    var selectedDocument by remember { mutableStateOf<ResultDocument?>(null) }

    var isSubmitting by remember { mutableStateOf(false) }
    var isImprovingTitle by remember { mutableStateOf(false) }
    var isImprovingBody by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    fun improveWithAi(field: String) {
        val currentText = if (field == "title") titleGu else bodyGu
        if (currentText.isBlank()) {
            errorMessage = if (isGujarati) "લખાણ સુધારવા માટે પહેલા લખાણ દાખલ કરો." else "Please type text to improve."
            return
        }

        coroutineScope.launch {
            if (field == "title") isImprovingTitle = true else isImprovingBody = true
            errorMessage = null
            successMessage = null
            try {
                val token = AppContainer.tokenStorage.getToken()
                val client = KtorClientFactory.create { token as String? }
                val requestBody = buildJsonObject {
                    put("text", currentText.trim())
                    put("type", if (field == "title") "title" else "message")
                }.toString()
                val endpoints = listOf(
                    Endpoints.NOTIFICATION_ENHANCE_ADMIN,
                    Endpoints.NOTIFICATION_ENHANCE
                ).distinct()
                var successfulResponse: io.ktor.client.statement.HttpResponse? = null
                var lastError = ""
                for (endpoint in endpoints) {
                    try {
                        val candidate = client.post(endpoint) {
                            contentType(ContentType.Application.Json)
                            setBody(requestBody)
                        }
                        if (candidate.status.isSuccess()) {
                            successfulResponse = candidate
                            break
                        }
                        lastError = candidate.bodyAsText()
                    } catch (_: Exception) {
                        lastError = if (isGujarati) "AI સેવા અનુપલબ્ધ છે." else "AI service unavailable."
                    }
                }
                val response = successfulResponse ?: error("AI_ENHANCE_FAILED\n$lastError")
                if (response.status.isSuccess()) {
                    val rawResponse = response.bodyAsText()
                    Napier.d("AI raw response: $rawResponse", tag = "SendNotification")
                    val json = Json.parseToJsonElement(rawResponse).jsonObject
                    if (field == "title") {
                        val raw = json["enhanced_gu"]?.jsonPrimitive?.contentOrNull ?: titleGu
                        Napier.d("AI raw enhanced: $raw", tag = "SendNotification")
                        titleGu = stripSalutation(raw, titleGu)
                        Napier.d("AI stripped: $titleGu", tag = "SendNotification")
                        titleEn = json["enhanced_en"]?.jsonPrimitive?.contentOrNull ?: titleEn
                    } else {
                        val raw = json["enhanced_gu"]?.jsonPrimitive?.contentOrNull ?: bodyGu
                        Napier.d("AI raw enhanced: $raw", tag = "SendNotification")
                        bodyGu = stripSalutation(raw, bodyGu)
                        Napier.d("AI stripped: $bodyGu", tag = "SendNotification")
                        bodyEn = json["enhanced_en"]?.jsonPrimitive?.contentOrNull ?: bodyEn
                        titleGu = generateAutoNoticeTitle(bodyGu)
                    }
                    successMessage = if (isGujarati) "AI દ્વારા લખાણ સુધારી દેવાયું!" else "AI enhanced the text successfully!"
                } else {
                    val errBody = runCatching { response.bodyAsText() }.getOrDefault("")
                    Napier.e("AI request failed: ${response.status} $errBody", tag = "SendNotification")
                    errorMessage = if (isGujarati) "AI સુધારો શક્ય થયો નથી. કૃપા કરીને ફરી પ્રયાસ કરો." else "AI enhancement failed. Please try again."
                }
            } catch (exception: Exception) {
                Napier.e("AI enhancement error", exception, tag = "SendNotification")
                errorMessage = if (isGujarati) "AI સેવા ઑફલાઇન છે. ફરી પ્રયાસ કરો." else "AI enhancement service is offline. Please try again."
            }

            if (field == "title") isImprovingTitle = false else isImprovingBody = false
        }
    }

    suspend fun uploadImage(bytes: ByteArray): String {
        return runCatching {
            val token = AppContainer.tokenStorage.getToken() ?: return@runCatching ""
            val client = KtorClientFactory.create { token }
            val authUser = client.get(Endpoints.AUTH_USER).body<JsonObject>()
            val userId = sequenceOf(authUser, authUser["user"]?.let { runCatching { it.jsonObject }.getOrNull() })
                .filterNotNull()
                .flatMap { user -> sequenceOf("id", "authUserId", "auth_user_id", "sub").mapNotNull { key ->
                    user[key]?.jsonPrimitive?.contentOrNull?.takeIf(String::isNotBlank)
                } }
                .firstOrNull() ?: "member"

            val filePath = "$userId/broadcast-${Clock.System.now().toEpochMilliseconds()}-${Random.nextInt().toUInt().toString(16)}.jpg"
            val upload = client.post("${Endpoints.SUPABASE_URL.trimEnd('/')}/storage/v1/object/notices/$filePath") {
                contentType(ContentType.parse("image/jpeg"))
                header("Cache-Control", "max-age=31536000")
                header("x-upsert", "false")
                setBody(PlatformShrinkPoster(bytes))
            }
            if (!upload.status.isSuccess()) {
                val err = runCatching { upload.bodyAsText() }.getOrDefault("")
                Napier.e("Image upload failed: ${upload.status} $err", tag = "SendNotification")
                return@runCatching ""
            }
            "${Endpoints.SUPABASE_URL.trimEnd('/')}/storage/v1/object/public/notices/$filePath"
        }.getOrDefault("")
    }

    suspend fun uploadDocument(doc: ResultDocument): Pair<String, String> {
        return runCatching {
            val token = AppContainer.tokenStorage.getToken() ?: return@runCatching Pair("", "")
            val client = KtorClientFactory.create { token }
            val authUser = client.get(Endpoints.AUTH_USER).body<JsonObject>()
            val userId = sequenceOf(authUser, authUser["user"]?.let { runCatching { it.jsonObject }.getOrNull() })
                .filterNotNull()
                .flatMap { user -> sequenceOf("id", "authUserId", "auth_user_id", "sub").mapNotNull { key ->
                    user[key]?.jsonPrimitive?.contentOrNull?.takeIf(String::isNotBlank)
                } }
                .firstOrNull() ?: "member"

            val ext = doc.name.substringAfterLast('.', "").lowercase().ifBlank { "bin" }
            val filePath = "$userId/broadcast-${Clock.System.now().toEpochMilliseconds()}-${Random.nextInt().toUInt().toString(16)}.$ext"
            val upload = client.post("${Endpoints.SUPABASE_URL.trimEnd('/')}/storage/v1/object/notices/$filePath") {
                contentType(ContentType.parse(doc.contentType.ifBlank { "application/octet-stream" }))
                header("Cache-Control", "max-age=31536000")
                header("x-upsert", "false")
                setBody(doc.bytes)
            }
            if (!upload.status.isSuccess()) {
                return@runCatching Pair("", "")
            }
            val url = "${Endpoints.SUPABASE_URL.trimEnd('/')}/storage/v1/object/public/notices/$filePath"
            Pair(url, doc.name)
        }.getOrDefault(Pair("", ""))
    }

    fun submitBroadcast() {
        if (bodyGu.isBlank()) {
            errorMessage = if (isGujarati) "કૃપા કરીને સંદેશ ભરો." else "Please enter a message."
            return
        }

        coroutineScope.launch {
            isSubmitting = true
            errorMessage = null
            successMessage = null

            try {
                val token = AppContainer.syncEngine.refreshAccessTokenIfStale(AppContainer.tokenStorage.getToken())
                    ?: AppContainer.tokenStorage.getToken()
                    ?: error("Please sign in again.")
                val client = KtorClientFactory.create { token }

                var uploadedImageUrl: String? = null
                var imageUploadFailed = false
                selectedImageBytes?.let { bytes ->
                    val url = uploadImage(bytes)
                    if (url.isNotBlank()) {
                        uploadedImageUrl = url
                    } else {
                        imageUploadFailed = true
                    }
                }

                var uploadedAttachmentUrl: String? = null
                var uploadedAttachmentName: String? = null
                var documentUploadFailed = false
                selectedDocument?.let { doc ->
                    val (url, name) = uploadDocument(doc)
                    if (url.isNotBlank()) {
                        uploadedAttachmentUrl = url
                        uploadedAttachmentName = name
                    } else {
                        documentUploadFailed = true
                    }
                }

                if (imageUploadFailed || documentUploadFailed) {
                    errorMessage = if (isGujarati) "ફાઇલ અપલોડ નિષ્ફળ. ફરી પ્રયાસ કરો." else "File upload failed. Please try again."
                    isSubmitting = false
                    return@launch
                }

                val finalBodyGu = bodyGu.trim()
                val finalTitleGu = generateAutoNoticeTitle(finalBodyGu)
                val finalTitleEn = titleEn.trim().ifBlank { finalTitleGu }
                val finalBodyEn = bodyEn.trim().ifBlank { finalBodyGu }
                val finalNotifTitle = finalTitleGu

                // Post directly to send-notification edge function which handles FCM all_users topic
                var response = client.post("${Endpoints.SUPABASE_URL.trimEnd('/')}/functions/v1/send-notification") {
                    contentType(ContentType.Application.Json)
                    setBody(buildJsonObject {
                        put("title_gu", finalTitleGu)
                        put("title_en", finalTitleEn)
                        put("notification_title_gu", finalNotifTitle)
                        put("notification_title_en", finalNotifTitle)
                        put("body_gu", finalBodyGu)
                        put("body_en", finalBodyEn)
                        put("description_gu", finalBodyGu)
                        put("description_en", finalBodyEn)
                        if (!uploadedImageUrl.isNullOrBlank()) {
                            put("image_url", uploadedImageUrl!!)
                        }
                        if (!uploadedAttachmentUrl.isNullOrBlank()) {
                            put("attachment_url", uploadedAttachmentUrl!!)
                        }
                    }.toString())
                }

                if (response.status.value == 404) {
                    val profileId = runCatching {
                        val authUser = client.get(Endpoints.AUTH_USER).body<JsonObject>()
                        val userObj = authUser["user"]?.jsonObject ?: authUser
                        val authUserId = userObj["id"]?.jsonPrimitive?.contentOrNull
                        if (authUserId.isNullOrBlank()) {
                            null
                        } else {
                            val profiles = client.get("${Endpoints.PROFILES}?auth_user_id=eq.$authUserId&select=id").body<List<JsonObject>>()
                            profiles.firstOrNull()?.get("id")?.jsonPrimitive?.contentOrNull
                        }
                    }.getOrNull()
                    response = client.post("${Endpoints.REST_BASE_URL}/notices") {
                        contentType(ContentType.Application.Json)
                        header("Prefer", "return=representation")
                        setBody(buildJsonObject {
                            put("title_gu", finalTitleGu)
                            put("title_en", finalTitleEn)
                            put("description_gu", finalBodyGu)
                            put("description_en", finalBodyEn)
                            put("status", "PUBLISHED")
                            if (!profileId.isNullOrBlank()) {
                                put("published_by", profileId)
                            }
                            if (!uploadedImageUrl.isNullOrBlank()) {
                                put("image_url", uploadedImageUrl!!)
                            }
                            if (!uploadedAttachmentUrl.isNullOrBlank()) {
                                put("attachment_url", uploadedAttachmentUrl!!)
                            }
                        }.toString())
                    }

                    if (response.status.isSuccess()) {
                        val noticeBody = runCatching { response.bodyAsText() }.getOrDefault("")
                        val noticeObj = runCatching { Json.parseToJsonElement(noticeBody).jsonObject }.getOrNull()
                        val noticeId = noticeObj?.get("id")?.jsonPrimitive?.contentOrNull
                        
                        client.post("${Endpoints.REST_BASE_URL}/notifications") {
                            contentType(ContentType.Application.Json)
                            setBody(buildJsonObject {
                                put("title_gu", finalNotifTitle)
                                put("title_en", finalNotifTitle)
                                put("body_gu", finalBodyGu)
                                put("body_en", finalBodyEn)
                                put("type", "NOTICE")
                                if (!noticeId.isNullOrBlank()) {
                                    put("reference_id", noticeId)
                                }
                                if (!uploadedImageUrl.isNullOrBlank()) {
                                    put("image_url", uploadedImageUrl!!)
                                }
                                if (!uploadedAttachmentUrl.isNullOrBlank()) {
                                    put("attachment_url", uploadedAttachmentUrl!!)
                                }
                            }.toString())
                        }
                    }
                }

                if (!response.status.isSuccess()) {
                    val raw = runCatching { response.bodyAsText() }.getOrDefault("")
                    val msg = when {
                        response.status.value == 401 || raw.contains("JWT expired", ignoreCase = true) ->
                            if (isGujarati) "સેશન સમાપ્ત. ફરી સાઇન ઇન કરો." else "Session expired. Please sign in again."
                        else -> raw.ifBlank { "Failed to broadcast notification (${response.status.value})" }
                    }
                    error(msg)
                }

                successMessage = if (isGujarati) "સૂચના સફળતાપૂર્વક તમામ વપરાશકર્તાઓને મોકલાઈ ગઈ છે!" else "Notice successfully broadcast to all users!"
                kotlinx.coroutines.delay(1200)
                onSuccess()
            } catch (e: Exception) {
                errorMessage = e.message ?: (if (isGujarati) "સૂચના મોકલવામાં ભૂલ આવી." else "Error broadcasting notification.")
            } finally {
                isSubmitting = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (isGujarati) "સીધી સૂચના મોકલો" else "Send Direct Notice",
                            style = BodlaPageTitle
                        )
                        Text(
                            text = if (isGujarati) "તમામ વપરાશકર્તાઓને પુશ નોટિફિકેશન" else "Broadcast to all community members",
                            style = BodlaCaption,
                            color = TextSecondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text(text = "←", fontSize = 22.sp, color = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = WarmWhiteSurface)
            )
        },
        containerColor = CreamBackground,
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Status messages
            errorMessage?.let { err ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = RoseErrorBackground)
                ) {
                    Text(
                        text = err,
                        color = RoseErrorText,
                        style = BodlaBody,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }

            successMessage?.let { msg ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = EmeraldSuccessBackground)
                ) {
                    Text(
                        text = msg,
                        color = EmeraldSuccessText,
                        style = BodlaBody,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }

            // Category is always NOTICE — no selector needed

            // Gujarati Body with Gujlish & AI Improvement
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = WarmWhiteSurface),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(BorderStroke)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isGujarati) "સંદેશ (Gujarati Message Body) *" else "Message (Gujlish / Gujarati) *",
                            style = BodlaCardTitle.copy(fontSize = 14.sp),
                            color = TextPrimary
                        )
                        OutlinedButton(
                            onClick = { improveWithAi("body") },
                            enabled = !isImprovingBody && bodyGu.isNotBlank(),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            if (isImprovingBody) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Enhancing…",
                                    style = BodlaCaption.copy(fontWeight = FontWeight.Bold),
                                    color = TextPrimary
                                )
                            } else {
                                Text(
                                    text = "✨ AI Enhance",
                                    style = BodlaCaption.copy(fontWeight = FontWeight.Bold),
                                    color = TextPrimary
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    GujlishTextField(
                        value = bodyGu,
                        onValueChange = { bodyGu = it },
                        onSelectionChanged = { bodySelection = it },
                        minLines = 3,
                        singleLine = false,
                        placeholder = {
                            Text(
                                text = if (isGujarati) "સંદેશ લખો (દા.ત. badha hajar rehjo...)" else "Type announcement body...",
                                style = BodlaBody,
                                color = TextTertiary
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("B" to "*", "I" to "_", "S" to "~", "</>" to "`").forEach { (label, marker) ->
                            OutlinedButton(
                                onClick = {
                                    val start = bodySelection.start.coerceIn(0, bodyGu.length)
                                    val end = bodySelection.end.coerceIn(0, bodyGu.length)
                                    val first = minOf(start, end)
                                    val last = maxOf(start, end)
                                    val selected = bodyGu.substring(first, last)
                                    val replacement = if (selected.isNotEmpty()) marker + selected + marker else marker + marker
                                    val cursor = if (selected.isNotEmpty()) first + replacement.length else first + marker.length
                                    bodyGu = bodyGu.replaceRange(first, last, replacement)
                                    bodySelection = TextRange(cursor)
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.weight(1f).height(38.dp)
                            ) {
                                Text(
                                    text = label,
                                    style = if (marker == "*") BodlaCaption.copy(fontWeight = FontWeight.Bold)
                                    else if (marker == "_") BodlaCaption.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                                    else if (marker == "~") BodlaCaption.copy(textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough)
                                    else BodlaCaption.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                                    color = TextPrimary
                                )
                            }
                        }
                    }
                }
            }

            // Message design and preview
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = WarmWhiteSurface),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(BorderStroke)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = if (isGujarati) "સંદેશ પ્રિવ્યૂ" else "Message preview",
                        style = BodlaCardTitle.copy(fontSize = 14.sp),
                        color = TextPrimary
                    )
                    if (bodyGu.isNotBlank()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = CreamBackground)
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = if (isGujarati) "પ્રિવ્યૂ" else "Preview",
                                    style = BodlaCaption.copy(fontWeight = FontWeight.Bold),
                                    color = SaffronPrimary
                                )
                                Text(
                                    text = com.bodla.parivar.features.notices.formatNoticeText(
                                        bodyGu.trim()
                                    ),
                                    style = BodlaBody.copy(fontSize = 14.sp),
                                    color = TextPrimary
                                )
                            }
                        }
                    }
                }
            }

            // Photo or Document Attachment
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = WarmWhiteSurface),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(BorderStroke)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (isGujarati) "ફોટો અથવા દસ્તાવેજ (Attach Photo/Poster)" else "Attach Photo or Document",
                        style = BodlaCardTitle.copy(fontSize = 14.sp),
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    ImagePickerButton(
                        label = if (selectedImageBytes == null) {
                            if (isGujarati) "📷 ફોટો / દસ્તાવેજ પસંદ કરો" else "📷 Choose Photo / Image"
                        } else {
                            if (isGujarati) "✓ ફોટો પસંદ થઈ ગયો (બદલવા ક્લિક કરો)" else "✓ Photo Selected (Change)"
                        },
                        modifier = Modifier.fillMaxWidth(),
                        onImagePicked = { selectedImageBytes = it }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    ResultDocumentPicker(
                        label = if (selectedDocument == null) {
                            if (isGujarati) "📄 દસ્તાવેજ પસંદ કરો (PDF/Doc)" else "📄 Choose Document (PDF/Doc)"
                        } else {
                            if (isGujarati) "✓ દસ્તાવેજ પસંદ થયો: ${selectedDocument?.name}" else "✓ Document Selected: ${selectedDocument?.name}"
                        },
                        modifier = Modifier.fillMaxWidth(),
                        onDocumentPicked = { selectedDocument = it }
                    )
                }
            }

            // Optional English Details
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = WarmWhiteSurface),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(BorderStroke)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = if (isGujarati) "અંગ્રેજી વિગતો (Optional English)" else "English Content (Optional)",
                        style = BodlaCardTitle.copy(fontSize = 14.sp),
                        color = TextPrimary
                    )
                    OutlinedTextField(
                        value = bodyEn,
                        onValueChange = { bodyEn = it },
                        placeholder = { Text(if (isGujarati) "અંગ્રેજી સંદેશ" else "English Message", style = BodlaBody) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action Buttons
            BodlaButton(
                text = if (isSubmitting) {
                    if (isGujarati) "મોકલી રહ્યું છે..." else "Sending..."
                } else {
                    if (isGujarati) "તમામ વપરાશકર્તાઓને સૂચના મોકલો" else "Broadcast Notice to Everyone"
                },
                onClick = { submitBroadcast() },
                enabled = !isSubmitting && bodyGu.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            )

            BodlaOutlinedButton(
                text = if (isGujarati) "રદ કરો" else "Cancel",
                onClick = onBack,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

private fun generateAutoNoticeTitle(body: String): String {
    val cleaned = body
        .replace(Regex("[*_~`]"), "")
        .replace(Regex("\\s+"), " ")
        .replace(Regex("^[-•]\\s*"), "")
        .trim()
    if (cleaned.isBlank()) return ""
    val firstSentence = cleaned
        .substringBefore("।")
        .substringBefore(".")
        .substringBefore("!")
        .trim()
    val words = firstSentence.split(" ").filter { it.isNotBlank() }.take(7)
    val candidate = words.joinToString(" ")
    return if (candidate.length > 48) candidate.take(48).trimEnd() + "…" else candidate
}

private fun stripSalutation(enhanced: String, original: String): String {
    val originalTrimmed = original.trim()
    if (enhanced.isBlank()) return originalTrimmed
    // Normalize whitespace for matching
    val normalizedEnhanced = enhanced.replace(Regex("\\s+"), " ").trim()
    val normalizedOriginal = originalTrimmed.replace(Regex("\\s+"), " ").trim()
    var result = enhanced
    // If enhanced contains the original text, extract from there (most reliable)
    if (normalizedEnhanced.contains(normalizedOriginal) && normalizedEnhanced != normalizedOriginal) {
        val idx = normalizedEnhanced.indexOf(normalizedOriginal)
        // Map back to original string position (approximate)
        result = enhanced.substring(enhanced.indexOf(originalTrimmed.split(" ").firstOrNull() ?: originalTrimmed)).trim()
    } else {
        // Strip salutation prefix up to "ke," or "ke "
        val guKe = "\u0A95\u0AC7" // કે
        if (result.contains("$guKe,") || result.contains("$guKe ")) {
            val idx1 = result.indexOf("$guKe,")
            val idx2 = result.indexOf("$guKe ")
            val cutIdx = when {
                idx1 >= 0 && idx2 >= 0 -> minOf(idx1, idx2)
                idx1 >= 0 -> idx1
                else -> idx2
            }
            result = result.substring(cutIdx + guKe.length).trim()
        }
    }
    // Remove leading commas, spaces, asterisks, newlines
    result = result.trimStart(',', ' ', ';', ':', '*', '\n', '\r').trim()
    // Remove trailing signature like "— બોદલા પરિવાર" and everything after "—"
    val sigIdx = result.indexOf("\u2014")
    if (sigIdx >= 0) result = result.substring(0, sigIdx).trim()
    // Remove trailing asterisks
    result = result.trimEnd('*', ' ', '\n', '\r').trim()
    // If result is empty or still contains salutation keywords, use original
    if (result.isEmpty() || result.contains("\u0A9C\u0AA3\u0ABE\u0AB5\u0AB5\u0ABE\u0AA8\u0AC1\u0A82") || result.contains("\u0AB8\u0AB5\u0A87\u0AA8\u0AAF") || result.startsWith("\u0AB8\u0AB5")) {
        return originalTrimmed
    }
    return result
}
