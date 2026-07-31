package matchsong.app.feature.voice

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

/** M5.7 声音结果页（M2 占位：M5 展示音域/舒适音区/置信度）。 */
@Composable
fun VoiceResultScreen(onSeeRecommendations: () -> Unit) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = "你的声音分析", style = MaterialTheme.typography.headlineMedium)
        Text(
            text = "稳定音域：C3 - A4（示例）\n舒适音区：E3 - E4（示例）\n* 以上为本次录音估计",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(vertical = 16.dp),
        )
        Button(onClick = onSeeRecommendations) { Text("查看推荐歌曲") }
    }
}
