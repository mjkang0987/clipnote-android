package kr.co.clipnote.app.ui.info

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kr.co.clipnote.app.R
import kr.co.clipnote.app.i18n.LocalI18n
import kr.co.clipnote.app.ui.LocalNav
import kr.co.clipnote.app.ui.components.AppScaffold
import kr.co.clipnote.app.ui.theme.AppColor
import kr.co.clipnote.app.ui.theme.ShapeMd

/** 브랜드 로고 — 아이콘 + "ClipNote" 워드마크. */
@Composable
fun BrandLogo(size: Int = 20) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(R.drawable.brand_icon),
            contentDescription = null,
            modifier = Modifier.size((size + 4).dp).clip(RoundedCornerShape(((size + 4) * 0.22f).dp)),
        )
        Text(
            buildAnnotatedString {
                withStyle(SpanStyle(color = AppColor.fg)) { append("Clip") }
                withStyle(SpanStyle(color = AppColor.brand)) { append("Note") }
            },
            fontSize = size.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

/**
 * 소개 — 웹 홈 하단 소개 섹션과 같은 문구.
 *
 * 사용법 문단은 실제 버튼 이름(`homeActions_*`)을 끼워 넣는다. 문장에 버튼 이름을 직접 적으면
 * 버튼 라벨이 바뀔 때 안내만 옛 이름으로 남는다.
 */
@Composable
fun AboutScreen() {
    val i18n = LocalI18n.current
    val nav = LocalNav.current

    AppScaffold(title = i18n.t(R.string.menu_about), onBack = { nav.back() }) {
        Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp),
        ) {
            BrandLogo(24)
            Section("ClipNote${i18n.t(R.string.about_titleSuffix)}", modifier = Modifier.padding(top = 16.dp))
            Paragraph(i18n.t(R.string.about_body1))
            Paragraph(i18n.t(R.string.about_body2))
            Paragraph(i18n.t(R.string.about_body3))

            Section(i18n.t(R.string.about_howTitle), modifier = Modifier.padding(top = 24.dp))
            // 번호는 목록 표시라 사전에 넣지 않는다(번역 대상이 아니다).
            Step(1, i18n.t(R.string.about_how1))
            Step(
                2,
                i18n.t(
                    R.string.about_how2,
                    i18n.t(R.string.homeActions_createLink),
                    i18n.t(R.string.homeActions_copyLink),
                ),
            )
            Step(3, i18n.t(R.string.about_how3, i18n.t(R.string.homeActions_copyOriginal)))
            Step(
                4,
                i18n.t(
                    R.string.about_how4,
                    i18n.t(R.string.homeActions_saveToClips),
                    i18n.t(R.string.homeActions_saveHere),
                ),
            )

            // 로그인/게스트 비교는 로그인 화면과 같은 한 벌(`compare_*`)을 쓴다 — 전에는 두
            // 화면에 따로 적혀 있어 같은 뜻이 조금씩 다르게 갈렸다.
            CompareBoxes(modifier = Modifier.padding(top = 16.dp))
        }
    }
}

/** 로그인 / 게스트 비교. 로그인 화면과 소개 화면이 같은 것을 쓴다. */
@Composable
fun CompareBoxes(modifier: Modifier = Modifier) {
    val i18n = LocalI18n.current
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        InfoBox(
            title = i18n.t(R.string.compare_signedInTitle),
            accent = true,
            items = listOf(
                i18n.t(R.string.compare_signedInItem1, i18n.t(R.string.compare_signedInItem1Emphasis)),
                i18n.t(R.string.compare_signedInItem2),
                i18n.t(R.string.compare_signedInItem3, i18n.t(R.string.compare_signedInItem3Emphasis)),
            ),
        )
        InfoBox(
            title = i18n.t(R.string.compare_guestTitle),
            accent = false,
            items = listOf(
                i18n.t(R.string.compare_guestItem1),
                i18n.t(R.string.compare_guestItem2, i18n.t(R.string.common_myClips)),
                i18n.t(
                    R.string.compare_guestItem3,
                    i18n.t(R.string.compare_guestItem3Device),
                    i18n.t(R.string.compare_guestItem3NoLink),
                ),
            ),
        )
    }
}

@Composable
private fun InfoBox(title: String, accent: Boolean, items: List<String>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (accent) AppColor.brandSoft else AppColor.surface, ShapeMd)
            .border(1.dp, if (accent) AppColor.brand.copy(alpha = 0.35f) else AppColor.border, ShapeMd)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            title,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (accent) AppColor.brandStrong else AppColor.fg,
        )
        // 글머리표는 문구가 아니라 목록 표시라 사전에 넣지 않는다.
        items.forEach { Text("· $it", fontSize = 14.sp, lineHeight = 21.sp, color = AppColor.fgMuted) }
    }
}

/**
 * 자주 묻는 질문 — 웹 홈 하단 FAQ 와 **같은 6문항·같은 문구**.
 *
 * 답변에 버튼 이름이 나오는 문항은 실제 라벨을 끼워 넣는다. 문장에 직접 적으면 버튼 이름이
 * 바뀔 때 FAQ 만 옛 이름으로 남아 사용자가 화면에서 못 찾는다.
 */
@Composable
fun FaqScreen() {
    val i18n = LocalI18n.current
    val nav = LocalNav.current
    val copyLink = i18n.t(R.string.homeActions_copyLink)
    val copyOriginal = i18n.t(R.string.homeActions_copyOriginal)

    val items = listOf(
        i18n.t(R.string.faq_q1, copyLink, copyOriginal) to i18n.t(R.string.faq_a1, copyLink, copyOriginal),
        i18n.t(R.string.faq_q2) to i18n.t(R.string.faq_a2, i18n.t(R.string.common_myClips)),
        i18n.t(R.string.faq_q3) to i18n.t(R.string.faq_a3),
        i18n.t(R.string.faq_q4) to i18n.t(R.string.faq_a4),
        i18n.t(R.string.faq_q5) to i18n.t(R.string.faq_a5),
        i18n.t(R.string.faq_q6) to i18n.t(R.string.faq_a6),
    )

    AppScaffold(title = i18n.t(R.string.faq_title), onBack = { nav.back() }) {
        Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            items.forEach { (question, answer) ->
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(question, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = AppColor.fg)
                    Text(answer, fontSize = 14.sp, lineHeight = 22.sp, color = AppColor.fgMuted)
                }
            }
        }
    }
}

@Composable
private fun Section(text: String, modifier: Modifier = Modifier) {
    Text(text, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = AppColor.fg, modifier = modifier)
}

@Composable
private fun Paragraph(text: String) {
    Text(
        text,
        fontSize = 14.sp,
        lineHeight = 22.sp,
        color = AppColor.fgMuted,
        modifier = Modifier.padding(top = 10.dp),
    )
}

@Composable
private fun Step(number: Int, text: String) {
    Text(
        "$number. $text",
        fontSize = 14.sp,
        lineHeight = 21.sp,
        color = AppColor.fgMuted,
        modifier = Modifier.padding(top = 6.dp),
    )
}

/** 본문 배경(경계선 없는 은은한 블록) — 개인정보 화면의 안내문에 쓴다. */
internal val NoticeBackground: Color = AppColor.surface
