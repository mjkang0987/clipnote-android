package kr.co.clipnote.core

import kr.co.clipnote.core.util.isFetchableUrl
import kr.co.clipnote.core.util.openableWebUrl
import kr.co.clipnote.core.util.prettyHost
import kr.co.clipnote.core.util.proxiedImageUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UrlsTest {
    @Test
    fun `스킴이 없어도 호스트에 점이 있으면 추출 대상`() {
        assertTrue(isFetchableUrl("clipnote.co.kr"))
        assertTrue(isFetchableUrl("https://clipnote.co.kr/abc"))
    }

    @Test
    fun `점이 없는 값은 추출 대상이 아니다`() {
        assertFalse(isFetchableUrl("localhost"))
        assertFalse(isFetchableUrl("   "))
        assertFalse(isFetchableUrl(""))
    }

    @Test
    fun `표시용 호스트는 www 를 떼고 루트 경로를 생략한다`() {
        assertEquals("clipnote.co.kr", prettyHost("https://www.clipnote.co.kr/"))
        assertEquals("clipnote.co.kr/abc", prettyHost("https://clipnote.co.kr/abc"))
        assertEquals("clipnote.co.kr", prettyHost("clipnote.co.kr"))
    }

    @Test
    fun `이미지는 프록시를 거친다`() {
        val proxied = proxiedImageUrl("https://cdn.example.com/a b.png", "https://clipnote.co.kr")
        assertEquals("https://clipnote.co.kr/api/image?url=https%3A%2F%2Fcdn.example.com%2Fa+b.png", proxied)
    }

    @Test
    fun `빈 이미지는 프록시 주소를 만들지 않는다`() {
        assertNull(proxiedImageUrl(null, "https://clipnote.co.kr"))
        assertNull(proxiedImageUrl("   ", "https://clipnote.co.kr"))
    }

    @Test
    fun `스킴 없는 주소도 열 수 있게 정규화한다`() {
        assertEquals("https://clipnote.co.kr", openableWebUrl("clipnote.co.kr"))
        assertEquals("http://example.com/a", openableWebUrl("http://example.com/a"))
        assertNull(openableWebUrl(""))
    }
}
