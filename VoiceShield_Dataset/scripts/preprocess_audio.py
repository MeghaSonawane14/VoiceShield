"""
VoiceShield AI - Audio Standardization & Segmentation Preprocessor
==================================================================
Performs standardization and segmentation on raw audio inputs:
1. Load WAV / PCM audio
2. Convert to Mono
3. Resample to 16,000 Hz
4. Amplitude Normalization (RMS / Peak)
5. Silence Trimming & Energy-based VAD
6. Windowed Segmentation (e.g. 4.0s segments with 1.0s overlap)
7. Output to processed/real and processed/fake

Original raw files are left untouched.
"""

import os
import sys
import wave
import yaml
import argparse
import numpy as np
from pathlib import Path
from typing import Tuple, List, Dict, Any

if sys.platform == "win32":
    try:
        sys.stdout.reconfigure(encoding="utf-8")
    except Exception:
        pass

SCRIPTS_DIR = Path(__file__).resolve().parent
DATASET_ROOT = SCRIPTS_DIR.parent
CONFIG_PATH = DATASET_ROOT / "config.yaml"

def load_config() -> Dict[str, Any]:
    """Loads preprocessing configuration from config.yaml."""
    if CONFIG_PATH.exists():
        with open(CONFIG_PATH, "r", encoding="utf-8") as f:
            return yaml.safe_load(f)
    return {
        "audio": {
            "target_sample_rate": 16000,
            "channels": 1,
            "segment_duration": 4.0,
            "overlap": 1.0,
            "min_duration": 1.5,
            "normalize_amplitude": True,
            "trim_silence": True
        }
    }

def read_audio(file_path: Path) -> Tuple[np.ndarray, int]:
    """Reads WAV file and returns floating-point numpy array [-1.0, 1.0] and sample rate."""
    with wave.open(str(file_path), "rb") as wf:
        n_channels = wf.getnchannels()
        sampwidth = wf.getsampwidth()
        framerate = wf.getframerate()
        n_frames = wf.getnframes()
        raw_bytes = wf.readframes(n_frames)

        if sampwidth == 2:
            data = np.frombuffer(raw_bytes, dtype=np.int16).astype(np.float32) / 32768.0
        elif sampwidth == 1:
            data = (np.frombuffer(raw_bytes, dtype=np.uint8).astype(np.float32) - 128.0) / 128.0
        elif sampwidth == 4:
            data = np.frombuffer(raw_bytes, dtype=np.int32).astype(np.float32) / 2147483648.0
        else:
            data = np.frombuffer(raw_bytes, dtype=np.int16).astype(np.float32) / 32768.0

        if n_channels > 1:
            data = data.reshape(-1, n_channels).mean(axis=1)

    return data, framerate

def resample_linear(audio: np.ndarray, orig_sr: int, target_sr: int) -> np.ndarray:
    """Linear interpolation resampling to target sample rate."""
    if orig_sr == target_sr or len(audio) == 0:
        return audio
    duration = len(audio) / orig_sr
    target_length = int(duration * target_sr)
    if target_length <= 0:
        return np.zeros(target_sr, dtype=np.float32)
    orig_indices = np.linspace(0, len(audio) - 1, num=len(audio))
    target_indices = np.linspace(0, len(audio) - 1, num=target_length)
    return np.interp(target_indices, orig_indices, audio).astype(np.float32)

def normalize_amplitude(audio: np.ndarray, target_db: float = -20.0) -> np.ndarray:
    """Applies RMS normalization while preserving dynamic peaks."""
    if len(audio) == 0:
        return audio
    rms = np.sqrt(np.mean(audio ** 2) + 1e-9)
    current_db = 20 * np.log10(rms)
    gain = 10 ** ((target_db - current_db) / 20.0)
    norm = audio * gain
    # Soft limiter to prevent clipping
    peak = np.max(np.abs(norm))
    if peak > 0.95:
        norm = norm * (0.95 / peak)
    return norm

def trim_silence_vad(audio: np.ndarray, sr: int = 16000, frame_duration_ms: int = 20, threshold: float = 0.012) -> np.ndarray:
    """Trims leading/trailing silence using frame-wise RMS energy VAD."""
    if len(audio) == 0:
        return audio
    frame_len = int(sr * (frame_duration_ms / 1000.0))
    if frame_len <= 0 or len(audio) < frame_len:
        return audio

    n_frames = len(audio) // frame_len
    energies = [np.sqrt(np.mean(audio[i * frame_len:(i + 1) * frame_len] ** 2)) for i in range(n_frames)]

    active_indices = [i for i, e in enumerate(energies) if e >= threshold]
    if not active_indices:
        return audio  # Keep entire audio if all below threshold

    start_sample = max(0, (active_indices[0] - 1) * frame_len)
    end_sample = min(len(audio), (active_indices[-1] + 2) * frame_len)
    return audio[start_sample:end_sample]

def segment_audio(audio: np.ndarray, sr: int = 16000, seg_duration: float = 4.0, overlap: float = 1.0, min_duration: float = 1.5) -> List[np.ndarray]:
    """Segments audio into fixed-length windows with configurable overlap."""
    seg_samples = int(seg_duration * sr)
    hop_samples = int((seg_duration - overlap) * sr)
    min_samples = int(min_duration * sr)

    if len(audio) <= seg_samples:
        if len(audio) >= min_samples:
            # Pad to seg_samples with minimal edge reflection
            padded = np.pad(audio, (0, seg_samples - len(audio)), mode='constant')
            return [padded]
        return []

    segments = []
    start = 0
    while start + seg_samples <= len(audio):
        segments.append(audio[start:start + seg_samples])
        start += hop_samples

    # Check remaining tail
    remaining = len(audio) - start
    if remaining >= min_samples:
        padded = np.pad(audio[start:], (0, seg_samples - remaining), mode='constant')
        segments.append(padded)

    return segments

