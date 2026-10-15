package com.bodla.parivar.features.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bodla.parivar.core.localization.AppLanguage
import com.bodla.parivar.core.localization.LocalAppLanguage
import com.bodla.parivar.core.localization.Strings
import com.bodla.parivar.core.ui.PrintAction
import com.bodla.parivar.core.theme.*

private data class HistorySection(
    val titleGu: String,
    val titleEn: String,
    val textGu: String,
    val textEn: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VillageHistoryScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lang = LocalAppLanguage.current
    val sections = listOf(
        HistorySection(
            titleGu = "વસવાટ અને પરિવારનાં મૂળ",
            titleEn = "Settlement and family roots",
            textGu = "બોદલા ગામનો ઇતિહાસ અહીં વસેલા પરિવારોની સ્મૃતિઓ અને તેમના મૂળ સાથે ગૂંથાયેલો છે. ઇતિહાસ પુસ્તિકા કુટુંબોનાં નામો, તેમની વંશપરંપરા અને તેઓ કયા સ્થળોથી આવ્યા તેની વિગતો નોંધે છે. આ વિગતો માત્ર અલગ અલગ પરિવારોની ઓળખ જાળવતી નથી, પરંતુ સમય સાથે ગામમાં બનેલા સંબંધો અને સહજીવનની કડી પણ દર્શાવે છે. પેઢી દર પેઢી પરિવારોની મહેનત, પરંપરા અને ગામ સાથેનું જોડાણ બોદલાની સામૂહિક ઓળખનો પાયો બન્યું છે. પુસ્તિકામાં નોંધાયેલાં કુટુંબવૃત્તાંત ગામના ભૂતકાળને લોકોની યાદો અને તેમના મૂળ સાથે જોડે છે.",
            textEn = "The history of Bodla is woven together with the memories and roots of the families who made their home here. The history booklet records family names, lineages and the places from which families came. These details preserve not only the identity of individual families, but also the ties and shared life that grew among them over time. Across generations, families' work, traditions and connection to the village have helped shape Bodla's common identity. The family histories recorded in the booklet connect the village's past with the memories and origins of its people."
        ),
        HistorySection(
            titleGu = "ધાર્મિક અને સામૂહિક સ્થળો",
            titleEn = "Places of worship and community",
            textGu = "બોદલાનાં મંદિરો અને પવિત્ર સ્થળો ગામની ધાર્મિક શ્રદ્ધા સાથે સાથે લોકોના સામૂહિક જીવનની ઓળખ પણ છે. આવા સ્થળોએ પૂજા, પરંપરા અને પ્રસંગો દ્વારા ગ્રામજનોને એકસાથે જોડ્યા છે. ઇતિહાસ પુસ્તિકા આ સ્થળોને યાદ કરતાં તેમની સાથે જોડાયેલી સામાજિક સ્મૃતિ અને ગામના જીવનમાં તેમના સ્થાનને પણ ઉજાગર કરે છે. ગામનો વારસો માત્ર જૂની ઇમારતોમાં જ નહીં, પરંતુ લોકો જાળવી રાખે છે તેવી રીત-રિવાજો, પરસ્પર સહકાર અને પેઢીઓથી વહેંચાતી યાદોમાં પણ જીવંત રહે છે.",
            textEn = "Bodla's temples and sacred places express both religious faith and the shared life of the village. Through worship, traditions and community occasions, these places have brought residents together. In remembering them, the history booklet also highlights their place in village life and the social memories connected with them. A village's heritage lives not only in old buildings, but also in the customs people preserve, the support they give one another and the memories passed between generations."
        ),
        HistorySection(
            titleGu = "શિક્ષણ અને પંચાયત",
            titleEn = "Education and village governance",
            textGu = "ગામની પ્રાથમિક શાળા અને શિક્ષણની વધતી તકો બોદલાના સામાજિક પરિવર્તનની મહત્વની કડી છે. શાળાએ બાળકોને ભણવાની જગ્યા આપી અને આગળ વધવાની નવી શક્યતાઓ ખોલી; શિક્ષણ પ્રત્યેનો પ્રયાસ સમય સાથે અનેક પરિવારોના જીવનનો ભાગ બન્યો. પુસ્તિકા ગ્રામ પંચાયતની ભૂમિકા અને ગામના જાહેર જીવનમાં થયેલા પ્રયાસોને પણ નોંધે છે. સ્થાનિક જરૂરિયાતો ઓળખવી, ગામલોકો સાથે મળીને નિર્ણય લેવો અને જાહેર સુવિધાઓ માટે પ્રયત્ન કરવો એ ગામના વિકાસમાં સહભાગિતાનું મહત્વ દર્શાવે છે.",
            textEn = "The village primary school and expanding access to education are an important part of Bodla's social change. The school gave children a place to learn and opened new possibilities for the future; over time, the pursuit of education became part of life for many families. The booklet also records the role of the Gram Panchayat and efforts in public life. Identifying local needs, making decisions with residents and working for shared facilities show how participation has contributed to the village's development."
        ),
        HistorySection(
            titleGu = "ગામનો બદલાતો સમય",
            titleEn = "A village growing through time",
            textGu = "ખેતી બોદલાના જીવન અને પરિવારોની આજીવિકાનો મહત્વનો આધાર રહી છે. તેની સાથે ગામની જાહેર સુવિધાઓ, સામાજિક સંસ્થાઓ અને યુવાનોની પ્રવૃત્તિઓએ બદલાતી જરૂરિયાતોનો જવાબ આપ્યો છે. ઇતિહાસ પુસ્તિકા આ જુદા જુદા પાસાઓને ગામની આગળ વધતી સફર તરીકે રજૂ કરે છે: જ્યાં રોજિંદા જીવનની મહેનત, સામૂહિક પહેલ અને એકબીજાને આપેલો સહકાર સાથે મળીને પ્રગતિ લાવે છે. આ વારસો કોઈ એક વ્યક્તિ કે સમયગાળાની કહાની નથી; તે અનેક પેઢીઓએ પોતાના ગામ માટે કરેલા યોગદાનની સંયુક્ત સ્મૃતિ છે.",
            textEn = "Agriculture has long been an important foundation of life and livelihood for Bodla's families. Alongside it, public facilities, community organizations and youth activities have responded to the village's changing needs. The history booklet brings these different parts together as a continuing story of a village moving forward, where daily work, shared initiatives and mutual support all contribute to progress. This heritage is not the story of one person or one period; it is the shared memory of what generations of residents have contributed to their village."
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(Strings.villageHistory(lang), style = BodlaPageTitle) },
                actions = {
                    PrintAction(
                        title = if (lang == AppLanguage.GUJARATI) "ગામનો ઇતિહાસ" else "Village history",
                        content = sections.joinToString("\n\n") { "${it.titleGu}\n${it.textGu}" },
                        englishContent = sections.joinToString("\n\n") { "${it.titleEn}\n${it.textEn}" },
                        bothContent = sections.joinToString("\n\n") { "${it.titleGu}\n${it.textGu}\n${it.titleEn}\n${it.textEn}" }
                    )
                },
                navigationIcon = {
                    androidx.compose.material3.IconButton(onClick = onBack) {
                        Text(text = "←", fontSize = 22.sp, color = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = WarmWhiteSurface)
            )
        },
        containerColor = CreamBackground,
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Column(modifier = Modifier.padding(bottom = 4.dp)) {
                    Text(
                        text = if (lang == AppLanguage.GUJARATI) "સ્મૃતિઓ, મૂળ અને પેઢીઓની સામૂહિક સફર" else "Memories, roots and a story shared across generations",
                        style = BodlaBody,
                        color = SaffronPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (lang == AppLanguage.GUJARATI)
                            "આ પુસ્તિકા બોદલાની કહાનીને પરિવારોનાં મૂળ અને ગામના સામૂહિક જીવન સાથે જોડે છે. તેમાં કુટુંબો, ધાર્મિક સ્થળો, શિક્ષણ, ગ્રામ પંચાયત અને ગામની પ્રગતિ જેવા વિષયો દ્વારા સમય સાથે બદલાતા જીવનની ઝલક મળે છે. નીચેનો પરિચય આ વિષયોને વાંચવા યોગ્ય લાંબા વર્ણનમાં ગોઠવે છે. ચોક્કસ નામો, તારીખો અને આંકડાઓ માટે મૂળ ગુજરાતી પુસ્તિકા જુઓ."
                        else
                            "The booklet connects Bodla's story with the roots of its families and the shared life of the village. Through its themes of families, places of worship, education, the Gram Panchayat and village development, it offers a glimpse of how life has changed over time. The account below brings these themes together in a longer narrative. Refer to the original Gujarati booklet for names, dates and figures.",
                        style = BodlaBody,
                        color = TextSecondary
                    )
                }
            }
            items(sections) { section ->
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    Text(
                        text = if (lang == AppLanguage.GUJARATI) section.titleGu else section.titleEn,
                        style = BodlaCardTitle,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (lang == AppLanguage.GUJARATI) section.textGu else section.textEn,
                        style = BodlaBody,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = BorderStroke)
                }
            }
        }
    }
}
