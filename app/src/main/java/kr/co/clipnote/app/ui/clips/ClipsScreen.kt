package kr.co.clipnote.app.ui.clips

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kr.co.clipnote.app.ClipsRefresh
import kr.co.clipnote.app.R
import kr.co.clipnote.app.i18n.LocalI18n
import kr.co.clipnote.app.ui.LocalContainer
import kr.co.clipnote.app.ui.LocalNav
import kr.co.clipnote.app.ui.Route
import kr.co.clipnote.app.ui.VmFactory
import kr.co.clipnote.app.ui.components.AppScaffold
import kr.co.clipnote.app.ui.components.ClipThumbnail
import kr.co.clipnote.app.ui.components.ConfirmEmphasis
import kr.co.clipnote.app.ui.components.ConfirmLayer
import kr.co.clipnote.app.ui.components.FilterChip
import kr.co.clipnote.app.ui.components.PrimaryButton
import kr.co.clipnote.app.ui.components.RunningDino
import kr.co.clipnote.app.ui.components.SecondaryButton
import kr.co.clipnote.app.ui.components.SpinnerLabel
import kr.co.clipnote.app.ui.components.TagChip
import kr.co.clipnote.app.ui.components.ToolbarTextButton
import kr.co.clipnote.app.ui.components.clickableRow
import kr.co.clipnote.app.ui.components.emphasized
import kr.co.clipnote.app.ui.home.copyToClipboard
import kr.co.clipnote.app.ui.openInBrowser
import kr.co.clipnote.app.ui.theme.AppColor
import kr.co.clipnote.app.ui.theme.ShapeMd
import kr.co.clipnote.app.ui.theme.ShapeSm
import kr.co.clipnote.core.clips.groupClipsByDate
import kr.co.clipnote.core.model.UClip
import kr.co.clipnote.core.theme.gradientNamed
import kr.co.clipnote.core.util.openableWebUrl
import kr.co.clipnote.core.util.prettyHost

