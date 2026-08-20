package kr.co.clipnote.app.ads

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

/**
 * 앵커 적응형 배너.
 *
 * 설정이 없으면 **아무것도 그리지 않는다** — 빈 자리를 남겨 두면 광고를 끈 빌드에서 화면
 * 아래가 이유 없이 비어 보인다.
 */
@Composable
fun AdBanner(modifier: Modifier = Modifier) {
    if (!AdConfig.enabled) return
    val widthDp = LocalConfiguration.current.screenWidthDp

    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        AndroidView(
            modifier = Modifier.fillMaxWidth().height(AdConfig.bannerHeight),
            factory = { context -> createAdView(context, widthDp) },
        )
    }
}

private fun createAdView(context: Context, widthDp: Int): AdView = AdView(context).apply {
    setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, widthDp))
    adUnitId = AdConfig.bannerUnitId
    loadAd(AdRequest.Builder().build())
}
