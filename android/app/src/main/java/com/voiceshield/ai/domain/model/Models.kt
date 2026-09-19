package com.voiceshield.ai.domain.model

data class User(
    val uid: String,
    val email: String,
    val name: String,
    val hasVoiceProfile: Boolean = false,
    val voiceProfileStatus: String = "NOT_REGISTERED"
)

data class VoiceProfile(
    val id: String,
    val userId: String,
    val name: String,
    val status: String,
    val registrationDate: String,
    val embeddingDim: Int = 128,
    val notes: String = ""
)

data class AnalysisResult(
    val sessionId: String,
    val timestamp: String,
    val timestampSec: Float = 0.0f,
    val cloneProbability: Float = 0.0f,
    val speakerSimilarity: Float = 0.0f,
    val riskScore: Float = 0.0f,
    val riskScorePct: Int = 0,
    val riskLevel: RiskLevel = RiskLevel.LOW,
    val voiceStatus: VoiceStatus = VoiceStatus.LIKELY_AUTHENTIC,
    val confidence: Float = 0.85f,
    val likelySpeaker: String = "Unknown",
    val isSpeakerMatched: Boolean = false,
    val detectionStatus: String = "ACTIVE",
    val riskReasons: List<String> = emptyList(),
    val modelName: String = "AcousticArtifactDetector",
    val isDemoModel: Boolean = false
)

data class RiskTimelinePoint(
    val timestampSec: Float,
    val timeDisplay: String,
    val cloneProb: Float,
    val speakerSim: Float,
    val riskScore: Float,
    val riskLevel: RiskLevel
)

data class RiskEvent(
    val timestampSec: Float,
    val type: String,
    val description: String,
    val severity: String
)

data class SecurityReport(
    val reportId: String,
    val sessionId: String,
    val timestamp: String,
    val durationSec: Float,
    val overallRiskScore: Int,
    val riskLevel: RiskLevel,
    val cloneProbability: Float,
    val speakerSimilarity: Float,
    val likelySpeaker: String,
    val verificationResult: String,
    val recommendedAction: String,
    val events: List<RiskEvent> = emptyList(),
    val timeline: List<RiskTimelinePoint> = emptyList(),
    val isDemo: Boolean = false
)

data class DemoScenario(
    val id: String,
    val title: String,
    val category: String,
    val description: String,
    val baseRiskLevel: RiskLevel,
    val expectedSpeaker: String,
    val transcript: String,
    val steps: List<AnalysisResult>
)
