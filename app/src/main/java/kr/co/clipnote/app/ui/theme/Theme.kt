package kr.co.clipnote.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kr.co.clipnote.core.theme.ClipGradient

/** 웹·iOS 와 같은 팔레트. 값이 갈리면 세 화면이 다른 제품처럼 보인다. */
object AppColor {
    val brand = Color(0xFF7C5CFC)
    val brandStrong = Color(0xFF5B3FE0)
    val brandSoft = Color(0xFFEFEBFF)
    val bg = Color(0xFFFFFFFF)
    val surface = Color(0xFFF7F7F9)
    val border = Color(0xFFE4E4E7)
    val fg = Color(0xFF18181B)
    val fgMuted = Color(0xFF71717A)
    val success = Color(0xFF16A34A)
    val danger = Color(0xFFDC2626)
    val warning = Color(0xFFD97706)
    val white = Color(0xFFFFFFFF)
}

object Radius {
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val full = 999.dp
}

val ShapeSm = RoundedCornerShape(Radius.sm)
val ShapeMd = RoundedCornerShape(Radius.md)
val ShapeFull = RoundedCornerShape(Radius.full)

/** 코어의 0xRRGGBB 정수를 Compose 색으로. */
fun Int.toComposeColor(): Color = Color(0xFF000000L.toInt() or this)

fun ClipGradient.brush(): Brush = Brush.linearGradient(
    colors = listOf(from.toComposeColor(), to.toComposeColor()),
)

private val ClipNoteColorScheme = lightColorScheme(
    primary = AppColor.brand,
    onPrimary = AppColor.white,
    secondary = AppColor.brandStrong,
    background = AppColor.bg,
    onBackground = AppColor.fg,
    surface = AppColor.bg,
    onSurface = AppColor.fg,
    surfaceVariant = AppColor.surface,
    onSurfaceVariant = AppColor.fgMuted,
    outline = AppColor.border,
    error = AppColor.danger,
)

/**
 * 라이트 팔레트 하나만 쓴다.
 *
 * 웹·iOS 도 다크 팔레트를 두지 않았다 — 공유 카드가 밝은 배경 위 그라디언트라, 앱만 어두우면
 * 화면에서 본 카드와 실제로 공유되는 카드가 달라 보인다. 다크를 넣으려면 세 곳을 같이 해야 한다.
 */
@Composable
fun ClipNoteTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = ClipNoteColorScheme, content = content)
}
