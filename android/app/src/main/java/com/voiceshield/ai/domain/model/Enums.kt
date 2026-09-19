package com.voiceshield.ai.domain.model

enum class RiskLevel {
    LOW,
    SUSPICIOUS,
    HIGH;

    companion object {
        fun fromString(value: String?): RiskLevel = when (value?.uppercase()) {
            "CRITICAL", "HIGH" -> HIGH
            "SUSPICIOUS", "MEDIUM" -> SUSPICIOUS
            else -> LOW
        }
    }
}

enum class VoiceStatus {
    LIKELY_AUTHENTIC,
    SUSPICIOUS,
    POTENTIAL_CLONE,
    GARBLED_AUDIO;

    companion object {
        fun fromString(value: String?): VoiceStatus = when (value?.uppercase()) {
            "POTENTIAL_CLONE", "LIKELY_CLONED" -> POTENTIAL_CLONE
            "SUSPICIOUS" -> SUSPICIOUS
            "GARBLED_AUDIO", "POOR_QUALITY" -> GARBLED_AUDIO
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
