package kr.co.clipnote.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.AnnotatedString
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import kr.co.clipnote.app.i18n.LocalI18n
import kr.co.clipnote.app.ui.AppRouter
import kr.co.clipnote.app.ui.LocalContainer
import kr.co.clipnote.app.ui.LocalNav
import kr.co.clipnote.app.ui.LocalRouter
import kr.co.clipnote.app.ui.NavActions
import kr.co.clipnote.app.ui.Route
import kr.co.clipnote.app.ui.clips.ClipsScreen
import kr.co.clipnote.app.ui.clips.LocalClipsScreen
import kr.co.clipnote.app.ui.clips.MigrateLayer
import kr.co.clipnote.app.ui.components.ConfirmLayer
import kr.co.clipnote.app.ui.home.HomeScreen
import kr.co.clipnote.app.ui.info.AboutScreen
import kr.co.clipnote.app.ui.info.AccountDeleteScreen
import kr.co.clipnote.app.ui.info.FaqScreen
import kr.co.clipnote.app.ui.info.PrivacyScreen
import kr.co.clipnote.app.ui.login.LoginSheet
import kr.co.clipnote.app.ui.onboarding.OnboardingScreen
import kr.co.clipnote.app.ui.settings.SettingsScreen
import kr.co.clipnote.app.ui.theme.ClipNoteTheme
import kr.co.clipnote.core.auth.ShareDeepLink

/**
 * 앱의 유일한 Activity.
 *
 * `singleTask` 라 딥링크·공유 인텐트가 새 인스턴스를 만들지 않고 [onNewIntent] 로 들어온다 —
 * OAuth 콜백이 돌아왔을 때 홈이 초기화되면 입력하던 게 날아간다.
 */
class MainActivity : ComponentActivity() {
    private val container: AppContainer
        get() = (application as ClipNoteApplication).container

    /** 아직 화면에 전달하지 못한 인텐트. Compose 가 붙기 전에 도착할 수 있다. */
    private var pendingIntent by mutableStateOf<Intent?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        pendingIntent = intent

