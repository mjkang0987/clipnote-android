package kr.co.clipnote.core

import kr.co.clipnote.core.clips.ClipDateBucket
import kr.co.clipnote.core.clips.clipDateBucket
import kr.co.clipnote.core.clips.groupClipsByDate
import kr.co.clipnote.core.clips.toUClip
import kr.co.clipnote.core.model.DbClip
import kr.co.clipnote.core.model.UClip
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class ClipGroupingTest {
    private val zone: ZoneId = ZoneId.of("Asia/Seoul")
    private val now = LocalDateTime.of(2026, 8, 20, 14, 0).atZone(zone).toInstant().toEpochMilli()

    private fun at(year: Int, month: Int, day: Int): Long =
        LocalDateTime.of(year, month, day, 9, 0).atZone(zone).toInstant().toEpochMilli()

    @Test
    fun `오늘 어제 이번주 이번달 그 밖으로 나뉜다`() {
        assertEquals(ClipDateBucket.Today, clipDateBucket(at(2026, 8, 20), now, zone))
        assertEquals(ClipDateBucket.Yesterday, clipDateBucket(at(2026, 8, 19), now, zone))
        assertEquals(ClipDateBucket.ThisWeek, clipDateBucket(at(2026, 8, 16), now, zone))
        assertEquals(ClipDateBucket.ThisMonth, clipDateBucket(at(2026, 8, 2), now, zone))
        assertEquals(ClipDateBucket.YearMonth(2026, 7), clipDateBucket(at(2026, 7, 30), now, zone))
    }

    @Test
    fun `묶음 순서는 목록 순서를 따른다`() {
        val clips = listOf(
            clip("a", at(2026, 8, 20)),
            clip("b", at(2026, 8, 20)),
            clip("c", at(2026, 8, 19)),
            clip("d", at(2026, 5, 1)),
        )
        val groups = groupClipsByDate(clips, now, zone)
        assertEquals(3, groups.size)
        assertEquals(ClipDateBucket.Today, groups[0].bucket)
        assertEquals(listOf("a", "b"), groups[0].clips.map { it.id })
        assertEquals(ClipDateBucket.Yesterday, groups[1].bucket)
        assertEquals(ClipDateBucket.YearMonth(2026, 5), groups[2].bucket)
    }

    @Test
    fun `DB 클립은 slug 를 id 로 쓰고 로컬이 아니다`() {
        val db = DbClip(
            slug = "abc",
            url = "https://example.com",
            title = "제목",
            gradient = "ocean",
            tags = listOf("개발"),
            shared = true,
            createdAt = "2026-08-19T04:19:12.345678+00:00",
        )
        val uclip = db.toUClip()
        assertEquals("abc", uclip.id)
        assertEquals("abc", uclip.slug)
        assertFalse(uclip.local)
        assertEquals(ClipDateBucket.Yesterday, clipDateBucket(uclip.savedAt, now, zone))
    }

    private fun clip(id: String, savedAt: Long) = UClip(
        id = id, slug = id, url = "https://example.com/$id", title = id,
        description = null, image = null, siteName = null, gradient = "ocean",
        tags = emptyList(), shared = false, local = false, savedAt = savedAt,
    )
}
