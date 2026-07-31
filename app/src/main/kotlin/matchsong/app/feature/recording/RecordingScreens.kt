package matchsong.app.feature.recording

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import matchsong.app.design.MatchSongSpacing
import matchsong.app.design.components.PrimaryButton
import matchsong.app.design.components.state.PermissionState
import matchsong.domain.recording.PermissionState
import matchsong.domain.recording.RecordingState

/**
 * M3.1-2 录音准备页：权限请求 + 拒绝/永久拒绝处理（ACC-3）。
 */
@Composable
fun PrepareScreen(
    onStartRecording: () -> Unit,
    viewModel: RecordingViewModel = hiltViewModel(),
) {
    val permissionState by viewModel.permissionState.collectAsState()
    val context = LocalContext.current

    val permissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) { granted ->
            // shouldShowRationale 由系统状态机在 UI 层补充判定（M3.1-1 注释：状态机经结果事件驱动）
            viewModel.onPermissionResult(granted, shouldShowRationale = !granted)
        }

    // onResume 重新判定（设置返回后刷新，ACC-3）
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) viewModel.onAppResumed()
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        // 清理 observer 由 LaunchedEffect 生命周期管理（简化：单次注册）
    }

    when (permissionState) {
        PermissionState.NOT_REQUESTED,
        PermissionState.REQUESTING,
        -> {
            PrepareContent(
                onStart = {
                    viewModel.requestPermission()
                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                },
            )
        }

        PermissionState.DENIED -> {
            PrepareContent(
                onStart = {
                    viewModel.requestPermission()
                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                },
                hint = "麦克风权限被拒绝，再次尝试需要授权",
            )
        }

        PermissionState.PERMANENTLY_DENIED -> {
            PermissionState(
                title = "麦克风权限已被永久拒绝",
                description = "请前往系统设置开启麦克风权限后继续",
                onRetry = {
                    val intent =
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                    context.startActivity(intent)
                },
            )
        }

        PermissionState.UNAVAILABLE -> {
            PermissionState(
                title = "未检测到麦克风",
                description = "此设备没有可用的麦克风，无法录音",
                onRetry = {},
            )
        }

        PermissionState.GRANTED -> {
            LaunchedEffect(permissionState) { onStartRecording() }
        }
    }
}

@Composable
private fun PrepareContent(
    onStart: () -> Unit,
    hint: String? = null,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(MatchSongSpacing.L),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = "录音准备", style = MaterialTheme.typography.headlineMedium)
        Text(
            text = "清唱 15-30 秒\n找一个安静环境，手机距离嘴 15-30cm",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(vertical = MatchSongSpacing.M),
        )
        if (hint != null) {
            Text(hint, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
        }
        PrimaryButton(text = "开始录音", onClick = onStart, modifier = Modifier.padding(top = MatchSongSpacing.M))
    }
}

/**
 * M3.6-2 录音页：倒计时 → 录音中（音量条/时长）→ 停止。
 */
@Composable
fun RecordingScreen(
    onFinished: () -> Unit,
    viewModel: RecordingViewModel = hiltViewModel(),
) {
    val recordingState by viewModel.recordingState.collectAsState()
    val volume by viewModel.volume.collectAsState()
    val context = LocalContext.current

    // 录音完成后跳转（Completed → 质量结果页）
    LaunchedEffect(recordingState) {
        if (recordingState == RecordingState.COMPLETED) onFinished()
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(MatchSongSpacing.L),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        when (recordingState) {
            RecordingState.IDLE,
            RecordingState.PREPARING,
            -> Text("准备录音…", style = MaterialTheme.typography.headlineMedium)

            RecordingState.COUNTDOWN -> Text("倒计时 3…", style = MaterialTheme.typography.headlineLarge)

            RecordingState.RECORDING -> {
                Text("录音中…", style = MaterialTheme.typography.headlineMedium)
                // 音量条（FR-REC-4）：0..1 归一化 RMS
                val level = volume?.rms?.coerceIn(0.0, 1.0) ?: 0.0
                LinearProgressIndicator(
                    progress = { level.toFloat() },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = MatchSongSpacing.M)
                            .height(8.dp),
                )
                Row(modifier = Modifier.padding(vertical = MatchSongSpacing.S)) {
                    if (volume?.isTooQuiet == true) {
                        Text(
                            "声音过低",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                    if (volume?.isClipping == true) {
                        Text("削波", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelLarge)
                    }
                    if (volume?.hasInput == false) {
                        Text(
                            "无输入",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
                OutlinedButton(
                    onClick = viewModel::stopRecording,
                    modifier = Modifier.padding(top = MatchSongSpacing.L),
                ) {
                    Text("停止录音")
                }
            }

            RecordingState.STOPPING -> Text("正在停止…", style = MaterialTheme.typography.headlineMedium)

            RecordingState.COMPLETED -> Text("完成", style = MaterialTheme.typography.headlineMedium)

            RecordingState.FAILED -> {
                Text("录音失败", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.error)
                Button(onClick = onFinished) { Text("返回重试") }
            }
        }
    }
}
