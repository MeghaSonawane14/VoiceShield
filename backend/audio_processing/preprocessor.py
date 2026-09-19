import io
import wave
import struct
import math
import numpy as np
from typing import Tuple, Dict, Any, List

TARGET_SR = 16000

def load_audio_from_bytes(audio_bytes: bytes) -> Tuple[np.ndarray, int]:
    """
    Parse WAV or raw PCM bytes into a floating-point numpy array [-1.0, 1.0].
    Falls back to raw 16-bit PCM if WAV header is absent or corrupt.
    """
    try:
        with wave.open(io.BytesIO(audio_bytes), 'rb') as wf:
            n_channels = wf.getnchannels()
            sampwidth = wf.getsampwidth()
            framerate = wf.getframerate()
            n_frames = wf.getnframes()
            raw_data = wf.readframes(n_frames)

            if sampwidth == 2:
                # 16-bit PCM
                data = np.frombuffer(raw_data, dtype=np.int16).astype(np.float32) / 32768.0
            elif sampwidth == 1:
                # 8-bit unsigned
                data = (np.frombuffer(raw_data, dtype=np.uint8).astype(np.float32) - 128) / 128.0
            elif sampwidth == 4:
                # 32-bit int
                data = np.frombuffer(raw_data, dtype=np.int32).astype(np.float32) / 2147483648.0
            else:
                data = np.frombuffer(raw_data, dtype=np.int16).astype(np.float32) / 32768.0

            if n_channels > 1:
                data = data.reshape(-1, n_channels).mean(axis=1)

            return resample_audio(data, framerate, TARGET_SR), TARGET_SR
    except Exception:
        # Fallback: Assume raw 16-bit 16kHz PCM
        try:
            length = len(audio_bytes) - (len(audio_bytes) % 2)
            data = np.frombuffer(audio_bytes[:length], dtype=np.int16).astype(np.float32) / 32768.0
            if len(data) == 0:
                data = np.zeros(TARGET_SR, dtype=np.float32)
            return data, TARGET_SR
        except Exception:
            return np.zeros(TARGET_SR, dtype=np.float32), TARGET_SR

def resample_audio(audio: np.ndarray, orig_sr: int, target_sr: int) -> np.ndarray:
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

def normalize_audio(audio: np.ndarray, target_db: float = -20.0) -> np.ndarray:
    """Peak and RMS normalization with clipping protection."""
    if len(audio) == 0:
        return audio
    peak = np.max(np.abs(audio))
    if peak < 1e-6:
        return audio
    # Normalize peak to 0.95
    normalized = audio / (peak + 1e-6) * 0.95
    return normalized

def apply_vad(audio: np.ndarray, frame_size: int = 512, energy_thresh: float = 0.005) -> Tuple[np.ndarray, float]:
    """
    Voice Activity Detection using frame energy.
    Returns: (voiced_audio, speech_ratio)
    """
    if len(audio) < frame_size:
        return audio, 1.0
    num_frames = len(audio) // frame_size
    voiced_frames = []
    total_frames = num_frames
    
    for i in range(num_frames):
        frame = audio[i * frame_size:(i + 1) * frame_size]
        energy = np.mean(frame ** 2)
        if energy >= energy_thresh:
            voiced_frames.append(frame)

    if voiced_frames:
        voiced_audio = np.concatenate(voiced_frames)
        speech_ratio = len(voiced_frames) / max(total_frames, 1)
    else:
        voiced_audio = audio
        speech_ratio = 0.1

    return voiced_audio, speech_ratio

def create_mel_filterbank(num_filters: int = 40, n_fft: int = 512, sr: int = 16000) -> np.ndarray:
    """Construct Mel-spaced triangular filterbank matrix."""
    low_freq = 0.0
    high_freq = sr / 2.0
    low_mel = 2595.0 * np.log10(1.0 + low_freq / 700.0)
    high_mel = 2595.0 * np.log10(1.0 + high_freq / 700.0)
    mel_points = np.linspace(low_mel, high_mel, num_filters + 2)
    hz_points = 700.0 * (10.0 ** (mel_points / 2595.0) - 1.0)
    bin_points = np.floor((n_fft + 1) * hz_points / sr).astype(int)

    num_bins = n_fft // 2 + 1
    filterbank = np.zeros((num_filters, num_bins), dtype=np.float32)

    for m in range(1, num_filters + 1):
        f_left = bin_points[m - 1]
        f_center = bin_points[m]
        f_right = bin_points[m + 1]

        for k in range(f_left, f_center):
            if f_center > f_left:
                filterbank[m - 1, min(k, num_bins - 1)] = (k - f_left) / (f_center - f_left)
        for k in range(f_center, f_right):
            if f_right > f_center:
                filterbank[m - 1, min(k, num_bins - 1)] = (f_right - k) / (f_right - f_center)

    return filterbank

