import sqlite3
import json
import uuid
import datetime
from typing import List, Dict, Any, Optional
import numpy as np
from backend.config import DATABASE_PATH

def get_connection():
    conn = sqlite3.connect(str(DATABASE_PATH))
    conn.row_factory = sqlite3.Row
    return conn

def init_db():
    conn = get_connection()
    cursor = conn.cursor()

    # Trusted Speakers / Voice Profiles
    cursor.execute("""
    CREATE TABLE IF NOT EXISTS trusted_speakers (
        id TEXT PRIMARY KEY,
        user_id TEXT DEFAULT 'default',
        name TEXT NOT NULL,
        relationship TEXT DEFAULT 'Trusted Contact',
        registration_date TEXT NOT NULL,
        embedding_json TEXT NOT NULL,
        sample_path TEXT,
        notes TEXT
    )
    """)

    # Sessions
    cursor.execute("""
    CREATE TABLE IF NOT EXISTS sessions (
        id TEXT PRIMARY KEY,
        user_id TEXT DEFAULT 'default',
        session_code TEXT NOT NULL,
        start_time TEXT NOT NULL,
        duration_sec REAL DEFAULT 0.0,
        input_mode TEXT DEFAULT 'DEMO',
        likely_speaker TEXT,
        speaker_similarity REAL DEFAULT 0.0,
        max_clone_prob REAL DEFAULT 0.0,
        overall_risk INTEGER DEFAULT 0,
        risk_level TEXT DEFAULT 'LOW',
        status TEXT DEFAULT 'COMPLETED'
    )
    """)

    # Audio Analysis Timeline
    cursor.execute("""
    CREATE TABLE IF NOT EXISTS audio_analysis (
        id TEXT PRIMARY KEY,
        session_id TEXT NOT NULL,
        timestamp_sec REAL NOT NULL,
        clone_prob REAL NOT NULL,
        speaker_similarity REAL NOT NULL,
        is_synthetic INTEGER NOT NULL,
        replay_risk TEXT DEFAULT 'LOW',
        acoustic_features_json TEXT,
        FOREIGN KEY (session_id) REFERENCES sessions(id)
    )
    """)

    # Risk / Security Events
    cursor.execute("""
    CREATE TABLE IF NOT EXISTS risk_events (
        id TEXT PRIMARY KEY,
        session_id TEXT NOT NULL,
        timestamp_sec REAL NOT NULL,
        event_type TEXT NOT NULL,
        description TEXT NOT NULL,
        severity TEXT NOT NULL,
        FOREIGN KEY (session_id) REFERENCES sessions(id)
    )
    """)

    # Verification Events
    cursor.execute("""
    CREATE TABLE IF NOT EXISTS verification_events (
        id TEXT PRIMARY KEY,
        session_id TEXT,
        challenge_phrase TEXT NOT NULL,
        response_text TEXT,
        result TEXT NOT NULL,
        timestamp TEXT NOT NULL
    )
    """)

    # Reports
    cursor.execute("""
    CREATE TABLE IF NOT EXISTS reports (
        id TEXT PRIMARY KEY,
        user_id TEXT DEFAULT 'default',
        session_id TEXT UNIQUE NOT NULL,
        created_at TEXT NOT NULL,
        report_json TEXT NOT NULL,
        FOREIGN KEY (session_id) REFERENCES sessions(id)
    )
    """)

    # Migration check: Ensure user_id column exists if table existed previously
    for table in ["trusted_speakers", "sessions", "reports"]:
        cursor.execute(f"PRAGMA table_info({table})")
        columns = [col[1] for col in cursor.fetchall()]
        if "user_id" not in columns:
            try:
                cursor.execute(f"ALTER TABLE {table} ADD COLUMN user_id TEXT DEFAULT 'default'")
            except Exception:
                pass

    conn.commit()

    # Pre-seed initial trusted speakers if empty
    cursor.execute("SELECT COUNT(*) FROM trusted_speakers")
    count = cursor.fetchone()[0]
    if count == 0:
        seed_trusted_speakers(cursor)
        conn.commit()

    conn.close()

def generate_synthetic_embedding(seed: int, dim: int = 128) -> List[float]:
    """Generate normalized acoustic speaker embedding vector."""
    rng = np.random.RandomState(seed)
    vec = rng.randn(dim).astype(np.float32)
    norm = np.linalg.norm(vec)
    if norm > 0:
        vec = vec / norm
    return [round(float(x), 5) for x in vec]

