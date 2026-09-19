"""
VoiceShield AI - Master Dataset Pipeline Runner
===============================================
Executes the complete dataset pipeline end-to-end:
Stage 1: Ingestion & Sample Audio Setup
Stage 2: Audio Standardization, VAD & Segmentation
Stage 3: Metadata Manifest Generation (Master, Speakers, Deepfake, Intent)
Stage 4: Reproducible Zero-Speaker-Leakage Splitting (70/15/15)
Stage 5: Acoustic Feature Extraction (MFCC, Mel-Spec, Spectral Moments, F0)
Stage 6: 128-D Speaker Latent Embeddings & Centroid Profiling
Stage 7: Full Acoustic, Schema & Leakage Validation Audit
Stage 8: Visual Analytics Dashboard Generation

Usage:
    python VoiceShield_Dataset/scripts/run_pipeline.py
"""

import sys
import time
import subprocess
from pathlib import Path

# Ensure UTF-8 stdout on Windows
if sys.platform == "win32":
    try:
        sys.stdout.reconfigure(encoding="utf-8")
    except Exception:
        pass

SCRIPTS_DIR = Path(__file__).resolve().parent

STAGES = [
    ("Stage 1: Ingestion & Sample Setup", ["python", str(SCRIPTS_DIR / "download_dataset.py"), "--setup-sample"]),
    ("Stage 2: Audio Standardization & VAD", ["python", str(SCRIPTS_DIR / "preprocess_audio.py")]),
    ("Stage 3: Metadata Manifest Creation", ["python", str(SCRIPTS_DIR / "create_metadata.py")]),
    ("Stage 4: Zero-Speaker-Leakage Split", ["python", str(SCRIPTS_DIR / "split_dataset.py"), "--speaker-disjoint"]),
    ("Stage 5: Acoustic Feature Extraction", ["python", str(SCRIPTS_DIR / "extract_features.py")]),
    ("Stage 6: Speaker Embeddings & Centroids", ["python", str(SCRIPTS_DIR / "extract_speaker_embeddings.py")]),
    ("Stage 7: Integrity & Leakage Validation Audit", ["python", str(SCRIPTS_DIR / "validate_dataset.py")]),
    ("Stage 8: Visual Analytics Dashboard", ["python", str(SCRIPTS_DIR / "dataset_dashboard.py")]),
]

def main():
    print("=" * 80)
    print(" VoiceShield AI - Master Dataset Pipeline Execution")
    print("=" * 80)
    start_total = time.time()

    for idx, (title, cmd) in enumerate(STAGES, start=1):
        print(f"\n[{idx}/8] Executing {title}...")
        t0 = time.time()
        res = subprocess.run(cmd, capture_output=False, text=True)
        elapsed = time.time() - t0
        if res.returncode != 0:
            print(f"[!] FAILED at {title} (exit code: {res.returncode})")
            sys.exit(res.returncode)
        print(f"    -> Done in {elapsed:.2f}s")

    total_elapsed = time.time() - start_total
    print("\n" + "=" * 80)
    print(f" [✓] COMPLETE DATASET PIPELINE EXECUTED SUCCESSFULLY in {total_elapsed:.2f}s")
    print("=" * 80)

if __name__ == "__main__":
    main()
