package kr.co.clipnote.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kr.co.clipnote.app.ui.theme.AppColor
import kr.co.clipnote.app.ui.theme.ShapeFull
import kr.co.clipnote.app.ui.theme.ShapeMd
import kr.co.clipnote.app.ui.theme.ShapeSm
import kr.co.clipnote.app.ui.theme.clipGradient
import kr.co.clipnote.app.ui.theme.softShadow
import kr.co.clipnote.core.theme.ClipGradient
import kr.co.clipnote.core.util.proxiedImageUrl

/** 태그 칩. */
@Composable
fun TagChip(text: String, small: Boolean = false) {
    Text(
        text = text,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        color = AppColor.brandStrong,
        modifier = Modifier
            .background(AppColor.brandSoft, ShapeFull)
            .padding(horizontal = if (small) 8.dp else 10.dp, vertical = if (small) 2.dp else 4.dp),
    )
}

/**
 * 태그 필터 칩.
 *
 * 활성은 **채운 브랜드 + 흰 글자**다(웹 `ClipsClient` 와 같은 모양). 연보라 배경은 태그 칩
 * (`TagChip`)의 것이라, 필터에도 쓰면 "고른 것" 과 "그냥 태그" 가 같아 보인다.
 *
 * 높이를 44dp 아래로 두지 않는다 — 가이드 §6 의 최소 터치 영역이고, 글자만 기준으로 잡으면
 * 33dp 밖에 안 나온다.
 */
@Composable
fun FilterChip(label: String, active: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .heightIn(min = 44.dp)
            .background(if (active) AppColor.brand else AppColor.bg, ShapeFull)
            .border(1.dp, if (active) AppColor.brand else AppColor.border, ShapeFull)
            .clickableRow(onClick)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
            color = if (active) AppColor.white else AppColor.fgMuted,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * 클립 썸네일 — 그라디언트 배경 위에 원본 대표 이미지.
 *
 * **이미지는 반드시 프록시를 거친다.** 원본 URL 을 직접 부르면 hotlink 차단·referer 요구
 * (네이버 CDN)·혼합 콘텐츠에 걸려 실패한다. 실패해도 그라디언트가 남아서 **눈에는 정상처럼
 * 보이고**, 스크롤할 때마다 실패 요청만 반복된다. 그림 그리는 자리를 여기 하나로 모아 둔 이유다.
 */
@Composable
fun ClipThumbnail(
    imageUrl: String?,
    gradient: ClipGradient,
    apiBase: String,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.clipGradient(gradient)) {
        val proxied = proxiedImageUrl(imageUrl, apiBase)
        if (proxied != null) {
            AsyncImage(
                model = proxied,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/** 목록에서 보일 모습 — 썸네일 + 제목·호스트·태그. */
@Composable
fun ClipCard(
    title: String,
    host: String?,
    imageUrl: String?,
    gradient: ClipGradient,
    tags: List<String>,
    apiBase: String,
    modifier: Modifier = Modifier,
    thumbnailSize: Dp = 56.dp,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .softShadow(ShapeMd)
            .background(AppColor.surface, ShapeMd)
            .border(1.dp, AppColor.border, ShapeMd)
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ClipThumbnail(
            imageUrl = imageUrl,
            gradient = gradient,
            apiBase = apiBase,
            modifier = Modifier.size(thumbnailSize).clip(ShapeMd),
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = AppColor.fg,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (!host.isNullOrEmpty()) {
                Text(host, fontSize = 13.sp, color = AppColor.fgMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (tags.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(top = 2.dp),
                ) {
                    tags.take(4).forEach { TagChip(it, small = true) }
                }
            }
        }
        trailing?.invoke()
    }
}

/**
 * 공유 카드 미리보기 — 서버 `/api/og` 가 만드는 이미지를 화면에서 재현한다.
 *
 * 비율 1200:630 을 지키고 글자 크기는 카드 폭에 비례시킨다(웹의 `cqw` 대응). 원본 대표
 * 이미지가 있으면 그 이미지가 그대로 공유되므로 **글자를 얹지 않는다** — 얹으면 화면에서 본
 * 카드와 실제 공유되는 카드가 달라진다.
 */
@Composable
fun SharePreviewCard(
    title: String,
    description: String?,
    siteName: String?,
    gradient: ClipGradient,
    imageUrl: String?,
    apiBase: String,
    modifier: Modifier = Modifier,
) {
    val hasImage = !imageUrl.isNullOrBlank()
    BoxWithCardWidth(modifier = modifier) { width ->
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1200f / 630f)
                .softShadow(ShapeMd)
                .clip(ShapeMd)
                .clipGradient(gradient),
        ) {
            val proxied = proxiedImageUrl(imageUrl, apiBase)
            if (proxied != null) {
                AsyncImage(
                    model = proxied,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            if (!hasImage) {
                // 하단 스크림 — 그라디언트 위 흰 글자의 가독성.
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                0.5f to Color.Transparent,
                                1f to Color.Black.copy(alpha = 0.28f),
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(width * 0.06f),
                    verticalArrangement = Arrangement.spacedBy(width.value.dp * 0.012f),
                ) {
                    if (!siteName.isNullOrEmpty()) {
                        Text(
                            siteName.uppercase(),
                            fontSize = (width.value * 0.026f).sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.92f),
                            maxLines = 1,
                        )
                    }
                    Text(
                        title,
                        fontSize = (width.value * if (title.length > 40) 0.05f else 0.06f).sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (!description.isNullOrEmpty()) {
                        Text(
                            description,
                            fontSize = (width.value * 0.028f).sp,
                            color = Color.White.copy(alpha = 0.9f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Text(
                        "ClipNote",
                        fontSize = (width.value * 0.026f).sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.95f),
                        modifier = Modifier.padding(top = width.value.dp * 0.008f),
                    )
                }
            }
        }
    }
}

/** 카드 폭을 재서 넘긴다 — 글자 크기를 폭에 비례시키려면 값이 필요하다. */
@Composable
private fun BoxWithCardWidth(
    modifier: Modifier = Modifier,
    content: @Composable (Dp) -> Unit,
) {
    androidx.compose.foundation.layout.BoxWithConstraints(modifier = modifier.fillMaxWidth().wrapContentHeight()) {
        content(maxWidth)
    }
}
