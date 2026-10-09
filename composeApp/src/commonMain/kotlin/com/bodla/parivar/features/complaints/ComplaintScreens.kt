package com.bodla.parivar.features.complaints

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bodla.parivar.core.localization.AppLanguage
import com.bodla.parivar.core.localization.LocalAppLanguage
import com.bodla.parivar.core.localization.Strings
import com.bodla.parivar.core.navigation.Screen
import com.bodla.parivar.core.theme.*
import com.bodla.parivar.core.ui.*
import com.bodla.parivar.domain.model.Complaint

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComplaintListScreen(
    onNavigate: (Screen) -> Unit,
    complaints: List<Complaint> = emptyList(),
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null
) {
    val lang = LocalAppLanguage.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = Strings.complaintsTitle(lang), style = BodlaPageTitle) },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Text(text = "←", fontSize = 22.sp, color = TextPrimary)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = WarmWhiteSurface)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onNavigate(Screen.CreateComplaint) },
                containerColor = SaffronPrimary,
                contentColor = WarmWhiteSurface
            ) {
                Text(text = "+", fontSize = 24.sp)
            }
        },
        containerColor = CreamBackground,
        modifier = modifier
    ) { innerPadding ->
        val displayComplaints = if (complaints.isNotEmpty()) complaints else listOf(
            Complaint(
                id = "demo_comp_1",
                userId = "user_1",
                title = "[DEMO DATA] પંચાયત રોડ પર શેરી લાઈટ બંધ છે",
                description = "આ ડેમો ફરિયાદ છે. છેલ્લા બે દિવસથી પંચાયત રોડ પર લાઈટ બંધ હોવાથી અંધારું રહે છે.",
                categoryNameGu = "શેરી લાઈટ",
                categoryNameEn = "Street Lighting",
                status = "IN_PROGRESS",
                createdAt = "2026-09-26"
            )
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(displayComplaints) { complaint ->
                BodlaCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StatusChip(status = complaint.status)
                        DemoDataBadge()
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = complaint.title,
                        style = BodlaCardTitle,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = complaint.description,
                        style = BodlaBody.copy(fontSize = 14.sp),
                        color = TextSecondary,
                        maxLines = 2
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "નોંધણી તારીખ / Date: " + complaint.createdAt,
                        style = BodlaCaption,
                        color = TextTertiary
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateComplaintScreen(
    onBack: () -> Unit,
    onSubmit: (title: String, description: String, category: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val lang = LocalAppLanguage.current
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("પીવાનું પાણી") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = Strings.newComplaint(lang), style = BodlaPageTitle) },
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text(Strings.complaintTitle(lang)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text(Strings.complaintDescription(lang)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 4,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            BodlaButton(
                text = Strings.submit(lang),
                onClick = {
                    onSubmit(title, description, selectedCategory)
                    onBack()
                },
                enabled = title.isNotBlank() && description.isNotBlank()
            )
        }
    }
}
