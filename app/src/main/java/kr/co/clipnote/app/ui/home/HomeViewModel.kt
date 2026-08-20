package kr.co.clipnote.app.ui.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kr.co.clipnote.app.data.LocalClipStore
import kr.co.clipnote.core.model.ClipMetadata
import kr.co.clipnote.core.model.CreateClipInput
import kr.co.clipnote.core.model.CreateClipResult
import kr.co.clipnote.core.net.ApiClient
import kr.co.clipnote.core.theme.ClipGradient
import kr.co.clipnote.core.theme.pickGradient
import kr.co.clipnote.core.util.isFetchableUrl
import kr.co.clipnote.core.util.parseTags
import kr.co.clipnote.core.util.prettyHost

/**
 * 홈에서 사용자에게 보여 줄 오류.
 *
 * **문자열이 아니라 케이스로 둔다.** 표시 언어를 아는 건 화면이고, 모델이 문장을 만들면 그
 * 시점의 언어로 굳는다 — 언어를 바꿔도 오류만 옛 언어로 남는다.
 */
sealed interface HomeError {
    data object MetaFailed : HomeError
    data object TitleRequiredForLink : HomeError
    data object TitleRequiredForClip : HomeError

    /** 링크 생성 실패. 서버가 사유를 주면 그걸 쓰고(이미 사람이 읽는 문장), 없으면 사전 문구. */
    data class LinkCreateFailed(val server: String?) : HomeError

    /** 서버가 준 메시지를 그대로 보여 준다. */
    data class Server(val message: String) : HomeError
}

/** 홈 화면 상태·로직 — URL 디바운스 메타 추출 + 저장/공유 입력 조립. */
class HomeViewModel(
    private val api: ApiClient,
    private val localClips: LocalClipStore,
) : ViewModel() {

    var url by mutableStateOf("")
        private set
    var title by mutableStateOf("")
    var tagInput by mutableStateOf("")

    var meta by mutableStateOf<ClipMetadata?>(null)
        private set
    var loading by mutableStateOf(false)
        private set
    var error by mutableStateOf<HomeError?>(null)

    private var debounceJob: Job? = null
    private var fetchedUrl: String? = null

    /** 직전 URL — 이번 변경이 타이핑인지 붙여넣기인지 가르는 데만 쓴다. */
    private var previousUrl = ""

    // MARK: - 파생값

    val tags: List<String> get() = parseTags(tagInput)
    val trimmedUrl: String get() = url.trim()
    val hasInput: Boolean get() = trimmedUrl.isNotEmpty()
    val noMeta: Boolean get() = meta?.source == "none"
    val metaReason: String? get() = meta?.reason

    private val trimmedTitle: String get() = title.trim()

    /** 그라디언트 시드 — 웹·iOS 와 같은 우선순위(입력 제목 > 메타 제목 > url > "clipnote"). */
    val gradient: ClipGradient
        get() {
            val metaTitle = meta?.title
            return when {
                trimmedTitle.isNotEmpty() -> pickGradient(trimmedTitle)
                !metaTitle.isNullOrEmpty() -> pickGradient(metaTitle)
                else -> pickGradient(url.ifEmpty { "clipnote" })
            }
        }

    /**
     * 저장·전송·미리보기에 쓰는 제목.
     *
     * 비어 있으면 화면이 자리표시자를 대신 그린다 — 자리표시자는 번역 대상이라 모델이 들고
     * 있으면 안 된다.
     */
    val resolvedTitle: String
        get() {
            val metaTitle = meta?.title
            return when {
                trimmedTitle.isNotEmpty() -> trimmedTitle
                !metaTitle.isNullOrEmpty() -> metaTitle
                hasInput -> prettyHost(url)
                else -> ""
            }
        }

    val previewDescription: String? get() = meta?.description
    val previewImage: String? get() = meta?.image
    val previewSiteName: String? get() = meta?.siteName

    // MARK: - 메타 추출

    /**
     * 이번 변경이 **사람이 한 글자 친 것**인가.
     *
     * 아니라면 붙여넣기·공유로 URL 이 통째로 들어온 것이다. 그때는 더 들어올 글자가 없으니
     * 디바운스를 기다릴 이유가 없다 — 이 앱의 주 입력이 붙여넣기라 체감이 그만큼 빨라진다.
     */
    fun isTyping(previous: String, current: String): Boolean =
        current.length == previous.length + 1 && current.startsWith(previous)

    fun onUrlChanged(value: String) {
        url = value
        val target = value.trim()
        val typed = isTyping(previousUrl, target)
        previousUrl = target

        if (fetchedUrl != null && fetchedUrl != target) {
            fetchedUrl = null
            meta = null
            title = ""
        }
        debounceJob?.cancel()
        if (!isFetchableUrl(target) || fetchedUrl == target) return
        debounceJob = viewModelScope.launch {
            if (typed) delay(TYPING_DEBOUNCE_MS)
            loadMeta(target)
        }
    }

    suspend fun loadMeta(target: String) {
        if (!isFetchableUrl(target)) return
        loading = true
        error = null
        try {
            val data = api.fetchMetadata(target)
            meta = data
            fetchedUrl = target
            val fetchedTitle = data.title
            if (!fetchedTitle.isNullOrEmpty() && trimmedTitle.isEmpty()) {
                title = fetchedTitle
            }
        } catch (cancelled: CancellationException) {
            // 다음 글자가 들어와 취소된 것이라 오류가 아니다 — 코루틴 취소는 그대로 올린다.
            throw cancelled
        } catch (_: Exception) {
            error = HomeError.MetaFailed
        } finally {
            loading = false
        }
    }

    // MARK: - 저장 / 공유

    private fun makeInput(save: Boolean?): CreateClipInput? {
        val resolved = resolvedTitle
        if (resolved.isEmpty()) return null
        return CreateClipInput(
            url = trimmedUrl,
            title = resolved,
            description = meta?.description,
            image = meta?.image,
            siteName = meta?.siteName,
            tags = tags,
            gradient = gradient.name,
            save = save,
        )
    }

    /** 게스트: 이 기기에 저장. 제목이 없으면 false. */
    suspend fun saveToDevice(): Boolean {
        val resolved = resolvedTitle
        if (resolved.isEmpty()) return false
        localClips.save(
            url = trimmedUrl,
            title = resolved,
            description = meta?.description,
            image = meta?.image,
            siteName = meta?.siteName,
            gradient = gradient.name,
            tags = tags,
        )
        return true
    }

    /** 로그인: 공유 링크 생성. 실패하면 `error` 를 채우고 null. */
    suspend fun createShare(accessToken: String?): CreateClipResult? {
        val input = makeInput(save = null) ?: run {
            error = HomeError.TitleRequiredForLink
            return null
        }
        error = null
        val result = api.createClip(input, accessToken)
        if (result.error != null || result.shareUrl == null) {
            error = HomeError.LinkCreateFailed(result.error)
            return null
        }
        return result
    }

    /** 로그인: 공유 카드 없이 바로 내 클립(DB)에 저장. */
    suspend fun saveToClips(accessToken: String?): Boolean {
        val input = makeInput(save = true) ?: run {
            error = HomeError.TitleRequiredForClip
            return false
        }
        error = null
        val result = api.createClip(input, accessToken)
        val message = result.error
        if (message != null) {
            error = HomeError.Server(message)
            return false
        }
        return true
    }

    private companion object {
        /** 타이핑 디바운스. 한 글자마다 요청을 보내지 않으려고 둔다. */
        const val TYPING_DEBOUNCE_MS = 600L
    }
}
