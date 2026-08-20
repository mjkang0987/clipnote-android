package kr.co.clipnote.core.clips

import kr.co.clipnote.core.model.DbClip
import kr.co.clipnote.core.model.UClip
import kr.co.clipnote.core.util.parseIsoDate
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/** DB 클립 → 통합 뷰 모델. id = slug. */
fun DbClip.toUClip(): UClip = UClip(
    id = slug,
    slug = slug,
    url = url,
    title = title,
    description = description,
    image = image,
    siteName = siteName,
    gradient = gradient,
    tags = tags,
    shared = shared,
    local = false,
    savedAt = parseIsoDate(createdAt) ?: 0L,
)

/**
 * 목록 머리글이 될 날짜 묶음.
 *
 * **문구가 아니라 케이스로 둔다.** `2026년 7월` 같은 연월은 형식 자체가 언어마다 다르고
 * (en `July 2026`, ja `2026年7月`) 사전으로는 표현할 수 없다. 화면이 시스템 포매터로 그린다 —
 * iOS 가 `RelativeDateTimeFormatter` 에 맡긴 것과 같은 이유다.
 */
sealed interface ClipDateBucket {
    data object Today : ClipDateBucket
    data object Yesterday : ClipDateBucket
    data object ThisWeek : ClipDateBucket
    data object ThisMonth : ClipDateBucket

    /** 그 밖 — 연·월로 표기한다. */
    data class YearMonth(val year: Int, val month: Int) : ClipDateBucket
}

data class ClipDateGroup(val bucket: ClipDateBucket, val clips: List<UClip>)

/** 저장 시각이 어느 묶음에 드는지. */
fun clipDateBucket(
    savedAt: Long,
    now: Long = System.currentTimeMillis(),
    zone: ZoneId = ZoneId.systemDefault(),
): ClipDateBucket {
    val date = Instant.ofEpochMilli(savedAt).atZone(zone).toLocalDate()
    val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
    val days = ChronoUnit.DAYS.between(date, today)
    return when {
        days <= 0L -> ClipDateBucket.Today
        days == 1L -> ClipDateBucket.Yesterday
        days < 7L -> ClipDateBucket.ThisWeek
        sameMonth(date, today) -> ClipDateBucket.ThisMonth
        else -> ClipDateBucket.YearMonth(date.year, date.monthValue)
    }
}

/**
 * 순서를 유지한 채 날짜로 묶는다. 목록이 이미 최신순이라 그룹도 최신순이 된다.
 */
fun groupClipsByDate(
    clips: List<UClip>,
    now: Long = System.currentTimeMillis(),
    zone: ZoneId = ZoneId.systemDefault(),
): List<ClipDateGroup> {
    val order = LinkedHashMap<ClipDateBucket, MutableList<UClip>>()
    for (clip in clips) {
        order.getOrPut(clipDateBucket(clip.savedAt, now, zone)) { mutableListOf() }.add(clip)
    }
    return order.map { (bucket, items) -> ClipDateGroup(bucket, items) }
}

private fun sameMonth(a: LocalDate, b: LocalDate): Boolean =
    a.year == b.year && a.monthValue == b.monthValue