def seed_trusted_speakers(cursor):
    now = datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    # 1. Try loading from VoiceShield_Dataset/metadata/speakers.csv if present
    from backend.config import DATASET_DIR
    speakers_csv = DATASET_DIR / "metadata" / "speakers.csv"
    seeded_dataset = False
    if speakers_csv.exists():
        import csv
        try:
            with open(speakers_csv, "r", encoding="utf-8") as f:
                reader = list(csv.DictReader(f))
                for row in reader:
                    spk_id = row.get("speaker_id", "")
                    spk_name = row.get("speaker_name", spk_id)
                    rel = "Enrolled Trusted Voice"
                    notes = f"Languages: {row.get('language')}, Gender: {row.get('gender')}, Samples: {row.get('num_samples')}"
                    
                    # Load actual centroid embedding if exists
                    centroid_file = DATASET_DIR / row.get("centroid_path", "")
                    if centroid_file.exists():
                        centroid = np.load(str(centroid_file)).tolist()
                    else:
                        centroid = generate_synthetic_embedding(hash(spk_id) % 10000)

                    cursor.execute("""
                    INSERT INTO trusted_speakers (id, user_id, name, relationship, registration_date, embedding_json, notes)
                    VALUES (?, 'default', ?, ?, ?, ?, ?)
                    """, (spk_id, spk_name, rel, now, json.dumps(centroid), notes))
                seeded_dataset = True
        except Exception:
            seeded_dataset = False

    if not seeded_dataset:
        initial_profiles = [
            {
                "id": "spk-001",
                "name": "Rahul Sharma",
                "relationship": "Son / Family",
                "registration_date": now,
                "seed": 42,
                "notes": "Consented voice sample enrolled. Acoustic profile: baritone, fundamental freq ~128 Hz."
            },
            {
                "id": "spk-002",
                "name": "Dr. Ananya Iyer",
                "relationship": "Trusted Colleague",
                "registration_date": now,
                "seed": 108,
                "notes": "Consented voice sample enrolled. Acoustic profile: mezzo-soprano."
            },
            {
                "id": "spk-003",
                "name": "Dr. Vikram Sen",
                "relationship": "Research Advisor / Faculty",
                "registration_date": now,
                "seed": 256,
                "notes": "Consented faculty voice sample enrolled for academic demonstration."
            }
        ]

        for p in initial_profiles:
            emb = generate_synthetic_embedding(p["seed"])
            cursor.execute("""
            INSERT INTO trusted_speakers (id, user_id, name, relationship, registration_date, embedding_json, notes)
            VALUES (?, 'default', ?, ?, ?, ?, ?)
            """, (p["id"], p["name"], p["relationship"], p["registration_date"], json.dumps(emb), p["notes"]))

# Database operations
def get_all_trusted_speakers() -> List[Dict[str, Any]]:
    conn = get_connection()
    cursor = conn.cursor()
    cursor.execute("SELECT * FROM trusted_speakers ORDER BY registration_date DESC")
    rows = cursor.fetchall()
    speakers = []
    for r in rows:
        emb = json.loads(r["embedding_json"])
        speakers.append({
            "id": r["id"],
            "name": r["name"],
            "relationship": r["relationship"],
            "registration_date": r["registration_date"],
            "notes": r["notes"],
            "embedding_preview": emb[:8],  # First 8 dimensions for display
            "embedding_dim": len(emb),
            "sample_path": r["sample_path"]
        })
    conn.close()
    return speakers

def get_speaker_by_id(speaker_id: str) -> Optional[Dict[str, Any]]:
    conn = get_connection()
    cursor = conn.cursor()
    cursor.execute("SELECT * FROM trusted_speakers WHERE id = ?", (speaker_id,))
    r = cursor.fetchone()
    conn.close()
    if not r:
        return None
    return {
        "id": r["id"],
        "name": r["name"],
        "relationship": r["relationship"],
        "registration_date": r["registration_date"],
        "notes": r["notes"],
        "embedding": json.loads(r["embedding_json"]),
        "sample_path": r["sample_path"]
    }

def add_trusted_speaker(name: str, relationship: str, embedding: List[float], notes: str = "") -> Dict[str, Any]:
    conn = get_connection()
    cursor = conn.cursor()
    speaker_id = f"spk-{uuid.uuid4().hex[:6]}"
    now = datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    cursor.execute("""
    INSERT INTO trusted_speakers (id, name, relationship, registration_date, embedding_json, notes)
    VALUES (?, ?, ?, ?, ?, ?)
    """, (speaker_id, name, relationship, now, json.dumps(embedding), notes))
    conn.commit()
    conn.close()
    return {
        "id": speaker_id,
        "name": name,
        "relationship": relationship,
        "registration_date": now,
        "embedding_dim": len(embedding),
        "embedding_preview": embedding[:8],
        "notes": notes
    }

