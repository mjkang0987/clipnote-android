package kr.co.clipnote.app.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kr.co.clipnote.app.R
import kr.co.clipnote.app.i18n.AppLanguage
import kr.co.clipnote.app.i18n.LocalI18n
import kr.co.clipnote.app.ui.LocalContainer
import kr.co.clipnote.app.ui.LocalNav
import kr.co.clipnote.app.ui.LocalRouter
import kr.co.clipnote.app.ui.Route
import kr.co.clipnote.app.ui.components.AppScaffold
import kr.co.clipnote.app.ui.info.GuardView
import kr.co.clipnote.app.ui.theme.AppColor
import kr.co.clipnote.app.ui.theme.ShapeMd

/**
 * 설정 — 표시 언어·계정 정보·로그아웃·개인정보처리방침·문의·회원 탈퇴.
 *
 * 계정 항목은 로그인 사용자 전용이지만 **표시 언어는 로그인과 무관하게 항상 바꿀 수 있다** —
 * 게스트가 언어를 못 고르면 안 되므로 가드 화면에도 같은 행을 노출한다.
 */
@Composable
fun SettingsScreen() {
    val container = LocalContainer.current
    val i18n = LocalI18n.current
    val nav = LocalNav.current
    val router = LocalRouter.current
    val context = LocalContext.current
    val auth by container.auth.state.collectAsState()

    /** 문의 메일 주소 — 본문 안내와 mailto 링크가 같은 값을 쓰도록 한 곳에 둔다. */
    val contactEmail = "pikaworks.help@gmail.com"

    AppScaffold(title = i18n.t(R.string.common_settings), onBack = { nav.back() }) {
        Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp),
        ) {
            if (!auth.loggedIn) {
                // 언어는 로그인 없이도 바꿀 수 있어야 한다.
                LanguageRow()
                GuardView { nav.home() }
            } else {
                Text(i18n.t(R.string.settings_title), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = AppColor.fg)
                Text(
                    i18n.t(R.string.settings_subtitle),
                    fontSize = 14.sp,
                    color = AppColor.fgMuted,
                    modifier = Modifier.padding(top = 4.dp),
                )

                // 계정 이름은 이메일 > 공급자 이름 순. 네이버는 이메일을 주지 않는다.
                // 둘 다 없을 때만 사전의 대체 문구를 쓴다 — 여기에 "소셜" 이 오면 이름 자리에
                // 분류명이 들어가 어색해진다.
                val account = auth.account
                val accountLabel = account?.email ?: account?.providerName ?: i18n.t(R.string.settings_accountFallback)
                val providerName = account?.providerName ?: i18n.t(R.string.settings_providerUnknown)

                Column(modifier = Modifier.padding(top = 24.dp)) {
                    HorizontalDivider(color = AppColor.border)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                accountLabel,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = AppColor.fg,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                i18n.t(R.string.settings_signedInWith, providerName),
                                fontSize = 13.sp,
                                color = AppColor.fgMuted,
                                maxLines = 1,
                            )
                        }
                        TextButton(onClick = { router.confirmLogout = true }) {
                            Text(i18n.t(R.string.common_logout), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = AppColor.fgMuted)
                        }
                    }
                }

                LanguageRow()

                SettingsRow(i18n.t(R.string.common_privacy), i18n.t(R.string.settings_viewLink)) {
                    nav.go(Route.PRIVACY)
                }

                SettingsRow(i18n.t(R.string.settings_contact), i18n.t(R.string.settings_contactAction)) {
                    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$contactEmail"))
                    runCatching { context.startActivity(intent) }
                }
                Text(
                    i18n.t(R.string.settings_contactNote, contactEmail),
                    fontSize = 12.sp,
                    color = AppColor.fgMuted,
                    modifier = Modifier.padding(bottom = 4.dp),
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                        .background(AppColor.danger.copy(alpha = 0.05f), ShapeMd)
                        .border(1.dp, AppColor.danger.copy(alpha = 0.3f), ShapeMd)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(i18n.t(R.string.settings_dangerTitle), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = AppColor.danger)
                    Text(i18n.t(R.string.settings_dangerBody), fontSize = 14.sp, lineHeight = 21.sp, color = AppColor.fgMuted)
                    TextButton(onClick = { nav.go(Route.ACCOUNT_DELETE) }, contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
                        Text(i18n.t(R.string.settings_withdraw), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = AppColor.danger)
                    }
                }
            }
        }
    }
}

/**
 * 표시 언어.
 *
 * 고르면 **앱을 다시 켜지 않고** 화면 문자열이 바뀐다. 각 언어는 그 언어로 표기한다.
 */
@Composable
private fun LanguageRow() {
    val i18n = LocalI18n.current
    var open by remember { mutableStateOf(false) }

    Column {
        HorizontalDivider(color = AppColor.border)
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                i18n.t(R.string.settings_language),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = AppColor.fg,
                modifier = Modifier.weight(1f),
            )
            Box {
                TextButton(onClick = { open = true }) {
                    Text("${i18n.language.label} ›", fontSize = 14.sp, color = AppColor.fgMuted)
                }
                DropdownMenu(expanded = open, onDismissRequest = { open = false }, containerColor = AppColor.bg) {
                    AppLanguage.entries.forEach { language ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    language.label,
                                    color = if (language == i18n.language) AppColor.brandStrong else AppColor.fg,
                                )
                            },
                            onClick = {
                                open = false
                                i18n.select(language)
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsRow(title: String, action: String, onClick: () -> Unit) {
    Column {
        HorizontalDivider(color = AppColor.border)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = AppColor.fg,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onClick) {
                Text(action, fontSize = 14.sp, color = AppColor.fgMuted)
            }
        }
    }
}
