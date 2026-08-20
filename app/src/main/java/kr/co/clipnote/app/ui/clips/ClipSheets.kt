package kr.co.clipnote.app.ui.clips

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kr.co.clipnote.app.R
import kr.co.clipnote.app.i18n.LocalI18n
import kr.co.clipnote.app.ui.components.GhostButton
import kr.co.clipnote.app.ui.components.PrimaryButton
import kr.co.clipnote.app.ui.components.clickableRow
import kr.co.clipnote.app.ui.theme.AppColor
import kr.co.clipnote.app.ui.theme.ShapeFull
import kr.co.clipnote.app.ui.theme.ShapeSm
import kr.co.clipnote.core.util.parseTags

/** 단건 편집 — 제목·태그. 저장 방식(로컬/DB)은 호출부가 정한다. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditClipSheet(
    initialTitle: String,
    initialTags: List<String>,
    onSubmit: suspend (String, List<String>) -> Unit,
    onDismiss: () -> Unit,
) {
    val i18n = LocalI18n.current
    val scope = rememberCoroutineScope()
    var title by remember { mutableStateOf(initialTitle) }
    var tagInput by remember { mutableStateOf(initialTags.joinToString(", ")) }
    var saving by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
        containerColor = AppColor.bg,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(i18n.t(R.string.clips_editTitle), fontSize = 17.sp, fontWeight = FontWeight.Bold, color = AppColor.fg)

            SheetLabel(i18n.t(R.string.clips_editTitleLabel))
            SheetField(title, { title = it }, i18n.t(R.string.clips_editTitleLabel))

            // 라벨과 보조 설명은 사전에서 따로 둔다 — 웹도 굵게/흐리게로 나눠 그린다.
            SheetLabel("${i18n.t(R.string.clips_editTagsLabel)} ${i18n.t(R.string.clips_editTagsNote)}")
            SheetField(tagInput, { tagInput = it }, i18n.t(R.string.clips_editTagsPlaceholder))

            Row(
                modifier = Modifier.padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                GhostButton(i18n.t(R.string.common_cancel), modifier = Modifier.weight(1f), onClick = onDismiss)
                PrimaryButton(
                    label = i18n.t(if (saving) R.string.clips_savingEdit else R.string.common_save),
                    modifier = Modifier.weight(1f),
                    enabled = !saving,
                    loading = saving,
                ) {
                    if (saving) return@PrimaryButton
                    scope.launch {
                        saving = true
                        // 제목을 비워 저장하면 원래 제목을 지키는 게 사용자가 기대하는 동작이다.
                        val finalTitle = title.trim().ifEmpty { initialTitle }
                        onSubmit(finalTitle, parseTags(tagInput))
                        saving = false
                        onDismiss()
                    }
                }
            }
        }
    }
}

/** 선택한 클립에 태그 일괄 적용 — 추가(기존∪신규) / 교체(기존 무시). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagApplySheet(
    count: Int,
    onApply: (List<String>, TagMode) -> Unit,
    onDismiss: () -> Unit,
) {
    val i18n = LocalI18n.current
    var tagInput by remember { mutableStateOf("") }
    var mode by remember { mutableStateOf(TagMode.ADD) }

    val addLabel = i18n.t(R.string.clips_bulkTagModeAddEmphasis)
    val replaceLabel = i18n.t(R.string.clips_bulkTagModeReplaceEmphasis)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
        containerColor = AppColor.bg,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(i18n.t(R.string.clips_bulkTagTitle), fontSize = 17.sp, fontWeight = FontWeight.Bold, color = AppColor.fg)
            // 대상 수는 제목에 괄호로 붙이지 않고 별도 줄에 둔다 — 수량 표기(`3개`/`3 clips`)는
            // 언어마다 단위 위치가 달라 문장에서 떼어 만든다.
            Text(
                i18n.t(R.string.clips_bulkTagBody, i18n.t(R.string.clips_countUnit, count)),
                fontSize = 13.sp,
                color = AppColor.fgMuted,
            )

            SheetLabel("${i18n.t(R.string.clips_editTagsLabel)} ${i18n.t(R.string.clips_editTagsNote)}")
            SheetField(tagInput, { tagInput = it }, i18n.t(R.string.clips_editTagsPlaceholder))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ModeChip(addLabel, mode == TagMode.ADD) { mode = TagMode.ADD }
                ModeChip(replaceLabel, mode == TagMode.REPLACE) { mode = TagMode.REPLACE }
            }
            // 칩 라벨(강조 낱말)을 문장에 끼워 설명을 만든다. 낱말 위치가 언어마다 달라
            // 앞/뒤로 쪼개 적을 수 없다 — ko `기존 태그에 {추가}` vs en `{Add} to existing tags`.
            Text(
                if (mode == TagMode.ADD) i18n.t(R.string.clips_bulkTagModeAdd, addLabel)
                else i18n.t(R.string.clips_bulkTagModeReplace, replaceLabel),
                fontSize = 12.sp,
                color = AppColor.fgMuted,
            )

            Row(
                modifier = Modifier.padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                GhostButton(i18n.t(R.string.common_cancel), modifier = Modifier.weight(1f), onClick = onDismiss)
                PrimaryButton(i18n.t(R.string.clips_bulkTagApply), modifier = Modifier.weight(1f)) {
                    val tags = parseTags(tagInput)
                    // 교체는 빈 태그(전부 지우기)도 뜻이 통하지만, 추가는 넣을 게 있어야 한다.
                    if (mode == TagMode.REPLACE || tags.isNotEmpty()) onApply(tags, mode)
                    onDismiss()
                }
            }
        }
    }
}

@Composable
private fun ModeChip(label: String, active: Boolean, onClick: () -> Unit) {
    Text(
        label,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        color = if (active) AppColor.brandStrong else AppColor.fgMuted,
        modifier = Modifier
            .background(if (active) AppColor.brandSoft else AppColor.surface, ShapeFull)
            .border(1.dp, if (active) AppColor.brand else AppColor.border, ShapeFull)
            .clickableRow(onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@Composable
private fun SheetLabel(text: String) {
    Text(text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = AppColor.fg)
}

@Composable
private fun SheetField(value: String, onValueChange: (String) -> Unit, placeholder: String) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, fontSize = 15.sp, color = AppColor.fgMuted) },
        singleLine = true,
        shape = ShapeSm,
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = AppColor.fg,
            unfocusedTextColor = AppColor.fg,
            cursorColor = AppColor.fg,
            focusedContainerColor = AppColor.surface,
            unfocusedContainerColor = AppColor.surface,
            focusedBorderColor = AppColor.brand,
            unfocusedBorderColor = AppColor.border,
        ),
    )
}
