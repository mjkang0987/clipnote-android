package kr.co.clipnote.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import kr.co.clipnote.app.ui.theme.AppColor
import kr.co.clipnote.app.ui.theme.Radius
import kr.co.clipnote.app.ui.theme.ShapeMd

/** 확인 레이어의 강조. 웹이 레이어마다 다르게 준 무게를 그대로 옮긴다. */
enum class ConfirmEmphasis {
    /** 확인이 기본 동작 — 브랜드색으로 채운다(옮기기). */
    NORMAL,

    /** 되돌릴 수 없다 — 위험색으로 채운다(삭제). */
    DESTRUCTIVE,
}

/**
 * 확인 레이어 — 제목·본문·버튼 두 개.
 *
 * **왜 `AlertDialog` 가 아닌가.** 웹이 네이티브 `confirm()` 을 걷어내고 레이어로 바꿨고
 * (`c6c1d54`), iOS 도 맞췄다. 모양 때문만이 아니라 **동작 때문이다** — 옮기기 흐름은 바깥을
 * 눌러 닫은 것("나중에")과 취소를 누른 것("옮기지 않겠다")이 달라야 한다. 버튼을 직접 그리면
 * 그 구분이 선다.
 *
 * 높이는 내용에 맡긴다. 고정 높이로 두면 한국어보다 긴 번역(영어가 특히 길다)에서 아래가 잘린다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfirmLayer(
    title: String,
    message: AnnotatedString,
    confirmLabel: String,
    cancelLabel: String,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    onDismissRequest: () -> Unit,
    emphasis: ConfirmEmphasis = ConfirmEmphasis.NORMAL,
    busy: Boolean = false,
    busyLabel: String? = null,
) {
    val sheetState = rememberModalBottomSheetState(
        // 옮기는 중에 스와이프로 닫히면 무엇이 옮겨졌는지 알 수 없게 된다.
        confirmValueChange = { !busy },
    )
    ModalBottomSheet(
        onDismissRequest = { if (!busy) onDismissRequest() },
        sheetState = sheetState,
        containerColor = AppColor.surface,
        shape = RoundedCornerShape(topStart = Radius.lg, topEnd = Radius.lg),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(title, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = AppColor.fg)
            Text(message, fontSize = 14.sp, lineHeight = 21.sp, color = AppColor.fgMuted)
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                GhostButton(cancelLabel, modifier = Modifier.weight(1f), shape = ShapeMd) {
                    if (!busy) onCancel()
                }
                PrimaryButton(
                    label = if (busy) busyLabel ?: confirmLabel else confirmLabel,
                    modifier = Modifier.weight(1f),
                    enabled = !busy,
                    loading = busy,
                    color = if (emphasis == ConfirmEmphasis.DESTRUCTIVE) AppColor.danger else AppColor.brand,
                    shape = ShapeMd,
                    onClick = onConfirm,
                )
            }
        }
    }
}
