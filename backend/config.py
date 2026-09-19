import os
from pathlib import Path

try:
    from dotenv import load_dotenv
    # Paths
    BASE_DIR = Path(__file__).resolve().parent.parent
    load_dotenv(BASE_DIR / ".env")
except ImportError:
    BASE_DIR = Path(__file__).resolve().parent.parent

BACKEND_DIR = BASE_DIR / "backend"
DATA_DIR = BASE_DIR / "data"
DATABASE_PATH = DATA_DIR / "voiceshield.db"
DEMO_DIR = BASE_DIR / "demo"
REPORTS_DIR = BASE_DIR / "reports"
DATASET_DIR = BASE_DIR / "VoiceShield_Dataset"

# Ensure runtime directories exist
DATA_DIR.mkdir(parents=True, exist_ok=True)
REPORTS_DIR.mkdir(parents=True, exist_ok=True)
DEMO_DIR.mkdir(parents=True, exist_ok=True)

# Settings
ELEVENLABS_API_KEY = os.getenv("ELEVENLABS_API_KEY", "").strip()
HOST = os.getenv("HOST", "127.0.0.1")
PORT = int(os.getenv("PORT", "8000"))
FIREBASE_PROJECT_ID = os.getenv("FIREBASE_PROJECT_ID", "voiceshield-ai")
DEV_AUTH_ENABLED = os.getenv("DEV_AUTH_ENABLED", "true").lower() in ("true", "1", "yes")

# Detection & Risk Thresholds
# Configurable initial defaults: LOW: 0.00-0.30, SUSPICIOUS: 0.31-0.60, HIGH: 0.61-1.00
RISK_THRESHOLD_LOW = 0.30
RISK_THRESHOLD_SUSPICIOUS = 0.60

CLONE_PROB_SUSPICIOUS_THRESHOLD = 0.50
CLONE_PROB_HIGH_THRESHOLD = 0.75
SPEAKER_MATCH_HIGH_THRESHOLD = 0.75
SPEAKER_MATCH_MED_THRESHOLD = 0.50

# Academic note
SYSTEM_DISCLAIMER = (
    "VoiceShield AI provides probabilistic security analysis and is intended as an academic prototype. "
    "Predictions are probabilistic indications and must not be treated as 100% conclusive proof."
)

