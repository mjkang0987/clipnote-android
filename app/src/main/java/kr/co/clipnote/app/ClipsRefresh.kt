package kr.co.clipnote.app

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * 내 클립 목록을 다시 읽으라는 신호.
 *
 * 목록을 바꾸는 곳이 목록 화면만이 아니다 — 옮기기·탈퇴·로컬 화면의 삭제가 모두 목록을
 * 낡게 만든다. 화면끼리 서로를 알지 않도록 신호 하나를 두고 각자 구독한다.
 */
object ClipsRefresh {
    private val _events = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val events: SharedFlow<Unit> = _events.asSharedFlow()

    fun emit() {
        _events.tryEmit(Unit)
    }
}
