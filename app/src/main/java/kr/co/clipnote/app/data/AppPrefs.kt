package kr.co.clipnote.app.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.serialization.encodeToString
import kr.co.clipnote.core.auth.AuthSession
import kr.co.clipnote.core.net.ClipNoteJson

/**
 * 앱 설정·세션 저장소.
 *
 * **세션을 평문 SharedPreferences 에 둔다.** 안드로이드의 앱별 저장소는 다른 앱이 읽지 못하고,
 * 루팅된 기기에서는 어떤 로컬 암호화도 결국 같은 기기의 키로 풀린다. `EncryptedSharedPreferences`
 * 는 그 위에 한 겹을 더 얹지만 지금은 의존성을 늘릴 만큼의 이득이 없다고 봤다 — 담기는 값은
 * 만료가 짧은 access token 과 refresh token 이고, 로그아웃·탈퇴에서 지운다.
 * (이 판단을 바꾸려면 `androidx.security:security-crypto` 로 이 클래스만 갈아 끼우면 된다.)
 */
class AppPrefs(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("clipnote", Context.MODE_PRIVATE)

    // MARK: - 온보딩

    var onboardingSeen: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDING_SEEN, false)
        set(value) = prefs.edit { putBoolean(KEY_ONBOARDING_SEEN, value) }

    // MARK: - 표시 언어

    var languageTag: String?
        get() = prefs.getString(KEY_LANGUAGE, null)
        set(value) = prefs.edit { putString(KEY_LANGUAGE, value) }

    // MARK: - 로그인

    /**
     * 지난 실행의 로그인 여부.
     *
     * 세션을 복원하기 전 첫 프레임에 이 값으로 그린다 — 없으면 홈 버튼이 게스트용으로 떴다가
     * 로그인용으로 튄다(iOS 에서 실제로 있었던 깜빡임).
     */
    var lastLoggedIn: Boolean
        get() = prefs.getBoolean(KEY_LAST_LOGGED_IN, false)
        set(value) = prefs.edit { putBoolean(KEY_LAST_LOGGED_IN, value) }

    /** 마지막으로 쓴 로그인 수단 — 로그인 화면의 '최근 로그인' 배지. */
    var lastLoginProvider: String?
        get() = prefs.getString(KEY_LAST_PROVIDER, null)
        set(value) = prefs.edit { putString(KEY_LAST_PROVIDER, value) }

    fun session(): AuthSession? {
        val raw = prefs.getString(KEY_SESSION, null) ?: return null
        return runCatching { ClipNoteJson.decodeFromString<AuthSession>(raw) }.getOrNull()
    }

    fun setSession(session: AuthSession?) {
        prefs.edit {
            if (session == null) remove(KEY_SESSION)
            else putString(KEY_SESSION, ClipNoteJson.encodeToString(session))
            putBoolean(KEY_LAST_LOGGED_IN, session != null)
        }
    }

    /** OAuth 왕복 동안만 살아 있는 PKCE verifier. 교환이 끝나면 지운다. */
    var pendingPkceVerifier: String?
        get() = prefs.getString(KEY_PKCE, null)
        set(value) = prefs.edit { putString(KEY_PKCE, value) }

    // MARK: - 태그 빈도

    fun knownTags(): Map<String, Int> {
        val raw = prefs.getString(KEY_KNOWN_TAGS, null) ?: return emptyMap()
        return runCatching {
            ClipNoteJson.decodeFromString<Map<String, Int>>(raw)
        }.getOrElse { emptyMap() }
    }

    fun setKnownTags(map: Map<String, Int>) {
        prefs.edit { putString(KEY_KNOWN_TAGS, ClipNoteJson.encodeToString(map)) }
    }

    private companion object {
        const val KEY_ONBOARDING_SEEN = "clipnote.onboardingSeen"
        const val KEY_LANGUAGE = "app.language"
        const val KEY_LAST_LOGGED_IN = "clipnote.auth.lastLoggedIn"
        const val KEY_LAST_PROVIDER = "clipnote.lastLoginProvider"
        const val KEY_SESSION = "clipnote.auth.session"
        const val KEY_PKCE = "clipnote.auth.pkceVerifier"
        const val KEY_KNOWN_TAGS = "clipnote.knownTags.v1"
    }
}
