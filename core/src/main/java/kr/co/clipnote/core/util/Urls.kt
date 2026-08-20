package kr.co.clipnote.core.util

import java.net.URI
import java.net.URLEncoder

/** 자동 메타 추출을 시도할 만한 URL 인가(host 에 "." 이 있는가). */
fun isFetchableUrl(raw: String): Boolean {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return false
    val candidate = if (trimmed.startsWith("http")) trimmed else "https://$trimmed"
    val host = hostOf(candidate) ?: return false
    return host.contains(".")
}

/** 표시용 호스트+경로(`www.` 제거, 루트 경로는 생략). */
fun prettyHost(raw: String): String {
    val candidate = if (raw.startsWith("http")) raw else "https://$raw"
    val uri = runCatching { URI(candidate) }.getOrNull() ?: return raw
    val host = uri.host ?: return raw
    val trimmedHost = host.removePrefix("www.")
    val path = uri.path.orEmpty()
    return trimmedHost + if (path.isNotEmpty() && path != "/") path else ""
}

/**
 * 원본 대표 이미지를 서버 프록시(`/api/image`)로 감싼 URL.
 *
 * 앱이 원본을 직접 부르면 hotlink 차단·referer 요구(네이버 CDN)·혼합 콘텐츠(http)로 자주
 * 막힌다. 실패해도 뒤의 그라디언트가 보여서 **눈에는 정상처럼 보이고** 스크롤할 때마다 실패
 * 요청만 반복된다. 저장되는 원본 값은 그대로 두고 표시할 때만 감싼다.
 */
fun proxiedImageUrl(original: String?, base: String): String? {
    val value = original?.trim().orEmpty()
    if (value.isEmpty()) return null
    return base.trimEnd('/') + "/api/image?url=" + URLEncoder.encode(value, "UTF-8")
}

/**
 * 저장된 URL 을 브라우저로 열 수 있는 형태로 정규화. 열 수 없으면 null.
 * 스킴이 없으면 https 를 붙이고, 파싱에 실패하면 퍼센트 인코딩으로 한 번 더 시도한다.
 */
fun openableWebUrl(raw: String): String? {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return null
    val withScheme =
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) trimmed
        else "https://$trimmed"
    if (schemeOf(withScheme) != null) return withScheme
    val encoded = withScheme.replace(" ", "%20")
    return if (schemeOf(encoded) != null) encoded else null
}

private fun schemeOf(value: String): String? {
    val uri = runCatching { URI(value) }.getOrNull() ?: return null
    val scheme = uri.scheme?.lowercase()
    return if ((scheme == "http" || scheme == "https") && !uri.host.isNullOrEmpty()) scheme else null
}

private fun hostOf(value: String): String? =
    runCatching { URI(value) }.getOrNull()?.host
