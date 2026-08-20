package kr.co.clipnote.core.net

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kr.co.clipnote.core.model.ClipMetadata
import kr.co.clipnote.core.model.ClipsResponse
import kr.co.clipnote.core.model.CreateClipInput
import kr.co.clipnote.core.model.CreateClipResult
import kr.co.clipnote.core.model.DbClip
import kr.co.clipnote.core.model.DeleteAccountResult
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody

/** clipnote.co.kr API. 엔드포인트 구성은 iOS `APIClient` 와 1:1 이다. */
class ApiClient(
    private val baseUrl: HttpUrl,
    private val client: OkHttpClient = defaultClient(),
) {
    constructor(baseUrl: String, client: OkHttpClient = defaultClient()) :
        this(baseUrl.toHttpUrl(), client)

    /** 목록 조회 결과. `failed` 를 **빈 목록과 구분해서** 돌려준다 — 아래 `getClips` 주석 참고. */
    data class ClipsResult(val loggedIn: Boolean, val clips: List<DbClip>, val failed: Boolean)

    /** `GET /api/metadata?url=` — 실패하면 예외를 던진다(호출부가 오류 문구를 고른다). */
    suspend fun fetchMetadata(url: String): ClipMetadata = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(baseUrl.newBuilder().addPathSegments("api/metadata").addQueryParameter("url", url).build())
            .build()
        client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            check(response.isSuccessful) { "metadata ${response.code}" }
            ClipNoteJson.decodeFromString<ClipMetadata>(body)
        }
    }

    /** `POST /api/clip` — 공유 카드 생성(또는 `save=true` 로 내 클립에 바로 저장). */
    suspend fun createClip(input: CreateClipInput, accessToken: String?): CreateClipResult =
        withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url(baseUrl.newBuilder().addPathSegments("api/clip").build())
                .post(ClipNoteJson.encodeToString(input).asJsonBody())
                .authorized(accessToken)
                .build()
            runCatching {
                client.newCall(request).execute().use { response ->
                    val body = response.body?.string().orEmpty()
                    val decoded = runCatching {
                        ClipNoteJson.decodeFromString<CreateClipResult>(body)
                    }.getOrNull()
                    if (!response.isSuccessful) {
                        CreateClipResult(error = decoded?.error ?: "clip ${response.code}")
                    } else {
                        // 2xx 인데 본문을 못 읽으면 성공으로 본다(error = null).
                        decoded ?: CreateClipResult()
                    }
                }
            }.getOrElse { CreateClipResult(error = "network") }
        }

    /** `/api/og` 미리보기 이미지 주소. 서버가 만드는 OG 이미지와 같은 인자를 쓴다. */
    fun ogImageUrl(title: String, description: String?, siteName: String?, gradient: String): String =
        baseUrl.newBuilder()
            .addPathSegments("api/og")
            .addQueryParameter("title", title)
            .addQueryParameter("g", gradient)
            .apply {
                if (!description.isNullOrEmpty()) addQueryParameter("desc", description)
                if (!siteName.isNullOrEmpty()) addQueryParameter("site", siteName)
            }
            .build()
            .toString()

    /**
     * `GET /api/clips`.
     *
     * 실패를 `(false, [])` 로 흘리면 **로그아웃 상태의 빈 목록과 구분되지 않는다** — 지하철에서
     * 목록을 열면 DB 에 클립이 있는데도 "아직 저장한 클립이 없어요" 가 뜨고, 사용자는 클립이
     * 날아간 줄 안다. 그래서 `failed` 를 따로 돌려준다(웹 `a6a4984` 와 같은 이유).
     */
    suspend fun getClips(accessToken: String?): ClipsResult = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(baseUrl.newBuilder().addPathSegments("api/clips").build())
            .authorized(accessToken)
            .build()
        runCatching {
            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    ClipsResult(loggedIn = false, clips = emptyList(), failed = true)
                } else {
                    val decoded = ClipNoteJson.decodeFromString<ClipsResponse>(body)
                    ClipsResult(decoded.loggedIn, decoded.clips, failed = false)
                }
            }
        }.getOrElse { ClipsResult(loggedIn = false, clips = emptyList(), failed = true) }
    }

    /** `PATCH /api/clip/{slug}` — 주어진 필드만 보낸다(null 은 "건드리지 마라"). */
    suspend fun updateClip(
        slug: String,
        title: String? = null,
        tags: List<String>? = null,
        shared: Boolean? = null,
        accessToken: String?,
    ): Boolean = withContext(Dispatchers.IO) {
        val patch: JsonObject = buildJsonObject {
            if (title != null) put("title", title)
            if (tags != null) put("tags", buildJsonArray { tags.forEach { add(JsonPrimitive(it)) } })
            if (shared != null) put("shared", shared)
        }
        val request = Request.Builder()
            .url(baseUrl.newBuilder().addPathSegments("api/clip/$slug").build())
            .patch(patch.toString().asJsonBody())
            .authorized(accessToken)
            .build()
        request.succeeds()
    }

    /** `DELETE /api/clip/{slug}`. */
    suspend fun deleteClip(slug: String, accessToken: String?): Boolean = withContext(Dispatchers.IO) {
        Request.Builder()
            .url(baseUrl.newBuilder().addPathSegments("api/clip/$slug").build())
            .delete()
            .authorized(accessToken)
            .build()
            .succeeds()
    }

    /** `DELETE /api/account` — 계정과 모든 클립을 영구 삭제. */
    suspend fun deleteAccount(accessToken: String?): DeleteAccountResult = withContext(Dispatchers.IO) {
        if (accessToken == null) return@withContext DeleteAccountResult(ok = false, error = "no_token")
        val request = Request.Builder()
            .url(baseUrl.newBuilder().addPathSegments("api/account").build())
            .delete()
            .authorized(accessToken)
            .build()
        runCatching {
            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    DeleteAccountResult(ok = true)
                } else {
                    val error = runCatching {
                        ClipNoteJson.decodeFromString<CreateClipResult>(body).error
                    }.getOrNull()
                    DeleteAccountResult(ok = false, error = error ?: "account ${response.code}")
                }
            }
        }.getOrElse { DeleteAccountResult(ok = false, error = "network") }
    }

    private fun Request.succeeds(): Boolean = runCatching {
        client.newCall(this).execute().use { it.isSuccessful }
    }.getOrElse { false }

    private fun Request.Builder.authorized(accessToken: String?): Request.Builder =
        apply { if (accessToken != null) header("Authorization", "Bearer $accessToken") }

    companion object {
        private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()

        private fun String.asJsonBody(): RequestBody = toRequestBody(JSON_MEDIA)

        fun defaultClient(): OkHttpClient = OkHttpClient.Builder().build()
    }
}
