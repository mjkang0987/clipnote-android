package kr.co.clipnote.app.ui.clips

import kr.co.clipnote.app.data.LocalClipStore
import kr.co.clipnote.core.model.CreateClipInput
import kr.co.clipnote.core.model.UClip
import kr.co.clipnote.core.net.ApiClient

/** 옮기기 결과. */
data class MigrateResult(val uploaded: Int, val allOk: Boolean)

/**
 * 이 기기의 클립을 계정으로 옮긴다.
 *
 * **전량 업로드에 성공했을 때만 로컬을 비운다.** 일부만 올라간 상태에서 지우면, 실패한 것은
 * 서버에도 기기에도 없다 — 되돌릴 방법이 없는 손실이다. 남겨 두면 다시 시도할 수 있다.
 */
class MigrateLocalClips(
    private val api: ApiClient,
    private val localClips: LocalClipStore,
) {
    suspend fun run(accessToken: String?): MigrateResult {
        val clips = localClips.all()
        if (clips.isEmpty()) return MigrateResult(0, allOk = true)

        var uploaded = 0
        for (clip in clips) {
            val result = api.createClip(clip.toCreateInput(), accessToken)
            if (result.error == null) uploaded += 1
        }

        val allOk = uploaded == clips.size
        if (allOk) localClips.clear()
        return MigrateResult(uploaded, allOk)
    }
}

private fun UClip.toCreateInput(): CreateClipInput = CreateClipInput(
    url = url,
    title = title,
    description = description,
    image = image,
    siteName = siteName,
    tags = tags,
    gradient = gradient,
    save = true,
)
