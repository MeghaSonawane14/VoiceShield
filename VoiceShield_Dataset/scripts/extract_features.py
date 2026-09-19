"""
VoiceShield AI - Forensic Acoustic Feature Extractor
====================================================
Extracts discriminative acoustic representations for deepfake detection:
1. MFCC (13 coefficients + 13 delta coefficients = 26-D)
2. Mel Spectrogram (80 mel filter banks, log-energy)
3. Spectral Centroid (mean frequency weighted by amplitude)
4. Spectral Bandwidth (spread of spectral energy)
5. Spectral Rolloff (frequency below which 85% energy resides)
6. Zero Crossing Rate (ZCR - frequency of sign changes)
7. RMS Energy (root-mean-square frame energy)
8. Fundamental Pitch (F0 estimation via normalized autocorrelation)

Features are saved into features/<file_id>.npz and a compiled summary matrix.
"""

import os
import sys
import wave
import csv
import argparse
import numpy as np
from pathlib import Path
from typing import Dict, Any, Tuple

if sys.platform == "win32":
    try:
        sys.stdout.reconfigure(encoding="utf-8")
    except Exception:
        pass

SCRIPTS_DIR = Path(__file__).resolve().parent
DATASET_ROOT = SCRIPTS_DIR.parent
METADATA_PATH = DATASET_ROOT / "metadata" / "metadata.csv"
FEATURES_DIR = DATASET_ROOT / "features"

def read_audio(file_path: Path) -> Tuple[np.ndarray, int]:
    """Reads 16-bit PCM mono WAV file into float32 array [-1.0, 1.0]."""
    with wave.open(str(file_path), "rb") as wf:
        n_channels = wf.getnchannels()
        sr = wf.getframerate()
        frames = wf.getnframes()
        raw = wf.readframes(frames)
        data = np.frombuffer(raw, dtype=np.int16).astype(np.float32) / 32768.0
        if n_channels > 1:
            data = data.reshape(-1, n_channels).mean(axis=1)
        return data, sr

def hz_to_mel(hz):
    return 2595.0 * np.log10(1.0 + hz / 700.0)

def mel_to_hz(mel):
    return 700.0 * (10.0 ** (mel / 2595.0) - 1.0)

def get_mel_filterbank(n_mels=80, n_fft=1024, sr=16000, fmin=20.0, fmax=8000.0):
    """Generates triangular Mel filterbank matrix (n_mels, n_fft // 2 + 1)."""
    mel_min = hz_to_mel(fmin)
    mel_max = hz_to_mel(fmax)
    mel_points = np.linspace(mel_min, mel_max, n_mels + 2)
    hz_points = mel_to_hz(mel_points)
    bin_points = np.floor((n_fft + 1) * hz_points / sr).astype(int)

    n_bins = n_fft // 2 + 1
    fbank = np.zeros((n_mels, n_bins), dtype=np.float32)

    for m in range(1, n_mels + 1):
        f_m_minus = bin_points[m - 1]
        f_m = bin_points[m]
        f_m_plus = bin_points[m + 1]

        for k in range(f_m_minus, f_m):
            if f_m > f_m_minus and k < n_bins:
                fbank[m - 1, k] = (k - f_m_minus) / (f_m - f_m_minus)
        for k in range(f_m, f_m_plus):
            if f_m_plus > f_m and k < n_bins:
                fbank[m - 1, k] = (f_m_plus - k) / (f_m_plus - f_m)

    return fbank

def compute_mfcc(mel_spectrogram: np.ndarray, n_mfcc=13):
    """Computes Discrete Cosine Transform (DCT-II) over log-mel spectrogram."""
    log_mel = np.log(np.maximum(mel_spectrogram, 1e-6))
    n_mels = log_mel.shape[0]
    dct_basis = np.zeros((n_mfcc, n_mels), dtype=np.float32)
    for i in range(n_mfcc):
        dct_basis[i, :] = np.cos(np.pi * i * (np.arange(n_mels) + 0.5) / n_mels)
    mfcc = np.dot(dct_basis, log_mel)
    return mfcc

