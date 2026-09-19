# VoiceShield AI — Real-Time Voice Clone Detection & Impersonation Defense System

VoiceShield AI is an end-to-end cybersecurity solution and native Android application designed to detect potential AI-generated/cloned speech and speaker impersonation attacks in real-time.

---

## 🛡️ Architecture & Project Structure

```
VoiceShield AI/
├── android/                         # Native Android Application (Kotlin, Jetpack Compose, Material 3)
│   ├── app/src/main/java/com/voiceshield/ai/
│   │   ├── domain/                  # Clean Architecture domain models & repository interfaces
│   │   ├── data/                    # Room DB, Retrofit API client, WebSocket client, Repositories
│   │   ├── services/                # AudioRecordManager (16kHz PCM), DemoScenarioProvider, ReportExporter
│   │   ├── ui/                      # Jetpack Compose UI Screens, ViewModels & Cyber Design System
│   │   │   ├── auth/                # Login, Register, Forgot Password with Dev Sign-In
│   │   │   ├── home/                # Active Defense Dashboard, Security Posture Metrics
│   │   │   ├── analysis/            # Live Audio Waveform, Risk Gauge, Timeline & High-Risk Alert Dialog
│   │   │   ├── protectedcall/       # Simulated Incoming/Active Call Monitor with Live Anti-Impersonation HUD
│   │   │   ├── report/              # Forensic Security Report with TXT Export & Sharing
│   │   │   ├── history/             # User-Isolated Session History & Audit Detail
│   │   │   ├── profile/             # Guided 10-Second Voice Biometric Enrollment
│   │   │   └── demo/                # Isolated 7-Scenario Quick Demo Lab
│   │   └── navigation/              # Compose Navigation Graph & Bottom Navigation Bar
│   └── build.gradle.kts             # Target SDK 34, Min SDK 26, Compose BOM 2024.02.00
│
├── backend/                         # FastAPI Python Real-Time Audio Server
│   ├── main.py                      # FastAPI App Entrypoint & REST API Endpoints
│   ├── websocket/voice_socket.py    # Real-Time WebSocket (/ws/voice-analysis/{session_id})
│   ├── clone_detection/detector.py  # VoiceCloneDetector, DemoModelProvider, Spectral Classifier
│   ├── auth/firebase_auth.py        # Dev/Firebase Bearer Token Authenticator
│   ├── database/db.py               # User-Isolated SQLite/Firestore Session & Report Persistence
│   └── config.py                    # Environment & Risk Threshold Configuration
│
├── VoiceShield_Dataset/             # Multi-Speaker Audio Dataset & Manifest
│   ├── metadata/speakers.csv        # Speaker Registry & Enrolled Audio Manifest
│   └── scripts/dataset_dashboard.py # Dataset Summary & Manifest Generators
│
├── frontend/                        # Web Surveillance & Forensic Audit Dashboard (Vite + React)
│   ├── src/App.jsx                  # 6 Tab Dashboard including Dataset & Benchmarks
│   └── dist/                        # Production Static Build
│
└── tests/                           # Python Pytest Verification Suite
    └── test_voiceshield.py          # 12-Point Unit & Integration Tests (100% Pass)
```

---

## ⚡ Key Features

### 1. Real-Time Audio Analysis & Streaming
- **16 kHz 16-Bit Mono PCM Streaming**: Captures ambient mic audio or call stream and transmits audio chunks via WebSocket (`/ws/voice-analysis/{session_id}`).
- **Real-Time Risk Gauge**: Live score (0–100) with dynamic color coding:
  - 🟢 **Safe / Authentic** ($\le 30$)
  - 🟡 **Suspicious / Moderate Risk** ($31 - 69$)
  - 🔴 **High Risk / Impersonation** ($\ge 70$)
- **Live Canvas Waveform & Risk Timeline**: Continuous visualizer updating RMS amplitudes and scrolling risk history.

### 2. Protected Call Monitor & Advisory Guidance
- Simulated incoming telecom call with live anti-impersonation HUD.
- Immediate advisory alerts when caller voice pattern deviates from enrolled profile.
- **Interactive Voice Challenge**: Prompts analysts to ask tongue-twister phrases to disrupt neural vocoder synthesis.

### 3. User-Isolated Data Architecture
- Every voice profile, session history record, and forensic report is strictly bound to the authenticated `user_id`.
- User A can never inspect or alter User B's voice embeddings, reports, or surveillance logs.

### 4. Quick Demo Sandbox Mode
- **7 Pre-Packaged Scenarios**: Safe Authentic Voice, Moderate Garbled Audio, High Risk ElevenLabs Clone, Tortoise-TTS Clone, Speaker Impersonation Attempt, Dynamic Shift (Authentic $\to$ Cloned mid-call), Low SNR Audio.
- **Clear Sandbox Marker**: Displays `"DEMO MODE — SAMPLE DATA"`.
- Isolated from real history to prevent data pollution.

### 5. Forensic Security Reports
- Multi-dimensional breakdown: Clone Probability, Speaker Biometric Similarity, Acoustic Model Attribution (e.g. ElevenLabs, Tortoise, VITS), NLP Intent Classification (Urgency, Financial, Credentials).
- Exportable to formatted TXT reports for security auditing.

---

## 🚀 Running the Server & Applications

### Prerequisites
- Python 3.10+
- Node.js 18+ (for Web Frontend)
- Android Studio / Android SDK (for Kotlin Android Build)

### 1. Start the FastAPI Backend
```bash
# Navigate to project root
pip install -r requirements.txt

# Launch FastAPI server on localhost:8000
uvicorn backend.main:app --host 0.0.0.0 --port 8000 --reload
```
Swagger API docs will be available at: `http://localhost:8000/docs`.

### 2. Run Backend Pytest Verification
```bash
pytest tests/test_voiceshield.py -v
```

### 3. Build & Launch Web Dashboard
```bash
cd frontend
npm install
npm run dev      # Launch dev server on http://localhost:5173
npm run build    # Generate production build in dist/
```

### 4. Build & Run Android Application
1. Open the `android/` directory in Android Studio.
2. Ensure Android SDK 34 is installed.
3. Run on an Android Emulator (API 26+) or physical device.
4. Note: On Android Emulator, `10.0.2.2:8000` is pre-configured to connect to the host FastAPI backend.

---

## ⚖️ Ethical & Probabilistic Disclaimer

> [!IMPORTANT]
> **Probabilistic Classification Notice**
> VoiceShield AI utilizes machine learning models and signal processing heuristics to estimate the likelihood of synthetic speech and speaker mismatch. AI clone detection is **probabilistic** and must **never** be presented as 100% infallible legal proof. Analysts should always verify caller identities using secondary out-of-band communication channels.
