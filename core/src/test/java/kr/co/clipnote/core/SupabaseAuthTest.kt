package kr.co.clipnote.core

import kotlinx.coroutines.test.runTest
import kr.co.clipnote.core.auth.AuthSession
import kr.co.clipnote.core.auth.PkcePair
import kr.co.clipnote.core.auth.SupabaseAuth
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SupabaseAuthTest {
    private lateinit var server: MockWebServer
    private lateinit var auth: SupabaseAuth

    @Before
    fun setUp() {
        server = MockWebServer().apply { start() }
        auth = SupabaseAuth(server.url("/"), anonKey = "anon")
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `설정이 없으면 클라이언트를 만들지 않는다`() {
        // 잘못 설정된 빌드가 켜자마자 죽는 대신, 로그인만 꺼진 채 앱이 살아야 한다.
        assertNull(SupabaseAuth.createOrNull("", "anon"))
        assertNull(SupabaseAuth.createOrNull("https://x.supabase.co", " "))
    }

    @Test
    fun `authorize URL 에 PKCE challenge 가 실린다`() {
        val pkce = PkcePair.generate()
        val url = auth.authorizeUrl("google", "clipnote://auth/callback", pkce)
        assertTrue(url.contains("/auth/v1/authorize"))
        assertTrue(url.contains("provider=google"))
        assertTrue(url.contains("code_challenge=${pkce.challenge}"))
        assertTrue(url.contains("code_challenge_method=s256"))
        assertTrue(url.contains("redirect_to=clipnote%3A%2F%2Fauth%2Fcallback"))
    }

    @Test
    fun `code 교환은 verifier 를 함께 보낸다`() = runTest {
        server.enqueue(
            MockResponse().setBody(
                """{"access_token":"at","refresh_token":"rt","expires_in":3600,
                   "user":{"email":"a@b.c","app_metadata":{"provider":"google"}}}"""
            )
        )
        val session = auth.exchangeCode("code123", "verifier123").getOrThrow()
        assertEquals("at", session.accessToken)
        assertEquals("a@b.c", session.email)
        assertEquals("google", session.provider)

        val request = server.takeRequest()
        assertEquals("/auth/v1/token?grant_type=pkce", request.path)
        assertEquals("anon", request.getHeader("apikey"))
        val body = request.body.readUtf8()
        assertTrue(body.contains("\"auth_code\":\"code123\""))
        assertTrue(body.contains("\"code_verifier\":\"verifier123\""))
    }

    @Test
    fun `실패 응답의 사유를 예외 메시지로 올린다`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(401)
                .setBody("""{"error":"invalid_grant","error_description":"코드가 만료됐어요"}""")
        )
        val result = auth.exchangeCode("code", "verifier")
        assertTrue(result.isFailure)
        assertEquals("코드가 만료됐어요", result.exceptionOrNull()?.message)
    }

    @Test
    fun `magiclink verify 는 token_hash 를 보낸다`() = runTest {
        server.enqueue(MockResponse().setBody("""{"access_token":"at","refresh_token":"rt","expires_at":9999999999}"""))
        val session = auth.verifyMagicLink("hash").getOrThrow()
        assertEquals(9999999999L, session.expiresAt)
        val body = server.takeRequest().body.readUtf8()
        assertTrue(body.contains("\"type\":\"magiclink\""))
        assertTrue(body.contains("\"token_hash\":\"hash\""))
    }

    @Test
    fun `expires_at 이 없으면 expires_in 으로 채운다`() {
        val session = auth.parseSession("""{"access_token":"at","expires_in":100}""", nowSeconds = 1000)
        assertEquals(1100L, session.expiresAt)
    }

    @Test
    fun `만료 직전이면 갱신 대상`() {
        val session = AuthSession("at", "rt", expiresAt = 1000)
        assertTrue(session.needsRefresh(nowSeconds = 950))
        assertFalse(session.needsRefresh(nowSeconds = 800))
    }
}
