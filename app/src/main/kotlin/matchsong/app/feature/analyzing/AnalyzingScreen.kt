package matchsong.app.feature.analyzing

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** M5 分析中页面（M2 占位：进度展示，M5 接真实 YIN 分析管线）。 */
@Composable
fun AnalyzingScreen(onDone: () -> Unit) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator()
        Text(
            text = "正在分析你的声音…",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(top = 16.dp),
        )
        Text(text = "（M2 占位：YIN 分析在 M5 实现）", style = MaterialTheme.typography.bodyMedium)
        Button(onClick = onDone, modifier = Modifier.padding(top = 24.dp)) { Text("查看结果（模拟）") }
    }
}