def extract_acoustic_features(audio: np.ndarray, sr: int = 16000) -> Dict[str, Any]:
    """
    Extract forensic acoustic features for deepfake & clone detection.
    Computes:
    - MFCC approximation (13 bins)
    - Spectral Centroid (Hz)
    - Spectral Rolloff (Hz)
    - Spectral Flux
    - Zero-Crossing Rate (ZCR)
    - Root-Mean-Square Energy (RMS)
    - High-frequency phase/spectral distortion (vocoder artifacts)
    """
    if len(audio) < 256:
        audio = np.pad(audio, (0, 256 - len(audio)))

    # Frame-level STFT
    frame_size = 512
    hop_size = 256
    window = np.hanning(frame_size)
    num_frames = max(1, (len(audio) - frame_size) // hop_size)

    magnitudes = []
    zcr_values = []
    rms_values = []

    for i in range(num_frames):
        frame = audio[i * hop_size: i * hop_size + frame_size]
        if len(frame) < frame_size:
            break
        # ZCR
        zcr = np.mean(np.abs(np.diff(np.signbit(frame))))
        zcr_values.append(zcr)
        # RMS
        rms = np.sqrt(np.mean(frame ** 2))
        rms_values.append(rms)
        # STFT
        w_frame = frame * window
        fft_res = np.abs(np.fft.rfft(w_frame))
        magnitudes.append(fft_res)

    if not magnitudes:
        magnitudes = [np.zeros(frame_size // 2 + 1)]
        zcr_values = [0.05]
        rms_values = [0.1]

    mag_matrix = np.array(magnitudes)  # shape: (frames, freq_bins)
    avg_spectrum = np.mean(mag_matrix, axis=0) + 1e-8
    freq_bins = np.linspace(0, sr / 2, len(avg_spectrum))

    # 1. Spectral Centroid
    spectral_centroid = float(np.sum(freq_bins * avg_spectrum) / np.sum(avg_spectrum))

    # 2. Spectral Rolloff (85% energy)
    cum_energy = np.cumsum(avg_spectrum)
    total_energy = cum_energy[-1]
    rolloff_idx = np.where(cum_energy >= 0.85 * total_energy)[0]
    spectral_rolloff = float(freq_bins[rolloff_idx[0]]) if len(rolloff_idx) > 0 else float(freq_bins[-1])

    # 3. Spectral Flux
    if len(mag_matrix) > 1:
        flux = float(np.mean(np.sqrt(np.sum(np.diff(mag_matrix, axis=0) ** 2, axis=1))))
    else:
        flux = 0.02

    # 4. Mel & MFCC approximation
    fbank = create_mel_filterbank(num_filters=26, n_fft=frame_size, sr=sr)
    mel_energies = np.dot(fbank, avg_spectrum)
    log_mel = np.log(np.maximum(mel_energies, 1e-6))
    # DCT type II approximation
    num_mfcc = 13
    mfcc = []
    for k in range(num_mfcc):
        basis = np.cos(math.pi * k * (np.arange(len(log_mel)) + 0.5) / len(log_mel))
        mfcc.append(float(np.sum(log_mel * basis)))

    # 5. Vocoder artifact metric:
    # Neural vocoders (HiFi-GAN, WaveNet, Tacotron2) exhibit characteristic unnatural high-frequency
    # energy dropoff ratios, periodic harmonic dips, and higher spectral flatness in upper bins.
    upper_bins = avg_spectrum[int(len(avg_spectrum) * 0.6):]
    lower_bins = avg_spectrum[:int(len(avg_spectrum) * 0.6)]
    high_freq_ratio = float(np.sum(upper_bins) / (np.sum(lower_bins) + 1e-6))
    
    # Spectral flatness (ratio of geometric to arithmetic mean)
    geo_mean = np.exp(np.mean(np.log(avg_spectrum + 1e-8)))
    arith_mean = np.mean(avg_spectrum)
    spectral_flatness = float(geo_mean / (arith_mean + 1e-8))

    return {
        "spectral_centroid": round(spectral_centroid, 2),
        "spectral_rolloff": round(spectral_rolloff, 2),
        "spectral_flux": round(flux, 4),
        "zero_crossing_rate": round(float(np.mean(zcr_values)), 4),
        "rms_energy": round(float(np.mean(rms_values)), 4),
        "spectral_flatness": round(spectral_flatness, 4),
        "high_freq_ratio": round(high_freq_ratio, 4),
        "mfcc": [round(x, 3) for x in mfcc]
    }
