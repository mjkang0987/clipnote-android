package kr.co.clipnote.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/**
 * 생성자 인자가 있는 ViewModel 을 만드는 최소 팩토리.
 *
 * `remember { ... }` 로 대신하면 화면 회전에서 상태가 날아간다 — URL 을 붙여넣고 제목을
 * 고치던 중에 그러면 처음부터 다시 해야 한다. DI 프레임워크를 얹을 만큼의 일은 아니라
 * 이 여덟 줄로 끝낸다.
 */
class VmFactory<VM : ViewModel>(private val build: () -> VM) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = build() as T
}
