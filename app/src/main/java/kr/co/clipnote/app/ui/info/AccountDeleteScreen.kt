package kr.co.clipnote.app.ui.info

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
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
import kr.co.clipnote.app.ui.components.ConfirmEmphasis
import kr.co.clipnote.app.ui.components.ConfirmLayer
import kr.co.clipnote.app.ui.components.PrimaryButton
import kr.co.clipnote.app.ui.components.clickableRow
import kr.co.clipnote.app.ui.theme.AppColor
import kr.co.clipnote.app.ui.theme.ShapeMd

/**
 * 회원 탈퇴 — 계정과 저장된 모든 클립을 영구 삭제.
 *
 * 삭제는 서버(`DELETE /api/account`)가 하고, 성공하면 로컬 세션과 이 기기 클립을 비운다.
 * 확인을 두 번 받는다(체크박스 + 레이어) — 되돌릴 수 없는 유일한 동작이다.
 */
@Composable
fun AccountDeleteScreen() {
    val container = LocalContainer.current
    val i18n = LocalI18n.current
    val nav = LocalNav.current
    val scope = rememberCoroutineScope()
    val auth by container.auth.state.collectAsState()

    var agreed by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var confirmOpen by remember { mutableStateOf(false) }
    var doneOpen by remember { mutableStateOf(false) }

    AppScaffold(title = i18n.t(R.string.settings_withdraw), onBack = { nav.back() }) {
        if (!auth.loggedIn && !doneOpen) {
            GuardView { nav.home() }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp),
            ) {
                Text(i18n.t(R.string.settings_withdraw), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = AppColor.fg)
                Text(
                    i18n.t(R.string.settings_withdrawBody),
                    fontSize = 14.sp,
                    lineHeight = 21.sp,
                    color = AppColor.fgMuted,
                    modifier = Modifier.padding(top = 8.dp),
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                        .background(AppColor.surface, ShapeMd)
                        .border(1.dp, AppColor.border, ShapeMd)
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    // 글머리표는 목록 표시라 사전에 넣지 않는다.
                    Text("• ${i18n.t(R.string.settings_withdrawItemAccount)}", fontSize = 14.sp, color = AppColor.fgMuted)
                    Text("• ${i18n.t(R.string.settings_withdrawItemClips)}", fontSize = 14.sp, color = AppColor.fgMuted)
                    Text("• ${i18n.t(R.string.settings_withdrawItemLocal)}", fontSize = 14.sp, color = AppColor.fgMuted)
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                        .clickableRow { agreed = !agreed },
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .background(if (agreed) AppColor.danger else Color.Transparent, RoundedCornerShape(5.dp))
                            .border(2.dp, if (agreed) AppColor.danger else AppColor.border, RoundedCornerShape(5.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (agreed) {
                            Icon(Icons.Filled.Check, contentDescription = null, tint = AppColor.white, modifier = Modifier.size(14.dp))
                        }
                    }
                    Text(i18n.t(R.string.settings_withdrawAgree), fontSize = 14.sp, color = AppColor.fg)
                }

                error?.let {
                    Text(it, fontSize = 14.sp, color = AppColor.danger, modifier = Modifier.padding(top = 12.dp))
                }

                PrimaryButton(
                    label = i18n.t(R.string.settings_withdraw),
                    modifier = Modifier.padding(top = 20.dp),
                    enabled = agreed && !busy,
                    loading = busy,
                    color = AppColor.danger,
                ) { confirmOpen = true }

                TextButton(onClick = { nav.back() }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    Text(i18n.t(R.string.common_cancel), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = AppColor.fgMuted)
                }
            }
        }
    }

    if (confirmOpen) {
        ConfirmLayer(
            title = i18n.t(R.string.settings_withdrawTitle),
            message = AnnotatedString(i18n.t(R.string.settings_dangerBody)),
            confirmLabel = i18n.t(R.string.settings_withdraw),
            cancelLabel = i18n.t(R.string.common_cancel),
            emphasis = ConfirmEmphasis.DESTRUCTIVE,
            onConfirm = {
                confirmOpen = false
                scope.launch {
                    busy = true
                    error = null
                    val result = container.api.deleteAccount(container.auth.validAccessToken())
                    if (!result.ok) {
                        busy = false
                        error = i18n.t(
                            if (result.error == "network") R.string.settings_withdrawNetworkFailed
                            else R.string.settings_withdrawFailed
                        )
                        return@launch
                    }
                    // 서버 삭제가 끝났으니 이 기기에 남은 것도 정리한다.
                    container.localClips.clear()
                    container.auth.signOut()
                    ClipsRefresh.emit()
                    busy = false
                    doneOpen = true
                }
            },
            onCancel = { confirmOpen = false },
            onDismissRequest = { confirmOpen = false },
        )
    }

    if (doneOpen) {
        AlertDialog(
            onDismissRequest = { doneOpen = false; nav.home() },
            title = { Text(i18n.t(R.string.settings_withdrawDoneTitle), color = AppColor.fg) },
            text = { Text(i18n.t(R.string.settings_withdrawDoneBody), color = AppColor.fgMuted) },
            confirmButton = {
                TextButton(onClick = { doneOpen = false; nav.home() }) {
                    Text(i18n.t(R.string.common_confirm), color = AppColor.brandStrong)
                }
            },
            containerColor = AppColor.bg,
        )
    }
}

/** 로그인해야 볼 수 있는 화면의 안내. 설정·탈퇴가 같은 것을 쓴다. */
@Composable
fun GuardView(onHome: () -> Unit) {
    val i18n = LocalI18n.current
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(i18n.t(R.string.settings_guardBody), fontSize = 15.sp, color = AppColor.fgMuted)
        TextButton(onClick = onHome) {
            Text(i18n.t(R.string.settings_guardHome), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = AppColor.brandStrong)
        }
    }
}
