"""
VoiceShield AI - Authentication & User Isolation Module
======================================================
Verifies Firebase Authentication tokens sent from the Android application.
Enforces per-user data isolation so User A never accesses User B's voice profile,
analysis sessions, or security reports.
"""

import os
from typing import Optional, Dict, Any
from fastapi import HTTPException, Security, Depends, status
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials
from backend.config import FIREBASE_PROJECT_ID, DEV_AUTH_ENABLED

security = HTTPBearer(auto_error=False)

# Optional firebase_admin initialization
_firebase_initialized = False
try:
    import firebase_admin
    from firebase_admin import auth as firebase_auth, credentials
    
    cred_path = os.getenv("GOOGLE_APPLICATION_CREDENTIALS", "")
    if cred_path and os.path.exists(cred_path):
        cred = credentials.Certificate(cred_path)
        firebase_admin.initialize_app(cred)
        _firebase_initialized = True
    elif os.getenv("FIREBASE_CONFIG") or os.getenv("GCLOUD_PROJECT"):
        firebase_admin.initialize_app()
        _firebase_initialized = True
except Exception:
    _firebase_initialized = False


def verify_firebase_token(id_token: str) -> Dict[str, Any]:
    """
    Verify Firebase ID Token.
    In production: calls firebase_admin.auth.verify_id_token.
    In dev/test mode: validates structured dev tokens or mock payloads.
    """
    if _firebase_initialized:
        try:
            decoded = firebase_auth.verify_id_token(id_token)
            return {
                "uid": decoded.get("uid"),
                "email": decoded.get("email", ""),
                "name": decoded.get("name", "User"),
                "is_anonymous": decoded.get("firebase", {}).get("sign_in_provider") == "anonymous"
            }
        except Exception as e:
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail=f"Invalid or expired Firebase token: {str(e)}"
            )

    # Dev / Local Prototype Mode Fallback
    if DEV_AUTH_ENABLED:
        # Support test tokens like: "Bearer dev-user-<uid>" or simple JWT-like formats
        clean_token = id_token.strip()
        if clean_token.startswith("dev-") or clean_token.startswith("test-"):
            parts = clean_token.split(":")
            uid = parts[0]
            email = parts[1] if len(parts) > 1 else f"{uid}@voiceshield.internal"
            return {
                "uid": uid,
                "email": email,
                "name": uid.replace("dev-", "").replace("-", " ").title(),
                "is_anonymous": False
            }
        
        # Base64 or standard string fallback for simulator testing
        return {
            "uid": f"user-{clean_token[:12] if len(clean_token) >= 12 else 'demo-default'}",
            "email": "demo@voiceshield.ai",
            "name": "Demo Security Analyst",
            "is_anonymous": False
        }

    raise HTTPException(
        status_code=status.HTTP_401_UNAUTHORIZED,
        detail="Authentication service unavailable. Please configure Firebase credentials."
    )


async def get_current_user(
    credentials: Optional[HTTPAuthorizationCredentials] = Security(security)
) -> Dict[str, Any]:
    """FastAPI dependency for protected routes requiring valid authentication."""
    if not credentials or not credentials.credentials:
        # In DEV_AUTH_ENABLED mode, provide a default isolated session user if none provided
        if DEV_AUTH_ENABLED:
            return {
                "uid": "default-local-user",
                "email": "local@voiceshield.ai",
                "name": "Local Analyst",
                "is_anonymous": True
            }
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Authentication token required. Bearer ID token missing."
        )

    return verify_firebase_token(credentials.credentials)


async def get_optional_user(
    credentials: Optional[HTTPAuthorizationCredentials] = Security(security)
) -> Optional[Dict[str, Any]]:
    """FastAPI dependency for routes that can function with or without auth."""
    if not credentials or not credentials.credentials:
        return None
    try:
        return verify_firebase_token(credentials.credentials)
    except HTTPException:
        return None
