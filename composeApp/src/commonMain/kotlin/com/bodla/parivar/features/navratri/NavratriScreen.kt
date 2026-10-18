package com.bodla.parivar.features.navratri

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import com.bodla.parivar.core.di.AppContainer
import com.bodla.parivar.core.localization.AppLanguage
import com.bodla.parivar.core.localization.LocalAppLanguage
import com.bodla.parivar.core.theme.*
import com.bodla.parivar.core.ui.PrintAction
import com.bodla.parivar.features.notices.NoticeAttachmentPreview
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.coroutines.launch

@Serializable
private data class NavratriBookingField(
    val key: String,
    @SerialName("label_gu") val labelGu: String,
    @SerialName("label_en") val labelEn: String,
    val type: String = "TEXT",
    val required: Boolean = false
)

@Serializable
private data class NavratriOption(
    val id: String,
    val slug: String,
    @SerialName("title_gu") val titleGu: String,
    @SerialName("title_en") val titleEn: String,
    @SerialName("description_gu") val descriptionGu: String? = null,
    @SerialName("description_en") val descriptionEn: String? = null,
    @SerialName("report_url") val reportUrl: String? = null,
    @SerialName("option_type") val optionType: String,
    @SerialName("cash_only") val cashOnly: Boolean = true,
    @SerialName("form_fields") val formFields: List<NavratriBookingField> = emptyList(),
    @SerialName("is_active") val isActive: Boolean = true
)

@Serializable
private data class NavratriBookingAdmin(
    val id: String,
    @SerialName("option_id") val optionId: String,
    @SerialName("customer_name") val customerName: String,
    val phone: String? = null,
    val amount: Double? = null,
    val quantity: Int = 1,
    val notes: String? = null,
    @SerialName("form_data") val formData: Map<String, String> = emptyMap(),
    @SerialName("payment_status") val paymentStatus: String,
    val status: String,
    @SerialName("created_at") val createdAt: String,
    val option: NavratriBookingOption? = null
)

@Serializable
private data class NavratriBookingOption(
    @SerialName("title_gu") val titleGu: String? = null,
    @SerialName("title_en") val titleEn: String? = null
)

