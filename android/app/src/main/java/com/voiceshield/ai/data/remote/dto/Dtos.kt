package com.voiceshield.ai.data.remote.dto

import com.google.gson.annotations.SerializedName

data class VerifyAuthResponse(
    val status: String,
    val user: UserDto,
    val message: String
)

data class UserDto(
    @SerializedName("user_id") val userId: String,
    val email: String,
    val name: String,
    @SerializedName("has_voice_profile") val hasVoiceProfile: Boolean,
    @SerializedName("voice_profile_status") val voiceProfileStatus: String
)

data class VoiceProfileDto(
    val id: String?,
    @SerializedName("user_id") val userId: String?,
    val name: String?,
    val status: String?,
    @SerializedName("registration_date") val registrationDate: String?,
    @SerializedName("embedding_dim") val embeddingDim: Int?,
    val message: String?
)

data class StartAnalysisResponse(
    @SerializedName("session_id") val sessionId: String,
    val status: String,
    @SerializedName("websocket_url") val websocketUrl: String
)

data class AnalysisWebSocketResultDto(
    @SerializedName("session_id") val sessionId: String,
    val timestamp: String,
    @SerializedName("timestamp_sec") val timestampSec: Float,
    @SerializedName("clone_probability") val cloneProbability: Float,
    @SerializedName("speaker_similarity") val speakerSimilarity: Float,
    @SerializedName("risk_score") val riskScore: Float,
    @SerializedName("risk_score_pct") val riskScorePct: Int,
    @SerializedName("risk_level") val riskLevel: String,
    @SerializedName("voice_status") val voiceStatus: String,
    val confidence: Float,
    @SerializedName("likely_speaker") val likelySpeaker: String,
    @SerializedName("is_speaker_matched") val isSpeakerMatched: Boolean,
    @SerializedName("detection_status") val detectionStatus: String,
    @SerializedName("risk_reasons") val riskReasons: List<String>?,
    @SerializedName("model_name") val modelName: String?,
    @SerializedName("is_demo_model") val isDemoModel: Boolean?
)

data class StopAnalysisResponse(
    val status: String,
    @SerializedName("session_id") val sessionId: String,
    @SerializedName("report_id") val reportId: String,
    val report: SecurityReportDto?
)

data class SecurityReportDto(
    @SerializedName("report_id") val reportId: String,
    @SerializedName("session_id") val sessionId: String,
    @SerializedName("session_code") val sessionCode: String?,
    val timestamp: String?,
    @SerializedName("audio_duration_sec") val audioDurationSec: Float?,
    @SerializedName("executive_summary") val executiveSummary: ExecutiveSummaryDto?,
    @SerializedName("timeline_events") val timelineEvents: List<RiskEventDto>?,
    @SerializedName("timeline_data_points") val timelineDataPoints: List<TimelinePointDto>?
)

data class ExecutiveSummaryDto(
    @SerializedName("overall_risk_score") val overallRiskScore: Int,
    @SerializedName("risk_level") val riskLevel: String,
    @SerializedName("clone_probability") val cloneProbability: Float,
    @SerializedName("likely_impersonated_speaker") val likelyImpersonatedSpeaker: String?,
    @SerializedName("speaker_similarity") val speakerSimilarity: Float?,
    @SerializedName("verification_result") val verificationResult: String?,
    @SerializedName("recommended_action") val recommendedAction: String?
)

data class RiskEventDto(
    @SerializedName("timestamp_sec") val timestampSec: Float,
    val type: String?,
    val description: String?,
    val severity: String?
)

data class TimelinePointDto(
    @SerializedName("timestamp_sec") val timestampSec: Float,
    @SerializedName("clone_prob") val cloneProb: Float,
    @SerializedName("speaker_similarity") val speakerSimilarity: Float,
    @SerializedName("is_synthetic") val isSynthetic: Boolean?
)

data class SessionHistoryDto(
    val id: String,
    @SerializedName("session_code") val sessionCode: String,
    @SerializedName("start_time") val startTime: String,
    @SerializedName("duration_sec") val durationSec: Float,
    @SerializedName("input_mode") val inputMode: String,
    @SerializedName("likely_speaker") val likelySpeaker: String?,
    @SerializedName("speaker_similarity") val speakerSimilarity: Float,
    @SerializedName("max_clone_prob") val maxCloneProb: Float,
    @SerializedName("overall_risk") val overallRisk: Int,
    @SerializedName("risk_level") val riskLevel: String,
    val status: String
)

data class DatasetStatsDto(
    @SerializedName("total_files") val totalFiles: Int,
    @SerializedName("real_count") val realCount: Int,
    @SerializedName("fake_count") val fakeCount: Int,
    @SerializedName("real_pct") val realPct: Float,
    @SerializedName("fake_pct") val fakePct: Float,
    @SerializedName("avg_duration_sec") val avgDurationSec: Float,
    @SerializedName("enrolled_speakers_count") val enrolledSpeakersCount: Int,
    @SerializedName("audit_status") val auditStatus: String
)
