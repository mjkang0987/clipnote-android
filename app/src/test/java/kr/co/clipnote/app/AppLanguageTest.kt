package kr.co.clipnote.app

import kr.co.clipnote.app.i18n.AppLanguage
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class AppLanguageTest {
    @Test
    fun `시스템 선호 언어 중 첫 지원 언어를 고른다`() {
        assertEquals(
            AppLanguage.JAPANESE,
            AppLanguage.matchingSystem(listOf(Locale.forLanguageTag("ja-JP"))),
        )
        assertEquals(
            AppLanguage.ENGLISH,
            AppLanguage.matchingSystem(listOf(Locale.forLanguageTag("fr-FR"), Locale.forLanguageTag("en-US"))),
        )
    }

    @Test
    fun `중국어 변형은 모두 간체로 본다`() {
        // 지금 지원이 간체 하나뿐이라 그렇다. 번체를 추가하면 이 분기를 세분화해야 한다.
        assertEquals(
            AppLanguage.CHINESE_SIMPLIFIED,
            AppLanguage.matchingSystem(listOf(Locale.forLanguageTag("zh-Hant-TW"))),
        )
    }

    @Test
    fun `지원 언어가 없으면 원본 언어로`() {
        assertEquals(
            AppLanguage.KOREAN,
            AppLanguage.matchingSystem(listOf(Locale.forLanguageTag("de-DE"))),
        )
        assertEquals(AppLanguage.KOREAN, AppLanguage.matchingSystem(emptyList()))
    }

    @Test
    fun `태그로 되찾을 수 있다`() {
        AppLanguage.entries.forEach { language ->
            assertEquals(language, AppLanguage.fromTag(language.tag))
        }
    }
}
