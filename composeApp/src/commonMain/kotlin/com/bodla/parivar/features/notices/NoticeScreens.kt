package com.bodla.parivar.features.notices

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil3.compose.AsyncImage
import com.bodla.parivar.core.localization.AppLanguage
import com.bodla.parivar.core.localization.LocalAppLanguage
import com.bodla.parivar.core.localization.Strings
import com.bodla.parivar.core.navigation.Screen
import com.bodla.parivar.core.theme.*
import com.bodla.parivar.core.ui.*
import com.bodla.parivar.domain.model.Notice
import com.bodla.parivar.core.ui.PrintAction

private fun formatNoticeText(value: String): AnnotatedString = buildAnnotatedString {
    val markup = Regex("""(https?://[^\s<>]+|\*[^*\n]+\*|_[^_\n]+_|~[^~\n]+~|`[^`\n]+`)""", RegexOption.IGNORE_CASE)
    var cursor = 0
    markup.findAll(value).forEach { match ->
        append(value.substring(cursor, match.range.first))
        if (match.value.startsWith("http://", ignoreCase = true) || match.value.startsWith("https://", ignoreCase = true)) {
            val url = match.value.trimEnd('.', ',', '!', '?', ':', ';', ')', ']')
            val trailing = match.value.removePrefix(url)
            pushStringAnnotation(tag = "URL", annotation = url)
            pushStyle(SpanStyle(color = androidx.compose.ui.graphics.Color(0xFF1565C0), textDecoration = TextDecoration.Underline))
            append(url)
            pop() // style
            pop() // URL annotation
            append(trailing)
            cursor = match.range.last + 1
            return@forEach
        }
        val style = when (match.value.first()) {
            '*' -> SpanStyle(fontWeight = FontWeight.Bold)
            '_' -> SpanStyle(fontStyle = FontStyle.Italic)
            '~' -> SpanStyle(textDecoration = TextDecoration.LineThrough)
            else -> SpanStyle(fontFamily = FontFamily.Monospace)
        }
        withStyle(style) { append(match.value.substring(1, match.value.length - 1)) }
        cursor = match.range.last + 1
    }
    append(value.substring(cursor))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoticeListScreen(
    onNavigate: (Screen) -> Unit,
    notices: List<Notice> = emptyList(),
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val lang = LocalAppLanguage.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = Strings.navNotices(lang), style = BodlaPageTitle) },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Text(text = "←", fontSize = 22.sp, color = TextPrimary)
                        }
                    }
                },
                actions = {
                    PrintAction(title = if (lang == AppLanguage.GUJARATI) "સૂચનાઓ" else "Notices",
                        content = notices.joinToString("\n\n") { "${it.titleGu}\n${it.descriptionGu.orEmpty()}\n${it.createdAt}" },
                        englishContent = notices.joinToString("\n\n") { "${it.titleEn}\n${it.descriptionEn.orEmpty()}\n${it.createdAt}" },
                        bothContent = notices.joinToString("\n\n") { "${it.titleGu}\n${it.descriptionGu.orEmpty()}\n${it.titleEn}\n${it.descriptionEn.orEmpty()}\n${it.createdAt}" })
                },
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
            Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Text(
                    text = "No published notices yet. Pull down to refresh.",
                    style = BodlaBody,
                    color = TextSecondary
                )
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
                text = formatNoticeText(desc),
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
    onBack: () -> Unit,
    notice: Notice? = null,
    modifier: Modifier = Modifier
) {
    val lang = LocalAppLanguage.current
    val uriHandler = LocalUriHandler.current
    val showImagePreview = remember { mutableStateOf(false) }
    val showAttachmentPreview = remember { mutableStateOf(false) }
    val currentNotice = notice

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
                .verticalScroll(rememberScrollState())
        ) {
            if (currentNotice == null) {
                EmptyStateView(
                    message = if (lang == AppLanguage.GUJARATI) "આ સૂચના હવે ઉપલબ્ધ નથી." else "This notice is no longer available."
                )
            } else BodlaCard {
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
                HorizontalDivider(color = BorderStroke)
                Spacer(modifier = Modifier.height(16.dp))
                val noticeDescription = formatNoticeText((if (lang == AppLanguage.GUJARATI) currentNotice.descriptionGu else currentNotice.descriptionEn).orEmpty())
                Text(
                    text = noticeDescription,
                    style = BodlaBody,
                    color = TextPrimary,
                    lineHeight = 26.sp,
                    modifier = Modifier.clickable {
                        noticeDescription.getStringAnnotations("URL", 0, noticeDescription.length)
                            .firstOrNull()
                            ?.let { uriHandler.openUri(it.item) }
                    }
                )
                val imageUrl = currentNotice.imageUrl
                if (!imageUrl.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(18.dp))
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = if (lang == AppLanguage.GUJARATI) "સૂચનાનો ફોટો" else "Notice image",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp).clickable { showImagePreview.value = true }
                    )
                    OutlinedButton(onClick = { uriHandler.openUri("$imageUrl?download") }, modifier = Modifier.fillMaxWidth()) {
                        Text(if (lang == AppLanguage.GUJARATI) "ફોટો ડાઉનલોડ કરો" else "Download image")
                    }
                }
                val attachmentUrl = currentNotice.attachmentUrl
                if (!attachmentUrl.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(onClick = { showAttachmentPreview.value = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(if (lang == AppLanguage.GUJARATI) "વિડિયો / દસ્તાવેજ જુઓ" else "View video / document")
                    }
                }
            }
        }
    }

    if (currentNotice == null) return

    if (showImagePreview.value && !currentNotice.imageUrl.isNullOrBlank()) {
        AlertDialog(
            onDismissRequest = { showImagePreview.value = false },
            confirmButton = {
                TextButton(onClick = { showImagePreview.value = false }) {
                    Text(if (lang == AppLanguage.GUJARATI) "બંધ કરો" else "Close")
                }
            },
            text = {
                ZoomableImage(
                    model = currentNotice.imageUrl,
                    contentDescription = if (lang == AppLanguage.GUJARATI) "સૂચનાનો ફોટો" else "Notice image",
                    modifier = Modifier.fillMaxWidth().heightIn(min = 280.dp, max = 560.dp),
                    zoomInLabel = if (lang == AppLanguage.GUJARATI) "મોટું કરો" else "Zoom in",
                    zoomOutLabel = if (lang == AppLanguage.GUJARATI) "નાનું કરો" else "Zoom out",
                    resetLabel = if (lang == AppLanguage.GUJARATI) "રીસેટ" else "Reset"
                )
            }
        )
    }

    val attachmentUrl = currentNotice.attachmentUrl
    if (showAttachmentPreview.value && !attachmentUrl.isNullOrBlank()) {
        Dialog(onDismissRequest = { showAttachmentPreview.value = false }) {
            Surface(shape = MaterialTheme.shapes.large, color = WarmWhiteSurface) {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    NoticeAttachmentPreview(
                        url = attachmentUrl,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 300.dp, max = 560.dp)
                    )
                    OutlinedButton(
                        onClick = { uriHandler.openUri(attachmentUrl + if (attachmentUrl.contains('?')) "&download" else "?download") },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    ) { Text(if (lang == AppLanguage.GUJARATI) "ડાઉનલોડ કરો" else "Download") }
                    TextButton(
                        onClick = { showAttachmentPreview.value = false },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(if (lang == AppLanguage.GUJARATI) "બંધ કરો" else "Close") }
                }
            }
        }
    }
}

@Composable
expect fun NoticeAttachmentPreview(url: String, modifier: Modifier = Modifier)
