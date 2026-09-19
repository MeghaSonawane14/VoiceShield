package com.voiceshield.ai.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "local_sessions")
data class SessionEntity(
    @PrimaryKey val sessionId: String,
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

@Entity(tableName = "local_reports")
data class ReportEntity(
    @PrimaryKey val reportId: String,
    val sessionId: String,
    val userId: String,
    val createdAt: String,
    val reportJson: String,
    val isDemo: Boolean = false
)

@Entity(tableName = "local_voice_profile")
data class VoiceProfileEntity(
    @PrimaryKey val userId: String,
    val profileId: String,
    val name: String,
    val status: String,
    val registrationDate: String,
    val embeddingDim: Int,
    val notes: String
)
