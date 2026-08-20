package kr.co.clipnote.core.auth

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

/**
 * OAuth PKCE 쌍.
 *
 * 공개 클라이언트(모바일 앱)는 client_secret 을 안전하게 담을 수 없어서, 대신 매번 만든
 * verifier 의 해시를 미리 보내고 교환할 때 원본을 제시한다. 중간에서 authorization code 를
 * 가로채도 verifier 없이는 토큰으로 바꾸지 못한다.
 */
data class PkcePair(val verifier: String, val challenge: String) {
    companion object {
        private val random = SecureRandom()
        private val encoder: Base64.Encoder = Base64.getUrlEncoder().withoutPadding()

        fun generate(): PkcePair {
            val bytes = ByteArray(64).also(random::nextBytes)
            val verifier = encoder.encodeToString(bytes)
            val digest = MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray(Charsets.US_ASCII))
            return PkcePair(verifier, encoder.encodeToString(digest))
        }
    }
}

/** 네이버 state 에 실어 보내는 일회용 nonce. */
fun randomNonce(length: Int = 10): String {
    val alphabet = "abcdefghijklmnopqrstuvwxyz0123456789"
    val random = SecureRandom()
    return buildString { repeat(length) { append(alphabet[random.nextInt(alphabet.length)]) } }
}
