import math
import wave
import struct
import numpy as np
from pathlib import Path
from typing import Dict, Any, List
from backend.config import DEMO_DIR

DEMO_SCENARIOS = {
    "scenario_1": {
        "id": "scenario_1",
        "title": "Scenario 1: Genuine Trusted Speaker",
        "category": "Baseline Natural Speech",
        "speaker_name": "Rahul Sharma",
        "description": "Authentic audio from enrolled trusted contact (son). Low synthetic probability and high speaker match.",
        "transcript": "Hey Dad, I just reached the college campus library. Everything is fine, I will call you back in an hour.",
        "audio_file": "scenario_1_genuine.wav",
        "metrics": {
            "clone_probability": 0.08,
            "speaker_similarity": 0.94,
            "likely_speaker": "Rahul Sharma",
            "risk_score": 12,
            "risk_level": "LOW",
            "suspicious_intent": "LOW",
            "detected_triggers": [],
            "replay_risk": "LOW (Experimental)",
            "verification_status": "NOT_NEEDED"
        },
        "timeline": [
            {"time": "00:00", "clone_prob": 0.06, "speaker_sim": 0.92, "risk": 10, "status": "Genuine"},
            {"time": "00:03", "clone_prob": 0.08, "speaker_sim": 0.95, "risk": 12, "status": "Genuine"},
            {"time": "00:06", "clone_prob": 0.07, "speaker_sim": 0.94, "risk": 11, "status": "Genuine"},
            {"time": "00:09", "clone_prob": 0.09, "speaker_sim": 0.93, "risk": 13, "status": "Genuine"}
        ],
        "events": [
            {"timestamp_sec": 1.2, "type": "AUTH", "description": "Speaker matched: Rahul Sharma (94% confidence)", "severity": "INFO"},
            {"timestamp_sec": 3.5, "type": "ACOUSTIC", "description": "Natural vocal tract resonance & pitch variation confirmed", "severity": "INFO"}
        ]
    },
    "scenario_2": {
        "id": "scenario_2",
        "title": "Scenario 2: AI Voice Impersonating Trusted Speaker",
        "category": "Targeted Deepfake Impersonation",
        "speaker_name": "Rahul Sharma (Impersonated)",
        "description": "Synthesized voice mimicking Rahul Sharma's acoustic profile. System flags impersonation alert.",
        "transcript": "Hello Dad, please listen carefully. My phone broke and I am stuck in an emergency. Send me money right now.",
        "audio_file": "scenario_2_impersonation.wav",
        "metrics": {
            "clone_probability": 0.91,
            "speaker_similarity": 0.89,
            "likely_speaker": "Rahul Sharma",
            "risk_score": 87,
            "risk_level": "CRITICAL",
            "suspicious_intent": "MEDIUM",
            "detected_triggers": ["Emergency pretext", "Financial request"],
            "replay_risk": "LOW (Experimental)",
            "verification_status": "REQUIRED"
        },
        "timeline": [
            {"time": "00:00", "clone_prob": 0.35, "speaker_sim": 0.84, "risk": 38, "status": "Analyzing"},
            {"time": "00:03", "clone_prob": 0.68, "speaker_sim": 0.88, "risk": 64, "status": "Suspicious"},
            {"time": "00:06", "clone_prob": 0.88, "speaker_sim": 0.89, "risk": 82, "status": "AI-Generated"},
            {"time": "00:09", "clone_prob": 0.91, "speaker_sim": 0.89, "risk": 87, "status": "AI-Generated"}
        ],
        "events": [
            {"timestamp_sec": 2.1, "type": "DETECTION", "description": "Neural vocoder phase anomaly detected in high frequencies", "severity": "WARNING"},
            {"timestamp_sec": 4.5, "type": "SPEAKER_MATCH", "description": "Acoustic fingerprint closely resembles Rahul Sharma (89%)", "severity": "WARNING"},
            {"timestamp_sec": 7.0, "type": "ALERT", "description": "CRITICAL: Possible voice clone impersonation of Rahul Sharma", "severity": "CRITICAL"}
        ]
    },
    "scenario_3": {
        "id": "scenario_3",
        "title": "Scenario 3: Unknown AI Synthetic Voice",
        "category": "Robocall / Generic Synthetic Audio",
        "speaker_name": "Unknown",
        "description": "Synthesized voice with no match against registered trusted contacts. High clone probability but unknown speaker.",
        "transcript": "This is an automated regulatory notification regarding an immediate update required for your tax file.",
        "audio_file": "scenario_3_unknown_ai.wav",
        "metrics": {
            "clone_probability": 0.87,
            "speaker_similarity": 0.22,
            "likely_speaker": "Unknown / No trusted speaker match",
            "risk_score": 74,
            "risk_level": "HIGH",
            "suspicious_intent": "LOW",
            "detected_triggers": [],
            "replay_risk": "LOW (Experimental)",
            "verification_status": "RECOMMENDED"
        },
        "timeline": [
            {"time": "00:00", "clone_prob": 0.42, "speaker_sim": 0.18, "risk": 45, "status": "Analyzing"},
            {"time": "00:03", "clone_prob": 0.79, "speaker_sim": 0.20, "risk": 68, "status": "AI-Generated"},
            {"time": "00:06", "clone_prob": 0.87, "speaker_sim": 0.22, "risk": 74, "status": "AI-Generated"}
        ],
        "events": [
            {"timestamp_sec": 2.0, "type": "DETECTION", "description": "Flat spectral tilt characteristic of fast speech synthesis", "severity": "WARNING"},
            {"timestamp_sec": 4.2, "type": "MATCH", "description": "No trusted speaker profile matched (max sim: 22%)", "severity": "INFO"}
        ]
    },
    "scenario_4": {
        "id": "scenario_4",
        "title": "Scenario 4: High-Risk Impersonation with Financial Phishing",
        "category": "Active Scam Execution",
        "speaker_name": "Rahul Sharma (Impersonated)",
        "description": "Cloned voice combined with aggressive credential and UPI fund transfer demand.",
        "transcript": "Please send me the OTP immediately and transfer ₹20,000 to my friend's UPI account. Hurry, it's urgent!",
        "audio_file": "scenario_4_scam.wav",
        "metrics": {
            "clone_probability": 0.90,
            "speaker_similarity": 0.91,
            "likely_speaker": "Rahul Sharma",
            "risk_score": 94,
            "risk_level": "CRITICAL",
            "suspicious_intent": "HIGH",
            "detected_triggers": ["OTP request", "Financial request", "UPI payment demand", "High urgency / pressure"],
            "replay_risk": "MEDIUM (Experimental)",
            "verification_status": "REQUIRED"
        },
        "timeline": [
            {"time": "00:00", "clone_prob": 0.40, "speaker_sim": 0.88, "risk": 50, "status": "Analyzing"},
            {"time": "00:03", "clone_prob": 0.76, "speaker_sim": 0.90, "risk": 75, "status": "Suspicious"},
            {"time": "00:06", "clone_prob": 0.88, "speaker_sim": 0.91, "risk": 89, "status": "AI-Generated"},
            {"time": "00:09", "clone_prob": 0.90, "speaker_sim": 0.91, "risk": 94, "status": "Scam Alert"}
        ],
        "events": [
            {"timestamp_sec": 1.8, "type": "INTENT", "description": "High-urgency social engineering keyword detected", "severity": "WARNING"},
            {"timestamp_sec": 3.9, "type": "INTENT", "description": "OTP and immediate bank/UPI transaction request detected", "severity": "CRITICAL"},
            {"timestamp_sec": 6.5, "type": "ALERT", "description": "CRITICAL: Simultaneous clone detection + credential extortion", "severity": "CRITICAL"}
        ]
    },
    "scenario_5": {
        "id": "scenario_5",
        "title": "Scenario 5: Security Challenge Verification Failed",
        "category": "Verification Protocol Failure",
        "speaker_name": "Suspicious Caller",
        "description": "Caller failed the random phrase challenge ('Blue Tiger 47'). System locks session and advises disconnection.",
        "transcript": "Uh, what? What code are you talking about? Just send the money, don't waste time!",
        "audio_file": "scenario_5_failed_verif.wav",
        "metrics": {
            "clone_probability": 0.86,
            "speaker_similarity": 0.72,
            "likely_speaker": "Inconclusive (Rahul Sharma low match)",
            "risk_score": 96,
            "risk_level": "CRITICAL",
            "suspicious_intent": "HIGH",
            "detected_triggers": ["High urgency / pressure", "Challenge evasion"],
            "replay_risk": "HIGH (Experimental)",
            "verification_status": "FAILED"
        },
        "timeline": [
            {"time": "00:00", "clone_prob": 0.65, "speaker_sim": 0.70, "risk": 68, "status": "Suspicious"},
            {"time": "00:04", "clone_prob": 0.82, "speaker_sim": 0.71, "risk": 84, "status": "Challenge Issued"},
            {"time": "00:08", "clone_prob": 0.86, "speaker_sim": 0.72, "risk": 96, "status": "Verification Failed"}
        ],
        "events": [
            {"timestamp_sec": 3.0, "type": "CHALLENGE", "description": "Issued challenge phrase: 'Blue Tiger 47'", "severity": "INFO"},
            {"timestamp_sec": 6.8, "type": "VERIFICATION", "description": "Caller failed security phrase matching (Confidence 12%)", "severity": "CRITICAL"},
            {"timestamp_sec": 8.0, "type": "PROTECTION", "description": "Recommended immediate call termination and account freeze", "severity": "CRITICAL"}
        ]
    }
}

