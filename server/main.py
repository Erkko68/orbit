"""The assistant's HTTP server: checks who is calling, then asks the model. Runs in Docker next to the model (ADR 0007).

It speaks the shape of a Firebase callable, so the app calls it like one: `{"data": …}` in, `{"result": …}` or
`{"error": {status, message, details}}` out, with the caller's Firebase ID token as a bearer token.
"""

import logging
import os
from collections.abc import Callable

from flask import Flask, jsonify, request

import models
from assistant import AssistantError, Generate, first_question

HTTP_STATUS = {"UNAUTHENTICATED": 401, "UNAVAILABLE": 503}

# The uid in a Firebase ID token, or an exception when the token is not valid.
VerifyToken = Callable[[str], dict]


def firebase_tokens() -> VerifyToken:
    """firebase-admin's check, which needs only the project id: no service account, no billing."""
    import firebase_admin
    from firebase_admin import auth

    firebase_admin.initialize_app(options={"projectId": os.environ.get("FIREBASE_PROJECT_ID", "orbit-app-dev")})
    return auth.verify_id_token


def create_app(verify_token: VerifyToken | None = None, generate: Generate | None = None) -> Flask:
    """The server, with Firebase and the configured model unless tests pass their own."""
    verify_token = verify_token or firebase_tokens()
    generate = generate or models.from_env(os.environ)
    app = Flask(__name__)

    def caller() -> str | None:
        """The signed-in caller's uid, or None without a valid ID token."""
        header = request.headers.get("Authorization", "")
        if not header.startswith("Bearer "):
            return None
        try:
            return verify_token(header.removeprefix("Bearer "))["uid"]
        except Exception:
            logging.info("Rejected a call with an invalid ID token")
            return None

    @app.post("/interview")
    def interview():
        try:
            return jsonify(result=first_question(caller(), generate))
        except AssistantError as e:
            error = {"status": e.status, "message": e.message, "details": e.details}
            return jsonify(error=error), HTTP_STATUS[e.status]

    @app.get("/health")
    def health():
        return jsonify(ok=True)

    return app
