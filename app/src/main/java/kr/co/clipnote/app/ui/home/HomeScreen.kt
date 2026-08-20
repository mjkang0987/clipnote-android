package kr.co.clipnote.app.ui.home

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kr.co.clipnote.app.R
import kr.co.clipnote.app.i18n.LocalI18n
import kr.co.clipnote.app.ui.LocalContainer
import kr.co.clipnote.app.ui.LocalNav
import kr.co.clipnote.app.ui.LocalRouter
import kr.co.clipnote.app.ui.Route
import kr.co.clipnote.app.ui.VmFactory
import kr.co.clipnote.app.ui.components.AppScaffold
import kr.co.clipnote.app.ui.components.ClipCard
import kr.co.clipnote.app.ui.components.PrimaryButton
import kr.co.clipnote.app.ui.components.RunningDino
import kr.co.clipnote.app.ui.components.SecondaryButton
import kr.co.clipnote.app.ui.components.SharePreviewCard
import kr.co.clipnote.app.ui.components.TagChip
import kr.co.clipnote.app.ui.components.ToolbarTextButton
import kr.co.clipnote.app.ui.components.clickableRow
import kr.co.clipnote.app.ui.onboarding.TourAnchor
import kr.co.clipnote.app.ui.onboarding.tourAnchor
import kr.co.clipnote.app.ui.theme.AppColor
import kr.co.clipnote.app.ui.theme.ShapeMd
import kr.co.clipnote.app.ui.theme.ShapeSm
import kr.co.clipnote.core.util.buildShareText
import kr.co.clipnote.core.util.prettyHost

/**
 * 홈 — 링크 붙여넣기 → 메타 추출 → 미리보기 → 저장(게스트는 이 기기, 로그인은 공유·계정).
 *
 * 버튼 배치는 로그인 여부와 상관없이 같다 — **위 줄은 공유·복사, 저장은 항상 하단 한 줄.**
 * 전에는 저장이 위에 있어서 같은 버튼이 로그인 여부에 따라 위아래로 튀었다.
 */
