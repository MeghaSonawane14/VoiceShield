package com.voiceshield.ai.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.voiceshield.ai.data.remote.VoiceAnalysisWebSocketClient
import com.voiceshield.ai.domain.repository.AnalysisRepository
import com.voiceshield.ai.domain.repository.AuthRepository
import com.voiceshield.ai.domain.repository.VoiceProfileRepository
import com.voiceshield.ai.services.AudioRecordManager
import com.voiceshield.ai.services.DemoScenarioProvider
import com.voiceshield.ai.ui.analysis.LiveAnalysisScreen
import com.voiceshield.ai.ui.analysis.LiveAnalysisViewModel
import com.voiceshield.ai.ui.auth.AuthViewModel
import com.voiceshield.ai.ui.auth.ForgotPasswordScreen
import com.voiceshield.ai.ui.auth.LoginScreen
import com.voiceshield.ai.ui.auth.RegisterScreen
import com.voiceshield.ai.ui.demo.QuickDemoScreen
import com.voiceshield.ai.ui.history.HistoryScreen
import com.voiceshield.ai.ui.history.HistoryViewModel
import com.voiceshield.ai.ui.history.SessionDetailScreen
import com.voiceshield.ai.ui.home.HomeScreen
import com.voiceshield.ai.ui.home.HomeViewModel
import com.voiceshield.ai.ui.profile.ProfileScreen
import com.voiceshield.ai.ui.profile.ProfileViewModel
import com.voiceshield.ai.ui.protectedcall.ProtectedCallScreen
import com.voiceshield.ai.ui.report.SecurityReportScreen
import com.voiceshield.ai.ui.splash.SplashScreen

@Composable
fun NavGraph(
    navController: NavHostController,
    authRepository: AuthRepository,
    voiceProfileRepository: VoiceProfileRepository,
    analysisRepository: AnalysisRepository,
    audioRecordManager: AudioRecordManager,
    webSocketClient: VoiceAnalysisWebSocketClient,
    demoScenarioProvider: DemoScenarioProvider,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route,
        modifier = modifier
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(
                authRepository = authRepository,
                onNavigateNext = { isLoggedIn ->
                    val destination = if (isLoggedIn) Screen.Home.route else Screen.Login.route
                    navController.navigate(destination) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Login.route) {
            val authViewModel = AuthViewModel(authRepository)
            LoginScreen(
                viewModel = authViewModel,
                onNavigateHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateRegister = {
                    navController.navigate(Screen.Register.route)
                },
                onNavigateForgotPassword = {
                    navController.navigate(Screen.ForgotPassword.route)
                }
            )
        }

        composable(Screen.Register.route) {
            val authViewModel = AuthViewModel(authRepository)
            RegisterScreen(
                viewModel = authViewModel,
                onNavigateHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Register.route) { inclusive = true }
                    }
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.ForgotPassword.route) {
            val authViewModel = AuthViewModel(authRepository)
            ForgotPasswordScreen(
                viewModel = authViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.Home.route) {
            val homeViewModel = HomeViewModel(authRepository, voiceProfileRepository, analysisRepository)
            HomeScreen(
                viewModel = homeViewModel,
                onNavigateLiveAnalysis = { mode ->
                    navController.navigate(Screen.LiveAnalysis.createRoute(inputMode = mode))
                },
                onNavigateProtectedCall = {
                    navController.navigate(Screen.ProtectedCall.route)
                },
                onNavigateDemo = {
                    navController.navigate(Screen.QuickDemo.route)
                },
                onNavigateProfile = {
                    navController.navigate(Screen.Profile.route)
                },
                onNavigateHistory = {
                    navController.navigate(Screen.History.route)
                },
                onNavigateSessionDetail = { sessionId ->
                    navController.navigate(Screen.SessionDetail.createRoute(sessionId))
                }
            )
        }

        composable(Screen.ProtectedCall.route) {
            ProtectedCallScreen(
                onNavigateBack = { navController.popBackStack() },
                onLaunchDeepAnalysis = {
                    navController.navigate(Screen.LiveAnalysis.createRoute(inputMode = "CALL"))
                }
            )
        }

        composable(
            route = Screen.LiveAnalysis.route,
            arguments = listOf(
                navArgument("inputMode") { type = NavType.StringType; defaultValue = "MIC" },
                navArgument("scenarioId") { type = NavType.StringType; nullable = true; defaultValue = null }
            )
        ) { backStackEntry ->
            val inputMode = backStackEntry.arguments?.getString("inputMode") ?: "MIC"
            val scenarioId = backStackEntry.arguments?.getString("scenarioId")

            val liveViewModel = LiveAnalysisViewModel(
                authRepository = authRepository,
                voiceProfileRepository = voiceProfileRepository,
                analysisRepository = analysisRepository,
                audioRecordManager = audioRecordManager,
                webSocketClient = webSocketClient,
                demoScenarioProvider = demoScenarioProvider
            )

            LiveAnalysisScreen(
                viewModel = liveViewModel,
                inputMode = inputMode,
                scenarioId = scenarioId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateReport = { reportId ->
                    navController.navigate(Screen.SecurityReport.createRoute(reportId)) {
                        popUpTo(Screen.LiveAnalysis.route) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Screen.SecurityReport.route,
            arguments = listOf(navArgument("reportId") { type = NavType.StringType })
        ) { backStackEntry ->
            val reportId = backStackEntry.arguments?.getString("reportId") ?: ""
            SecurityReportScreen(
                reportId = reportId,
                analysisRepository = analysisRepository,
                authRepository = authRepository,
                onNavigateHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                },
                onReanalyze = {
                    navController.navigate(Screen.LiveAnalysis.createRoute(inputMode = "MIC"))
                }
            )
        }

        composable(Screen.History.route) {
            val historyViewModel = HistoryViewModel(authRepository, analysisRepository)
            HistoryScreen(
                viewModel = historyViewModel,
                onNavigateSessionDetail = { sessionId ->
                    navController.navigate(Screen.SessionDetail.createRoute(sessionId))
                }
            )
        }

        composable(
            route = Screen.SessionDetail.route,
            arguments = listOf(navArgument("sessionId") { type = NavType.StringType })
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId") ?: ""
            val historyViewModel = HistoryViewModel(authRepository, analysisRepository)
            SessionDetailScreen(
                sessionId = sessionId,
                viewModel = historyViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.QuickDemo.route) {
            QuickDemoScreen(
                demoScenarioProvider = demoScenarioProvider,
                analysisRepository = analysisRepository,
                onLaunchScenario = { scenarioId ->
                    navController.navigate(Screen.LiveAnalysis.createRoute(inputMode = "DEMO", scenarioId = scenarioId))
                }
            )
        }

        composable(Screen.Profile.route) {
            val profileViewModel = ProfileViewModel(authRepository, voiceProfileRepository, audioRecordManager)
            ProfileScreen(
                viewModel = profileViewModel,
                onNavigateLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }
            )
        }
    }
}