        setContent {
            ClipNoteTheme {
                CompositionLocalProvider(
                    LocalContainer provides container,
                    LocalI18n provides container.i18n,
                ) {
                    ClipNoteApp(
                        pendingIntent = pendingIntent,
                        onIntentConsumed = { pendingIntent = null },
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingIntent = intent
    }
}

@Composable
private fun ClipNoteApp(pendingIntent: Intent?, onIntentConsumed: () -> Unit) {
    val container = LocalContainer.current
    val i18n = LocalI18n.current
    val navController = rememberNavController()
    val router = remember { AppRouter() }
    val scope = rememberCoroutineScope()

    var onboardingSeen by remember { mutableStateOf(container.prefs.onboardingSeen) }
    var migratePrompt by remember { mutableStateOf<Int?>(null) }
    var migratePrompted by remember { mutableStateOf(false) }

    val auth by container.auth.state.collectAsState()

    val nav = remember(navController) {
        NavActions(
            go = { route ->
                // 같은 화면을 두 번 쌓지 않는다 — 메뉴에서 현재 화면을 다시 고르면 뒤로가기만 늘어난다.
                navController.navigate(route) { launchSingleTop = true }
            },
            home = { navController.popBackStack(Route.HOME, inclusive = false) },
            back = { navController.popBackStack() },
        )
    }

    // 인텐트 처리 — auth 딥링크는 인증이 삼키고, 공유는 홈 입력칸으로 간다.
    LaunchedEffect(pendingIntent) {
        val intent = pendingIntent ?: return@LaunchedEffect
        handleIntent(intent, container, router, nav)
        onIntentConsumed()
    }

    /**
     * 로그인 전환 감지 → 이 기기에 클립이 있으면 계정으로 옮길지 **권한다.**
     *
     * **거절해도 잃는 게 없다.** 로그인 목록 위에 진입 줄이 서고 거기서 언제든 다시 옮기거나
     * 지울 수 있다. 전에는 서버 것만 보여줘서 거절하면 그 클립을 볼 방법이 없어졌고 — 그래서
     * 거절이 곧 "그럼 지울까?" 로 이어졌다. 거절이 삭제를 뜻하면 그건 선택지가 아니다.
     */
    LaunchedEffect(auth.loggedIn) {
        if (!auth.loggedIn) {
            migratePrompted = false
            return@LaunchedEffect
        }
        if (migratePrompted) return@LaunchedEffect
        val count = container.localClips.count()
        if (count > 0) {
            migratePrompted = true
            migratePrompt = count
        }
    }

    CompositionLocalProvider(LocalRouter provides router, LocalNav provides nav) {
        if (!onboardingSeen) {
            OnboardingScreen {
                container.prefs.onboardingSeen = true
                onboardingSeen = true
            }
            return@CompositionLocalProvider
        }

        NavHost(navController = navController, startDestination = Route.HOME) {
            composable(Route.HOME) { HomeScreen() }
            composable(Route.CLIPS) { ClipsScreen() }
            composable(Route.LOCAL_CLIPS) { LocalClipsScreen() }
            composable(Route.ABOUT) { AboutScreen() }
            composable(Route.FAQ) { FaqScreen() }
            composable(Route.PRIVACY) { PrivacyScreen() }
            composable(Route.ACCOUNT_DELETE) { AccountDeleteScreen() }
            composable(Route.SETTINGS) { SettingsScreen() }
        }

        if (router.showLogin) {
            LoginSheet(onDismiss = { router.showLogin = false })
        }

        // 사용법 다시 보기 — 첫 실행 온보딩과 같은 것을 띄운다.
        if (router.showTour) {
            OnboardingScreen { router.showTour = false }
        }

        /**
         * 로그아웃 확인.
         *
         * 되돌릴 수 없는 일은 아니지만 다시 들어오려면 OAuth 를 한 번 더 거쳐야 해서 잘못
         * 누르면 성가시다. 본문이 "클립은 그대로 있다" 를 먼저 말하는 것도 그래서다 —
         * 이 화면에서 사용자가 가장 먼저 떠올리는 게 "지금 나가면 저장한 게 사라지나" 이다.
         */
        if (router.confirmLogout) {
            ConfirmLayer(
                title = i18n.t(R.string.logout_confirmTitle),
                message = AnnotatedString(i18n.t(R.string.logout_confirmBody)),
                confirmLabel = i18n.t(R.string.common_logout),
                cancelLabel = i18n.t(R.string.common_cancel),
                onConfirm = {
                    router.confirmLogout = false
                    scope.launch { container.auth.signOut() }
                },
                onCancel = { router.confirmLogout = false },
                onDismissRequest = { router.confirmLogout = false },
            )
        }

        migratePrompt?.let { count ->
            MigrateLayer(pendingCount = count, onDismiss = { migratePrompt = null })
        }
    }
}

/**
 * 인텐트를 화면 동작으로.
 *
 * 공유(`ACTION_SEND`)로 들어온 글에서 URL 만 뽑는다 — 카카오톡·유튜브 같은 앱은 "제목 https://…"
 * 처럼 문장을 통째로 넘겨서, 그대로 입력칸에 넣으면 메타 추출이 실패한다.
 */
private fun handleIntent(
    intent: Intent,
    container: AppContainer,
    router: AppRouter,
    nav: NavActions,
) {
    when (intent.action) {
        Intent.ACTION_VIEW -> {
            val data = intent.dataString ?: return
            if (container.auth.handleDeepLink(data)) return
            ShareDeepLink.parse(data)?.let {
                nav.home()
                router.pendingSharedUrl = it
            }
        }

        Intent.ACTION_SEND -> {
            val text = intent.getStringExtra(Intent.EXTRA_TEXT) ?: return
            extractUrl(text)?.let {
                nav.home()
                router.pendingSharedUrl = it
            }
        }
    }
}

/** 공유받은 글에서 첫 http(s) 주소를 뽑는다. 없으면 원문을 그대로 쓴다(사용자가 고칠 수 있게). */
internal fun extractUrl(text: String): String? {
    val trimmed = text.trim()
    if (trimmed.isEmpty()) return null
    val match = Regex("""https?://\S+""").find(trimmed)
    return match?.value ?: trimmed
}
