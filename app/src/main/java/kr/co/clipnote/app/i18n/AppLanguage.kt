package kr.co.clipnote.app.i18n

import java.util.Locale

/**
 * 앱 안에서 고를 수 있는 표시 언어.
 * 웹·iOS 의 지원 목록과 **같은 코드·같은 순서**를 유지한다. 한쪽만 늘리지 않는다.
 */
enum class AppLanguage(val tag: String, val label: String) {
    /** 각 언어를 그 언어로 표기한다 — 영어만 아는 사용자가 "영어"를 못 찾는 일을 막는다. */
    KOREAN("ko", "한국어"),
    ENGLISH("en", "English"),
    JAPANESE("ja", "日本語"),
    CHINESE_SIMPLIFIED("zh-Hans", "简体中文"),
    ;

    /** 리소스 조회에 쓸 로케일. `zh-Hans` 는 스크립트 표기라 그대로 만들면 안 된다. */
    val locale: Locale
        get() = when (this) {
            CHINESE_SIMPLIFIED -> Locale.forLanguageTag("zh-Hans-CN")
            else -> Locale.forLanguageTag(tag)
        }

    companion object {
        fun fromTag(tag: String?): AppLanguage? = entries.firstOrNull { it.tag == tag }

        /**
         * 시스템 선호 언어 중 가장 먼저 맞는 지원 언어. 없으면 한국어(원본 언어).
         *
         * 중국어는 번체·홍콩 등 변형이 많은데 지원은 간체 하나뿐이라 `zh` 로 시작하면 모두
         * 간체로 본다. 번체를 추가하면 이 분기를 세분화해야 한다.
         */
        fun matchingSystem(preferred: List<Locale>): AppLanguage {
            for (locale in preferred) {
                if (locale.language == "zh") return CHINESE_SIMPLIFIED
                fromTag(locale.language)?.let { return it }
            }
            return KOREAN
        }
    }
}
