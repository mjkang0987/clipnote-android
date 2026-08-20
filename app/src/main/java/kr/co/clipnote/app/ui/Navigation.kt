package kr.co.clipnote.app.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.toArgb
import kr.co.clipnote.app.ui.theme.AppColor
import androidx.browser.customtabs.CustomTabColorSchemeParams

/** 상위 화면 라우트. 헤더 메뉴가 이동 대상으로 쓴다. */
object Route {
    const val HOME = "home"
    const val CLIPS = "clips"

    /** 이 기기에만 남은 클립 — 목록 위의 진입 줄에서만 들어간다. */
    const val LOCAL_CLIPS = "localClips"
    const val ABOUT = "about"
    const val FAQ = "faq"
    const val PRIVACY = "privacy"
    const val ACCOUNT_DELETE = "accountDelete"
    const val SETTINGS = "settings"
    const val ONBOARDING = "onboarding"
}

/**
 * 화면 밖에서 다뤄야 하는 UI 상태.
 *
 * 로그아웃 확인이 여기 있는 이유 — 부르는 곳이 헤더 메뉴와 설정 두 군데인데, 각자 레이어를
 * 달면 같은 확인이 두 벌 생긴다. 상태도 레이어도 한 곳에만 둔다.
 */
class AppRouter {
    var showLogin by mutableStateOf(false)
    var showTour by mutableStateOf(false)
    var confirmLogout by mutableStateOf(false)

    /** 공유 인텐트가 넘긴 URL — 홈 입력칸에 채우고 소비 후 null. */
    var pendingSharedUrl by mutableStateOf<String?>(null)
}

/**
 * 링크를 Custom Tabs 로 연다.
 *
 * 브라우저를 통째로 띄우지 않는 이유는 **돌아올 자리 때문**이다. OAuth 콜백이 딥링크로 앱에
 * 돌아와야 하는데, 외부 브라우저로 나가면 사용자가 직접 앱을 다시 찾아 들어와야 한다.
 * Custom Tabs 가 없는 기기에서는 일반 브라우저로 떨어진다.
 */
fun openInBrowser(context: Context, url: String) {
    val uri = Uri.parse(url)
    runCatching {
        CustomTabsIntent.Builder()
            .setShowTitle(true)
            .setDefaultColorSchemeParams(
                CustomTabColorSchemeParams.Builder()
                    .setToolbarColor(AppColor.bg.toArgb())
                    .build()
            )
            .build()
            .launchUrl(context, uri)
    }.onFailure {
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
    }
}
