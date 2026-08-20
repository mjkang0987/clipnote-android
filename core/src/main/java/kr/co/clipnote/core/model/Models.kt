package kr.co.clipnote.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** `/api/metadata` 응답 — 링크에서 읽어 낸 미리보기 정보. */
@Serializable
data class ClipMetadata(
    val url: String,
    val title: String? = null,
    val description: String? = null,
    val image: String? = null,
    val siteName: String? = null,
    val source: String = "",
    val reason: String? = null,
)

/** 계정(DB)에 저장된 클립. `slug` 가 공유 링크의 주소가 된다. */
@Serializable
data class DbClip(
    val slug: String,
    val url: String,
    val title: String,
    val description: String? = null,
    val image: String? = null,
    val siteName: String? = null,
    val gradient: String = "",
    val tags: List<String> = emptyList(),
    val saved: Boolean = false,
    val shared: Boolean = false,
    val createdAt: String = "",
)

/**
 * `POST /api/clip` 본문.
 *
 * null 필드는 **보내지 않는다**(`explicitNulls = false` 인 Json 으로 직렬화한다) — 서버가
 * `null` 과 미지정을 다르게 다룬다. iOS `CreateClipInput.encode(to:)` 의 `encodeIfPresent` 대응.
 */
@Serializable
data class CreateClipInput(
    val url: String,
    val title: String,
    val description: String? = null,
    val image: String? = null,
    val siteName: String? = null,
    val tags: List<String>? = null,
    val gradient: String,
    val save: Boolean? = null,
)

@Serializable
data class CreateClipResult(
    val slug: String? = null,
    val shareUrl: String? = null,
    val alreadySaved: Boolean? = null,
    val error: String? = null,
)

@Serializable
data class ClipsResponse(
    val loggedIn: Boolean = false,
    val clips: List<DbClip> = emptyList(),
)

data class DeleteAccountResult(val ok: Boolean, val error: String? = null)

/**
 * 로컬 클립과 DB 클립을 한 화면에서 다루기 위한 통합 뷰 모델.
 *
 * `id` 는 DB 클립이면 slug, 로컬 클립이면 url. `slug` 가 없으면 **공유 링크를 만들 수 없다** —
 * 화면이 그 차이를 이 필드로 판단한다.
 */
data class UClip(
    val id: String,
    val slug: String?,
    val url: String,
    val title: String,
    val description: String?,
    val image: String?,
    val siteName: String?,
    val gradient: String,
    val tags: List<String>,
    val shared: Boolean,
    val local: Boolean,
    /** 저장 시각(epoch millis). 목록을 날짜로 묶는 데 쓴다. */
    val savedAt: Long,
)

/** 인증에 쓰는 소셜 공급자. `id` 는 Supabase 가 아는 이름이다. */
enum class AuthProvider(val id: String, val displayName: String) {
    GOOGLE("google", "Google"),
    KAKAO("kakao", "Kakao"),
    NAVER("naver", "Naver"),
    ;

    companion object {
        fun from(id: String?): AuthProvider? = entries.firstOrNull { it.id == id }
    }
}
