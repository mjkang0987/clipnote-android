package kr.co.clipnote.core.auth

import kotlinx.serialization.Serializable

/** Supabase 가 돌려준 세션. `expiresAt` 은 epoch 초. */
@Serializable
data class AuthSession(
    val accessToken: String,
    val refreshToken: String,
    val expiresAt: Long,
    val email: String? = null,
    val provider: String? = null,
) {
    /**
     * 만료까지 [skewSeconds] 초도 안 남았는가.
     *
     * 여유를 두는 이유는 요청이 날아가는 동안에도 시간이 흐르기 때문이다. 정확히 만료 시각에
     * 갱신하면 왕복 중에 만료된 토큰으로 도착하는 요청이 생긴다.
     */
    fun needsRefresh(nowSeconds: Long, skewSeconds: Long = 60): Boolean =
        nowSeconds >= expiresAt - skewSeconds
}

/** 설정 화면에 보여 줄 계정 정보. */
data class AccountInfo(val email: String?, val provider: String?) {
    /**
     * 공급자 표시 이름. **라틴 표기로 고정하고 번역하지 않는다** — 언어마다 `카카오`/`カカオ`/
     * `卡考` 로 갈리면 사용자가 자기가 무엇으로 로그인했는지 못 알아본다(웹·iOS 와 같은 결정).
     *
     * 모르는 공급자는 null 을 돌려주고 화면이 `settings_providerUnknown`("소셜")을 쓴다.
     */
    val providerName: String?
        get() = kr.co.clipnote.core.model.AuthProvider.from(provider)?.displayName
}

/** 로그인 실패 사유. **문자열이 아니라 케이스로 둔다** — 문장을 만드는 건 화면의 몫이다. */
sealed interface AuthErrorMessage {
    /** 네이버 client_id 가 빌드에 없다(`secrets.properties` 누락). */
    data object NaverNotConfigured : AuthErrorMessage

    /** Supabase·시스템이 준 설명. 이미 사람이 읽는 문장이라 그대로 보여 준다. */
    data class System(val text: String) : AuthErrorMessage
}