def delete_trusted_speaker(speaker_id: str) -> bool:
    conn = get_connection()
    cursor = conn.cursor()
    cursor.execute("DELETE FROM trusted_speakers WHERE id = ?", (speaker_id,))
    deleted = cursor.rowcount > 0
    conn.commit()
    conn.close()
    return deleted

def create_session(session_code: str, input_mode: str, user_id: str = "default") -> str:
    conn = get_connection()
    cursor = conn.cursor()
    session_id = f"sess-{uuid.uuid4().hex[:8]}"
    now = datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    cursor.execute("""
    INSERT INTO sessions (id, user_id, session_code, start_time, input_mode, status)
    VALUES (?, ?, ?, ?, ?, 'ACTIVE')
    """, (session_id, user_id, session_code, now, input_mode))
    conn.commit()
    conn.close()
    return session_id

def update_session(session_id: str, **kwargs):
    conn = get_connection()
    cursor = conn.cursor()
    updates = []
    values = []
    for k, v in kwargs.items():
        updates.append(f"{k} = ?")
        values.append(v)
    values.append(session_id)
    sql = f"UPDATE sessions SET {', '.join(updates)} WHERE id = ?"
    cursor.execute(sql, tuple(values))
    conn.commit()
    conn.close()

def get_session(session_id: str) -> Optional[Dict[str, Any]]:
    conn = get_connection()
    cursor = conn.cursor()
    cursor.execute("SELECT * FROM sessions WHERE id = ?", (session_id,))
    row = cursor.fetchone()
    conn.close()
    if not row:
        return None
    return dict(row)

def get_user_sessions(user_id: str) -> List[Dict[str, Any]]:
    """Retrieve all analysis sessions belonging strictly to user_id."""
    conn = get_connection()
    cursor = conn.cursor()
    cursor.execute("SELECT * FROM sessions WHERE user_id = ? ORDER BY start_time DESC", (user_id,))
    rows = cursor.fetchall()
    conn.close()
    return [dict(r) for r in rows]

def delete_user_session(session_id: str, user_id: str) -> bool:
    """Delete session and cascaded data belonging strictly to user_id."""
    conn = get_connection()
    cursor = conn.cursor()
    cursor.execute("DELETE FROM reports WHERE session_id = ? AND user_id = ?", (session_id, user_id))
    cursor.execute("DELETE FROM audio_analysis WHERE session_id = ?", (session_id,))
    cursor.execute("DELETE FROM risk_events WHERE session_id = ?", (session_id,))
    cursor.execute("DELETE FROM sessions WHERE id = ? AND user_id = ?", (session_id, user_id))
    deleted = cursor.rowcount > 0
    conn.commit()
    conn.close()
    return deleted

def get_user_voice_profile(user_id: str) -> Optional[Dict[str, Any]]:
    """Retrieve the enrolled voice profile for a specific user."""
    conn = get_connection()
    cursor = conn.cursor()
    cursor.execute("SELECT * FROM trusted_speakers WHERE user_id = ? ORDER BY registration_date DESC LIMIT 1", (user_id,))
    row = cursor.fetchone()
    conn.close()
    if not row:
        return None
    emb = json.loads(row["embedding_json"])
    return {
        "id": row["id"],
        "user_id": row["user_id"],
        "name": row["name"],
        "relationship": row["relationship"],
        "registration_date": row["registration_date"],
        "notes": row["notes"],
        "status": "ACTIVE",
        "embedding": emb,
        "embedding_preview": emb[:8],
        "embedding_dim": len(emb)
    }

def save_user_voice_profile(user_id: str, name: str, embedding: List[float], relationship: str = "Self / Enrolled Voice", notes: str = "") -> Dict[str, Any]:
    """Create or update user's isolated voice profile."""
    conn = get_connection()
    cursor = conn.cursor()
    # Check if profile already exists for user
    cursor.execute("SELECT id FROM trusted_speakers WHERE user_id = ?", (user_id,))
    existing = cursor.fetchone()
    now = datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S")

    if existing:
        spk_id = existing["id"]
        cursor.execute("""
        UPDATE trusted_speakers SET name = ?, relationship = ?, registration_date = ?, embedding_json = ?, notes = ?
        WHERE id = ? AND user_id = ?
        """, (name, relationship, now, json.dumps(embedding), notes, spk_id, user_id))
    else:
        spk_id = f"vprof-{uuid.uuid4().hex[:8]}"
        cursor.execute("""
        INSERT INTO trusted_speakers (id, user_id, name, relationship, registration_date, embedding_json, notes)
        VALUES (?, ?, ?, ?, ?, ?, ?)
        """, (spk_id, user_id, name, relationship, now, json.dumps(embedding), notes))

    conn.commit()
    conn.close()
    return {
        "id": spk_id,
        "user_id": user_id,
        "name": name,
        "status": "ACTIVE",
        "registration_date": now,
        "embedding_dim": len(embedding)
    }