@Serializable
private data class NavratriBookingPayload(
    @SerialName("option_id") val optionId: String,
    @SerialName("customer_name") val customerName: String,
    val phone: String? = null,
    val amount: Double? = null,
    val quantity: Int = 1,
    val notes: String? = null,
    @SerialName("payment_method") val paymentMethod: String = "CASH",
    @SerialName("payment_status") val paymentStatus: String = "PENDING",
    val status: String = "PENDING",
    @SerialName("form_data") val formData: JsonObject
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavratriScreen(onBack: () -> Unit, canManage: Boolean = false, modifier: Modifier = Modifier) {
    val lang = LocalAppLanguage.current
    val coroutineScope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current
    var options by remember { mutableStateOf<List<NavratriOption>>(emptyList()) }
    var bookings by remember { mutableStateOf<List<NavratriBookingAdmin>>(emptyList()) }
    var adminView by remember(canManage) { mutableStateOf(canManage) }
    var selected by remember { mutableStateOf<NavratriOption?>(null) }
    var customerName by remember { mutableStateOf("") }
    var formValues by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var loading by remember { mutableStateOf(true) }
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var success by remember { mutableStateOf<String?>(null) }
    var showReportPreview by remember { mutableStateOf(false) }

    LaunchedEffect(canManage) {
        try {
            val optionsUrl = if (canManage) {
                "${com.bodla.parivar.core.network.Endpoints.REST_BASE_URL}/navratri_options?select=*&order=display_order.asc"
            } else {
                com.bodla.parivar.core.network.Endpoints.NAVRATRI_OPTIONS
            }
            val response = AppContainer.httpClient.get(optionsUrl)
            val body = response.bodyAsText()
            if (!response.status.isSuccess()) throw IllegalStateException("HTTP ${response.status.value}: ${body.take(240)}")
            options = Json { ignoreUnknownKeys = true; isLenient = true }.decodeFromString(body)
            if (canManage) {
                val bookingResponse = AppContainer.httpClient.get("${com.bodla.parivar.core.network.Endpoints.NAVRATRI_BOOKINGS}?select=*,option:navratri_options(title_gu,title_en)&order=created_at.desc")
                val bookingBody = bookingResponse.bodyAsText()
                if (!bookingResponse.status.isSuccess()) throw IllegalStateException("HTTP ${bookingResponse.status.value}: ${bookingBody.take(240)}")
                bookings = Json { ignoreUnknownKeys = true; isLenient = true }.decodeFromString(bookingBody)
            }
        } catch (e: Exception) {
            error = e.message ?: "Could not load Navratri options."
        } finally {
            loading = false
        }
    }

    val setOptionActive: (NavratriOption, Boolean) -> Unit = { option, active ->
        coroutineScope.launch {
            try {
                val response = AppContainer.httpClient.patch("${com.bodla.parivar.core.network.Endpoints.REST_BASE_URL}/navratri_options?id=eq.${option.id}") {
                    contentType(ContentType.Application.Json)
                    setBody("{\"is_active\":$active}")
                }
                if (!response.status.isSuccess()) throw IllegalStateException("HTTP ${response.status.value}: ${response.bodyAsText().take(200)}")
                options = options.map { if (it.id == option.id) it.copy(isActive = active) else it }
            } catch (e: Exception) {
                error = e.message ?: "Could not update the Navratri option."
            }
        }
    }

    val updateBooking: (NavratriBookingAdmin, String, String) -> Unit = { booking, column, value ->
        coroutineScope.launch {
            try {
                val endpoint = "${com.bodla.parivar.core.network.Endpoints.NAVRATRI_BOOKINGS}?id=eq.${booking.id}"
                val body = if (column == "payment_status") "{\"payment_status\":\"$value\"}" else "{\"status\":\"$value\"}"
                val response = AppContainer.httpClient.patch(endpoint) {
                    contentType(ContentType.Application.Json)
                    setBody(body)
                }
                if (!response.status.isSuccess()) throw IllegalStateException("HTTP ${response.status.value}: ${response.bodyAsText().take(200)}")
                bookings = bookings.map {
                    if (it.id != booking.id) it else if (column == "payment_status") it.copy(paymentStatus = value) else it.copy(status = value)
                }
            } catch (e: Exception) {
                error = e.message ?: "Could not update the booking."
            }
        }
    }

    val submitBooking: () -> Unit = {
        val option = selected
        if (option != null && !submitting) {
            error = null
            success = null
            val missingRequired = option.formFields.firstOrNull { it.required && formValues[it.key].isNullOrBlank() }
            val phoneValue = formValues["phone"]?.trim().orEmpty()
            val amountValue = formValues["amount"]?.trim()?.toDoubleOrNull()
            when {
                customerName.trim().length < 2 -> error = if (lang == AppLanguage.GUJARATI) "તમારું નામ દાખલ કરો." else "Enter your name."
                option.formFields.any { it.key == "phone" } && phoneValue.length < 7 -> error = if (lang == AppLanguage.GUJARATI) "માન્ય ફોન નંબર દાખલ કરો." else "Enter a valid phone number."
                option.formFields.any { it.key == "amount" } && (amountValue == null || amountValue < 0) -> error = if (lang == AppLanguage.GUJARATI) "માન્ય રકમ દાખલ કરો." else "Enter a valid amount."
                missingRequired != null -> error = if (lang == AppLanguage.GUJARATI) "કૃપા કરીને ${missingRequired.labelGu} ભરો." else "Please enter ${missingRequired.labelEn}."
                else -> {
                    submitting = true
                    val payload = NavratriBookingPayload(
                        optionId = option.id,
                        customerName = customerName.trim(),
                        phone = phoneValue.ifBlank { null },
                        amount = amountValue,
                        formData = buildJsonObject { formValues.forEach { (key, value) -> put(key, value) } }
                    )
                    coroutineScope.launch {
                        try {
                            val response = AppContainer.httpClient.post(com.bodla.parivar.core.network.Endpoints.NAVRATRI_BOOKINGS) {
                                contentType(ContentType.Application.Json)
                                header("Prefer", "return=minimal")
                                setBody(Json.encodeToString(payload))
                            }
                            val body = response.bodyAsText()
                            if (!response.status.isSuccess()) throw IllegalStateException("HTTP ${response.status.value}: ${body.take(240)}")
                            success = if (lang == AppLanguage.GUJARATI) "બુકિંગ વિનંતી નોંધાઈ. ચુકવણી રોકડમાં થશે." else "Booking request submitted. Payment will be collected in cash."
                            selected = null
                            customerName = ""
                            formValues = emptyMap()
                        } catch (e: Exception) {
                            error = e.message ?: "Could not submit the booking."
                        } finally {
                            submitting = false
                        }
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (lang == AppLanguage.GUJARATI) "નવરાત્રી" else "Navratri") },
                navigationIcon = {
                    IconButton(onClick = { if (selected != null) selected = null else onBack() }) {
                        Text("←", fontSize = 22.sp, color = TextPrimary)
                    }
                },
                actions = {
                    if (canManage && adminView && selected == null) {
                        val guBookingReport = bookings.joinToString("\n\n") { booking ->
                            "${booking.customerName}\n${booking.option?.titleGu.orEmpty()}\n${booking.phone.orEmpty()}\n${booking.amount ?: ""}\n${booking.status} / ${booking.paymentStatus}"
                        }
                        val enBookingReport = bookings.joinToString("\n\n") { booking ->
                            "${booking.customerName}\n${booking.option?.titleEn.orEmpty()}\n${booking.phone.orEmpty()}\n${booking.amount ?: ""}\n${booking.status} / ${booking.paymentStatus}"
                        }
                        val bookingReport = bookings.joinToString("\n\n") { booking ->
                            "${booking.customerName}\n${booking.option?.titleGu.orEmpty()} / ${booking.option?.titleEn.orEmpty()}\n${booking.phone.orEmpty()}\n${booking.amount ?: ""}\n${booking.status} / ${booking.paymentStatus}"
                        }
                        PrintAction(
                            title = if (lang == AppLanguage.GUJARATI) "નવરાત્રી બુકિંગ" else "Navratri bookings",
                            content = bookingReport,
                            englishContent = enBookingReport,
                            bothContent = bookingReport
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = WarmWhiteSurface)
            )
        },
        containerColor = CreamBackground,
        modifier = modifier
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(if (lang == AppLanguage.GUJARATI) "નવરાત્રી સેવાઓ" else "Navratri services", style = BodlaPageTitle, color = TextPrimary)
                Text(if (lang == AppLanguage.GUJARATI) "બુકિંગની ચુકવણી રોકડમાં જ સ્વીકારવામાં આવશે." else "Booking payments are handled in cash only.", style = BodlaCaption, color = TextSecondary)
                if (canManage) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = adminView, onClick = { adminView = true; selected = null }, label = { Text(if (lang == AppLanguage.GUJARATI) "વ્યવસ્થાપન" else "Manage") })
                        FilterChip(selected = !adminView, onClick = { adminView = false; selected = null }, label = { Text(if (lang == AppLanguage.GUJARATI) "રહેવાસી દૃશ્ય" else "Resident view") })
                    }
                }
                if (error != null) Text(error.orEmpty(), color = MaterialTheme.colorScheme.error, style = BodlaCaption)
                if (success != null) Text(success.orEmpty(), color = MaterialTheme.colorScheme.primary, style = BodlaCaption)
                if (loading) LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 12.dp))
            }

            if (canManage && adminView && selected == null) {
                item {
                    Text(if (lang == AppLanguage.GUJARATI) "નવરાત્રી વિકલ્પો" else "Navratri options", style = BodlaCardTitle, color = TextPrimary)
                }
                items(options, key = { "option-${it.id}" }) { option ->
                    Card(colors = CardDefaults.cardColors(containerColor = WarmWhiteSurface)) {
                        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(if (lang == AppLanguage.GUJARATI) option.titleGu else option.titleEn, style = BodlaCardTitle, color = TextPrimary)
                                Text(if (option.optionType == "REPORT") if (lang == AppLanguage.GUJARATI) "અહેવાલ" else "Report" else if (lang == AppLanguage.GUJARATI) "રોકડ બુકિંગ" else "Cash booking", style = BodlaCaption, color = TextSecondary)
                            }
                            if (option.optionType == "REPORT" && !option.reportUrl.isNullOrBlank()) {
                                TextButton(onClick = { selected = option }) {
                                    Text(if (lang == AppLanguage.GUJARATI) "જુઓ" else "View")
                                }
                            }
                            Switch(checked = option.isActive, onCheckedChange = { setOptionActive(option, it) })
                        }
                    }
                }
                item {
                    Text(if (lang == AppLanguage.GUJARATI) "બુકિંગ વિનંતીઓ" else "Booking requests", style = BodlaCardTitle, color = TextPrimary)
                }
                if (bookings.isEmpty()) item { Text(if (lang == AppLanguage.GUJARATI) "હજુ કોઈ બુકિંગ નથી." else "No booking requests yet.", style = BodlaBody, color = TextSecondary) }
                items(bookings, key = { "booking-${it.id}" }) { booking ->
                    Card(colors = CardDefaults.cardColors(containerColor = WarmWhiteSurface)) {
                        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(booking.customerName, style = BodlaCardTitle, color = TextPrimary)
                            Text(if (lang == AppLanguage.GUJARATI) booking.option?.titleGu ?: "નવરાત્રી" else booking.option?.titleEn ?: "Navratri", style = BodlaCaption, color = TextSecondary)
                            if (!booking.phone.isNullOrBlank()) Text(booking.phone, style = BodlaBody, color = TextSecondary)
                            if (booking.amount != null) Text("₹${booking.amount}", style = BodlaBody, color = TextPrimary)
                            if (booking.quantity > 1) Text("Qty: ${booking.quantity}", style = BodlaCaption, color = TextSecondary)
                            booking.formData.forEach { (key, value) -> Text("$key: $value", style = BodlaCaption, color = TextSecondary) }
                            if (!booking.notes.isNullOrBlank()) Text(booking.notes, style = BodlaCaption, color = TextSecondary)
                            Text("${booking.status} · ${booking.paymentStatus}", style = BodlaCaption, color = SaffronPrimary)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { updateBooking(booking, "payment_status", if (booking.paymentStatus == "PAID") "PENDING" else "PAID") }) {
                                    Text(if (booking.paymentStatus == "PAID") if (lang == AppLanguage.GUJARATI) "ચૂકવેલ" else "Paid" else if (lang == AppLanguage.GUJARATI) "બાકી · ચૂકવેલ કરો" else "Pending · mark paid")
                                }
                                when (booking.status) {
                                    "PENDING" -> Button(onClick = { updateBooking(booking, "status", "CONFIRMED") }) {
                                        Text(if (lang == AppLanguage.GUJARATI) "મંજૂર કરો" else "Confirm")
                                    }
                                    "CONFIRMED" -> Button(onClick = { updateBooking(booking, "status", "COMPLETED") }) {
                                        Text(if (lang == AppLanguage.GUJARATI) "પૂર્ણ" else "Complete")
                                    }
                                }
                                if (booking.status != "CANCELLED" && booking.status != "COMPLETED") {
                                    TextButton(onClick = { updateBooking(booking, "status", "CANCELLED") }) {
                                        Text(if (lang == AppLanguage.GUJARATI) "રદ કરો" else "Cancel")
                                    }
                                }
                            }
                        }
                    }
                }
            } else if (selected == null) {
                items(if (canManage && !adminView) options.filter { it.isActive } else options) { option ->
                    Card(onClick = {
                        error = null
                        success = null
                        selected = option
                    }, colors = CardDefaults.cardColors(containerColor = WarmWhiteSurface)) {
                        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(if (lang == AppLanguage.GUJARATI) option.titleGu else option.titleEn, style = BodlaCardTitle, color = TextPrimary)
                            val description = if (lang == AppLanguage.GUJARATI) option.descriptionGu else option.descriptionEn
                            if (!description.isNullOrBlank()) Text(description, style = BodlaBody, color = TextSecondary)
                            Text(
                                when {
                                    option.optionType == "REPORT" -> if (lang == AppLanguage.GUJARATI) "અહેવાલ" else "Report"
                                    option.cashOnly -> if (lang == AppLanguage.GUJARATI) "બુકિંગ · રોકડ ચુકવણી" else "Booking · cash payment"
                                    else -> if (lang == AppLanguage.GUJARATI) "બુકિંગ" else "Booking"
                                }, style = BodlaCaption, color = SaffronPrimary
                            )
                        }
                    }
                }
                if (!loading && options.isEmpty() && error == null) item { Text(if (lang == AppLanguage.GUJARATI) "હાલ કોઈ વિકલ્પ ઉપલબ્ધ નથી." else "No Navratri options are available yet.", color = TextSecondary) }
            } else if (selected?.optionType == "REPORT") {
                val report = selected!!
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = WarmWhiteSurface)) {
                        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(if (lang == AppLanguage.GUJARATI) report.titleGu else report.titleEn, style = BodlaCardTitle, color = TextPrimary)
                            Text((if (lang == AppLanguage.GUJARATI) report.descriptionGu else report.descriptionEn).orEmpty(), style = BodlaBody, color = TextSecondary)
                            if (!report.reportUrl.isNullOrBlank()) {
                                OutlinedButton(onClick = { showReportPreview = true }, modifier = Modifier.fillMaxWidth()) {
                                    Text(if (lang == AppLanguage.GUJARATI) "અહેવાલ જુઓ" else "View report")
                                }
                                OutlinedButton(onClick = {
                                    val url = report.reportUrl
                                    uriHandler.openUri(url + if (url.contains('?')) "&download" else "?download")
                                }, modifier = Modifier.fillMaxWidth()) {
                                    Text(if (lang == AppLanguage.GUJARATI) "અહેવાલ ડાઉનલોડ કરો" else "Download report")
                                }
                            }
                        }
                    }
                }
            } else {
                val option = selected!!
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = WarmWhiteSurface)) {
                        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(if (lang == AppLanguage.GUJARATI) option.titleGu else option.titleEn, style = BodlaCardTitle, color = TextPrimary)
                            com.bodla.parivar.core.ui.GujlishTextField(
                                value = customerName,
                                onValueChange = { customerName = it },
                                label = { Text(if (lang == AppLanguage.GUJARATI) "નામ (ગુજલિશ / ગુજરાતી)" else "Name") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            option.formFields.forEach { field ->
                                val label = if (lang == AppLanguage.GUJARATI) field.labelGu else field.labelEn
                                val value = formValues[field.key].orEmpty()
                                if (field.type == "PHONE" || field.type == "NUMBER" || field.type == "AMOUNT") {
                                    OutlinedTextField(
                                        value = value,
                                        onValueChange = { newValue -> formValues = formValues + (field.key to newValue) },
                                        label = { Text(label + if (field.required) " *" else "") },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = when (field.type) {
                                            "PHONE" -> KeyboardType.Phone
                                            "NUMBER" -> KeyboardType.Number
                                            "AMOUNT" -> KeyboardType.Decimal
                                            else -> KeyboardType.Text
                                        })
                                    )
                                } else {
                                    com.bodla.parivar.core.ui.GujlishTextField(
                                        value = value,
                                        onValueChange = { newValue -> formValues = formValues + (field.key to newValue) },
                                        label = { Text(label + if (field.required) " *" else "") },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = field.type != "NOTES",
                                        minLines = if (field.type == "NOTES") 3 else 1
                                    )
                                }
                            }
                            if (option.cashOnly) Text(if (lang == AppLanguage.GUJARATI) "ચુકવણી રોકડમાં જ સ્વીકારવામાં આવશે." else "Payment is cash only.", style = BodlaCaption, color = SaffronPrimary)
                            Button(onClick = submitBooking, enabled = !submitting, modifier = Modifier.fillMaxWidth()) {
                                if (submitting) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                                else Text(if (lang == AppLanguage.GUJARATI) "બુકિંગ મોકલો" else "Submit booking")
                            }
                        }
                    }
                }
            }
        }
    }

    val reportUrl = selected?.takeIf { it.optionType == "REPORT" }?.reportUrl
    if (showReportPreview && !reportUrl.isNullOrBlank()) {
        AlertDialog(
            onDismissRequest = { showReportPreview = false },
            title = { Text(if (lang == AppLanguage.GUJARATI) "અહેવાલ" else "Aheval report") },
            text = {
                NoticeAttachmentPreview(
                    url = reportUrl,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 300.dp, max = 560.dp)
                )
            },
            confirmButton = {
                TextButton(onClick = { showReportPreview = false }) {
                    Text(if (lang == AppLanguage.GUJARATI) "બંધ કરો" else "Close")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    uriHandler.openUri(reportUrl + if (reportUrl.contains('?')) "&download" else "?download")
                }) {
                    Text(if (lang == AppLanguage.GUJARATI) "ડાઉનલોડ કરો" else "Download")
                }
            }
        )
    }
}
