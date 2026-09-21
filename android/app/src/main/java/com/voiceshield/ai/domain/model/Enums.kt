package com.voiceshield.ai.domain.model

enum class RiskLevel {
    AUTHENTIC,
    LOW,
    SUSPICIOUS,
    HIGH,
    HIGH_RISK;

    val displayName: String
        get() = when (this) {
            AUTHENTIC, LOW -> "AUTHENTIC"
            SUSPICIOUS -> "SUSPICIOUS"
            HIGH, HIGH_RISK -> "HIGH RISK"
        }

    val isHighRisk: Boolean
        get() = this == HIGH || this == HIGH_RISK

    companion object {
        fun fromString(value: String?): RiskLevel = when (value?.uppercase()?.replace("-", "_")?.trim()) {
            "CRITICAL", "HIGH", "HIGH_RISK" -> HIGH_RISK
            "SUSPICIOUS", "MEDIUM" -> SUSPICIOUS
            "AUTHENTIC", "LOW", "SAFE", "LIKELY_AUTHENTIC" -> AUTHENTIC
            else -> AUTHENTIC
        }
    }
}

enum class VoiceStatus {
    AUTHENTIC,
    LIKELY_AUTHENTIC,
    SUSPICIOUS,
    POTENTIAL_CLONE,
    GARBLED_AUDIO;

    companion object {
        fun fromString(value: String?): VoiceStatus = when (value?.uppercase()?.replace("-", "_")?.trim()) {
            "POTENTIAL_CLONE", "LIKELY_CLONED", "CLONED" -> POTENTIAL_CLONE
            "SUSPICIOUS" -> SUSPICIOUS
            "GARBLED_AUDIO", "POOR_QUALITY", "NOISY" -> GARBLED_AUDIO
            else -> LIKELY_AUTHENTIC
        }
    }
}

enum class ConnectionStatus {
    CONNECTED,
    CONNECTING,
    DISCONNECTED
}

enum class InputMode {
    LIVE_MIC,
    AUDIO_UPLOAD,
    DEMO
}
