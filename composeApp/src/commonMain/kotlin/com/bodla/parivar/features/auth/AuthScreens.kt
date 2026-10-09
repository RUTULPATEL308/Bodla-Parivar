package com.bodla.parivar.features.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bodla.parivar.core.localization.AppLanguage
import com.bodla.parivar.core.localization.LocalAppLanguage
import com.bodla.parivar.core.localization.Strings
import com.bodla.parivar.core.navigation.Screen
import com.bodla.parivar.core.theme.*
import com.bodla.parivar.core.ui.BodlaButton
import com.bodla.parivar.core.ui.BodlaCard
import com.bodla.parivar.domain.model.Profile
import com.bodla.parivar.domain.repository.AuthRepository
import com.bodla.parivar.composeapp.generated.resources.Res
import com.bodla.parivar.composeapp.generated.resources.bodla_parivar_logo
import org.jetbrains.compose.resources.painterResource
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    authRepository: AuthRepository,
    onLoginSuccess: (Profile) -> Unit,
    onNavigateRegister: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lang = LocalAppLanguage.current
    val coroutineScope = rememberCoroutineScope()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = if (lang == AppLanguage.GUJARATI) "પ્રવેશ (Login)" else "Login", style = BodlaPageTitle) },
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
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(Res.drawable.bodla_parivar_logo),
                contentDescription = Strings.APP_NAME_GU,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .padding(horizontal = 12.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = if (lang == AppLanguage.GUJARATI) "ગામના સભ્યો માટે ડિજિટલ પોર્ટલ" else "Digital Portal for Bodla Residents",
                style = BodlaCaption,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(32.dp))

            BodlaCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    if (errorMessage != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = errorMessage!!,
                                color = MaterialTheme.colorScheme.error,
                                style = BodlaCaption,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it; errorMessage = null },
                        label = { Text(text = if (lang == AppLanguage.GUJARATI) "ઈમેલ સરનામું (Email)" else "Email Address") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SaffronPrimary,
                            focusedLabelColor = SaffronPrimary
                        )
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it; errorMessage = null },
                        label = { Text(text = if (lang == AppLanguage.GUJARATI) "પાસવર્ડ (Password)" else "Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SaffronPrimary,
                            focusedLabelColor = SaffronPrimary
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    BodlaButton(
                        text = if (isLoading) {
                            if (lang == AppLanguage.GUJARATI) "પ્રવેશ થઈ રહ્યો છે..." else "Logging in..."
                        } else {
                            if (lang == AppLanguage.GUJARATI) "પ્રવેશ કરો" else "Log In"
                        },
                        onClick = {
                            if (email.isBlank() || password.isBlank()) {
                                errorMessage = if (lang == AppLanguage.GUJARATI) "કૃપા કરીને ઈમેલ અને પાસવર્ડ દાખલ કરો." else "Please enter email and password."
                                return@BodlaButton
                            }
                            isLoading = true
                            errorMessage = null
                            coroutineScope.launch {
                                val result = authRepository.login(email, password)
                                isLoading = false
                                result.onSuccess { profile ->
                                    onLoginSuccess(profile)
                                }.onFailure { error ->
                                    errorMessage = error.message ?: if (lang == AppLanguage.GUJARATI) "પ્રવેશ નિષ્ફળ રહ્યો." else "Login failed."
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            TextButton(onClick = onNavigateRegister) {
                Text(
                    text = if (lang == AppLanguage.GUJARATI) "નવા છો? એકાઉન્ટ બનાવો (Register)" else "New user? Create Account",
                    color = SaffronPrimary,
                    style = BodlaSection
                )
            }

        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    authRepository: AuthRepository,
    onRegisterSuccess: (Profile) -> Unit,
    onNavigateLogin: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lang = LocalAppLanguage.current
    val coroutineScope = rememberCoroutineScope()

    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = if (lang == AppLanguage.GUJARATI) "નવી નોંધણી (Register)" else "Register", style = BodlaPageTitle) },
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
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Image(
                painter = painterResource(Res.drawable.bodla_parivar_logo),
                contentDescription = Strings.APP_NAME_GU,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .padding(horizontal = 20.dp)
            )

            Text(
                text = if (lang == AppLanguage.GUJARATI) "બોદલા પરિવાર સાથે જોડાવો" else "Join Bodla Parivar",
                style = BodlaHeaderTitle,
                color = SaffronPrimary
            )
            Text(
                text = if (lang == AppLanguage.GUJARATI) "ગામના વિકાસ અને સેવા માટે આપની વિગતો નોંધાવો" else "Register to connect with village services and community",
                style = BodlaCaption,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(24.dp))

            BodlaCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    if (errorMessage != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = errorMessage!!,
                                color = MaterialTheme.colorScheme.error,
                                style = BodlaCaption,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { fullName = it; errorMessage = null },
                        label = { Text(text = if (lang == AppLanguage.GUJARATI) "પૂરું નામ (Full Name)" else "Full Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SaffronPrimary,
                            focusedLabelColor = SaffronPrimary
                        )
                    )

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it; errorMessage = null },
                        label = { Text(text = if (lang == AppLanguage.GUJARATI) "ઈમેલ (Email)" else "Email") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SaffronPrimary,
                            focusedLabelColor = SaffronPrimary
                        )
                    )

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it; errorMessage = null },
                        label = { Text(text = if (lang == AppLanguage.GUJARATI) "મોબાઇલ નંબર (Phone)" else "Phone Number") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SaffronPrimary,
                            focusedLabelColor = SaffronPrimary
                        )
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it; errorMessage = null },
                        label = { Text(text = if (lang == AppLanguage.GUJARATI) "પાસવર્ડ (Password)" else "Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SaffronPrimary,
                            focusedLabelColor = SaffronPrimary
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    BodlaButton(
                        text = if (isLoading) {
                            if (lang == AppLanguage.GUJARATI) "નોંધણી થઈ રહી છે..." else "Registering..."
                        } else {
                            if (lang == AppLanguage.GUJARATI) "નોંધણી પૂર્ણ કરો" else "Complete Registration"
                        },
                        onClick = {
                            if (fullName.isBlank() || email.isBlank() || password.isBlank()) {
                                errorMessage = if (lang == AppLanguage.GUJARATI) "કૃપા કરીને બધી જરૂરી વિગતો દાખલ કરો." else "Please enter all required fields."
                                return@BodlaButton
                            }
                            isLoading = true
                            errorMessage = null
                            coroutineScope.launch {
                                val result = authRepository.register(
                                    fullName = fullName,
                                    email = email,
                                    password = password,
                                    phone = phone.ifBlank { null }
                                )
                                isLoading = false
                                result.onSuccess { profile ->
                                    onRegisterSuccess(profile)
                                }.onFailure { error ->
                                    errorMessage = error.message ?: if (lang == AppLanguage.GUJARATI) "નોંધણી નિષ્ફળ રહી." else "Registration failed."
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            TextButton(onClick = onNavigateLogin) {
                Text(
                    text = if (lang == AppLanguage.GUJARATI) "પહેલેથી એકાઉન્ટ છે? પ્રવેશ કરો (Login)" else "Already have an account? Log In",
                    color = SaffronPrimary,
                    style = BodlaSection
                )
            }
        }
    }
}
