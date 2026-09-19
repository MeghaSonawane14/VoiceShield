package com.voiceshield.ai.ui.analysis

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.voiceshield.ai.data.remote.VoiceAnalysisWebSocketClient
import com.voiceshield.ai.domain.model.*
import com.voiceshield.ai.domain.repository.AnalysisRepository
import com.voiceshield.ai.domain.repository.AuthRepository
import com.voiceshield.ai.domain.repository.SessionEntityDomain
import com.voiceshield.ai.domain.repository.VoiceProfileRepository
import com.voiceshield.ai.services.AudioRecordManager
import com.voiceshield.ai.services.DemoScenarioProvider
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class LiveAnalysisUiState(
    val sessionId: String = UUID.randomUUID().toString(),
    val sessionCode: String = "VS-${System.currentTimeMillis() % 100000}",
    val connectionStatus: ConnectionStatus = ConnectionStatus.DISCONNECTED,
    val riskScore: Int = 0,
    val riskLevel: RiskLevel = RiskLevel.AUTHENTIC,
    val voiceStatus: VoiceStatus = VoiceStatus.AUTHENTIC,
    val cloneProbability: Float = 0.05f,
    val speakerSimilarity: Float = 0.95f,
    val detectedSpeaker: String = "Analyzing...",
    val enrolledSpeaker: String = "None Enrolled",
    val intent: String = "NORMAL",
    val spectralFlatness: Float = 0.02f,
    val modelAttribution: String = "Genuine human vocal tract dynamics",
    val isDemoModel: Boolean = false,
    val isDemo: Boolean = false,
    val timeline: List<RiskTimelinePoint> = emptyList(),
    val waveformAmplitudes: List<Float> = List(30) { 0.05f },
    val durationSec: Int = 0,
    val isRecording: Boolean = false,
    val isPaused: Boolean = false,
    val showHighRiskDialog: Boolean = false,
    val lastAlertReason: String = "",
    val eventsList: List<RiskEvent> = emptyList(),
    val generatedReportId: String? = null
)

