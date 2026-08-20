package kr.co.clipnote.app

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kr.co.clipnote.app.auth.AuthStore
import kr.co.clipnote.app.data.AppDatabase
import kr.co.clipnote.app.data.AppPrefs
import kr.co.clipnote.app.data.LocalClipStore
import kr.co.clipnote.app.i18n.LocalizationStore
import kr.co.clipnote.core.auth.SupabaseAuth
import kr.co.clipnote.core.net.ApiClient
import okhttp3.OkHttpClient

/**
 * 앱이 오래 들고 가는 것들을 한 자리에 모은다.
 *
 * DI 프레임워크를 쓰지 않는다 — 여기 있는 게 여섯 개고, 그 여섯이 서로를 어떻게 아는지가
 * 이 파일 하나로 다 보인다. 컨테이너를 [ClipNoteApplication] 이 만들고 화면은 넘겨받는다.
 */
class AppContainer(context: Context) {
    private val http: OkHttpClient = OkHttpClient.Builder().build()

    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val prefs = AppPrefs(context)

    val apiBase: String = BuildConfig.API_BASE

    val api = ApiClient(apiBase, http)

    val localClips = LocalClipStore(AppDatabase.get(context).localClips(), prefs)

    val i18n = LocalizationStore(context, prefs)

    val auth = AuthStore(
        supabase = SupabaseAuth.createOrNull(BuildConfig.SUPABASE_URL, BuildConfig.SUPABASE_ANON_KEY, http),
        prefs = prefs,
        scope = scope,
        naverClientId = BuildConfig.NAVER_CLIENT_ID,
    )
}
