package com.bodla.parivar.features.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bodla.parivar.core.di.AppContainer
import com.bodla.parivar.core.localization.AppLanguage
import com.bodla.parivar.core.localization.LocalAppLanguage
import com.bodla.parivar.core.localization.Strings
import com.bodla.parivar.core.theme.*
import com.bodla.parivar.core.ui.*
import com.bodla.parivar.domain.model.Profile
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    profile: Profile?,
    onBack: () -> Unit,
    onUpdated: (Profile) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val lang = LocalAppLanguage.current
    var editName by remember { mutableStateOf(profile?.fullName ?: "") }
    var editPhone by remember { mutableStateOf(profile?.phone ?: "") }
    var editAddress by remember { mutableStateOf(profile?.address ?: "") }
    var profileMessage by remember { mutableStateOf<String?>(null) }

    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordMessage by remember { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = if (lang == AppLanguage.GUJARATI) "પ્રોફાઇલ અપડેટ" else "Edit Profile", style = BodlaPageTitle) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Text(text = "←", fontSize = 22.sp, color = TextPrimary) }
                },
                colors = bodlaTopAppBarColors()
            )
        },
        containerColor = CreamBackground,
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                BodlaCard {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(text = if (lang == AppLanguage.GUJARATI) "પ્રોફાઇલ અપડેટ" else "Update Profile", style = BodlaCardTitle, color = TextPrimary)
                        OutlinedTextField(value = editName, onValueChange = { editName = it }, label = { Text(if (lang == AppLanguage.GUJARATI) "પૂરું નામ" else "Full Name") }, singleLine = true, modifier = Modifier.fillMaxWidth(), colors = bodlaTextFieldColors(), textStyle = BodlaBody.copy(color = TextPrimary))
                        OutlinedTextField(value = editPhone, onValueChange = { editPhone = it }, label = { Text(if (lang == AppLanguage.GUJARATI) "ફોન" else "Phone") }, singleLine = true, modifier = Modifier.fillMaxWidth(), colors = bodlaTextFieldColors(), textStyle = BodlaBody.copy(color = TextPrimary))
                        OutlinedTextField(value = editAddress, onValueChange = { editAddress = it }, label = { Text(if (lang == AppLanguage.GUJARATI) "સરનામું" else "Address") }, modifier = Modifier.fillMaxWidth(), colors = bodlaTextFieldColors(), textStyle = BodlaBody.copy(color = TextPrimary))
                        BodlaButton(
                            text = if (lang == AppLanguage.GUJARATI) "પ્રોફાઇલ સેવ કરો" else "Save Profile",
                            onClick = {
                                if (editName.isBlank()) {
                                    profileMessage = if (lang == AppLanguage.GUJARATI) "નામ જરૂરી છે." else "Name is required."
                                } else {
                                    coroutineScope.launch {
                                        val updated = profile?.copy(fullName = editName.trim(), phone = editPhone.trim().ifBlank { null }, address = editAddress.trim().ifBlank { null }) ?: return@launch
                                            val result = AppContainer.authRepository.updateProfile(updated)
                                            if (result.isSuccess) {
                                                passwordMessage = null
                                                profileMessage = if (lang == AppLanguage.GUJARATI) "પ્રોફાઇલ સફળતાપૂર્વક અપડેટ થયું." else "Profile updated successfully."
                                                onUpdated(updated)
                                            } else {
                                            result.exceptionOrNull()?.message ?: "Failed to update profile."
                                        }
                                    }
                                }
                            },
                            containerColor = SaffronPrimary,
                            contentColor = WarmWhiteSurface
                        )
                        profileMessage?.let { msg -> Text(text = msg, style = BodlaCaption, color = TextSecondary) }
                    }
                }
            }

            item {
                BodlaCard {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(text = if (lang == AppLanguage.GUJARATI) "પાસવર્ડ બદલો" else "Change Password", style = BodlaCardTitle, color = TextPrimary)
                        OutlinedTextField(value = newPassword, onValueChange = { newPassword = it }, label = { Text(if (lang == AppLanguage.GUJARATI) "નવો પાસવર્ડ" else "New Password") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), colors = bodlaTextFieldColors(), textStyle = BodlaBody.copy(color = TextPrimary))
                        OutlinedTextField(value = confirmPassword, onValueChange = { confirmPassword = it }, label = { Text(if (lang == AppLanguage.GUJARATI) "પાસવર્ડ પુષ્ટિ" else "Confirm Password") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), colors = bodlaTextFieldColors(), textStyle = BodlaBody.copy(color = TextPrimary))
                        BodlaButton(
                            text = if (lang == AppLanguage.GUJARATI) "પાસવર્ડ બદલો" else "Update Password",
                            onClick = {
                                if (newPassword.length < 6) {
                                    passwordMessage = if (lang == AppLanguage.GUJARATI) "પાસવર્ડ ઓછામાં ઓછો 6 અક્ષરનો હોવો જોઈએ." else "Password must be at least 6 characters."
                                } else if (newPassword != confirmPassword) {
                                    passwordMessage = if (lang == AppLanguage.GUJARATI) "પાસવર્ડ મેળ ખાતા નથી." else "Passwords do not match."
                                } else {
                                    coroutineScope.launch {
                                        val result = AppContainer.authRepository.updatePassword(newPassword)
                                        if (result.isSuccess) {
                                            passwordMessage = if (lang == AppLanguage.GUJARATI) "પાસવર્ડ સફળતાપૂર્વક બદલાયો." else "Password updated successfully."
                                            newPassword = ""
                                            confirmPassword = ""
                                        } else {
                                            passwordMessage = result.exceptionOrNull()?.message ?: "Failed to update password."
                                        }
                                    }
                                }
                            },
                            containerColor = SaffronPrimary,
                            contentColor = WarmWhiteSurface
                        )
                        passwordMessage?.let { msg -> Text(text = msg, style = BodlaCaption, color = TextSecondary) }
                    }
                }
            }
        }
    }
}