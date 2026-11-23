package com.bodla.parivar.features.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.bodla.parivar.core.di.AppContainer
import com.bodla.parivar.core.localization.AppLanguage
import com.bodla.parivar.core.localization.LocalAppLanguage
import com.bodla.parivar.core.network.Endpoints
import com.bodla.parivar.core.theme.BodlaBody
import com.bodla.parivar.core.theme.BodlaCaption
import com.bodla.parivar.core.theme.BodlaCardTitle
import com.bodla.parivar.core.theme.BodlaPageTitle
import com.bodla.parivar.core.theme.CreamBackground
import com.bodla.parivar.core.theme.SaffronPrimary
import com.bodla.parivar.core.theme.TextPrimary
import com.bodla.parivar.core.theme.TextSecondary
import com.bodla.parivar.core.theme.WarmWhiteSurface
import com.bodla.parivar.core.ui.BodlaCard
import com.bodla.parivar.core.ui.PrintAction
import com.bodla.parivar.domain.model.Profile
import com.bodla.parivar.domain.repository.AuthRepository
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Serializable
private data class AdminUsersResponse(
    val users: List<AdminUserDto>,
    @SerialName("canManageUsers") val canManageUsers: Boolean = false
)

@Serializable
private data class AdminUserDto(
    val id: String,
    @SerialName("full_name") val fullName: String,
    val email: String,
    val phone: String? = null,
    val status: String,
    val roles: List<String> = emptyList(),
    @SerialName("feature_permissions") val featurePermissions: List<String> = emptyList()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisteredUsersScreen(
    authRepository: AuthRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lang = LocalAppLanguage.current
    val gu = lang == AppLanguage.GUJARATI
    var profiles by remember { mutableStateOf<List<Profile>>(emptyList()) }
    var grants by remember { mutableStateOf<Map<String, AdminUserDto>>(emptyMap()) }
    var loading by remember { mutableStateOf(true) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var isAdmin by remember { mutableStateOf(false) }
    var canManageUsers by remember { mutableStateOf(false) }
    val adminApiBase = remember { Endpoints.ADMIN_API_BASE_URL.ifBlank { "https://bodla-parivar.vercel.app" }.trimEnd('/') }
    val apiJson = remember { Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true } }
    var editing by remember { mutableStateOf<Profile?>(null) }
    var deleting by remember { mutableStateOf<Profile?>(null) }
    var editName by remember { mutableStateOf("") }
    var editEmail by remember { mutableStateOf("") }
    var editPhone by remember { mutableStateOf("") }
    var editStatus by remember { mutableStateOf("ACTIVE") }
    var editRoles by remember { mutableStateOf(setOf("USER")) }
    var editFeatures by remember { mutableStateOf(setOf<String>()) }
    var isSuperAdmin by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    suspend fun reload() {
        loading = true
        message = null
        val current = authRepository.getCachedCurrentUser()
        isAdmin = current?.isAdmin == true || current?.roles.orEmpty().any { it.equals("ADMIN", true) || it.equals("SUPER_ADMIN", true) }
        isSuperAdmin = current?.roles.orEmpty().any { it.equals("SUPER_ADMIN", true) }
        // Render admin actions from the locally cached role immediately; the API
        // response below will confirm whether management is configured.
        canManageUsers = isAdmin
        coroutineScope {
            val membersRequest = async { authRepository.getRegisteredUsers() }
            val adminDetailsRequest = async {
                if (!isAdmin) return@async
                runCatching {
                    val apiResponse = AppContainer.httpClient.get("$adminApiBase/api/admin/users")
                    if (!apiResponse.status.isSuccess()) error(apiResponse.bodyAsText().ifBlank { "User management API (${apiResponse.status.description})" })
                    val payload = apiJson.decodeFromString<AdminUsersResponse>(apiResponse.bodyAsText())
                    canManageUsers = payload.canManageUsers
                    grants = payload.users.associateBy { it.id }
                }.onFailure {
                    canManageUsers = false
                    message = it.message ?: "Could not load user details."
                }
            }
            membersRequest.await()
                .onSuccess { profiles = it }
                .onFailure { message = it.message ?: "Could not load members." }
            adminDetailsRequest.await()
        }
        loading = false
    }
    LaunchedEffect(authRepository) { reload() }

    fun openEditor(profile: Profile) {
        val user = grants[profile.id]
        editing = profile
        editName = user?.fullName ?: profile.fullName
        editEmail = user?.email ?: profile.email.orEmpty()
        editPhone = user?.phone ?: profile.phone.orEmpty()
        editStatus = user?.status ?: profile.status
        editRoles = user?.roles?.toSet()?.takeIf { it.isNotEmpty() } ?: setOf("USER")
        editFeatures = user?.featurePermissions?.toSet() ?: emptySet()
        message = null
    }

    fun saveUser() {
        val profile = editing ?: return
        val base = adminApiBase
        if (editName.isBlank() || editEmail.isBlank()) {
            message = if (gu) "નામ અને ઈમેલ જરૂરી છે." else "Name and email are required."
            return
        }
        scope.launch {
            busy = true
            runCatching {
                val current = grants[profile.id]
                val roles = editRoles.toList().ifEmpty { listOf("USER") }
                val features = editFeatures.toList()
                val response = AppContainer.httpClient.patch("$base/api/admin/users") {
                    contentType(ContentType.Application.Json)
                    setBody(Json.encodeToString(buildJsonObject {
                        put("id", profile.id); put("full_name", editName.trim()); put("email", editEmail.trim())
                        put("phone", editPhone.trim()); put("status", editStatus)
                        put("roles", kotlinx.serialization.json.JsonArray(roles.map { kotlinx.serialization.json.JsonPrimitive(it) }))
                        put("feature_permissions", kotlinx.serialization.json.JsonArray(features.map { kotlinx.serialization.json.JsonPrimitive(it) }))
                    }).toString())
                }
                if (!response.status.isSuccess()) error(response.bodyAsText().ifBlank { "Update failed (${response.status.description})" })
                editing = null
                message = if (gu) "સભ્યની વિગતો સાચવાઈ." else "Member details updated."
                reload()
            }.onFailure { message = it.message ?: if (gu) "વિગતો અપડેટ થઈ નથી." else "Could not update member." }
            busy = false
        }
    }

    fun deleteUser(profile: Profile) {
        val base = adminApiBase
        scope.launch {
            busy = true
            runCatching {
                val response = AppContainer.httpClient.delete("$base/api/admin/users") {
                    contentType(ContentType.Application.Json)
                    setBody(buildJsonObject { put("id", profile.id) }.toString())
                }
                if (!response.status.isSuccess()) error(response.bodyAsText().ifBlank { "Delete failed (${response.status.description})" })
                deleting = null
                message = if (gu) "સભ્યનું એકાઉન્ટ ડિલીટ થયું." else "Member account deleted."
                reload()
            }.onFailure { message = it.message ?: if (gu) "સભ્ય ડિલીટ થયો નથી." else "Could not delete member." }
            busy = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (gu) "નોંધાયેલા સભ્યો" else "Registered users", style = BodlaPageTitle) },
                actions = { PrintAction(title = if (gu) "સભ્યો" else "Members", content = profiles.joinToString("\n\n") { "${it.fullName}\n${it.email.orEmpty()}\n${it.phone.orEmpty()}\n${it.status}" }) },
                navigationIcon = { IconButton(onClick = onBack) { Text("←", color = TextPrimary) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = WarmWhiteSurface)
            )
        },
        containerColor = CreamBackground,
        modifier = modifier
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text(if (gu) "કુલ સભ્યો: ${profiles.size}" else "${profiles.size} registered members", style = BodlaCaption, color = TextSecondary)
                if (isAdmin && !canManageUsers) Text(if (gu) "User edit/delete માટે Admin API configure કરવી જરૂરી છે." else "Configure Admin API URL to enable user edit and delete.", style = BodlaCaption, color = TextSecondary)
                message?.let { Text(it, style = BodlaCaption, color = if (it.contains("failed", true) || it.contains("નથી")) MaterialTheme.colorScheme.error else TextSecondary) }
                if (loading) Text(if (gu) "સભ્યો લોડ થઈ રહ્યા છે…" else "Loading members…", style = BodlaBody, color = TextSecondary)
            }
            items(profiles, key = { it.id }) { profile ->
                val user = grants[profile.id]
                BodlaCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(profile.fullName, style = BodlaCardTitle, color = TextPrimary)
                        Text(profile.email.orEmpty(), style = BodlaBody, color = TextSecondary)
                        if (!profile.phone.isNullOrBlank()) Text(profile.phone.orEmpty(), style = BodlaCaption, color = TextSecondary)
                        Text(user?.status ?: profile.status, style = BodlaCaption, color = TextSecondary)
                        if (isAdmin) {
                            if (canManageUsers) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { openEditor(profile) }, modifier = Modifier.weight(1f)) { Text(if (gu) "સંપાદિત" else "Edit") }
                                OutlinedButton(onClick = { deleting = profile }, modifier = Modifier.weight(1f)) { Text(if (gu) "ડિલીટ" else "Delete", color = MaterialTheme.colorScheme.error) }
                            }
                        }
                    }
                }
            }
        }
    }

    editing?.let { profile ->
        AlertDialog(
            onDismissRequest = { if (!busy) editing = null },
            title = { Text(if (gu) "સભ્ય સંપાદિત કરો" else "Edit member", color = MaterialTheme.colorScheme.onSurface) },
            text = {
                    Column(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 520.dp).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                    val fieldColors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        cursorColor = MaterialTheme.colorScheme.primary
                    )
                    val switchColors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                        checkedBorderColor = MaterialTheme.colorScheme.primary,
                        uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                        uncheckedBorderColor = MaterialTheme.colorScheme.outline
                    )
                    OutlinedTextField(editName, { editName = it }, label = { Text(if (gu) "નામ" else "Name") }, singleLine = true, colors = fieldColors)
                    OutlinedTextField(editEmail, { editEmail = it }, label = { Text("Email") }, singleLine = true, colors = fieldColors)
                    OutlinedTextField(editPhone, { editPhone = it }, label = { Text(if (gu) "મોબાઇલ નંબર" else "Phone") }, singleLine = true, colors = fieldColors)
                    Text(if (gu) "એકાઉન્ટ સ્થિતિ" else "Account status", style = BodlaCaption, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("ACTIVE", "SUSPENDED").forEach { status ->
                            if (editStatus == status) Button(onClick = { editStatus = status }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)) { Text(if (status == "ACTIVE") if (gu) "ચાલુ" else "Active" else if (gu) "બંધ" else "Suspended") }
                            else OutlinedButton(onClick = { editStatus = status }) { Text(if (status == "ACTIVE") if (gu) "ચાલુ" else "Active" else if (gu) "બંધ" else "Suspended", color = MaterialTheme.colorScheme.onSurface) }
                        }
                    }
                    Text(if (gu) "ભૂમિકા" else "Roles", style = BodlaCardTitle, color = MaterialTheme.colorScheme.onSurface)
                    val roleLabels = listOf(
                        "USER" to if (gu) "સભ્ય" else "Resident member",
                        "BUSINESS_OWNER" to if (gu) "વ્યવસાય માલિક" else "Business owner",
                        "CONTENT_MANAGER" to if (gu) "સામગ્રી વ્યવસ્થાપક" else "Content manager",
                        "MODERATOR" to if (gu) "મોડરેટર" else "Moderator",
                        "ADMIN" to if (gu) "એડમિન" else "Administrator"
                    ) + if (isSuperAdmin) listOf("SUPER_ADMIN" to if (gu) "સુપર એડમિન" else "Super administrator") else emptyList()
                    roleLabels.forEach { (role, label) ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(label, style = BodlaBody, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                            Switch(
                                checked = role in editRoles,
                                colors = switchColors,
                                onCheckedChange = { checked ->
                                    editRoles = if (checked) editRoles + role else editRoles - role
                                    if (editRoles.isEmpty()) editRoles = setOf("USER")
                                }
                            )
                        }
                    }
                    Text(if (gu) "સુવિધા પરવાનગીઓ" else "Feature access", style = BodlaCardTitle, color = MaterialTheme.colorScheme.onSurface)
                    val featureLabels = listOf(
                        "members" to if (gu) "સભ્યો" else "Members",
                        "notices" to if (gu) "સૂચનાઓ" else "Notices",
                        "offerings" to if (gu) "ચઢાવો" else "Offerings",
                        "navratri" to if (gu) "નવરાત્રી" else "Navratri",
                        "complaints" to if (gu) "ફરિયાદો" else "Complaints",
                        "emergency_contacts" to if (gu) "તાત્કાલિક સંપર્ક" else "Emergency contacts",
                        "village_history" to if (gu) "ગામનો ઇતિહાસ" else "Village history",
                        "events" to if (gu) "કાર્યક્રમો" else "Events",
                        "directory" to if (gu) "ડિરેક્ટરી" else "Directory",
                        "gallery" to if (gu) "ગેલેરી" else "Gallery",
                        "notifications" to if (gu) "સૂચના મોકલવી" else "Notifications"
                    )
                    featureLabels.forEach { (feature, label) ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(label, style = BodlaBody, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                            Switch(checked = feature in editFeatures, colors = switchColors, onCheckedChange = { checked ->
                                editFeatures = if (checked) editFeatures + feature else editFeatures - feature
                            })
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { saveUser() }, enabled = !busy) { Text(if (gu) "સાચવો" else "Save", color = MaterialTheme.colorScheme.primary) } },
            dismissButton = { TextButton(onClick = { editing = null }, enabled = !busy) { Text(if (gu) "રદ કરો" else "Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant) } }
        )
    }
    deleting?.let { profile ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text(if (gu) "સભ્ય ડિલીટ કરવો?" else "Delete member?") },
            text = { Text(if (gu) "${profile.fullName}નું એકાઉન્ટ કાયમ માટે ડિલીટ થશે." else "${profile.fullName}'s account will be permanently deleted.") },
            confirmButton = { TextButton(onClick = { deleteUser(profile) }, enabled = !busy) { Text(if (gu) "ડિલીટ" else "Delete", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { deleting = null }, enabled = !busy) { Text(if (gu) "રદ કરો" else "Cancel") } }
        )
    }
}
