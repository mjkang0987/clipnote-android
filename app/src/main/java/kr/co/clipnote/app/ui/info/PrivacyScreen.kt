package kr.co.clipnote.app.ui.info

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kr.co.clipnote.app.R
import kr.co.clipnote.app.i18n.AppLanguage
import kr.co.clipnote.app.i18n.LocalI18n
import kr.co.clipnote.app.ui.LocalNav
import kr.co.clipnote.app.ui.components.AppScaffold
import kr.co.clipnote.app.ui.theme.AppColor
import kr.co.clipnote.app.ui.theme.ShapeSm

/** 개인정보처리방침 — 앱에 내장한 정적 화면. 본문은 [PrivacyContent] 참고. */
@Composable
fun PrivacyScreen() {
    val i18n = LocalI18n.current
    val nav = LocalNav.current

    AppScaffold(title = i18n.t(R.string.common_privacy), onBack = { nav.back() }) {
        Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp),
        ) {
            Text(i18n.t(R.string.common_privacy), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = AppColor.fg)

            if (i18n.language != AppLanguage.KOREAN) {
                Text(
                    i18n.t(R.string.language_koreanOnlyNotice),
                    fontSize = 13.sp,
                    color = AppColor.fgMuted,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .background(NoticeBackground, ShapeSm)
                        .padding(10.dp),
                )
            }

            Text(
                PrivacyContent.EFFECTIVE_DATE,
                fontSize = 13.sp,
                color = AppColor.fgMuted,
                modifier = Modifier.padding(top = 4.dp),
            )
            Text(
                PrivacyContent.INTRO,
                fontSize = 14.sp,
                lineHeight = 22.sp,
                color = AppColor.fgMuted,
                modifier = Modifier.padding(top = 12.dp),
            )

            PrivacyContent.SECTIONS.forEach { section ->
                Column(
                    modifier = Modifier.padding(top = 22.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(section.heading, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = AppColor.fg)
                    section.body.forEach {
                        Text(it, fontSize = 14.sp, lineHeight = 22.sp, color = AppColor.fgMuted)
                    }
                }
            }

            Text(
                "© 2026 PIKAWORKS",
                fontSize = 12.sp,
                color = AppColor.fgMuted,
                modifier = Modifier.padding(top = 28.dp),
            )
        }
    }
}
