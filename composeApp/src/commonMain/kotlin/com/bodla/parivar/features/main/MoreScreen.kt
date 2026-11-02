package com.bodla.parivar.features.main

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bodla.parivar.core.localization.AppLanguage
import com.bodla.parivar.core.localization.LocalAppLanguage
import com.bodla.parivar.core.localization.Strings
import com.bodla.parivar.core.navigation.Screen

@Composable
fun MoreScreen(onNavigate: (Screen) -> Unit, modifier: Modifier = Modifier, canManageResults: Boolean = false) {
    val gu = LocalAppLanguage.current == AppLanguage.GUJARATI
    Column(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(Strings.more(LocalAppLanguage.current), style = MaterialTheme.typography.headlineMedium)
        Card(modifier = Modifier.fillMaxWidth().clickable { onNavigate(Screen.ResultUpload) }, shape = RoundedCornerShape(18.dp)) {
            Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("🎓", style = MaterialTheme.typography.headlineMedium)
                Column {
                    Text(if (canManageResults) "પરિણામ સબમિશન" else "પરિણામ અપલોડ", style = MaterialTheme.typography.titleMedium)
                    Text(if (canManageResults) "વિદ્યાર્થીઓના સબમિશન જુઓ" else "વિદ્યાર્થીની વિગતો અને પરિણામ મોકલો", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        Card(modifier = Modifier.fillMaxWidth().clickable { onNavigate(Screen.NoticeDesign) }, shape = RoundedCornerShape(18.dp)) {
            Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("🕯️", style = MaterialTheme.typography.headlineMedium)
                Column {
                    Text(Strings.antimYatraPost(LocalAppLanguage.current), style = MaterialTheme.typography.titleMedium)
                    Text(if (gu) "ગુજરાતી અંતિમ યાત્રા પોસ્ટ બનાવો અને શેર કરો" else "Create and share a Gujarati funeral notice", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        Card(modifier = Modifier.fillMaxWidth().clickable { onNavigate(Screen.BesanuDesign) }, shape = RoundedCornerShape(18.dp)) {
            Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("🕊️", style = MaterialTheme.typography.headlineMedium)
                Column {
                    Text(Strings.besanuPost(LocalAppLanguage.current), style = MaterialTheme.typography.titleMedium)
                    Text(if (gu) "ગુજરાતી બેસણું પોસ્ટ બનાવો અને શેર કરો" else "Create and share a Gujarati condolence meeting notice", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
