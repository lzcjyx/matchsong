package matchsong.app.feature.favorites

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

/** M8.3 收藏页（M2 占位：M8 接收藏仓库）。 */
@Composable
fun FavoritesScreen(onBack: () -> Unit) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = "收藏", style = MaterialTheme.typography.headlineMedium)
        Text(text = "（M2 占位：收藏在 M8 实现）", style = MaterialTheme.typography.bodyMedium)
        Button(onClick = onBack, modifier = Modifier.padding(top = 24.dp)) { Text("返回") }
    }
}
