"""Cloud Functions for Orbit. The AI assistant runs here; provider keys stay in the secret manager."""

from firebase_admin import initialize_app

initialize_app()
