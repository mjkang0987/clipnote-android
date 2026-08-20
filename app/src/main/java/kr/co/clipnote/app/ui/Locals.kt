package kr.co.clipnote.app.ui

import androidx.compose.runtime.compositionLocalOf
import kr.co.clipnote.app.AppContainer

/** 화면이 저장소·API 에 닿는 통로. 생성은 `AppContainer` 한 곳에서만 한다. */
val LocalContainer = compositionLocalOf<AppContainer> {
    error("AppContainer 가 주입되지 않았다")
}

val LocalRouter = compositionLocalOf<AppRouter> {
    error("AppRouter 가 주입되지 않았다")
}

/**
 * 화면 이동.
 *
 * `NavController` 를 그대로 넘기지 않는 이유는, 화면이 할 수 있는 이동을 이 세 가지로 묶어
 * 두면 "여기서 어디로 갈 수 있나" 가 한눈에 보이기 때문이다.
 */
data class NavActions(
    val go: (String) -> Unit,
    val home: () -> Unit,
    val back: () -> Unit,
)

val LocalNav = compositionLocalOf<NavActions> {
    error("NavActions 가 주입되지 않았다")
}
