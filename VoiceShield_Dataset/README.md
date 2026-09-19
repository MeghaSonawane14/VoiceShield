# VoiceShield AI — Dataset Pipeline & Benchmark Corpus
**Real-Time Voice Clone Detection and Impersonation Prevention Academic Evaluation Suite**

---

## 1. Dataset Purpose
The **VoiceShield AI Dataset** is an academic research benchmark designed to train, evaluate, and demonstrate real-time countermeasure systems against AI voice cloning and targeted social engineering impersonation.

The pipeline explicitly decouples the learning problem into **two independent ML tasks**:
- **Task A: Voice Clone / Deepfake Detection (Binary Classification)**  
  *Question:* Is the acoustic signal genuine human speech (`REAL`) or synthesized/vocoded speech (`FAKE`)?
- **Task B: Speaker Identification & Verification (Biometric Verification)**  
  *Question:* Does the acoustic vocal tract profile match an enrolled trusted speaker (`speaker_001` to `speaker_005`), regardless of whether the voice is genuine or cloned?

---

## 2. Dataset Sources & Licenses

| Corpus | Source Role | Audio Formats | License | Citation / Archive |
| :--- | :--- | :--- | :--- | :--- |
| **ASVspoof 2021 DF** | **PRIMARY** Benchmark | 16 kHz FLAC / WAV | ASVspoof 2021 Research Agreement (Non-commercial) | Yamagishi et al., *ASVspoof 2021: Accelerating Progress in Spoofed Speech Countermeasures*, 2021. [Zenodo 4835108](https://zenodo.org/record/4835108) |
| **WaveFake** | **SECONDARY** Benchmark | 16 kHz 16-bit WAV | Creative Commons Attribution 4.0 (CC BY 4.0) | Frank & Schönherr, *WaveFake: A Data Set to Facilitate Audio Deepfake Detection*, 2021. [Zenodo 5642694](https://zenodo.org/record/5642694) |
| **Fake-or-Real (FoR)** | **OPTIONAL** Benchmark | 16 kHz WAV / MP3 | CC BY-NC 4.0 | Reimao & Tzerpos, *FoR: A Dataset for Synthetic Speech Detection*, 2019. |
| **VoiceShield Consented** | **CUSTOM** Speaker Profiles | 16 kHz Mono PCM WAV | VoiceShield Academic Research Consent | Internal Multi-Lingual Corpus (English, Hindi, Marathi), 2026. |

---

## 3. ASVspoof 2021 Deepfake (DF) Evaluation
ASVspoof 2021 DF contains diverse spoofing compression artifacts (m4a, ogg, mp3) produced across neural text-to-speech (TTS) and voice conversion (VC) architectures.
- Bona fide samples map to: `REAL`
- Spoofed/vocoded samples map to: `FAKE`

---

## 4. WaveFake Evaluation
WaveFake provides cross-vocoder evaluation data across 6 state-of-the-art neural vocoders: MelGAN, Parallel WaveGAN, Multi-band MelGAN, HiFi-GAN, WaveGlow, and FullBand MelGAN. All samples map to `FAKE`.

---

## 5. Fake-or-Real (FoR)
FoR contains balanced pairs of real human audio and modern cloud TTS voices. Real audio maps to `REAL`; synthetic audio maps to `FAKE`.

---

## 6. Custom Consented Speaker Dataset
Contains consented multi-lingual voice recordings from 5 enrolled identities across English, Hindi, and Marathi:
- `speaker_001`: Rahul Sharma (Baritone, F0 ~130 Hz, English / Hindi / Marathi)
- `speaker_002`: Dr. Ananya Iyer (Mezzo-Soprano, F0 ~210 Hz, English / Hindi)
- `speaker_003`: Vikram Patel (Bass, F0 ~115 Hz, English / Hindi / Marathi)
- `speaker_004`: Priya Nair (Soprano, F0 ~225 Hz, English / Hindi)
- `speaker_005`: Rohan Deshmukh (Tenor, F0 ~125 Hz, English / Marathi)

---

## 7. Directory Structure

```
VoiceShield_Dataset/
├── config.yaml                     # Global pipeline parameters (audio, splits, features)
├── dashboard.html                  # Interactive visual HTML analytics dashboard
├── README.md                       # Complete documentation
├── demo_scenarios.json             # 5 standardized demo scenario specifications
│
├── raw/                            # Untouched raw external and custom recordings
│   ├── asvspoof/
│   ├── wavefake/
│   ├── fake_or_real/
│   └── custom/
│
├── processed/                      # Standardized 16kHz mono segmented audio
│   ├── real/                       # Bona fide human speech clips
│   └── fake/                       # AI-generated / vocoded deepfake clips
│
├── speaker_profiles/               # Trusted speaker audio samples and profile centroids
│   ├── speaker_001/                # (sample_XX.wav, embedding_XX.npy, centroid.npy)
│   ├── speaker_002/
│   ├── speaker_003/
│   ├── speaker_004/
│   └── speaker_005/
│
├── impersonation/                  # Test pairs for target impersonation evaluation
│   ├── genuine/                    # Authentic samples of target speaker
│   └── cloned/                     # AI clones mimicking the target speaker
│
├── replay/                         # Acoustic channel distortion test set
│   ├── original/
│   └── replayed/                   # Marked: EXPERIMENTAL
│
├── intent/                         # Conversational transcript audio samples
│   ├── normal/
│   └── suspicious/
│
├── demo_data/                      # Instant, verified demo audio files
│   ├── genuine_speaker.wav
│   ├── ai_voice.wav
│   ├── unknown_ai_voice.wav
│   ├── suspicious_request.wav
│   └── verification_failed.wav
│
├── metadata/                       # Audit-ready metadata manifests
│   ├── metadata.csv                # Master dataset manifest
│   ├── speakers.csv                # Enrolled speaker profile registry
│   ├── deepfake_metadata.csv       # Vocoder & artifact specifications
│   └── intent_metadata.csv         # Phishing & scam intent triggers
│
├── features/                       # Extracted forensic feature representations
│   ├── features_summary.csv        # Tabular summary metrics per clip
│   └── *.npz                       # Compressed acoustic feature arrays
│
├── train/                          # 70% Training partition manifest
├── validation/                     # 15% Validation partition manifest
├── test/                           # 15% Testing partition manifest
│
└── scripts/                        # Executable pipeline automation tools
    ├── download_dataset.py         # Ingestion, inspection, and sample setup
    ├── preprocess_audio.py         # 16kHz mono resampling, VAD, and segmentation
    ├── create_metadata.py          # Master CSV manifest generation
    ├── split_dataset.py            # Reproducible zero-speaker-leakage partitioning
    ├── extract_features.py         # MFCC, Mel spec, spectral moments, and F0
    ├── extract_speaker_embeddings.py # 128-D ECAPA-TDNN latent centroid extraction
    ├── validate_dataset.py         # Full acoustic, schema, and leakage validation
    └── dataset_dashboard.py        # ASCII CLI and interactive HTML visual reporting
```

---

## 8. Metadata Format

### `metadata/metadata.csv` (Master Manifest)
| Column | Description | Example |
| :--- | :--- | :--- |
| `file_id` | Unique sample identifier | `vs_0001` |
| `file_path` | Relative audio file path | `processed/real/speaker_001_english_01.wav` |
| `speaker_id` | Enrolled speaker ID or `unknown_speaker` | `speaker_001` |
| `label` | Standardized target label | `REAL` or `FAKE` |
| `dataset_source` | Origin corpus | `custom`, `ASVspoof2021_DF`, `WaveFake` |
| `language` | Spoken language | `English`, `Hindi`, `Marathi` |
| `duration` | Length in seconds | `4.0` |
| `sample_rate` | Audio sample rate (Hz) | `16000` |
| `attack_type` | Attack category | `none`, `voice_clone`, `vocoder_melgan` |
| `split` | Partition assignment | `train`, `validation`, `test` |

### `metadata/speakers.csv` (Speaker Profiles)
Includes `speaker_id, speaker_name, language, gender, num_samples, registration_date, status, centroid_path`.

### `metadata/intent_metadata.csv` (Scam & Intent Taxonomy)
Includes `text, category, risk_level, language` across:
- `normal`: Low risk conversational dialogue
- `otp_request`: High risk verification code harvesting
- `financial_request`: Critical risk wire transfer or UPI demands
- `credential_request`: Critical risk PIN / CVV / account queries
- `password_request`: Critical risk credential phishing
- `emergency_request`: Critical risk hospital / accident pretext

---

## 9. Audio Standardization Pipeline
Raw audio is standardized via `scripts/preprocess_audio.py`:
1. **Mono Conversion**: Multi-channel inputs are downmixed via cross-channel averaging.
2. **Resampling**: Linear interpolation resampling strictly to 16,000 Hz.
3. **Silence Trimming**: Energy-based Voice Activity Detection (VAD) with frame RMS thresholding at 0.012.
4. **Amplitude Normalization**: RMS normalization to -20.0 dB with soft-clipping protection.
5. **Windowed Segmentation**: Segments audio into 4.0-second clips with 1.0-second overlap (configurable in `config.yaml`).

---

## 10. Train / Validation / Test Splitting
Partitions follow a reproducible **70% / 15% / 15%** split via `scripts/split_dataset.py` with fixed random seed `42`.

---

## 11. Speaker Leakage Prevention
To evaluate genuine cross-speaker generalization, `scripts/split_dataset.py --speaker-disjoint` enforces **zero speaker overlap**:
- **Train Speakers**: `speaker_002`, `speaker_003`, `speaker_004`
- **Validation Speakers**: `speaker_005`
- **Test Speakers**: `speaker_001`
Recordings from a given speaker NEVER appear simultaneously in training and testing partitions.

---

## 12. Feature Extraction
`scripts/extract_features.py` extracts features saved as compressed `.npz` archives in `features/`:
- **MFCCs**: 13 cepstral coefficients + 13 delta coefficients (26-D)
- **Mel Spectrogram**: 80 triangular mel filterbank log-energy bins
- **Spectral Centroid**: Center of mass of spectral energy
- **Spectral Bandwidth**: Standard deviation of spectral spread
- **Spectral Rolloff**: 85th percentile energy cutoff frequency
- **Zero Crossing Rate (ZCR)**: Sign-change rate per frame
- **RMS Energy**: Temporal energy profile
- **Pitch ($F_0$)**: Fundamental frequency via normalized autocorrelation

---

## 13. Speaker Embeddings & Profile Centroids
`scripts/extract_speaker_embeddings.py` generates normalized 128-dimensional acoustic latent representations:
1. Computes embedding vectors for each sample: `embedding_01.npy`, `embedding_02.npy`, ...
2. Calculates L2-normalized mean centroid: `centroid.npy`
3. Verification uses cosine similarity:
   $$\text{sim}(u, v) = \frac{u \cdot v}{\|u\|_2 \|v\|_2}$$

---

## 14. Demo Dataset & Pre-Configured Scenarios
The `demo_data/` directory and `demo_scenarios.json` file provide verified demo samples matching the 5 required project scenarios:
- **Scenario 1: Genuine Trusted Speaker** (Clone prob: 8%, Speaker sim: 94%, Risk: LOW)
- **Scenario 2: AI Voice Impersonation** (Clone prob: 91%, Speaker sim: 89%, Likely impersonated: Speaker 001, Risk: CRITICAL)
- **Scenario 3: Unknown AI Voice** (Clone prob: 87%, Speaker sim: 22%, Likely speaker: Unknown, Risk: HIGH)
- **Scenario 4: Suspicious Financial Request** (Clone prob: 90%, Speaker sim: 91%, Intent: Financial / OTP request, Risk: CRITICAL)
- **Scenario 5: Verification Failed** (Verification: FAILED, Risk: CRITICAL)

*Note: All simulated values are clearly flagged with `demo_mode: true` for academic demonstration integrity.*

---

## 15. Dataset Limitations
1. **Replay Dataset**: Physical multi-room microphone replay data is limited; channel effects are marked **EXPERIMENTAL**.
2. **Environmental Noise**: Custom speech samples were recorded in controlled academic environments; wild acoustic evaluations require further data collection.
3. **Multilingual Evaluation**: Multilingual coverage is focused on English, Hindi, and Marathi; cross-lingual speaker matching requires phoneme-independent conditioning.

---

## 16. Privacy, Ethics & Informed Consent
- All custom speech recordings were collected with informed participant consent exclusively for academic research on voice security.
- Audio samples are anonymized under identifiers (`speaker_001` to `speaker_005`).
- No private telephone calls or unconsented speech data are contained in this corpus.
- All API keys, credentials, and private biometric records are strictly excluded.
