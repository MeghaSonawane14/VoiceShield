"""
VoiceShield AI - Comprehensive Unit & Integration Test Suite
===========================================================
Tests audio preprocessing, clone detection heuristics, speaker embeddings,
intent parsing, risk evaluation, challenge-response verification,
REST endpoints, and real-time WebSocket streaming.
"""

import os
import pytest
import numpy as np
from fastapi.testclient import TestClient

from backend.main import app
from backend.audio_processing.preprocessor import (
    normalize_audio, apply_vad, extract_acoustic_features
)
from backend.clone_detection.detector import (
    get_clone_detector, AcousticArtifactDetector, DemoModelProvider
)
from backend.speaker_recognition.recognizer import (
    compute_speaker_embedding, cosine_similarity
)
from backend.speech_to_text.transcriber import analyze_intent
from backend.verification.challenge import (
    generate_challenge, verify_response
)
from backend.risk_engine.evaluator import calculate_risk
from backend.reporting.generator import generate_security_report, generate_html_report
from backend.demo.scenarios import DEMO_SCENARIOS

client = TestClient(app)

# 1. Audio Preprocessing Tests
def test_audio_normalization_and_vad():
    sr = 16000
    t = np.linspace(0, 1.0, sr)
    raw_signal = np.sin(2 * np.pi * 440 * t) * 0.5
    
    norm = normalize_audio(raw_signal)
    assert np.max(np.abs(norm)) <= 1.01
    
    voiced, ratio = apply_vad(norm)
    assert len(voiced) > 0
    assert 0.0 <= ratio <= 1.0

def test_feature_extraction():
    sr = 16000
    t = np.linspace(0, 0.5, 8000)
    signal = np.sin(2 * np.pi * 300 * t) * 0.8
    feats = extract_acoustic_features(signal, sr)
    
    assert "spectral_centroid" in feats
    assert "spectral_flatness" in feats
    assert "spectral_rolloff" in feats
    assert "zero_crossing_rate" in feats
    assert "rms_energy" in feats
    assert "mfcc" in feats
    assert len(feats["mfcc"]) >= 13

# 2. Voice Clone Detector Tests
def test_clone_detector_calibrated():
    detector = get_clone_detector("calibrated")
    assert isinstance(detector, AcousticArtifactDetector)
    
    sr = 16000
    t = np.linspace(0, 0.5, 8000)
    audio = np.sin(2 * np.pi * 200 * t)
    feats = extract_acoustic_features(audio, sr)
    
    res = detector.predict(audio, feats, sr)
    assert "clone_probability" in res
    assert 0.0 <= res["clone_probability"] <= 1.0
    assert "classification" in res
    assert res["classification"] in ("LIKELY_CLONED", "SUSPICIOUS", "LIKELY_AUTHENTIC")
    assert res["is_demo_model"] is False

def test_clone_detector_demo_provider():
    detector = get_clone_detector("demo")
    assert isinstance(detector, DemoModelProvider)
    assert detector.is_demo is True
    
    sr = 16000
    audio = np.zeros(1000, dtype=np.float32)
    feats = {"spectral_flatness": 0.08}
    res = detector.predict(audio, feats, sr)
    assert res["is_demo_model"] is True
    assert "DEMO MODEL" in res["model_name"]

# 3. Speaker Verification & Embedding Tests
def test_speaker_embedding_and_similarity():
    sr = 16000
    t = np.linspace(0, 0.5, 8000)
    sig1 = np.sin(2 * np.pi * 150 * t)
    sig2 = np.sin(2 * np.pi * 150 * t)
    sig3 = np.sin(2 * np.pi * 600 * t)
    
    emb1 = compute_speaker_embedding(sig1, sr)
    emb2 = compute_speaker_embedding(sig2, sr)
    emb3 = compute_speaker_embedding(sig3, sr)
    
    assert len(emb1) == 128
    sim_identical = cosine_similarity(emb1, emb2)
    assert sim_identical >= 0.98
    
    sim_different = cosine_similarity(emb1, emb3)
    assert sim_different < sim_identical

# 4. Intent Analysis Tests
def test_intent_analysis_scam_detection():
    # Benign text
    benign = analyze_intent("Hey Dad, I am heading over to the campus library today to study.")
    assert benign["suspicious_intent"] == "LOW"
    assert benign["intent_score"] == 0.0
    
    # Scam / Phishing text
    scam = analyze_intent("Please send the OTP right now and transfer money to my UPI account immediately, it is an emergency!")
    assert scam["suspicious_intent"] == "HIGH"
    assert scam["urgency_detected"] is True
    assert scam["financial_request"] is True
    assert scam["credential_request"] is True
    assert len(scam["detected_triggers"]) >= 2

