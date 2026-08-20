package kr.co.clipnote.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ShareIntentTest {
    @Test
    fun `문장에 섞인 링크에서 주소만 뽑는다`() {
        // 카카오톡·유튜브는 "제목 https://…" 처럼 문장을 통째로 넘긴다. 그대로 입력칸에 넣으면
        // 메타 추출이 실패한다.
        assertEquals(
            "https://youtu.be/abc123",
            extractUrl("이 영상 재밌다 https://youtu.be/abc123"),
        )
    }

    @Test
    fun `주소만 왔으면 그대로`() {
        assertEquals("https://clipnote.co.kr", extractUrl("  https://clipnote.co.kr  "))
    }

    @Test
    fun `주소가 없으면 원문을 남긴다`() {
        // 사용자가 입력칸에서 고칠 수 있게 버리지 않는다.
        assertEquals("clipnote.co.kr", extractUrl("clipnote.co.kr"))
    }

    @Test
    fun `빈 글은 무시한다`() {
        assertNull(extractUrl("   "))
    }
}
