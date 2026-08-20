package kr.co.clipnote.core

import kr.co.clipnote.core.auth.AuthDeepLink
import kr.co.clipnote.core.auth.NaverAuth
import kr.co.clipnote.core.auth.PkcePair
import kr.co.clipnote.core.auth.ShareDeepLink
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DeepLinksTest {
    @Test
    fun `OAuth 콜백에서 code 를 뽑는다`() {
        val link = AuthDeepLink.parse("clipnote://auth/callback?code=abc123")
        assertEquals(AuthDeepLink.OAuthCallback("abc123"), link)
    }

    @Test
    fun `네이버 콜백에서 token_hash 를 뽑는다`() {
        val link = AuthDeepLink.parse("clipnote://auth/naver?token_hash=deadbeef&x=1")
        assertEquals(AuthDeepLink.Naver("deadbeef"), link)
    }

    @Test
    fun `빈 값이거나 모르는 경로면 auth 링크가 아니다`() {
        assertNull(AuthDeepLink.parse("clipnote://auth/callback?code="))
        assertNull(AuthDeepLink.parse("clipnote://auth/unknown?code=1"))
        assertNull(AuthDeepLink.parse("clipnote://share?url=https://a.com"))
        assertNull(AuthDeepLink.parse("https://clipnote.co.kr/auth/callback?code=1"))
        assertNull(AuthDeepLink.parse("not a url"))
    }

    @Test
    fun `공유 링크는 인코딩된 URL 을 되돌려 준다`() {
        val shared = ShareDeepLink.parse("clipnote://share?url=https%3A%2F%2Fexample.com%2Fa%3Fb%3D1")
        assertEquals("https://example.com/a?b=1", shared)
    }

    @Test
    fun `공유 링크가 아니면 null`() {
        assertNull(ShareDeepLink.parse("clipnote://auth/callback?code=1"))
        assertNull(ShareDeepLink.parse("clipnote://share"))
    }

    @Test
    fun `네이버 authorize URL 에 state 와 redirect 가 실린다`() {
        val url = NaverAuth.authorizeUrl("client-id", "nonce123")!!
        assertTrue(url.startsWith("https://nid.naver.com/oauth2.0/authorize?response_type=code"))
        assertTrue(url.contains("client_id=client-id"))
        assertTrue(url.contains("redirect_uri=https%3A%2F%2Fclipnote.co.kr%2Fapi%2Fauth%2Fnaver%2Fcallback"))
        // state 는 JSON 이라 인코딩된 중괄호가 보인다.
        assertTrue(url.contains("state=%7B"))
        assertTrue(url.contains("nonce123"))
    }

    @Test
    fun `client_id 가 없으면 authorize URL 을 만들지 않는다`() {
        assertNull(NaverAuth.authorizeUrl("  ", "nonce"))
    }

    @Test
    fun `PKCE 는 매번 다른 verifier 를 만들고 challenge 는 URL 안전 문자만 쓴다`() {
        val first = PkcePair.generate()
        val second = PkcePair.generate()
        assertNotEquals(first.verifier, second.verifier)
        assertTrue(first.challenge.all { it.isLetterOrDigit() || it == '-' || it == '_' })
        assertTrue(first.verifier.length >= 43)
    }
}
