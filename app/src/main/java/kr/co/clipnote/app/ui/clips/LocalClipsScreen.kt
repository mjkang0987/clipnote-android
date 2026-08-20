package kr.co.clipnote.app.ui.clips

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kr.co.clipnote.app.ClipsRefresh
import kr.co.clipnote.app.R
import kr.co.clipnote.app.i18n.LocalI18n
import kr.co.clipnote.app.ui.LocalContainer
import kr.co.clipnote.app.ui.LocalNav
import kr.co.clipnote.app.ui.components.AppScaffold
import kr.co.clipnote.app.ui.components.ClipCard
import kr.co.clipnote.app.ui.components.ConfirmEmphasis
import kr.co.clipnote.app.ui.components.ConfirmLayer
import kr.co.clipnote.app.ui.components.GhostButton
import kr.co.clipnote.app.ui.components.PrimaryButton
import kr.co.clipnote.app.ui.components.clickableRow
import kr.co.clipnote.app.ui.components.emphasized
import kr.co.clipnote.app.ui.openInBrowser
import kr.co.clipnote.app.ui.theme.AppColor
import kr.co.clipnote.core.clips.groupClipsByDate
import kr.co.clipnote.core.model.UClip
import kr.co.clipnote.core.theme.gradientNamed
import kr.co.clipnote.core.util.openableWebUrl
import kr.co.clipnote.core.util.prettyHost

/**
 * 이 기기에만 남은 클립 — 로그인 상태에서 계정 목록과 **자리를 나눠** 보여 준다.
 *
 * **왜 한 목록에 합치지 않나.** 성격이 달라서다. 계정 클립은 다른 기기에서도 보이고 공유
 * 링크를 만들 수 있지만, 이 기기 클립은 둘 다 못 한다(slug 가 없다). 한 줄짜리 배지로
 * 구분하기엔 차이가 커서 — 눌러 보고 나서야 안 되는 걸 알게 된다 — 자리를 따로 두고,
 * 여기서만 할 수 있는 일(옮기기·모두 삭제)을 아래 바에 모은다.
 *
 * **옮기거나 지운 뒤에 자동으로 닫지 않는다.** 옮기기 결과 알림이 이 화면에 붙어 있어서
 * 곧바로 닫으면 알림이 뜨기도 전에 같이 사라진다. 빈 화면을 보여 주고 돌아가는 건 사용자에게 맡긴다.
 */
@Composable
fun LocalClipsScreen() {
    val container = LocalContainer.current
    val i18n = LocalI18n.current
    val nav = LocalNav.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val auth by container.auth.state.collectAsState()

    var clips by remember { mutableStateOf<List<UClip>?>(null) }
    var migrateOpen by remember { mutableStateOf(false) }
    var deleteAllOpen by remember { mutableStateOf(false) }

    suspend fun reload() {
        clips = container.localClips.all()
    }

    LaunchedEffect(Unit) { reload() }
    LaunchedEffect(Unit) { ClipsRefresh.events.collect { reload() } }
    // 로그아웃하면 이 클립들이 곧 기본 목록이 된다 — 따로 볼 화면이 아니게 되므로 닫는다.
    LaunchedEffect(auth.loggedIn) {
        if (!auth.loading && !auth.loggedIn) nav.back()
    }

    val list = clips

    AppScaffold(
        title = i18n.t(R.string.localClips_title),
        onBack = { nav.back() },
        bottomBar = {
            if (!list.isNullOrEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AppColor.bg)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    GhostButton(i18n.t(R.string.localClips_deleteAll), modifier = Modifier.weight(1f)) {
                        deleteAllOpen = true
                    }
                    PrimaryButton(i18n.t(R.string.localClips_move), modifier = Modifier.weight(1f)) {
                        migrateOpen = true
                    }
                }
            }
        },
    ) {
        if (list != null && list.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Text(i18n.t(R.string.localClips_empty), fontSize = 15.sp, color = AppColor.fgMuted)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Text(
                        i18n.t(R.string.localClips_intro),
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                        color = AppColor.fgMuted,
                    )
                }
                // 계정 목록과 같은 구성 — 저장 시각으로 묶어 머리글을 붙인다.
                groupClipsByDate(list.orEmpty()).forEach { group ->
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
                        ClipCard(
                            title = clip.title,
                            host = prettyHost(clip.url),
                            imageUrl = clip.image,
                            gradient = gradientNamed(clip.gradient, clip.title.ifEmpty { clip.url }),
                            tags = clip.tags,
                            apiBase = container.apiBase,
                            modifier = Modifier.clickableRow {
                                openableWebUrl(clip.url)?.let { openInBrowser(context, it) }
                            },
                        )
                    }
                }
            }
        }
    }

    if (migrateOpen) {
        MigrateLayer(
            pendingCount = list?.size ?: 0,
            onDismiss = { migrateOpen = false },
            onFinished = { scope.launch { reload() } },
        )
    }

    if (deleteAllOpen) {
        val count = i18n.t(R.string.clips_countUnit, list?.size ?: 0)
        ConfirmLayer(
            title = i18n.t(R.string.localClips_deleteAllTitle),
            message = emphasized(i18n.t(R.string.localClips_deleteAllBody, count), listOf(count), AppColor.fg),
            confirmLabel = i18n.t(R.string.common_delete),
            cancelLabel = i18n.t(R.string.common_cancel),
            emphasis = ConfirmEmphasis.DESTRUCTIVE,
            onConfirm = {
                deleteAllOpen = false
                scope.launch {
                    container.localClips.clear()
                    ClipsRefresh.emit()
                    reload()
                }
            },
            onCancel = { deleteAllOpen = false },
            onDismissRequest = { deleteAllOpen = false },
        )
    }
}