@Composable
fun HomeScreen(vmKey: String? = null) {
    val container = LocalContainer.current
    val i18n = LocalI18n.current
    val router = LocalRouter.current
    val nav = LocalNav.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val vm: HomeViewModel = viewModel(
        key = vmKey,
        factory = remember { VmFactory { HomeViewModel(container.api, container.localClips) } },
    )
    val auth by container.auth.state.collectAsState()

    var savedLocal by remember { mutableStateOf(false) }
    var creating by remember { mutableStateOf(false) }
    var savingDirect by remember { mutableStateOf(false) }
    var directSaved by remember { mutableStateOf(false) }
    var shareUrl by remember { mutableStateOf<String?>(null) }
    var shareSheetOpen by remember { mutableStateOf(false) }
    var copiedLink by remember { mutableStateOf(false) }
    var copiedShare by remember { mutableStateOf(false) }

    // 공유 인텐트가 넘긴 URL 을 입력칸에 채운다(1회 소비).
    LaunchedEffect(router.pendingSharedUrl) {
        val shared = router.pendingSharedUrl
        if (!shared.isNullOrEmpty()) {
            vm.onUrlChanged(shared)
            router.pendingSharedUrl = null
        }
    }

    val guestShareText = buildShareText(vm.resolvedTitle, null, vm.url)

    AppScaffold(
        title = "",
        actions = {
            ToolbarTextButton(i18n.t(R.string.common_myClips)) { nav.go(Route.CLIPS) }
        },
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
            ) {
                Hero()

                FormCard(
                    vm = vm,
                    onUrlChanged = { value ->
                        vm.onUrlChanged(value)
                        // URL 이 바뀌면 이전 링크는 이 입력과 맞지 않는다 — 버려서 1차 버튼을
                        // '링크 만들기' 로 되돌린다.
                        shareUrl = null
                        shareSheetOpen = false
                    },
                )

                Actions(
                    loggedIn = container.auth.displayLoggedIn,
                    hasInput = vm.hasInput,
                    creating = creating,
                    savingDirect = savingDirect,
                    savedLocal = savedLocal,
                    directSaved = directSaved,
                    copiedLink = copiedLink,
                    copiedShare = copiedShare,
                    shareUrl = shareUrl,
                    onCreateShare = {
                        scope.launch {
                            creating = true
                            val result = vm.createShare(container.auth.validAccessToken())
                            creating = false
                            if (result?.shareUrl != null) {
                                shareUrl = result.shareUrl
                                shareSheetOpen = true
                            }
                        }
                    },
                    onCopyShareLink = {
                        shareUrl?.let { copyToClipboard(context, it) }
                        scope.launch { copiedLink = true; delay(1500); copiedLink = false }
                    },
                    onCopyOriginal = {
                        copyToClipboard(context, guestShareText)
                        scope.launch { copiedShare = true; delay(1500); copiedShare = false }
                    },
                    onShare = { shareText(context, guestShareText) },
                    onSaveToClips = {
                        scope.launch {
                            savingDirect = true
                            val ok = vm.saveToClips(container.auth.validAccessToken())
                            savingDirect = false
                            if (ok) {
                                directSaved = true
                                delay(1800)
                                directSaved = false
                            }
                        }
                    },
                    onSaveLocal = {
                        scope.launch {
                            if (vm.saveToDevice()) {
                                savedLocal = true
                                delay(1800)
                                savedLocal = false
                            }
                        }
                    },
                    onLoginHintClick = { router.showLogin = true },
                )

                vm.error?.let { ErrorBox(homeErrorMessage(it)) }
                if (vm.noMeta) {
                    vm.metaReason?.let { WarnBox(it) }
                }
                if (vm.hasInput) {
                    Previews(vm, container.apiBase)
                }
            }

            // 메타를 읽는 동안 공룡이 화면 가장자리를 돈다.
            if (vm.loading) RunningDino()
        }
    }

    if (shareSheetOpen) {
        val url = shareUrl
        if (url != null) {
            ShareResultSheet(
                title = vm.resolvedTitle,
                description = vm.previewDescription,
                url = url,
                onSave = { vm.saveToClips(container.auth.validAccessToken()) },
                onDismiss = { shareSheetOpen = false },
            )
        }
    }

    // 로그인 상태가 바뀌면 이전 결과는 이 계정의 것이 아니다.
    LaunchedEffect(auth.loggedIn) {
        shareUrl = null
        shareSheetOpen = false
    }
}

@Composable
private fun Hero() {
    val i18n = LocalI18n.current
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // 강조 낱말을 자리표시자로 두는 건 웹과 같은 이유다 — 언어마다 위치가 달라서
        // 앞/뒤로 쪼개면 문장이 깨진다.
        Text(
            i18n.t(R.string.home_hero_title, i18n.t(R.string.home_hero_titleAccent)),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = AppColor.fg,
            textAlign = TextAlign.Center,
        )
        Text(
            i18n.t(R.string.home_hero_subtitle),
            fontSize = 14.sp,
            color = AppColor.fgMuted,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun FormCard(vm: HomeViewModel, onUrlChanged: (String) -> Unit) {
    val i18n = LocalI18n.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .background(AppColor.surface, ShapeMd)
            .border(1.dp, AppColor.border, ShapeMd)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Field(
            label = i18n.t(R.string.home_form_urlLabel),
            required = true,
            muted = if (vm.loading) i18n.t(R.string.home_metaLoading) else null,
            modifier = Modifier.tourAnchor(TourAnchor.URL),
        ) {
            AppTextField(
                value = vm.url,
                onValueChange = onUrlChanged,
                placeholder = i18n.t(R.string.home_form_urlPlaceholder),
                keyboardType = KeyboardType.Uri,
                trailing = {
                    if (vm.url.isNotEmpty()) {
                        IconButton(onClick = { onUrlChanged("") }) {
                            Icon(
                                Icons.Filled.Clear,
                                contentDescription = i18n.t(R.string.home_clearInputAria),
                                tint = AppColor.fgMuted,
                            )
                        }
                    }
                },
            )
        }

        Column(
            modifier = Modifier.tourAnchor(TourAnchor.OPTIONS),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Field(label = i18n.t(R.string.home_form_titleLabel), muted = i18n.t(R.string.home_form_titleNote)) {
                AppTextField(
                    value = vm.title,
                    onValueChange = { vm.title = it },
                    placeholder = i18n.t(R.string.home_form_titlePlaceholder),
                )
            }

            Field(label = i18n.t(R.string.home_form_tagsLabel), muted = i18n.t(R.string.home_form_tagsNote)) {
                AppTextField(
                    value = vm.tagInput,
                    onValueChange = { vm.tagInput = it },
                    placeholder = i18n.t(R.string.home_form_tagsPlaceholder),
                )
            }
        }

        if (vm.tags.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                vm.tags.forEach { TagChip(it) }
            }
        }
    }
}

