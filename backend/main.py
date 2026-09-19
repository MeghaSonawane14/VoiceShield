import os
import json
import uuid
import datetime
from typing import Optional, List, Dict, Any
from fastapi import FastAPI, UploadFile, File, Form, HTTPException, WebSocket, WebSocketDisconnect, Depends
from fastapi.middleware.cors import CORSMiddleware
from fastapi.staticfiles import StaticFiles
from fastapi.responses import HTMLResponse, FileResponse
from pydantic import BaseModel

from backend.config import BASE_DIR, DEMO_DIR, REPORTS_DIR, DATASET_DIR, SYSTEM_DISCLAIMER
from backend.database.db import (
    init_db, get_all_trusted_speakers, add_trusted_speaker, delete_trusted_speaker,
    get_speaker_by_id, create_session, update_session, get_session,
    add_analysis_point, get_session_timeline, add_risk_event, get_risk_events,
    save_report, get_report, get_user_sessions, delete_user_session,
    get_user_voice_profile, save_user_voice_profile, delete_user_voice_profile,
    get_user_report
)
from backend.audio_processing.preprocessor import (
    load_audio_from_bytes, normalize_audio, apply_vad, extract_acoustic_features
)
from backend.clone_detection.detector import get_clone_detector
from backend.speaker_recognition.recognizer import (
    compute_speaker_embedding, match_speaker, cosine_similarity
)
from backend.speech_to_text.transcriber import transcribe_audio, analyze_intent
from backend.risk_engine.evaluator import calculate_risk
from backend.verification.challenge import (
    generate_challenge, get_current_challenge, verify_response
)
from backend.reporting.generator import generate_security_report, generate_html_report
from backend.demo.scenarios import DEMO_SCENARIOS, generate_sample_audio_files
from backend.auth.firebase_auth import get_current_user, get_optional_user
from backend.websocket.voice_socket import router as websocket_router

# Initialize database and demo files
init_db()
generate_sample_audio_files()

app = FastAPI(
    title="VoiceShield AI Backend",
    description="Real-Time Voice Clone Detection & Impersonation Prevention Academic Prototype",
    version="1.0.0"
)

# CORS Middleware
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Include real-time WebSocket router (/ws/voice-analysis/{session_id})
app.include_router(websocket_router)

# Mount demo audio files statically
app.mount("/demo/audio", StaticFiles(directory=str(DEMO_DIR)), name="demo_audio")

# Mount frontend production build if available
FRONTEND_DIST = BASE_DIR / "frontend" / "dist"
if FRONTEND_DIST.exists():
    app.mount("/app", StaticFiles(directory=str(FRONTEND_DIST), html=True), name="frontend_app")


# Models
class RiskAnalysisRequest(BaseModel):
    clone_probability: float
    speaker_similarity: float
    is_trusted_matched: bool = False
    intent_level: str = "LOW"
    intent_score: float = 0.0
    detected_triggers: List[str] = []
    verification_status: str = "NOT_STARTED"
    replay_risk: str = "LOW"

class VerificationCheckRequest(BaseModel):
    session_id: str = "default"
    response_text: str = ""
    simulate_result: str = "" # "PASS", "FAIL", or empty

class DemoRunRequest(BaseModel):
    scenario_id: str

@app.get("/")
def read_root():
    return {
        "system": "VoiceShield AI",
        "status": "Active",
        "version": "1.0.0",
        "academic_disclaimer": SYSTEM_DISCLAIMER,
        "enrolled_speakers": len(get_all_trusted_speakers())
    }

# =======================================================
# 1. AUDIO ANALYSIS ENDPOINTS
# =======================================================

