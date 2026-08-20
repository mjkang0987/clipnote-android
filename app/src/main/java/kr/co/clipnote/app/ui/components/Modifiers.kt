package kr.co.clipnote.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed

/** 리플 없이 영역 전체를 누를 수 있게. 카드·행처럼 자체 배경이 있는 것에 쓴다. */
fun Modifier.clickableRow(onClick: () -> Unit): Modifier = composed {
    clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        onClick = onClick,
    )
}

/** Compose 밖에서 만든 값을 화면 단위로 기억할 때 쓰는 짧은 별칭. */
@Composable
fun <T> rememberOnce(factory: () -> T): T = remember { factory() }
