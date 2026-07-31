package matchsong.app.feature.recording

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import matchsong.domain.recording.PermissionEvent
import matchsong.domain.recording.PermissionState
import matchsong.domain.recording.PermissionStateMachine
import matchsong.domain.recording.RecordingPort
import matchsong.domain.recording.RecordingState
import matchsong.domain.recording.VolumeLevel
import javax.inject.Inject

/**
 * M3.1-2 / M3.6-2 录音流程 ViewModel。
 *
 * - 权限状态机（FR-REC-5）：请求/拒绝/永久拒绝/设置返回/不可用；
 * - 录音状态流与音量流（经 [RecordingPort]）。
 */
@HiltViewModel
class RecordingViewModel
    @Inject
    constructor(
        private val recordingPort: RecordingPort,
        permissionStateMachine: PermissionStateMachine,
    ) : ViewModel() {
        private val permissionMachine = permissionStateMachine

        private val _permissionState = MutableStateFlow(permissionMachine.state)
        val permissionState: StateFlow<PermissionState> = _permissionState.asStateFlow()

        private val _recordingState = MutableStateFlow<RecordingState>(RecordingState.IDLE)
        val recordingState: StateFlow<RecordingState> = _recordingState.asStateFlow()

        private val _volume = MutableStateFlow<VolumeLevel?>(null)
        val volume: StateFlow<VolumeLevel?> = _volume.asStateFlow()

        init {
            viewModelScope.launch {
                recordingPort.stateFlow.collect { _recordingState.value = it }
            }
            viewModelScope.launch {
                recordingPort.volumeFlow.collect { _volume.value = it }
            }
        }

        /** 用户点击"开始测试"：请求权限（状态机驱动）。 */
        fun requestPermission() {
            permissionMachine.onEvent(PermissionEvent.Request)
            _permissionState.value = permissionMachine.state
            // 实际系统请求由 UI 层 rememberLauncherForActivityResult 发起；
            // 本方法仅驱动状态机（Request 状态标记）。
        }

        /** 系统权限请求结果回调（UI 注入）。 */
        fun onPermissionResult(
            granted: Boolean,
            shouldShowRationale: Boolean,
        ) {
            permissionMachine.onEvent(
                PermissionEvent.PermissionResult(granted, shouldShowRationale),
            )
            _permissionState.value = permissionMachine.state
            if (granted) {
                startRecording()
            }
        }

        /** 从设置返回（onResume 注入，ACC-3）。 */
        fun onAppResumed() {
            permissionMachine.onEvent(PermissionEvent.AppResumed)
            _permissionState.value = permissionMachine.state
        }

        /** 设备无麦克风（M3.3 初始化失败联动）。 */
        fun onDeviceUnavailable() {
            permissionMachine.onEvent(PermissionEvent.DeviceUnavailable)
            _permissionState.value = permissionMachine.state
        }

        /** 开始录音（经前台服务）。 */
        fun startRecording() {
            recordingPort.startRecording()
        }

        /** 停止录音。 */
        fun stopRecording() {
            recordingPort.stopRecording()
        }
    }