def generate_sample_audio_files():
    """Generate realistic synthesized acoustic wav files for all 5 scenarios."""
    sr = 16000
    for scenario_id, data in DEMO_SCENARIOS.items():
        file_path = DEMO_DIR / data["audio_file"]
        if file_path.exists():
            continue

        duration = 8.0  # seconds
        t = np.linspace(0, duration, int(sr * duration), endpoint=False)
        
        # Scenario-specific acoustic frequency synthesis
        if scenario_id == "scenario_1":
            # Natural human harmonic series with natural pitch micro-vibrato
            f0 = 130.0  # Baritone base pitch
            vibrato = 3.0 * np.sin(2 * np.pi * 5.0 * t)
            signal = (
                0.5 * np.sin(2 * np.pi * (f0 + vibrato) * t) +
                0.3 * np.sin(2 * np.pi * 2 * (f0 + vibrato) * t) +
                0.15 * np.sin(2 * np.pi * 3 * (f0 + vibrato) * t) +
                0.08 * np.sin(2 * np.pi * 4 * (f0 + vibrato) * t)
            )
            # Modulate amplitude envelope to mimic natural speech syllables
            envelope = 0.5 * (1.0 + np.sin(2 * np.pi * 1.8 * t)) * (0.8 + 0.2 * np.sin(2 * np.pi * 0.4 * t))
            signal = signal * envelope
        elif scenario_id in ["scenario_2", "scenario_4"]:
            # Cloned voice: rigid pitch (low jitter), slight robotic phase discontinuity
            f0 = 132.0  # Attempting to mimic 130 Hz
            signal = (
                0.55 * np.sin(2 * np.pi * f0 * t) +
                0.28 * np.sin(2 * np.pi * 2 * f0 * t) +
                0.18 * np.sin(2 * np.pi * 3 * f0 * t) +
                0.12 * np.sin(2 * np.pi * 4 * f0 * t) +
                # Subtle high frequency vocoder artifact
                0.04 * np.random.randn(len(t))
            )
            envelope = 0.6 * (1.0 + np.sin(2 * np.pi * 2.2 * t))
            signal = signal * envelope
        elif scenario_id == "scenario_3":
            # Robotic / TTS voice
            f0 = 175.0
            signal = (
                0.6 * np.sin(2 * np.pi * f0 * t) +
                0.3 * np.sin(2 * np.pi * 2 * f0 * t) +
                0.1 * np.sin(2 * np.pi * 3 * f0 * t)
            )
            envelope = 0.5 * (1.0 + np.sin(2 * np.pi * 2.5 * t))
            signal = signal * envelope
        else:
            # Flustered caller
            f0 = 210.0
            signal = 0.6 * np.sin(2 * np.pi * f0 * t) + 0.1 * np.random.randn(len(t))
            envelope = 0.5 * (1.0 + np.sin(2 * np.pi * 3.0 * t))
            signal = signal * envelope

        # Normalize and convert to 16-bit PCM
        peak = np.max(np.abs(signal))
        if peak > 0:
            signal = (signal / peak) * 0.85
        pcm = (signal * 32767).astype(np.int16)

        with wave.open(str(file_path), "wb") as wf:
            wf.setnchannels(1)
            wf.setsampwidth(2)
            wf.setframerate(sr)
            wf.writeframes(pcm.tobytes())

# Run audio generation on import
try:
    generate_sample_audio_files()
except Exception:
    pass