@Composable
private fun Field(
    label: String,
    required: Boolean = false,
    muted: String? = null,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = AppColor.fg)
            if (required) Text("*", fontSize = 13.sp, color = AppColor.danger)
            if (muted != null) Text(muted, fontSize = 13.sp, color = AppColor.fgMuted)
        }
        content()
    }
}

/**
 * 입력칸.
 *
 * 글자·커서 색을 검정으로 고정한다 — 기본값에 맡기면 URL 칸이 시스템 tint(파랑)로 그려져
 * 링크처럼 보인다(iOS 에서 같은 문제를 고쳤다).
 */
@Composable
private fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    trailing: @Composable (() -> Unit)? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, fontSize = 15.sp, color = AppColor.fgMuted) },
        singleLine = true,
        trailingIcon = trailing,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = ShapeSm,
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = AppColor.fg,
            unfocusedTextColor = AppColor.fg,
            cursorColor = AppColor.fg,
            focusedContainerColor = AppColor.bg,
            unfocusedContainerColor = AppColor.bg,
            focusedBorderColor = AppColor.brand,
            unfocusedBorderColor = AppColor.border,
        ),
    )
}

@Composable
private fun Actions(
    loggedIn: Boolean,
    hasInput: Boolean,
    creating: Boolean,
    savingDirect: Boolean,
    savedLocal: Boolean,
    directSaved: Boolean,
    copiedLink: Boolean,
    copiedShare: Boolean,
    shareUrl: String?,
    onCreateShare: () -> Unit,
    onCopyShareLink: () -> Unit,
    onCopyOriginal: () -> Unit,
    onShare: () -> Unit,
    onSaveToClips: () -> Unit,
    onSaveLocal: () -> Unit,
    onLoginHintClick: () -> Unit,
) {
    val i18n = LocalI18n.current
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (loggedIn) {
                if (shareUrl != null) {
                    SecondaryButton(
                        label = i18n.t(if (copiedLink) R.string.homeActions_copied else R.string.homeActions_copyLink),
                        modifier = Modifier.weight(1f),
                        onClick = onCopyShareLink,
                    )
                } else {
                    SecondaryButton(
                        label = i18n.t(if (creating) R.string.homeActions_creating else R.string.homeActions_createLink),
                        modifier = Modifier.weight(1f).tourAnchor(TourAnchor.SHARE),
                        enabled = hasInput && !creating,
                        loading = creating,
                        onClick = onCreateShare,
                    )
                }
            } else {
                SecondaryButton(
                    label = i18n.t(R.string.homeActions_share),
                    modifier = Modifier.weight(1f).tourAnchor(TourAnchor.SHARE),
                    enabled = hasInput,
                    onClick = onShare,
                )
            }
            SecondaryButton(
                label = i18n.t(if (copiedShare) R.string.homeActions_copied else R.string.homeActions_copyOriginal),
                modifier = Modifier.weight(1f).tourAnchor(TourAnchor.COPY_ORIGINAL),
                enabled = hasInput,
                onClick = onCopyOriginal,
            )
        }

        if (loggedIn) {
            PrimaryButton(
                label = i18n.t(
                    when {
                        directSaved -> R.string.homeActions_saved
                        savingDirect -> R.string.homeActions_saving
                        else -> R.string.homeActions_saveToClips
                    }
                ),
                modifier = Modifier.tourAnchor(TourAnchor.SAVE),
                enabled = hasInput && !savingDirect,
                loading = savingDirect,
                onClick = onSaveToClips,
            )
        } else {
            PrimaryButton(
                label = i18n.t(if (savedLocal) R.string.homeActions_saved else R.string.homeActions_saveHere),
                modifier = Modifier.tourAnchor(TourAnchor.SAVE),
                enabled = hasInput,
                onClick = onSaveLocal,
            )
            GuestHint(onLoginHintClick)
        }
    }
}

