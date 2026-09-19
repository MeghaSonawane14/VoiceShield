from typing import Dict, Any, List, Optional

def calculate_risk(
    clone_prob: float,
    speaker_similarity: float,
    is_trusted_speaker_matched: bool,
    intent_analysis: Dict[str, Any],
    verification_status: str = "NOT_STARTED", # NOT_STARTED, PASSED, FAILED
    replay_risk: str = "LOW"
) -> Dict[str, Any]:
    """
    Multi-Signal Risk Assessment Engine.
    Synthesizes synthetic clone probability, speaker profile similarity,
    conversational intent, acoustic replay signals, and verification state.
    """
    reasons: List[str] = []
    
    # 1. Base synthetic voice risk (0 - 45 points)
    # If clone_prob is high, this contributes strongly to overall risk
    synthetic_contribution = clone_prob * 45.0
    if clone_prob >= 0.75:
        reasons.append(f"High synthetic voice probability ({int(clone_prob * 100)}%)")
    elif clone_prob >= 0.50:
        reasons.append(f"Moderate synthetic voice probability ({int(clone_prob * 100)}%)")

    # 2. Impersonation factor (0 - 25 points)
    # Impersonation is uniquely dangerous when an AI voice sounds like a TRUSTED person
    impersonation_contribution = 0.0
    if clone_prob >= 0.60 and is_trusted_speaker_matched and speaker_similarity >= 0.70:
        impersonation_contribution = speaker_similarity * 25.0
        reasons.append(f"Strong similarity with trusted speaker profile ({int(speaker_similarity * 100)}%)")
    elif clone_prob < 0.30 and is_trusted_speaker_matched and speaker_similarity >= 0.80:
        # Genuine trusted voice actually lowers risk
        impersonation_contribution = -10.0
        reasons.append(f"Voice matches registered trusted speaker ({int(speaker_similarity * 100)}%)")

    # 3. Intent / Social Engineering risk (0 - 20 points)
    intent_score = intent_analysis.get("intent_score", 0.0)
    intent_level = intent_analysis.get("suspicious_intent", "LOW")
    triggers = intent_analysis.get("detected_triggers", [])
    
    intent_contribution = intent_score * 20.0
    if intent_level == "HIGH":
        reasons.append(f"Suspicious conversational intent detected ({', '.join(triggers)})")
    elif intent_level == "MEDIUM":
        reasons.append(f"Potentially sensitive intent detected ({', '.join(triggers)})")

    # 4. Replay risk (0 - 10 points)
    replay_contribution = 0.0
    if replay_risk == "HIGH":
        replay_contribution = 10.0
        reasons.append("High channel distortion / potential replay attack detected")
    elif replay_risk == "MEDIUM":
        replay_contribution = 5.0
        reasons.append("Moderate acoustic channel inconsistency")

    # Base subtotal
    raw_score = synthetic_contribution + impersonation_contribution + intent_contribution + replay_contribution

    # 5. Challenge-Response Verification modifier
    if verification_status == "FAILED":
        raw_score += 25.0
        reasons.append("Security challenge-response verification failed")
    elif verification_status == "PASSED":
        raw_score = max(0.0, raw_score - 30.0)
        reasons.append("Caller successfully passed security phrase verification")

    # Clamp 0 to 100
    risk_score = int(round(min(100.0, max(0.0, raw_score))))

    # Determine Risk Tier
    if risk_score >= 81:
        risk_level = "CRITICAL"
        recommended_action = "Possible impersonation – DO NOT share OTP, passwords, or financial information"
        alert_banner = "⚠ CRITICAL: POSSIBLE VOICE IMPERSONATION DETECTED"
    elif risk_score >= 61:
        risk_level = "HIGH"
        recommended_action = "High risk detected – Additional identity verification strongly recommended"
        alert_banner = "⚠ WARNING: SUSPICIOUS VOICE ACTIVITY"
    elif risk_score >= 31:
        risk_level = "MEDIUM"
        recommended_action = "Moderate risk – Stay cautious and verify caller authenticity if requests are unusual"
        alert_banner = "NOTICE: UNVERIFIED VOICE PATTERNS"
    else:
        risk_level = "LOW"
        recommended_action = "Conversation appears safe based on current acoustic and intent telemetry"
        alert_banner = None

    if not reasons:
        reasons.append("Acoustic parameters and conversation flow match natural expectations")

    return {
        "risk_score": risk_score,
        "risk_level": risk_level,
        "recommended_action": recommended_action,
        "alert_banner": alert_banner,
        "reasons": reasons,
        "signals": {
            "synthetic_voice": round(float(clone_prob), 3),
            "speaker_similarity": round(float(speaker_similarity), 3),
            "intent_level": intent_level,
            "replay_risk": replay_risk,
            "verification_status": verification_status
        }
    }
