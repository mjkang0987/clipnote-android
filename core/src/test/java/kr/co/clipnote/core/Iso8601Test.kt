package kr.co.clipnote.core

import kr.co.clipnote.core.util.parseIsoDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class Iso8601Test {
    @Test
    fun `소수점 여섯 자리 timestamptz 를 읽는다`() {
        // Supabase 의 `timestamptz` 는 마이크로초까지 온다 — iOS 는 이걸 못 읽어 따로 손봐야 했다.
        assertNotNull(parseIsoDate("2026-07-30T04:19:12.345678+00:00"))
    }

    @Test
    fun `Z 표기와 소수점 없는 값도 읽는다`() {
        assertEquals(parseIsoDate("2026-07-30T04:19:12Z"), parseIsoDate("2026-07-30T04:19:12+00:00"))
    }

    @Test
    fun `오프셋이 없으면 UTC 로 본다`() {
        assertEquals(parseIsoDate("2026-07-30T04:19:12Z"), parseIsoDate("2026-07-30T04:19:12"))
    }

    @Test
    fun `못 읽는 값은 null`() {
        assertNull(parseIsoDate(""))
        assertNull(parseIsoDate("어제"))
    }
}
