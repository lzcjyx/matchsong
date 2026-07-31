package matchsong.app.feature.recording

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** M2.4-2 录音准备页：测试方式选择与提示。M3 接真实权限。 */
@Composable
fun PrepareScreen(onStartRecording: () -> Unit) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = "录音准备", style = MaterialTheme.typography.headlineMedium)
        Text(
            text = "清唱 15-30 秒\n找一个安静环境，手机距离嘴 15-30cm",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(vertical = 16.dp),
        )
        Button(onClick = onStartRecording) { Text("开始录音") }
    }
}

/** M2.4-2 录音页：M3 前为占位（debug 构建走 FakeAudioRecorder 模拟）。 */
@Composable
fun RecordingScreen(onFinished: () -> Unit) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = "模拟录音中…", style = MaterialTheme.typography.headlineMedium)
        Text(text = "（M2 阶段：Fake 录音；M3 接入真实 AudioRecord）", style = MaterialTheme.typography.bodyMedium)
        Button(onClick = onFinished, modifier = Modifier.padding(top = 24.dp)) { Text("停止录音") }
    }
}
