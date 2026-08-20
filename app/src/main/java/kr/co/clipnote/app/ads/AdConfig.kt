package kr.co.clipnote.app.ads

import androidx.compose.ui.unit.dp
import kr.co.clipnote.app.BuildConfig

/**
 * AdMob 설정.
 *
 * App ID 는 매니페스트 meta-data 로만 들어가고(빌드 스크립트의 `admobAppId` placeholder),
 * 배너 unit 은 `BuildConfig` 로 온다. 디버그 빌드는 구글 공식 테스트 unit 을 쓴다 —
 * **실광고 자가 클릭은 계정 정지 사유다.**
 */
object AdConfig {
    /** 배너가 차지할 자리. 앵커 적응형은 기기 폭에 따라 50~90dp 로 변하니 여유를 둔다. */
    val bannerHeight = 64.dp

    val bannerUnitId: String get() = BuildConfig.ADMOB_BANNER_UNIT_ID

    /**
     * 광고를 켤지.
     *
     * unit ID 가 비어 있으면 SDK 가 빈 문자열로 `loadAd` 를 부르다 예외를 던진다. 배너는 홈
     * 첫 화면에 붙어 있어서 그러면 **켜자마자** 죽는다. 값이 없으면 광고만 끄고 앱은 살린다 —
     * 배너가 안 나오는 것보다 앱이 안 켜지는 게 훨씬 나쁘다(iOS 1.1.0 사고와 같은 방어).
     */
    val enabled: Boolean get() = bannerUnitId.isNotBlank()
}
