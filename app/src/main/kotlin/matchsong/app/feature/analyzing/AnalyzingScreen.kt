package matchsong.app.feature.analyzing

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import matchsong.app.design.MatchSongSpacing
import matchsong.app.design.components.state.ErrorState
import matchsong.app.design.components.state.LoadingState

/**
 * M8.1-1 分析中页面：真实分析状态展示（Loading → Done 自动跳转；Error 重试）。
 */
@Composable
fun AnalyzingScreen(
    onDone: () -> Unit,
    state: AnalyzingViewModel.UiState,
) {
    when (state) {
        AnalyzingViewModel.UiState.Idle -> Unit

        AnalyzingViewModel.UiState.Analyzing -> {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(MatchSongSpacing.L),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CircularProgressIndicator()
                Text(
                    text = "正在分析你的声音…",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(top = MatchSongSpacing.M),
                )
            }
        }

        is AnalyzingViewModel.UiState.Done -> {
            LoadingState(text = "分析完成，即将展示结果")
        }

        is AnalyzingViewModel.UiState.Error -> {
            ErrorState(
                message = (state as AnalyzingViewModel.UiState.Error).message,
                onRetry = onDone,
            )
        }
    }
}
