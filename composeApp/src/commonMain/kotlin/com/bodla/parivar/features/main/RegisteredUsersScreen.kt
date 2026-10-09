package com.bodla.parivar.features.main

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bodla.parivar.core.localization.AppLanguage
import com.bodla.parivar.core.localization.LocalAppLanguage
import com.bodla.parivar.core.theme.BodlaBody
import com.bodla.parivar.core.theme.BodlaCaption
import com.bodla.parivar.core.theme.BodlaCardTitle
import com.bodla.parivar.core.theme.BodlaPageTitle
import com.bodla.parivar.core.theme.CreamBackground
import com.bodla.parivar.core.theme.TextPrimary
import com.bodla.parivar.core.theme.TextSecondary
import com.bodla.parivar.core.theme.WarmWhiteSurface
import com.bodla.parivar.core.ui.BodlaCard
import com.bodla.parivar.domain.model.Profile
import com.bodla.parivar.domain.repository.AuthRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisteredUsersScreen(
    authRepository: AuthRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lang = LocalAppLanguage.current
    var profiles by remember { mutableStateOf<List<Profile>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(authRepository) {
        authRepository.getRegisteredUsers()
            .onSuccess { profiles = it }
            .onFailure { errorMessage = it.message ?: "Could not load registered users." }
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (lang == AppLanguage.GUJARATI) "નોંધાયેલા સભ્યો" else "Registered users", style = BodlaPageTitle) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("←", color = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = WarmWhiteSurface)
            )
        },
        containerColor = CreamBackground,
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text(
                    text = if (lang == AppLanguage.GUJARATI) "કુલ સભ્યો: ${profiles.size}" else "${profiles.size} registered members",
                    style = BodlaCaption,
                    color = TextSecondary,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            if (isLoading) {
                item { Text(if (lang == AppLanguage.GUJARATI) "સભ્યો લોડ થઈ રહ્યા છે…" else "Loading members…", style = BodlaBody, color = TextSecondary) }
            } else if (errorMessage != null) {
                item { Text(errorMessage.orEmpty(), style = BodlaBody, color = androidx.compose.material3.MaterialTheme.colorScheme.error) }
            } else if (profiles.isEmpty()) {
                item { Text(if (lang == AppLanguage.GUJARATI) "કોઈ નોંધાયેલા સભ્યો નથી." else "No registered users found.", style = BodlaBody, color = TextSecondary) }
            } else {
                items(profiles, key = { it.id }) { profile ->
                    BodlaCard(modifier = Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(profile.fullName, style = BodlaCardTitle, color = TextPrimary)
                            Text(profile.email.orEmpty(), style = BodlaBody, color = TextSecondary)
                            if (!profile.phone.isNullOrBlank()) {
                                Text(profile.phone.orEmpty(), style = BodlaCaption, color = TextSecondary)
                            }
                            Text(profile.status, style = BodlaCaption, color = TextSecondary)
                        }
                    }
                }
            }
        }
    }
}