/**
 * 게스트 안내 — `로그인` 낱말을 브랜드색으로 강조한다.
 *
 * 문장을 앞/뒤로 쪼개 사전에 넣으면 어순이 다른 언어에서 깨진다(영어는 `Sign in` 이 문장 맨
 * 앞이다). 사전에는 `%s` 가 든 온전한 문장 하나만 두고, 끼워 넣은 낱말을 기준으로 다시 쪼갠다.
 *
 * 탭 영역은 낱말이 아니라 **안내문 전체**다 — 12sp 글자 폭은 손가락으로 맞히기 어렵다.
 */
@Composable
private fun GuestHint(onClick: () -> Unit) {
    val i18n = LocalI18n.current
    val login = i18n.t(R.string.common_login)
    val full = i18n.t(R.string.homeActions_guestHint, login)
    Text(
        text = kr.co.clipnote.app.ui.components.emphasized(full, listOf(login), AppColor.brandStrong),
        fontSize = 12.sp,
        color = AppColor.fgMuted,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().clickableRow(onClick).padding(vertical = 4.dp),
    )
}

@Composable
private fun Previews(vm: HomeViewModel, apiBase: String) {
    val i18n = LocalI18n.current
    val title = vm.resolvedTitle.ifEmpty { i18n.t(R.string.home_preview_titlePlaceholder) }

    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
        verticalArrangement = Arrangement.spacedBy(40.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(i18n.t(R.string.home_cardPreview_title), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = AppColor.fg)
            Text(i18n.t(R.string.home_cardPreview_note), fontSize = 12.sp, color = AppColor.fgMuted)
            SharePreviewCard(
                title = title,
                description = vm.previewDescription,
                siteName = vm.previewSiteName,
                gradient = vm.gradient,
                imageUrl = vm.previewImage,
                apiBase = apiBase,
            )
            Text(i18n.t(R.string.home_cardPreview_caption), fontSize = 12.sp, color = AppColor.fgMuted)
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(i18n.t(R.string.home_clipPreview_title), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = AppColor.fg)
            Text(i18n.t(R.string.home_clipPreview_note), fontSize = 12.sp, color = AppColor.fgMuted)
            ClipCard(
                title = title,
                host = if (vm.hasInput) prettyHost(vm.url) else null,
                imageUrl = vm.previewImage,
                gradient = vm.gradient,
                tags = vm.tags,
                apiBase = apiBase,
            )
        }
    }
}

@Composable
private fun ErrorBox(text: String) {
    Text(
        text,
        fontSize = 14.sp,
        color = AppColor.danger,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
            .background(AppColor.danger.copy(alpha = 0.1f), ShapeSm)
            .padding(12.dp),
    )
}

@Composable
private fun WarnBox(text: String) {
    Text(
        "⚠️ $text",
        fontSize = 14.sp,
        color = AppColor.fg,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
            .background(AppColor.warning.copy(alpha = 0.1f), ShapeSm)
            .padding(12.dp),
    )
}

/**
 * 오류 케이스를 현재 언어의 문장으로.
 *
 * 서버가 준 문장은 그대로 쓴다 — 이미 사람이 읽는 말이고, 앱이 알 수 없는 사유를 담는다.
 */
@Composable
private fun homeErrorMessage(error: HomeError): String {
    val i18n = LocalI18n.current
    return when (error) {
        HomeError.MetaFailed -> i18n.t(R.string.home_errors_metaFailed)
        HomeError.TitleRequiredForLink -> i18n.t(R.string.home_errors_titleRequiredForLink)
        HomeError.TitleRequiredForClip -> i18n.t(R.string.home_errors_titleRequiredForClip)
        is HomeError.LinkCreateFailed -> error.server ?: i18n.t(R.string.home_errors_linkCreateFailed)
        is HomeError.Server -> error.message
    }
}

fun copyToClipboard(context: Context, text: String) {
    val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    manager.setPrimaryClip(ClipData.newPlainText("ClipNote", text))
}

fun shareText(context: Context, text: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, null))
}
