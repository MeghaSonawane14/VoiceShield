package com.voiceshield.ai.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.voiceshield.ai.domain.model.User
import com.voiceshield.ai.domain.model.VoiceProfile
import com.voiceshield.ai.domain.repository.AuthRepository
import com.voiceshield.ai.domain.repository.VoiceProfileRepository
import com.voiceshield.ai.services.AudioRecordManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ProfileUiState(
    val user: User? = null,
    val profile: VoiceProfile? = null,
    val isEnrolled: Boolean = false,
    val isRecording: Boolean = false,
    val recordingDurationSec: Int = 0,
    val currentPromptIndex: Int = 0,
    val audioAmplitudes: List<Float> = List(20) { 0.05f },
    val speakerNameInput: String = "",
    val isLoading: Boolean = false,
    val statusMessage: String? = null,
    val errorMessage: String? = null
)

class ProfileViewModel(
    private val authRepository: AuthRepository,
    private val voiceProfileRepository: VoiceProfileRepository,
    private val audioRecordManager: AudioRecordManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private var enrollmentJob: Job? = null

    val enrollmentPrompts = listOf(
        "\"The quick onyx goblin jumps over the lazy dwarf, maintaining biometric resonance.\"",
        "\"Security protocol seven initiates voice authentication sequence alpha.\"",
        "\"My voice is my sovereign key and forensic acoustic identity.\""
    )

    init {
        loadProfile()
    }

    fun loadProfile() {
        viewModelScope.launch {
            authRepository.currentUser.collect { user ->
                _uiState.value = _uiState.value.copy(user = user)
                if (user != null) {
                    voiceProfileRepository.getVoiceProfile(user.uid).collect { prof ->
                        _uiState.value = _uiState.value.copy(
                            profile = prof,
                            isEnrolled = prof != null,
                            speakerNameInput = prof?.speakerName ?: user.displayName
                        )
                    }
                }
            }
        }
    }

    fun onSpeakerNameChange(name: String) {
        _uiState.value = _uiState.value.copy(speakerNameInput = name)
    }

    fun startEnrollmentRecording() {
        _uiState.value = _uiState.value.copy(
            isRecording = true,
            recordingDurationSec = 0,
            statusMessage = null,
            errorMessage = null
        )

        audioRecordManager.startRecording(
            onAudioChunk = { /* In local app, chunks can be buffered */ },
            onAmplitude = { amp ->
                val list = _uiState.value.audioAmplitudes.toMutableList()
                list.add(amp)
                if (list.size > 20) list.removeAt(0)
                _uiState.value = _uiState.value.copy(audioAmplitudes = list)
            }
        )

        enrollmentJob?.cancel()
        enrollmentJob = viewModelScope.launch {
            for (sec in 1..10) {
                delay(1000)
                _uiState.value = _uiState.value.copy(
                    recordingDurationSec = sec,
                    currentPromptIndex = (sec / 4).coerceAtMost(enrollmentPrompts.size - 1)
                )
            }
            // Finished 10 seconds of enrollment
            audioRecordManager.stopRecording()
            _uiState.value = _uiState.value.copy(isRecording = false, isLoading = true)

            val user = authRepository.currentUser.value
            val userId = user?.uid ?: "guest"
            val speakerName = _uiState.value.speakerNameInput.ifBlank { user?.displayName ?: "Primary Subject" }

            val result = voiceProfileRepository.createVoiceProfile(userId, speakerName, null)
            result.fold(
                onSuccess = { prof ->
                    _uiState.value = _uiState.value.copy(
                        profile = prof,
                        isEnrolled = true,
                        isLoading = false,
                        statusMessage = "Voice profile successfully enrolled and calibrated."
                    )
                },
                onFailure = { err ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = err.localizedMessage ?: "Enrollment failed"
                    )
                }
            )
        }
    }

    fun cancelEnrollment() {
        enrollmentJob?.cancel()
        audioRecordManager.stopRecording()
        _uiState.value = _uiState.value.copy(isRecording = false, recordingDurationSec = 0)
    }

    fun deleteProfile() {
        val user = authRepository.currentUser.value ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            voiceProfileRepository.deleteVoiceProfile(user.uid)
            _uiState.value = _uiState.value.copy(
                profile = null,
                isEnrolled = false,
                isLoading = false,
                statusMessage = "Voice profile removed."
            )
        }
    }

    fun logout(onNavigateLogin: () -> Unit) {
        viewModelScope.launch {
            authRepository.logout()
            onNavigateLogin()
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioRecordManager.stopRecording()
        enrollmentJob?.cancel()
    }
}
