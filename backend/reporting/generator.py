import json
from typing import Dict, Any, List
from backend.config import SYSTEM_DISCLAIMER

def generate_security_report(
    session_id: str,
    session_data: Dict[str, Any],
    timeline: List[Dict[str, Any]],
    risk_events: List[Dict[str, Any]],
    verification_data: Dict[str, Any]
) -> Dict[str, Any]:
    """Generate comprehensive academic voice security forensic report."""
    
    overall_risk = session_data.get("overall_risk", 0)
    risk_level = session_data.get("risk_level", "LOW")
    clone_prob = session_data.get("max_clone_prob", 0.0)
    likely_speaker = session_data.get("likely_speaker", "Unknown / No trusted speaker match")
    speaker_sim = session_data.get("speaker_similarity", 0.0)
    
    report = {
        "report_id": f"REP-{session_id.upper()}",
        "session_id": session_id,
        "session_code": session_data.get("session_code", "VS-2026-001"),
        "timestamp": session_data.get("start_time", "2026-09-12 00:00:00"),
        "audio_duration_sec": session_data.get("duration_sec", 15.0),
        "input_mode": session_data.get("input_mode", "DEMO"),
        "executive_summary": {
            "overall_risk_score": overall_risk,
            "risk_level": risk_level,
            "clone_probability": round(clone_prob, 2),
            "clone_status": "AI-Generated / Suspected Clone" if clone_prob >= 0.60 else "Likely Genuine",
            "likely_impersonated_speaker": likely_speaker,
            "speaker_similarity": round(speaker_sim, 2),
            "verification_result": verification_data.get("result", "NOT_CONDUCTED"),
            "recommended_action": session_data.get("recommended_action", "Proceed with normal caution.")
        },
        "acoustic_forensics": {
            "detector_model": "AcousticArtifactDetector-ASVspoofBenchmark",
            "replay_attack_risk": session_data.get("replay_risk", "LOW (Experimental)"),
            "evaluation_sampling_rate": "16,000 Hz",
            "vad_speech_ratio": 0.88
        },
        "intent_analysis": {
            "suspicious_intent": session_data.get("suspicious_intent", "LOW"),
            "detected_triggers": session_data.get("detected_triggers", []),
            "transcript_excerpt": session_data.get("transcript", "N/A")
        },
        "timeline_events": risk_events,
        "timeline_data_points": timeline,
        "compliance_and_disclaimer": SYSTEM_DISCLAIMER
    }
    
    return report

def generate_html_report(report: Dict[str, Any]) -> str:
    """Generate professional printable HTML document for forensic documentation."""
    summary = report["executive_summary"]
    risk_color = "#ef4444" if summary["risk_level"] in ["CRITICAL", "HIGH"] else "#3b82f6"
    
    events_html = "".join([
        f"<li style='margin-bottom: 6px;'><strong>{e.get('timestamp_sec', 0):04.1f}s</strong> - [{e.get('severity', 'INFO')}] {e.get('description', '')}</li>"
        for e in report.get("timeline_events", [])
    ])
    if not events_html:
        events_html = "<li>No critical security escalations logged during session.</li>"

    html = f"""<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>VoiceShield AI - Forensic Security Report ({report['session_code']})</title>
    <style>
        body {{ font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; margin: 40px; color: #0f172a; background: #fff; line-height: 1.5; }}
        .header {{ border-bottom: 3px solid #2563eb; padding-bottom: 16px; margin-bottom: 24px; display: flex; justify-content: space-between; align-items: center; }}
        .badge {{ display: inline-block; padding: 4px 12px; border-radius: 9999px; font-weight: bold; color: #fff; background: {risk_color}; }}
        .card {{ background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 8px; padding: 18px; margin-bottom: 20px; }}
        .grid {{ display: grid; grid-template-columns: repeat(2, 1fr); gap: 16px; }}
        h1, h2, h3 {{ margin-top: 0; }}
        table {{ width: 100%; border-collapse: collapse; margin-top: 10px; }}
        th, td {{ border: 1px solid #cbd5e1; padding: 8px 12px; text-align: left; }}
        th {{ background: #f1f5f9; }}
        .footer {{ margin-top: 40px; padding-top: 12px; border-top: 1px solid #cbd5e1; font-size: 12px; color: #64748b; text-align: center; }}
        @media print {{ body {{ margin: 0; }} button {{ display: none; }} }}
    </style>
</head>
<body>
    <div class="header">
        <div>
            <h1>VoiceShield AI Forensic Security Report</h1>
            <p style="color: #64748b; margin: 0;">Session ID: <strong>{report['session_code']}</strong> | Date: {report['timestamp']}</p>
        </div>
        <div>
            <span class="badge">{summary['risk_level']} RISK ({summary['overall_risk_score']}/100)</span>
        </div>
    </div>

    <div class="card">
        <h2>Executive Summary</h2>
        <div class="grid">
            <div>
                <p><strong>Clone Probability:</strong> {int(summary['clone_probability'] * 100)}% ({summary['clone_status']})</p>
                <p><strong>Likely Impersonated Speaker:</strong> {summary['likely_impersonated_speaker']}</p>
                <p><strong>Speaker Similarity:</strong> {int(summary['speaker_similarity'] * 100)}%</p>
            </div>
            <div>
                <p><strong>Verification Status:</strong> {summary['verification_result']}</p>
                <p><strong>Replay Attack Risk:</strong> {report['acoustic_forensics']['replay_attack_risk']}</p>
                <p><strong>Input Mode:</strong> {report['input_mode']}</p>
            </div>
        </div>
        <p style="margin-top: 12px; padding: 10px; background: #eff6ff; border-left: 4px solid #2563eb;">
            <strong>Recommended Action:</strong> {summary['recommended_action']}
        </p>
    </div>

    <div class="card">
        <h3>Transcript & Intent Analysis</h3>
        <p><strong>Transcript Excerpt:</strong> "{report['intent_analysis']['transcript_excerpt']}"</p>
        <p><strong>Suspicious Intent Rating:</strong> {report['intent_analysis']['suspicious_intent']}</p>
        <p><strong>Detected Risk Indicators:</strong> {', '.join(report['intent_analysis']['detected_triggers']) if report['intent_analysis']['detected_triggers'] else 'None detected'}</p>
    </div>

    <div class="card">
        <h3>Forensic Timeline Events</h3>
        <ul>{events_html}</ul>
    </div>

    <div class="footer">
        <p>{report['compliance_and_disclaimer']}</p>
        <p>Generated by VoiceShield AI Academic Evaluation Suite</p>
    </div>
</body>
</html>"""
    return html
