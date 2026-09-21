package com.voiceshield.ai.services

import com.voiceshield.ai.domain.model.*

class DemoScenarioProvider {

    fun getAllScenarios(): List<DemoScenario> = scenariosList

    fun getScenario(id: String): DemoScenario {
        return scenariosList.find { it.id.equals(id, ignoreCase = true) }
            ?: scenariosList.first()
    }

    fun getDemoScenarios(): List<DemoScenario> = scenariosList

    companion object {
        private val scenariosList: List<DemoScenario> = listOf(
            // 1. Safe Authentic Voice
            DemoScenario(
                id = "safe_authentic",
                title = "Safe Authentic Voice",
                category = "Natural Human Speech",
                description = "Natural vocal tract resonance, organic micro-pitch jitter, and strong biometric alignment with enrolled profile.",
                baseRiskLevel = RiskLevel.AUTHENTIC,
                expectedRiskLevel = RiskLevel.AUTHENTIC,
                expectedSpeaker = "Trusted Contact",
                transcript = "Hey, I am heading over to the office right now. I will call you back on the landline in ten minutes.",
                expectedAction = "Permit call stream with passive background monitoring",
                timelinePoints = listOf(
                    RiskTimelinePoint("00:02", 2.0f, "00:02", 0.05f, 0.05f, 0.95f, 0.95f, 10, RiskLevel.AUTHENTIC),
                    RiskTimelinePoint("00:04", 4.0f, "00:04", 0.06f, 0.06f, 0.96f, 0.96f, 12, RiskLevel.AUTHENTIC),
                    RiskTimelinePoint("00:06", 6.0f, "00:06", 0.04f, 0.04f, 0.94f, 0.94f, 8, RiskLevel.AUTHENTIC),
                    RiskTimelinePoint("00:08", 8.0f, "00:08", 0.05f, 0.05f, 0.97f, 0.97f, 11, RiskLevel.AUTHENTIC)
                )
            ),

            // 2. Moderate Garbled Audio
            DemoScenario(
                id = "moderate_garbled",
                title = "Moderate Garbled Audio",
                category = "Acoustic Degradation",
                description = "Severe cellular compression and lossy codec packet drop simulating VoIP jitter. Inconclusive neural markers.",
                baseRiskLevel = RiskLevel.SUSPICIOUS,
                expectedRiskLevel = RiskLevel.SUSPICIOUS,
                expectedSpeaker = "Unverified Caller",
                transcript = "Hello? Can you hear me? The connection here in the tunnel is breaking up completely.",
                expectedAction = "Request speaker to re-dial or move to clearer cellular reception",
                timelinePoints = listOf(
                    RiskTimelinePoint("00:02", 2.0f, "00:02", 0.38f, 0.38f, 0.65f, 0.65f, 42, RiskLevel.SUSPICIOUS),
                    RiskTimelinePoint("00:04", 4.0f, "00:04", 0.45f, 0.45f, 0.58f, 0.58f, 48, RiskLevel.SUSPICIOUS),
                    RiskTimelinePoint("00:06", 6.0f, "00:06", 0.41f, 0.41f, 0.62f, 0.62f, 44, RiskLevel.SUSPICIOUS),
                    RiskTimelinePoint("00:08", 8.0f, "00:08", 0.49f, 0.49f, 0.55f, 0.55f, 52, RiskLevel.SUSPICIOUS)
                )
            ),

            // 3. High Risk ElevenLabs Clone
            DemoScenario(
                id = "elevenlabs_clone",
                title = "High Risk ElevenLabs Clone",
                category = "Neural Vocoder Deepfake",
                description = "Hyper-realistic voice clone generated via diffusion vocoder. Exhibits spectral flatness anomalies in upper frequencies.",
                baseRiskLevel = RiskLevel.HIGH_RISK,
                expectedRiskLevel = RiskLevel.HIGH_RISK,
                expectedSpeaker = "Synthetic Clone (ElevenLabs)",
                transcript = "Urgent message: Our bank account was compromised. Please transfer the vendor deposit to the temporary escrow.",
                expectedAction = "Trigger High-Risk Advisory Modal and halt financial transactions immediately",
                timelinePoints = listOf(
                    RiskTimelinePoint("00:02", 2.0f, "00:02", 0.78f, 0.78f, 0.88f, 0.88f, 76, RiskLevel.HIGH_RISK),
                    RiskTimelinePoint("00:04", 4.0f, "00:04", 0.89f, 0.89f, 0.90f, 0.90f, 88, RiskLevel.HIGH_RISK),
                    RiskTimelinePoint("00:06", 6.0f, "00:06", 0.94f, 0.94f, 0.92f, 0.92f, 94, RiskLevel.HIGH_RISK),
                    RiskTimelinePoint("00:08", 8.0f, "00:08", 0.96f, 0.96f, 0.89f, 0.89f, 95, RiskLevel.HIGH_RISK)
                )
            ),

            // 4. Tortoise-TTS Clone
            DemoScenario(
                id = "tortoise_tts_clone",
                title = "Tortoise-TTS Neural Clone",
                category = "Autoregressive Speech Synthesis",
                description = "Synthesized speech containing latent autoregressive timing delays and repetitive acoustic prosody patterns.",
                baseRiskLevel = RiskLevel.HIGH_RISK,
                expectedRiskLevel = RiskLevel.HIGH_RISK,
                expectedSpeaker = "Synthetic Clone (Tortoise)",
                transcript = "This is David from technical operations. We need you to verify your multi-factor credentials right now.",
                expectedAction = "Deploy conversational challenge phrase to expose vocoder synthesis latency",
                timelinePoints = listOf(
                    RiskTimelinePoint("00:02", 2.0f, "00:02", 0.72f, 0.72f, 0.82f, 0.82f, 74, RiskLevel.HIGH_RISK),
                    RiskTimelinePoint("00:04", 4.0f, "00:04", 0.84f, 0.84f, 0.85f, 0.85f, 85, RiskLevel.HIGH_RISK),
                    RiskTimelinePoint("00:06", 6.0f, "00:06", 0.91f, 0.91f, 0.86f, 0.86f, 91, RiskLevel.HIGH_RISK),
                    RiskTimelinePoint("00:08", 8.0f, "00:08", 0.88f, 0.88f, 0.84f, 0.84f, 89, RiskLevel.HIGH_RISK)
                )
            ),

            // 5. Speaker Impersonation Attempt
            DemoScenario(
                id = "speaker_impersonation",
                title = "Speaker Impersonation Mismatch",
                category = "Biometric Divergence",
                description = "Human voice attempting manual impersonation or social engineering. Fails enrolled 1:1 ECAPA-TDNN biometric check.",
                baseRiskLevel = RiskLevel.HIGH_RISK,
                expectedRiskLevel = RiskLevel.HIGH_RISK,
                expectedSpeaker = "Impersonator (Biometric Mismatch)",
                transcript = "Yes, it is me, your family member. My phone broke so I am calling from a stranger's number. Send money.",
                expectedAction = "Verify identity out-of-band using pre-enrolled trusted mobile number",
                timelinePoints = listOf(
                    RiskTimelinePoint("00:02", 2.0f, "00:02", 0.22f, 0.22f, 0.32f, 0.32f, 68, RiskLevel.SUSPICIOUS),
                    RiskTimelinePoint("00:04", 4.0f, "00:04", 0.25f, 0.25f, 0.28f, 0.28f, 75, RiskLevel.HIGH_RISK),
                    RiskTimelinePoint("00:06", 6.0f, "00:06", 0.30f, 0.30f, 0.24f, 0.24f, 82, RiskLevel.HIGH_RISK),
                    RiskTimelinePoint("00:08", 8.0f, "00:08", 0.28f, 0.28f, 0.21f, 0.21f, 84, RiskLevel.HIGH_RISK)
                )
            ),

            // 6. Dynamic Shift (Authentic -> Cloned mid-call)
            DemoScenario(
                id = "dynamic_shift",
                title = "Dynamic Shift (Mid-Call Clone Injection)",
                category = "Hybrid Voice Hijack",
                description = "Call begins with an authentic human voice, then seamlessly transitions to an AI cloned audio injection at 00:05.",
                baseRiskLevel = RiskLevel.HIGH_RISK,
                expectedRiskLevel = RiskLevel.HIGH_RISK,
                expectedSpeaker = "Authentic Contact -> Synthetic Injected",
                transcript = "Hi there, great seeing you yesterday. [Injection] By the way, send over the wire routing number right now.",
                expectedAction = "Observe live risk timeline spike from Green to Red and terminate session",
                timelinePoints = listOf(
                    RiskTimelinePoint("00:02", 2.0f, "00:02", 0.08f, 0.08f, 0.94f, 0.94f, 12, RiskLevel.AUTHENTIC),
                    RiskTimelinePoint("00:04", 4.0f, "00:04", 0.12f, 0.12f, 0.92f, 0.92f, 16, RiskLevel.AUTHENTIC),
                    RiskTimelinePoint("00:06", 6.0f, "00:06", 0.82f, 0.82f, 0.88f, 0.88f, 84, RiskLevel.HIGH_RISK),
                    RiskTimelinePoint("00:08", 8.0f, "00:08", 0.95f, 0.95f, 0.89f, 0.89f, 96, RiskLevel.HIGH_RISK)
                )
            ),

            // 7. Low SNR Audio
            DemoScenario(
                id = "low_snr_audio",
                title = "Low SNR / Replay Signature",
                category = "Acoustic Channel Reverberation",
                description = "Loud background acoustic noise combined with spectral re-recording artifacts typical of recorded voice playback.",
                baseRiskLevel = RiskLevel.SUSPICIOUS,
                expectedRiskLevel = RiskLevel.SUSPICIOUS,
                expectedSpeaker = "Loud Background / Speakerphone",
                transcript = "I am on the train platform right now, please listen to what I am saying about the project.",
                expectedAction = "Monitor acoustic channel reverberation and observe spectral flatness metrics",
                timelinePoints = listOf(
                    RiskTimelinePoint("00:02", 2.0f, "00:02", 0.35f, 0.35f, 0.68f, 0.68f, 38, RiskLevel.AUTHENTIC),
                    RiskTimelinePoint("00:04", 4.0f, "00:04", 0.48f, 0.48f, 0.60f, 0.60f, 52, RiskLevel.SUSPICIOUS),
                    RiskTimelinePoint("00:06", 6.0f, "00:06", 0.52f, 0.52f, 0.59f, 0.59f, 56, RiskLevel.SUSPICIOUS),
                    RiskTimelinePoint("00:08", 8.0f, "00:08", 0.46f, 0.46f, 0.64f, 0.64f, 50, RiskLevel.SUSPICIOUS)
                )
            )
        )
    }
}