# 5. Challenge Response Tests
def test_challenge_response():
    phrase = generate_challenge("test-session")
    assert len(phrase.split()) == 3
    
    # Pass simulation
    passed = verify_response("test-session", phrase, simulate_result="PASS")
    assert passed["passed"] is True
    assert passed["result"] == "PASSED"
    
    # Fail simulation
    failed = verify_response("test-session", "random words", simulate_result="FAIL")
    assert failed["passed"] is False
    assert failed["result"] == "FAILED"

# 6. Risk Engine Compound Evaluation Tests
def test_risk_evaluation_levels():
    # Low risk
    low = calculate_risk(
        clone_prob=0.08,
        speaker_similarity=0.92,
        is_trusted_speaker_matched=True,
        intent_analysis={"suspicious_intent": "LOW", "intent_score": 0.0, "detected_triggers": []}
    )
    assert low["risk_level"] == "LOW"
    assert low["risk_score"] < 35
    
    # High risk impersonation
    high = calculate_risk(
        clone_prob=0.92,
        speaker_similarity=0.88,
        is_trusted_speaker_matched=True,
        intent_analysis={"suspicious_intent": "HIGH", "intent_score": 0.85, "detected_triggers": ["OTP", "Urgency"]}
    )
    assert high["risk_level"] == "CRITICAL" or high["risk_level"] == "HIGH"
    assert high["risk_score"] >= 75

# 7. Demo Scenarios Completeness
def test_demo_scenarios():
    assert len(DEMO_SCENARIOS) >= 5
    for scen_id, data in DEMO_SCENARIOS.items():
        assert "title" in data
        assert "metrics" in data
        assert "timeline" in data
        assert "audio_file" in data

# 8. Report Generator Tests
def test_report_generation():
    sess_mock = {
        "id": "sess-test",
        "session_code": "VS-TEST",
        "duration_sec": 10.0,
        "input_mode": "TEST",
        "overall_risk": 82,
        "risk_level": "HIGH",
        "max_clone_prob": 0.88,
        "likely_speaker": "Rahul Sharma",
        "speaker_similarity": 0.85
    }
    report = generate_security_report("sess-test", sess_mock, [], [], {"result": "PASSED"})
    assert report["session_id"] == "sess-test"
    assert report["executive_summary"]["risk_level"] == "HIGH"
    
    html = generate_html_report(report)
    assert "<!DOCTYPE html>" in html
    assert "VoiceShield AI Forensic Security Report" in html

# 9. REST API Integration Tests
def test_api_endpoints():
    r_root = client.get("/")
    assert r_root.status_code == 200
    
    # User Profile
    r_user = client.get("/users/me")
    assert r_user.status_code == 200
    assert "user_id" in r_user.json()
    
    # Dataset Stats
    r_ds = client.get("/api/dataset/stats")
    assert r_ds.status_code == 200
    assert r_ds.json().get("total_files") == 55
    
    # Voice Profile lifecycle
    r_vp = client.get("/voice-profile")
    assert r_vp.status_code == 200
    
    # Analysis start and stop
    r_start = client.post("/analysis/start", data={"mode": "LIVE_MIC"})
    assert r_start.status_code == 200
    sess_id = r_start.json()["session_id"]
    
    r_stop = client.post("/analysis/stop", data={"session_id": sess_id})
    assert r_stop.status_code == 200
    assert r_stop.json()["status"] == "STOPPED"
    
    r_hist = client.get("/analysis/history")
    assert r_hist.status_code == 200
    assert isinstance(r_hist.json(), list)

# 10. Real-Time WebSocket Streaming Test
def test_websocket_audio_streaming():
    with client.websocket_connect("/ws/voice-analysis/test-ws-session") as ws:
        t = np.linspace(0, 0.5, 8000)
        pcm = (np.sin(2 * np.pi * 350 * t) * 30000).astype(np.int16)
        ws.send_bytes(pcm.tobytes())
        res = ws.receive_json()
        
        assert res["session_id"] == "test-ws-session"
        assert "risk_score" in res
        assert "risk_level" in res
        assert "clone_probability" in res
        assert "voice_status" in res
