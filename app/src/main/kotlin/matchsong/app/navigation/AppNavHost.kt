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
import matchsong.app.feature.analyzing.AnalyzingViewModel
import matchsong.app.feature.favorites.FavoritesScreen
import matchsong.app.feature.history.HistoryScreen
import matchsong.app.feature.home.HomeScreen
import matchsong.app.feature.onboarding.OnboardingScreen
import matchsong.app.feature.onboarding.OnboardingViewModel
import matchsong.app.feature.quality.QualityResultScreen
import matchsong.app.feature.quality.QualityResultViewModel
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
            val flowSession: matchsong.app.feature.flow.FlowSessionViewModel = hiltViewModel()
            val runner = matchsong.core.audio.android.RecordingSessionRunner.instance
            RecordingScreen(
                onFinished = {
                    // M8.1-1：录音完成 → 保存 WAV 路径到会话 → 进入质量检测
                    flowSession.setWavFile(runner?.lastWavFile)
                    navController.navigate(Routes.QUALITY_RESULT) { popUpTo(Routes.PREPARE) }
                },
            )
        }
        composable(Routes.QUALITY_RESULT) {
            val flowSession: matchsong.app.feature.flow.FlowSessionViewModel = hiltViewModel()
            val qualityVm: QualityResultViewModel = hiltViewModel()
            val qualityState by qualityVm.state.collectAsState()
            val wavFile by flowSession.wavFile.collectAsState()
            LaunchedEffect(wavFile) {
                if (wavFile != null) qualityVm.analyze(wavFile)
            }
            // 质量合格 → 自动进入分析（ACC-6）；失败 → 展示原因 + 重录
            LaunchedEffect(qualityState) {
                if (qualityState is QualityResultViewModel.UiState.Result) {
                    val report = (qualityState as QualityResultViewModel.UiState.Result).report
                    flowSession.setQualityReport(report)
                    if (report.isUsable) {
                        navController.navigate(Routes.ANALYZING) { popUpTo(Routes.RECORDING) }
                    }
                }
            }
            when (qualityState) {
                is QualityResultViewModel.UiState.Checking ->
                    matchsong.app.design.components.state.LoadingState(text = "检测录音质量…")
                is QualityResultViewModel.UiState.Result -> {
                    val report = (qualityState as QualityResultViewModel.UiState.Result).report
                    QualityResultScreen(
                        report = report,
                        onAnalyze = { navController.navigate(Routes.ANALYZING) { popUpTo(Routes.RECORDING) } },
                        onRetry = {
                            // M9.2：重录前清理本会话录音文件（FR-PRIV-1）
                            matchsong.core.audio.android.RecordingSessionRunner.instance?.cleanupSessionFiles()
                            flowSession.reset()
                            navController.navigate(Routes.PREPARE) { popUpTo(Routes.RECORDING) }
                        },
                    )
                }
                is QualityResultViewModel.UiState.Error ->
                    matchsong.app.design.components.state.ErrorState(
                        message = (qualityState as QualityResultViewModel.UiState.Error).message,
                        onRetry = {
                            matchsong.core.audio.android.RecordingSessionRunner.instance?.cleanupSessionFiles()
                            flowSession.reset()
                            navController.navigate(Routes.PREPARE) { popUpTo(Routes.RECORDING) }
                        },
                    )
                QualityResultViewModel.UiState.Idle -> Unit
            }
        }
        composable(Routes.ANALYZING) {
            val flowSession: matchsong.app.feature.flow.FlowSessionViewModel = hiltViewModel()
            val analyzingVm: AnalyzingViewModel = hiltViewModel()
            val analyzingState by analyzingVm.state.collectAsState()
            val wavFile by flowSession.wavFile.collectAsState()
            LaunchedEffect(wavFile) {
                if (wavFile != null && analyzingState is AnalyzingViewModel.UiState.Idle) {
                    analyzingVm.analyze(wavFile)
                }
            }
            LaunchedEffect(analyzingState) {
                if (analyzingState is AnalyzingViewModel.UiState.Done) {
                    flowSession.setAnalysisResult(
                        (analyzingState as AnalyzingViewModel.UiState.Done).result,
                    )
                    // M9.2/ACC-14：分析完成即删除原始音频（.pcm/.wav），仅保留派生特征与历史摘要
                    matchsong.core.audio.android.RecordingSessionRunner.instance?.cleanupSessionFiles()
                    flowSession.setWavFile(null)
                    navController.navigate(Routes.VOICE_RESULT) { popUpTo(Routes.QUALITY_RESULT) }
                }
            }
            AnalyzingScreen(
                onDone = {
                    navController.navigate(Routes.VOICE_RESULT) { popUpTo(Routes.QUALITY_RESULT) }
                },
                state = analyzingState,
            )
        }
        composable(Routes.VOICE_RESULT) {
            val flowSession: matchsong.app.feature.flow.FlowSessionViewModel = hiltViewModel()
            val analysisResult by flowSession.analysisResult.collectAsState()
            VoiceResultScreen(
                result = analysisResult,
                onSeeRecommendations = { navController.navigate(Routes.RECOMMENDATION_LIST) },
                onRetry = {
                    matchsong.core.audio.android.RecordingSessionRunner.instance?.cleanupSessionFiles()
                    flowSession.reset()
                    navController.navigate(Routes.PREPARE) { popUpTo(Routes.RECORDING) }
                },
            )
        }
        composable(Routes.RECOMMENDATION_LIST) {
            val flowSession: matchsong.app.feature.flow.FlowSessionViewModel = hiltViewModel()
            val recVm: matchsong.app.feature.recommendation.RecommendationListViewModel = hiltViewModel()
            val recState by recVm.state.collectAsState()
            val analysisResult by flowSession.analysisResult.collectAsState()
            LaunchedEffect(analysisResult) {
                if (recState is matchsong.app.feature.recommendation.RecommendationListViewModel.UiState.Idle) {
                    recVm.load(analysisResult)
                }
            }
            RecommendationListScreen(
                state = recState,
                onSongClick = { songId -> navController.navigate(Routes.recommendationDetail(songId)) },
                onRetry = {
                    matchsong.core.audio.android.RecordingSessionRunner.instance?.cleanupSessionFiles()
                    flowSession.reset()
                    navController.navigate(Routes.PREPARE) { popUpTo(Routes.RECORDING) }
                },
            )
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
        composable(Routes.FAVORITES) {
            FavoritesScreen(
                onBack = { navController.popBackStack() },
                onSongClick = { songId -> navController.navigate(Routes.recommendationDetail(songId)) },
            )
        }
        composable(Routes.HISTORY) {
            HistoryScreen(
                onBack = { navController.popBackStack() },
                // M8.4-2 历史详情（复用推荐详情作为只读结果页的 MVP 简化：跳回结果页）
                onHistoryClick = { navController.popBackStack() },
            )
        }
        composable(Routes.SETTINGS) { SettingsScreen(onDeleteAll = { navController.navigate(Routes.DELETE_CONFIRM) }) }
        composable(Routes.DELETE_CONFIRM) {
            DeleteConfirmScreen(
                // M9.3/ACC-15：删除全部数据成功 → 清空返回栈回 Splash → 重新 Onboarding（首次启动状态）
                onResetCompleted = {
                    navController.navigate(Routes.SPLASH) {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                },
                onCancel = { navController.popBackStack() },
            )
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
