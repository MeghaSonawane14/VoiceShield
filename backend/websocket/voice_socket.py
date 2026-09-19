"""
VoiceShield AI - Real-Time Voice Analysis WebSocket Endpoint
============================================================
Endpoint: /ws/voice-analysis/{session_id}
Receives continuous live audio chunks from the Android client or Web client.
Processes VAD, acoustic feature extraction, clone detection, speaker matching,
and risk evaluation, streaming continuous JSON responses back to the client.
"""

import io
import time
import json
import datetime
import numpy as np
from typing import Optional, Dict, Any
from fastapi import APIRouter, WebSocket, WebSocketDisconnect, Query

from backend.audio_processing.preprocessor import (
    load_audio_from_bytes, normalize_audio, apply_vad, extract_acoustic_features
)
from backend.clone_detection.detector import get_clone_detector
from backend.speaker_recognition.recognizer import compute_speaker_embedding, match_speaker
from backend.risk_engine.evaluator import calculate_risk
from backend.database.db import (
    add_analysis_point, add_risk_event, update_session, get_session, create_session
)
from backend.config import RISK_THRESHOLD_LOW, RISK_THRESHOLD_SUSPICIOUS

router = APIRouter()

@router.websocket("/ws/voice-analysis/{session_id}")
async def voice_analysis_websocket(
    websocket: WebSocket,
    session_id: str,
    token: Optional[str] = Query(None),
    provider: str = Query("calibrated")
):
    await websocket.accept()
    
    # Initialize or load session
    sess = get_session(session_id)
    if not sess:
        create_session(f"WS-{session_id[:8]}", input_mode="WEBSOCKET_STREAM")

    detector = get_clone_detector(provider)
    audio_buffer = bytearray()
    start_time = time.time()
    chunk_counter = 0

    try:
        while True:
            # Receive either binary audio data or text/JSON
            message = await websocket.receive()

            audio_bytes = None
            if "bytes" in message and message["bytes"]:
                audio_bytes = message["bytes"]
            elif "text" in message and message["text"]:
                try:
                    payload = json.loads(message["text"])
                    if payload.get("action") == "stop":
                        await websocket.send_json({
                            "type": "SESSION_STOPPED",
                            "session_id": session_id,
                            "timestamp": datetime.datetime.now().isoformat()
                        })
                        break
                    elif payload.get("action") == "ping":
                        await websocket.send_json({"type": "pong", "time": time.time()})
                        continue
                    elif "audio_b64" in payload:
                        import base64
                        audio_bytes = base64.b64decode(payload["audio_b64"])
                except Exception:
                    continue

            if not audio_bytes or len(audio_bytes) < 320:
                continue

            audio_buffer.extend(audio_bytes)

            # Process when buffer has at least ~0.5 seconds of 16kHz 16-bit PCM (16,000 samples * 2 bytes = 32,000 bytes/sec -> 16,000 bytes per 0.5s)
            # or minimum 1024 bytes for quick initial feedback
            if len(audio_buffer) >= 8000:
                chunk_counter += 1
                curr_elapsed = round(time.time() - start_time, 2)
                
                # Convert raw PCM bytes to float numpy array
                try:
                    # Treat as 16-bit PCM 16kHz Mono
                    raw_pcm = bytes(audio_buffer[:16000])
                    # Keep remaining bytes for continuity
                    audio_buffer = audio_buffer[8000:]

                    pcm_int16 = np.frombuffer(raw_pcm, dtype=np.int16)
                    float_audio = pcm_int16.astype(np.float32) / 32768.0
                    sr = 16000

                    # Preprocessing
                    norm_audio = normalize_audio(float_audio)
                    voiced_audio, speech_ratio = apply_vad(norm_audio)
                    
                    if len(voiced_audio) < 256:
                        voiced_audio = norm_audio

                    features = extract_acoustic_features(voiced_audio, sr)

                    # 1. AI Clone Detection
                    detection = detector.predict(voiced_audio, features, sr)
                    clone_prob = float(detection["clone_probability"])
                    clone_confidence = float(detection.get("confidence", 0.85))

                    # 2. Speaker Verification
                    emb = compute_speaker_embedding(voiced_audio, sr)
                    spk_res = match_speaker(emb)
                    speaker_sim = float(spk_res.get("similarity", 0.50))
                    likely_speaker = spk_res.get("likely_speaker", "Unknown")
                    is_speaker_matched = spk_res.get("matched", False)

                    # 3. Multi-Signal Risk Assessment
                    # Compound risk combining clone probability and speaker mismatch
                    # Risk score scaled 0.00 to 1.00 for Android WebSocket specification
                    mismatch_weight = 0.35 if not is_speaker_matched else -0.15
                    raw_risk = (clone_prob * 0.65) + max(0.0, (1.0 - speaker_sim) * 0.35)
                    raw_risk = float(np.clip(raw_risk, 0.05, 0.98))

                    risk_reasons = []
                    if clone_prob >= 0.65:
                        risk_reasons.append("Acoustic artifact: High spectral flatness characteristic of neural vocoders")
                    if not is_speaker_matched and clone_prob > 0.40:
                        risk_reasons.append(f"Speaker mismatch: Audio diverges from enrolled profile ({int(speaker_sim*100)}% match)")
                    if detection.get("replay_risk") in ("HIGH", "MEDIUM"):
                        risk_reasons.append("Replay risk: Elevated channel reverberation / acoustic re-recording signature")

                    if raw_risk >= RISK_THRESHOLD_SUSPICIOUS:
                        risk_level = "HIGH"
                        voice_status = "POTENTIAL_CLONE"
                    elif raw_risk >= RISK_THRESHOLD_LOW:
                        risk_level = "SUSPICIOUS"
                        voice_status = "SUSPICIOUS"
                    else:
                        risk_level = "LOW"
                        voice_status = "LIKELY_AUTHENTIC"

                    # Add database analysis point
                    add_analysis_point(
                        session_id=session_id,
                        timestamp_sec=curr_elapsed,
                        clone_prob=clone_prob,
                        speaker_similarity=speaker_sim,
                        is_synthetic=1 if clone_prob >= 0.60 else 0,
                        replay_risk=detection.get("replay_risk", "LOW"),
                        features=features
                    )

                    # Update overall session stats
                    update_session(
                        session_id,
                        duration_sec=curr_elapsed,
                        likely_speaker=likely_speaker,
                        speaker_similarity=speaker_sim,
                        max_clone_prob=clone_prob,
                        overall_risk=int(raw_risk * 100),
                        risk_level=risk_level
                    )

                    # Exact JSON payload matching Specification Section 11 & 18
                    response_payload = {
                        "session_id": session_id,
                        "timestamp": datetime.datetime.now().strftime("%H:%M:%S"),
                        "timestamp_sec": curr_elapsed,
                        "clone_probability": round(clone_prob, 2),
                        "speaker_similarity": round(speaker_sim, 2),
                        "risk_score": round(raw_risk, 2),
                        "risk_score_pct": int(raw_risk * 100),
                        "risk_level": risk_level,
                        "voice_status": voice_status,
                        "confidence": round(clone_confidence, 2),
                        "likely_speaker": likely_speaker,
                        "is_speaker_matched": is_speaker_matched,
                        "detection_status": "ACTIVE",
                        "risk_reasons": risk_reasons,
                        "model_name": detection.get("model_name", "AcousticArtifactDetector"),
                        "is_demo_model": detection.get("is_demo_model", False)
                    }

                    await websocket.send_json(response_payload)

                except Exception as ex:
                    # In case of malformed chunk, send diagnostic event rather than dropping connection
                    await websocket.send_json({
                        "session_id": session_id,
                        "timestamp": datetime.datetime.now().strftime("%H:%M:%S"),
                        "risk_level": "SUSPICIOUS",
                        "voice_status": "GARBLED_AUDIO",
                        "error": str(ex)
                    })

    except WebSocketDisconnect:
        pass
    except Exception:
        pass
    finally:
        try:
            await websocket.close()
        except Exception:
            pass