@app.post("/api/audio/analyze")
async def analyze_audio(
    file: UploadFile = File(...),
    session_id: Optional[str] = Form(None),
    input_mode: str = Form("UPLOAD"),
    fallback_transcript: Optional[str] = Form(None)
):
    """
    Complete audio pipeline:
    Audio -> Preprocessing -> Clone Detection -> Speaker Match -> Intent -> Risk
    """
    audio_bytes = await file.read()
    if len(audio_bytes) == 0:
        raise HTTPException(status_code=400, detail="Empty audio payload")

    if not session_id:
        session_id = create_session(f"VS-{datetime.datetime.now().strftime('%H%M%S')}", input_mode)

    # 1. Preprocessing
    audio_data, sr = load_audio_from_bytes(audio_bytes)
    audio_norm = normalize_audio(audio_data)
    voiced_audio, speech_ratio = apply_vad(audio_norm)
    acoustic_features = extract_acoustic_features(voiced_audio, sr)

    # 2. Clone Detection
    detector = get_clone_detector()
    detection_res = detector.predict(voiced_audio, acoustic_features, sr)
    clone_prob = detection_res["clone_probability"]
    replay_risk = detection_res["replay_risk"]

    # 3. Speaker Identification
    query_embedding = compute_speaker_embedding(voiced_audio, sr)
    speaker_match_res = match_speaker(query_embedding)
    speaker_sim = speaker_match_res["similarity"]
    likely_speaker = speaker_match_res["likely_speaker"]
    is_matched = speaker_match_res["matched"]

    # 4. Speech-to-Text & Intent Analysis
    transcript = transcribe_audio(audio_bytes, fallback_text=fallback_transcript or "")
    intent_analysis = analyze_intent(transcript)

    # 5. Multi-Signal Risk Assessment
    risk_res = calculate_risk(
        clone_prob=clone_prob,
        speaker_similarity=speaker_sim,
        is_trusted_speaker_matched=is_matched,
        intent_analysis=intent_analysis,
        verification_status="NOT_STARTED",
        replay_risk=replay_risk
    )

    # Record analysis timeline point & events
    duration_sec = round(len(audio_data) / sr, 2)
    add_analysis_point(
        session_id=session_id,
        timestamp_sec=duration_sec,
        clone_prob=clone_prob,
        speaker_similarity=speaker_sim,
        is_synthetic=1 if clone_prob >= 0.60 else 0,
        replay_risk=replay_risk,
        features=acoustic_features
    )

    if clone_prob >= 0.70:
        add_risk_event(session_id, duration_sec, "CLONE_DETECTED", f"Suspected AI-generated voice ({int(clone_prob*100)}%)", "WARNING")
    if is_matched and clone_prob >= 0.60:
        add_risk_event(session_id, duration_sec, "IMPERSONATION_RISK", f"Likely impersonation target: {likely_speaker}", "CRITICAL")
    if intent_analysis["suspicious_intent"] == "HIGH":
        add_risk_event(session_id, duration_sec, "SUSPICIOUS_INTENT", f"Phishing/financial indicators detected", "CRITICAL")

    update_session(
        session_id,
        duration_sec=duration_sec,
        likely_speaker=likely_speaker,
        speaker_similarity=speaker_sim,
        max_clone_prob=clone_prob,
        overall_risk=risk_res["risk_score"],
        risk_level=risk_res["risk_level"]
    )

    return {
        "session_id": session_id,
        "duration_sec": duration_sec,
        "clone_detection": detection_res,
        "speaker_identification": speaker_match_res,
        "intent_analysis": intent_analysis,
        "transcript": transcript,
        "risk_assessment": risk_res,
        "acoustic_features": acoustic_features,
        "academic_disclaimer": SYSTEM_DISCLAIMER
    }

