package com.voiceshield.ai.services

import com.voiceshield.ai.domain.model.*

object DemoScenarioProvider {

    fun getDemoScenarios(): List<DemoScenario> = listOf(
        // 1. Likely Authentic
        DemoScenario(
            id = "demo_authentic",
            title = "Likely Authentic Contact",
            category = "Natural Human Speech",
            description = "Natural vocal tract resonance and dynamic harmonics. Matches enrolled speaker profile.",
            baseRiskLevel = RiskLevel.LOW,
            expectedSpeaker = "Rahul Sharma",
            transcript = "Hey, I am heading over to the campus library right now. I will call you back in an hour.",
            steps = listOf(
                AnalysisResult(
                    sessionId = "DEMO-AUTH-001",
                    timestamp = "00:03",
                    timestampSec = 3.0f,
                    cloneProbability = 0.07f,
                    speakerSimilarity = 0.94f,
                    riskScore = 0.11f,
                    riskScorePct = 11,
                    riskLevel = RiskLevel.LOW,
                    voiceStatus = VoiceStatus.LIKELY_AUTHENTIC,
                    likelySpeaker = "Rahul Sharma",
                    isSpeakerMatched = true,
                    riskReasons = emptyList(),
                    isDemoModel = true
                ),
                AnalysisResult(
                    sessionId = "DEMO-AUTH-001",
                    timestamp = "00:06",
                    timestampSec = 6.0f,
                    cloneProbability = 0.08f,
                    speakerSimilarity = 0.95f,
                    riskScore = 0.12f,
                    riskScorePct = 12,
                    riskLevel = RiskLevel.LOW,
                    voiceStatus = VoiceStatus.LIKELY_AUTHENTIC,
                    likelySpeaker = "Rahul Sharma",
                    isSpeakerMatched = true,
                    riskReasons = emptyList(),
                    isDemoModel = true
                )
            )
        ),

        // 2. Suspicious
        DemoScenario(
            id = "demo_suspicious",
            title = "Suspicious Acoustic Artifacts",
            category = "Borderline Quality / Unnatural Resynthesis",
            description = "Acoustic features display unnatural high-frequency attenuation and mild jitter distortion.",
            baseRiskLevel = RiskLevel.SUSPICIOUS,
            expectedSpeaker = "Rahul Sharma (Unverified)",
            transcript = "Can you verify this urgent notification from the bank? The line is breaking up.",
            steps = listOf(
                AnalysisResult(
                    sessionId = "DEMO-SUSP-002",
                    timestamp = "00:03",
                    timestampSec = 3.0f,
                    cloneProbability = 0.48f,
                    speakerSimilarity = 0.68f,
                    riskScore = 0.45f,
                    riskScorePct = 45,
                    riskLevel = RiskLevel.SUSPICIOUS,
                    voiceStatus = VoiceStatus.SUSPICIOUS,
                    likelySpeaker = "Inconclusive Match",
                    isSpeakerMatched = false,
                    riskReasons = listOf("Unnatural spectral rolloff cutoff", "Borderline speaker profile divergence"),
                    isDemoModel = true
                )
            )
        ),

        // 3. High Risk
        DemoScenario(
            id = "demo_high_risk",
            title = "High-Risk AI Clone & Extortion",
            category = "Targeted Deepfake Scam",
            description = "High synthetic vocoder probability combined with aggressive pretext keywords.",
            baseRiskLevel = RiskLevel.HIGH,
            expectedSpeaker = "Impersonating Rahul Sharma",
            transcript = "Listen to me carefully! Send money immediately via UPI to this number, do not question it!",
            steps = listOf(
                AnalysisResult(
                    sessionId = "DEMO-HIGH-003",
                    timestamp = "00:04",
                    timestampSec = 4.0f,
                    cloneProbability = 0.91f,
                    speakerSimilarity = 0.89f,
                    riskScore = 0.94f,
                    riskScorePct = 94,
                    riskLevel = RiskLevel.HIGH,
                    voiceStatus = VoiceStatus.POTENTIAL_CLONE,
                    likelySpeaker = "Rahul Sharma (Impersonated)",
                    isSpeakerMatched = true,
                    riskReasons = listOf(
                        "High spectral flatness characteristic of neural vocoders",
                        "Urgency & credential extortion pretexts detected in speech"
                    ),
                    isDemoModel = true
                )
            )
        ),

        // 4. Garbled / Poor Audio
        DemoScenario(
            id = "demo_garbled",
            title = "Garbled / Poor Audio Quality",
            category = "Channel Degradation",
            description = "Low Signal-to-Noise Ratio (SNR) and severe packet loss. System advises cautionary re-verification.",
            baseRiskLevel = RiskLevel.SUSPICIOUS,
            expectedSpeaker = "Unknown",
            transcript = "[Low SNR / Clipped Audio] ...hello... can you... hear...",
            steps = listOf(
                AnalysisResult(
                    sessionId = "DEMO-GARB-004",
                    timestamp = "00:03",
                    timestampSec = 3.0f,
                    cloneProbability = 0.35f,
                    speakerSimilarity = 0.30f,
                    riskScore = 0.52f,
                    riskScorePct = 52,
                    riskLevel = RiskLevel.SUSPICIOUS,
                    voiceStatus = VoiceStatus.GARBLED_AUDIO,
                    likelySpeaker = "Unidentifiable due to channel noise",
                    isSpeakerMatched = false,
                    riskReasons = listOf("Excessive background noise / frame clipping prevents reliable acoustic modeling"),
                    isDemoModel = true
                )
            )
        ),

        // 5. Voice Clone Scenario
        DemoScenario(
            id = "demo_voice_clone",
            title = "Commercial TTS / Cloned Voice",
            category = "Autonomous Voice Generator",
            description = "Synthetic voice exhibiting phase continuity anomalies typical of diffusion/flow matching models.",
            baseRiskLevel = RiskLevel.HIGH,
            expectedSpeaker = "Synthetic Robocall",
            transcript = "Your account has been restricted due to unauthorized security actions. Press 1 to verify.",
            steps = listOf(
                AnalysisResult(
                    sessionId = "DEMO-CLONE-005",
                    timestamp = "00:03",
                    timestampSec = 3.0f,
                    cloneProbability = 0.88f,
                    speakerSimilarity = 0.15f,
                    riskScore = 0.85f,
                    riskScorePct = 85,
                    riskLevel = RiskLevel.HIGH,
                    voiceStatus = VoiceStatus.POTENTIAL_CLONE,
                    likelySpeaker = "No trusted speaker profile matched",
                    isSpeakerMatched = false,
                    riskReasons = listOf("Unnatural pitch regularity", "Zero speaker enrollment match"),
                    isDemoModel = true
                )
            )
        ),

        // 6. Speaker Mismatch Scenario
        DemoScenario(
            id = "demo_speaker_mismatch",
            title = "Unknown Caller Mismatch",
            category = "Unenrolled Voice",
            description = "Natural human voice, but completely distinct from all enrolled family/trusted profiles.",
            baseRiskLevel = RiskLevel.SUSPICIOUS,
            expectedSpeaker = "Unrecognized Caller",
            transcript = "Hello sir, I am calling regarding your recent loan application inquiry.",
            steps = listOf(
                AnalysisResult(
                    sessionId = "DEMO-MISM-006",
                    timestamp = "00:03",
                    timestampSec = 3.0f,
                    cloneProbability = 0.12f,
                    speakerSimilarity = 0.22f,
                    riskScore = 0.42f,
                    riskScorePct = 42,
                    riskLevel = RiskLevel.SUSPICIOUS,
                    voiceStatus = VoiceStatus.LIKELY_AUTHENTIC,
                    likelySpeaker = "Caller does not match enrolled trusted voiceprint",
                    isSpeakerMatched = false,
                    riskReasons = listOf("Speaker embedding similarity below 30% threshold"),
                    isDemoModel = true
                )
            )
        ),

        // 7. Dynamic / Changing Risk Scenario
        DemoScenario(
            id = "demo_dynamic_risk",
            title = "Dynamic Escalating Risk",
            category = "Multi-Stage Call Transition",
            description = "Starts as plausible natural conversation, then abruptly transitions to cloned synthetic voice.",
            baseRiskLevel = RiskLevel.HIGH,
            expectedSpeaker = "Rahul Sharma (Compromised)",
            transcript = "Hey Dad, my battery was dying... [pause] ...SEND ME THE MONEY NOW, I'M IN DANGER!",
            steps = listOf(
                AnalysisResult(
                    sessionId = "DEMO-DYN-007",
                    timestamp = "00:02",
                    timestampSec = 2.0f,
                    cloneProbability = 0.15f,
                    speakerSimilarity = 0.90f,
                    riskScore = 0.18f,
                    riskScorePct = 18,
                    riskLevel = RiskLevel.LOW,
                    voiceStatus = VoiceStatus.LIKELY_AUTHENTIC,
                    likelySpeaker = "Rahul Sharma",
                    isSpeakerMatched = true,
                    isDemoModel = true
                ),
                AnalysisResult(
                    sessionId = "DEMO-DYN-007",
                    timestamp = "00:05",
                    timestampSec = 5.0f,
                    cloneProbability = 0.55f,
                    speakerSimilarity = 0.85f,
                    riskScore = 0.58f,
                    riskScorePct = 58,
                    riskLevel = RiskLevel.SUSPICIOUS,
                    voiceStatus = VoiceStatus.SUSPICIOUS,
                    likelySpeaker = "Rahul Sharma",
                    isSpeakerMatched = true,
                    riskReasons = listOf("Acoustic transition: abrupt pitch contour shift"),
                    isDemoModel = true
                ),
                AnalysisResult(
                    sessionId = "DEMO-DYN-007",
                    timestamp = "00:08",
                    timestampSec = 8.0f,
                    cloneProbability = 0.93f,
                    speakerSimilarity = 0.88f,
                    riskScore = 0.95f,
                    riskScorePct = 95,
                    riskLevel = RiskLevel.HIGH,
                    voiceStatus = VoiceStatus.POTENTIAL_CLONE,
                    likelySpeaker = "Rahul Sharma (Impersonated)",
                    isSpeakerMatched = true,
                    riskReasons = listOf(
                        "CRITICAL: Neural vocoder signature detected",
                        "Pretext escalation: Emergency financial demand"
                    ),
                    isDemoModel = true
                )
            )
        )
    )
}
