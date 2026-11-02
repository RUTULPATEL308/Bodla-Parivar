package com.bodla.parivar.features.history

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bodla.parivar.core.localization.AppLanguage
import com.bodla.parivar.core.localization.LocalAppLanguage
import com.bodla.parivar.core.localization.Strings
import com.bodla.parivar.core.ui.PrintAction
import com.bodla.parivar.core.theme.*

private data class HRow(val cells: List<String>)
private data class HTable(val headers: List<String>, val rows: List<HRow>)
private data class HSection(
    val titleGu: String,
    val titleEn: String,
    val bodyGu: String,
    val bodyEn: String,
    val table: HTable? = null
)

private val NAVY = Color(0xFF1E293B)
private val DARK = Color(0xFF0F172A)
private val SLATE = Color(0xFF475569)
private val GOLD = Color(0xFFD97706)
private val WARM = Color(0xFFF1EAE4)
private val LIGHT = Color(0xFFF8FAFC)
private val BORDER = Color(0xFFD7DEE8)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VillageHistoryScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lang = LocalAppLanguage.current
    val sections = listOf(
        HSection("(૧) સ્થાપના", "(1) Founding", "હાલના રહેઠાણવાળા ગામની સ્થાપના વિ.સં. ૧૯૧૦ (ઈ.સ. ૧૮૫૪) માં. જણાવેલ વડવાઓએ બાંધેલ છે.", "Founded in VS 1910 (1854 CE).",
            HTable(listOf("વ્યક્તિ","હાલનો ટોડો","ક્યાંથી આવ્યા"), listOf(
                HRow(listOf("પટેલ જેસંગભા ઉમેદભા","વેલા-હરિ","હીંગળાજપુરા")),
                HRow(listOf("પટેલ ઉમેદભા મૂળજીભા","રામા-ઉમેદ","કનોડા")),
                HRow(listOf("પટેલ ધનજીભા હેમરાજભા","ઈશ્વર-શામળ","સદુથલા")),
                HRow(listOf("પટેલ ધનજીભા વાલજીભા","વિરચંદ-ધનજી","કનોડા")),
                HRow(listOf("પટેલ શીવરામ સવજીભા","માધા-શીવા","કુકરવાડા")),
                HRow(listOf("ભગુભા ભવાનભા","જોઈતા-માધા","કુકરવાડા")),
                HRow(listOf("પટેલ હરિભા બેચરભા","જોઈતા-શંકર","કનોડા")),
            ))),
        HSection("(૧) સ્થાપના - મૂળ", "(1) Founding - Origins", "આપણા વડવાઓનું મૂળ વતન ઊંઝા. આહીરો અને ચૌધરીઓએ જૂનું ગામ ખાલી કરતાં, ગાયકવાડ સરકાર પાસેથી પડતર જમીનો મેળવી, વડવાઓએ બોદલામાં નવેસરથી વસવાટ કર્યો. શરૂમાં ૧૬ પાટીદાર, ૧૦ ઠાકોર, ૨ વાઘરી, ૨ હરિજન ઘર.", "Ancestral home Unjha. When Ahirs and Chaudharys vacated, founders obtained wastelands from Gaekwad government. Initially 16 Patel, 10 Thakor, 2 Vaghari, 2 Harijan families.", null),
        HSection("(૨) જનસંખ્યા", "(2) Demographics", "એમ કુલ ૩૦ કુટુંબોથી વસેલું આ ગામ આજે ૧૬૦ ઘર પટેલો, ૫૦ ઘર ઠાકોરો, ૧૫ ઘર હરિજનો, ૧૦ ઘર પ્રજાપતિ, ૫ ઘર લુહાર, ૧ ઘર દરજી, ૧૦ ઘર રાવળ તથા ૬ ઘર વાઘરી એમ કુલ ૨૦૦ ઘરમાં આશરે ૧,૨૦૦ની વસ્તી. ૬૦-૬૫ પાટીદાર કુટુંબો બહાર વસે.", "Started with 30 families; now 200 houses, ~1,200 people. 60-65 Patidar families live outside.",
            HTable(listOf("સમુદાય","ઘર"), listOf(
                HRow(listOf("પાટીદાર","૧૬૦")), HRow(listOf("ઠાકોર","૫૦")), HRow(listOf("હરિજન","૧૫")),
                HRow(listOf("પ્રજાપતિ","૧૦")), HRow(listOf("લુહાર","૫")), HRow(listOf("દરજી","૧")),
                HRow(listOf("રાવળ","૧૦")), HRow(listOf("વાઘરી","૬")), HRow(listOf("કુલ","૨૦૦")),
            ))),
        HSection("(૩) આફતો", "(3) Calamities", "(૧) કુદરતી: ૭૦ વર્ષ પહેલાં પ્લેગ, ૪૫ મૃત્યુ. (૨) માનવીય: ચોકી પ્રથા દરમ્યાન ચોરીઓ; યુવાનોએ રખેવાડીનું બીડું ઝડપ્યું.", "(1) Natural: plague killed 45. (2) Human: thefts; youth took charge.", null),
        HSection("(૪) જીવન વ્યવસ્થા", "(4) Life & Water", "પીવાના પાણી માટે ગામ લોકોએ જાતે વ્યવસ્થા ઊભી કરી. ત્રણ કૂવામાંથી બે પડતર. ૧૯૨૦: ડોસાશેઠ (વાણીયો) ગામમાં આવ્યા.", "Village arranged water supply. Three wells, two defunct. 1920: Dosasheth settled.", null),
        HSection("(૫) દેવસ્થાનો", "(5) Temples", "દૂધેશ્વર મહાદેવ (૪૦૦ વર્ષ), ચામુંડા માતા, રામજી મંદિર (વિ.સં. ૧૯૨૫), દાસજીયા ગોગ (૧૦૦+ વર્ષ), હિંદવા પીર, રામદેવજી પીર, મનોકામના સિદ્ધ હનુમાનજી (૨૦૦૨). ૪ વાર્ષિક યજ્ઞો.", "Dudheshwar Mahadev (400 yrs), Chamunda Mata, Ramji Mandir (VS 1925), Dasjiya Gog (100+ yrs), Hindva Pir, Ramdevji Pir, Manokamna Siddh Hanumanji (2002). 4 annual Yagnas.", null),
        HSection("(૬) અભ્યાસ", "(6) Education", "ગાયકવાડ સરકારે પ્રાથમિક શાળા શરૂ. ૧૯૫૧: મણીલાલ રામદાસ. ૧૯૫૪: મહાદેવની જગ્યામાં પ્રથમ સરકારી પ્રાથમિક શાળા. આજે: ૩ ડૉક્ટર, ૪ M.Com, ૩ B.Com, ૨ B.Ed, ૨ M.A., ૪ LLB.", "Gaekwad government started primary school. 1951: village school by Manilal Ramdas. 1954: first government primary school. Today: 3 doctors, 4 M.Com, 3 B.Com, 2 B.Ed, 2 M.A., 4 LLB.", null),
        HSection("(૭) પંચાયત", "(7) Panchayat", "૧૯૫૦થી પંચાયતી રાજ. ૧૧ સરપંચો (૧૯૫૦-ચાલુ). ઉપલબ્ધીઓ: પ્રાથમિક શાળા, ગામ કૂવો, સેવા સહકારી મંડળી, એપ્રોચ રોડ, તળાવ ખોદકામ, વોટર વર્કસ, ઈલેક્ટ્રીકરણ, નર્મદા પાણી, બસસ્ટેન્ડ, આંગણવાડી.", "Panchayati Raj started 1950. 11 Sarpanchs (1950-present). Achievements: primary school, village well, service cooperative, approach road, pond excavation, water works, electrification, Narmada water, bus stand, Anganwadi.",
            HTable(listOf("સરપંચ","સમયગાળો"), listOf(
                HRow(listOf("મણીલાલ રામદાસ પટેલ","૧૯૫૦-૧૯૫૫")), HRow(listOf("જોઈતારામ શામળદાસ પટેલ","૧૯૫૫-૧૯૬૫")),
                HRow(listOf("પરસોત્તમ ઈશ્વરદાસ પટેલ","૧૯૬૫-૧૯૭૦")), HRow(listOf("પરસોત્તમ રામદાસ પટેલ","૧૯૭૦-૧૯૮૦")),
                HRow(listOf("રામાભાઈ શંકરદાસ પટેલ","૧૯૮૦-૧૯૮૫")), HRow(listOf("અમૃતભાઈ માધવદાસ પટેલ","૧૯૮૫-૧૯૮૬")),
                HRow(listOf("પરસોત્તમ જોઈતારામ પટેલ","૧૯૮૬-૧૯૯૧")), HRow(listOf("ભગાભાઈ શિવદાસ પટેલ","૧૯૯૧-૧૯૯૬")),
                HRow(listOf("કરશનજી કાનાજી ઠાકોર","૧૯૯૬-૨૦૦૫")), HRow(listOf("દિપાજી મફાજી ઠાકોર","૨૦૦૫-૨૦૦૬")),
                HRow(listOf("પ્રહલાદજી ખેંગારજી ઠાકોર","૨૦૦૬ થી ચાલુ")),
            ))),
        HSection("(૮) આવકનાં સાધનો", "(8) Income Sources", "૧૯૬૬-૬૭ સુધી ખેતી એકમાત્ર આવક. ત્યારબાદ નોકરી અને વેપાર. હીરા ઉદ્યોગ. ૧૯૭૨ પછી બોરીંગ. ડેરી મંડળી. બોર રીપેરીંગ મશીનો. આવક: માત્ર ખેતી ૨૦, ખેતી+નોકરી ૬૦.", "Until 1966-67 agriculture was sole income. Then jobs and business. Diamond industry. 1972+: borewells. Dairy cooperative. Bore repair machines.", null),
        HSection("(૯) યુવા અને સાંસ્કૃતિક", "(9) Youth & Culture", "૧૯૫૮થી યુવક મંડળ. બસસ્ટેન્ડ અને શાળામાં કૂવા, ૩૦+ વૃક્ષો. દર વર્ષે સાંસ્કૃતિક કાર્યક્રમો. ૧૯૮૫માં નવરાત્રી મંડળ. ધાર્મિક: ગાયત્રી સમૂહ પ્રાર્થના (દર ગુરુવાર), સ્વાધ્યાય (૨૦ વર્ષ), ચોટીલા પગપાળા સંઘ (૧૫ વર્ષ), મહિલા ભજન મંડળ (દૈનિક), ૪ વાર્ષિક યજ્ઞો.", "Youth Mandal since 1958. Wells and 30+ trees. Annual cultural programs. Navratri Mandal (1985). Religious activities.", null),
        HSection("(૧૦) ધાર્મિક પ્રવૃત્તિઓ", "(10) Religious Activities", "૧. ગાયત્રી સમૂહ પ્રાર્થના - દર ગુરુવાર (૧૯૮૫થી). ૨. સ્વાધ્યાય - ૨૦ વર્ષ. ૩. ચોટીલા પગપાળા સંઘ - ૧૫ વર્ષ. ૪. મહિલા ભજન મંડળ - દૈનિક. ૪ વાર્ષિક યજ્ઞો: નવચંડી, હનુમાનજી, ગોગા મહારાજ, રામાપીર.", "1. Gayatri group prayer (Thu since 1985). 2. Swadhyay (20 yrs). 3. Chotila pilgrimage (15 yrs). 4. Mahila Bhajan Mandal (daily). 4 annual Yagnas.", null),
        HSection("(૧૧) વિકાસ ગાથા", "(11) Development", "છેલ્લા ૨૫ વર્ષોમાં ગ્રામ વિકાસ. ૧૯૦ પાટીદાર કુટુંબ: ૧૦૦ ગામમાં, ૬૦ બહાર. માત્ર ખેતી ૨૦, ખેતી+નોકરી ૬૦. શિક્ષણ: ૩ ડૉક્ટર, ૪ M.Com, ૩ B.Com, ૨ B.Ed, ૨ M.A., ૪ LLB. ૫૦ વર્ષ પહેલાં માટીના ઘરો; આજે ૯૫% પાકાં. ૧૩ ટ્રેક્ટર, ૩૦ બોર રીપેરીંગ મશીન, ૩૫ કાર, ૪ જીપ, પ માર્શલ/કોલીસ, ૮ બોરીંગ. વિશેષતા: તમામ પાટીદારો એક જ બાપના વંશજ; કોઈએ ક્યારેય વિઘન ઉભું કર્યું નથી.", "Last 25 years of village development. 190 Patidar families: 100 in village, 60 outside. Farming only: 20, farming+jobs: 60. Education: 3 doctors, 4 M.Com, 3 B.Com, 2 B.Ed, 2 M.A., 4 LLB. 95% pucca. 13 tractors, 30 bore-repair machines, 35 cars, 4 jeeps, 5 marshal/colis, 8 tube wells. Unity: all Patidars descend from one ancestor.",
            HTable(listOf("સાધન","સંખ્યા"), listOf(
                HRow(listOf("ટ્રેક્ટર્સ","૧૩")), HRow(listOf("બોર રીપેરીંગ મશીનો","૩૦")),
                HRow(listOf("કારો","૩૫")), HRow(listOf("જીપ ગાડીઓ","૪")),
                HRow(listOf("માર્શલ/કોલીસ","પ")), HRow(listOf("બોરીંગ (ટયુબવેલ)","૮")),
            ))),
        HSection("(૧૨) આભાર દર્શન", "(12) Acknowledgment", "સાદર, આપણા ઘરઆંગણે ગામદેવી/દેવતાઓનાં આશીર્વાદથી આજનો રૂડો અવસર, ગામલોકોનાં સંપૂર્ણ ભાવનાપૂર્ણ સાથ અને સહકારથી ઉજવવામાં આવે છે. તે બદલ આપણાં પાટીદાર ચોયસી સમાજનો ગૌરવ સાથે આભાર માનીએ છીએ.", "With respect, this occasion is celebrated with the blessings of village deities and the full emotional support and cooperation of village residents. We express our gratitude with pride to the Patidar Choryasi Samaj.", null),
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(Strings.villageHistory(lang), style = BodlaPageTitle) },
                navigationIcon = {
                    androidx.compose.material3.IconButton(onClick = onBack) {
                        Text(text = "←", fontSize = 22.sp, color = NAVY)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFFFFFFF))
            )
        },
        containerColor = Color(0xFFFFFFFF),
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Column(modifier = Modifier.padding(bottom = 4.dp)) {
                    Text(text = if (lang == AppLanguage.GUJARATI) "બોદલા ગામનો ઇતિહાસ" else "History of Bodla Village", style = BodlaBody, color = GOLD, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = if (lang == AppLanguage.GUJARATI) INTRO_GU else INTRO_EN, style = BodlaBody, color = SLATE)
                }
            }
            items(sections) { section ->
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    Text(text = if (lang == AppLanguage.GUJARATI) section.titleGu else section.titleEn, style = BodlaCardTitle, color = DARK)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = if (lang == AppLanguage.GUJARATI) section.bodyGu else section.bodyEn, style = BodlaBody, color = SLATE)
                    section.table?.let { t ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Column(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                            Row {
                                t.headers.forEach { h ->
                                    Text(text = h, style = BodlaCaption.copy(fontWeight = FontWeight.SemiBold), color = DARK, modifier = Modifier.width(140.dp).padding(6.dp))
                                }
                            }
                            HorizontalDivider(color = BORDER)
                            t.rows.forEach { r ->
                                Row {
                                    r.cells.forEach { c ->
                                        Text(text = c, style = BodlaCaption, color = SLATE, modifier = Modifier.width(140.dp).padding(6.dp))
                                    }
                                }
                                HorizontalDivider(color = BORDER.copy(alpha = 0.4f))
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = BORDER)
                }
            }
        }
    }
}

