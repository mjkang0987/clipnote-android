package kr.co.clipnote.app.ui.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned

/** 강조 대상 영역. 실제 UI 요소에 [tourAnchor] 로 붙인다. */
enum class TourAnchor { URL, OPTIONS, SAVE, SHARE, COPY_ORIGINAL }

/**
 * 투어가 켜져 있을 때만 채워지는 좌표 수집함.
 *
 * null 이면 투어 중이 아니다 — 그때 [tourAnchor] 는 아무 일도 하지 않는다. 홈 화면이 평소에도
 * 좌표를 계속 재고 있으면 스크롤할 때마다 쓸데없는 갱신이 돈다.
 */
val LocalTourAnchors = compositionLocalOf<SnapshotStateMap<TourAnchor, Rect>?> { null }

/** 이 요소의 화면 좌표를 투어에 알린다. 투어가 아니면 무해하다. */
fun Modifier.tourAnchor(anchor: TourAnchor): Modifier = composed {
    val sink = LocalTourAnchors.current
    if (sink == null) this
    else onGloballyPositioned { coordinates -> sink[anchor] = coordinates.boundsInRoot() }
}

/** 투어 한 단계. [anchor] 가 null 이면 구멍 없이 목업만 보여 주는 단계다. */
data class TourStep(
    val anchor: TourAnchor?,
    val title: String,
    val desc: String,
    /** 사용 대상 배지. null 이면 배지 없음. */
    val audience: String?,
)
