package com.voiceshield.ai.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.voiceshield.ai.domain.model.SessionEntityDomain
import com.voiceshield.ai.domain.model.User
import com.voiceshield.ai.domain.model.VoiceProfile
import com.voiceshield.ai.domain.repository.AnalysisRepository
import com.voiceshield.ai.domain.repository.AuthRepository
import com.voiceshield.ai.domain.repository.VoiceProfileRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class HomeUiState(
    val user: User? = null,
    val voiceProfile: VoiceProfile? = null,
    val recentSessions: List<SessionEntityDomain> = emptyList(),
    val averageRiskScore: Int = 0,
    val totalSessionsAnalyzed: Int = 0,
    val highRiskDetectionsCount: Int = 0,
    val isEnrolled: Boolean = false,
    val isLoading: Boolean = false
)

class HomeViewModel(
    private val authRepository: AuthRepository,
    private val voiceProfileRepository: VoiceProfileRepository,
    private val analysisRepository: AnalysisRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            authRepository.currentUser.collect { user ->
                if (user != null) {
                    _uiState.value = _uiState.value.copy(user = user)
                    
                    // Observe voice profile
                    launch {
                        voiceProfileRepository.getVoiceProfile(user.uid).collect { profile ->
                            _uiState.value = _uiState.value.copy(
                                voiceProfile = profile,
                                isEnrolled = profile != null
                            )
                        }
                    }

                    // Observe recent sessions
                    launch {
                        analysisRepository.getSessionHistory(user.uid).collect { sessions ->
                            val userSessions = sessions.filter { !it.isDemo }
                            val total = userSessions.size
                            val avgRisk = if (total > 0) userSessions.map { it.overallRisk }.average().toInt() else 0
                            val highRisk = userSessions.count { it.overallRisk >= 70 }
                            
                            _uiState.value = _uiState.value.copy(
                                recentSessions = userSessions.take(5),
                                totalSessionsAnalyzed = total,
                                averageRiskScore = avgRisk,
                                highRiskDetectionsCount = highRisk
                            )
                        }
                    }
                }
            }
        }
    }

    fun logout(onNavigateLogin: () -> Unit) {
        viewModelScope.launch {
            authRepository.logout()
            onNavigateLogin()
        }
    }
}
