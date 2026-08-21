package kr.co.clipnote.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import kr.co.clipnote.core.theme.ClipGradient

/**
 * 웹 `app/globals.css` 의 토큰과 **같은 값**. 값이 갈리면 세 화면이 다른 제품처럼 보인다.
 * 정의는 웹 `design-guide.md` §2 가 source of truth 다.
 */
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

/** 웹 가이드 §5 — `sm 8 · md 12 · lg 16 · xl 24 · full 9999`. */
object Radius {
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
    val full = 999.dp
}

val ShapeSm = RoundedCornerShape(Radius.sm)
val ShapeMd = RoundedCornerShape(Radius.md)
val ShapeLg = RoundedCornerShape(Radius.lg)
val ShapeFull = RoundedCornerShape(Radius.full)

/** 코어의 0xRRGGBB 정수를 Compose 색으로. */
fun Int.toComposeColor(): Color = Color(this or 0xFF000000.toInt())

/**
 * 웹의 `shadow-soft`
 * (`0 1px 2px rgba(0,0,0,.04), 0 8px 24px rgba(0,0,0,.06)`)에 해당하는 그림자.
 *
 * 두 겹을 그대로 옮길 수는 없다 — Compose 의 `shadow` 는 한 겹이고 CSS 처럼 흐림 반경과
 * 오프셋을 따로 주지 못한다. 넓게 퍼지는 쪽(0 8px 24px)이 눈에 보이는 층이라 그쪽을 맞추고,
 * 색을 아주 옅게 깔아 CSS 의 6% 알파에 가깝게 뒀다.
 *
 * **떠 있는 요소에만 쓴다**(가이드 §5). 목록의 모든 줄에 그림자를 주면 화면이 시끄러워진다.
 */
fun Modifier.softShadow(shape: Shape = ShapeMd): Modifier = shadow(
    elevation = 8.dp,
    shape = shape,
    clip = false,
    ambientColor = Color.Black.copy(alpha = 0.10f),
    spotColor = Color.Black.copy(alpha = 0.10f),
)

/**
 * 그라디언트를 **135도**로 칠한다(가이드 §3 "각도 기본 135deg").
 *
 * `Brush.linearGradient(colors)` 는 상자의 왼쪽 위 모서리에서 오른쪽 아래 모서리로 긋는데,
 * 그러면 각도가 상자 비율을 따라간다 — 공유 카드(1200:630)처럼 납작한 상자에서는 30도쯤으로
 * 누워 웹과 눈에 띄게 달라진다. 상자 비율과 무관하게 45도(=CSS 135deg)로 고정한다.
 */
fun ClipGradient.brush(size: Size): Brush {
    val colors = listOf(from.toComposeColor(), to.toComposeColor())
    // 45도 방향 단위 벡터. 그라디언트 선 길이 = |W·sin45| + |H·cos45|.
    val unit = 0.70710678f
    val half = unit * (size.width + size.height) / 2f
    val centerX = size.width / 2f
    val centerY = size.height / 2f
    return Brush.linearGradient(
        colors = colors,
        start = Offset(centerX - unit * half, centerY - unit * half),
        end = Offset(centerX + unit * half, centerY + unit * half),
    )
}

/** 배경을 브랜드 그라디언트로. 상자 크기를 알아야 각도를 고정할 수 있어 `drawBehind` 로 그린다. */
fun Modifier.clipGradient(gradient: ClipGradient): Modifier =
    drawBehind { drawRect(gradient.brush(size)) }

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
 * 라이트 팔레트만 쓴다.
 *
 * 웹은 `globals.css` 에 `prefers-color-scheme: dark` 토큰을 **임시로** 정의해 뒀지만
 * 가이드 §8 이 "MVP 는 라이트 기본, 정식 대응은 후순위" 라고 못 박았고 iOS 도 라이트 하나다.
 * 앱만 먼저 다크를 켜면 공유 카드(밝은 배경 위 그라디언트)와 화면이 어긋나 보인다 —
 * 넣으려면 셋을 같이 해야 한다.
 */
@Composable
fun ClipNoteTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ClipNoteColorScheme,
        typography = ClipNoteTypography,
    ) {
        // 화면이 크기만 지정해도 Pretendard 가 적용되도록 기본 스타일을 깔아 둔다.
        CompositionLocalProvider(LocalTextStyle provides BodyTextStyle, content = content)
    }
}
