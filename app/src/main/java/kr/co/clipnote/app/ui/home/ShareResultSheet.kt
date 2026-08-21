package kr.co.clipnote.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kr.co.clipnote.app.R
import kr.co.clipnote.app.i18n.LocalI18n
import kr.co.clipnote.app.ui.LocalContainer
import kr.co.clipnote.app.ui.components.GhostButton
import kr.co.clipnote.app.ui.components.PrimaryButton
import kr.co.clipnote.app.ui.components.SecondaryButton
import kr.co.clipnote.app.ui.openInBrowser
import kr.co.clipnote.app.ui.theme.AppColor
import kr.co.clipnote.app.ui.theme.Radius
import kr.co.clipnote.app.ui.theme.Radius
import kr.co.clipnote.app.ui.theme.ShapeSm
import kr.co.clipnote.core.util.buildShareText

/** 공유 링크가 만들어진 뒤 — 복사·열기·내 클립에 저장. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareResultSheet(
    title: String,
    description: String?,
    url: String,
    onSave: suspend () -> Boolean,
    onDismiss: () -> Unit,
) {
    val i18n = LocalI18n.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState()

    var copied by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var saved by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = AppColor.surface,
        shape = RoundedCornerShape(topStart = Radius.lg, topEnd = Radius.lg),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(i18n.t(R.string.home_result_title), fontSize = 17.sp, fontWeight = FontWeight.Bold, color = AppColor.fg)
            Text(i18n.t(R.string.home_result_body), fontSize = 13.sp, color = AppColor.fgMuted)

            Text(
                url,
                fontSize = 13.sp,
                color = AppColor.fg,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppColor.bg, ShapeSm)
                    .border(1.dp, AppColor.border, ShapeSm)
                    .padding(horizontal = 12.dp, vertical = 12.dp),
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PrimaryButton(
                    label = i18n.t(if (copied) R.string.homeActions_copied else R.string.homeActions_copyLink),
                    modifier = Modifier.weight(1f),
                ) {
                    copyToClipboard(context, buildShareText(title, description, url))
                    scope.launch { copied = true; delay(1500); copied = false }
                }
                GhostButton(i18n.t(R.string.home_result_open), modifier = Modifier.weight(1f)) {
                    openInBrowser(context, url)
                }
            }

            SecondaryButton(
                label = i18n.t(
                    when {
                        saved -> R.string.home_result_savedToClips
                        saving -> R.string.homeActions_saving
                        else -> R.string.homeActions_saveToClips
                    }
                ),
                enabled = !saving && !saved,
                loading = saving,
            ) {
                scope.launch {
                    saving = true
                    val ok = onSave()
                    saving = false
                    if (ok) saved = true
                }
            }

            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text(i18n.t(R.string.home_result_close), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = AppColor.fgMuted)
            }
        }
    }
}
