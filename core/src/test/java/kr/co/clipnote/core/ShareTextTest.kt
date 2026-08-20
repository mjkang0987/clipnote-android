package kr.co.clipnote.core

import kr.co.clipnote.core.util.SHARE_TITLE_MAX_LENGTH
import kr.co.clipnote.core.util.buildShareText
import kr.co.clipnote.core.util.orderedUnique
import kr.co.clipnote.core.util.parseTags
import kr.co.clipnote.core.util.truncateShareTitle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ShareTextTest {
    @Test
    fun `제목과 URL 을 줄바꿈으로 잇는다`() {
        assertEquals("제목\nhttps://clipnote.co.kr/abc", buildShareText("제목", "설명", "https://clipnote.co.kr/abc"))
    }

    @Test
    fun `설명은 붙이지 않는다`() {
        val text = buildShareText("제목", "긴 설명입니다", "https://clipnote.co.kr/abc")
        assertTrue(!text.contains("긴 설명입니다"))
    }

    @Test
    fun `제목이 비면 URL 만 남는다`() {
        assertEquals("https://clipnote.co.kr/abc", buildShareText("", null, "https://clipnote.co.kr/abc"))
    }

    @Test
    fun `긴 제목은 말줄임표를 포함해 최대 길이로 줄인다`() {
        val long = "가".repeat(200)
        val result = truncateShareTitle(long)
        assertEquals(SHARE_TITLE_MAX_LENGTH, result.length)
        assertTrue(result.endsWith("…"))
    }

    @Test
    fun `짧은 제목은 그대로 둔다`() {
        assertEquals("짧은 제목", truncateShareTitle("짧은 제목"))
    }

    @Test
    fun `개행이 섞인 제목은 한 줄로 정리한다`() {
        assertEquals("첫 줄 둘째 줄", truncateShareTitle("첫 줄\n\n둘째  줄"))
    }

    @Test
    fun `잘린 끝의 공백은 떼낸다`() {
        // 20자에서 잘리면 19번째가 공백인 문자열 — 공백 뒤에 말줄임표가 붙으면 지저분하다.
        val value = "a".repeat(18) + " " + "b".repeat(30)
        assertEquals("a".repeat(18) + "…", truncateShareTitle(value, max = 20))
    }

    @Test
    fun `태그는 쉼표로 나누고 최대 6개까지`() {
        assertEquals(listOf("a", "b", "c", "d", "e", "f"), parseTags(" a, b ,c,d,e,f,g "))
    }

    @Test
    fun `빈 태그는 버린다`() {
        assertEquals(listOf("개발"), parseTags(",, 개발 , ,"))
    }

    @Test
    fun `중복 제거는 순서를 지킨다`() {
        assertEquals(listOf("b", "a", "c"), orderedUnique(listOf("b", "a", "b", "c", "a")))
    }
}
