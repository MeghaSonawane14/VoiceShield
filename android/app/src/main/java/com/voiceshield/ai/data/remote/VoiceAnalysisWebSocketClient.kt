package com.voiceshield.ai.data.remote

import com.google.gson.Gson
import com.voiceshield.ai.data.remote.dto.AnalysisWebSocketResultDto
import com.voiceshield.ai.domain.model.AnalysisResult
import com.voiceshield.ai.domain.model.ConnectionStatus
import com.voiceshield.ai.domain.model.RiskLevel
import com.voiceshield.ai.domain.model.VoiceStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.*
import okio.ByteString

class VoiceAnalysisWebSocketClient(
    private val okHttpClient: OkHttpClient = ApiClient.okHttpClient,
    private val gson: Gson = Gson()
) {
    private val scope = CoroutineScope(Dispatchers.IO)
    private var webSocket: WebSocket? = null
    private var currentSessionId: String? = null

    private val _connectionStatus = MutableStateFlow(ConnectionStatus.DISCONNECTED)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus

    private val _latestResult = MutableSharedFlow<AnalysisResult>(replay = 1)
    val latestResult: SharedFlow<AnalysisResult> = _latestResult

    fun connect(sessionId: String) {
        currentSessionId = sessionId
        _connectionStatus.value = ConnectionStatus.CONNECTING

        val wsUrl = ApiClient.getWebSocketUrl(sessionId)
        val request = Request.Builder().url(wsUrl).build()

        webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                _connectionStatus.value = ConnectionStatus.CONNECTED
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val dto = gson.fromJson(text, AnalysisWebSocketResultDto::class.java)
                    if (dto.sessionId != null) {
                        val domainModel = AnalysisResult(
                            sessionId = dto.sessionId,
                            timestamp = dto.timestamp ?: "",
                            timestampSec = dto.timestampSec,
                            cloneProbability = dto.cloneProbability,
                            speakerSimilarity = dto.speakerSimilarity,
                            riskScore = dto.riskScore,
                            riskScorePct = dto.riskScorePct,
                            riskLevel = RiskLevel.fromString(dto.riskLevel),
                            voiceStatus = VoiceStatus.fromString(dto.voiceStatus),
                            confidence = dto.confidence,
                            likelySpeaker = dto.likelySpeaker ?: "Unknown",
                            isSpeakerMatched = dto.isSpeakerMatched,
                            detectionStatus = dto.detectionStatus ?: "ACTIVE",
                            riskReasons = dto.riskReasons ?: emptyList(),
                            modelName = dto.modelName ?: "AcousticArtifactDetector",
                            isDemoModel = dto.isDemoModel ?: false
                        )
                        scope.launch {
                            _latestResult.emit(domainModel)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                _connectionStatus.value = ConnectionStatus.DISCONNECTED
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                _connectionStatus.value = ConnectionStatus.DISCONNECTED
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                _connectionStatus.value = ConnectionStatus.DISCONNECTED
            }
        })
    }

    fun sendAudioChunk(pcmBytes: ByteArray) {
        webSocket?.send(ByteString.of(*pcmBytes))
    }

    fun disconnect() {
        try {
            webSocket?.close(1000, "Session ended by user")
        } catch (e: Exception) {
            // Ignored
        } finally {
            webSocket = null
            _connectionStatus.value = ConnectionStatus.DISCONNECTED
        }
    }
}
