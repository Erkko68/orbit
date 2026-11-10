"""The assistant's HTTP server: checks who is calling, then asks the model. Runs in Docker next to the model (ADR 0007).

It speaks the shape of a Firebase callable, so the app calls it like one: `{"data": …}` in, `{"result": …}` or
`{"error": {status, message, details}}` out, with the caller's Firebase ID token as a bearer token. Standard library
only, so its image installs nothing.
"""

import json
import logging
from collections.abc import Mapping
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

from assistant import auth, providers
from assistant.interview import AssistantError, Generate, first_question

HTTP_STATUS = {"UNAUTHENTICATED": 401, "UNAVAILABLE": 503}


def caller(authorization: str, verify_token: auth.VerifyToken) -> str | None:
    """The signed-in caller's uid from the Authorization header, or None without a valid ID token."""
    if not authorization.startswith("Bearer "):
        return None
    try:
        return verify_token(authorization.removeprefix("Bearer "))
    except Exception:
        logging.info("Rejected a call with an invalid ID token")
        return None


def respond(method: str, path: str, authorization: str, verify_token: auth.VerifyToken, generate: Generate) -> tuple[int, dict]:
    """The HTTP status and JSON body for a request, apart from the socket so tests can call it directly."""
    if (method, path) == ("GET", "/health"):
        return 200, {"ok": True}
    if (method, path) == ("POST", "/interview"):
        try:
            return 200, {"result": first_question(caller(authorization, verify_token), generate)}
        except AssistantError as e:
            return HTTP_STATUS[e.status], {"error": {"status": e.status, "message": e.message, "details": e.details}}
    return 404, {"error": {"status": "NOT_FOUND", "message": "No such endpoint."}}


def handler(verify_token: auth.VerifyToken, generate: Generate) -> type[BaseHTTPRequestHandler]:
    """The request handler, bound to how tokens are checked and which model answers."""

    class Handler(BaseHTTPRequestHandler):
        def do_GET(self):
            self.answer("GET")

        def do_POST(self):
            # The body ({"data": …}) is read so the connection stays clean; the first question needs none of it.
            self.rfile.read(int(self.headers.get("Content-Length") or 0))
            self.answer("POST")

        def answer(self, method: str):
            status, body = respond(method, self.path, self.headers.get("Authorization", ""), verify_token, generate)
            data = json.dumps(body).encode()
            self.send_response(status)
            self.send_header("Content-Type", "application/json")
            self.send_header("Content-Length", str(len(data)))
            self.end_headers()
            self.wfile.write(data)

        def log_message(self, format, *args):
            logging.info(format, *args)

    return Handler


def server(env: Mapping[str, str]) -> ThreadingHTTPServer:
    """The server the environment describes. In Docker it listens on every interface, behind the reverse proxy."""
    address = (env.get("HOST", "0.0.0.0"), int(env.get("PORT", "8080")))
    return ThreadingHTTPServer(address, handler(auth.firebase_tokens(env), providers.from_env(env)))

