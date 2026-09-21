package com.voiceshield.ai.domain.model

data class User(
    val uid: String,
    val email: String,
    val name: String,
    val hasVoiceProfile: Boolean = false,
    val voiceProfileStatus: String = "NOT_REGISTERED"
) {
    val displayName: String
        get() = name.ifBlank { "Cyber Analyst" }
}

data class VoiceProfile(
    val id: String,
    val userId: String,
    val name: String,
    val status: String,
    val registrationDate: String,
    val embeddingDim: Int = 128,
    val notes: String = "",
    val durationSec: Float = 10.0f
) {
    val speakerName: String
        get() = name

    val createdAt: String
        get() = registrationDate
}

data class SessionEntityDomain(
    val sessionId: String,
    val userId: String,
    val sessionCode: String,
    val startTime: String,
    val durationSec: Float,
    val inputMode: String,
    val overallRisk: Int,
    val riskLevel: String,
    val maxCloneProb: Float,
    val speakerSimilarity: Float,
    val likelySpeaker: String,
    val isDemo: Boolean = false
)

data class AnalysisResult(
    val sessionId: String,
    val timestamp: String,
    val timestampSec: Float = 0.0f,
    val cloneProbability: Float = 0.0f,
    val speakerSimilarity: Float = 0.0f,
    val riskScore: Float = 0.0f,
    val riskScorePct: Int = (riskScore * 100).toInt(),
    val overallRisk: Int = if (riskScore > 1.0f) riskScore.toInt() else (riskScore * 100).toInt(),
    val riskLevel: String = "LOW",
    val voiceStatus: VoiceStatus = VoiceStatus.LIKELY_AUTHENTIC,
    val confidence: Float = 0.85f,
    val likelySpeaker: String = "Unknown",
    val detectedSpeaker: String = likelySpeaker,
    val enrolledSpeaker: String = "Trusted Voice Profile",
    val isSpeakerMatched: Boolean = false,
    val detectionStatus: String = "ACTIVE",
    val riskReasons: List<String> = emptyList(),
    val modelName: String = "AcousticArtifactDetector",
    val modelAttribution: String = "FastAPI Neural Forensic Pipeline",
    val intent: String = "NORMAL",
    val spectralFlatness: Float = 0.02f,
    val isDemoModel: Boolean = false
)

data class RiskTimelinePoint(
    val timestamp: String = "00:00",
    val timestampSec: Float = 0.0f,
    val timeDisplay: String = timestamp,
    val cloneProb: Float = 0.0f,
    val cloneProbability: Float = cloneProb,
    val speakerSim: Float = 0.0f,
    val speakerSimilarity: Float = speakerSim,
    val riskScore: Int = (cloneProb * 100).toInt(),
    val riskLevel: RiskLevel = RiskLevel.fromString(
        if (riskScore >= 70) "HIGH_RISK" else if (riskScore >= 40) "SUSPICIOUS" else "AUTHENTIC"
    )
)

data class RiskEvent(
    val timestamp: Float = 0.0f,
    val timestampSec: Float = timestamp,
    val type: String = "ALERT",
    val description: String = "",
    val severity: String = "MEDIUM"
)

data class SecurityReport(
    val reportId: String,
    val sessionId: String,
    val sessionCode: String = sessionId,
    val timestamp: String,
    val durationSec: Float,
    val overallRiskScore: Int,
    val overallRiskLevel: String = "AUTHENTIC",
    val riskLevel: RiskLevel = RiskLevel.fromString(overallRiskLevel),
    val cloneProbability: Float = 0.0f,
    val maxCloneProbability: Float = cloneProbability,
    val speakerSimilarity: Float = 0.0f,
    val meanSpeakerSimilarity: Float = speakerSimilarity,
    val likelySpeaker: String = "Unknown",
    val detectedSpeaker: String = likelySpeaker,
    val enrolledSpeaker: String = "Trusted Voice Profile",
    val verificationResult: String = "AUTHENTIC",
    val recommendedAction: String = "Verify via independent channel if credentials are requested",
    val intentClassification: String = "NORMAL",
    val modelAttribution: String = "FastAPI Neural Forensic Pipeline",
    val inputMode: String = "MIC",
    val events: List<RiskEvent> = emptyList(),
    val riskEvents: List<RiskEvent> = events,
    val timeline: List<RiskTimelinePoint> = emptyList(),
    val advisoryRecommendations: List<String> = emptyList(),
    val isDemoModel: Boolean = false,
    val isDemo: Boolean = false
)

data class DemoScenario(
    val id: String,
    val title: String,
    val category: String,
    val description: String,
    val baseRiskLevel: RiskLevel,
    val expectedRiskLevel: RiskLevel = baseRiskLevel,
    val expectedSpeaker: String,
    val transcript: String,
    val expectedAction: String = "Verify identity via out-of-band challenge",
    val steps: List<AnalysisResult> = emptyList(),
    val timelinePoints: List<RiskTimelinePoint> = emptyList()
)
