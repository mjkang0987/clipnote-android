package kr.co.clipnote.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 비로그인 사용자의 클립 — 이 기기에만 보관한다(공유 링크 없음).
 *
 * `url` 이 기본키다. 같은 링크를 다시 저장하면 새 줄이 생기는 게 아니라 최신으로 덮인다
 * (웹·iOS 의 upsert 규칙과 같다).
 */
@Entity(tableName = "local_clips")
data class LocalClipEntity(
    @PrimaryKey val url: String,
    val title: String,
    val description: String?,
    val image: String?,
    val siteName: String?,
    val gradient: String,
    /** 쉼표로 이어 붙인 태그. 태그는 최대 6개고 쉼표를 허용하지 않아 구분자로 충분하다. */
    val tags: String,
    val savedAt: Long,
)
