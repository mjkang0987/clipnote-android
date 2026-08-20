package kr.co.clipnote.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kr.co.clipnote.app.ui.theme.AppColor
import kr.co.clipnote.app.ui.theme.ShapeSm

/**
 * 진행 중을 알리는 인라인 라벨.
 *
 * 글자만 "저장 중…" 으로 바꾸면 눌렸는지 아닌지 눈에 잘 안 띈다 — 움직이는 게 하나 있어야 한다.
 */
@Composable
fun SpinnerLabel(title: String, loading: Boolean, tint: Color) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (loading) {
            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = tint)
        }
        Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = tint)
    }
}

/**
 * 채운 보라 버튼.
 *
 * **한 화면에 하나뿐이다**(웹 `b824002` 규칙). 게스트 화면에서는 '이 기기에 저장',
 * 로그인 화면에서는 '내 클립에 저장' 이 그 자리다. 둘 이상이면 무엇이 주 동작인지 흐려진다.
 */
@Composable
fun PrimaryButton(
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    color: Color = AppColor.brand,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().height(48.dp),
        shape = ShapeSm,
        colors = ButtonDefaults.buttonColors(
            containerColor = color,
            contentColor = AppColor.white,
            disabledContainerColor = color.copy(alpha = 0.5f),
            disabledContentColor = AppColor.white.copy(alpha = 0.8f),
        ),
    ) {
        SpinnerLabel(label, loading, AppColor.white)
    }
}

/** 테두리 + 연보라. 링크·복사·공유가 모두 이걸 쓴다. */
@Composable
fun SecondaryButton(
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    onClick: () -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().height(48.dp),
        shape = ShapeSm,
        border = BorderStroke(1.dp, if (enabled) AppColor.brand else AppColor.border),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = AppColor.brandSoft,
            contentColor = AppColor.brandStrong,
            disabledContainerColor = AppColor.brandSoft.copy(alpha = 0.5f),
            disabledContentColor = AppColor.brandStrong.copy(alpha = 0.5f),
        ),
    ) {
        SpinnerLabel(label, loading, if (enabled) AppColor.brandStrong else AppColor.brandStrong.copy(alpha = 0.5f))
    }
}

/**
 * 취소 자리의 버튼.
 *
 * **레이어마다 같은 모양이어야 한다.** 화면마다 다르면 같은 자리의 버튼이 달라 보여
 * 어느 쪽이 취소인지 매번 다시 읽어야 한다.
 */
@Composable
fun GhostButton(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(46.dp)
            .background(AppColor.surface, ShapeSm)
            .border(1.dp, AppColor.border, ShapeSm)
            .clickableRow(onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = AppColor.fg)
    }
}
