package kr.co.clipnote.core.auth

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kr.co.clipnote.core.net.ClipNoteJson
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Supabase 인증(GoTrue) REST 클라이언트.
 *
 * **왜 공식 SDK 를 쓰지 않나.** 앱이 쓰는 인증 표면이 네 개뿐이다 — authorize URL 만들기,
 * PKCE code 교환, magiclink token_hash verify, refresh. 이 넷을 위해 SDK 를 얹으면 안드로이드
 * 쪽 의존성 트리가 크게 늘고, 세션 저장 방식도 SDK 안에 숨는다. REST 로 직접 부르면 무엇이
 * 오가는지 코드에 그대로 보이고, 이 파일이 안드로이드에 기대지 않아 유닛 테스트가 된다.
 *
 * iOS 는 `supabase-swift` 를 쓴다 — 두 앱이 다른 길로 같은 엔드포인트를 부르는 셈이라,
 * 서버 쪽 설정(리다이렉트 URL·공급자)은 **양쪽에 똑같이** 되어 있어야 한다.
 */
class SupabaseAuth(
    private val baseUrl: HttpUrl,
    private val anonKey: String,
    private val client: OkHttpClient = OkHttpClient(),
) {
    /** 설정이 없으면 null — 앱은 켜지고 로그인만 꺼진다(잘못 설정된 빌드가 죽지 않게). */
    companion object {
        private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()

        fun createOrNull(url: String, anonKey: String, client: OkHttpClient = OkHttpClient()): SupabaseAuth? {
            val parsed = url.trim().toHttpUrlOrNull() ?: return null
            if (anonKey.isBlank()) return null
            return SupabaseAuth(parsed, anonKey.trim(), client)
        }
    }

    /**
     * 소셜 로그인 authorize URL. 브라우저(Custom Tabs)로 열고, 콜백은 [redirectTo] 딥링크로 온다.
     *
     * PKCE 라서 `code_challenge` 를 여기서 보내고, 돌아온 code 를 [exchangeCode] 에서 verifier 와
     * 함께 제시한다.
     */
    fun authorizeUrl(provider: String, redirectTo: String, pkce: PkcePair): String =
        baseUrl.newBuilder()
            .addPathSegments("auth/v1/authorize")
            .addQueryParameter("provider", provider)
            .addQueryParameter("redirect_to", redirectTo)
            .addQueryParameter("code_challenge", pkce.challenge)
            .addQueryParameter("code_challenge_method", "s256")
            .build()
            .toString()

    /** `POST /auth/v1/token?grant_type=pkce` — authorization code → 세션. */
    suspend fun exchangeCode(code: String, verifier: String): Result<AuthSession> =
        postForSession(
            path = "auth/v1/token",
            query = mapOf("grant_type" to "pkce"),
            body = buildJsonObject {
                put("auth_code", code)
                put("code_verifier", verifier)
            },
        )

    /**
     * `POST /auth/v1/verify` — 네이버 콜백이 준 magiclink token_hash → 세션.
     *
     * 네이버는 Supabase 기본 공급자가 아니라, 우리 서버가 네이버 인증을 확인한 뒤 magiclink 를
     * 발급해 앱으로 돌려보낸다(iOS `lib/naver.ts` 와 같은 흐름).
     */
    suspend fun verifyMagicLink(tokenHash: String): Result<AuthSession> =
        postForSession(
            path = "auth/v1/verify",
            body = buildJsonObject {
                put("type", "magiclink")
                put("token_hash", tokenHash)
            },
        )

    /** `POST /auth/v1/token?grant_type=refresh_token`. */
    suspend fun refresh(refreshToken: String): Result<AuthSession> =
        postForSession(
            path = "auth/v1/token",
            query = mapOf("grant_type" to "refresh_token"),
            body = buildJsonObject { put("refresh_token", refreshToken) },
        )

    /** `POST /auth/v1/logout` — 서버 쪽 세션 무효화. 실패해도 로컬 세션은 비운다. */
    suspend fun signOut(accessToken: String): Boolean = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(baseUrl.newBuilder().addPathSegments("auth/v1/logout").build())
            .post("{}".toRequestBody(JSON_MEDIA))
            .header("apikey", anonKey)
            .header("Authorization", "Bearer $accessToken")
            .build()
        runCatching { client.newCall(request).execute().use { it.isSuccessful } }.getOrElse { false }
    }

    private suspend fun postForSession(
        path: String,
        query: Map<String, String> = emptyMap(),
        body: JsonObject,
    ): Result<AuthSession> = withContext(Dispatchers.IO) {
        val url = baseUrl.newBuilder().addPathSegments(path).apply {
            query.forEach { (name, value) -> addQueryParameter(name, value) }
        }.build()
        val request = Request.Builder()
            .url(url)
            .post(body.toString().toRequestBody(JSON_MEDIA))
            .header("apikey", anonKey)
            .header("Content-Type", "application/json")
            .build()
        runCatching {
            client.newCall(request).execute().use { response ->
                val text = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    throw IllegalStateException(errorMessage(text) ?: "auth ${response.code}")
                }
                parseSession(text)
            }
        }
    }

    /** 토큰 응답을 세션으로. `expires_at` 이 없는 응답도 있어 `expires_in` 으로 채운다. */
    internal fun parseSession(text: String, nowSeconds: Long = System.currentTimeMillis() / 1000): AuthSession {
        val json = ClipNoteJson.parseToJsonElement(text).jsonObject
        val accessToken = json["access_token"]?.jsonPrimitive?.contentOrNull
            ?: error("access_token 이 없다")
        val refreshToken = json["refresh_token"]?.jsonPrimitive?.contentOrNull.orEmpty()
        val expiresAt = json["expires_at"]?.jsonPrimitive?.contentOrNull?.toLongOrNull()
            ?: (nowSeconds + (json["expires_in"]?.jsonPrimitive?.contentOrNull?.toLongOrNull() ?: 3600L))
        val user = json["user"]?.jsonObject
        return AuthSession(
            accessToken = accessToken,
            refreshToken = refreshToken,
            expiresAt = expiresAt,
            email = user?.get("email")?.jsonPrimitive?.contentOrNull,
            provider = user?.get("app_metadata")?.jsonObject?.get("provider")?.jsonPrimitive?.contentOrNull,
        )
    }

    private fun errorMessage(text: String): String? = runCatching {
        val json = ClipNoteJson.parseToJsonElement(text).jsonObject
        json["error_description"]?.jsonPrimitive?.contentOrNull
            ?: json["msg"]?.jsonPrimitive?.contentOrNull
            ?: json["error"]?.jsonPrimitive?.contentOrNull
    }.getOrNull()
}
