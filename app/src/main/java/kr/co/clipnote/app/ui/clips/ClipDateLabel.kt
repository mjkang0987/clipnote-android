package kr.co.clipnote.app.ui.clips

import android.icu.text.RelativeDateTimeFormatter
import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import kr.co.clipnote.app.i18n.LocalI18n
import kr.co.clipnote.core.clips.ClipDateBucket
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * 날짜 묶음의 머리글.
 *
 * **문자열 리소스에 넣지 않는다.** 문구가 넷(오늘·어제·이번 주·이번 달)이라 사전에 넣을 수는
 * 있지만, `2026년 7월` 같은 연월은 **형식 자체가 언어마다 다르다**(en `July 2026`,
 * ja `2026年7月`). 형식은 사전으로 표현할 수 없어 어차피 시스템 포매터가 필요하고, 그러면
 * 넷도 같은 곳에 맡기는 게 일관된다(웹이 `Intl` 에, iOS 가 `RelativeDateTimeFormatter` 에
 * 맡긴 것과 같은 판단).
 *
 * 로케일은 **표시 언어**를 따른다 — 시스템 언어가 아니다. 앱 안에서 언어를 바꿀 수 있으니
 * 머리글도 같이 바뀌어야 한다.
 */
@Composable
fun clipDateLabel(bucket: ClipDateBucket): String {
    val locale = LocalI18n.current.language.locale
    val relative = RelativeDateTimeFormatter.getInstance(locale)
    return when (bucket) {
        ClipDateBucket.Today ->
            relative.format(RelativeDateTimeFormatter.Direction.THIS, RelativeDateTimeFormatter.AbsoluteUnit.DAY)

        ClipDateBucket.Yesterday ->
            relative.format(RelativeDateTimeFormatter.Direction.LAST, RelativeDateTimeFormatter.AbsoluteUnit.DAY)

        ClipDateBucket.ThisWeek ->
            relative.format(RelativeDateTimeFormatter.Direction.THIS, RelativeDateTimeFormatter.AbsoluteUnit.WEEK)

        ClipDateBucket.ThisMonth ->
            relative.format(RelativeDateTimeFormatter.Direction.THIS, RelativeDateTimeFormatter.AbsoluteUnit.MONTH)

        is ClipDateBucket.YearMonth -> {
            // 연·월 배치는 언어마다 다르다. 로케일이 선호하는 패턴을 물어서 쓴다.
            val pattern = DateFormat.getBestDateTimePattern(locale, "yMMMM")
            LocalDate.of(bucket.year, bucket.month, 1)
                .format(DateTimeFormatter.ofPattern(pattern, locale))
        }
    }
}
