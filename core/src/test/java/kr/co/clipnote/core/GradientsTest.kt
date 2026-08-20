package kr.co.clipnote.core

import kr.co.clipnote.core.theme.GRADIENTS
import kr.co.clipnote.core.theme.gradientNamed
import kr.co.clipnote.core.theme.pickGradient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class GradientsTest {
    @Test
    fun `같은 시드는 항상 같은 그라디언트`() {
        assertSame(pickGradient("clipnote"), pickGradient("clipnote"))
    }

    @Test
    fun `웹 해시와 같은 값을 고른다`() {
        // 웹 `pickGradient` 의 `(hash * 31 + charCodeAt(i)) | 0` 을 그대로 옮긴 결과.
        // 같은 클립이 기기마다 다른 색이면 같은 것으로 보이지 않는다 — 이 값이 갈리면 안 된다.
        assertEquals("grape", pickGradient("clipnote").name)
        assertEquals("grape", pickGradient("ClipNote").name)
        assertEquals("sunset", pickGradient("").name)
    }

    @Test
    fun `아주 긴 시드에서도 인덱스가 범위 안에 있다`() {
        // Int 오버플로가 음수로 돌 때 abs 가 음수를 내는 경계(Int MIN VALUE)를 밟아도 죽지 않아야 한다.
        repeat(500) { index ->
            val gradient = pickGradient("가".repeat(index) + "seed$index")
            assertTrue(GRADIENTS.contains(gradient))
        }
    }

    @Test
    fun `이름으로 찾고 없으면 시드로 폴백`() {
        assertEquals("ocean", gradientNamed("ocean", "무관").name)
        assertEquals("mint", pickGradient("example").name)
        assertEquals(pickGradient("제목").name, gradientNamed("모르는이름", "제목").name)
    }
}