/** 내 클립 목록 — 게스트는 이 기기, 로그인은 계정. 필터·편집·삭제·공유·바로가기·다중선택. */
@Composable
fun ClipsScreen() {
    val container = LocalContainer.current
    val i18n = LocalI18n.current
    val nav = LocalNav.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val auth by container.auth.state.collectAsState()

    val vm: ClipsViewModel = viewModel(
        factory = remember {
            VmFactory {
                ClipsViewModel(
                    api = container.api,
                    localClips = container.localClips,
                    shareBase = container.apiBase,
                    accessToken = { container.auth.validAccessToken() },
                )
            }
        },
    )

    var editing by remember { mutableStateOf<UClip?>(null) }
    var pendingDelete by remember { mutableStateOf<UClip?>(null) }
    var copiedId by remember { mutableStateOf<String?>(null) }
    var makingSharedId by remember { mutableStateOf<String?>(null) }

    var selectMode by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf(setOf<String>()) }
    var tagApplyOpen by remember { mutableStateOf(false) }
    var bulkDeleteOpen by remember { mutableStateOf(false) }
    var bulkBusy by remember { mutableStateOf(false) }

    fun exitSelect() {
        selectMode = false
        selected = emptySet()
    }

    LaunchedEffect(auth.loggedIn, auth.loading) {
        if (!auth.loading) {
            exitSelect()
            vm.load(auth.loggedIn)
        }
    }
    LaunchedEffect(Unit) {
        ClipsRefresh.events.collect { vm.reload() }
    }

    AppScaffold(
        title = if (selectMode) i18n.t(R.string.clips_selectedCount, selected.size)
        else i18n.t(R.string.common_myClips),
        // 뒤로가기는 시스템 제스처가 맡는다 — 상단은 iOS 와 같이 메뉴 자리로 둔다.
        showMenu = !selectMode,
        showAds = !selectMode,
        actions = {
            when {
                selectMode -> ToolbarTextButton(i18n.t(R.string.common_cancel)) { exitSelect() }
                auth.loggedIn && vm.clips?.isNotEmpty() == true ->
                    ToolbarTextButton(i18n.t(R.string.clips_select)) { selectMode = true }
            }
        },
        bottomBar = {
            if (selectMode) {
                BulkBar(
                    count = selected.size,
                    onApplyTags = { tagApplyOpen = true },
                    onDelete = { bulkDeleteOpen = true },
                )
            }
        },
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            val clips = vm.clips
            when {
                clips == null -> LoadingState()

                // 실패는 빈 목록보다 **먼저** 본다 — "저장한 클립이 없어요" 가 뜨면
                // 사용자는 클립이 날아간 줄 안다.
                vm.loadFailed -> LoadFailedState { scope.launch { vm.reload() } }

                clips.isEmpty() -> Column(modifier = Modifier.fillMaxSize()) {
                    // 계정 목록이 비어도 이 기기 클립은 남아 있을 수 있다(옮기기를 거절한 경우).
                    // 진입 줄을 여기에도 두지 않으면 그 클립에 닿을 길이 아예 없어진다.
                    if (vm.localOnlyCount > 0 && !selectMode) {
                        LocalEntryRow(vm.localOnlyCount, Modifier.padding(16.dp)) { nav.go(Route.LOCAL_CLIPS) }
                    }
                    EmptyState { nav.home() }
                }

                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (vm.localOnlyCount > 0 && !selectMode) {
                        item {
                            LocalEntryRow(vm.localOnlyCount) { nav.go(Route.LOCAL_CLIPS) }
                        }
                    }
                    if (vm.allTags.isNotEmpty()) {
                        item { FilterRow(vm) }
                    }
                    if (vm.filtered.isEmpty()) {
                        item {
                            Text(
                                i18n.t(R.string.clips_emptyForTag, vm.activeTag.orEmpty()),
                                fontSize = 14.sp,
                                color = AppColor.fgMuted,
                            )
                        }
                    }
                    // 저장 시각으로 묶어 머리글을 붙인다. 목록이 이미 최신순이라 그룹도 최신순이다.
                    groupClipsByDate(vm.filtered).forEach { group ->
                        item(key = "header-${group.bucket}") {
                            Text(
                                clipDateLabel(group.bucket),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = AppColor.fgMuted,
                                modifier = Modifier.padding(top = 8.dp),
                            )
                        }
                        items(group.clips, key = { it.id }) { clip ->
                            ClipRow(
                                clip = clip,
                                apiBase = container.apiBase,
                                selectMode = selectMode,
                                isSelected = clip.id in selected,
                                copied = copiedId == clip.id,
                                makingShared = makingSharedId == clip.id,
                                onToggle = {
                                    selected = if (clip.id in selected) selected - clip.id else selected + clip.id
                                },
                                onLongPress = {
                                    if (auth.loggedIn && !selectMode) {
                                        selectMode = true
                                        selected = setOf(clip.id)
                                    }
                                },
                                onEdit = { editing = clip },
                                onDelete = { pendingDelete = clip },
                                onCopyShare = {
                                    vm.shareText(clip)?.let { copyToClipboard(context, it) }
                                    scope.launch {
                                        copiedId = clip.id
                                        delay(1500)
                                        if (copiedId == clip.id) copiedId = null
                                    }
                                },
                                onMakeShared = {
                                    scope.launch {
                                        makingSharedId = clip.id
                                        vm.makeShared(clip)
                                        makingSharedId = null
                                    }
                                },
                                onOpen = {
                                    openableWebUrl(clip.url)?.let { openInBrowser(context, it) }
                                },
                            )
                        }
                    }
                }
            }

            if (bulkBusy) BlockingOverlay()
        }
    }

    editing?.let { clip ->
        EditClipSheet(
            initialTitle = clip.title,
            initialTags = clip.tags,
            onSubmit = { title, tags -> vm.saveEdit(clip, title, tags) },
            onDismiss = { editing = null },
        )
    }

    pendingDelete?.let { clip ->
        ConfirmLayer(
            title = i18n.t(R.string.clips_deleteTitle),
            message = emphasized(
                i18n.t(R.string.clips_deleteBody, clip.title),
                listOf(clip.title),
                AppColor.fg,
            ),
            confirmLabel = i18n.t(R.string.common_delete),
            cancelLabel = i18n.t(R.string.common_cancel),
            emphasis = ConfirmEmphasis.DESTRUCTIVE,
            onConfirm = {
                pendingDelete = null
                scope.launch { vm.delete(clip) }
            },
            onCancel = { pendingDelete = null },
            onDismissRequest = { pendingDelete = null },
        )
    }

    if (tagApplyOpen) {
        TagApplySheet(
            count = selected.size,
            onApply = { tags, mode ->
                scope.launch {
                    bulkBusy = true
                    vm.applyTags(selected, tags, mode)
                    bulkBusy = false
                    exitSelect()
                }
            },
            onDismiss = { tagApplyOpen = false },
        )
    }

    if (bulkDeleteOpen) {
        val count = i18n.t(R.string.clips_countUnit, selected.size)
        ConfirmLayer(
            title = i18n.t(R.string.clips_bulkDeleteTitle, count),
            message = AnnotatedString(i18n.t(R.string.clips_irreversible)),
            confirmLabel = i18n.t(R.string.common_delete),
            cancelLabel = i18n.t(R.string.common_cancel),
            emphasis = ConfirmEmphasis.DESTRUCTIVE,
            onConfirm = {
                bulkDeleteOpen = false
                scope.launch {
                    bulkBusy = true
                    vm.bulkDelete(selected)
                    bulkBusy = false
                    exitSelect()
                }
            },
            onCancel = { bulkDeleteOpen = false },
            onDismissRequest = { bulkDeleteOpen = false },
        )
    }
}

