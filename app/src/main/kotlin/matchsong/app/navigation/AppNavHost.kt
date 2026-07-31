package matchsong.app.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import matchsong.app.feature.analyzing.AnalyzingScreen
import matchsong.app.feature.favorites.FavoritesScreen
import matchsong.app.feature.history.HistoryScreen
import matchsong.app.feature.home.HomeScreen
import matchsong.app.feature.onboarding.OnboardingScreen
import matchsong.app.feature.onboarding.OnboardingViewModel
import matchsong.app.feature.quality.QualityResultScreen
import matchsong.app.feature.recommendation.RecommendationDetailScreen
import matchsong.app.feature.recommendation.RecommendationListScreen
import matchsong.app.feature.recording.PrepareScreen
import matchsong.app.feature.recording.RecordingScreen
import matchsong.app.feature.settings.DeleteConfirmScreen
import matchsong.app.feature.settings.SettingsScreen
import matchsong.app.feature.splash.SplashViewModel
import matchsong.app.feature.voice.VoiceResultScreen

/**
 * M2.1-1 导航宿主：注册全部 MVP 路由（FR-SHELL-1）。
 *
 * Splash 按同意状态分流（M2.3-2）；录音流程前进栈约定：
 * Prepare→Recording→QualityResult→Analyzing→VoiceResult→RecommendationList，
 * "重新录制"= popUpTo(Prepare)。
 */
@Suppress("LongMethod") // 路由注册为声明式清单，逐路由拆分会破坏可读性（M2.1-1 决策）
@Composable
fun AppNavHost(
    navController: NavHostController,
    startDestination: String,
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
    ) {
        composable(Routes.SPLASH) {
            SplashScreen(
                onGoOnboarding = {
                    navController.navigate(Routes.ONBOARDING) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                },
                onGoHome = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                },
            )
        }
        composable(Routes.ONBOARDING) {
            val vm: OnboardingViewModel = hiltViewModel()
            val state by vm.state.collectAsState()
            LaunchedEffect(state) {
                if (state == OnboardingViewModel.UiState.Agreed) {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                    }
                }
            }
            OnboardingScreen(onAgree = vm::onAgree)
        }
        composable(Routes.HOME) {
            HomeScreen(onStartTest = {
                navController.navigate(Routes.PREPARE)
            }, onHistory = {
                navController.navigate(
                    Routes.HISTORY,
                )
            }, onSettings = { navController.navigate(Routes.SETTINGS) })
        }
        composable(Routes.PREPARE) { PrepareScreen(onStartRecording = { navController.navigate(Routes.RECORDING) }) }
        composable(Routes.RECORDING) {
            RecordingScreen(onFinished = { navController.navigate(Routes.QUALITY_RESULT) { popUpTo(Routes.PREPARE) } })
        }
        composable(Routes.QUALITY_RESULT) {
            // M4.5：占位报告（演示可分析状态）；M8.1 接入真实录音质量管线
            QualityResultScreen(
                report = matchsong.app.feature.quality.DemoQualityReport,
                onAnalyze = { navController.navigate(Routes.ANALYZING) { popUpTo(Routes.RECORDING) } },
                onRetry = { navController.navigate(Routes.PREPARE) { popUpTo(Routes.RECORDING) } },
            )
        }
        composable(Routes.ANALYZING) {
            AnalyzingScreen(onDone = { navController.navigate(Routes.VOICE_RESULT) { popUpTo(Routes.QUALITY_RESULT) } })
        }
        composable(Routes.VOICE_RESULT) {
            VoiceResultScreen(onSeeRecommendations = { navController.navigate(Routes.RECOMMENDATION_LIST) })
        }
        composable(Routes.RECOMMENDATION_LIST) {
            RecommendationListScreen(onSongClick = {
                    songId ->
                navController.navigate(Routes.recommendationDetail(songId))
            })
        }
        composable(
            route = Routes.RECOMMENDATION_DETAIL,
            arguments =
                listOf(
                    androidx.navigation.navArgument(NavArgs.SONG_ID) { type = androidx.navigation.NavType.StringType },
                ),
        ) { entry ->
            val songId = entry.arguments?.getString(NavArgs.SONG_ID).orEmpty()
            RecommendationDetailScreen(songId = songId, onBack = { navController.popBackStack() })
        }
        composable(Routes.FAVORITES) { FavoritesScreen(onBack = { navController.popBackStack() }) }
        composable(Routes.HISTORY) { HistoryScreen(onBack = { navController.popBackStack() }) }
        composable(Routes.SETTINGS) { SettingsScreen(onDeleteAll = { navController.navigate(Routes.DELETE_CONFIRM) }) }
        composable(Routes.DELETE_CONFIRM) {
            DeleteConfirmScreen(onConfirm = {
                navController.popBackStack(Routes.SETTINGS, inclusive = false)
            }, onCancel = { navController.popBackStack() })
        }
    }
}

@Composable
private fun SplashScreen(
    onGoOnboarding: () -> Unit,
    onGoHome: () -> Unit,
) {
    val vm: SplashViewModel = hiltViewModel()
    val destination by vm.destination.collectAsState()

    LaunchedEffect(destination) {
        when (destination) {
            SplashViewModel.Destination.ONBOARDING -> onGoOnboarding()
            SplashViewModel.Destination.HOME -> onGoHome()
            null -> Unit // 等待状态加载
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator()
        Text("MatchSong")
    }
}
