package kr.co.clipnote.core.util

/**
 * 공유 텍스트 제목의 최대 길이(말줄임표 `…` 포함).
 * 웹 `lib/shareText.ts` 의 `SHARE_TITLE_MAX` 와 같은 값을 유지한다.
 */
const val SHARE_TITLE_MAX_LENGTH = 80

/**
 * 클립보드·공유에 넣을 문자열. 형식은 `제목\nURL`.
 *
 * 설명은 붙이지 않는다 — 길어서 붙여넣은 글이 지저분해진다(웹 `c4c4ad9`).
 * `description` 은 호출부 시그니처 호환을 위해 남기되 쓰지 않는다.
 */
fun buildShareText(title: String, description: String?, url: String): String =
    listOf(truncateShareTitle(title), url).filter { it.isNotEmpty() }.joinToString("\n")

/**
 * 제목을 `max` 자(말줄임표 포함)로 줄인다.
 *
 * 인스타그램 어댑터는 `og:title` 을 제목으로 쓰는데 인스타는 거기에 캡션 전문을 넣는다.
 * 자르지 않으면 공유문이 캡션 통째로 길어진다.
 */
fun truncateShareTitle(value: String, max: Int = SHARE_TITLE_MAX_LENGTH): String {
    val title = collapseWhitespace(value)
    if (max < 1 || title.length <= max) return title
    // 말줄임표가 한 자를 차지하므로 본문은 max-1 자까지. 잘린 끝의 공백은 떼낸다.
    return title.take(max - 1).trimEnd() + "…"
}

/**
 * 공백·줄바꿈을 한 칸으로 정리(앞뒤 공백 제거 포함).
 *
 * 인스타 캡션처럼 개행이 섞인 제목을 그대로 쓰면 `제목\nURL` 형식이 깨져 링크가 어느 줄에
 * 있는지 알 수 없게 된다.
 */
private fun collapseWhitespace(value: String): String =
    value.split(Regex("\\s+")).filter { it.isNotEmpty() }.joinToString(" ")

/** 태그 입력 파싱: 쉼표 구분 · 트림 · 빈값 제거 · 최대 6개. */
fun parseTags(input: String): List<String> =
    input.split(",").map { it.trim() }.filter { it.isNotEmpty() }.take(6)

/** 순서 보존 중복 제거. */
fun orderedUnique(values: List<String>): List<String> {
    val seen = LinkedHashSet<String>()
    for (value in values) seen.add(value)
    return seen.toList()
}
