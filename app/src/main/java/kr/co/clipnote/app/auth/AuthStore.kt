package kr.co.clipnote.app.auth

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kr.co.clipnote.app.data.AppPrefs
import kr.co.clipnote.core.auth.AccountInfo
import kr.co.clipnote.core.auth.AuthDeepLink
import kr.co.clipnote.core.auth.AuthErrorMessage
import kr.co.clipnote.core.auth.AuthSession
import kr.co.clipnote.core.auth.NaverAuth
import kr.co.clipnote.core.auth.PkcePair
import kr.co.clipnote.core.auth.SupabaseAuth
import kr.co.clipnote.core.auth.randomNonce
import kr.co.clipnote.core.model.AuthProvider

/** 화면이 읽는 인증 상태. `loggedIn` 은 세션 유무에서 나온다. */
data class AuthState(
    val session: AuthSession? = null,
    val loading: Boolean = true,
) {
    val loggedIn: Boolean get() = session != null
    val account: AccountInfo?
        get() = session?.let { AccountInfo(email = it.email, provider = it.provider) }
}

/**
 * 인증 상태의 단일 소유자.
 *
 * 세션 저장·갱신을 여기서만 한다 — 화면마다 토큰을 들고 다니면 어느 것이 최신인지 알 수 없게
 * 된다. 실제 프로토콜은 [SupabaseAuth](코어)가 맡고, 이 클래스는 **언제** 부를지를 정한다.
 */
