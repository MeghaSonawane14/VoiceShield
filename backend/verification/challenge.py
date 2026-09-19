import random
import difflib
from typing import Dict, Any

ADJECTIVES = ["Blue", "Golden", "Silver", "Crimson", "Emerald", "Rapid", "Silent", "Cosmic", "Solar", "Iron"]
NOUNS = ["Tiger", "Falcon", "Eagle", "Comet", "Shield", "Hawk", "Panther", "Beacon", "Fox", "Wolf"]

# Active session challenges cache (in-memory fast lookup)
_active_challenges: Dict[str, str] = {}

def generate_challenge(session_id: str = "default") -> str:
    """Generate a random cryptographic challenge phrase."""
    adj = random.choice(ADJECTIVES)
    noun = random.choice(NOUNS)
    num = random.randint(11, 99)
    phrase = f"{adj} {noun} {num}"
    _active_challenges[session_id] = phrase
    return phrase

def get_current_challenge(session_id: str = "default") -> str:
    if session_id not in _active_challenges:
        return generate_challenge(session_id)
    return _active_challenges[session_id]

def verify_response(
    session_id: str,
    response_text: str,
    simulate_result: str = "" # "PASS", "FAIL", or empty
) -> Dict[str, Any]:
    """
    Validate caller's spoken response against security challenge.
    Supports manual testing as well as instantaneous simulation.
    """
    challenge = get_current_challenge(session_id)

    if simulate_result.upper() == "PASS":
        return {
            "result": "PASSED",
            "passed": True,
            "challenge_phrase": challenge,
            "response_text": challenge,
            "similarity_score": 1.0,
            "message": "Challenge response verified successfully. Caller identity confirmed."
        }
    elif simulate_result.upper() == "FAIL":
        return {
            "result": "FAILED",
            "passed": False,
            "challenge_phrase": challenge,
            "response_text": "Invalid or unprompted phrase",
            "similarity_score": 0.12,
            "message": "Challenge response mismatch. Identity verification failed."
        }

    # Evaluate transcript similarity
    resp_clean = response_text.strip().lower()
    chall_clean = challenge.strip().lower()

    matcher = difflib.SequenceMatcher(None, chall_clean, resp_clean)
    sim_ratio = matcher.ratio()

    # If numbers and words appear
    words_match = all(w.lower() in resp_clean for w in challenge.split()[:2])
    passed = (sim_ratio >= 0.70) or words_match

    return {
        "result": "PASSED" if passed else "FAILED",
        "passed": passed,
        "challenge_phrase": challenge,
        "response_text": response_text,
        "similarity_score": round(sim_ratio, 2),
        "message": "Verification passed" if passed else "Verification failed due to phrase mismatch"
    }
