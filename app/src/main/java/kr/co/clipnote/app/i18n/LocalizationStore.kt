package kr.co.clipnote.app.i18n

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.os.ConfigurationCompat
import kr.co.clipnote.app.data.AppPrefs

/**
 * 표시 언어 상태 + 문자열 조회. 언어를 바꾸면 **앱을 다시 켜지 않고** 화면이 갱신된다.
 *
 * 안드로이드 기본 동작은 시스템 언어(또는 per-app language)를 따르는 것이고, 앱 안에서 즉시
 * 바꾸려면 그 언어로 만든 `Resources` 에서 직접 읽어야 한다. `language` 가 Compose 상태라
 * 값이 바뀌면 `t(...)` 를 쓰는 화면이 다시 그려진다.
 *
 * 번역이 빠진 키는 시스템 리소스 폴백이 기본값(`values/` = 한국어)으로 떨어뜨린다 —
 * iOS 가 `t(_:)` 안에서 한국어로 폴백하는 것과 결과가 같다.
 */
class LocalizationStore(context: Context, private val prefs: AppPrefs) {
    private val appContext = context.applicationContext

    var language: AppLanguage by mutableStateOf(initialLanguage(appContext, prefs))
        private set

    private var resources: Resources = resourcesFor(appContext, language)

    fun select(language: AppLanguage) {
        if (language == this.language) return
        this.language = language
        resources = resourcesFor(appContext, language)
        prefs.languageTag = language.tag
    }

    fun t(resId: Int): String = resources.getString(resId)

    fun t(resId: Int, vararg args: Any): String = resources.getString(resId, *args)

    private companion object {
        fun initialLanguage(context: Context, prefs: AppPrefs): AppLanguage {
            AppLanguage.fromTag(prefs.languageTag)?.let { return it }
            val locales = ConfigurationCompat.getLocales(context.resources.configuration)
            val preferred = (0 until locales.size()).mapNotNull { locales[it] }
            // 시스템 언어로 정해졌어도 **결과를 적어 둔다** — 다음 실행에서 시스템 설정이
            // 바뀌었다고 앱 언어까지 따라 바뀌면, 사용자는 자기가 안 건드린 게 바뀐 걸로 본다.
            return AppLanguage.matchingSystem(preferred).also { prefs.languageTag = it.tag }
        }

        fun resourcesFor(context: Context, language: AppLanguage): Resources {
            val configuration = Configuration(context.resources.configuration)
            configuration.setLocale(language.locale)
            return context.createConfigurationContext(configuration).resources
        }
    }
}

/**
 * 화면에서 문자열을 읽는 통로.
 *
 * `stringResource()` 를 쓰지 않는 이유는 그게 **시스템 언어**를 따르기 때문이다. 설정에서 고른
 * 언어로 그리려면 이 스토어를 거쳐야 한다.
 */
val LocalI18n = compositionLocalOf<LocalizationStore> {
    error("LocalizationStore 가 주입되지 않았다")
}
