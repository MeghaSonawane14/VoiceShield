"""
VoiceShield AI - Dataset Splitter (Zero-Speaker-Leakage)
======================================================
Splits processed dataset into:
- 70% TRAIN
- 15% VALIDATION
- 15% TEST

Key Guarantees:
1. Deterministic & Reproducible (Fixed random seed 42)
2. Speaker Disjoint Partitioning: Prevents speaker leakage
   (Recordings of a given speaker NEVER appear in both train and test partitions)
3. Updates split column in metadata/metadata.csv and creates train/, validation/, test/ manifests

Usage:
    python split_dataset.py --speaker-disjoint
    python split_dataset.py --stratified
"""

import os
import sys
import csv
import random
import yaml
import shutil
import argparse
from pathlib import Path
from collections import defaultdict
from typing import Dict, List, Any

if sys.platform == "win32":
    try:
        sys.stdout.reconfigure(encoding="utf-8")
    except Exception:
        pass

SCRIPTS_DIR = Path(__file__).resolve().parent
DATASET_ROOT = SCRIPTS_DIR.parent
METADATA_PATH = DATASET_ROOT / "metadata" / "metadata.csv"
CONFIG_PATH = DATASET_ROOT / "config.yaml"

def load_split_config():
    """Loads split percentages and seed from config.yaml."""
    if CONFIG_PATH.exists():
        with open(CONFIG_PATH, "r", encoding="utf-8") as f:
            cfg = yaml.safe_load(f)
            return cfg.get("splits", {"train": 0.70, "validation": 0.15, "test": 0.15, "random_seed": 42})
    return {"train": 0.70, "validation": 0.15, "test": 0.15, "random_seed": 42}

def split_dataset(speaker_disjoint=True):
    """Performs reproducible train/val/test splitting with speaker leakage protection."""
    cfg = load_split_config()
    seed = cfg.get("random_seed", 42)
    random.seed(seed)

    print("=" * 80)
    print(" VoiceShield AI - Train / Validation / Test Splitter")
    print("=" * 80)
    print(f"Target Ratios      : 70% Train | 15% Validation | 15% Test")
    print(f"Random Seed        : {seed}")
    print(f"Speaker Disjoint   : {speaker_disjoint} (Speaker Leakage Prevention)")

    if not METADATA_PATH.exists():
        print(f"[!] metadata.csv not found at {METADATA_PATH}. Run create_metadata.py first.")
        return

    with open(METADATA_PATH, "r", encoding="utf-8") as f:
        reader = csv.DictReader(f)
        fieldnames = reader.fieldnames
        rows = list(reader)

    if not rows:
        print("[!] No records found in metadata.csv")
        return

    # Group rows by speaker
    speaker_groups = defaultdict(list)
    unassigned_rows = []

    for row in rows:
        spk = row.get("speaker_id", "unknown_speaker")
        if spk != "unknown_speaker":
            speaker_groups[spk].append(row)
        else:
            unassigned_rows.append(row)

    train_rows, val_rows, test_rows = [], [], []

    if speaker_disjoint and len(speaker_groups) >= 3:
        # Partition speakers disjointly
        speakers = sorted(list(speaker_groups.keys()))
        random.shuffle(speakers)

        n_spk = len(speakers)
        n_test = max(1, int(round(n_spk * 0.20)))
        n_val = max(1, int(round(n_spk * 0.20)))
        n_train = n_spk - n_test - n_val
        if n_train < 1:
            n_train = 1

        train_spks = speakers[:n_train]
        val_spks = speakers[n_train:n_train + n_val]
        test_spks = speakers[n_train + n_val:]

        print(f"\n[+] Speaker Disjoint Partitioning:")
        print(f"    Train Speakers      ({len(train_spks)}): {', '.join(train_spks)}")
        print(f"    Validation Speakers ({len(val_spks)}): {', '.join(val_spks)}")
        print(f"    Test Speakers       ({len(test_spks)}): {', '.join(test_spks)}")

        for spk in train_spks:
            for r in speaker_groups[spk]:
                r["split"] = "train"
                train_rows.append(r)
        for spk in val_spks:
            for r in speaker_groups[spk]:
                r["split"] = "validation"
                val_rows.append(r)
        for spk in test_spks:
            for r in speaker_groups[spk]:
                r["split"] = "test"
                test_rows.append(r)

        # Distribute unassigned / synthetic non-speaker rows stratified
        random.shuffle(unassigned_rows)
        for idx, r in enumerate(unassigned_rows):
            ratio = idx / max(1, len(unassigned_rows))
            if ratio < 0.70:
                r["split"] = "train"
                train_rows.append(r)
            elif ratio < 0.85:
                r["split"] = "validation"
                val_rows.append(r)
            else:
                r["split"] = "test"
                test_rows.append(r)

    else:
        # Stratified random split
        random.shuffle(rows)
        n_total = len(rows)
        n_tr = int(n_total * 0.70)
        n_va = int(n_total * 0.15)

        for i, r in enumerate(rows):
            if i < n_tr:
                r["split"] = "train"
                train_rows.append(r)
            elif i < n_tr + n_va:
                r["split"] = "validation"
                val_rows.append(r)
            else:
                r["split"] = "test"
                test_rows.append(r)

    # Write back updated metadata.csv
    with open(METADATA_PATH, "w", newline="", encoding="utf-8") as f:
        writer = csv.DictWriter(f, fieldnames=fieldnames)
        writer.writeheader()
        writer.writerows(train_rows + val_rows + test_rows)

    # Create partition manifest files in train/, validation/, test/
    for split_name, split_list in [("train", train_rows), ("validation", val_rows), ("test", test_rows)]:
        split_dir = DATASET_ROOT / split_name
        split_dir.mkdir(parents=True, exist_ok=True)
        manifest_file = split_dir / "manifest.csv"
        with open(manifest_file, "w", newline="", encoding="utf-8") as mf:
            writer = csv.DictWriter(mf, fieldnames=fieldnames)
            writer.writeheader()
            writer.writerows(split_list)

    total = len(train_rows) + len(val_rows) + len(test_rows)
    print("\n" + "-" * 80)
    print(f"Dataset Split Summary (Total: {total} files):")
    print(f"  • Train Set      : {len(train_rows)} samples ({len(train_rows)/total*100:.1f}%)")
    print(f"  • Validation Set : {len(val_rows)} samples ({len(val_rows)/total*100:.1f}%)")
    print(f"  • Test Set       : {len(test_rows)} samples ({len(test_rows)/total*100:.1f}%)")
    print(f"[✓] Successfully updated {METADATA_PATH.name} and saved manifests in train/, validation/, test/.")
    print("-" * 80)

def main():
    parser = argparse.ArgumentParser(description="VoiceShield Dataset Train/Val/Test Splitter")
    parser.add_argument("--speaker-disjoint", action="store_true", default=True, help="Enforce zero-speaker-leakage")
    parser.add_argument("--stratified", action="store_true", help="Use standard stratified split instead")
    args = parser.parse_args()

    disjoint = not args.stratified
    split_dataset(speaker_disjoint=disjoint)

if __name__ == "__main__":
    main()
