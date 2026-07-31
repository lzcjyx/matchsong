package matchsong.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import matchsong.app.design.MatchSongTheme
import matchsong.app.navigation.AppNavHost
import matchsong.app.navigation.Routes

/**
 * M2.1-1 应用入口：挂载 Navigation Compose NavHost。
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MatchSongTheme {
                MatchSongApp()
            }
        }
    }
}

@Composable
fun MatchSongApp() {
    val navController = rememberNavController()
    // M2.3-2 启动分流：根据 ConsentRecord 决定 SPLASH/ONBOARDING/HOME
    AppNavHost(
        navController = navController,
        startDestination = Routes.SPLASH,
    )
}