@app.post("/api/audio/stream")
async def stream_audio_chunk(
    file: UploadFile = File(...),
    session_id: str = Form(...),
    timestamp_sec: float = Form(0.0)
):
    """Chunked audio streaming endpoint for live microphone monitoring."""
    audio_bytes = await file.read()
    audio_data, sr = load_audio_from_bytes(audio_bytes)
    audio_norm = normalize_audio(audio_data)
    voiced_audio, _ = apply_vad(audio_norm)
    acoustic_features = extract_acoustic_features(voiced_audio, sr)

    detector = get_clone_detector()
    detection_res = detector.predict(voiced_audio, acoustic_features, sr)
    clone_prob = detection_res["clone_probability"]

    query_embedding = compute_speaker_embedding(voiced_audio, sr)
    speaker_match_res = match_speaker(query_embedding)
    speaker_sim = speaker_match_res["similarity"]

    intent_analysis = {"suspicious_intent": "LOW", "intent_score": 0.0, "detected_triggers": []}
    risk_res = calculate_risk(
        clone_prob=clone_prob,
        speaker_similarity=speaker_sim,
        is_trusted_speaker_matched=speaker_match_res["matched"],
        intent_analysis=intent_analysis,
        replay_risk=detection_res["replay_risk"]
    )

    add_analysis_point(
        session_id=session_id,
        timestamp_sec=timestamp_sec,
        clone_prob=clone_prob,
        speaker_similarity=speaker_sim,
        is_synthetic=1 if clone_prob >= 0.60 else 0,
        replay_risk=detection_res["replay_risk"],
        features=acoustic_features
    )

    return {
        "session_id": session_id,
        "timestamp_sec": timestamp_sec,
        "clone_probability": clone_prob,
        "speaker_similarity": speaker_sim,
        "likely_speaker": speaker_match_res["likely_speaker"],
        "risk_score": risk_res["risk_score"],
        "risk_level": risk_res["risk_level"]
    }

# =======================================================
# 2. TRUSTED SPEAKERS
# =======================================================

@app.get("/api/speakers")
def list_speakers():
    return get_all_trusted_speakers()

@app.post("/api/speakers/register")
async def register_speaker(
    name: str = Form(...),
    relationship: str = Form("Trusted Contact"),
    notes: str = Form(""),
    audio: Optional[UploadFile] = File(None)
):
    if not name or len(name.strip()) == 0:
        raise HTTPException(status_code=400, detail="Speaker name is required")

    if audio:
        audio_bytes = await audio.read()
        audio_data, sr = load_audio_from_bytes(audio_bytes)
        embedding = compute_speaker_embedding(audio_data, sr)
    else:
        # Generate synthetic unique acoustic profile if audio sample was skipped
        import random
        seed = random.randint(1000, 9999)
        from backend.database.db import generate_synthetic_embedding
        embedding = generate_synthetic_embedding(seed)

    created = add_trusted_speaker(name=name.strip(), relationship=relationship.strip(), embedding=embedding, notes=notes)
    return created

@app.delete("/api/speakers/{speaker_id}")
def delete_speaker(speaker_id: str):
    success = delete_trusted_speaker(speaker_id)
    if not success:
        raise HTTPException(status_code=404, detail="Speaker profile not found")
    return {"message": "Speaker profile removed successfully", "id": speaker_id}

@app.post("/api/speakers/verify")
async def verify_speaker_match(
    speaker_id: str = Form(...),
    audio: UploadFile = File(...)
):
    spk = get_speaker_by_id(speaker_id)
    if not spk:
        raise HTTPException(status_code=404, detail="Speaker not found")

    audio_bytes = await audio.read()
    audio_data, sr = load_audio_from_bytes(audio_bytes)
    embedding = compute_speaker_embedding(audio_data, sr)
    sim = cosine_similarity(embedding, spk["embedding"])

    return {
        "speaker_id": speaker_id,
        "speaker_name": spk["name"],
        "similarity": sim,
        "is_match": sim >= 0.70,
        "status": "Likely Match" if sim >= 0.70 else "Mismatch / Unknown"
    }

# =======================================================
# 3. VERIFICATION CHALLENGE
# =======================================================

@app.post("/api/verification/challenge")
def get_challenge(session_id: Optional[str] = "default"):
    phrase = generate_challenge(session_id)
    return {
        "session_id": session_id,
        "challenge_phrase": phrase,
        "instructions": f"Please instruct the caller to speak the security phrase: '{phrase}'"
    }

