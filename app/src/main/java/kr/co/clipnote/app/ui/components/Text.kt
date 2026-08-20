package kr.co.clipnote.app.ui.components

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Color

/**
 * 문장 안의 낱말만 굵게.
 *
 * 사전에는 `%s` 가 든 **온전한 문장 하나만** 둔다 — 앞/뒤로 쪼개 적으면 어순이 다른 언어에서
 * 깨진다(ko `기존 태그에 {추가}` vs en `{Add} to existing tags`). 화면에서 끼워 넣은 낱말을
 * 기준으로 그 조각만 다시 찾아 강조한다(웹 `interpolateNode` 와 같은 방식).
 *
 * 낱말이 문장에 없으면 강조 없이 그린다 — 번역이 자리표시자를 빠뜨렸을 때 화면이 깨지는 것보다 낫다.
 */
fun emphasized(sentence: String, words: List<String>, accent: Color): AnnotatedString =
    buildAnnotatedString {
        append(sentence)
        for (word in words) {
            if (word.isEmpty()) continue
            val start = sentence.indexOf(word)
            if (start < 0) continue
            addStyle(
                SpanStyle(fontWeight = FontWeight.Bold, color = accent),
                start,
                start + word.length,
            )
        }
    }
