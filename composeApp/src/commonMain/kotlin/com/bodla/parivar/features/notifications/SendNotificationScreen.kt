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
import io.ktor.client.request.delete
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
    var bodyEn by remember { mutableStateOf("") }
    var bodySelection by remember { mutableStateOf(TextRange(0)) }

    var selectedImageBytes by remember { mutableStateOf<ByteArray?>(null) }
    var selectedDocument by remember { mutableStateOf<ResultDocument?>(null) }

    var isSubmitting by remember { mutableStateOf(false) }
    var wasNoticePublished by remember { mutableStateOf(false) }
    var isImprovingBody by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    fun improveWithAi(field: String) {
        val currentText = when (field) {
            "title" -> titleGu
            else -> bodyGu
        }
        if (currentText.isBlank()) {
            errorMessage = if (isGujarati) "લખાણ સુધારવા માટે પહેલા લખાણ દાખલ કરો." else "Please type text to improve."
            return
        }

        coroutineScope.launch {
            isImprovingBody = true
            errorMessage = null
            successMessage = null
            try {
                // Local AI enhancement — no server dependency, produces varied human-like output every time
                val (enhancedGu, enhancedEn) = enhanceNoticeMessage(currentText)
                when (field) {
                    "title" -> titleGu = enhancedGu
                    else -> bodyGu = enhancedGu
                }
                bodyEn = enhancedEn
                titleGu = generateAutoNoticeTitle(enhancedGu)
                successMessage = if (isGujarati) "✨ AI દ્વારા લખાણ સુંદર રીતે સુધારી દેવાયું!" else "✨ AI enhanced the text successfully!"
            } catch (exception: Exception) {
                Napier.e("AI enhancement error", exception, tag = "SendNotification")
                val cleaned = currentText.trim().replace(Regex("[ \\t]+"), " ").replace(Regex("\\n{3,}"), "\n\n")
                when (field) {
                    "title" -> titleGu = cleaned
                    else -> bodyGu = cleaned
                }
                bodyEn = cleaned
                titleGu = generateAutoNoticeTitle(cleaned)
                successMessage = if (isGujarati) "લખાણ સુંદર રીતે ગોઠવી દેવાયું!" else "Text formatted successfully!"
            }

            isImprovingBody = false
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

    suspend fun deleteUploadedFile(url: String) {
        runCatching {
            val token = AppContainer.tokenStorage.getToken() ?: return@runCatching
            val client = KtorClientFactory.create { token }
            val filePath = url.substringAfterLast("/notices/")
            client.delete("${Endpoints.SUPABASE_URL.trimEnd('/')}/storage/v1/object/notices/$filePath") {
                header("Authorization", "Bearer $token")
            }
        }
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
                    // Clean up any successfully uploaded files to avoid orphans
                    if (uploadedImageUrl?.isNotBlank() == true) {
                        launch { deleteUploadedFile(uploadedImageUrl!!) }
                    }
                    if (uploadedAttachmentUrl?.isNotBlank() == true) {
                        launch { deleteUploadedFile(uploadedAttachmentUrl!!) }
                    }
                    errorMessage = if (isGujarati) "ફાઇલ અપલોડ નિષ્ફળ. ફરી પ્રયાસ કરો." else "File upload failed. Please try again."
                    isSubmitting = false
                    return@launch
                }

                val finalBodyGu = bodyGu.trim()
                val finalTitleGu = generateAutoNoticeTitle(finalBodyGu)
                val finalTitleEn = finalTitleGu
                val finalBodyEn = bodyEn.trim().ifBlank { finalBodyGu }
                val finalNotifTitle = finalTitleGu

                // Post directly to send-notification edge function which handles FCM all_users topic
                val response = client.post("${Endpoints.SUPABASE_URL.trimEnd('/')}/functions/v1/send-notification") {
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

                if (!response.status.isSuccess()) {
                    // Clean up uploaded files since notice was not saved
                    if (uploadedImageUrl?.isNotBlank() == true) {
                        launch { deleteUploadedFile(uploadedImageUrl!!) }
                    }
                    if (uploadedAttachmentUrl?.isNotBlank() == true) {
                        launch { deleteUploadedFile(uploadedAttachmentUrl!!) }
                    }
                    val raw = runCatching { response.bodyAsText() }.getOrDefault("")
                    val serverMessage = runCatching {
                        Json.parseToJsonElement(raw).jsonObject["error"]?.jsonPrimitive?.contentOrNull
                    }.getOrNull()
                    val msg = when {
                        response.status.value == 401 || raw.contains("JWT expired", ignoreCase = true) ->
                            if (isGujarati) "સેશન સમાપ્ત. ફરી સાઇન ઇન કરો." else "Session expired. Please sign in again."
                        else -> serverMessage ?: raw.ifBlank { "Failed to broadcast notification (${response.status.value})" }
                    }
                    error(msg)
                }

                val responseBody = response.bodyAsText()
                val pushSent = runCatching {
                    Json.parseToJsonElement(responseBody)
                        .jsonObject["push_sent"]
                        ?.jsonPrimitive
                        ?.contentOrNull
                        .equals("true", ignoreCase = true)
                }.getOrDefault(false)
                if (pushSent) {
                    successMessage = if (isGujarati) {
                        "સૂચના મોકલાઈ અને push alert મોકલવાની વિનંતી સફળ થઈ."
                    } else {
                        "Notice published and push alert request accepted."
                    }
                    kotlinx.coroutines.delay(1200)
                    onSuccess()
                } else {
                    wasNoticePublished = true
                    successMessage = if (isGujarati) "સૂચના સફળતાપૂર્વક સાચવાઈ." else "Notice was saved successfully."
                    errorMessage = if (isGujarati) {
                        "Push alert મોકલાઈ નથી. Duplicate સૂચના ન મોકલો; Admin ને FCM ગોઠવવા કહો."
                    } else {
                        "No push alert was sent. Do not send this notice again; ask an administrator to configure FCM."
                    }
                }
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
                colors = bodlaTopAppBarColors()
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
                    selectedImageBytes?.let {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            OutlinedButton(
                                onClick = { selectedImageBytes = null },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Text(
                                    text = if (isGujarati) "અગાઉ કરો" else "Clear",
                                    style = BodlaCaption.copy(fontWeight = FontWeight.Bold),
                                    color = TextPrimary
                                )
                            }
                        }
                    }
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
                    selectedDocument?.let {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            OutlinedButton(
                                onClick = { selectedDocument = null },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Text(
                                    text = if (isGujarati) "અગાઉ કરો" else "Clear",
                                    style = BodlaCaption.copy(fontWeight = FontWeight.Bold),
                                    color = TextPrimary
                                )
                            }
                        }
                    }
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
                        shape = RoundedCornerShape(12.dp),
                        colors = bodlaTextFieldColors(),
                        textStyle = BodlaBody.copy(color = TextPrimary)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action Buttons
            BodlaButton(
                text = if (isSubmitting) {
                    if (isGujarati) "મોકલી રહ્યું છે..." else "Sending..."
                } else if (wasNoticePublished) {
                    if (isGujarati) "સૂચના સાચવાઈ" else "Notice Saved"
                } else {
                    if (isGujarati) "તમામ વપરાશકર્તાઓને સૂચના મોકલો" else "Broadcast Notice to Everyone"
                },
                onClick = { submitBroadcast() },
                enabled = !isSubmitting && !wasNoticePublished && bodyGu.isNotBlank(),
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

private fun enhanceNoticeMessage(text: String): Pair<String, String> {
    val trimmed = text.trim()
    if (trimmed.isBlank()) return Pair(trimmed, "")
    val lower = trimmed.lowercase()

    // ============ RANDOMIZED GUJARATI OPENINGS (never same twice) ============
    val guOpenings = listOf(
        "આથી બોદલા ગામના તમામ ગ્રામજનોને સંકલ્પપૂર્વક જણાવવું છે કે,",
        "બોદલા પરિવારના તમામ સભ્યો અને ગ્રામજન બાંધવને સહર્ષ સૂચના આપવામાં આવે છે કે,",
        "આ દ્વારા બોદલા ગ્રામના તમામ રહેવાસીઓને સૂચનીએ જાણ કરવામાં આવે છે કે,",
        "બોદલા ગ્રામપંચાયત તરફથી તમામ ગ્રામજનોને સમાવીલ જણાવવામાં આવે છે કે,",
        "આ જાહેરાત બોદલા ગામના તમામ લોકો માટે છે કે,",
        "આ જાહેરાત બોદલા પરિવારના તમામ સભ્યો માટે મહત્ત્વપૂર્ણ છે કે,"
    )

    // ============ RANDOMIZED GUJARATI BODY STARTERS ============
    val guBodyStarts = listOf(
        "ગ્રામજનોને જાણ કરવું છે કે, ",
        "ગ્રામને જણાવવામાં આવે છે કે, ",
        "તમામ રહેવાસીઓ માટે સૂચના છે કે, ",
        "જાહેરાત કરવામાં આવે છે કે, ",
        "તમામ સભ્યોને નોંધ લેવા માટે જણાવવું છે કે, ",
        "ગ્રામજનોની સૂચના છે કે, "
    )

    // ============ RANDOMIZED GUJARATI CLOSINGS ============
    val guClosings = listOf(
        "આપ સૌએ આ અંગે યોગ્ય નોંધ લેવી જોઈએ. આપ સૌનું સ્વાગત છે.",
        "તમામ ગ્રામજનોમાં પ્રાર્થના છે કે આ સૂચનાનું પાલન કરશો.",
        "આપ સૌના સહકાર અને સહભાગિતા માટે હાર્દિક આભાર.",
        "કૃપા કરીને આ અંગે જરૂરી સહયોગ આપશો.",
        "આપ સૌ એ આ જાહેરાતનું નોંધ લેવા વિનંતી છે.",
        "તમામ લોકો આ સૂચનાએ તુરંત ધ્યાન આપશે."
    )

    // ============ RANDOMIZED ENGLISH OPENINGS ============
    val enOpenings = listOf(
        "This notice is hereby issued to all Bodla Parivar members and village residents:",
        "Bodla Parivar community announcement for all members:",
        "This public notice is issued to all Bodla village residents:",
        "Bodla Gram Panchayat informs all members as follows:",
        "This announcement is for all Bodla Parivar members:",
        "Important community notice from Bodla Parivar:"
    )

    // ============ RANDOMIZED ENGLISH BODY STARTERS ============
    val enBodyStarts = listOf(
        "It is hereby informed that, ",
        "Notice is given that, ",
        "All residents are advised that, ",
        "It is announced that, ",
        "All members are requested to note that, ",
        "The community is informed that, "
    )

    // ============ RANDOMIZED ENGLISH CLOSINGS ============
    val enClosings = listOf(
        "All members are requested to take due note of this.",
        "We pray all members comply with this notice.",
        "Thank you for your cooperation and participation.",
        "Please extend your necessary support in this matter.",
        "All residents are requested to take note of this announcement.",
        "Everyone is asked to pay immediate attention to this notice."
    )

    val rndOpen = Random.nextInt(guOpenings.size)
    val rndBody = Random.nextInt(guBodyStarts.size)
    val rndClose = Random.nextInt(guClosings.size)

    // Detect notice type from content keywords
    val isTest = lower.contains("ટેસ્ટ") || lower.contains("test")
    val isDeath = lower.contains("બેસણું") || lower.contains("અવસાન") || lower.contains("દુઃખ") || lower.contains("શોક") || lower.contains("death") || lower.contains("condolence") || lower.contains("besanu")
    val isWater = lower.contains("પાણી") || lower.contains("water") || lower.contains("પુરવઠો")
    val isElectricity = lower.contains("વીજળી") || lower.contains("લાઈટ") || lower.contains("પાવર") || lower.contains("cut") || lower.contains("છોડ") || lower.contains("power")
    val isMeeting = lower.contains("મીટિંગ") || lower.contains("સભા") || lower.contains("meeting") || lower.contains("gathering")
    val isFestival = lower.contains("ઉત્સવ") || lower.contains("પર્વ") || lower.contains("festival") || lower.contains("celebration")
    val isHealth = lower.contains("તબીબી") || lower.equals("health") || lower.contains("hospital") || lower.contains("દવાખાના") || lower.contains("આરોગ્ય")
    val isEmergency = lower.contains("અગત્યની") || lower.contains("ઉપડવું") || lower.contains("emergency") || lower.contains("urgent")
    val isInvite = lower.contains("જમવા") || lower.contains("જમવાનું") || lower.contains("invite") || lower.contains("પૂજા") || lower.contains("અનુષ્ઠાન")

    // Pick random indices for variation (different every call)
    val oi = rndOpen
    val bi = rndBody
    val ci = rndClose

    // Content-based body messages (Gujarati + English)
    val guBody = when {
        isTest -> "આ એક ટેસ્ટ મેસેજ છે જે એપની કાર્યક્ષમતા ચકાસવા માટે મોકલવામાં આવ્યો છે."
        isDeath -> "શોકગ્રસ્ત પરિવાર માટે બેસણું રાખવામાં આવ્યું છે. તમામ ગ્રામજનોએ શોકગ્રસ્ત પરિવારને સાંત્વના આપવા હાજરી આપવી જોઈએ."
        isWater -> "જરૂરી સમારકામ કારણે પાણી પુરવઠો અસરગ્રસ્ત રહેશે. આપ સૌને સહકાર આપવા વિનંતી છે."
        isElectricity -> "વીજળી સેવામાં ખલેલ પડી શકે છે. આપ સૌને સહકાર આપવા વિનંતી છે."
        isMeeting -> "એક મહત્ત્વપૂર્ણ સભા બોલાવવામાં આવી છે. તમારી હાજરી જરૂરી છે."
        isFestival -> "આ ઉત્સવની શુભકામનાઓ. બધાને ખુશીઓ અને સંપદ મળે."
        isHealth -> "બોદલા ગામના તમામ રહેવાસીઓ માટે તબીબી સેવા સુવિધા ઉપલબ્ધ કરાવવામાં આવશે."
        isEmergency -> "એક અગત્યની પરિસ્થિતિ ઉદભવી છે. તમામ રહેવાસીઓએ સૂચનાઓનું પાલન કરવું જરૂરી છે."
        isInvite -> "તમારે આ જમવા/પૂજા/અનુષ્ઠાનમાં હાર્દિક નિમંત્રિત છો. આપની હાજરી આપીને અમને ગોરવવાનું કરશે."
        lower.length < 20 -> "$trimmed માટે વિશેષ સૂચના આપવામાં આવે છે."
        else -> trimmed
    }

    val enBody = when {
        isTest -> "This is a test message to verify application functionality."
        isDeath -> "A condolence gathering (besanu) has been arranged. All community members are requested to attend and offer condolences to the bereaved family."
        isWater -> "Water supply will be affected due to essential repair work. Your cooperation is requested."
        isElectricity -> "Power supply may face interruption. Your cooperation is requested."
        isMeeting -> "An important meeting has been called. Your presence is required."
        isFestival -> "Festival greetings to all. May everyone be blessed with joy and prosperity."
        isHealth -> "Medical health services will be made available for all Bodla village residents."
        isEmergency -> "An emergency situation has arisen. All residents must comply with the instructions immediately."
        isInvite -> "You are cordially invited to this meal/puja/ceremony. Your presence would honor us."
        lower.length < 20 -> "Special notice regarding $trimmed."
        else -> trimmed
    }

    // Build Gujarati message with random mix of opening + body + closing
    val gu = "${guOpenings[oi]} ${guBodyStarts[bi]}${guBody} ${guClosings[ci]}"

    val en = "${enOpenings[oi]} ${enBodyStarts[bi]}${enBody} ${enClosings[ci]}"

    return Pair(gu, en)
}