@app.post("/api/verification/check")
def check_verification(req: VerificationCheckRequest):
    result = verify_response(
        session_id=req.session_id,
        response_text=req.response_text,
        simulate_result=req.simulate_result
    )
    return result

# =======================================================
# 4. RISK ASSESSMENT
# =======================================================

@app.post("/api/risk/analyze")
def analyze_risk_endpoint(req: RiskAnalysisRequest):
    res = calculate_risk(
        clone_prob=req.clone_probability,
        speaker_similarity=req.speaker_similarity,
        is_trusted_speaker_matched=req.is_trusted_matched,
        intent_analysis={
            "suspicious_intent": req.intent_level,
            "intent_score": req.intent_score,
            "detected_triggers": req.detected_triggers
        },
        verification_status=req.verification_status,
        replay_risk=req.replay_risk
    )
    return res

# =======================================================
# 5. SESSIONS & REPORTS
# =======================================================

@app.get("/api/sessions/{session_id}")
def get_session_info(session_id: str):
    sess = get_session(session_id)
    if not sess:
        raise HTTPException(status_code=404, detail="Session not found")
    return sess

@app.get("/api/sessions/{session_id}/timeline")
def get_timeline(session_id: str):
    return {
        "session_id": session_id,
        "timeline": get_session_timeline(session_id),
        "events": get_risk_events(session_id)
    }

@app.get("/api/sessions/{session_id}/report")
def get_session_report(session_id: str):
    sess = get_session(session_id)
    if not sess:
        # Fallback to demo report if not found in db
        sess = {
            "id": session_id,
            "session_code": f"VS-{session_id[-6:]}",
            "start_time": datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
            "duration_sec": 12.0,
            "input_mode": "DEMO",
            "likely_speaker": "Rahul Sharma",
            "speaker_similarity": 0.89,
            "max_clone_prob": 0.91,
            "overall_risk": 87,
            "risk_level": "CRITICAL",
            "transcript": "Hello Dad, please listen carefully. Send me the money right now.",
            "suspicious_intent": "HIGH",
            "detected_triggers": ["Emergency pretext", "Financial request"],
            "replay_risk": "LOW (Experimental)",
            "recommended_action": "Do not share financial or personal credentials."
        }

    timeline = get_session_timeline(session_id)
    events = get_risk_events(session_id)
    verif = {"result": "FAILED" if sess.get("overall_risk", 0) > 80 else "PASSED"}

    report = generate_security_report(session_id, sess, timeline, events, verif)
    save_report(session_id, report)
    return report

@app.get("/api/sessions/{session_id}/report/html", response_class=HTMLResponse)
def get_session_report_html(session_id: str):
    rep_json = get_session_report(session_id)
    html = generate_html_report(rep_json)
    return html

# =======================================================
# 6. DEMO & SIMULATION MODE
# =======================================================

@app.get("/api/demo/scenarios")
def list_demo_scenarios():
    return list(DEMO_SCENARIOS.values())

