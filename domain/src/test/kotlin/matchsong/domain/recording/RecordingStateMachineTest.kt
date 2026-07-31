package matchsong.domain.recording

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * M3.4-1 录音状态机测试：全状态转移路径（FR-REC-6）。
 */
class RecordingStateMachineTest {
    private fun happyPath(): RecordingStateMachine {
        val sm = RecordingStateMachine()
        sm.onEvent(RecordingEvent.Start)
        sm.onEvent(RecordingEvent.Prepared)
        sm.onEvent(RecordingEvent.RecordingStarted)
        return sm
    }

    @Test
    fun `full happy path with countdown ticks`() {
        val sm = RecordingStateMachine()
        assertEquals(RecordingState.PREPARING, sm.onEvent(RecordingEvent.Start))
        assertEquals(RecordingState.COUNTDOWN, sm.onEvent(RecordingEvent.Prepared))
        assertEquals(3, sm.countdownRemaining)
        assertEquals(RecordingState.COUNTDOWN, sm.onEvent(RecordingEvent.Tick))
        assertEquals(2, sm.countdownRemaining)
        assertEquals(RecordingState.COUNTDOWN, sm.onEvent(RecordingEvent.Tick))
        assertEquals(1, sm.countdownRemaining)
        // 第 3 次 tick 后倒计时结束，进入 RECORDING（3 秒倒计时 = 3 次 tick）
        assertEquals(RecordingState.RECORDING, sm.onEvent(RecordingEvent.Tick))
        assertEquals(RecordingState.STOPPING, sm.onEvent(RecordingEvent.UserStop))
        assertEquals(RecordingState.COMPLETED, sm.onEvent(RecordingEvent.Stopped))
        assertFalse(sm.interrupted)
    }

    @Test
    fun `auto stop completes normally`() {
        val sm = happyPath()
        assertEquals(RecordingState.STOPPING, sm.onEvent(RecordingEvent.AutoStop))
        assertEquals(RecordingState.COMPLETED, sm.onEvent(RecordingEvent.Stopped))
    }

    @Test
    fun `focus lost marks interrupted`() {
        val sm = happyPath()
        assertEquals(RecordingState.STOPPING, sm.onEvent(RecordingEvent.FocusLost))
        assertTrue(sm.interrupted)
        assertEquals(RecordingState.COMPLETED, sm.onEvent(RecordingEvent.Stopped))
        assertTrue(sm.interrupted)
    }

    @Test
    fun `error during recording goes to failed with reason`() {
        val sm = happyPath()
        assertEquals(
            RecordingState.FAILED,
            sm.onEvent(RecordingEvent.Error(RecordingFailureReason.READ_ERROR)),
        )
        assertEquals(RecordingFailureReason.READ_ERROR, sm.failureReason)
    }

    @Test
    fun `error during countdown also fails`() {
        val sm = RecordingStateMachine()
        sm.onEvent(RecordingEvent.Start)
        sm.onEvent(RecordingEvent.Prepared)
        assertEquals(
            RecordingState.FAILED,
            sm.onEvent(RecordingEvent.Error(RecordingFailureReason.PERMISSION_REVOKED)),
        )
    }

    @Test
    fun `invalid events ignored`() {
        val sm = RecordingStateMachine()
        // IDLE 时 Stopped/UserStop 均被忽略
        assertEquals(RecordingState.IDLE, sm.onEvent(RecordingEvent.UserStop))
        assertEquals(RecordingState.IDLE, sm.onEvent(RecordingEvent.Stopped))
        // COMPLETED 后 Error 被忽略
        val sm2 = happyPath()
        sm2.onEvent(RecordingEvent.UserStop)
        sm2.onEvent(RecordingEvent.Stopped)
        assertEquals(RecordingState.COMPLETED, sm2.onEvent(RecordingEvent.Error(RecordingFailureReason.UNKNOWN)))
    }

    @Test
    fun `isActive true only in active states`() {
        val sm = happyPath()
        assertTrue(sm.isActive())
        sm.onEvent(RecordingEvent.UserStop)
        assertTrue(sm.isActive())
        sm.onEvent(RecordingEvent.Stopped)
        assertFalse(sm.isActive())
    }

    @Test
    fun `start resets interrupted and failure`() {
        val sm = happyPath()
        sm.onEvent(RecordingEvent.FocusLost)
        sm.onEvent(RecordingEvent.Stopped)
        assertTrue(sm.interrupted)
        // 新会话重置
        sm.onEvent(RecordingEvent.Start)
        assertFalse(sm.interrupted)
        assertEquals(RecordingState.PREPARING, sm.state)
    }
}
