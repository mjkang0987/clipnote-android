package kr.co.clipnote.core

import kotlinx.coroutines.test.runTest
import kr.co.clipnote.core.model.CreateClipInput
import kr.co.clipnote.core.net.ApiClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ApiClientTest {
    private lateinit var server: MockWebServer
    private lateinit var api: ApiClient

    @Before
    fun setUp() {
        server = MockWebServer().apply { start() }
        api = ApiClient(server.url("/"))
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `메타를 읽고 모르는 필드는 무시한다`() = runTest {
        server.enqueue(
            MockResponse().setBody(
                """{"url":"https://a.com","title":"제목","source":"og","futureField":1}"""
            )
        )
        val meta = api.fetchMetadata("https://a.com")
        assertEquals("제목", meta.title)
        assertEquals("og", meta.source)
        val request = server.takeRequest()
        assertEquals("/api/metadata?url=https%3A%2F%2Fa.com", request.path)
    }

    @Test
    fun `클립 생성 본문에는 null 필드를 넣지 않는다`() = runTest {
        server.enqueue(MockResponse().setBody("""{"slug":"abc","shareUrl":"https://clipnote.co.kr/abc"}"""))
        val result = api.createClip(
            CreateClipInput(url = "https://a.com", title = "제목", gradient = "ocean"),
            accessToken = "token",
        )
        assertEquals("https://clipnote.co.kr/abc", result.shareUrl)
        assertNull(result.error)

        val request = server.takeRequest()
        val body = request.body.readUtf8()
        assertEquals("Bearer token", request.getHeader("Authorization"))
        assertFalse("null 필드는 보내지 않는다: $body", body.contains("null"))
        assertFalse(body.contains("description"))
    }

    @Test
    fun `실패 응답의 사유를 그대로 돌려준다`() = runTest {
        server.enqueue(MockResponse().setResponseCode(400).setBody("""{"error":"이미 저장된 링크예요"}"""))
        val result = api.createClip(
            CreateClipInput(url = "https://a.com", title = "제목", gradient = "ocean"),
            accessToken = null,
        )
        assertEquals("이미 저장된 링크예요", result.error)
    }

    @Test
    fun `목록 조회 실패는 빈 목록과 구분된다`() = runTest {
        server.enqueue(MockResponse().setResponseCode(500))
        val failed = api.getClips("token")
        assertTrue(failed.failed)
        assertTrue(failed.clips.isEmpty())

        server.enqueue(MockResponse().setBody("""{"loggedIn":true,"clips":[]}"""))
        val empty = api.getClips("token")
        assertFalse(empty.failed)
        assertTrue(empty.loggedIn)
    }

    @Test
    fun `수정은 준 필드만 담는다`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200))
        assertTrue(api.updateClip(slug = "abc", shared = true, accessToken = "token"))
        val request = server.takeRequest()
        assertEquals("PATCH", request.method)
        assertEquals("/api/clip/abc", request.path)
        assertEquals("""{"shared":true}""", request.body.readUtf8())
    }

    @Test
    fun `토큰이 없으면 계정 삭제를 시도하지 않는다`() = runTest {
        val result = api.deleteAccount(null)
        assertFalse(result.ok)
        assertEquals("no_token", result.error)
        assertEquals(0, server.requestCount)
    }

    @Test
    fun `OG 이미지 주소에 제목과 그라디언트가 실린다`() {
        val url = api.ogImageUrl("제목", description = null, siteName = "clipnote.co.kr", gradient = "ocean")
        assertTrue(url.contains("/api/og?"))
        assertTrue(url.contains("g=ocean"))
        assertTrue(url.contains("site=clipnote.co.kr"))
        assertFalse(url.contains("desc="))
    }
}
