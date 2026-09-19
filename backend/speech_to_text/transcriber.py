import re
import os
import requests
from typing import Dict, Any, List, Tuple
from backend.config import ELEVENLABS_API_KEY

SUSPICIOUS_PATTERNS = [
    (r"\b(otp|one[-\s]?time[-\s]?password|verification[-\s]?code)\b", "OTP request", 0.40),
    (r"\b(bank[-\s]?account|account[-\s]?number|ifsc|cvv|routing[-\s]?number)\b", "Banking credentials", 0.35),
    (r"\b(password|pin|security[-\s]?pin|mpin)\b", "Security credentials request", 0.35),
    (r"\b(transfer|send|wire|pay|deposit)\s+(money|funds|cash|rs|rupees|\$|₹|\d+)\b", "Financial request", 0.30),
    (r"\b(upi|gpay|google[-\s]?pay|phonepe|paytm)\b", "UPI payment demand", 0.25),
    (r"\b(emergency|hospital|accident|police|arrested|urgent|trouble)\b", "Emergency pretext", 0.25),
    (r"\b(immediately|right[-\s]?now|asap|hurry|don't tell anyone|keep it secret)\b", "High urgency / pressure", 0.20),
    (r"\b(credit[-\s]?card|debit[-\s]?card|card[-\s]?number|expiry)\b", "Card details request", 0.35)
]

def transcribe_audio(audio_bytes: bytes, fallback_text: str = "") -> str:
    """
    Transcribe audio bytes using ElevenLabs Scribe STT API if key is available.
    Gracefully falls back to fallback_text or generic speech text.
    """
    if ELEVENLABS_API_KEY and len(audio_bytes) > 1024:
        try:
            # ElevenLabs Scribe API endpoint
            url = "https://api.elevenlabs.io/v1/speech-to-text"
            headers = {
                "xi-api-key": ELEVENLABS_API_KEY
            }
            files = {
                "file": ("audio.wav", audio_bytes, "audio/wav")
            }
            data = {
                "model_id": "scribe_v1"
            }
            resp = requests.post(url, headers=headers, files=files, data=data, timeout=8)
            if resp.status_code == 200:
                result = resp.json()
                return result.get("text", "").strip()
        except Exception:
            pass # Fall through to graceful fallback

    return fallback_text or "Hello, can you hear me clearly? Please confirm."

def analyze_intent(transcript: str) -> Dict[str, Any]:
    """
    Analyze conversation transcript for suspicious scam, credential phishing,
    or social engineering intent patterns.
    """
    if not transcript or len(transcript.strip()) == 0:
        return {
            "suspicious_intent": "LOW",
            "intent_score": 0.0,
            "detected_triggers": [],
            "urgency_detected": False,
            "financial_request": False,
            "credential_request": False
        }

    text_lower = transcript.lower()
    triggers = []
    accumulated_risk = 0.0

    has_urgency = False
    has_financial = False
    has_credential = False

    for pattern, label, weight in SUSPICIOUS_PATTERNS:
        if re.search(pattern, text_lower):
            triggers.append(label)
            accumulated_risk += weight
            if "urgency" in label.lower():
                has_urgency = True
            if "financial" in label.lower() or "upi" in label.lower() or "banking" in label.lower():
                has_financial = True
            if "otp" in label.lower() or "credentials" in label.lower() or "card" in label.lower():
                has_credential = True

    # Multi-trigger compounding risk (e.g. OTP + Urgency is classic scam indicator)
    if has_credential and has_financial:
        accumulated_risk += 0.20
    if has_financial and has_urgency:
        accumulated_risk += 0.15

    intent_score = round(float(min(1.0, accumulated_risk)), 3)

    if intent_score >= 0.55:
        intent_level = "HIGH"
    elif intent_score >= 0.25:
        intent_level = "MEDIUM"
    else:
        intent_level = "LOW"

    return {
        "suspicious_intent": intent_level,
        "intent_score": intent_score,
        "detected_triggers": list(set(triggers)),
        "urgency_detected": has_urgency,
        "financial_request": has_financial,
        "credential_request": has_credential
    }
