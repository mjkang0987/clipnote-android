package kr.co.clipnote.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import kr.co.clipnote.app.R
import kotlin.math.roundToInt

private val FRAMES = intArrayOf(
    R.drawable.dino_run1,
    R.drawable.dino_run2,
    R.drawable.dino_run3,
    R.drawable.dino_run4,
)

/**
 * 기다리는 동안 화면 가장자리를 걸어 다니는 도트 공룡.
 *
 * 카드 테두리에 묶어 뒀더니 좁아서 갇힌 것처럼 보였다(iOS 에서 겪은 것과 같다). 화면 전체를
 * 상자로 삼으면 위·양옆 가장자리를 타고 다닌다. 오버레이라 자리를 차지하지 않아 스크롤·입력·
 * 배너 어느 것도 밀리지 않는다. 아래쪽 면은 걷지 않는다 — 배너와 겹친다.
 */
@Composable
fun RunningDino(modifier: Modifier = Modifier, size: Int = 28, periodMs: Int = 9000) {
    val configuration = LocalConfiguration.current
    val transition = rememberInfiniteTransition(label = "dino")

    // 0→1 이 한 바퀴. 위쪽 → 오른쪽 → (아래 건너뜀) → 왼쪽 순으로 돈다.
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(periodMs, easing = LinearEasing), RepeatMode.Restart),
        label = "lap",
    )
    val frame by transition.animateFloat(
        initialValue = 0f,
        targetValue = FRAMES.size.toFloat(),
        animationSpec = infiniteRepeatable(tween(520, easing = LinearEasing), RepeatMode.Restart),
        label = "frame",
    )

    val width = configuration.screenWidthDp - size
    val height = configuration.screenHeightDp - size

    // 세 변만 쓴다. 각 변에 길이에 비례한 시간을 준다 — 안 그러면 짧은 변에서 순간이동처럼 보인다.
    val top = width.toFloat()
    val side = height.toFloat()
    val total = top + side * 2
    val travelled = progress * total

    val (x, y, facingRight) = when {
        travelled < side -> Triple(0f, side - travelled, true)          // 왼쪽 벽을 타고 올라간다
        travelled < side + top -> Triple(travelled - side, 0f, true)    // 위쪽을 가로지른다
        else -> Triple(top, travelled - side - top, false)              // 오른쪽 벽을 타고 내려온다
    }

    Box(modifier = modifier.fillMaxSize()) {
        Image(
            painter = painterResource(FRAMES[frame.toInt().coerceIn(0, FRAMES.size - 1)]),
            contentDescription = null,
            filterQuality = FilterQuality.None,
            modifier = Modifier
                .offset(x = x.roundToInt().dp, y = y.roundToInt().dp)
                .size(size.dp)
                .graphicsLayer(scaleX = if (facingRight) 1f else -1f),
        )
    }
}
