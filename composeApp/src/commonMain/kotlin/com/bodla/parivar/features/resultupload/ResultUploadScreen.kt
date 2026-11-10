package com.bodla.parivar.features.resultupload

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bodla.parivar.core.navigation.Screen
import com.bodla.parivar.core.theme.*
import com.bodla.parivar.core.ui.BodlaCard
import com.bodla.parivar.core.ui.BodlaButton
import com.bodla.parivar.core.ui.GujlishTextField
import com.bodla.parivar.core.ui.PlatformOpenUrl
import com.bodla.parivar.core.ui.PlatformShareFile
import com.bodla.parivar.domain.repository.ResultUploadRepository
import com.bodla.parivar.features.design.ResultDocument
import com.bodla.parivar.features.design.ResultDocumentPicker
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultUploadScreen(repository: ResultUploadRepository, canManage: Boolean, onBack: () -> Unit, onSubmissionSuccess: () -> Unit = onBack, modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    var student by remember { mutableStateOf("") }
    var standard by remember { mutableStateOf("") }
    var school by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<ResultDocument?>(null) }
    var enabled by remember { mutableStateOf<Boolean?>(null) }
    var sending by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var failure by remember { mutableStateOf(false) }
    var submissions by remember { mutableStateOf<List<com.bodla.parivar.domain.repository.ResultUpload>>(emptyList()) }
    var loadingSubmissions by remember { mutableStateOf(false) }
    var savingSetting by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (canManage) {
            loadingSubmissions = true
            repository.isEnabled().onSuccess { enabled = it }.onFailure { message = it.message; failure = true }
            repository.getSubmissions().onSuccess { submissions = it }.onFailure {
                message = it.message ?: "સબમિશન લોડ થઈ શક્યાં નથી."
                failure = true
            }
            loadingSubmissions = false
        } else {
            repository.isEnabled().onSuccess { enabled = it }.onFailure {
                enabled = false
                message = "કનેક્શન તપાસી શકાતું નથી. ફરી પ્રયાસ કરો."
                failure = true
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("પરિણામ અપલોડ", style = BodlaPageTitle, color = TextPrimary) }, navigationIcon = {
                IconButton(onClick = onBack) { Text("←", fontSize = 22.sp, color = TextPrimary) }
            }, colors = bodlaTopAppBarColors())
        },
        containerColor = CreamBackground,
        modifier = modifier
    ) { padding ->
        if (canManage) {
            LazyColumn(Modifier.fillMaxSize().padding(padding).padding(horizontal = 12.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    Row(Modifier.fillMaxWidth().heightIn(min = 42.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("પરિણામ અપલોડ", style = BodlaCardTitle, color = TextPrimary)
                            Text(if (enabled == true) "ચાલુ" else "બંધ", style = BodlaCaption, color = TextSecondary)
                        }
                        Switch(
                            checked = enabled == true,
                            onCheckedChange = { next ->
                                savingSetting = true
                                scope.launch {
                                    repository.setEnabled(next)
                                        .onSuccess { enabled = it; message = null; failure = false }
                                        .onFailure { message = it.message; failure = true }
                                    savingSetting = false
                                }
                            },
                            enabled = enabled != null && !savingSetting
                        )
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = {
                            loadingSubmissions = true
                            scope.launch {
                                repository.getSubmissions().onSuccess { submissions = it; message = null; failure = false }
                                    .onFailure { message = it.message; failure = true }
                                loadingSubmissions = false
                            }
                        }, enabled = !loadingSubmissions, modifier = Modifier.weight(1f).height(38.dp), contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)) { Text("Refresh", style = BodlaCaption) }
                        Button(onClick = {
                            val csv = buildString {
                                appendLine(listOf("Student Name", "Standard", "School Name", "Submitted By", "Phone", "File", "Submitted At").joinToString(",") { it.csvCell() })
                                submissions.forEach { row ->
                                    appendLine(listOf(row.studentName, row.standard, row.schoolName, row.submittedBy, row.submitterPhone.orEmpty(), row.fileName, row.createdAt).joinToString(",") { it.csvCell() })
                                }
                            }
                            PlatformShareFile(csv, "result-uploads.csv", "text/csv")
                        }, enabled = submissions.isNotEmpty(), modifier = Modifier.weight(1f).height(38.dp), contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)) { Text("Download Excel", style = BodlaCaption) }
                    }
                    message?.let { Text(it, color = if (failure) MaterialTheme.colorScheme.error else LeafGreenSecondary, style = BodlaBody) }
                }
                if (loadingSubmissions) item { CircularProgressIndicator(color = SaffronPrimary) }
                if (!loadingSubmissions && submissions.isEmpty()) item { Text(message ?: "હજુ કોઈ પરિણામ સબમિટ થયું નથી.", style = BodlaBody, color = TextSecondary) }
                items(submissions) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = WarmWhiteSurface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderStroke))
                    ) {
                        Column(Modifier.padding(horizontal = 12.dp, vertical = 9.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                Text(item.studentName, style = BodlaCardTitle, color = TextPrimary, maxLines = 1, modifier = Modifier.weight(1f))
                                Text("ધોરણ ${item.standard}", style = BodlaCaption, color = SaffronPrimary)
                            }
                            Text("શાળા: ${item.schoolName}", style = BodlaCaption, color = TextSecondary, maxLines = 1)
                            Text("મોકલનાર: ${item.submittedBy}  •  ${item.createdAt.take(10)}", style = BodlaCaption, color = TextTertiary, maxLines = 1)
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                Text(item.fileName, style = BodlaCaption, color = TextTertiary, maxLines = 1, modifier = Modifier.weight(1f))
                                TextButton(onClick = {
                            scope.launch {
                                repository.getSignedFileUrl(item.filePath)
                                    .onSuccess { PlatformOpenUrl(it) }
                                    .onFailure { message = it.message; failure = true }
                            }
                                }, contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp), modifier = Modifier.height(32.dp)) { Text("દસ્તાવેજ જુઓ", style = BodlaCaption) }
                            }
                        }
                    }
                }
            }
        } else Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            when (enabled) {
                null -> CircularProgressIndicator(color = SaffronPrimary)
                false -> Text("હાલમાં પરિણામ અપલોડ બંધ છે.", style = BodlaBody, color = TextSecondary)
                true -> {
                    GujlishTextField(student, { student = it }, label = { Text("વિદ્યાર્થીનું નામ") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(12.dp))
                    GujlishTextField(standard, { standard = it }, label = { Text("ધોરણ") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(12.dp))
                    GujlishTextField(school, { school = it }, label = { Text("શાળાનું નામ") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(12.dp))
                    ResultDocumentPicker("છબી અથવા PDF પસંદ કરો", Modifier.fillMaxWidth()) { selected = it }
                    selected?.let { Text("પસંદ કરેલી ફાઇલ: ${it.name}", style = BodlaBody, color = TextSecondary) }
                    message?.let { Text(it, color = if (failure) MaterialTheme.colorScheme.error else LeafGreenSecondary, style = BodlaBody) }
                    BodlaButton(
                        text = if (sending) "મોકલાઈ રહ્યું છે…" else "પરિણામ મોકલો",
                        onClick = {
                            val file = selected
                            if (file != null) {
                                sending = true
                                message = null
                                scope.launch {
                                    repository.submit(student, standard, school, file.name, file.contentType, file.bytes)
                                        .onSuccess { onSubmissionSuccess() }
                                        .onFailure { message = it.message ?: "મોકલી શકાયું નથી. ફરી પ્રયાસ કરો."; failure = true }
                                    sending = false
                                }
                            }
                        },
                        enabled = !sending && student.isNotBlank() && standard.isNotBlank() && school.isNotBlank() && selected != null
                    )
                }
            }
        }
    }
}

private fun String.csvCell(): String = "\"${replace("\"", "\"\"")}\""
