package kr.co.clipnote.app

import android.app.Application
import com.google.android.gms.ads.MobileAds
import kr.co.clipnote.app.ads.AdConfig

class ClipNoteApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)

        // 설정이 없는 빌드에서는 SDK 를 시작하지 않는다. 초기화 자체가 매니페스트의 App ID 를
        // 검증하고, 값이 이상하면 앱을 끝낸다 — iOS 1.1.0 이 실행 즉시 죽은 이유가 그거였다.
        if (AdConfig.enabled) {
            MobileAds.initialize(this)
        }
    }
}
