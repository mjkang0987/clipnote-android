package kr.co.clipnote.core.theme

/** 클립 카드 배경 그라디언트. 색은 0xRRGGBB 정수 — Compose 에 기대지 않으려고 원시값으로 둔다. */
data class ClipGradient(val name: String, val from: Int, val to: Int)

val GRADIENTS: List<ClipGradient> = listOf(
    ClipGradient("sunset", 0xFF6B6B, 0xFFA94D),
    ClipGradient("ocean", 0x4F8DFD, 0x6FE0C9),
    ClipGradient("grape", 0x7C5CFC, 0xE879F9),
    ClipGradient("forest", 0x0EA5E9, 0x22C55E),
    ClipGradient("peach", 0xFB7185, 0xFDBA74),
    ClipGradient("midnight", 0x4338CA, 0x7C3AED),
    ClipGradient("mint", 0x06B6D4, 0x34D399),
    ClipGradient("rose", 0xEC4899, 0x8B5CF6),
)

/**
 * 결정적 그라디언트 선택.
 *
 * 웹·iOS 와 **같은 색이 나와야 한다** — 같은 클립이 기기마다 다른 색이면 같은 것으로 안 보인다.
 * JS 의 `(hash * 31 + charCodeAt(i)) | 0` 을 그대로 옮긴다. Kotlin `Char.code` 가 UTF-16 코드
 * 유닛이고 `Int` 곱셈이 32비트 랩어라운드라 JS 의 `|0` 과 결과가 같다.
 */
fun pickGradient(seed: String): ClipGradient {
    var hash = 0
    for (char in seed) {
        hash = hash * 31 + char.code
    }
    // `Math.abs(Int.MIN_VALUE)` 는 자기 자신(음수)이라 인덱스가 음수가 된다. Long 으로 넓혀서 막는다.
    val index = (Math.abs(hash.toLong()) % GRADIENTS.size).toInt()
    return GRADIENTS[index]
}

/** 이름으로 찾고, 없으면 seed 로 고른다(서버가 모르는 이름을 준 경우). */
fun gradientNamed(name: String, seed: String): ClipGradient =
    GRADIENTS.firstOrNull { it.name == name } ?: pickGradient(seed)
