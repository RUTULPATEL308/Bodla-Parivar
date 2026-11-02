package com.bodla.parivar.features.design

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.bodla.parivar.core.localization.AppLanguage
import com.bodla.parivar.core.localization.LocalAppLanguage
import com.bodla.parivar.core.ui.GujlishTextField
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoticeDesignScreen(initialData: NoticeDesignData, onBack: () -> Unit, onPreview: (NoticeDesignData) -> Unit, modifier: Modifier = Modifier) {
    val lang = LocalAppLanguage.current
    var data by remember(initialData) { mutableStateOf(initialData) }
    var dateTarget by remember { mutableStateOf<DateTarget?>(null) }
    var timeTarget by remember { mutableStateOf(false) }
    val gu = lang == AppLanguage.GUJARATI
    @Composable fun field(label: String, value: String, keyboardType: KeyboardType = KeyboardType.Text, update: (String) -> Unit) {
        GujlishTextField(value = value, onValueChange = update, label = { Text(label) }, placeholder = { Text(if (gu) "અહીં લખો" else "Enter here") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = keyboardType), modifier = Modifier.fillMaxWidth())
    }
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = onBack) { Text(if (gu) "← પાછા" else "← Back") }
            Text(if (gu) "અંતિમ યાત્રા પોસ્ટ" else "Antim yatra Post", style = MaterialTheme.typography.titleLarge)
        }
        Text(if (gu) "બધી વિગતો ગુજરાતી લિપિમાં લખો" else "Enter each detail in Gujarati script", style = MaterialTheme.typography.bodyMedium)
        field(if (gu) "અહીં નામ લખો" else "Name", data.name) { data = data.copy(name = it) }
        DateField(if (gu) "અવસાનની તારીખ" else "Date of passing", data.dateOfDeath, if (gu) "તારીખ પસંદ કરો" else "Choose date") { dateTarget = DateTarget.DEATH }
        Text(if (data.day.isBlank()) (if (gu) "વાર તારીખ પસંદ કર્યા પછી દેખાશે" else "Day will appear after choosing a date") else "${if (gu) "વાર" else "Day"}: ${data.day}", style = MaterialTheme.typography.bodyMedium)
        DateField(if (gu) "અંતિમ યાત્રાની તારીખ" else "Antim yatra date", data.antimYatraDate, if (gu) "તારીખ પસંદ કરો" else "Choose date") { dateTarget = DateTarget.YATRA }
        DateField(if (gu) "સમય" else "Time", data.antimYatraTime, if (gu) "સમય પસંદ કરો" else "Choose time") { timeTarget = true }
        field(if (gu) "અહીં સરનામું લખો" else "Address", data.address) { data = data.copy(address = it) }
        field(if (gu) "સ્મશાન ઘાટ" else "Cremation ground", data.smashanGhat) { data = data.copy(smashanGhat = it) }
        Text(if (gu) "પરિવારના સભ્યોના નામ" else "Family members", style = MaterialTheme.typography.titleMedium)
        data.familyMembers.forEachIndexed { index, member -> field(if (gu) "સભ્ય ${index + 1}" else "Member ${index + 1}", member) { value -> data = data.copy(familyMembers = data.familyMembers.toMutableList().also { it[index] = value }) } }
        field(if (gu) "મોબાઇલ નંબર" else "Mobile number", data.mobileNumber, KeyboardType.Phone) { data = data.copy(mobileNumber = it.filter(Char::isDigit).take(15)) }
        ImagePickerButton(if (data.photoBytes == null) { if (gu) "ફોટો અપલોડ કરો" else "Upload photo" } else { if (gu) "ફોટો બદલો" else "Change photo" }, Modifier.fillMaxWidth()) { data = data.copy(photoBytes = it) }
        Button(onClick = { onPreview(data) }, modifier = Modifier.fillMaxWidth()) { Text(if (gu) "જુઓ / Preview" else "Preview") }
    }

    dateTarget?.let { target ->
        val state = rememberDatePickerState()
        DatePickerDialog(onDismissRequest = { dateTarget = null }, confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let { millis ->
                    val date = Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.UTC).date
                    val value = "${date.dayOfMonth.toString().padStart(2, '0')}/${date.monthNumber.toString().padStart(2, '0')}/${date.year}"
                    val dayNames = if (gu) listOf("સોમવાર", "મંગળવાર", "બુધવાર", "ગુરુવાર", "શુક્રવાર", "શનિવાર", "રવિવાર") else listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
                    data = if (target == DateTarget.DEATH) data.copy(dateOfDeath = value, day = dayNames[date.dayOfWeek.ordinal]) else data.copy(antimYatraDate = value)
                }
                dateTarget = null
            }) { Text(if (gu) "પસંદ કરો" else "Select") }
        }, dismissButton = { TextButton(onClick = { dateTarget = null }) { Text(if (gu) "રદ કરો" else "Cancel") } }) { DatePicker(state = state) }
    }
    if (timeTarget) {
        val state = rememberTimePickerState()
        AlertDialog(onDismissRequest = { timeTarget = false }, confirmButton = { TextButton(onClick = { data = data.copy(antimYatraTime = "${state.hour.toString().padStart(2, '0')}:${state.minute.toString().padStart(2, '0')}"); timeTarget = false }) { Text(if (gu) "પસંદ કરો" else "Select") } }, dismissButton = { TextButton(onClick = { timeTarget = false }) { Text(if (gu) "રદ કરો" else "Cancel") } }, text = { TimePicker(state = state) }, title = { Text(if (gu) "સમય પસંદ કરો" else "Choose time") })
    }
}

@Composable
private fun DateField(label: String, value: String, actionLabel: String, onClick: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) { Text(value.ifBlank { actionLabel }) }
    }
}

private enum class DateTarget { DEATH, YATRA }