def extract_acoustic_features(audio: np.ndarray, sr: int = 16000, n_fft=1024, hop_length=512) -> Dict[str, np.ndarray]:
    """Extracts comprehensive audio features."""
    if len(audio) < n_fft:
        audio = np.pad(audio, (0, n_fft - len(audio)), mode='constant')

    # 1. Framing & STFT
    n_frames = 1 + (len(audio) - n_fft) // hop_length
    window = np.hanning(n_fft)
    stft = np.zeros((n_fft // 2 + 1, n_frames), dtype=np.complex64)

    for t in range(n_frames):
        seg = audio[t * hop_length: t * hop_length + n_fft] * window
        stft[:, t] = np.fft.rfft(seg)

    magnitude = np.abs(stft)
    power = magnitude ** 2
    freqs = np.linspace(0, sr / 2, n_fft // 2 + 1)

    # 2. Mel Spectrogram (80 bands)
    fbank = get_mel_filterbank(n_mels=80, n_fft=n_fft, sr=sr)
    mel_spec = np.dot(fbank, power)

    # 3. MFCC (13 coefficients) + Deltas
    mfcc = compute_mfcc(mel_spec, n_mfcc=13)
    # Delta MFCC
    if mfcc.shape[1] > 2:
        delta_mfcc = np.diff(mfcc, axis=1, prepend=mfcc[:, :1])
    else:
        delta_mfcc = np.zeros_like(mfcc)

    # 4. Spectral Centroid
    norm_sum = np.sum(magnitude, axis=0) + 1e-9
    spectral_centroid = np.sum(freqs[:, np.newaxis] * magnitude, axis=0) / norm_sum

    # 5. Spectral Bandwidth
    diff = freqs[:, np.newaxis] - spectral_centroid[np.newaxis, :]
    spectral_bandwidth = np.sqrt(np.sum((diff ** 2) * magnitude, axis=0) / norm_sum)

    # 6. Spectral Rolloff (85% energy)
    cumsum_power = np.cumsum(power, axis=0)
    total_power = cumsum_power[-1, :] + 1e-9
    threshold = 0.85 * total_power
    rolloff_indices = np.argmax(cumsum_power >= threshold[np.newaxis, :], axis=0)
    spectral_rolloff = freqs[rolloff_indices]

    # 7. Zero Crossing Rate (ZCR)
    zcr = np.zeros(n_frames, dtype=np.float32)
    for t in range(n_frames):
        seg = audio[t * hop_length: t * hop_length + n_fft]
        zcr[t] = np.mean(np.abs(np.diff(np.signbit(seg))))

    # 8. RMS Energy
    rms_energy = np.zeros(n_frames, dtype=np.float32)
    for t in range(n_frames):
        seg = audio[t * hop_length: t * hop_length + n_fft]
        rms_energy[t] = np.sqrt(np.mean(seg ** 2) + 1e-9)

    # 9. Pitch / F0 via Autocorrelation
    pitch_contour = np.zeros(n_frames, dtype=np.float32)
    min_lag = int(sr / 400.0) # max 400 Hz
    max_lag = int(sr / 60.0)  # min 60 Hz
    for t in range(n_frames):
        seg = audio[t * hop_length: t * hop_length + n_fft]
        if np.std(seg) > 0.01:
            corr = np.correlate(seg, seg, mode='full')
            corr = corr[len(corr) // 2:]
            if len(corr) > max_lag:
                peak_lag = min_lag + np.argmax(corr[min_lag:max_lag])
                if corr[peak_lag] > 0.3 * corr[0]:
                    pitch_contour[t] = sr / float(peak_lag)

    return {
        "mfcc": mfcc,
        "delta_mfcc": delta_mfcc,
        "mel_spectrogram": mel_spec,
        "spectral_centroid": spectral_centroid,
        "spectral_bandwidth": spectral_bandwidth,
        "spectral_rolloff": spectral_rolloff,
        "zcr": zcr,
        "rms_energy": rms_energy,
        "pitch_f0": pitch_contour
    }

def run_feature_extraction():
    """Iterates through metadata.csv and extracts features for all audio clips."""
    FEATURES_DIR.mkdir(parents=True, exist_ok=True)
    if not METADATA_PATH.exists():
        print(f"[!] metadata.csv not found at {METADATA_PATH}. Run create_metadata.py first.")
        return

    print("=" * 80)
    print(" VoiceShield AI - Acoustic Feature Extraction Pipeline")
    print("=" * 80)
    print("Extracting: MFCC (13-D + delta), Mel Spec (80 bands), Centroid, Bandwidth, Rolloff, ZCR, RMS, F0")

    with open(METADATA_PATH, "r", encoding="utf-8") as f:
        reader = csv.DictReader(f)
        records = list(reader)

    extracted_count = 0
    feature_summaries = []

    for r in records:
        file_path = DATASET_ROOT / r["file_path"]
        if not file_path.exists():
            continue

        try:
            audio, sr = read_audio(file_path)
            feats = extract_acoustic_features(audio, sr=sr)
            file_id = r["file_id"]
            save_path = FEATURES_DIR / f"{file_id}.npz"

            np.savez_compressed(
                save_path,
                mfcc=feats["mfcc"],
                delta_mfcc=feats["delta_mfcc"],
                mel_spectrogram=feats["mel_spectrogram"],
                spectral_centroid=feats["spectral_centroid"],
                spectral_bandwidth=feats["spectral_bandwidth"],
                spectral_rolloff=feats["spectral_rolloff"],
                zcr=feats["zcr"],
                rms_energy=feats["rms_energy"],
                pitch_f0=feats["pitch_f0"]
            )

            # Record summary vector for instant tabular analysis
            summary = {
                "file_id": file_id,
                "label": r["label"],
                "mean_centroid": float(np.mean(feats["spectral_centroid"])),
                "mean_bandwidth": float(np.mean(feats["spectral_bandwidth"])),
                "mean_rolloff": float(np.mean(feats["spectral_rolloff"])),
                "mean_zcr": float(np.mean(feats["zcr"])),
                "mean_rms": float(np.mean(feats["rms_energy"])),
                "mean_pitch_f0": float(np.mean(feats["pitch_f0"][feats["pitch_f0"] > 0])) if np.any(feats["pitch_f0"] > 0) else 0.0
            }
            feature_summaries.append(summary)
            extracted_count += 1
        except Exception as e:
            print(f"  [!] Error extracting features for {file_path.name}: {e}")

    # Save summary CSV
    summary_csv = FEATURES_DIR / "features_summary.csv"
    if feature_summaries:
        with open(summary_csv, "w", newline="", encoding="utf-8") as f:
            writer = csv.DictWriter(f, fieldnames=list(feature_summaries[0].keys()))
            writer.writeheader()
            writer.writerows(feature_summaries)

    print("\n" + "-" * 80)
    print(f"[✓] Feature Extraction Complete: {extracted_count} audio files processed.")
    print(f"    Saved feature matrices to: {FEATURES_DIR.relative_to(DATASET_ROOT)}/*.npz")
    print(f"    Saved summary metrics to : {summary_csv.relative_to(DATASET_ROOT)}")
    print("-" * 80)

def main():
    parser = argparse.ArgumentParser(description="VoiceShield Feature Extractor")
    parser.add_argument("--run", action="store_true", default=True, help="Run feature extraction on dataset")
    args = parser.parse_args()
    run_feature_extraction()

if __name__ == "__main__":
    main()
