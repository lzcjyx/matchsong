package matchsong.app.feature.quality

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

/** M4.5 音频质量结果页（M2 占位）。M4 起展示 AudioQualityReport 与失败原因。 */
@Composable
fun QualityResultScreen(onAnalyze: () -> Unit) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = "音频质量检测", style = MaterialTheme.typography.headlineMedium)
        Text(text = "（M2 占位：质量检测在 M4 实现）", style = MaterialTheme.typography.bodyMedium)
        Button(onClick = onAnalyze, modifier = Modifier.padding(top = 24.dp)) { Text("开始分析") }
    }
}
