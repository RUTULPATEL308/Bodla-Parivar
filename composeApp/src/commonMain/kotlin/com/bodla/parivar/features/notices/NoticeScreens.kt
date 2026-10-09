package com.bodla.parivar.features.notices

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bodla.parivar.core.localization.AppLanguage
import com.bodla.parivar.core.localization.LocalAppLanguage
import com.bodla.parivar.core.localization.Strings
import com.bodla.parivar.core.navigation.Screen
import com.bodla.parivar.core.theme.*
import com.bodla.parivar.core.ui.*
import com.bodla.parivar.domain.model.Notice

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoticeListScreen(
    onNavigate: (Screen) -> Unit,
    notices: List<Notice> = emptyList(),
    modifier: Modifier = Modifier
) {
    val lang = LocalAppLanguage.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = Strings.navNotices(lang), style = BodlaPageTitle) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = WarmWhiteSurface,
                    titleContentColor = TextPrimary
                )
            )
        },
        containerColor = CreamBackground,
        modifier = modifier
    ) { innerPadding ->
        if (notices.isEmpty()) {
            val demoNotices = listOf(
                Notice(
                    id = "demo_notice_1",
                    titleGu = "[DEMO DATA] ગ્રામ પંચાયત સામાન્ય સભાનું આયોજન",
                    titleEn = "[DEMO DATA] Gram Panchayat General Meeting Scheduled",
                    descriptionGu = "આ ડેમો સૂચના છે. બોદલા ગામના તમામ ગ્રામજનોને જણાવવાનું કે આગામી રવિવારે પંચાયત હોલ ખાતે બેઠક મળશે.",
                    descriptionEn = "This is demo data. Notice to all Bodla residents regarding upcoming community meeting at Panchayat hall.",
                    categoryNameGu = "ગ્રામ પંચાયત",
                    categoryNameEn = "Gram Panchayat",
                    isPinned = true,
                    createdAt = "2026-09-28"
                ),
                Notice(
                    id = "demo_notice_2",
                    titleGu = "[DEMO DATA] પીવાના પાણીના વિતરણ સમયમાં ફેરફાર",
                    titleEn = "[DEMO DATA] Drinking Water Distribution Timetable",
                    descriptionGu = "આ ડેમો સૂચના છે. પાઇપલાઇન સમારકામના કારણે આવતીકાલે પાણીનો સમય સવારે ૬ થી ૮ રહેશે.",
                    descriptionEn = "This is demo data. Due to maintenance, water supply will run 6am to 8am tomorrow.",
                    categoryNameGu = "સામાન્ય સૂચના",
                    categoryNameEn = "General Notice",
                    isPinned = false,
                    createdAt = "2026-09-27"
                )
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(demoNotices) { notice ->
                    NoticeCard(
                        notice = notice,
                        onClick = { onNavigate(Screen.NoticeDetail(notice.id)) }
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(notices) { notice ->
                    NoticeCard(
                        notice = notice,
                        onClick = { onNavigate(Screen.NoticeDetail(notice.id)) }
                    )
                }
            }
        }
    }
}

@Composable
fun NoticeCard(
    notice: Notice,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lang = LocalAppLanguage.current

    BodlaCard(
        onClick = onClick,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val category = if (lang == AppLanguage.GUJARATI) notice.categoryNameGu else notice.categoryNameEn
            if (!category.isNullOrBlank()) {
                Surface(
                    color = LightIconBg,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = category,
                        style = BodlaCaption.copy(fontWeight = FontWeight.SemiBold),
                        color = SaffronPrimary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
            DemoDataBadge()
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (lang == AppLanguage.GUJARATI) notice.titleGu else notice.titleEn,
            style = BodlaCardTitle,
            color = TextPrimary
        )
        val desc = if (lang == AppLanguage.GUJARATI) notice.descriptionGu else notice.descriptionEn
        if (!desc.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = desc,
                style = BodlaBody.copy(fontSize = 14.sp),
                color = TextSecondary,
                maxLines = 2
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = notice.createdAt,
            style = BodlaCaption,
            color = TextTertiary
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoticeDetailScreen(
    noticeId: String,
    onBack: () -> Unit,
    notice: Notice? = null,
    modifier: Modifier = Modifier
) {
    val lang = LocalAppLanguage.current
    val currentNotice = notice ?: Notice(
        id = noticeId,
        titleGu = "[DEMO DATA] ગ્રામ પંચાયત સામાન્ય સભાનું આયોજન",
        titleEn = "[DEMO DATA] Gram Panchayat General Meeting Scheduled",
        descriptionGu = "આ ડેમો સૂચના છે. બોદલા ગામના તમામ ગ્રામજનોને જણાવવાનું કે આગામી રવિવારે પંચાયત હોલ ખાતે ગામના વિકાસકાર્યો અંગે ચર્ચા કરવા સામાન્ય સભા મળશે. આપ સૌને ઉપસ્થિત રહેવા નમ્ર વિનંતી છે.",
        descriptionEn = "This is demo data. Notice to all Bodla residents regarding upcoming community meeting at Panchayat hall to discuss village development projects. Everyone is cordially invited.",
        categoryNameGu = "ગ્રામ પંચાયત",
        categoryNameEn = "Gram Panchayat",
        createdAt = "2026-09-28"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = Strings.navNotices(lang), style = BodlaPageTitle) },
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
                .padding(16.dp)
        ) {
            BodlaCard {
                DemoDataBadge()
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (lang == AppLanguage.GUJARATI) currentNotice.titleGu else currentNotice.titleEn,
                    style = BodlaPageTitle,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "તારીખ / Date: " + currentNotice.createdAt,
                    style = BodlaCaption,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Divider(color = BorderStroke)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = (if (lang == AppLanguage.GUJARATI) currentNotice.descriptionGu else currentNotice.descriptionEn).orEmpty(),
                    style = BodlaBody,
                    color = TextPrimary,
                    lineHeight = 26.sp
                )
            }
        }
    }
}