@app.post("/api/demo/run")
def run_demo_scenario(req: DemoRunRequest):
    scenario_id = req.scenario_id
    if scenario_id not in DEMO_SCENARIOS:
        raise HTTPException(status_code=404, detail="Demo scenario not found")

    scen = DEMO_SCENARIOS[scenario_id]
    session_id = create_session(f"DEMO-{scenario_id.upper()}", "SIMULATED_DEMO")

    update_session(
        session_id,
        duration_sec=9.0,
        likely_speaker=scen["metrics"]["likely_speaker"],
        speaker_similarity=scen["metrics"]["speaker_similarity"],
        max_clone_prob=scen["metrics"]["clone_probability"],
        overall_risk=scen["metrics"]["risk_score"],
        risk_level=scen["metrics"]["risk_level"]
    )

    # Populate timeline
    for pt in scen["timeline"]:
        parts = pt["time"].split(":")
        sec = float(parts[0]) * 60 + float(parts[1])
        add_analysis_point(
            session_id=session_id,
            timestamp_sec=sec,
            clone_prob=pt["clone_prob"],
            speaker_similarity=pt["speaker_sim"],
            is_synthetic=1 if pt["clone_prob"] >= 0.60 else 0,
            replay_risk=scen["metrics"]["replay_risk"]
        )

    # Populate events
    for ev in scen["events"]:
        add_risk_event(
            session_id=session_id,
            timestamp_sec=ev["timestamp_sec"],
            event_type=ev["type"],
            description=ev["description"],
            severity=ev["severity"]
        )

    audio_url = f"/demo/audio/{scen['audio_file']}"

    return {
        "session_id": session_id,
        "scenario": scen,
        "audio_url": audio_url,
        "academic_disclaimer": SYSTEM_DISCLAIMER
    }

# =======================================================
# 7. ANDROID AUTHENTICATION & USER ISOLATION
# =======================================================

@app.post("/auth/verify")
async def verify_auth_token(user: Dict[str, Any] = Depends(get_current_user)):
    return {
        "status": "authenticated",
        "user": user,
        "message": "Firebase ID token verified successfully"
    }

@app.get("/users/me")
async def get_current_user_profile(user: Dict[str, Any] = Depends(get_current_user)):
    vp = get_user_voice_profile(user["uid"])
    return {
        "user_id": user["uid"],
        "email": user.get("email", ""),
        "name": user.get("name", "User"),
        "has_voice_profile": vp is not None,
        "voice_profile_status": "ACTIVE" if vp else "NOT_REGISTERED"
    }

# =======================================================
# 8. ANDROID VOICE PROFILE MANAGEMENT
# =======================================================

@app.post("/voice-profile/create")
async def create_user_voice_profile_endpoint(
    name: str = Form("Enrolled User"),
    notes: str = Form(""),
    audio: Optional[UploadFile] = File(None),
    user: Dict[str, Any] = Depends(get_current_user)
):
    uid = user["uid"]
    if audio:
        audio_bytes = await audio.read()
        audio_data, sr = load_audio_from_bytes(audio_bytes)
        embedding = compute_speaker_embedding(audio_data, sr)
    else:
        # Fallback calibrated synthetic embedding derived from user ID seed
        import hashlib
        seed = int(hashlib.md5(uid.encode()).hexdigest()[:8], 16) % 100000
        from backend.database.db import generate_synthetic_embedding
        embedding = generate_synthetic_embedding(seed)

    saved = save_user_voice_profile(user_id=uid, name=name, embedding=embedding, notes=notes)
    return saved

@app.get("/voice-profile")
async def get_user_voice_profile_endpoint(user: Dict[str, Any] = Depends(get_current_user)):
    vp = get_user_voice_profile(user["uid"])
    if not vp:
        return {"status": "NOT_REGISTERED", "message": "No voice profile enrolled for this account"}
    return vp

@app.put("/voice-profile")
async def update_user_voice_profile_endpoint(
    name: str = Form("Enrolled User"),
    notes: str = Form(""),
    audio: Optional[UploadFile] = File(None),
    user: Dict[str, Any] = Depends(get_current_user)
):
    return await create_user_voice_profile_endpoint(name=name, notes=notes, audio=audio, user=user)

@app.delete("/voice-profile")
async def delete_user_voice_profile_endpoint(user: Dict[str, Any] = Depends(get_current_user)):
    success = delete_user_voice_profile(user["uid"])
    return {"deleted": success, "message": "Voice profile removed successfully"}

# =======================================================
# 9. ANDROID ANALYSIS & SESSIONS
# =======================================================

