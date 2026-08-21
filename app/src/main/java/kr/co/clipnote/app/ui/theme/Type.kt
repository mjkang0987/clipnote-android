package kr.co.clipnote.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import kr.co.clipnote.app.R

/**
 * Pretendard — 웹(`clipnote`)의 브랜드 서체.
 *
 * **왜 번들하나.** 웹은 `globals.css` 에서 Pretendard 를 불러오고 디자인 가이드가 이걸
 * 기본 서체로 못 박았다. 안드로이드 기본 한글 서체(Noto Sans KR)는 자소 폭·굵기 대비가
 * 달라서, 같은 문구를 나란히 놓으면 다른 제품처럼 보인다.
 *
 * **대가는 용량이다.** 한글 전 음절을 담아야 해서 굵기 하나가 1.5MB 고, 네 벌이면
 * 6MB 다(APK 에서는 압축돼 절반쯤). 줄이려면 굵기를 400/700 둘로 줄이는 방법이 있는데,
 * 그러면 Medium(500)·SemiBold(600) 자리가 합성 굵기로 흐려진다. `plan.md` 참고.
 *
 * iOS 는 시스템 서체를 쓴다 — 애플 플랫폼에서는 Pretendard 가 시스템 서체와 거의 같아서
 * 굳이 실을 이유가 없다. 안드로이드는 그 전제가 성립하지 않는다.
 */
val Pretendard = FontFamily(
    Font(R.font.pretendard_regular, FontWeight.Normal),
    Font(R.font.pretendard_medium, FontWeight.Medium),
    Font(R.font.pretendard_semibold, FontWeight.SemiBold),
    Font(R.font.pretendard_bold, FontWeight.Bold),
)

/**
 * 타이포 스케일.
 *
 * 웹 가이드의 스케일(display 40 / h1 28 / h2 22 / body 16 / small 14 / caption 12)을
 * 그대로 쓰지는 않는다 — 40px 히어로는 데스크톱 폭 기준이고 휴대폰에서는 두세 줄로 접힌다.
 * 웹의 **모바일 뷰**가 쓰는 값(히어로 24, 본문 15~16, 보조 13~14, 캡션 12)에 맞춘다.
 *
 * 여기서 정하는 건 기본값이라, 화면이 `fontSize` 를 직접 주면 그쪽이 이긴다.
 */
val ClipNoteTypography = Typography().let { base ->
    Typography(
        displayLarge = base.displayLarge.pretendard(),
        displayMedium = base.displayMedium.pretendard(),
        displaySmall = base.displaySmall.pretendard(),
        headlineLarge = base.headlineLarge.pretendard(),
        headlineMedium = base.headlineMedium.pretendard(),
        headlineSmall = base.headlineSmall.pretendard(),
        titleLarge = base.titleLarge.pretendard(),
        titleMedium = base.titleMedium.pretendard(),
        titleSmall = base.titleSmall.pretendard(),
        bodyLarge = base.bodyLarge.pretendard(),
        bodyMedium = base.bodyMedium.pretendard(),
        bodySmall = base.bodySmall.pretendard(),
        labelLarge = base.labelLarge.pretendard(),
        labelMedium = base.labelMedium.pretendard(),
        labelSmall = base.labelSmall.pretendard(),
    )
}

private fun TextStyle.pretendard(): TextStyle = copy(fontFamily = Pretendard)

/** 본문 기본 — 화면이 크기를 따로 주지 않을 때 쓰인다. */
val BodyTextStyle = TextStyle(fontFamily = Pretendard, fontSize = 15.sp, lineHeight = 23.sp)
