package kr.co.clipnote.app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import kr.co.clipnote.app.R
import kr.co.clipnote.app.ads.AdBanner
import kr.co.clipnote.app.i18n.LocalI18n
import kr.co.clipnote.app.ui.LocalContainer
import kr.co.clipnote.app.ui.LocalNav
import kr.co.clipnote.app.ui.LocalRouter
import kr.co.clipnote.app.ui.Route
import kr.co.clipnote.app.ui.theme.AppColor

/**
 * 상위 화면의 공통 뼈대 — 좌측 메뉴가 달린 상단 바 + 하단 광고 배너.
 *
 * 배너를 화면마다 따로 달면 한 곳만 빠뜨리기 쉽다. 한 자리에 모아 둔다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScaffold(
    title: String,
    modifier: Modifier = Modifier,
    showMenu: Boolean = true,
    onBack: (() -> Unit)? = null,
    showAds: Boolean = true,
    actions: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    content: @Composable () -> Unit,
) {
    Scaffold(
        modifier = modifier,
        containerColor = AppColor.bg,
        topBar = {
            TopAppBar(
                title = {
                    Text(title, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = AppColor.fg)
                },
                navigationIcon = {
                    when {
                        onBack != null -> IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = AppColor.fg)
                        }

                        showMenu -> HeaderMenu()
                        else -> Unit
                    }
                },
                actions = { actions() },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppColor.bg,
                    titleContentColor = AppColor.fg,
                ),
            )
        },
        bottomBar = {
            Column {
                bottomBar()
                if (showAds) AdBanner()
            }
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            content()
        }
    }
}

/**
 * 좌측 햄버거 메뉴 — 화면 이동 + 로그인/로그아웃 + 개인정보.
 *
 * iOS 는 사이드 슬라이드였는데 안드로이드는 상단 오버플로 메뉴가 관례라 그쪽을 따랐다.
 * 항목과 순서는 같다.
 */
@Composable
fun HeaderMenu() {
    val i18n = LocalI18n.current
    val router = LocalRouter.current
    val nav = LocalNav.current
    val auth by LocalContainer.current.auth.state.collectAsState()
    var open by remember { mutableStateOf(false) }

    fun close(action: () -> Unit) {
        open = false
        action()
    }

    Box {
        IconButton(onClick = { open = true }) {
            Icon(Icons.Filled.Menu, contentDescription = i18n.t(R.string.menu_aria), tint = AppColor.fg)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }, containerColor = AppColor.bg) {
            MenuRow(i18n.t(R.string.clips_newClip)) { close { nav.home() } }
            MenuRow(i18n.t(R.string.common_myClips)) { close { nav.go(Route.CLIPS) } }
            MenuRow(i18n.t(R.string.menu_tour)) { close { router.showTour = true } }
            MenuRow(i18n.t(R.string.menu_about)) { close { nav.go(Route.ABOUT) } }
            MenuRow(i18n.t(R.string.faq_title)) { close { nav.go(Route.FAQ) } }

            HorizontalDivider(color = AppColor.border)

            if (auth.loggedIn) {
                MenuRow(i18n.t(R.string.common_settings)) { close { nav.go(Route.SETTINGS) } }
                MenuRow(i18n.t(R.string.common_logout)) { close { router.confirmLogout = true } }
            } else {
                MenuRow(i18n.t(R.string.common_login)) { close { router.showLogin = true } }
            }

            HorizontalDivider(color = AppColor.border)

            MenuRow(i18n.t(R.string.common_privacy)) { close { nav.go(Route.PRIVACY) } }
        }
    }
}

@Composable
private fun MenuRow(label: String, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(label, color = AppColor.fg, fontSize = 15.sp) },
        onClick = onClick,
    )
}

/** 툴바 오른쪽의 글자 버튼(‘내 클립’·‘선택’·‘취소’). */
@Composable
fun ToolbarTextButton(label: String, color: Color = AppColor.brandStrong, onClick: () -> Unit) {
    TextButton(onClick = onClick) {
        Text(label, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = color)
    }
}
