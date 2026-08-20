package kr.co.clipnote.app.ui.login

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kr.co.clipnote.app.R
import kr.co.clipnote.app.i18n.LocalI18n
import kr.co.clipnote.app.ui.LocalContainer
import kr.co.clipnote.app.ui.components.clickableRow
import kr.co.clipnote.app.ui.info.BrandLogo
import kr.co.clipnote.app.ui.info.CompareBoxes
import kr.co.clipnote.app.ui.openInBrowser
import kr.co.clipnote.app.ui.theme.AppColor
import kr.co.clipnote.app.ui.theme.ShapeFull
import kr.co.clipnote.app.ui.theme.ShapeMd
import kr.co.clipnote.core.auth.AuthErrorMessage
import kr.co.clipnote.core.model.AuthProvider

private const val PRIVACY_URL = "https://clipnote.co.kr/privacy"

/**
 * 로그인 — SNS 버튼 + 개인정보 동의 + 게스트 계속 + 최근 로그인 배지 + 안내.
 *
 * 동의 없이는 버튼이 동작하지 않는다. 브라우저로 나갔다 돌아오는 흐름이라, 동의를 나중에
 * 물으면 사용자가 이미 공급자 화면에 가 있는 상태가 된다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginSheet(onDismiss: () -> Unit) {
    val container = LocalContainer.current
    val i18n = LocalI18n.current
    val context = LocalContext.current
    val auth by container.auth.state.collectAsState()
    val lastError by container.auth.lastError.collectAsState()
    val naverCallbacks by container.auth.naverCallbacks.collectAsState()

    var agreed by remember { mutableStateOf(false) }
    var consentError by remember { mutableStateOf(false) }
    var loadingProvider by remember { mutableStateOf<String?>(null) }
    val lastProvider = remember { container.prefs.lastLoginProvider }

    // 로그인이 끝나면 닫는다.
    LaunchedEffect(auth.loggedIn) { if (auth.loggedIn) onDismiss() }
    // 네이버 콜백이 돌아왔거나 오류가 났으면 진행 표시를 끈다.
    LaunchedEffect(naverCallbacks, lastError) { loadingProvider = null }

    fun start(provider: AuthProvider) {
        if (!agreed) {
            consentError = true
            return
        }
        consentError = false
        loadingProvider = provider.id
        val url = if (provider == AuthProvider.NAVER) {
            container.auth.naverAuthorizeUrl()
        } else {
            container.auth.authorizeUrl(provider)
        }
        if (url == null) {
            loadingProvider = null
            return
        }
        openInBrowser(context, url)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = AppColor.bg,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BrandLogo(28)
            Text(i18n.t(R.string.common_login), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = AppColor.fg)
            Text(
                i18n.t(R.string.login_subtitleWithKakao),
                fontSize = 14.sp,
                color = AppColor.fgMuted,
                textAlign = TextAlign.Center,
            )

            ConsentBox(
                agreed = agreed,
                onToggle = {
                    agreed = !agreed
                    if (agreed) consentError = false
                },
                onPrivacy = { openInBrowser(context, PRIVACY_URL) },
            )

            // 공급자 이름은 라틴 표기로 고정한다 — 번역하면 사용자가 자기 계정을 못 알아본다.
            ProviderButton(
                provider = AuthProvider.GOOGLE,
                background = AppColor.bg,
                foreground = AppColor.fg,
                bordered = true,
                loading = loadingProvider == AuthProvider.GOOGLE.id,
                disabled = loadingProvider != null,
                recent = lastProvider == AuthProvider.GOOGLE.id && loadingProvider == null,
                onClick = { start(AuthProvider.GOOGLE) },
            )
            ProviderButton(
                provider = AuthProvider.KAKAO,
                background = Color(0xFFFEE500),
                foreground = Color(0xFF191600),
                bordered = false,
                loading = loadingProvider == AuthProvider.KAKAO.id,
                disabled = loadingProvider != null,
                recent = lastProvider == AuthProvider.KAKAO.id && loadingProvider == null,
                onClick = { start(AuthProvider.KAKAO) },
            )
            ProviderButton(
                provider = AuthProvider.NAVER,
                background = Color(0xFF03C75A),
                foreground = AppColor.white,
                bordered = false,
                loading = loadingProvider == AuthProvider.NAVER.id,
                disabled = loadingProvider != null,
                recent = lastProvider == AuthProvider.NAVER.id && loadingProvider == null,
                onClick = { start(AuthProvider.NAVER) },
            )

            if (consentError) {
                Text(i18n.t(R.string.login_errorConsent), fontSize = 13.sp, color = AppColor.danger)
            }
            lastError?.let { error ->
                Text(
                    when (error) {
                        AuthErrorMessage.NaverNotConfigured -> i18n.t(R.string.login_errorNaverNotConfigured)
                        // 시스템·Supabase 가 준 문장은 그대로 쓴다 — 앱이 알 수 없는 사유를 담는다.
                        is AuthErrorMessage.System -> error.text
                    },
                    fontSize = 13.sp,
                    color = AppColor.danger,
                    textAlign = TextAlign.Center,
                )
            }

            Divider()

            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text(
                    i18n.t(R.string.login_continueAsGuest),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColor.fgMuted,
                )
            }

            Text(
                i18n.t(R.string.login_compareTitle),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = AppColor.fgMuted,
            )
            CompareBoxes()
        }
    }
}

@Composable
private fun ConsentBox(agreed: Boolean, onToggle: () -> Unit, onPrivacy: () -> Unit) {
    val i18n = LocalI18n.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, AppColor.border, ShapeMd)
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .background(if (agreed) AppColor.brand else Color.Transparent, ShapeMd)
                .border(2.dp, if (agreed) AppColor.brand else AppColor.fgMuted, ShapeMd)
                .clickableRow(onToggle),
            contentAlignment = Alignment.Center,
        ) {
            if (agreed) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = AppColor.white, modifier = Modifier.size(14.dp))
            }
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                i18n.t(R.string.login_consent),
                fontSize = 13.sp,
                lineHeight = 19.sp,
                color = AppColor.fgMuted,
                modifier = Modifier.clickableRow(onToggle),
            )
            Text(
                i18n.t(R.string.login_privacyCheck),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = AppColor.brandStrong,
                modifier = Modifier.clickableRow(onPrivacy),
            )
        }
    }
}

@Composable
private fun ProviderButton(
    provider: AuthProvider,
    background: Color,
    foreground: Color,
    bordered: Boolean,
    loading: Boolean,
    disabled: Boolean,
    recent: Boolean,
    onClick: () -> Unit,
) {
    val i18n = LocalI18n.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .alpha(if (disabled && !loading) 0.5f else 1f)
            .background(background, ShapeMd)
            .then(if (bordered) Modifier.border(1.dp, AppColor.border, ShapeMd) else Modifier)
            .clickableRow { if (!disabled) onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            if (loading) i18n.t(R.string.login_redirecting)
            else i18n.t(R.string.login_continueWith, provider.displayName),
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = foreground,
        )
        if (recent) {
            Text(
                i18n.t(R.string.login_recent),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = AppColor.brandStrong,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 12.dp)
                    .background(AppColor.brandSoft, ShapeFull)
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            )
        }
    }
}

@Composable
private fun Divider() {
    val i18n = LocalI18n.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.weight(1f).height(1.dp).background(AppColor.border))
        Text(i18n.t(R.string.login_or), fontSize = 12.sp, color = AppColor.fgMuted)
        Box(modifier = Modifier.weight(1f).height(1.dp).background(AppColor.border))
    }
}
