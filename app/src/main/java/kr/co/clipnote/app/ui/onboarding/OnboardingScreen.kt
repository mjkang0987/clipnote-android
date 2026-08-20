package kr.co.clipnote.app.ui.onboarding

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kr.co.clipnote.app.R
import kr.co.clipnote.app.i18n.LocalI18n
import kr.co.clipnote.app.ui.components.PrimaryButton
import kr.co.clipnote.app.ui.components.TagChip
import kr.co.clipnote.app.ui.components.clickableRow
import kr.co.clipnote.app.ui.home.HomeScreen
import kr.co.clipnote.app.ui.theme.AppColor
import kr.co.clipnote.app.ui.theme.ShapeMd
import kr.co.clipnote.app.ui.theme.ShapeSm

/**
 * 온보딩 — **실제 홈 화면 위에** 스포트라이트를 얹어 "어느 영역이 어느 기능인지" 를 짚는다.
 *
 * 슬라이드 이미지를 쓰지 않는 이유는, 그림으로 배운 위치와 실제 화면이 어긋나기 시작하는 게
 * 시간 문제이기 때문이다. 진짜 화면을 비추면 어긋날 수가 없다. 대신 그 화면은 눌리지 않는다 —
 * 투어 중에 저장이 일어나면 안 된다.
 */
@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    val i18n = LocalI18n.current
    val anchors = remember { SnapshotStateMap<TourAnchor, Rect>() }
    var index by remember { mutableStateOf(0) }

    val steps = listOf(
        TourStep(
            TourAnchor.URL,
            i18n.t(R.string.onboarding_urlTitle),
            i18n.t(R.string.onboarding_urlDesc),
            i18n.t(R.string.onboarding_audienceAnyone),
        ),
        TourStep(
            TourAnchor.OPTIONS,
            i18n.t(R.string.onboarding_optionsTitle),
            i18n.t(R.string.onboarding_optionsDesc),
            i18n.t(R.string.onboarding_audienceAnyone),
        ),
        TourStep(
            TourAnchor.SAVE,
            i18n.t(R.string.onboarding_saveTitle),
            i18n.t(R.string.onboarding_saveDesc),
            i18n.t(R.string.onboarding_audienceAnyone),
        ),
        TourStep(
            TourAnchor.SHARE,
            i18n.t(R.string.onboarding_shareTitle),
            i18n.t(R.string.onboarding_shareDesc),
            i18n.t(R.string.onboarding_audienceLogin),
        ),
        // 홈 버튼 셋 중 `원본 복사` 만 설명이 없었다. 로그인 없이도 쓸 수 있는 기능이라
        // 게스트에게 특히 알려줄 값어치가 있다.
        TourStep(
            TourAnchor.COPY_ORIGINAL,
            i18n.t(R.string.onboarding_copyOriginalTitle),
            i18n.t(R.string.onboarding_copyOriginalDesc, i18n.t(R.string.homeActions_copyOriginal)),
            i18n.t(R.string.onboarding_audienceAnyone),
        ),
        // 툴바처럼 좌표를 잡기 어려운 대상은 구멍 대신 목업으로 안내한다.
        TourStep(
            null,
            i18n.t(R.string.onboarding_clipsTitle),
            i18n.t(R.string.onboarding_clipsDesc, i18n.t(R.string.common_myClips)),
            null,
        ),
    )

    val step = steps[index]
    val rect = step.anchor?.let { anchors[it] }?.takeIf { it.width > 0f }?.inflate(8f)

    Box(modifier = Modifier.fillMaxSize()) {
        CompositionLocalProvider(LocalTourAnchors provides anchors) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    // 투어 중에는 실제 UI 를 조작할 수 없다. 이벤트를 내려가기 전에 삼킨다.
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                awaitPointerEvent(PointerEventPass.Initial).changes.forEach { it.consume() }
                            }
                        }
                    },
            ) {
                HomeScreen(vmKey = "tour")
            }
        }

        Spotlight(rect) { if (step.anchor != null) index = (index + 1).coerceAtMost(steps.lastIndex) }

        Column(modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(16.dp)) {
            // 강조 영역이 화면 아래쪽이면 말풍선을 위로 올린다 — 안 그러면 겹쳐서 가린다.
            val screenHeightPx = with(LocalDensity.current) {
                LocalConfiguration.current.screenHeightDp.dp.toPx()
            }
            val calloutOnTop = rect != null && rect.center.y > screenHeightPx * 0.5f
            if (!calloutOnTop) Spacer(modifier = Modifier.weight(1f))
            if (step.anchor == null) ClipsPreviewMock(modifier = Modifier.padding(bottom = 16.dp))
            Callout(
                step = step,
                index = index,
                total = steps.size,
                onPrevious = { index = (index - 1).coerceAtLeast(0) },
                onNext = { if (index == steps.lastIndex) onDone() else index += 1 },
                onSkip = onDone,
            )
            if (calloutOnTop) Spacer(modifier = Modifier.weight(1f))
        }
    }
}

