package matchsong.app.feature.home

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

/** M2.4-2 首页：开始测试 / 历史 / 设置 三入口。 */
@Composable
fun HomeScreen(
    onStartTest: () -> Unit,
    onHistory: () -> Unit,
    onSettings: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = "MatchSong", style = MaterialTheme.typography.headlineLarge)
        Text(text = "录几句，找到适合你的歌", style = MaterialTheme.typography.bodyLarge)
        Button(onClick = onStartTest, modifier = Modifier.padding(top = 24.dp)) { Text("开始测试") }
        Button(onClick = onHistory, modifier = Modifier.padding(top = 8.dp)) { Text("历史记录") }
        Button(onClick = onSettings, modifier = Modifier.padding(top = 8.dp)) { Text("设置") }
    }
}