def save_wav(file_path: Path, audio: np.ndarray, sr: int = 16000):
    """Saves 16-bit mono PCM WAV file."""
    file_path.parent.mkdir(parents=True, exist_ok=True)
    pcm = (np.clip(audio, -1.0, 1.0) * 32767).astype(np.int16)
    with wave.open(str(file_path), "wb") as wf:
        wf.setnchannels(1)
        wf.setsampwidth(2)
        wf.setframerate(sr)
        wf.writeframes(pcm.tobytes())

def preprocess_directory(raw_dir: Path, out_dir: Path, label: str, cfg: Dict[str, Any]) -> int:
    """Preprocesses all audio files in raw_dir and outputs segmented standardized files to out_dir."""
    audio_cfg = cfg.get("audio", {})
    target_sr = audio_cfg.get("target_sample_rate", 16000)
    seg_dur = audio_cfg.get("segment_duration", 4.0)
    overlap = audio_cfg.get("overlap", 1.0)
    min_dur = audio_cfg.get("min_duration", 1.5)
    do_norm = audio_cfg.get("normalize_amplitude", True)
    do_trim = audio_cfg.get("trim_silence", True)

    files = list(raw_dir.glob("**/*.wav")) + list(raw_dir.glob("**/*.flac"))
    processed_count = 0

    for file_path in files:
        try:
            audio, sr = read_audio(file_path)
            audio = resample_linear(audio, sr, target_sr)

            if do_trim:
                audio = trim_silence_vad(audio, target_sr)

            if do_norm:
                audio = normalize_amplitude(audio)

            segments = segment_audio(audio, target_sr, seg_dur, overlap, min_dur)
            base_name = file_path.stem

            for idx, seg in enumerate(segments):
                out_name = f"{base_name}_seg{idx:02d}.wav" if len(segments) > 1 else f"{base_name}.wav"
                out_file = out_dir / out_name
                save_wav(out_file, seg, target_sr)
                processed_count += 1
        except Exception as e:
            print(f"  [!] Error processing {file_path.name}: {e}")

    return processed_count

def run_pipeline():
    """Main preprocessing execution entrypoint."""
    cfg = load_config()
    print("=" * 80)
    print(" VoiceShield AI - Audio Preprocessing & Standardization Pipeline")
    print("=" * 80)
    print(f"Target Sample Rate : {cfg['audio']['target_sample_rate']} Hz")
    print(f"Target Channels    : Mono (1)")
    print(f"Segment Length     : {cfg['audio']['segment_duration']}s (Overlap: {cfg['audio']['overlap']}s)")
    print(f"Config File        : {CONFIG_PATH.relative_to(DATASET_ROOT)}\n")

    real_out = DATASET_ROOT / "processed" / "real"
    fake_out = DATASET_ROOT / "processed" / "fake"
    real_out.mkdir(parents=True, exist_ok=True)
    fake_out.mkdir(parents=True, exist_ok=True)

    # 1. Process Custom Consented Recordings -> REAL
    custom_dir = DATASET_ROOT / "raw" / "custom"
    count_custom = preprocess_directory(custom_dir, real_out, "REAL", cfg)
    print(f"[+] Processed Custom Consented Speech -> {count_custom} real segments")

    # 2. Process ASVspoof Raw (bonafide -> REAL, spoof -> FAKE)
    asv_dir = DATASET_ROOT / "raw" / "asvspoof"
    count_asv_real = 0
    count_asv_fake = 0
    for f in asv_dir.glob("*.wav"):
        if "bonafide" in f.name.lower() or "real" in f.name.lower():
            count_asv_real += preprocess_directory(f.parent, real_out, "REAL", cfg)
            break
    for f in asv_dir.glob("*.wav"):
        if "spoof" in f.name.lower() or "fake" in f.name.lower() or "clone" in f.name.lower():
            count_asv_fake += preprocess_directory(f.parent, fake_out, "FAKE", cfg)
            break
    print(f"[+] Processed ASVspoof Samples -> {count_asv_real} real, {count_asv_fake} fake")

    # 3. Process WaveFake -> FAKE
    wf_dir = DATASET_ROOT / "raw" / "wavefake"
    count_wf = preprocess_directory(wf_dir, fake_out, "FAKE", cfg)
    print(f"[+] Processed WaveFake Synthesis -> {count_wf} fake segments")

    # 4. Process Fake-or-Real -> Appropriate labels
    for_dir = DATASET_ROOT / "raw" / "fake_or_real"
    for f in for_dir.glob("*.wav"):
        if "real" in f.name.lower():
            preprocess_directory(f.parent, real_out, "REAL", cfg)
            break
    for f in for_dir.glob("*.wav"):
        if "fake" in f.name.lower():
            preprocess_directory(f.parent, fake_out, "FAKE", cfg)
            break

    total_real = len(list(real_out.glob("*.wav")))
    total_fake = len(list(fake_out.glob("*.wav")))
    print("\n" + "-" * 80)
    print(f"[✓] Preprocessing Complete: {total_real} REAL segments | {total_fake} FAKE segments")
    print(f"    Saved to: {DATASET_ROOT / 'processed'}")
    print("-" * 80)

def main():
    parser = argparse.ArgumentParser(description="VoiceShield Audio Preprocessing Pipeline")
    parser.add_argument("--run", action="store_true", default=True, help="Execute preprocessing on raw audio")
    args = parser.parse_args()
    run_pipeline()

if __name__ == "__main__":
    main()