@Composable
private fun FilterRow(vm: ClipsViewModel) {
    val i18n = LocalI18n.current
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(i18n.t(R.string.clips_allTags), vm.activeTag == null) { vm.activeTag = null }
        vm.allTags.forEach { tag ->
            FilterChip(tag, vm.activeTag == tag) { vm.activeTag = tag }
        }
    }
}

/**
 * ‘이 기기에 남은 클립 3개 ›’.
 *
 * 옮기기를 거절했을 때 그 클립으로 가는 **유일한 길**이다. 다중선택 중에는 감춘다 —
 * 선택 대상이 아닌 줄이 목록에 섞이면 무엇이 선택되는지 흐려진다.
 */
@Composable
private fun LocalEntryRow(count: Int, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val i18n = LocalI18n.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(AppColor.surface, ShapeMd)
            .border(1.dp, AppColor.border, ShapeMd)
            .clickableRow(onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(Icons.Filled.PhoneAndroid, contentDescription = null, tint = AppColor.fgMuted, modifier = Modifier.size(16.dp))
        Text(
            i18n.t(R.string.clips_localEntry, i18n.t(R.string.clips_countUnit, count)),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = AppColor.fg,
            modifier = Modifier.weight(1f),
        )
        Text("›", fontSize = 16.sp, color = AppColor.fgMuted)
    }
}

@Composable
private fun BulkBar(count: Int, onApplyTags: () -> Unit, onDelete: () -> Unit) {
    val i18n = LocalI18n.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColor.bg)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SecondaryButton(i18n.t(R.string.clips_applyTags), modifier = Modifier.weight(1f), onClick = onApplyTags)
        PrimaryButton(
            label = i18n.t(R.string.clips_bulkDeleteButton, count),
            modifier = Modifier.weight(1f),
            enabled = count > 0,
            color = AppColor.danger,
            onClick = onDelete,
        )
    }
}

/**
 * 목록을 기다리는 동안.
 *
 * 무슨 일이 벌어지는지 알리는 건 글이고, 공룡은 기다리는 시간을 견딜 만하게 만드는 장식이라
 * 둘 다 둔다(홈과 같은 자리 — 화면 가장자리).
 */
@Composable
private fun LoadingState() {
    val i18n = LocalI18n.current
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(i18n.t(R.string.clips_loading), fontSize = 14.sp, color = AppColor.fgMuted)
        RunningDino()
    }
}

/**
 * 목록 조회가 실패했을 때. 빈 목록과 **다른 화면**이어야 한다.
 *
 * 문구가 "사라진 건 아니에요" 까지 말하는 이유는, 이 화면에서 사용자가 가장 먼저 떠올리는 게
 * "내 클립이 날아갔나" 이기 때문이다.
 */
@Composable
private fun LoadFailedState(onRetry: () -> Unit) {
    val i18n = LocalI18n.current
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(i18n.t(R.string.clips_loadFailed), fontSize = 15.sp, color = AppColor.fgMuted, textAlign = TextAlign.Center)
        PrimaryButton(i18n.t(R.string.clips_retry), modifier = Modifier.fillMaxWidth(0.6f), onClick = onRetry)
    }
}

