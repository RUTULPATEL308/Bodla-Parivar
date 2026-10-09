package com.bodla.parivar.features.emergency

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bodla.parivar.core.localization.AppLanguage
import com.bodla.parivar.core.localization.LocalAppLanguage
import com.bodla.parivar.core.localization.Strings
import com.bodla.parivar.core.theme.*
import com.bodla.parivar.core.ui.*
import com.bodla.parivar.domain.model.EmergencyContact

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmergencyScreen(
    onBack: () -> Unit,
    emergencyContacts: List<EmergencyContact> = emptyList(),
    modifier: Modifier = Modifier
) {
    val lang = LocalAppLanguage.current

    val officialHelplines = if (emergencyContacts.isNotEmpty()) emergencyContacts else listOf(
        EmergencyContact(
            id = "em_1",
            nameGu = "ઈમરજન્સી એમ્બ્યુલન્સ (૧૦૮)",
            nameEn = "Emergency Ambulance (108)",
            organization = "108 GVK EMRI",
            phone = "108",
            category = "AMBULANCE",
            displayOrder = 1
        ),
        EmergencyContact(
            id = "em_2",
            nameGu = "પોલીસ કંટ્રોલ રૂમ (૧૦૦ / ૧૧૨)",
            nameEn = "Police Emergency (100 / 112)",
            organization = "Gujarat Police",
            phone = "112",
            alternatePhone = "100",
            category = "POLICE",
            displayOrder = 2
        ),
        EmergencyContact(
            id = "em_3",
            nameGu = "ફાયર બ્રિગેડ (૧૦૧)",
            nameEn = "Fire Emergency (101)",
            organization = "Fire Services Mehsana",
            phone = "101",
            category = "FIRE",
            displayOrder = 3
        ),
        EmergencyContact(
            id = "em_4",
            nameGu = "અભયમ મહિલા હેલ્પલાઇન (૧૮૧)",
            nameEn = "Abhayam Women Helpline (181)",
            organization = "Abhayam 181",
            phone = "181",
            category = "HELPLINE",
            displayOrder = 4
        ),
        EmergencyContact(
            id = "em_5",
            nameGu = "ચાઇલ્ડલાઇન હેલ્પલાઇન (૧૦૯૮)",
            nameEn = "Child Helpline (1098)",
            organization = "Childline India",
            phone = "1098",
            category = "HELPLINE",
            displayOrder = 5
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = Strings.emergencyTitle(lang), style = BodlaPageTitle) },
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
        ) {
            // Notice banner explaining verified numbers
            Surface(
                color = StatusError.copy(alpha = 0.1f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "🚨 ", fontSize = 20.sp)
                    Text(
                        text = Strings.emergencyNotice(lang),
                        style = BodlaButton.copy(fontSize = 14.sp),
                        color = StatusError
                    )
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(officialHelplines) { contact ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(CardCornerRadius),
                        colors = CardDefaults.cardColors(containerColor = WarmWhiteSurface),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = androidx.compose.ui.graphics.SolidColor(BorderStroke)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (lang == AppLanguage.GUJARATI) contact.nameGu else contact.nameEn,
                                    style = BodlaCardTitle,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = contact.organization.orEmpty(),
                                    style = BodlaCaption,
                                    color = TextSecondary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "📞 " + contact.phone,
                                    style = BodlaPageTitle.copy(fontSize = 20.sp),
                                    color = StatusError
                                )
                            }

                            // Large Call Button (Accessible touch target >= 48dp)
                            Button(
                                onClick = { /* Intent to dial contact.phone */ },
                                shape = RoundedCornerShape(ButtonCornerRadius),
                                colors = ButtonDefaults.buttonColors(containerColor = StatusError),
                                modifier = Modifier
                                    .height(52.dp)
                                    .padding(start = 8.dp)
                            ) {
                                Text(
                                    text = Strings.callNow(lang),
                                    style = BodlaButton,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