private val INTRO_GU = "આજરોજ તા. ૨-૧૧-૨૦૦૮ને રવિવારે, સમસ્ત ચોર્યાસી કડવા પાટીદાર સમાજ - કેળવણી ઉત્તેજક મંડળ આયોજિત તેજસ્વી તારલા અભિવાદન કાર્યક્રમના યજમાન બનવાનું સદ્ભાગ્ય અમારા ગામને સાંપડ્યું છે. જેને સમસ્ત બોદલા ગ્રામ પરિવારનાં અહોભાગ્ય માનીએ છીએ. આપણી સંસ્થાના એક અંગ સમાન બોદલા ગામનો ઈતિહાસ અને તેની પ્રગતિની રૂપરેખા આપ સૌ સમક્ષ પુસ્તિકારૂપે પ્રગટ કરતાં અમે આનંદ અનુભવીએ છીએ. સમાજમાં અમારા અસ્તિત્વની જાણકારી ફેલાય, ઉપરાંત ખુદના ઉદ્ગમથી અજાણ એવા ગામ લોકોને પણ પોતાની સ્થિતિ અને પ્રગતિનો અહેસાસ થાય અને એમના ઉત્સાહમાં વધારો થાય એવી અપેક્ષાએ કરેલો આ પ્રયત્ન આપ સૌ અતિરેકમાં નહિં ખપાવશો એવી અભ્યર્થના.\n\nઆજથી ૪૦૦ વર્ષ પહેલાં ભારત જયારે દેશી રજવાડાંનું રાજય હતું ત્યારે રાજા પોતાની વસ્તીની માહિતી તેમણે નીમેલા ભાટ-ચારણો દ્વારા મેળવતા. દરેક ગામના અને વર્ણના પેઢીનામા લખવાના ઈજારા આપવામાં આવતા. ભાટ-ચારણો તેમને સોંપાયેલ કુટુંબોના પેઢીનામાના ચોપડા રાખતા. જેમાં વ્યક્તિ વિશેષના ઉલ્લેખ પણ સચવાતા. દર ચાર-પાંચ વર્ષે ગામે ગામ ફરી પેઢીમાં નવા જન્મનાર બાળકનો ઉમેરો નોંધવામાં આવતો. યજમાનો પોતાના બારોટને રહેવા-જમવાની વ્યવસ્થા સાચવતા અને ભેટ-સોગાદો પણ આપતા. આમ ગામના દરેક વ્યક્તિ પોતાની વંશાવલીથી માહિતગાર રહેતા. એ પ્રથા હાલમાં તો ભૂંસાઈ જવા આવી છે. પરંતુ એ પ્રથામાં ઉછરેલા આપણા દાદાઓ (૧) સ્વ. વેલાભા હરિદાસ (૨) સ્વ. ઈસ્વરભા શામળદાસ તથા (૩) માજી સરપંચ પુરુષોત્તમદાસ રામદાસ (૮૭) વિગેરેના મુખેથી અને બારોટના ચોપડે નોંધાયા પ્રમાણે બોદલા ગામનો ઈતિહાસ આપ સમક્ષ રજૂ કરતાં આનંદ અનુભવું છું."
private val INTRO_EN = "Today, Sunday 2 Nov 2008, Bodla village has the honor of hosting the \"Tejasvi Tarala Abhivadan\" program organized by the Choryasi Kadva Patidar Samaj - Kelvani Uttejak Mandal. 400 years ago, when India was a land of princely states, kings obtained population information through Barots (bards). Each village and caste had genealogies recorded. This practice has now faded, but elders raised in that tradition - Sw. Velabha Haridas, Sw. Ishwarbha Shamaldas and ex-Sarpanch Purushottamdas Ramdas (87) - recount Bodla's history from Barot records."


