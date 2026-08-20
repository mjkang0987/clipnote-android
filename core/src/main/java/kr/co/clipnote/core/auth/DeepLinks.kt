package kr.co.clipnote.core.auth

import java.net.URI
import java.net.URLDecoder

/** 앱이 여는 커스텀 스킴. iOS 와 같은 값이라 서버 콜백 설정을 공유한다. */
const val APP_SCHEME = "clipnote"

/**
 * `clipnote://auth/...` 딥링크.
 *
 * - `clipnote://auth/callback?code=...` → Supabase OAuth code 교환(Google/Kakao)
 * - `clipnote://auth/naver?token_hash=...` → 네이버 magiclink verify
 */
sealed interface AuthDeepLink {
    data class OAuthCallback(val code: String) : AuthDeepLink
    data class Naver(val tokenHash: String) : AuthDeepLink

    companion object {
        fun parse(url: String): AuthDeepLink? {
            val uri = runCatching { URI(url) }.getOrNull() ?: return null
            if (uri.scheme != APP_SCHEME || uri.host != "auth") return null
            val query = queryParams(uri.rawQuery)
            return when (uri.path) {
                "/callback" -> query["code"]?.takeIf { it.isNotEmpty() }?.let(::OAuthCallback)
                "/naver" -> query["token_hash"]?.takeIf { it.isNotEmpty() }?.let(::Naver)
                else -> null
            }
        }
    }
}

/**
 * 공유 인텐트가 넘긴 `clipnote://share?url=<원본>`.
 *
 * 안드로이드는 `ACTION_SEND` 로 바로 받으므로 이 링크가 꼭 필요하지는 않지만, iOS 와 같은
 * 규약을 두어 웹·다른 앱이 같은 방식으로 홈 입력칸을 채울 수 있게 한다.
 */
object ShareDeepLink {
    fun parse(url: String): String? {
        val uri = runCatching { URI(url) }.getOrNull() ?: return null
        if (uri.scheme != APP_SCHEME || uri.host != "share") return null
        return queryParams(uri.rawQuery)["url"]?.takeIf { it.isNotEmpty() }
    }
}

/**
 * 쿼리 문자열 파서.
 *
 * `android.net.Uri` 를 쓰지 않는 이유는 **테스트 때문**이다 — 안드로이드 프레임워크에 기대면
 * 이 파싱을 확인하는 데 에뮬레이터나 Robolectric 이 필요해진다. 딥링크는 로그인이 되고
 * 안 되고를 가르는 자리라 가장 확실히 테스트되어야 한다.
 */
internal fun queryParams(rawQuery: String?): Map<String, String> {
    if (rawQuery.isNullOrEmpty()) return emptyMap()
    return rawQuery.split("&").mapNotNull { pair ->
        val index = pair.indexOf('=')
        if (index <= 0) return@mapNotNull null
        val name = decode(pair.substring(0, index))
        val value = decode(pair.substring(index + 1))
        name to value
    }.toMap()
}

private fun decode(value: String): String =
    runCatching { URLDecoder.decode(value, "UTF-8") }.getOrElse { value }
