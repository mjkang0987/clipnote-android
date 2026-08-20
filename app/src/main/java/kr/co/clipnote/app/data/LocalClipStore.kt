package kr.co.clipnote.app.data

import kr.co.clipnote.core.model.UClip
import kr.co.clipnote.core.util.orderedUnique
import kr.co.clipnote.core.util.parseTags

/**
 * 이 기기에 남는 클립 저장소.
 *
 * 규칙은 웹·iOS 와 같다 — 같은 url 은 최신으로 덮고, 최대 [MAX_CLIPS] 개, 최신순.
 * 자주 쓴 태그(`knownTags`)는 빈도 맵으로 따로 센다.
 */
class LocalClipStore(
    private val dao: LocalClipDao,
    private val prefs: AppPrefs,
    private val maxClips: Int = MAX_CLIPS,
) {
    companion object {
        const val MAX_CLIPS = 300
    }

    suspend fun all(): List<UClip> = dao.all().map(LocalClipEntity::toUClip)

    suspend fun count(): Int = dao.count()

    suspend fun save(
        url: String,
        title: String,
        description: String?,
        image: String?,
        siteName: String?,
        gradient: String,
        tags: List<String>,
    ) {
        dao.upsert(
            LocalClipEntity(
                url = url,
                title = title,
                description = description,
                image = image,
                siteName = siteName,
                gradient = gradient,
                tags = tags.joinToString(","),
                savedAt = System.currentTimeMillis(),
            )
        )
        dao.trimTo(maxClips)
        recordTags(tags)
    }

    suspend fun delete(url: String) = dao.delete(url)

    /** 단건 편집 — 준 필드만 갱신한다. */
    suspend fun update(url: String, title: String? = null, tags: List<String>? = null) {
        val existing = dao.find(url) ?: return
        dao.upsert(
            existing.copy(
                title = title ?: existing.title,
                tags = tags?.joinToString(",") ?: existing.tags,
            )
        )
        if (tags != null) recordTags(tags)
    }

    /** 로그인 마이그레이션이 **전량 성공했을 때만** 부른다. */
    suspend fun clear() = dao.clear()

    /** 자주 쓴 순으로 정렬된 과거 태그(자동완성용). */
    fun knownTags(): List<String> =
        prefs.knownTags().entries.sortedByDescending { it.value }.map { it.key }

    private fun recordTags(tags: List<String>) {
        val map = prefs.knownTags().toMutableMap()
        var changed = false
        for (tag in tags) {
            val key = tag.trim()
            if (key.isEmpty()) continue
            map[key] = (map[key] ?: 0) + 1
            changed = true
        }
        if (changed) prefs.setKnownTags(map)
    }
}

/** 로컬 클립 → 통합 뷰 모델. id = url, 공유 불가(slug 가 없다). */
fun LocalClipEntity.toUClip(): UClip = UClip(
    id = url,
    slug = null,
    url = url,
    title = title,
    description = description,
    image = image,
    siteName = siteName,
    gradient = gradient,
    tags = orderedUnique(parseTags(tags)),
    shared = false,
    local = true,
    savedAt = savedAt,
)
