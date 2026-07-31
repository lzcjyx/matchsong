package matchsong.domain.recording

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * M3.1-1 权限状态机测试：全部转移路径（FR-REC-5）。
 */
class PermissionStateMachineTest {
    @Test
    fun `request from not requested goes to requesting`() {
        val sm = PermissionStateMachine()
        assertEquals(PermissionState.REQUESTING, sm.onEvent(PermissionEvent.Request))
    }

    @Test
    fun `granted result goes to granted`() {
        val sm = PermissionStateMachine()
        sm.onEvent(PermissionEvent.Request)
        assertEquals(
            PermissionState.GRANTED,
            sm.onEvent(PermissionEvent.PermissionResult(granted = true, shouldShowRationale = true)),
        )
    }

    @Test
    fun `denied with rationale goes to denied and can retry`() {
        val sm = PermissionStateMachine()
        sm.onEvent(PermissionEvent.Request)
        assertEquals(
            PermissionState.DENIED,
            sm.onEvent(PermissionEvent.PermissionResult(granted = false, shouldShowRationale = true)),
        )
        assertTrue(sm.canRequest())
        assertEquals(PermissionState.REQUESTING, sm.onEvent(PermissionEvent.Request))
    }

    @Test
    fun `denied without rationale goes to permanently denied and cannot retry`() {
        val sm = PermissionStateMachine()
        sm.onEvent(PermissionEvent.Request)
        assertEquals(
            PermissionState.PERMANENTLY_DENIED,
            sm.onEvent(PermissionEvent.PermissionResult(granted = false, shouldShowRationale = false)),
        )
        assertFalse(sm.canRequest())
        // 再次 Request 被忽略
        assertEquals(PermissionState.PERMANENTLY_DENIED, sm.onEvent(PermissionEvent.Request))
    }

    @Test
    fun `granted state keeps on app resumed`() {
        val sm = PermissionStateMachine(PermissionState.GRANTED)
        assertEquals(PermissionState.GRANTED, sm.onEvent(PermissionEvent.AppResumed))
    }

    @Test
    fun `device unavailable from any state`() {
        for (initial in PermissionState.entries) {
            val sm = PermissionStateMachine(initial)
            assertEquals(PermissionState.UNAVAILABLE, sm.onEvent(PermissionEvent.DeviceUnavailable))
        }
    }

    @Test
    fun `permission result ignored when not requesting`() {
        val sm = PermissionStateMachine(PermissionState.GRANTED)
        assertEquals(
            PermissionState.GRANTED,
            sm.onEvent(PermissionEvent.PermissionResult(granted = false, shouldShowRationale = false)),
        )
    }
}
