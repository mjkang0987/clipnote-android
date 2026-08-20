package kr.co.clipnote.app.ui.clips

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kr.co.clipnote.app.data.LocalClipStore
import kr.co.clipnote.core.clips.toUClip
import kr.co.clipnote.core.model.UClip
import kr.co.clipnote.core.net.ApiClient
import kr.co.clipnote.core.util.buildShareText
import kr.co.clipnote.core.util.orderedUnique

enum class TagMode { ADD, REPLACE }

/**
 * 내 클립 목록 상태·로직.
 *
 * 목록의 출처는 로그인 여부로 갈린다 — 게스트는 이 기기, 로그인은 계정. **섞지 않는다.**
 * 계정 클립만 공유 링크를 만들 수 있고 다른 기기에서도 보인다. 겉모습이 같은데 할 수 있는
 * 일이 다르면 눌러 보고 나서야 알게 된다. 로그인 상태에서 이 기기에 남은 클립은
 * [localOnlyCount] 로만 알리고 전용 화면이 맡는다.
 */
class ClipsViewModel(
    private val api: ApiClient,
    private val localClips: LocalClipStore,
    private val shareBase: String,
    private val accessToken: suspend () -> String?,
) : ViewModel() {

    /** null = 아직 안 읽음. 빈 목록과 다르다. */
    var clips by mutableStateOf<List<UClip>?>(null)
        private set

    /**
     * 목록에 있는 모든 태그(순서 보존·중복 제거).
     *
     * **목록이 바뀔 때 한 번만 계산한다.** 파생 계산으로 두면 필터 칩과 목록이 각각 읽어서
     * 렌더 한 번에 전체를 두 번씩 훑는다.
     */
    var allTags by mutableStateOf<List<String>>(emptyList())
        private set

    var activeTag by mutableStateOf<String?>(null)

    /**
     * 마지막 조회가 **실패**했는가. 빈 목록과 구분해야 한다.
     *
     * 실패를 빈 목록으로 흘리면 "아직 저장한 클립이 없어요" 가 뜬다. 클립이 사라진 줄 알고
     * 다시 만들게 되는 화면이라, 실패는 실패라고 말하고 다시 시도할 길을 준다.
     */
    var loadFailed by mutableStateOf(false)
        private set

    /** 이 기기에만 남아 있는 클립 수. 로그인 목록 위의 진입 줄이 쓴다. */
    var localOnlyCount by mutableStateOf(0)
        private set

    private var loggedIn = false

    suspend fun load(loggedIn: Boolean) {
        this.loggedIn = loggedIn
        if (!loggedIn) {
            loadFailed = false
            localOnlyCount = 0
            apply(localClips.all())
            return
        }
        localOnlyCount = localClips.count()
        val result = api.getClips(accessToken())
        loadFailed = result.failed
        if (result.failed) {
            // 이미 받아 둔 목록이 있으면 지우지 않는다 — 잠깐 끊겼다고 화면에서 비우면
            // 그것도 사라진 것처럼 보인다. 첫 조회였다면 로딩 상태에서는 빠져나온다.
            if (clips == null) apply(emptyList())
            return
        }
        apply(result.clips.map { it.toUClip() })
    }

    suspend fun reload() = load(loggedIn)

    private fun apply(next: List<UClip>) {
        clips = next
        allTags = orderedUnique(next.flatMap { it.tags })
        // 필터로 걸어 둔 태그가 사라졌으면 필터도 푼다 — 안 그러면 빈 화면에 갇힌다.
        if (activeTag != null && activeTag !in allTags) activeTag = null
    }

    val filtered: List<UClip>
        get() {
            val list = clips ?: return emptyList()
            val tag = activeTag ?: return list
            return list.filter { tag in it.tags }
        }

    /** 공유 복사 텍스트 — 제목 + 브릿지 링크. 로컬 클립은 slug 가 없어 null. */
    fun shareText(clip: UClip): String? {
        val slug = clip.slug ?: return null
        return buildShareText(clip.title, clip.description, "${shareBase.trimEnd('/')}/$slug")
    }

    // MARK: - 변경(각자 reload 로 마무리)

    private suspend fun removeOne(clip: UClip) {
        if (clip.local) localClips.delete(clip.url)
        else clip.slug?.let { api.deleteClip(it, accessToken()) }
    }

    suspend fun delete(clip: UClip) {
        removeOne(clip)
        reload()
    }

    /**
     * 다중선택 일괄 삭제.
     *
     * 서버 왕복을 **동시에** 보낸다. 순차로 기다리면 10개를 지울 때 왕복 10번을 줄줄이
     * 기다린다 — 모바일 네트워크에서 체감이 크다.
     */
    suspend fun bulkDelete(ids: Set<String>) {
        val targets = lookup(ids)
        if (targets.isEmpty()) return
        forEachConcurrently(targets) { removeOne(it) }
        reload()
    }

    suspend fun saveEdit(clip: UClip, title: String, tags: List<String>) {
        if (clip.local) localClips.update(clip.url, title = title, tags = tags)
        else clip.slug?.let { api.updateClip(it, title = title, tags = tags, accessToken = accessToken()) }
        reload()
    }

    /** 공유 링크 켜기(shared=true). */
    suspend fun makeShared(clip: UClip): Boolean {
        val slug = clip.slug ?: return false
        val ok = api.updateClip(slug, shared = true, accessToken = accessToken())
        if (ok) reload()
        return ok
    }

    /** 태그 일괄 — add 는 기존∪신규(중복 제거·최대 6), replace 는 신규만(최대 6). */
    suspend fun applyTags(ids: Set<String>, tags: List<String>, mode: TagMode) {
        val targets = lookup(ids)
        if (targets.isEmpty()) return
        forEachConcurrently(targets) { clip ->
            val next = when (mode) {
                TagMode.ADD -> orderedUnique(clip.tags + tags).take(6)
                TagMode.REPLACE -> tags.take(6)
            }
            if (clip.local) localClips.update(clip.url, tags = next)
            else clip.slug?.let { api.updateClip(it, tags = next, accessToken = accessToken()) }
        }
        reload()
    }

    /** id 목록을 클립으로. 하나마다 배열을 훑으면 선택이 늘수록 제곱으로 느려진다. */
    private fun lookup(ids: Set<String>): List<UClip> =
        clips.orEmpty().filter { it.id in ids }

    /**
     * 상한을 두고 동시에 실행한다.
     *
     * 무제한으로 풀면 선택이 많을 때(로컬 상한 300개) 서버를 때린다.
     */
    private suspend fun forEachConcurrently(clips: List<UClip>, work: suspend (UClip) -> Unit) {
        val gate = Semaphore(MAX_IN_FLIGHT)
        coroutineScope {
            clips.map { clip -> async { gate.withPermit { work(clip) } } }.forEach { it.await() }
        }
    }

    private companion object {
        const val MAX_IN_FLIGHT = 6
    }
}
