package matchsong.app.navigation

import android.content.Context
import android.content.ContextWrapper
import androidx.activity.ComponentActivity
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
import androidx.compose.ui.platform.LocalContext
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
import matchsong.app.feature.recommendation.RecommendationListViewModel
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

private tailrec fun Context.findActivity(): ComponentActivity? =
    when (this) {
        is ComponentActivity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }

@Suppress("LongMethod") // 路由注册为声明式清单，逐路由拆分会破坏可读性（M2.1-1 决策）
@Composable
fun AppNavHost(
    navController: NavHostController,
    startDestination: String,
) {
    // BUG-019 核心修复：FlowSessionViewModel 必须 Activity 作用域（单一实例跨页共享）——
    // 此前每路由各自 hiltViewModel()（作用域=各自 back stack entry），RECORDING 写入的
    // wavFile 对 QUALITY_RESULT 的新实例不可见 → 永远"录音文件不可用"。
    // 注意：NavHost 内的 LocalContext 是 destination 的 ContextWrapper，需解包取 Activity。
    val activity = LocalContext.current.findActivity()
    val flowSession: matchsong.app.feature.flow.FlowSessionViewModel =
        if (activity != null) {
            hiltViewModel(activity)
        } else {
            hiltViewModel()
        }
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
            val qualityVm: QualityResultViewModel = hiltViewModel()
            val qualityState by qualityVm.state.collectAsState()
            val wavFile by flowSession.wavFile.collectAsState()
            // BUG-014 防御层：WAV 缺失（finalize 失败/竞态兜底）→ 显式错误而非白屏
            if (wavFile == null) {
                matchsong.app.design.components.state.ErrorState(
                    message = "录音文件不可用，请重新录制",
                    onRetry = {
                        flowSession.reset()
                        navController.navigate(Routes.PREPARE) { popUpTo(Routes.PREPARE) { inclusive = true } }
                    },
                )
                return@composable
            }
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
                            navController.navigate(Routes.PREPARE) { popUpTo(Routes.PREPARE) { inclusive = true } }
                        },
                    )
                }
                is QualityResultViewModel.UiState.Error ->
                    matchsong.app.design.components.state.ErrorState(
                        message = (qualityState as QualityResultViewModel.UiState.Error).message,
                        onRetry = {
                            matchsong.core.audio.android.RecordingSessionRunner.instance?.cleanupSessionFiles()
                            flowSession.reset()
                            navController.navigate(Routes.PREPARE) { popUpTo(Routes.PREPARE) { inclusive = true } }
                        },
                    )
                QualityResultViewModel.UiState.Idle -> Unit
            }
        }
        composable(Routes.ANALYZING) {
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
            val analysisResult by flowSession.analysisResult.collectAsState()
            VoiceResultScreen(
                result = analysisResult,
                onSeeRecommendations = { navController.navigate(Routes.RECOMMENDATION_LIST) },
                onRetry = {
                    matchsong.core.audio.android.RecordingSessionRunner.instance?.cleanupSessionFiles()
                    flowSession.reset()
                    navController.navigate(Routes.PREPARE) { popUpTo(Routes.PREPARE) { inclusive = true } }
                },
            )
        }
        composable(Routes.RECOMMENDATION_LIST) {
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
                onSongClick = { songId ->
                    // M10.6（BUG-004）：详情页切真实推荐项数据（含反馈关联 resultId）
                    val success = recState as? RecommendationListViewModel.UiState.Success
                    val item = success?.result?.recommendations?.firstOrNull { it.song.songId == songId }
                    if (item != null) {
                        navController.navigate(
                            Routes.recommendationDetail(
                                songId = item.song.songId,
                                title = item.song.title,
                                artist = item.song.artist,
                                score = item.score.toInt(),
                                keyShift = item.keyShiftSemitones,
                                explanation = item.explanation.firstOrNull(),
                                // 已知差距（P3）：RecommendationResult 未携带 resultId（data-model §2.12），
                                // 反馈暂以 null 关联；M11 前补字段后接通
                                resultId = null,
                            ),
                        )
                    }
                },
                onRetry = {
                    matchsong.core.audio.android.RecordingSessionRunner.instance?.cleanupSessionFiles()
                    flowSession.reset()
                    navController.navigate(Routes.PREPARE) { popUpTo(Routes.PREPARE) { inclusive = true } }
                },
            )
        }
        composable(
            route = Routes.RECOMMENDATION_DETAIL,
            arguments =
                listOf(
                    androidx.navigation.navArgument(NavArgs.SONG_ID) { type = androidx.navigation.NavType.StringType },
                    androidx.navigation.navArgument(NavArgs.SONG_TITLE) {
                        type = androidx.navigation.NavType.StringType
                        defaultValue = ""
                    },
                    androidx.navigation.navArgument(NavArgs.SONG_ARTIST) {
                        type = androidx.navigation.NavType.StringType
                        defaultValue = ""
                    },
                    androidx.navigation.navArgument(NavArgs.SCORE) {
                        // 导航参数仅 StringType 支持 nullable（IntType 会抛 IllegalArgumentException）
                        type = androidx.navigation.NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                    androidx.navigation.navArgument(NavArgs.KEY_SHIFT) {
                        type = androidx.navigation.NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                    androidx.navigation.navArgument(NavArgs.EXPLANATION) {
                        type = androidx.navigation.NavType.StringType
                        defaultValue = ""
                    },
                    androidx.navigation.navArgument(NavArgs.RESULT_ID) {
                        type = androidx.navigation.NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                ),
        ) { entry ->
            val args = entry.arguments
            RecommendationDetailScreen(
                songId = args?.getString(NavArgs.SONG_ID).orEmpty(),
                title = args?.getString(NavArgs.SONG_TITLE).orEmpty(),
                artist = args?.getString(NavArgs.SONG_ARTIST).orEmpty(),
                score = args?.getString(NavArgs.SCORE)?.toIntOrNull()?.toDouble(),
                keyShiftSemitones = args?.getString(NavArgs.KEY_SHIFT)?.toIntOrNull(),
                explanation = args?.getString(NavArgs.EXPLANATION),
                resultId = args?.getString(NavArgs.RESULT_ID),
                onBack = { navController.popBackStack() },
            )
        }
        composable(Routes.FAVORITES) {
            val favoritesVm: matchsong.app.feature.favorites.FavoritesViewModel = hiltViewModel()
            val favoritesVmState by favoritesVm.uiState.collectAsState()
            FavoritesScreen(
                onBack = { navController.popBackStack() },
                onSongClick = { songId ->
                    // 收藏入口无推荐上下文：仅传歌曲名/歌手（score/shift/resultId 为 null）
                    val success =
                        favoritesVmState as? matchsong.app.feature.favorites.FavoritesViewModel.UiState.Content
                    val song = success?.songs?.firstOrNull { it.songId == songId }
                    navController.navigate(
                        Routes.recommendationDetail(
                            songId = songId,
                            title = song?.title.orEmpty(),
                            artist = song?.artist.orEmpty(),
                            score = null,
                            keyShift = null,
                            explanation = null,
                            resultId = null,
                        ),
                    )
                },
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