class LiveAnalysisViewModel(
    private val authRepository: AuthRepository,
    private val voiceProfileRepository: VoiceProfileRepository,
    private val analysisRepository: AnalysisRepository,
    private val audioRecordManager: AudioRecordManager,
    private val webSocketClient: VoiceAnalysisWebSocketClient,
    private val demoScenarioProvider: DemoScenarioProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(LiveAnalysisUiState())
    val uiState: StateFlow<LiveAnalysisUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null
    private var demoJob: Job? = null
    private var isHighRiskTriggered = false

    fun startSession(inputMode: String = "MIC", scenarioId: String? = null) {
        val newSessionId = UUID.randomUUID().toString()
        val newCode = "VS-${(10000..99999).random()}"
        isHighRiskTriggered = false

        val currentUser = authRepository.currentUser.value
        val userId = currentUser?.uid ?: "guest"

        viewModelScope.launch {
            // Check enrolled profile
            voiceProfileRepository.getVoiceProfile(userId).collect { profile ->
                if (profile != null) {
                    _uiState.value = _uiState.value.copy(enrolledSpeaker = profile.speakerName)
                }
            }
        }

        _uiState.value = _uiState.value.copy(
            sessionId = newSessionId,
            sessionCode = newCode,
            durationSec = 0,
            isRecording = true,
            isPaused = false,
            isDemo = scenarioId != null || inputMode == "DEMO",
            timeline = emptyList(),
            eventsList = emptyList(),
            connectionStatus = ConnectionStatus.CONNECTING
        )

        startDurationTimer()

        if (scenarioId != null || inputMode == "DEMO") {
            startDemoSimulation(scenarioId ?: "clone_impersonation")
        } else {
            startLiveMicCapture(newSessionId)
        }
    }

    private fun startLiveMicCapture(sessionId: String) {
        // Connect WebSocket
        webSocketClient.connect(
            sessionId = sessionId,
            onOpen = {
                _uiState.value = _uiState.value.copy(connectionStatus = ConnectionStatus.CONNECTED)
            },
            onResult = { result ->
                handleIncomingAnalysisResult(result)
            },
            onFailure = { _, _ ->
                _uiState.value = _uiState.value.copy(connectionStatus = ConnectionStatus.DISCONNECTED)
            },
            onClosing = {
                _uiState.value = _uiState.value.copy(connectionStatus = ConnectionStatus.DISCONNECTED)
            }
        )

        // Stream microphone audio
        audioRecordManager.startRecording(
            onAudioChunk = { pcmChunk ->
                if (!_uiState.value.isPaused) {
                    webSocketClient.sendAudioChunk(pcmChunk)
                }
            },
            onAmplitude = { amp ->
                if (!_uiState.value.isPaused) {
                    val currentList = _uiState.value.waveformAmplitudes.toMutableList()
                    currentList.add(amp)
                    if (currentList.size > 30) currentList.removeAt(0)
                    _uiState.value = _uiState.value.copy(waveformAmplitudes = currentList)
                }
            }
        )
    }

    private fun startDemoSimulation(scenarioId: String) {
        _uiState.value = _uiState.value.copy(connectionStatus = ConnectionStatus.CONNECTED)
        val scenario = demoScenarioProvider.getScenario(scenarioId)

        demoJob?.cancel()
        demoJob = viewModelScope.launch {
            scenario.timelinePoints.forEach { point ->
                if (!_uiState.value.isPaused) {
                    val riskLevel = when {
                        point.riskScore >= 70 -> RiskLevel.HIGH_RISK
                        point.riskScore >= 40 -> RiskLevel.SUSPICIOUS
                        else -> RiskLevel.AUTHENTIC
                    }
                    val currentTimeline = _uiState.value.timeline.toMutableList().apply { add(point) }
                    val currentAmps = _uiState.value.waveformAmplitudes.toMutableList()
                    val fakeAmp = (0.2f + (Math.random() * 0.7f).toFloat())
                    currentAmps.add(fakeAmp)
                    if (currentAmps.size > 30) currentAmps.removeAt(0)

                    val cloneProb = point.riskScore / 100f
                    val sim = if (scenarioId.contains("mismatch")) 0.28f else 0.88f

                    _uiState.value = _uiState.value.copy(
                        riskScore = point.riskScore,
                        riskLevel = riskLevel,
                        cloneProbability = cloneProb,
                        speakerSimilarity = sim,
                        detectedSpeaker = if (sim < 0.5f) "Unrecognized Speaker" else (_uiState.value.enrolledSpeaker.ifEmpty { "Enrolled Subject" }),
                        timeline = currentTimeline,
                        waveformAmplitudes = currentAmps,
                        modelAttribution = if (cloneProb > 0.6f) "Synthesized via neural TTS / vocoder artifacts" else "Genuine human vocal dynamics",
                        isDemoModel = true
                    )

                    if (point.riskScore >= 70 && !isHighRiskTriggered) {
                        isHighRiskTriggered = true
                        _uiState.value = _uiState.value.copy(
                            showHighRiskDialog = true,
                            lastAlertReason = "Synthetic acoustic cues detected at timestamp ${point.timestamp}s"
                        )
                    }
                }
                delay(1200)
            }
        }
    }

    private fun handleIncomingAnalysisResult(result: AnalysisResult) {
        val riskLvl = when (result.riskLevel) {
            "HIGH_RISK" -> RiskLevel.HIGH_RISK
            "SUSPICIOUS" -> RiskLevel.SUSPICIOUS
            else -> RiskLevel.AUTHENTIC
        }

        val point = RiskTimelinePoint(
            timestamp = result.timestamp,
            riskScore = result.overallRisk,
            cloneProbability = result.cloneProbability,
            speakerSimilarity = result.speakerSimilarity
        )

        val updatedTimeline = _uiState.value.timeline.toMutableList().apply { add(point) }

        _uiState.value = _uiState.value.copy(
            riskScore = result.overallRisk,
            riskLevel = riskLvl,
            cloneProbability = result.cloneProbability,
            speakerSimilarity = result.speakerSimilarity,
            detectedSpeaker = result.detectedSpeaker,
            enrolledSpeaker = result.enrolledSpeaker,
            intent = result.intent,
            spectralFlatness = result.spectralFlatness,
            modelAttribution = result.modelAttribution,
            isDemoModel = result.isDemoModel,
            timeline = updatedTimeline
        )

        if (result.overallRisk >= 70 && !isHighRiskTriggered) {
            isHighRiskTriggered = true
            _uiState.value = _uiState.value.copy(
                showHighRiskDialog = true,
                lastAlertReason = "Real-time acoustic analysis detected synthetic speech and/or speaker mismatch."
            )
        }
    }

    private fun startDurationTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_uiState.value.isRecording) {
                delay(1000)
                if (!_uiState.value.isPaused) {
                    _uiState.value = _uiState.value.copy(durationSec = _uiState.value.durationSec + 1)
                }
            }
        }
    }

    fun pauseResume() {
        _uiState.value = _uiState.value.copy(isPaused = !_uiState.value.isPaused)
    }

    fun dismissHighRiskDialog() {
        _uiState.value = _uiState.value.copy(showHighRiskDialog = false)
    }

    fun stopSession(onReportCreated: (String) -> Unit) {
        timerJob?.cancel()
        demoJob?.cancel()
        audioRecordManager.stopRecording()
        webSocketClient.disconnect()

        val state = _uiState.value
        val currentUser = authRepository.currentUser.value
        val userId = currentUser?.uid ?: "guest"
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

        viewModelScope.launch {
            val sessionDomain = SessionEntityDomain(
                sessionId = state.sessionId,
                userId = userId,
                sessionCode = state.sessionCode,
                startTime = timestamp,
                durationSec = state.durationSec.toFloat(),
                inputMode = if (state.isDemo) "DEMO" else "MIC",
                overallRisk = state.riskScore,
                riskLevel = state.riskLevel.name,
                maxCloneProb = state.cloneProbability,
                speakerSimilarity = state.speakerSimilarity,
                likelySpeaker = state.detectedSpeaker,
                isDemo = state.isDemo
            )

            // Save to Room DB
            analysisRepository.saveSession(sessionDomain)

            // Generate report
            val reportId = UUID.randomUUID().toString()
            val report = SecurityReport(
                reportId = reportId,
                sessionId = state.sessionId,
                sessionCode = state.sessionCode,
                timestamp = timestamp,
                durationSec = state.durationSec.toFloat(),
                inputMode = if (state.isDemo) "DEMO" else "MIC",
                overallRiskScore = state.riskScore,
                overallRiskLevel = state.riskLevel.name,
                maxCloneProbability = state.cloneProbability,
                meanSpeakerSimilarity = state.speakerSimilarity,
                detectedSpeaker = state.detectedSpeaker,
                enrolledSpeaker = state.enrolledSpeaker,
                intentClassification = state.intent,
                modelAttribution = state.modelAttribution,
                isDemoModel = state.isDemoModel,
                isDemo = state.isDemo,
                riskEvents = listOf(
                    RiskEvent(
                        timestamp = state.durationSec.toFloat() / 2,
                        description = "Analysis peak observation: synthetic confidence ${(state.cloneProbability * 100).toInt()}%",
                        severity = state.riskLevel.name
                    )
                ),
                advisoryRecommendations = listOf(
                    "Probabilistic AI detection indicates: ${state.riskLevel.name}.",
                    "Advisory note: Never treat automated AI predictions as 100% infallible legal proof.",
                    "Verify speaker identity via secondary channel if sensitive credentials are requested."
                )
            )

            analysisRepository.saveReport(report, userId)

            _uiState.value = _uiState.value.copy(
                isRecording = false,
                generatedReportId = reportId,
                connectionStatus = ConnectionStatus.DISCONNECTED
            )

            onReportCreated(reportId)
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioRecordManager.stopRecording()
        webSocketClient.disconnect()
        timerJob?.cancel()
        demoJob?.cancel()
    }
}
