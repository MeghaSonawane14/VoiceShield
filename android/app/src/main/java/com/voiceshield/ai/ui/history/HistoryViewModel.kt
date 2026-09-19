package com.voiceshield.ai.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.voiceshield.ai.domain.model.SessionEntityDomain
import com.voiceshield.ai.domain.repository.AnalysisRepository
import com.voiceshield.ai.domain.repository.AuthRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class HistoryUiState(
    val sessions: List<SessionEntityDomain> = emptyList(),
    val filteredSessions: List<SessionEntityDomain> = emptyList(),
    val selectedFilter: String = "ALL", // ALL, HIGH_RISK, SUSPICIOUS, AUTHENTIC
    val isLoading: Boolean = true,
    val selectedSessionDetail: SessionEntityDomain? = null
)

class HistoryViewModel(
    private val authRepository: AuthRepository,
    private val analysisRepository: AnalysisRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        loadHistory()
    }

    fun loadHistory() {
        viewModelScope.launch {
            val user = authRepository.currentUser.value
            val userId = user?.uid ?: "guest"
            analysisRepository.getSessionHistory(userId).collect { allSessions ->
                val realSessions = allSessions.filter { !it.isDemo }
                _uiState.value = _uiState.value.copy(
                    sessions = realSessions,
                    filteredSessions = applyFilter(realSessions, _uiState.value.selectedFilter),
                    isLoading = false
                )
            }
        }
    }

    fun setFilter(filter: String) {
        _uiState.value = _uiState.value.copy(
            selectedFilter = filter,
            filteredSessions = applyFilter(_uiState.value.sessions, filter)
        )
    }

    private fun applyFilter(list: List<SessionEntityDomain>, filter: String): List<SessionEntityDomain> {
        return when (filter) {
            "HIGH_RISK" -> list.filter { it.overallRisk >= 70 }
            "SUSPICIOUS" -> list.filter { it.overallRisk in 40..69 }
            "AUTHENTIC" -> list.filter { it.overallRisk < 40 }
            else -> list
        }
    }

    fun loadSessionDetail(sessionId: String) {
        viewModelScope.launch {
            val user = authRepository.currentUser.value
            val userId = user?.uid ?: "guest"
            val detail = analysisRepository.getSessionById(sessionId, userId)
            _uiState.value = _uiState.value.copy(selectedSessionDetail = detail)
        }
    }

    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            val user = authRepository.currentUser.value
            val userId = user?.uid ?: "guest"
            analysisRepository.deleteSession(sessionId, userId)
        }
    }
}
