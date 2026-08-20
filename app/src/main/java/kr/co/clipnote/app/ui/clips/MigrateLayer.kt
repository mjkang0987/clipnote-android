package kr.co.clipnote.app.ui.clips

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import kr.co.clipnote.app.ClipsRefresh
import kr.co.clipnote.app.R
import kr.co.clipnote.app.i18n.LocalI18n
import kr.co.clipnote.app.ui.LocalContainer
import kr.co.clipnote.app.ui.components.ConfirmLayer
import kr.co.clipnote.app.ui.components.emphasized
import kr.co.clipnote.app.ui.theme.AppColor

/**
 * 이 기기의 클립을 계정으로 옮기는 흐름 — 확인 레이어·진행 표시·결과 알림 한 벌.
 *
 * **왜 따로 두나.** 부르는 곳이 둘이다 — 로그인 직후 한 번 권하는 자리와 '이 기기에 남은 클립'
 * 화면의 옮기기 버튼. 같은 문구·같은 결과 처리를 두 벌 두면 한쪽만 고쳐지는 일이 생긴다
 * (웹에서 실제로 그랬다).
 */
@Composable
fun MigrateLayer(
    pendingCount: Int,
    onDismiss: () -> Unit,
    onFinished: (Boolean) -> Unit = {},
) {
    val container = LocalContainer.current
    val i18n = LocalI18n.current
    val scope = rememberCoroutineScope()

    var migrating by remember { mutableStateOf(false) }
    var resultMessage by remember { mutableStateOf<String?>(null) }

    val count = i18n.t(R.string.clips_countUnit, pendingCount)

    if (resultMessage == null) {
        ConfirmLayer(
            title = i18n.t(R.string.clips_migrateTitle),
            message = emphasized(i18n.t(R.string.clips_migrateBody, count), listOf(count), AppColor.fg),
            confirmLabel = i18n.t(R.string.clips_migrateConfirm, count),
            cancelLabel = i18n.t(R.string.common_cancel),
            busy = migrating,
            busyLabel = i18n.t(R.string.clips_migrating),
            onConfirm = {
                if (migrating) return@ConfirmLayer
                scope.launch {
                    migrating = true
                    val result = MigrateLocalClips(container.api, container.localClips)
                        .run(container.auth.validAccessToken())
                    migrating = false
                    // 결과는 결정이 아니라 알림이라 레이어로 만들지 않는다.
                    resultMessage = if (result.allOk) {
                        i18n.t(R.string.clips_migrateDone, i18n.t(R.string.clips_countUnit, result.uploaded))
                    } else {
                        i18n.t(R.string.clips_migratePartial)
                    }
                    ClipsRefresh.emit()
                    onFinished(result.allOk)
                }
            },
            onCancel = onDismiss,
            onDismissRequest = onDismiss,
        )
    } else {
        AlertDialog(
            onDismissRequest = { resultMessage = null; onDismiss() },
            title = { Text(i18n.t(R.string.clips_migrateResultTitle), color = AppColor.fg) },
            text = { Text(resultMessage.orEmpty(), color = AppColor.fgMuted) },
            confirmButton = {
                TextButton(onClick = { resultMessage = null; onDismiss() }) {
                    Text(i18n.t(R.string.common_confirm), color = AppColor.brandStrong)
                }
            },
            containerColor = AppColor.bg,
        )
    }
}
