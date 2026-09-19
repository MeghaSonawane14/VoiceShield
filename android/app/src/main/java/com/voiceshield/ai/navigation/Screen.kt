package com.voiceshield.ai.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Login : Screen("login")
    object Register : Screen("register")
    object ForgotPassword : Screen("forgot_password")
    
    // Bottom Nav items
    object Home : Screen("home")
    object History : Screen("history")
    object QuickDemo : Screen("quick_demo")
    object Profile : Screen("profile")
    
    // Detail / Action screens
    object ProtectedCall : Screen("protected_call")
    object LiveAnalysis : Screen("live_analysis/{inputMode}?scenarioId={scenarioId}") {
        fun createRoute(inputMode: String = "MIC", scenarioId: String? = null): String {
            return if (scenarioId != null) {
                "live_analysis/$inputMode?scenarioId=$scenarioId"
            } else {
                "live_analysis/$inputMode"
            }
        }
    }
    object SecurityReport : Screen("security_report/{reportId}") {
        fun createRoute(reportId: String): String = "security_report/$reportId"
    }
    object SessionDetail : Screen("session_detail/{sessionId}") {
        fun createRoute(sessionId: String): String = "session_detail/$sessionId"
    }
}
