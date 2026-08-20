package kr.co.clipnote.core.util

import java.time.Instant
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

/**
 * 서버가 준 ISO 시각 문자열을 epoch millis 로. 못 읽으면 null.
 *
 * Supabase 의 `timestamptz` 는 소수점 이하가 여섯 자리로 오고
 * (`2026-07-30T04:19:12.345678+00:00`), 자리수가 없는 판(`...Z`)도 섞여 온다.
 * `DateTimeFormatter.ISO_OFFSET_DATE_TIME` 이 둘 다 읽지만, 오프셋이 아예 없는 값
 * (`2026-07-30T04:19:12`)은 UTC 로 본다 — 날짜 그룹에 초 이하는 필요 없다.
 */
fun parseIsoDate(text: String): Long? {
    val trimmed = text.trim()
    if (trimmed.isEmpty()) return null
    runCatching { return OffsetDateTime.parse(trimmed).toInstant().toEpochMilli() }
    runCatching { return Instant.parse(trimmed).toEpochMilli() }
    runCatching {
        return java.time.LocalDateTime
            .parse(trimmed, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            .toInstant(java.time.ZoneOffset.UTC)
            .toEpochMilli()
    }
    return null
}