class AuthStore(
    private val supabase: SupabaseAuth?,
    private val prefs: AppPrefs,
    private val scope: CoroutineScope,
    private val naverClientId: String,
) {
    private val _state = MutableStateFlow(AuthState(session = null, loading = true))
    val state: StateFlow<AuthState> = _state.asStateFlow()

    private val _lastError = MutableStateFlow<AuthErrorMessage?>(null)
    val lastError: StateFlow<AuthErrorMessage?> = _lastError.asStateFlow()

    /**
     * 네이버 콜백이 앱으로 돌아올 때마다 증가.
     *
     * 로그인 화면이 이 값의 변화에 맞춰 진행 표시를 끈다. "앱이 포그라운드로 왔다"로 판단하면
     * 브라우저가 뜨는 순간에도 걸려서 로그인 도중에 표시가 꺼진다.
     */
    private val _naverCallbacks = MutableStateFlow(0)
    val naverCallbacks: StateFlow<Int> = _naverCallbacks.asStateFlow()

    /**
     * 세션이 확정되기 전 첫 프레임에 쓸 표시용 로그인 여부.
     *
     * 없으면 홈 버튼이 게스트용으로 떴다가 로그인용으로 튄다. 토큰이 필요한 동작은 여전히
     * 진짜 세션으로 막으므로, 이 힌트가 틀려도 잘못된 요청이 나가지는 않는다.
     */
    val displayLoggedIn: Boolean
        get() = if (_state.value.loading) prefs.lastLoggedIn else _state.value.loggedIn

    /** token_hash 는 1회용. 딥링크가 두 번 도착해도 다시 verify 하지 않는다. */
    private val consumedTokenHashes = mutableSetOf<String>()
    private val refreshMutex = Mutex()

    val configured: Boolean get() = supabase != null

    init {
        scope.launch { restore() }
    }

    private suspend fun restore() {
        val stored = prefs.session()
        if (stored == null || supabase == null) {
            _state.value = AuthState(session = null, loading = false)
            return
        }
        val session =
            if (stored.needsRefresh(nowSeconds())) refreshed(stored) else stored
        apply(session)
    }

    /**
     * 지금 쓸 수 있는 access token. 만료가 가까우면 먼저 갱신한다.
     *
     * 갱신은 뮤텍스로 한 번만 돈다 — 목록·저장이 동시에 부르면 refresh token 을 두 번 쓰게
     * 되는데, Supabase 는 한 번 쓴 refresh token 을 무효화해서 뒤엣것이 로그아웃당한다.
     */
    suspend fun validAccessToken(): String? {
        val current = _state.value.session ?: return null
        if (!current.needsRefresh(nowSeconds())) return current.accessToken
        return refreshMutex.withLock {
            val latest = _state.value.session ?: return@withLock null
            if (!latest.needsRefresh(nowSeconds())) return@withLock latest.accessToken
            val next = refreshed(latest)
            apply(next)
            next?.accessToken
        }
    }

    private suspend fun refreshed(session: AuthSession): AuthSession? {
        val client = supabase ?: return null
        if (session.refreshToken.isEmpty()) return null
        return client.refresh(session.refreshToken).getOrNull()
    }

    private fun apply(session: AuthSession?) {
        prefs.setSession(session)
        _state.value = AuthState(session = session, loading = false)
    }

    // MARK: - 로그인 시작

    /**
     * Google·Kakao — Supabase OAuth(PKCE). authorize URL 을 브라우저로 열도록 돌려준다.
     * 실제로 여는 건 화면이다(Custom Tabs 를 띄우려면 Activity 컨텍스트가 필요하다).
     */
    fun authorizeUrl(provider: AuthProvider): String? {
        val client = supabase ?: run {
            _lastError.value = AuthErrorMessage.System("로그인 설정이 없어요.")
            return null
        }
        _lastError.value = null
        val pkce = PkcePair.generate()
        prefs.pendingPkceVerifier = pkce.verifier
        prefs.lastLoginProvider = provider.id
        return client.authorizeUrl(provider.id, OAUTH_REDIRECT, pkce)
    }

    /** 네이버 — 우리 서버 콜백을 거치는 커스텀 OAuth. 설정이 없으면 null. */
    fun naverAuthorizeUrl(): String? {
        val url = NaverAuth.authorizeUrl(naverClientId, randomNonce())
        if (url == null) {
            _lastError.value = AuthErrorMessage.NaverNotConfigured
            return null
        }
        _lastError.value = null
        prefs.lastLoginProvider = AuthProvider.NAVER.id
        return url
    }

    // MARK: - 콜백

    /** `clipnote://auth/...` 진입점. auth 링크가 아니면 false. */
    fun handleDeepLink(url: String): Boolean {
        val link = AuthDeepLink.parse(url) ?: return false
        scope.launch { consume(link) }
        return true
    }

    private suspend fun consume(link: AuthDeepLink) {
        val client = supabase ?: return
        when (link) {
            is AuthDeepLink.OAuthCallback -> {
                val verifier = prefs.pendingPkceVerifier ?: return
                client.exchangeCode(link.code, verifier)
                    .onSuccess {
                        prefs.pendingPkceVerifier = null
                        apply(it)
                    }
                    .onFailure { _lastError.value = AuthErrorMessage.System(it.message.orEmpty()) }
            }

            is AuthDeepLink.Naver -> {
                // 콜백이 돌아왔다는 사실 자체를 먼저 알린다(성공·실패·중복 무관).
                _naverCallbacks.value += 1
                if (!consumedTokenHashes.add(link.tokenHash)) return
                client.verifyMagicLink(link.tokenHash)
                    .onSuccess { apply(it) }
                    .onFailure {
                        // 실패했으면 다시 시도할 수 있어야 한다.
                        consumedTokenHashes.remove(link.tokenHash)
                        _lastError.value = AuthErrorMessage.System(it.message.orEmpty())
                    }
            }
        }
    }

    // MARK: - 로그아웃

    suspend fun signOut() {
        val token = _state.value.session?.accessToken
        if (token != null) supabase?.signOut(token)
        prefs.pendingPkceVerifier = null
        apply(null)
    }

    /** 회원 탈퇴 뒤처리 — 서버가 이미 계정을 지웠으니 로컬 세션만 비운다. */
    fun clearSessionLocally() = apply(null)

    fun clearError() {
        _lastError.value = null
    }

    private fun nowSeconds(): Long = System.currentTimeMillis() / 1000

    companion object {
        /** Supabase 가 돌려보낼 딥링크. iOS 와 같은 값이라 서버 설정을 공유한다. */
        const val OAUTH_REDIRECT = "clipnote://auth/callback"
    }
}
