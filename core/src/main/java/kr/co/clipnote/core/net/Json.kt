package kr.co.clipnote.core.net

import kotlinx.serialization.json.Json

/**
 * 앱 전역 JSON 설정.
 *
 * - `ignoreUnknownKeys` — 서버가 필드를 늘려도 앱이 깨지지 않는다.
 * - `explicitNulls = false` — null 필드를 **보내지 않는다.** 서버가 `null`(값을 비워라)과
 *   미지정(건드리지 마라)을 다르게 다루기 때문이다. iOS 의 `encodeIfPresent` 대응.
 */
val ClipNoteJson: Json = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
    encodeDefaults = false
}
