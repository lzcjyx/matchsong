package matchsong.app.feature.onboarding

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

/**
 * M2.3-1 Onboarding 页面：隐私与录音说明（六项）+ 同意操作。
 * 不同意 = 不操作停留在本页（MVP 停留策略，ACC-2）。
 * 本阶段不请求真实权限（PLAN M2.3：权限流程在 M3 完成）。
 */
@Composable
fun OnboardingScreen(onAgree: () -> Unit) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = "欢迎使用 MatchSong", style = MaterialTheme.typography.headlineMedium)
        Text(
            text =
                "隐私说明（六项）\n" +
                    "1. 为什么需要麦克风：录制你的演唱以分析音域\n" +
                    "2. 录音用于什么：仅用于本次音域分析\n" +
                    "3. 是否上传音频：不上传（全部本地处理）\n" +
                    "4. 是否保存音频：默认不保存（临时缓存，分析后删除）\n" +
                    "5. 如何删除数据：设置页可删除全部数据\n" +
                    "6. 结果非医学或专业诊断",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(vertical = 16.dp),
        )
        Button(onClick = onAgree) { Text("同意并继续") }
    }
}
