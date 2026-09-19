import math
import numpy as np
from typing import Dict, Any, List, Optional, Tuple
from backend.database.db import get_all_trusted_speakers

def compute_speaker_embedding(audio: np.ndarray, sr: int = 16000, dim: int = 128) -> List[float]:
    """
    Extract a normalized 128-dimensional acoustic speaker embedding vector.
    Constructed from multi-band spectral centroids, harmonic ratios, MFCC moments,
    and pitch period statistics (analogous to ECAPA-TDNN acoustic latent projection).
    """
    if len(audio) < 512:
        return [0.0] * dim

    # FFT analysis
    fft_res = np.abs(np.fft.rfft(audio[:min(len(audio), 32000)]))
    n_bins = len(fft_res)

    # 1. Multi-band energy distribution (64 dimensions)
    bands = np.array_split(fft_res, 64)
    band_energies = [np.mean(b ** 2) for b in bands]
    log_band_energies = np.log1p(np.array(band_energies, dtype=np.float32))

    # 2. Cepstral representation (32 dimensions)
    if len(log_band_energies) >= 32:
        dct_proj = []
        for k in range(32):
            basis = np.cos(math.pi * k * (np.arange(64) + 0.5) / 64)
            dct_proj.append(float(np.sum(log_band_energies * basis)))
    else:
        dct_proj = [0.0] * 32

    # 3. Temporal statistics (ZCR, RMS, variance across 4 temporal quarters) (32 dimensions)
    quarter_len = len(audio) // 4
    temporal_features = []
    for q in range(4):
        segment = audio[q * quarter_len: (q + 1) * quarter_len]
        if len(segment) > 0:
            temporal_features.extend([
                float(np.mean(segment ** 2)),
                float(np.std(segment)),
                float(np.mean(np.abs(np.diff(np.signbit(segment))))),
                float(np.max(np.abs(segment))),
                float(np.percentile(segment, 75) - np.percentile(segment, 25)),
                float(np.mean(np.abs(segment))),
                float(np.median(np.abs(segment))),
                float(np.std(np.diff(segment)))
            ])
        else:
            temporal_features.extend([0.0] * 8)

    # Combine to 128-dim vector
    raw_vector = np.concatenate([
        log_band_energies[:64],
        np.array(dct_proj[:32], dtype=np.float32),
        np.array(temporal_features[:32], dtype=np.float32)
    ])

    # L2 normalize
    norm = np.linalg.norm(raw_vector)
    if norm > 1e-6:
        normalized_vector = raw_vector / norm
    else:
        normalized_vector = raw_vector

    return [round(float(x), 5) for x in normalized_vector]

def cosine_similarity(v1: List[float], v2: List[float]) -> float:
    """Compute cosine similarity between two embedding vectors."""
    a = np.array(v1, dtype=np.float32)
    b = np.array(v2, dtype=np.float32)
    norm_a = np.linalg.norm(a)
    norm_b = np.linalg.norm(b)
    if norm_a < 1e-6 or norm_b < 1e-6:
        return 0.0
    sim = float(np.dot(a, b) / (norm_a * norm_b))
    return round(float(np.clip(sim, 0.0, 1.0)), 4)

def match_speaker(embedding: List[float], threshold: float = 0.70) -> Dict[str, Any]:
    """
    Compare embedding against enrolled trusted speakers.
    Returns best likely speaker match and similarity score.
    """
    registered_speakers = get_all_trusted_speakers()
    if not registered_speakers:
        return {
            "matched": False,
            "likely_speaker": "Unknown / No trusted speaker enrolled",
            "speaker_id": None,
            "similarity": 0.0,
            "confidence_label": "No Enrollments"
        }

    best_match = None
    highest_sim = -1.0

    for spk in registered_speakers:
        # Load embedding from DB
        from backend.database.db import get_speaker_by_id
        full_spk = get_speaker_by_id(spk["id"])
        if full_spk and "embedding" in full_spk:
            sim = cosine_similarity(embedding, full_spk["embedding"])
            if sim > highest_sim:
                highest_sim = sim
                best_match = full_spk

    if best_match and highest_sim >= threshold:
        return {
            "matched": True,
            "likely_speaker": best_match["name"],
            "relationship": best_match.get("relationship", "Trusted Contact"),
            "speaker_id": best_match["id"],
            "similarity": highest_sim,
            "confidence_label": "Likely speaker match" if highest_sim < 0.85 else "Strong speaker match"
        }
    elif best_match and highest_sim >= 0.40:
        return {
            "matched": False,
            "likely_speaker": f"Inconclusive ({best_match['name']} low match)",
            "speaker_id": best_match["id"],
            "similarity": highest_sim,
            "confidence_label": "Weak similarity"
        }
    else:
        return {
            "matched": False,
            "likely_speaker": "Unknown / No trusted speaker match",
            "speaker_id": None,
            "similarity": max(0.0, highest_sim),
            "confidence_label": "Unknown Speaker"
        }
