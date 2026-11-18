"""The assistant's HTTP server: checks who is calling, then asks the model. Runs in Docker next to the model (ADR 0007).

It speaks the shape of a Firebase callable, so the app calls it like one: `{"data": …}` in, `{"result": …}` or
`{"error": {status, message, details}}` out, with the caller's Firebase ID token as a bearer token. Standard library
only, so its image installs nothing.
"""

import json
import logging
from collections.abc import Mapping
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

from assistant import auth, providers, quota
from assistant.interview import AssistantError, Generate, next_step

HTTP_STATUS = {
    "INVALID_ARGUMENT": 400,
    "UNAUTHENTICATED": 401,
    "FAILED_PRECONDITION": 412,
    "RESOURCE_EXHAUSTED": 429,
    "UNAVAILABLE": 503,
}

# Values of ASSISTANT_ENABLED that switch the assistant off (#68). Anything else, or nothing, leaves it on.
OFF = {"false", "0", "no", "off"}

# Sent with every answer (#324, from the OWASP ZAP scan): the body is JSON and never to be sniffed as something else,
# an interview belongs to one user so nothing on the way keeps a copy, and the server is only reached over HTTPS.
SECURITY_HEADERS = {
    "X-Content-Type-Options": "nosniff",
    "Cache-Control": "no-store",
    "Strict-Transport-Security": "max-age=31536000",
}


def assistant_enabled(env: Mapping[str, str]) -> bool:
    """Whether the kill switch is left alone: ASSISTANT_ENABLED in the environment says `false` to turn it off."""
    return env.get("ASSISTANT_ENABLED", "true").strip().lower() not in OFF


def caller(authorization: str, verify_token: auth.VerifyToken) -> str | None:
    """The signed-in caller's uid from the Authorization header, or None without a valid ID token."""
    if not authorization.startswith("Bearer "):
        return None
    try:
        return verify_token(authorization.removeprefix("Bearer "))
    except Exception:
        logging.info("Rejected a call with an invalid ID token")
        return None


def respond(
    method: str,
    path: str,
    authorization: str,
    verify_token: auth.VerifyToken,
    generate: Generate,
    body: bytes = b"",
    enabled: bool = True,
    meter: quota.Meter = lambda uid: None,
) -> tuple[int, dict]:
    """
    The HTTP status and JSON body for a request, apart from the socket so tests can call it directly. Every signed-in
    call to the interview counts against the caller's quota ([meter]) before the model is asked.
    """
    if (method, path) == ("GET", "/health"):
        return 200, {"ok": True}
    if (method, path) == ("POST", "/interview"):
        try:
            if not enabled:
                raise AssistantError("FAILED_PRECONDITION", "The assistant is switched off.", {"reason": "disabled"})
            uid = caller(authorization, verify_token)
            if uid is not None:
                meter(uid)
            return 200, {"result": next_step(uid, call_data(body), generate)}
        except AssistantError as e:
            return HTTP_STATUS[e.status], {"error": {"status": e.status, "message": e.message, "details": e.details}}
    return 404, {"error": {"status": "NOT_FOUND", "message": "No such endpoint."}}


def call_data(body: bytes) -> object:
    """The `data` of a callable's body, or None when the body is not one."""
    try:
        request = json.loads(body or b"{}")
    except ValueError:
        return None
    return request.get("data") if isinstance(request, dict) else None


def handler(
    verify_token: auth.VerifyToken, generate: Generate, enabled: bool = True, meter: quota.Meter = lambda uid: None
) -> type[BaseHTTPRequestHandler]:
    """The request handler, bound to how tokens are checked, which model answers, whether it is on and the quota."""

    class Handler(BaseHTTPRequestHandler):
        def do_GET(self):
            self.answer("GET")

        def do_POST(self):
            self.answer("POST", self.rfile.read(int(self.headers.get("Content-Length") or 0)))

        def answer(self, method: str, request: bytes = b""):
            authorization = self.headers.get("Authorization", "")
            status, body = respond(method, self.path, authorization, verify_token, generate, request, enabled, meter)
            data = json.dumps(body).encode()
            self.send_response(status)
            self.send_header("Content-Type", "application/json")
            self.send_header("Content-Length", str(len(data)))
            for name, value in SECURITY_HEADERS.items():
                self.send_header(name, value)
            self.end_headers()
            self.wfile.write(data)

        def log_message(self, format, *args):
            logging.info(format, *args)

    return Handler


def server(env: Mapping[str, str]) -> ThreadingHTTPServer:
    """The server the environment describes. In Docker it listens on every interface, behind the reverse proxy."""
    address = (env.get("HOST", "0.0.0.0"), int(env.get("PORT", "8080")))
    if not assistant_enabled(env):
        logging.warning("The assistant is switched off (ASSISTANT_ENABLED=%s)", env.get("ASSISTANT_ENABLED"))
    return ThreadingHTTPServer(
        address,
        handler(auth.firebase_tokens(env), providers.from_env(env), assistant_enabled(env), quota.from_env(env)),
    )

