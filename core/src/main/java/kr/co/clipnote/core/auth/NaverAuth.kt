package kr.co.clipnote.core.auth

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kr.co.clipnote.core.net.ClipNoteJson
import java.net.URLEncoder

/**
 * 네이버 로그인(커스텀 OAuth).
 *
 * Supabase 기본 공급자가 아니라서 우리 서버 콜백을 거친다 — 네이버가 서버로 code 를 주면,
 * 서버가 magiclink token_hash 를 만들어 `clipnote://auth/naver` 로 앱에 돌려보낸다.
 * 앱은 그 token_hash 를 [SupabaseAuth.verifyMagicLink] 로 세션과 바꾼다.
 */
object NaverAuth {
    /** 서버 콜백. 네이버 개발자 콘솔에 등록된 값과 같아야 한다. */
    const val CALLBACK = "https://clipnote.co.kr/api/auth/naver/callback"

    /** 콜백이 앱으로 복귀할 딥링크. */
    const val RETURN_URL = "clipnote://auth/naver"

    /** state 에 담기는 값. iOS·RN `lib/naver.ts` 와 같은 스키마다. */
    @Serializable
    data class State(val returnUrl: String, val n: String)

    /** authorize URL. client_id 가 없으면 null(설정 누락). */
    fun authorizeUrl(clientId: String, nonce: String): String? {
        if (clientId.isBlank()) return null
        val state = ClipNoteJson.encodeToString(State(RETURN_URL, nonce))
        return "https://nid.naver.com/oauth2.0/authorize" +
            "?response_type=code" +
            "&client_id=" + encode(clientId) +
            "&redirect_uri=" + encode(CALLBACK) +
            "&state=" + encode(state)
    }

    private fun encode(value: String): String = URLEncoder.encode(value, "UTF-8")
}