@Composable
private fun EmptyState(onCreate: () -> Unit) {
    val i18n = LocalI18n.current
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(i18n.t(R.string.clips_empty), fontSize = 15.sp, color = AppColor.fgMuted)
        PrimaryButton(i18n.t(R.string.clips_emptyCta), modifier = Modifier.fillMaxWidth(0.6f), onClick = onCreate)
    }
}

@Composable
private fun BlockingOverlay() {
    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.25f)),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier.background(AppColor.surface, ShapeMd).padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(color = AppColor.brand)
        }
    }
}

/** 목록 카드 한 줄 — 썸네일·제목·호스트·태그 + ⋮ 메뉴 + 액션행(공유/바로가기). */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ClipRow(
    clip: UClip,
    apiBase: String,
    selectMode: Boolean,
    isSelected: Boolean,
    copied: Boolean,
    makingShared: Boolean,
    onToggle: () -> Unit,
    onLongPress: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onCopyShare: () -> Unit,
    onMakeShared: () -> Unit,
    onOpen: () -> Unit,
) {
    val i18n = LocalI18n.current
    val gradient = gradientNamed(clip.gradient, clip.title.ifEmpty { clip.url })
    var menuOpen by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColor.surface, ShapeMd)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) AppColor.brand else AppColor.border,
                shape = ShapeMd,
            )
            .combinedClickable(
                onClick = { if (selectMode) onToggle() },
                onLongClick = onLongPress,
            ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (selectMode) Checkbox(isSelected)
            ClipThumbnail(
                imageUrl = clip.image,
                gradient = gradient,
                apiBase = apiBase,
                modifier = Modifier.size(56.dp).clip(ShapeSm),
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    clip.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColor.fg,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    prettyHost(clip.url),
                    fontSize = 13.sp,
                    color = AppColor.fgMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (clip.tags.isNotEmpty()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(top = 2.dp),
                    ) {
                        clip.tags.take(4).forEach { TagChip(it, small = true) }
                    }
                }
            }
            if (!selectMode) {
                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = null, tint = AppColor.fgMuted)
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }, containerColor = AppColor.bg) {
                        DropdownMenuItem(
                            text = { Text(i18n.t(R.string.clips_edit), color = AppColor.fg) },
                            onClick = { menuOpen = false; onEdit() },
                        )
                        DropdownMenuItem(
                            text = { Text(i18n.t(R.string.common_delete), color = AppColor.danger) },
                            onClick = { menuOpen = false; onDelete() },
                        )
                    }
                }
            }
        }

        if (!selectMode) {
            HorizontalDivider(color = AppColor.border)
            Row(modifier = Modifier.fillMaxWidth().height(44.dp)) {
                if (!clip.local) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .clickableRow(if (clip.shared) onCopyShare else onMakeShared),
                        contentAlignment = Alignment.Center,
                    ) {
                        SpinnerLabel(
                            title = i18n.t(
                                when {
                                    clip.shared && copied -> R.string.clips_copied
                                    clip.shared -> R.string.clips_copyShareLink
                                    makingShared -> R.string.clips_creatingShareLink
                                    else -> R.string.clips_createShareLink
                                }
                            ),
                            loading = makingShared,
                            tint = AppColor.brandStrong,
                        )
                    }
                    androidx.compose.material3.VerticalDivider(color = AppColor.border, modifier = Modifier.height(24.dp).align(Alignment.CenterVertically))
                }
                Box(
                    modifier = Modifier.weight(1f).fillMaxSize().clickableRow(onOpen),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        i18n.t(R.string.clips_openOriginal),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AppColor.fg,
                    )
                }
            }
        }
    }
}

@Composable
private fun Checkbox(isSelected: Boolean) {
    Box(
        modifier = Modifier
            .size(24.dp)
            .background(if (isSelected) AppColor.brand else Color.Transparent, CircleShape)
            .border(2.dp, if (isSelected) AppColor.brand else AppColor.border, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (isSelected) {
            Icon(Icons.Filled.Check, contentDescription = null, tint = AppColor.white, modifier = Modifier.size(14.dp))
        }
    }
}