@app.post("/analysis/start")
async def start_analysis_session(
    mode: str = Form("LIVE_MIC"),
    user: Dict[str, Any] = Depends(get_current_user)
):
    uid = user["uid"]
    session_id = create_session(f"VS-{datetime.datetime.now().strftime('%H%M%S')}", input_mode=mode, user_id=uid)
    return {
        "session_id": session_id,
        "status": "STARTED",
        "websocket_url": f"/ws/voice-analysis/{session_id}"
    }

@app.post("/analysis/upload")
async def upload_audio_analysis(
    file: UploadFile = File(...),
    custom_pretext: Optional[str] = Form(None),
    user: Dict[str, Any] = Depends(get_current_user)
):
    uid = user["uid"]
    audio_bytes = await file.read()
    if len(audio_bytes) < 100:
        raise HTTPException(status_code=400, detail="Invalid or empty audio payload")

    # Format validation
    filename = (file.filename or "uploaded_audio.wav").lower()
    valid_exts = (".wav", ".mp3", ".m4a", ".ogg", ".webm", ".flac")
    if not any(filename.endswith(ext) for ext in valid_exts):
        raise HTTPException(status_code=400, detail=f"Unsupported format. Expected one of: {valid_exts}")

    session_id = create_session(f"UP-{datetime.datetime.now().strftime('%H%M%S')}", input_mode="FILE_UPLOAD", user_id=uid)

    audio_data, sr = load_audio_from_bytes(audio_bytes)
    audio_norm = normalize_audio(audio_data)
    voiced_audio, speech_ratio = apply_vad(audio_norm)
    features = extract_acoustic_features(voiced_audio, sr)

    detector = get_clone_detector()
    detection = detector.predict(voiced_audio, features, sr)

    # Check against user's specific voice profile if enrolled
    user_vp = get_user_voice_profile(uid)
    query_emb = compute_speaker_embedding(voiced_audio, sr)

    if user_vp:
        sim = cosine_similarity(query_emb, user_vp["embedding"])
        spk_res = {
            "matched": sim >= 0.70,
            "likely_speaker": user_vp["name"] if sim >= 0.70 else "Possible Speaker Mismatch",
            "similarity": sim,
            "speaker_id": user_vp["id"]
        }
    else:
        spk_res = match_speaker(query_emb)

    transcript = transcribe_audio(audio_bytes, fallback_text=custom_pretext or "")
    intent = analyze_intent(transcript)

    risk_res = calculate_risk(
        clone_prob=detection["clone_probability"],
        speaker_similarity=spk_res["similarity"],
        is_trusted_speaker_matched=spk_res["matched"],
        intent_analysis=intent,
        replay_risk=detection.get("replay_risk", "LOW")
    )

    duration_sec = round(len(audio_data) / sr, 2)
    add_analysis_point(
        session_id=session_id,
        timestamp_sec=duration_sec,
        clone_prob=detection["clone_probability"],
        speaker_similarity=spk_res["similarity"],
        is_synthetic=1 if detection["clone_probability"] >= 0.60 else 0,
        replay_risk=detection.get("replay_risk", "LOW"),
        features=features
    )

    update_session(
        session_id,
        duration_sec=duration_sec,
        likely_speaker=spk_res["likely_speaker"],
        speaker_similarity=spk_res["similarity"],
        max_clone_prob=detection["clone_probability"],
        overall_risk=risk_res["risk_score"],
        risk_level=risk_res["risk_level"]
    )

    timeline = get_session_timeline(session_id)
    events = get_risk_events(session_id)
    sess_dict = get_session(session_id) or {}
    sess_dict.update({
        "overall_risk": risk_res["risk_score"],
        "risk_level": risk_res["risk_level"],
        "max_clone_prob": detection["clone_probability"],
        "likely_speaker": spk_res["likely_speaker"],
        "speaker_similarity": spk_res["similarity"],
        "transcript": transcript,
        "suspicious_intent": intent["suspicious_intent"],
        "detected_triggers": intent["detected_triggers"]
    })
    report = generate_security_report(session_id, sess_dict, timeline, events, {"result": "NOT_CONDUCTED"})
    save_report(session_id, report, user_id=uid)

    return {
        "session_id": session_id,
        "duration_sec": duration_sec,
        "clone_detection": detection,
        "speaker_verification": spk_res,
        "intent_analysis": intent,
        "risk_assessment": risk_res,
        "report_id": report["report_id"],
        "academic_disclaimer": SYSTEM_DISCLAIMER
    }