def delete_user_voice_profile(user_id: str) -> bool:
    """Delete voice profile belonging strictly to user_id."""
    conn = get_connection()
    cursor = conn.cursor()
    cursor.execute("DELETE FROM trusted_speakers WHERE user_id = ?", (user_id,))
    deleted = cursor.rowcount > 0
    conn.commit()
    conn.close()
    return deleted

def add_analysis_point(session_id: str, timestamp_sec: float, clone_prob: float, 
                       speaker_similarity: float, is_synthetic: int, replay_risk: str = "LOW", 
                       features: Optional[Dict[str, Any]] = None):
    conn = get_connection()
    cursor = conn.cursor()
    point_id = f"anl-{uuid.uuid4().hex[:8]}"
    features_json = json.dumps(features or {})
    cursor.execute("""
    INSERT INTO audio_analysis (id, session_id, timestamp_sec, clone_prob, speaker_similarity, is_synthetic, replay_risk, acoustic_features_json)
    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
    """, (point_id, session_id, timestamp_sec, clone_prob, speaker_similarity, is_synthetic, replay_risk, features_json))
    conn.commit()
    conn.close()

def get_session_timeline(session_id: str) -> List[Dict[str, Any]]:
    conn = get_connection()
    cursor = conn.cursor()
    cursor.execute("SELECT * FROM audio_analysis WHERE session_id = ? ORDER BY timestamp_sec ASC", (session_id,))
    rows = cursor.fetchall()
    conn.close()
    timeline = []
    for r in rows:
        timeline.append({
            "timestamp_sec": r["timestamp_sec"],
            "clone_prob": r["clone_prob"],
            "speaker_similarity": r["speaker_similarity"],
            "is_synthetic": bool(r["is_synthetic"]),
            "replay_risk": r["replay_risk"],
            "acoustic_features": json.loads(r["acoustic_features_json"]) if r["acoustic_features_json"] else {}
        })
    return timeline

def add_risk_event(session_id: str, timestamp_sec: float, event_type: str, description: str, severity: str):
    conn = get_connection()
    cursor = conn.cursor()
    event_id = f"evt-{uuid.uuid4().hex[:8]}"
    cursor.execute("""
    INSERT INTO risk_events (id, session_id, timestamp_sec, event_type, description, severity)
    VALUES (?, ?, ?, ?, ?, ?)
    """, (event_id, session_id, timestamp_sec, event_type, description, severity))
    conn.commit()
    conn.close()

def get_risk_events(session_id: str) -> List[Dict[str, Any]]:
    conn = get_connection()
    cursor = conn.cursor()
    cursor.execute("SELECT * FROM risk_events WHERE session_id = ? ORDER BY timestamp_sec ASC", (session_id,))
    rows = cursor.fetchall()
    conn.close()
    return [dict(r) for r in rows]

def save_report(session_id: str, report_data: Dict[str, Any], user_id: str = "default"):
    conn = get_connection()
    cursor = conn.cursor()
    report_id = f"rep-{session_id[-8:] if len(session_id) >= 8 else uuid.uuid4().hex[:8]}"
    now = datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    cursor.execute("""
    INSERT OR REPLACE INTO reports (id, user_id, session_id, created_at, report_json)
    VALUES (?, ?, ?, ?, ?)
    """, (report_id, user_id, session_id, now, json.dumps(report_data)))
    conn.commit()
    conn.close()

def get_report(session_id: str) -> Optional[Dict[str, Any]]:
    conn = get_connection()
    cursor = conn.cursor()
    cursor.execute("SELECT * FROM reports WHERE session_id = ? OR id = ?", (session_id, session_id))
    row = cursor.fetchone()
    conn.close()
    if not row:
        return None
    return json.loads(row["report_json"])

def get_user_report(report_or_session_id: str, user_id: str) -> Optional[Dict[str, Any]]:
    """Retrieve report verifying user ownership isolation."""
    conn = get_connection()
    cursor = conn.cursor()
    cursor.execute("""
    SELECT * FROM reports WHERE (session_id = ? OR id = ?) AND (user_id = ? OR user_id = 'default')
    """, (report_or_session_id, report_or_session_id, user_id))
    row = cursor.fetchone()
    conn.close()
    if not row:
        return None
    return json.loads(row["report_json"])