/** dim + 구멍. 구멍은 `BlendMode.Clear` 로 뚫는다(별도 레이어라야 동작한다). */
@Composable
private fun Spotlight(rect: Rect?, onTap: () -> Unit) {
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            // alpha 를 1 미만으로 두면 오프스크린 레이어가 생겨 Clear 가 화면 전체를 지우지 않는다.
            .graphicsLayer(alpha = 0.99f)
            .clickableRow(onTap),
    ) {
        drawRect(Color.Black.copy(alpha = 0.62f))
        if (rect != null) {
            drawRoundRect(
                color = Color.Transparent,
                topLeft = Offset(rect.left, rect.top),
                size = Size(rect.width, rect.height),
                cornerRadius = CornerRadius(16f, 16f),
                blendMode = BlendMode.Clear,
            )
            drawRoundRect(
                color = AppColor.brand,
                topLeft = Offset(rect.left, rect.top),
                size = Size(rect.width, rect.height),
                cornerRadius = CornerRadius(16f, 16f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f),
            )
        }
    }
}

@Composable
private fun Callout(
    step: TourStep,
    index: Int,
    total: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSkip: () -> Unit,
) {
    val i18n = LocalI18n.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColor.bg, ShapeMd)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        step.audience?.let {
            Text(
                it,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = AppColor.brandStrong,
                modifier = Modifier
                    .background(AppColor.brandSoft, ShapeSm)
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            )
        }
        Text(step.title, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = AppColor.fg)
        Text(step.desc, fontSize = 14.sp, lineHeight = 21.sp, color = AppColor.fgMuted)

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                "${index + 1} / $total",
                fontSize = 12.sp,
                color = AppColor.fgMuted,
                modifier = Modifier.weight(1f),
            )
            if (index > 0) {
                TextButton(onClick = onPrevious) {
                    Text(i18n.t(R.string.onboarding_previous), color = AppColor.fgMuted, fontSize = 14.sp)
                }
            }
            TextButton(onClick = onSkip) {
                Text(i18n.t(R.string.onboarding_skip), color = AppColor.fgMuted, fontSize = 14.sp)
            }
        }
        PrimaryButton(
            label = i18n.t(if (index == total - 1) R.string.onboarding_start else R.string.onboarding_next),
            onClick = onNext,
        )
    }
}

/** ‘내 클립’ 목업 — 툴바 버튼은 좌표로 짚기 어려워, 그 화면이 어떻게 생겼는지를 대신 보여 준다. */
@Composable
private fun ClipsPreviewMock(modifier: Modifier = Modifier) {
    val i18n = LocalI18n.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(AppColor.bg, ShapeMd)
            .border(1.dp, AppColor.border, ShapeMd)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        MockRow(i18n.t(R.string.onboarding_mockTitle1), i18n.t(R.string.onboarding_mockTag1))
        MockRow(i18n.t(R.string.onboarding_mockTitle2), i18n.t(R.string.onboarding_mockTag2))
    }
}

@Composable
private fun MockRow(title: String, tag: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColor.surface, ShapeSm)
            .padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(AppColor.brandSoft, ShapeSm)
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = AppColor.fg)
            TagChip(tag, small = true)
        }
    }
}