@app.post("/analysis/stop")
async def stop_analysis_session(
    session_id: str = Form(...),
    user: Dict[str, Any] = Depends(get_current_user)
):
    uid = user["uid"]
    sess = get_session(session_id)
    if not sess:
        raise HTTPException(status_code=404, detail="Session not found")
    timeline = get_session_timeline(session_id)
    events = get_risk_events(session_id)
    report = generate_security_report(session_id, sess, timeline, events, {"result": "COMPLETED"})
    save_report(session_id, report, user_id=uid)
    return {
        "status": "STOPPED",
        "session_id": session_id,
        "report_id": report["report_id"],
        "report": report
    }

@app.get("/analysis/history")
async def get_user_analysis_history(user: Dict[str, Any] = Depends(get_current_user)):
    return get_user_sessions(user["uid"])

@app.get("/analysis/{session_id}")
async def get_analysis_details(
    session_id: str,
    user: Dict[str, Any] = Depends(get_current_user)
):
    sess = get_session(session_id)
    if not sess:
        raise HTTPException(status_code=404, detail="Session not found")
    if sess.get("user_id") not in (user["uid"], "default"):
        raise HTTPException(status_code=403, detail="Access denied to this session")
    return {
        "session": sess,
        "timeline": get_session_timeline(session_id),
        "events": get_risk_events(session_id),
        "report": get_report(session_id)
    }

@app.get("/report/{report_id}")
async def get_report_endpoint(
    report_id: str,
    user: Dict[str, Any] = Depends(get_current_user)
):
    rep = get_user_report(report_id, user["uid"])
    if not rep:
        raise HTTPException(status_code=404, detail="Report not found or access denied")
    return rep

@app.delete("/analysis/{session_id}")
async def delete_analysis_session_endpoint(
    session_id: str,
    user: Dict[str, Any] = Depends(get_current_user)
):
    deleted = delete_user_session(session_id, user["uid"])
    if not deleted:
        raise HTTPException(status_code=404, detail="Session not found or already deleted")
    return {"deleted": True, "session_id": session_id}

# =======================================================
# 10. DATASET & BENCHMARK ANALYTICS
# =======================================================

@app.get("/api/dataset/stats")
def get_dataset_stats_endpoint():
    from VoiceShield_Dataset.scripts.dataset_dashboard import get_dataset_summary
    return get_dataset_summary()

@app.get("/api/dataset/manifest")
def get_dataset_manifest_endpoint(
    split: Optional[str] = None,
    label: Optional[str] = None,
    language: Optional[str] = None,
    speaker_id: Optional[str] = None
):
    from VoiceShield_Dataset.scripts.dataset_dashboard import get_dataset_manifest
    return get_dataset_manifest(split=split, label=label, language=language, speaker_id=speaker_id)

@app.get("/api/dataset/dashboard", response_class=HTMLResponse)
def get_dataset_dashboard_html():
    from backend.config import DATASET_DIR
    dash_file = DATASET_DIR / "dashboard.html"
    if not dash_file.exists():
        from VoiceShield_Dataset.scripts.dataset_dashboard import generate_dashboard
        generate_dashboard()
    with open(dash_file, "r", encoding="utf-8") as f:
        return f.read()

if __name__ == "__main__":
    import uvicorn
    from backend.config import HOST, PORT
    print(f"🚀 Starting VoiceShield AI Backend on http://{HOST}:{PORT}")
    print(f"📚 API Documentation available at http://{HOST}:{PORT}/docs")
    uvicorn.run("backend.main:app", host=HOST, port=PORT, reload=True)


